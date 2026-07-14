package com.coppersmith.music1chat.resolver

import com.coppersmith.music1chat.models.Station
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import android.util.Log

class StreamResolver {

    suspend fun resolve(
        station: Station
    ): ResolutionResult = withContext(Dispatchers.IO) {

        val candidateUrls = buildList {
            val savedResolvedUrl =
                station.resolvedStreamUrl.trim()

            if (savedResolvedUrl.isNotBlank()) {
                add(savedResolvedUrl)
            }

            val originalUrl =
                station.streamUrl.trim()

            if (originalUrl.isNotBlank()) {
                add(originalUrl)
            }

            knownReplacementUrl(station)
                ?.let(::add)
        }.distinct()

        if (candidateUrls.isEmpty()) {
            return@withContext ResolutionResult(
                success = false,
                errorMessage = "The station has no stream URL."
            )
        }

        var lastFailure: ResolutionResult? = null

        candidateUrls.forEach { candidateUrl ->
            val result = resolveUrl(
                startingUrl = candidateUrl,
                depth = 0,
                visitedUrls = mutableSetOf()
            )

            if (result.success) {
                return@withContext result
            }

            lastFailure = result
        }

        lastFailure
            ?: ResolutionResult(
                success = false,
                errorMessage = "No working stream URL was found."
            )
    }

    private fun resolveUrl(
        startingUrl: String,
        depth: Int,
        visitedUrls: MutableSet<String>
    ): ResolutionResult {
        if (depth > MAX_RESOLUTION_DEPTH) {
            return ResolutionResult(
                success = false,
                errorMessage =
                    "The playlist nesting limit was exceeded."
            )
        }

        if (!visitedUrls.add(startingUrl)) {
            return ResolutionResult(
                success = false,
                errorMessage =
                    "A redirect or playlist loop was detected."
            )
        }

        var currentUrl = startingUrl
        var redirectCount = 0

        while (redirectCount <= MAX_REDIRECTS) {
            val connection =
                try {
                    openConnection(currentUrl)
                } catch (exception: Exception) {
                    return ResolutionResult(
                        success = false,
                        errorMessage =
                            exception.message
                                ?: "Unable to open the stream URL."
                    )
                }

            try {
                val responseCode =
                    connection.responseCode

                if (responseCode in REDIRECT_CODES) {
                    val location =
                        connection
                            .getHeaderField("Location")
                            ?.trim()

                    if (location.isNullOrBlank()) {
                        return ResolutionResult(
                            success = false,
                            errorMessage =
                                "The server redirected without a destination."
                        )
                    }

                    currentUrl =
                        URL(
                            URL(currentUrl),
                            location
                        ).toString()

                    if (!visitedUrls.add(currentUrl)) {
                        return ResolutionResult(
                            success = false,
                            errorMessage =
                                "A redirect loop was detected."
                        )
                    }

                    redirectCount++
                    continue
                }

                if (responseCode !in 200..299) {
                    return ResolutionResult(
                        success = false,
                        errorMessage =
                            "The stream server returned HTTP $responseCode."
                    )
                }

                val contentType =
                    connection.contentType
                        ?.lowercase()
                        .orEmpty()

                if (looksLikeHtml(contentType)) {
                    return ResolutionResult(
                        success = false,
                        errorMessage =
                            "The URL returned a web page instead of audio."
                    )
                }

                if (looksLikePls(currentUrl, contentType)) {
                    val playlistText =
                        readLimitedText(connection)

                    val playlistUrls =
                        parsePls(
                            playlistText = playlistText,
                            baseUrl = currentUrl
                        )

                    return resolvePlaylistCandidates(
                        candidateUrls = playlistUrls,
                        depth = depth + 1,
                        visitedUrls = visitedUrls
                    )
                }

                if (looksLikeM3u(currentUrl, contentType)) {
                    /*
                     * HLS .m3u8 URLs are directly playable by
                     * ExoPlayer and should not be reduced to one
                     * media-segment URL.
                     */
                    if (currentUrl
                            .substringBefore("?")
                            .lowercase()
                            .endsWith(".m3u8")
                    ) {
                        return verifiedResult(currentUrl)
                    }

                    val playlistText =
                        readLimitedText(connection)

                    val playlistUrls =
                        parseM3u(
                            playlistText = playlistText,
                            baseUrl = currentUrl
                        )

                    return resolvePlaylistCandidates(
                        candidateUrls = playlistUrls,
                        depth = depth + 1,
                        visitedUrls = visitedUrls
                    )
                }

                /*
                 * A successful non-HTML, non-playlist response is
                 * treated as a playable stream endpoint.
                 */
                return verifiedResult(currentUrl)

            } catch (exception: Exception) {
                return ResolutionResult(
                    success = false,
                    errorMessage =
                        exception.message
                            ?: "Unable to verify the stream."
                )
            } finally {
                connection.disconnect()
            }
        }

        return ResolutionResult(
            success = false,
            errorMessage =
                "The stream exceeded the redirect limit."
        )
    }

    private fun resolvePlaylistCandidates(
        candidateUrls: List<String>,
        depth: Int,
        visitedUrls: MutableSet<String>
    ): ResolutionResult {
        if (candidateUrls.isEmpty()) {
            return ResolutionResult(
                success = false,
                errorMessage =
                    "The playlist contained no stream URLs."
            )
        }

        var lastFailure: ResolutionResult? = null

        candidateUrls.forEach { candidateUrl ->
            val result = resolveUrl(
                startingUrl = candidateUrl,
                depth = depth,
                visitedUrls = visitedUrls
            )

            if (result.success) {
                return result
            }

            lastFailure = result
        }

        return lastFailure
            ?: ResolutionResult(
                success = false,
                errorMessage =
                    "No playable stream was found in the playlist."
            )
    }

