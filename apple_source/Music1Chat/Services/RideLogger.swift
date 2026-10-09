import Foundation
import UIKit

/**
 * Diagnostic logger for No Hands Radio (iOS version).
 * Fully synchronized with modern Android RideLogger architecture:
 * - Thread-safe background writes (no UI/audio stutter)
 * - Automatic size-capping (max 512 KB)
 * - 7-day timestamp pruning
 */
final class RideLogger: @unchecked Sendable {
    static let shared = RideLogger()

    private let logFileName = "ride_log.txt"
    private let maxFileSizeBytes: Int = 512 * 1024 // 512 KB cap
    private let writeQueue = DispatchQueue(label: "com.nohandsradio.ridelogger", qos: .utility)

    private var logFileURL: URL? {
        FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first?.appendingPathComponent(logFileName)
    }

    private let dateFormatter: DateFormatter = {
        let df = DateFormatter()
        df.dateFormat = "yyyy-MM-dd HH:mm:ss.SSS"
        return df
    }()

    private init() {
        #if DEBUG
        log("LOG_SYSTEM: Logger initialized in DEBUG mode.")
        #endif
        writeQueue.async { [weak self] in
            self?.pruneOldEntries()
            self?.enforceFileSizeLimit()
        }
    }

 func log(_ message: String) {
     // Console output for active debugging
     print(message)

     // Background write
     writeQueue.async { [weak self] in
         guard let self = self, let url = self.logFileURL else { return }

         // Thread-safe date formatting inside the serial queue
         let timestamp = self.dateFormatter.string(from: Date())
         let logEntry = "\(timestamp)  \(message)\n"

         if let data = logEntry.data(using: .utf8) {
             if FileManager.default.fileExists(atPath: url.path) {
                 if let fileHandle = try? FileHandle(forWritingTo: url) {
                     fileHandle.seekToEndOfFile()
                     fileHandle.write(data)
                     fileHandle.closeFile()
                 }
             } else {
                 try? data.write(to: url, options: .atomic)
             }
         }
     }
 }

    private func pruneOldEntries() {
        guard let url = logFileURL,
              let content = try? String(contentsOf: url, encoding: .utf8) else { return }

        let lines = content.components(separatedBy: .newlines)
        let oneWeekAgo = Calendar.current.date(byAdding: .day, value: -7, to: Date()) ?? Date()
        let cutoffString = dateFormatter.string(from: oneWeekAgo)
        let cutoffDate = String(cutoffString.prefix(10)) // "yyyy-MM-dd"

        let filteredLines = lines.filter { line in
            guard line.count >= 10 else { return true }
            let lineDate = String(line.prefix(10))
            return lineDate >= cutoffDate
        }

        if filteredLines.count < lines.count {
            let newContent = filteredLines.joined(separator: "\n")
            try? newContent.write(to: url, atomically: true, encoding: .utf8)
            print("LOG_SYSTEM: Pruned old log entries. Kept \(filteredLines.count) lines.")
        }
    }

    private func enforceFileSizeLimit() {
        guard let url = logFileURL,
              let attributes = try? FileManager.default.attributesOfItem(atPath: url.path),
              let fileSize = attributes[.size] as? Int,
              fileSize > maxFileSizeBytes,
              let content = try? String(contentsOf: url, encoding: .utf8) else { return }

        let lines = content.components(separatedBy: .newlines)
        // Keep the newest half of the log lines if exceeding size limit
        let linesToKeep = Array(lines.suffix(lines.count / 2))
        let trimmedContent = linesToKeep.joined(separator: "\n")
        try? trimmedContent.write(to: url, atomically: true, encoding: .utf8)
        print("LOG_SYSTEM: Trimmed oversized log file to \(linesToKeep.count) lines.")
    }

    func clearLog() {
        writeQueue.async { [weak self] in
            guard let self = self, let url = self.logFileURL else { return }
            try? FileManager.default.removeItem(at: url)
            print("LOG_SYSTEM: Log cleared.")
        }
    }

    func getLogURL() -> URL? {
        return logFileURL
    }

    func getLogContent() -> String {
        guard let url = logFileURL, let content = try? String(contentsOf: url, encoding: .utf8) else {
            return "No log found."
        }
        return content
    }
}