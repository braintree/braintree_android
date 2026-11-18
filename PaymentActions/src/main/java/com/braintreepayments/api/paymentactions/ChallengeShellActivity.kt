package com.braintreepayments.api.paymentactions

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.braintreepayments.api.core.BraintreeException
import kotlinx.coroutines.launch

/**
 * Invisible Activity that hosts Cardinal's challenge, which needs an Activity that `handleNextAction`
 * doesn't have. The intent carries only a token; the challenge data stays in [PendingChallenges].
 */
internal class ChallengeShellActivity : AppCompatActivity() {

    private var pendingChallenge: PendingChallenge? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val challenge = PendingChallenges.find(intent.getStringExtra(EXTRA_TOKEN))
        if (challenge == null || !challenge.attach()) {
            // The process died, the handshake timed out, or the challenge already ended.
            finish()
            return
        }
        pendingChallenge = challenge

        // Registered on every onCreate so a result that arrives after a recreation reaches this instance.
        val starter = challenge.registrar.register(this) { result -> challenge.complete(result) }

        // Close the shell however the challenge ends.
        lifecycleScope.launch {
            challenge.awaitOutcome()
            finish()
        }

        if (challenge.state == PendingChallenge.State.ATTACHED) {
            // Runs after onCreate returns, as in the legacy ThreeDSecureActivity.
            Handler(Looper.getMainLooper()).post { startChallenge(challenge, starter) }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun startChallenge(challenge: PendingChallenge, starter: ChallengeStarter) {
        // Skip if the shell closed or was recreated meanwhile; a recreated shell posts its own start.
        if (isFinishing || isDestroyed) return
        // Claim the start before calling the provider, so a challenge that ended meanwhile (e.g. canceled
        // on another thread) is never shown.
        if (!challenge.tryIssue()) return
        try {
            starter.start(challenge.transactionId, challenge.payload)
        } catch (error: RuntimeException) {
            challenge.complete(ChallengeResult.Failed(BraintreeException(CHALLENGE_START_FAILED_MESSAGE, error)))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Finishing without a verdict (back press, task cleared) means the customer left.
        if (isFinishing && !isChangingConfigurations) {
            pendingChallenge?.complete(ChallengeResult.Abandoned)
        }
    }

    companion object {
        internal const val EXTRA_TOKEN = "com.braintreepayments.api.paymentactions.ChallengeShellActivity.EXTRA_TOKEN"
        internal const val CHALLENGE_START_FAILED_MESSAGE = "The 3D Secure provider could not start the challenge."

        /**
         * Builds the shell intent. A non-Activity context needs [Intent.FLAG_ACTIVITY_NEW_TASK]; the shell
         * still opens on top of the app's existing task.
         */
        internal fun createIntent(context: Context, token: String): Intent =
            Intent(context, ChallengeShellActivity::class.java)
                .putExtra(EXTRA_TOKEN, token)
                .apply { if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
    }
}
