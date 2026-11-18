package com.braintreepayments.api.paymentactions

import android.content.Intent
import android.os.Looper
import androidx.fragment.app.FragmentActivity
import androidx.test.core.app.ApplicationProvider
import com.braintreepayments.api.core.BraintreeException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class ChallengeShellActivityUnitTest {

    private val registrar = FakeChallengeRegistrar()
    private val tokens = mutableListOf<String>()

    @After
    fun tearDown() {
        tokens.forEach(PendingChallenges::remove)
    }

    private fun pendingChallenge() = PendingChallenge("mpi-tx-id", "challenge-payload", registrar)

    private fun register(challenge: PendingChallenge): String = PendingChallenges.add(challenge).also(tokens::add)

    private fun shell(token: String?): ActivityController<ChallengeShellActivity> {
        val intent = Intent(ApplicationProvider.getApplicationContext(), ChallengeShellActivity::class.java)
        token?.let { intent.putExtra(ChallengeShellActivity.EXTRA_TOKEN, it) }
        return Robolectric.buildActivity(ChallengeShellActivity::class.java, intent)
    }

    private fun idleMainLooper() = shadowOf(Looper.getMainLooper()).idle()

    private fun PendingChallenge.outcome(): ChallengeResult = runBlocking { withTimeout(1_000) { awaitOutcome() } }

    @Test
    fun `a shell with an unknown token finishes without touching the provider`() {
        val controller = shell("unknown-token").create()

        assertTrue(controller.get().isFinishing)
        assertTrue(registrar.registeredActivities.isEmpty())
    }

    @Test
    fun `a shell without a token finishes without touching the provider`() {
        val controller = shell(token = null).create()

        assertTrue(controller.get().isFinishing)
        assertTrue(registrar.registeredActivities.isEmpty())
    }

    @Test
    fun `a shell for a challenge that already ended finishes without touching the provider`() {
        val challenge = pendingChallenge()
        challenge.complete(ChallengeResult.Failed(BraintreeException("handshake timed out")))

        val controller = shell(register(challenge)).create()

        assertTrue(controller.get().isFinishing)
        assertTrue(registrar.registeredActivities.isEmpty())
    }

    @Test
    fun `a new shell attaches, registers on itself, and starts the challenge once`() = runBlocking {
        val challenge = pendingChallenge()

        val controller = shell(register(challenge)).setup()
        idleMainLooper()

        challenge.awaitAttached()
        assertEquals(listOf<FragmentActivity>(controller.get()), registrar.registeredActivities)
        assertEquals(listOf("mpi-tx-id" to "challenge-payload"), registrar.starts)
        assertEquals(PendingChallenge.State.ISSUED, challenge.state)
        assertFalse(controller.get().isFinishing)
    }

    @Test
    fun `a recreated shell registers again but never starts the challenge again`() {
        val challenge = pendingChallenge()
        val controller = shell(register(challenge)).setup()
        idleMainLooper()

        controller.recreate()
        idleMainLooper()

        assertEquals(2, registrar.registeredActivities.size)
        assertSame(controller.get(), registrar.registeredActivities.last())
        assertEquals(1, registrar.starts.size)
        // A configuration change isn't the customer leaving.
        assertEquals(PendingChallenge.State.ISSUED, challenge.state)
    }

    @Test
    fun `a shell attaching to a challenge that was attached but never started starts it`() {
        val challenge = pendingChallenge()
        // A previous shell attached, then was destroyed before its posted start ran.
        challenge.attach()

        shell(register(challenge)).setup()
        idleMainLooper()

        assertEquals(1, registrar.starts.size)
        assertEquals(PendingChallenge.State.ISSUED, challenge.state)
    }

    @Test
    fun `the provider's verdict ends the challenge and closes the shell`() {
        val challenge = pendingChallenge()
        val controller = shell(register(challenge)).setup()
        idleMainLooper()

        registrar.deliver(ChallengeResult.Completed("server-jwt"))
        idleMainLooper()

        assertEquals(ChallengeResult.Completed("server-jwt"), challenge.outcome())
        assertTrue(controller.get().isFinishing)
    }

    @Test
    fun `a verdict delivered on a background thread closes the shell`() {
        val challenge = pendingChallenge()
        val controller = shell(register(challenge)).setup()
        idleMainLooper()

        Thread { registrar.deliver(ChallengeResult.Completed("server-jwt")) }.apply {
            start()
            join()
        }
        idleMainLooper()

        assertEquals(ChallengeResult.Completed("server-jwt"), challenge.outcome())
        assertTrue(controller.get().isFinishing)
    }

    @Test
    fun `the shell closes when the challenge ends elsewhere, such as the presenter's watchdog`() {
        val challenge = pendingChallenge()
        val controller = shell(register(challenge)).setup()
        idleMainLooper()

        challenge.complete(ChallengeResult.Failed(BraintreeException("timed out")))
        idleMainLooper()

        assertTrue(controller.get().isFinishing)
    }

    @Test
    fun `finishing the shell before a verdict reports the challenge abandoned`() {
        val challenge = pendingChallenge()
        val controller = shell(register(challenge)).setup()
        idleMainLooper()

        controller.get().finish()
        controller.pause().stop().destroy()

        assertSame(ChallengeResult.Abandoned, challenge.outcome())
    }

    @Test
    fun `a back press before the challenge starts reports it abandoned without starting it`() {
        val challenge = pendingChallenge()
        val controller = shell(register(challenge)).create()

        // The start is posted to run after onCreate; the back press lands first.
        controller.get().onBackPressedDispatcher.onBackPressed()
        idleMainLooper()
        controller.destroy()

        assertTrue(registrar.starts.isEmpty())
        assertSame(ChallengeResult.Abandoned, challenge.outcome())
    }

    @Test
    fun `a provider that can't start the challenge ends it as Failed and closes the shell`() {
        val cause = IllegalStateException("invalid transaction id")
        registrar.startError = cause
        val challenge = pendingChallenge()
        val controller = shell(register(challenge)).setup()

        idleMainLooper()

        val error = assertIs<BraintreeException>(assertIs<ChallengeResult.Failed>(challenge.outcome()).error)
        assertEquals(ChallengeShellActivity.CHALLENGE_START_FAILED_MESSAGE, error.message)
        assertSame(cause, error.cause)
        assertTrue(controller.get().isFinishing)
    }

    @Test
    fun `the shell claims the challenge before starting it`() {
        val challenge = pendingChallenge()
        var stateWhenStarted: PendingChallenge.State? = null
        registrar.onStart = { stateWhenStarted = challenge.state }

        shell(register(challenge)).setup()
        idleMainLooper()

        // Already ISSUED when the provider is called: a concurrent complete() either wins before the
        // claim, and the start is skipped, or lands after it, never between the check and the start.
        assertEquals(PendingChallenge.State.ISSUED, stateWhenStarted)
    }

    @Test
    fun `a challenge that ends on another thread before the posted start runs is never started`() {
        val challenge = pendingChallenge()
        val controller = shell(register(challenge)).create()

        // For example, handleNextAction canceled on a background dispatcher right after the shell attached.
        Thread { challenge.complete(ChallengeResult.Failed(BraintreeException("interrupted"))) }.apply {
            start()
            join()
        }
        idleMainLooper()

        assertTrue(registrar.starts.isEmpty())
        assertTrue(controller.get().isFinishing)
    }

    /**
     * A [ChallengeRegistrar] that records what the shell does and lets a test deliver the provider's verdict.
     */
    private class FakeChallengeRegistrar : ChallengeRegistrar {
        val registeredActivities = mutableListOf<FragmentActivity>()
        val starts = mutableListOf<Pair<String, String>>()
        var startError: RuntimeException? = null
        var onStart: (() -> Unit)? = null
        private var onResult: ((ChallengeResult) -> Unit)? = null

        override fun register(activity: FragmentActivity, onResult: (ChallengeResult) -> Unit): ChallengeStarter {
            registeredActivities += activity
            this.onResult = onResult
            return ChallengeStarter { transactionId, payload ->
                onStart?.invoke()
                startError?.let { throw it }
                starts += transactionId to payload
            }
        }

        fun deliver(result: ChallengeResult) {
            onResult!!.invoke(result)
        }
    }
}
