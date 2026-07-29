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
        override fun onSessionStarting(session: CastSession) {}
        override fun onSessionStarted(session: CastSession, sessionId: String) {
            onSessionStatusChanged(session, true)
        }
        override fun onSessionStartFailed(session: CastSession, error: Int) {
            onSessionStatusChanged(null, false)
        }
        override fun onSessionEnding(session: CastSession) {}
        override fun onSessionEnded(session: CastSession, error: Int) {
            onSessionStatusChanged(null, false)
        }
        override fun onSessionResuming(session: CastSession, sessionId: String) {}
        override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {
            onSessionStatusChanged(session, true)
        }
        override fun onSessionResumeFailed(session: CastSession, error: Int) {
            onSessionStatusChanged(null, false)
        }
        override fun onSessionSuspended(session: CastSession, reason: Int) {
            onSessionStatusChanged(null, false)
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

    fun stopCasting() {
        try {
            castContext.sessionManager.endCurrentSession(true)
        } catch (e: Exception) {
            Log.e("CastManager", "Error ending session", e)
        }
    }

    val currentSession: CastSession?
        get() = castContext.sessionManager.currentCastSession

    val isConnected: Boolean
        get() = currentSession?.isConnected == true
}
