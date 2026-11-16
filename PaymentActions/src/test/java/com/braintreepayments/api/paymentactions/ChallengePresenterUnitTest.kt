package com.braintreepayments.api.paymentactions

import android.app.Activity
import android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_CACHED
import android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
import android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND_SERVICE
import android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_GONE
import android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_PERCEPTIBLE
import android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_SERVICE
import android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE
import android.content.Context
import android.content.Intent
import android.os.TransactionTooLargeException
import com.braintreepayments.api.core.BraintreeException
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ChallengePresenterUnitTest {

    private val context = mockk<Context>(relaxed = true)
    private val registrar = ChallengeRegistrar { _, _ -> ChallengeStarter { _, _ -> } }
    private var isApplicationActive = true
    private val startedTokens = mutableListOf<String>()

    private var onShellStarted: (PendingChallenge) -> Unit = {}

    @Before
    fun setUp() {
        every { context.packageName } returns "com.example.app"
        every { context.startActivity(any()) } answers {
            val token = firstArg<Intent>().getStringExtra(ChallengeShellActivity.EXTRA_TOKEN)!!
            startedTokens += token
            PendingChallenges.find(token)?.let(onShellStarted)
        }
    }

    @After
    fun tearDown() {
        startedTokens.forEach(PendingChallenges::remove)
    }

    private val sut = ChallengePresenter(
        context,
        watchdogTimeout = WATCHDOG_TIMEOUT,
        applicationStateProvider = { isApplicationActive },
    )

    private suspend fun present() = sut.present("mpi-tx-id", "challenge-payload", registrar)

    /** A shell that attaches, then reports [result] after [delay]. */
    private fun TestScope.shellReporting(result: ChallengeResult, after: Duration = Duration.ZERO) {
        onShellStarted = { challenge ->
            challenge.attach()
            launch {
                delay(after)
                challenge.complete(result)
            }
        }
    }

    @Test
    fun `present returns Failed without starting the shell when the app isn't active`() = runTest {
        isApplicationActive = false

        val result = present()

        val error = assertIs<BraintreeException>(assertIs<ChallengeResult.Failed>(result).error)
        assertEquals(ChallengePresenter.FOREGROUND_REQUIRED_MESSAGE, error.message)
        verify(exactly = 0) { context.startActivity(any()) }
    }

    @Test
    fun `present starts the shell with a token for the challenge it registered`() = runTest {
        var registered: PendingChallenge? = null
        onShellStarted = { challenge ->
            registered = challenge
            challenge.attach()
            challenge.complete(ChallengeResult.Abandoned)
        }

        present()

        val challenge = registered!!
        assertEquals("mpi-tx-id", challenge.transactionId)
        assertEquals("challenge-payload", challenge.payload)
        assertSame(registrar, challenge.registrar)
    }

    @Test
    fun `present starts the shell from the application context as a new task`() = runTest {
        shellReporting(ChallengeResult.Abandoned)

        present()

        verify {
            context.startActivity(match { intent ->
                intent.component?.className == ChallengeShellActivity::class.java.name &&
                    intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0
            })
        }
    }

    @Test
    fun `present returns the outcome the shell reports`() = runTest {
        val outcome = ChallengeResult.Completed("server-jwt")
        shellReporting(outcome, after = 1.minutes)

        val result = present()

        assertSame(outcome, result)
        assertEquals(1.minutes.inWholeMilliseconds, currentTime)
    }

    @Test
    fun `present unregisters the challenge once it ends`() = runTest {
        shellReporting(ChallengeResult.Abandoned)

        present()

        assertNull(PendingChallenges.find(startedTokens.single()))
    }

    @Test
    fun `present returns Failed when the shell doesn't attach within the handshake timeout`() = runTest {
        val result = present()

        val error = assertIs<BraintreeException>(assertIs<ChallengeResult.Failed>(result).error)
        assertEquals(ChallengePresenter.CHALLENGE_NOT_SHOWN_MESSAGE, error.message)
        assertEquals(ChallengePresenter.HANDSHAKE_TIMEOUT.inWholeMilliseconds, currentTime)
    }

    @Test
    fun `a shell that attaches after the handshake timed out is turned away`() = runTest {
        var challenge: PendingChallenge? = null
        onShellStarted = { challenge = it }

        present()

        assertFalse(challenge!!.attach())
    }

    @Test
    fun `present returns Failed when the provider stays silent past the watchdog timeout`() = runTest {
        var challenge: PendingChallenge? = null
        onShellStarted = {
            it.attach()
            challenge = it
        }

        val result = present()

        val error = assertIs<BraintreeException>(assertIs<ChallengeResult.Failed>(result).error)
        assertEquals(ChallengePresenter.CHALLENGE_TIMED_OUT_MESSAGE, error.message)
        assertEquals(WATCHDOG_TIMEOUT.inWholeMilliseconds, currentTime)
        // Ending the challenge is what closes a live shell.
        assertEquals(PendingChallenge.State.COMPLETED, challenge!!.state)
    }

    @Test
    fun `present returns Failed wrapping the cause when the shell can't be started`() = runTest {
        val cause = RuntimeException(TransactionTooLargeException())
        every { context.startActivity(any()) } throws cause

        val result = present()

        val error = assertIs<BraintreeException>(assertIs<ChallengeResult.Failed>(result).error)
        assertEquals(ChallengePresenter.SHELL_LAUNCH_FAILED_MESSAGE, error.message)
        assertSame(cause, error.cause)
    }

    @Test
    fun `canceling present ends the challenge so a live shell closes, and unregisters it`() = runTest {
        var challenge: PendingChallenge? = null
        onShellStarted = {
            it.attach()
            challenge = it
        }
        val job = launch { present() }
        runCurrent()

        job.cancel()
        runCurrent()

        assertEquals(PendingChallenge.State.COMPLETED, challenge!!.state)
        assertNull(PendingChallenges.find(startedTokens.single()))
    }

    @Test
    fun `createIntent from a non-Activity context adds FLAG_ACTIVITY_NEW_TASK`() {
        val intent = ChallengeShellActivity.createIntent(context, "token")

        assertNotEquals(0, intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK)
        assertEquals("token", intent.getStringExtra(ChallengeShellActivity.EXTRA_TOKEN))
    }

    @Test
    fun `createIntent from an Activity context starts the shell in the Activity's task`() {
        val activity = mockk<Activity>(relaxed = true) {
            every { packageName } returns "com.example.app"
        }

        val intent = ChallengeShellActivity.createIntent(activity, "token")

        assertEquals(0, intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    @Test
    fun `only foreground and visible process importances count as active`() {
        val visible = listOf(IMPORTANCE_FOREGROUND, IMPORTANCE_VISIBLE)
        val notVisible = listOf(
            IMPORTANCE_FOREGROUND_SERVICE,
            IMPORTANCE_PERCEPTIBLE,
            IMPORTANCE_SERVICE,
            IMPORTANCE_CACHED,
            IMPORTANCE_GONE,
        )

        visible.forEach { assertTrue(ActivityManagerApplicationStateProvider.isActiveImportance(it)) }
        notVisible.forEach { assertFalse(ActivityManagerApplicationStateProvider.isActiveImportance(it)) }
    }

    private companion object {
        val WATCHDOG_TIMEOUT = 6.minutes
    }
}
