package com.coppersmith.music1chat.diagnostics

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Small, field-oriented diagnostic logger for Bluetooth/navigation ride tests.
 *
 * The current ride log is stored in Downloads/Music1Chat/Music1Chat-RideLog.txt.
 * Starting a new ride log replaces the previous one.
 */
object RideLogger {
    private const val FILE_NAME = "Music1Chat-RideLog.txt"
    private val RELATIVE_DIRECTORY =
        "${Environment.DIRECTORY_DOWNLOADS}/Music1Chat"
    private const val MAX_LINES = 1_000

    private val lock = Any()
    private val timestampFormat =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    private var appContext: Context? = null
    private var activeUri: Uri? = null
    private var mostRecentUri: Uri? = null
    private var lineCount: Int = 0

    val isActive: Boolean
        get() = synchronized(lock) { activeUri != null }

    val hasLog: Boolean
        get() = synchronized(lock) { mostRecentUri != null }

    fun start(context: Context): Result<Unit> = runCatching {
        require(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            "Ride logging requires Android 10 or newer."
        }

        synchronized(lock) {
            stopLocked(writeFooter = false)

            val applicationContext = context.applicationContext
            val resolver = applicationContext.contentResolver
            val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI

            resolver.delete(
                collection,
                "${MediaStore.MediaColumns.DISPLAY_NAME}=? AND " +
                        "${MediaStore.MediaColumns.RELATIVE_PATH}=?",
                arrayOf(FILE_NAME, "$RELATIVE_DIRECTORY/")
            )

            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, FILE_NAME)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                put(MediaStore.MediaColumns.RELATIVE_PATH, RELATIVE_DIRECTORY)
            }

            val uri = checkNotNull(resolver.insert(collection, values)) {
                "Android could not create the ride log file."
            }

            appContext = applicationContext
            activeUri = uri
            mostRecentUri = uri
            lineCount = 0

            appendLocked("Ride log started")
            appendLocked("Device Android version=${Build.VERSION.RELEASE} sdk=${Build.VERSION.SDK_INT}")
        }
    }

    fun stop() {
        synchronized(lock) {
            stopLocked(writeFooter = true)
        }
    }

    fun log(message: String) {
        synchronized(lock) {
            if (activeUri == null || lineCount >= MAX_LINES) return

            appendLocked(message.replace('\n', ' '))

            if (lineCount == MAX_LINES) {
                appendLocked("Maximum ride-log size reached; further events were not recorded.")
                stopLocked(writeFooter = false)
            }
        }
    }

    fun share(context: Context): Result<Unit> = runCatching {
        val uri = synchronized(lock) {
            mostRecentUri
        } ?: error("No ride log is available to share.")

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Music1Chat ride log")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(
            Intent.createChooser(intent, "Share ride log").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }

    private fun stopLocked(writeFooter: Boolean) {
        if (writeFooter && activeUri != null) {
            appendLocked("Ride log stopped")
        }
        activeUri = null
        appContext = null
    }

    private fun appendLocked(message: String) {
        val context = appContext ?: return
        val uri = activeUri ?: return
        val timestamp = timestampFormat.format(Date())
        val line = "$timestamp  $message\n"

        context.contentResolver
            .openOutputStream(uri, "wa")
            ?.bufferedWriter()
            ?.use { writer ->
                writer.write(line)
            }
            ?: error("Android could not append to the ride log file.")

        lineCount++
    }
}