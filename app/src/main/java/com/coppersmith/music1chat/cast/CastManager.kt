package com.coppersmith.music1chat.cast

import android.content.Context
import android.util.Log
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener

class CastManager(
    context: Context,
    private val onSessionStatusChanged: (CastSession?, Boolean) -> Unit
) {
    private val applicationContext =
        context.applicationContext

    private val castContext: CastContext by lazy {
        CastContext.getSharedInstance(applicationContext)
    }

    private val sessionManagerListener = object : SessionManagerListener<CastSession> {
        override fun onSessionStarting(session: CastSession) {
            Log.d("CastManager", "onSessionStarting")
        }

        override fun onSessionStarted(session: CastSession, sessionId: String) {
            Log.d("CastManager", "onSessionStarted")
            onSessionStatusChanged(session, true)
        }

        override fun onSessionStartFailed(session: CastSession, error: Int) {
            Log.d("CastManager", "onSessionStartFailed: $error")
            onSessionStatusChanged(null, false)
        }

        override fun onSessionEnding(session: CastSession) {
            Log.d("CastManager", "onSessionEnding")
        }

        override fun onSessionEnded(session: CastSession, error: Int) {
            Log.d("CastManager", "onSessionEnded: $error")
            onSessionStatusChanged(null, false)
        }

        override fun onSessionResuming(session: CastSession, sessionId: String) {
            Log.d("CastManager", "onSessionResuming")
        }

        override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {
            Log.d("CastManager", "onSessionResumed")
            onSessionStatusChanged(session, true)
        }

        override fun onSessionResumeFailed(session: CastSession, error: Int) {
            Log.d("CastManager", "onSessionResumeFailed: $error")
            onSessionStatusChanged(null, false)
        }

        override fun onSessionSuspended(session: CastSession, reason: Int) {
            Log.d("CastManager", "onSessionSuspended: $reason")
        }
    }

    fun register() {
        castContext.sessionManager.addSessionManagerListener(
            sessionManagerListener,
            CastSession::class.java
        )
        
        // Initial check
        val session = castContext.sessionManager.currentCastSession
        if (session?.isConnected == true) {
            onSessionStatusChanged(session, true)
        }
    }

    fun unregister() {
        castContext.sessionManager.removeSessionManagerListener(
            sessionManagerListener,
            CastSession::class.java
        )
    }

    val currentSession: CastSession?
        get() = castContext.sessionManager.currentCastSession

    val isConnected: Boolean
        get() = currentSession?.isConnected == true
}