    private fun parsePls(
        playlistText: String,
        baseUrl: String
    ): List<String> {
        return playlistText
            .lineSequence()
            .map { it.trim() }
            .filter { line ->
                line.matches(
                    Regex(
                        pattern = """(?i)File\d+\s*=.*"""
                    )
                )
            }
            .mapNotNull { line ->
                line.substringAfter(
                    delimiter = "=",
                    missingDelimiterValue = ""
                )
                    .trim()
                    .takeIf { it.isNotBlank() }
            }
            .mapNotNull { value ->
                resolveRelativeUrl(
                    baseUrl = baseUrl,
                    candidateUrl = value
                )
            }
            .distinct()
            .toList()
    }

    private fun parseM3u(
        playlistText: String,
        baseUrl: String
    ): List<String> {
        return playlistText
            .lineSequence()
            .map { it.trim() }
            .filter { line ->
                line.isNotBlank() &&
                        !line.startsWith("#")
            }
            .mapNotNull { value ->
                resolveRelativeUrl(
                    baseUrl = baseUrl,
                    candidateUrl = value
                )
            }
            .distinct()
            .toList()
    }

    private fun resolveRelativeUrl(
        baseUrl: String,
        candidateUrl: String
    ): String? {
        return try {
            URL(
                URL(baseUrl),
                candidateUrl
            ).toString()
        } catch (_: Exception) {
            null
        }
    }

    private fun readLimitedText(
        connection: HttpURLConnection
    ): String {
        return connection.inputStream
            .bufferedReader()
            .use { reader ->
                val buffer = CharArray(
                    PLAYLIST_BUFFER_SIZE
                )

                val builder = StringBuilder()
                var totalRead = 0

                while (totalRead < MAX_PLAYLIST_CHARACTERS) {
                    val remaining =
                        MAX_PLAYLIST_CHARACTERS - totalRead

                    val count =
                        reader.read(
                            buffer,
                            0,
                            minOf(
                                buffer.size,
                                remaining
                            )
                        )

                    if (count <= 0) {
                        break
                    }

                    builder.append(
                        buffer,
                        0,
                        count
                    )

                    totalRead += count
                }

                builder.toString()
            }
    }

    private fun looksLikePls(
        url: String,
        contentType: String
    ): Boolean {
        val cleanUrl =
            url.substringBefore("?")
                .lowercase()

        return cleanUrl.endsWith(".pls") ||
                contentType.contains("audio/x-scpls") ||
                contentType.contains("application/pls") ||
                contentType.contains("application/x-scpls")
    }

    private fun looksLikeM3u(
        url: String,
        contentType: String
    ): Boolean {
        val cleanUrl =
            url.substringBefore("?")
                .lowercase()

        return cleanUrl.endsWith(".m3u") ||
                cleanUrl.endsWith(".m3u8") ||
                contentType.contains("audio/x-mpegurl") ||
                contentType.contains("application/x-mpegurl") ||
                contentType.contains(
                    "application/vnd.apple.mpegurl"
                )
    }

    private fun looksLikeHtml(
        contentType: String
    ): Boolean {
        return contentType.contains("text/html") ||
                contentType.contains(
                    "application/xhtml"
                )
    }

    private fun verifiedResult(
        resolvedUrl: String
    ): ResolutionResult {
        return ResolutionResult(
            success = true,
            resolvedUrl = resolvedUrl,
            verified = true
        )
    }

    private fun knownReplacementUrl(
        station: Station
    ): String? {
        val normalizedName =
            station.name
                .trim()
                .lowercase()

        return when {
            normalizedName.contains("kdfc") ->
                KDFC_REPLACEMENT_URL

            else -> null
        }
    }

    private fun openConnection(
        streamUrl: String
    ): HttpURLConnection {
        return (
                URL(streamUrl).openConnection()
                        as HttpURLConnection
                ).apply {

                instanceFollowRedirects = false
                requestMethod = "GET"

                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS

                useCaches = false

                setRequestProperty(
                    "User-Agent",
                    USER_AGENT
                )

                setRequestProperty(
                    "Accept",
                    "audio/*, application/pls, " +
                            "audio/x-scpls, " +
                            "application/x-mpegurl, " +
                            "application/vnd.apple.mpegurl, */*"
                )
            }
    }

    companion object {
        private const val MAX_REDIRECTS = 8
        private const val MAX_RESOLUTION_DEPTH = 4

        private const val CONNECT_TIMEOUT_MS = 5_000
        private const val READ_TIMEOUT_MS = 5_000

        private const val PLAYLIST_BUFFER_SIZE = 4_096
        private const val MAX_PLAYLIST_CHARACTERS = 262_144

        private const val USER_AGENT =
            "Music1Chat/1.0 Android Radio Resolver"

        private const val KDFC_REPLACEMENT_URL =
            "http://96.aac.pls.kusc.live"

        private val REDIRECT_CODES = setOf(
            HttpURLConnection.HTTP_MOVED_PERM,
            HttpURLConnection.HTTP_MOVED_TEMP,
            HttpURLConnection.HTTP_SEE_OTHER,
            307,
            308
        )
    }
}