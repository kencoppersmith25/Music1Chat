package com.coppersmith.music1chat.diagnostics

import android.content.Context
import java.io.File

/**
 * Automates "Self-Cleaning" for No Hands Radio.
 * 
 * Prevents the app from becoming "clogged" with corrupted temporary files
 * or image fragments that can cause the "App has a bug" system messages.
 */
object MaintenanceManager {

    /**
     * Clears all temporary files from the app's cache directory.
     * Safe to call on startup or shutdown.
     */
    fun scrub(context: Context) {
        try {
            val cacheDir = context.cacheDir
            if (cacheDir != null && cacheDir.exists()) {
                val files = cacheDir.listFiles()
                if (files != null) {
                    for (file in files) {
                        deleteRecursive(file)
                    }
                }
            }
            RideLogger.log("MAINTENANCE: Cache scrubbed successfully.")
        } catch (e: Exception) {
            RideLogger.log("MAINTENANCE: Cache scrub failed: ${e.message}")
        }
    }

    private fun deleteRecursive(fileOrDirectory: File) {
        if (fileOrDirectory.isDirectory) {
            val children = fileOrDirectory.listFiles()
            if (children != null) {
                for (child in children) {
                    deleteRecursive(child)
                }
            }
        }
        fileOrDirectory.delete()
    }
}
