package com.coppersmith.music1chat.ads

import android.app.Activity
import androidx.appcompat.app.AlertDialog

/**
 * Temporary visual test for Music1Chat's advertising policy.
 *
 * This is not an advertising SDK. It lets us verify that AdManager permits
 * and blocks interstitial opportunities at the expected times.
 */
object TestInterstitialPresenter {

    fun show(
        activity: Activity,
        onDismissed: () -> Unit
    ): Boolean {
        if (activity.isFinishing || activity.isDestroyed) {
            return false
        }

        activity.runOnUiThread {
            AlertDialog.Builder(activity)
                .setTitle("Music1Chat Ad Test")
                .setMessage(
                    "A full-page advertisement would appear here.\n\n" +
                            "Music playback should continue underneath it."
                )
                .setPositiveButton("Close") { dialog, _ ->
                    dialog.dismiss()
                }
                .setOnDismissListener {
                    onDismissed()
                }
                .setCancelable(true)
                .show()
        }

        return true
    }
}