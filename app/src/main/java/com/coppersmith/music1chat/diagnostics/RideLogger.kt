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
 * Automatic field-oriented diagnostic logger.
 *
 * A new timestamped file is created each time the app process starts.
 * Previous logs remain in Downloads/Music1Chat.
 */
object RideLogger {

    private val RELATIVE_DIRECTORY =
        "${Environment.DIRECTORY_DOWNLOADS}/Music1Chat"

    private const val MAX_LINES = 10_000

    private val lock = Any()

    private val lineTimestampFormat =
        SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss.SSS",
            Locale.US
        )

    private val fileTimestampFormat =
        SimpleDateFormat(
            "yyyy-MM-dd_HH-mm-ss",
            Locale.US
        )

    private var appContext: Context? = null
    private var activeUri: Uri? = null
    private var mostRecentUri: Uri? = null
    private var lineCount: Int = 0

    val isActive: Boolean
        get() =
            synchronized(lock) {
                activeUri != null
            }

    val hasLog: Boolean
        get() =
            synchronized(lock) {
                mostRecentUri != null
            }

    /**
     * Starts a new timestamped log.
     *
     * Calling this again during the same process does nothing, so accidental
     * duplicate initialization cannot create several files.
     */
    fun startAutomatically(
        context: Context
    ): Result<Unit> =
        runCatching {
            require(
                Build.VERSION.SDK_INT >=
                        Build.VERSION_CODES.Q
            ) {
                "Ride logging requires Android 10 or newer."
            }

            synchronized(lock) {
                if (activeUri != null) {
                    return@runCatching
                }

                val applicationContext =
                    context.applicationContext

                val resolver =
                    applicationContext.contentResolver

                val collection =
                    MediaStore.Downloads
                        .EXTERNAL_CONTENT_URI

                val fileName =
                    "Music1Chat-RideLog-" +
                            fileTimestampFormat.format(
                                Date()
                            ) +
                            ".txt"

                val values =
                    ContentValues().apply {
                        put(
                            MediaStore.MediaColumns.DISPLAY_NAME,
                            fileName
                        )
                        put(
                            MediaStore.MediaColumns.MIME_TYPE,
                            "text/plain"
                        )
                        put(
                            MediaStore.MediaColumns.RELATIVE_PATH,
                            RELATIVE_DIRECTORY
                        )
                    }

                val uri =
                    checkNotNull(
                        resolver.insert(
                            collection,
                            values
                        )
                    ) {
                        "Android could not create the ride log file."
                    }

                appContext = applicationContext
                activeUri = uri
                mostRecentUri = uri
                lineCount = 0

                appendLocked("Ride log started")
                appendLocked(
                    "Device Android version=" +
                            "${Build.VERSION.RELEASE} " +
                            "sdk=${Build.VERSION.SDK_INT}"
                )
            }
        }

    fun stop() {
        synchronized(lock) {
            stopLocked(writeFooter = true)
        }
    }

    fun log(
        message: String
    ) {
        synchronized(lock) {
            if (
                activeUri == null ||
                lineCount >= MAX_LINES
            ) {
                return
            }

            appendLocked(
                message.replace('\n', ' ')
            )

            if (lineCount == MAX_LINES) {
                appendLocked(
                    "Maximum ride-log size reached; " +
                            "further events were not recorded."
                )

                stopLocked(
                    writeFooter = false
                )
            }
        }
    }

    fun share(
        context: Context
    ): Result<Unit> =
        runCatching {
            val uri =
                synchronized(lock) {
                    mostRecentUri
                } ?: error(
                    "No ride log is available to share."
                )

            val intent =
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(
                        Intent.EXTRA_STREAM,
                        uri
                    )
                    putExtra(
                        Intent.EXTRA_SUBJECT,
                        "Music1Chat ride log"
                    )
                    addFlags(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }

            context.startActivity(
                Intent.createChooser(
                    intent,
                    "Share ride log"
                ).apply {
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                    )
                }
            )
        }

    private fun stopLocked(
        writeFooter: Boolean
    ) {
        if (
            writeFooter &&
            activeUri != null
        ) {
            appendLocked("Ride log stopped")
        }

        activeUri = null
        appContext = null
    }

    private fun appendLocked(
        message: String
    ) {
        val context =
            appContext ?: return

        val uri =
            activeUri ?: return

        val timestamp =
            lineTimestampFormat.format(
                Date()
            )

        val line =
            "$timestamp  $message\n"

        context.contentResolver
            .openOutputStream(
                uri,
                "wa"
            )
            ?.bufferedWriter()
            ?.use { writer ->
                writer.write(line)
            }
            ?: error(
                "Android could not append to the ride log file."
            )

        lineCount++
    }
}