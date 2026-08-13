//
//  StreamResolverService.swift
//  Music1Chat
//
//  Created by kenneth coppersmith on 7/21/26.
//

import Foundation

struct StreamResolutionResult {
    let success: Bool
    let resolvedURL: String?
    let verified: Bool
    let errorMessage: String?

    static func verified(_ url: String) -> StreamResolutionResult {
        StreamResolutionResult(
            success: true,
            resolvedURL: url,
            verified: true,
            errorMessage: nil
        )
    }

    static func failure(_ message: String) -> StreamResolutionResult {
        StreamResolutionResult(
            success: false,
            resolvedURL: nil,
            verified: false,
            errorMessage: message
        )
    }
}

final class StreamResolverService {
    private let maximumResolutionDepth = 4
    private let maximumPlaylistCharacters = 262_144

    private let session: URLSession

    init() {
        let configuration = URLSessionConfiguration.ephemeral

        configuration.timeoutIntervalForRequest = 3.5
        configuration.timeoutIntervalForResource = 4
        configuration.requestCachePolicy = .reloadIgnoringLocalCacheData

        session = URLSession(configuration: configuration)
    }

    func resolve(
        station: Station
    ) async -> StreamResolutionResult {
        var candidateURLs: [String] = []

        let originalURL = station.streamURL.trimmingCharacters(
            in: .whitespacesAndNewlines
        )

        if !originalURL.isEmpty {
            candidateURLs.append(originalURL)
        }

        if let replacementURL = knownReplacementURL(
            stationName: station.name
        ) {
            candidateURLs.append(replacementURL)
        }

        candidateURLs = Array(Set(candidateURLs))

        guard !candidateURLs.isEmpty else {
            return .failure("The station has no stream URL.")
        }

        var lastFailure: StreamResolutionResult?

        for candidateURL in candidateURLs {
            let result = await resolveURL(
                startingURL: candidateURL,
                depth: 0,
                visitedURLs: []
            )

            if result.success {
                return result
            }

            lastFailure = result
        }

        return lastFailure
            ?? .failure("No working stream URL was found.")
    }

    private func resolveURL(
        startingURL: String,
        depth: Int,
        visitedURLs: Set<String>
    ) async -> StreamResolutionResult {
        guard depth <= maximumResolutionDepth else {
            return .failure("The playlist nesting limit was exceeded.")
        }

        guard !visitedURLs.contains(startingURL) else {
            return .failure("A redirect or playlist loop was detected.")
        }

        guard let url = URL(string: startingURL) else {
            return .failure("The stream URL is invalid.")
        }

        var updatedVisitedURLs = visitedURLs
        updatedVisitedURLs.insert(startingURL)

        // Try a HEAD request first for speed
        var headRequest = URLRequest(url: url)
        headRequest.httpMethod = "HEAD"
        headRequest.timeoutInterval = 2.0

        if let (data, response) = try? await session.data(for: headRequest),
           let httpResponse = response as? HTTPURLResponse,
           200...299 ~= httpResponse.statusCode {

            let finalURL = httpResponse.url?.absoluteString ?? startingURL
            let contentType = httpResponse.value(forHTTPHeaderField: "Content-Type")?.lowercased() ?? ""

            // If it's a known non-playlist audio type, we're done immediately
            if contentType.contains("audio/mpeg") || contentType.contains("audio/aac") || isHLSURL(finalURL) {
                return .verified(finalURL)
            }
        }

        // Fall back to GET if HEAD failed or if we need to parse a playlist
        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        request.timeoutInterval = 3.5

        request.setValue(
            "Music1Chat/1.0 iOS Radio Resolver",
            forHTTPHeaderField: "User-Agent"
        )

        request.setValue(
            """
            audio/*, application/pls, audio/x-scpls, \
            application/x-mpegurl, \
            application/vnd.apple.mpegurl, */*
            """,
            forHTTPHeaderField: "Accept"
        )

        do {
            let (bytes, response) = try await session.bytes(
                for: request
            )

            guard let httpResponse =
                    response as? HTTPURLResponse else {
                return .failure(
                    "The stream returned an invalid response."
                )
            }

            guard 200...299 ~= httpResponse.statusCode else {
                return .failure(
                    "The stream server returned HTTP \(httpResponse.statusCode)."
                )
            }

            let finalURL =
                httpResponse.url?.absoluteString ?? startingURL

            let contentType =
                httpResponse.value(
                    forHTTPHeaderField: "Content-Type"
                )?
                .lowercased()
                ?? ""

            if looksLikeHTML(contentType: contentType) {
                return .failure(
                    "The URL returned a web page instead of audio."
                )
            }

            if isHLSURL(finalURL) {
                return .verified(finalURL)
            }

            if looksLikePLS(
                url: finalURL,
                contentType: contentType
            ) {
                let playlistText = try await readLimitedText(
                    from: bytes
                )

                let playlistURLs = parsePLS(
                    playlistText: playlistText,
                    baseURL: finalURL
                )

                return await resolvePlaylistCandidates(
                    playlistURLs,
                    depth: depth + 1,
                    visitedURLs: updatedVisitedURLs
                )
            }

            if looksLikeM3U(
                url: finalURL,
                contentType: contentType
            ) {
                let playlistText = try await readLimitedText(
                    from: bytes
                )

                let playlistURLs = parseM3U(
                    playlistText: playlistText,
                    baseURL: finalURL
                )

                return await resolvePlaylistCandidates(
                    playlistURLs,
                    depth: depth + 1,
                    visitedURLs: updatedVisitedURLs
                )
            }

            return .verified(finalURL)

        } catch {
            return .failure(
                error.localizedDescription.isEmpty
                    ? "Unable to verify the stream."
                    : error.localizedDescription
            )
        }
    }

