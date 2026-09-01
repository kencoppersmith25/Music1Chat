import Foundation

/**
 * Automates "Self-Cleaning" for No Hands Radio on iOS.
 */
struct MaintenanceService {

    static func scrub() {
        let fileManager = FileManager.default
        guard let cacheURL = fileManager.urls(for: .cachesDirectory, in: .userDomainMask).first else { return }

        do {
            let contents = try fileManager.contentsOfDirectory(at: cacheURL, includingPropertiesForKeys: nil)
            for fileURL in contents {
                try fileManager.removeItem(at: fileURL)
            }
            RideLogger.shared.log("MAINTENANCE: Cache scrubbed successfully.")
        } catch {
            RideLogger.shared.log("MAINTENANCE: Cache scrub failed: \(error.localizedDescription)")
        }
    }
}
