package com.coppersmith.music1chat.diagnostics

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar

/**
 * Diagnostic logger for No Hands Radio (Android version).
 * Matches the iPhone "Single File + Self Pruning" behavior.
 */
object RideLogger {

    private const val LOG_FILE_NAME = "ride_log.txt"
    private const val PUBLIC_FILE_NAME = "nhr_ride_log.txt"
    private const val LOG_SUBDIRECTORY = "logs"
    private const val MAX_LINES = 10_000

    private val lock = Any()

    private val timestampFormat =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    private var appContext: Context? = null
    private var logFile: File? = null
    private var publicMediaUri: Uri? = null
    private var lineCount: Int = 0

    val isActive: Boolean
        get() = synchronized(lock) { logFile != null }

    val hasLog: Boolean
        get() = synchronized(lock) { lineCount > 0 }

    fun startAutomatically(context: Context): Result<Unit> = runCatching {
        synchronized(lock) {
            if (logFile != null) return@runCatching

            val applicationContext = context.applicationContext
            this.appContext = applicationContext

            val logDir = File(applicationContext.filesDir, LOG_SUBDIRECTORY)
            if (!logDir.exists()) {
                logDir.mkdirs()
            }

            val file = File(logDir, LOG_FILE_NAME)
            this.logFile = file
            android.util.Log.d("kencheck", "INTERNAL FILE PATH: ${file.absolutePath}")

            // Set up or find the existing public download file
            setupPublicMediaStoreFile(applicationContext)

            // Prune both internal and public logs to 7 days
            pruneOldEntries(file)
            prunePublicMediaStoreFile(applicationContext)

            lineCount = if (file.exists()) file.readLines().size else 0

            log("LOG_SYSTEM: Ride log started")
            log("LOG_SYSTEM: Device Android version=${Build.VERSION.RELEASE} sdk=${Build.VERSION.SDK_INT}")
        }
    }

    private fun setupPublicMediaStoreFile(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val resolver = context.contentResolver
                val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

                val projection = arrayOf(MediaStore.Downloads._ID, MediaStore.Downloads.DISPLAY_NAME)
                val selection = "${MediaStore.Downloads.DISPLAY_NAME} LIKE ?"
                val selectionArgs = arrayOf("nhr_ride_log%.txt")

                val matchingUris = mutableListOf<Uri>()

                resolver.query(
                    collection,
                    projection,
                    selection,
                    selectionArgs,
                    "${MediaStore.Downloads.DATE_ADDED} ASC"
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID)
                    val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DISPLAY_NAME)
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        val name = cursor.getString(nameCol)
                        val uri = ContentUris.withAppendedId(collection, id)

                        if (name == PUBLIC_FILE_NAME) {
                            matchingUris.add(0, uri)
                        } else {
                            matchingUris.add(uri)
                        }
                    }
                }

                if (matchingUris.isNotEmpty()) {
                    publicMediaUri = matchingUris.first()
                    android.util.Log.d("kencheck", "REUSING EXISTING PUBLIC LOG: $publicMediaUri")

                    if (matchingUris.size > 1) {
                        for (i in 1 until matchingUris.size) {
                            try {
                                resolver.delete(matchingUris[i], null, null)
                            } catch (_: Exception) {}
                        }
                    }
                } else {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, PUBLIC_FILE_NAME)
                        put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                        put(MediaStore.Downloads.RELATIVE_PATH, "Download/Music1Chat")
                    }
                    publicMediaUri = resolver.insert(collection, contentValues)
                    android.util.Log.d("kencheck", "CREATED SINGLE PUBLIC MEDIASTORE FILE: $publicMediaUri")
                }
            } catch (e: Exception) {
                android.util.Log.e("kencheck", "MediaStore setup error: ${e.message}", e)
            }
        }
    }

    fun log(message: String) {
        android.util.Log.d("kencheck", message)

        synchronized(lock) {
            val file = logFile ?: return
            if (lineCount >= MAX_LINES) return

            val timestamp = timestampFormat.format(Date())
            val line = "$timestamp  ${message.replace('\n', ' ')}\n"

            // Write to private sandbox
            try {
                file.appendText(line)
                lineCount++
            } catch (e: Exception) {
                android.util.Log.e("kencheck", "Failed writing to internal log: ${e.message}")
            }

            // Append to public MediaStore file
            publicMediaUri?.let { uri ->
                try {
                    appContext?.contentResolver?.openOutputStream(uri, "wa")?.use { stream ->
                        stream.write(line.toByteArray())
                    }
                } catch (e: Exception) {
                    android.util.Log.e("kencheck", "Failed writing to MediaStore log: ${e.message}")
                }
            }
        }
    }

    fun stop() {
        synchronized(lock) {
            log("LOG_SYSTEM: Ride log stopped")
        }

        appContext?.let {
            MaintenanceManager.scrub(it)
        }
    }

    fun share(context: Context): Result<Unit> = runCatching {
        val file = synchronized(lock) { logFile } ?: error("No log file available.")
        if (!file.exists()) error("Log file is empty.")

        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "No Hands Radio Ride Log - $timestamp")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Share Ride Log via…").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    fun clearLog() {
        synchronized(lock) {
            logFile?.let { file ->
                if (file.exists()) {
                    file.delete()
                }
                lineCount = 0
            }
            publicMediaUri?.let { uri ->
                try {
                    appContext?.contentResolver?.openOutputStream(uri, "wt")?.use { stream ->
                        stream.write("".toByteArray())
                    }
                } catch (_: Exception) {}
            }
            log("LOG_SYSTEM: Log cleared.")
        }
    }

    private fun getCutoffDateString(): String {
        val calendar = Calendar.getInstance()

        //correct value for 7 days...
        calendar.add(Calendar.DAY_OF_YEAR, -7)

        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
    }

    private fun pruneOldEntries(file: File) {
        if (!file.exists()) return

        try {
            val cutoffDate = getCutoffDateString()
            val lines = file.readLines()

            val filteredLines = lines.filter { line ->
                if (line.length < 10) return@filter true
                val lineDate = line.substring(0, 10)
                lineDate >= cutoffDate
            }

            if (filteredLines.size < lines.size) {
                file.writeText(filteredLines.joinToString("\n") + "\n")
            }
        } catch (e: Exception) {
            android.util.Log.w("kencheck", "Internal pruning failed: ${e.message}")
        }
    }

    private fun prunePublicMediaStoreFile(context: Context) {
        val uri = publicMediaUri ?: return
        try {
            val cutoffDate = getCutoffDateString()
            val lines = mutableListOf<String>()

            context.contentResolver.openInputStream(uri)?.use { stream ->
                BufferedReader(InputStreamReader(stream)).use { reader ->
                    var line = reader.readLine()
                    while (line != null) {
                        lines.add(line)
                        line = reader.readLine()
                    }
                }
            }

            val filteredLines = lines.filter { line ->
                if (line.length < 10) return@filter true
                val lineDate = line.substring(0, 10)
                lineDate >= cutoffDate
            }

            if (filteredLines.size < lines.size) {
                context.contentResolver.openOutputStream(uri, "wt")?.use { stream ->
                    val output = filteredLines.joinToString("\n") + "\n"
                    stream.write(output.toByteArray())
                }
                android.util.Log.d("kencheck", "Pruned ${lines.size - filteredLines.size} old lines from public log")
            }
        } catch (e: Exception) {
            android.util.Log.w("kencheck", "Public log pruning failed: ${e.message}")
        }
    }
}