    private func resolvePlaylistCandidates(
        _ candidateURLs: [String],
        depth: Int,
        visitedURLs: Set<String>
    ) async -> StreamResolutionResult {
        guard !candidateURLs.isEmpty else {
            return .failure(
                "The playlist contained no stream URLs."
            )
        }

        var lastFailure: StreamResolutionResult?

        for candidateURL in candidateURLs {
            let result = await resolveURL(
                startingURL: candidateURL,
                depth: depth,
                visitedURLs: visitedURLs
            )

            if result.success {
                return result
            }

            lastFailure = result
        }

        return lastFailure
            ?? .failure(
                "No playable stream was found in the playlist."
            )
    }

    private func readLimitedText(
        from bytes: URLSession.AsyncBytes
    ) async throws -> String {
        var data = Data()
        data.reserveCapacity(
            min(maximumPlaylistCharacters, 16_384)
        )

        for try await byte in bytes {
            data.append(byte)

            if data.count >= maximumPlaylistCharacters {
                break
            }
        }

        return String(data: data, encoding: .utf8)
            ?? String(data: data, encoding: .isoLatin1)
            ?? ""
    }

    private func parsePLS(
        playlistText: String,
        baseURL: String
    ) -> [String] {
        var results: [String] = []

        for rawLine in playlistText.components(
            separatedBy: .newlines
        ) {
            let line = rawLine.trimmingCharacters(
                in: .whitespacesAndNewlines
            )

            guard let equalsIndex = line.firstIndex(of: "=") else {
                continue
            }

            let key = String(line[..<equalsIndex])
                .trimmingCharacters(
                    in: .whitespacesAndNewlines
                )
                .lowercased()

            guard key.hasPrefix("file") else {
                continue
            }

            let value = String(
                line[line.index(after: equalsIndex)...]
            )
            .trimmingCharacters(
                in: .whitespacesAndNewlines
            )

            if let resolvedURL = resolveRelativeURL(
                baseURL: baseURL,
                candidateURL: value
            ) {
                results.append(resolvedURL)
            }
        }

        return unique(results)
    }

    private func parseM3U(
        playlistText: String,
        baseURL: String
    ) -> [String] {
        let results = playlistText
            .components(separatedBy: .newlines)
            .map {
                $0.trimmingCharacters(
                    in: .whitespacesAndNewlines
                )
            }
            .filter {
                !$0.isEmpty && !$0.hasPrefix("#")
            }
            .compactMap {
                resolveRelativeURL(
                    baseURL: baseURL,
                    candidateURL: $0
                )
            }

        return unique(results)
    }

    private func resolveRelativeURL(
        baseURL: String,
        candidateURL: String
    ) -> String? {
        guard !candidateURL.isEmpty,
              let base = URL(string: baseURL),
              let resolved = URL(
                string: candidateURL,
                relativeTo: base
              )?
              .absoluteURL else {
            return nil
        }

        return resolved.absoluteString
    }

    private func looksLikePLS(
        url: String,
        contentType: String
    ) -> Bool {
        let cleanURL = url
            .components(separatedBy: "?")[0]
            .lowercased()

        return cleanURL.hasSuffix(".pls")
            || contentType.contains("audio/x-scpls")
            || contentType.contains("application/pls")
            || contentType.contains("application/x-scpls")
    }

    private func looksLikeM3U(
        url: String,
        contentType: String
    ) -> Bool {
        let cleanURL = url
            .components(separatedBy: "?")[0]
            .lowercased()

        return cleanURL.hasSuffix(".m3u")
            || contentType.contains("audio/x-mpegurl")
            || contentType.contains("application/x-mpegurl")
            || contentType.contains(
                "application/vnd.apple.mpegurl"
            )
    }

    private func isHLSURL(_ url: String) -> Bool {
        url
            .components(separatedBy: "?")[0]
            .lowercased()
            .hasSuffix(".m3u8")
    }

    private func looksLikeHTML(
        contentType: String
    ) -> Bool {
        contentType.contains("text/html")
            || contentType.contains("application/xhtml")
    }

    private func knownReplacementURL(
        stationName: String
    ) -> String? {
        let normalizedName = stationName
            .trimmingCharacters(
                in: .whitespacesAndNewlines
            )
            .lowercased()

        if normalizedName.contains("kdfc") {
            return "http://96.aac.pls.kusc.live"
        }

        return nil
    }

    private func unique(
        _ values: [String]
    ) -> [String] {
        var seen: Set<String> = []

        return values.filter {
            seen.insert($0).inserted
        }
    }
}
