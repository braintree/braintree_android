package com.braintreepayments.api.paymentactions

import android.app.ActivityManager
import android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
import android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE
import android.content.Context
import com.braintreepayments.api.core.BraintreeException
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Starts [ChallengeShellActivity] and waits for the outcome. Fails instead of hanging when the app isn't
 * visible, the shell never starts (handshake), or the provider goes silent (watchdog).
 *
 * @param watchdogTimeout set above the provider's own challenge timeout.
 */
internal class ChallengeCoordinator(
    private val context: Context,
    private val watchdogTimeout: Duration,
    private val applicationStateProvider: ApplicationStateProviding = ActivityManagerApplicationStateProvider,
    private val handshakeTimeout: Duration = HANDSHAKE_TIMEOUT,
) {

    /** Starts the shell and suspends until the challenge ends. */
    suspend fun startChallenge(transactionId: String, payload: String, registrar: ChallengeRegistrar): ChallengeResult {
        if (!applicationStateProvider.isApplicationActive()) {
            return ChallengeResult.Failed(BraintreeException(FOREGROUND_REQUIRED_MESSAGE))
        }

        val challenge = PendingChallenge(transactionId, payload, registrar)
        val token = PendingChallenges.add(challenge)
        try {
            return startShell(token) ?: awaitOutcome(challenge)
        } finally {
            // If this call is canceled, end the challenge so a live shell closes.
            challenge.complete(ChallengeResult.Failed(BraintreeException(CHALLENGE_INTERRUPTED_MESSAGE)))
            PendingChallenges.remove(token)
        }
    }

    /** Returns a failure if the shell couldn't be started, or `null` if it was. */
    @Suppress("TooGenericExceptionCaught")
    private fun startShell(token: String): ChallengeResult.Failed? = try {
        context.startActivity(ChallengeShellActivity.createIntent(context, token))
        null
    } catch (error: RuntimeException) {
        ChallengeResult.Failed(BraintreeException(SHELL_LAUNCH_FAILED_MESSAGE, error))
    }

    private suspend fun awaitOutcome(challenge: PendingChallenge): ChallengeResult {
        // Launch handshake: startActivity returns normally even when Android blocks the launch.
        val attached = withTimeoutOrNull(handshakeTimeout) { challenge.awaitAttached() } != null
        if (!attached) {
            challenge.completeIfNotAttached(ChallengeResult.Failed(BraintreeException(CHALLENGE_NOT_SHOWN_MESSAGE)))
        }

        // Watchdog: bounds silence from the provider once the shell is up.
        if (withTimeoutOrNull(watchdogTimeout) { challenge.awaitOutcome() } == null) {
            challenge.complete(ChallengeResult.Failed(BraintreeException(CHALLENGE_TIMED_OUT_MESSAGE)))
        }
        return challenge.awaitOutcome()
    }

    companion object {
        /** How long the shell has to start before the launch counts as blocked. */
        internal val HANDSHAKE_TIMEOUT: Duration = 3.seconds

        internal const val FOREGROUND_REQUIRED_MESSAGE =
            "The 3D Secure challenge requires the app to be in the foreground."
        internal const val SHELL_LAUNCH_FAILED_MESSAGE = "The 3D Secure challenge could not be launched."
        internal const val CHALLENGE_NOT_SHOWN_MESSAGE =
            "The 3D Secure challenge did not start. The app may have moved to the background."
        internal const val CHALLENGE_TIMED_OUT_MESSAGE =
            "The 3D Secure challenge did not finish within the expected time."
        internal const val CHALLENGE_INTERRUPTED_MESSAGE = "The 3D Secure challenge was interrupted."
    }
}

/** Whether the app is on screen, so an Activity it starts is actually shown. */
internal fun interface ApplicationStateProviding {
    fun isApplicationActive(): Boolean
}

/** [ApplicationStateProviding] from this process's importance. */
internal object ActivityManagerApplicationStateProvider : ApplicationStateProviding {

    override fun isApplicationActive(): Boolean {
        val processInfo = ActivityManager.RunningAppProcessInfo()
        ActivityManager.getMyMemoryState(processInfo)
        return isActiveImportance(processInfo.importance)
    }

    /** Only foreground and visible count; a foreground service alone doesn't. */
    internal fun isActiveImportance(importance: Int): Boolean =
        importance == IMPORTANCE_FOREGROUND || importance == IMPORTANCE_VISIBLE
}
