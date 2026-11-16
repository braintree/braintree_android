package com.braintreepayments.api.paymentactions

import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class PendingChallengeUnitTest {

    private val registrar = ChallengeRegistrar { _, _ -> ChallengeStarter { _, _ -> } }
    private val sut = PendingChallenge("mpi-tx-id", "challenge-payload", registrar)
    private val tokens = mutableListOf<String>()

    @After
    fun tearDown() {
        tokens.forEach(PendingChallenges::remove)
    }

    @Test
    fun `a new challenge is CREATED`() {
        assertEquals(PendingChallenge.State.CREATED, sut.state)
    }

    @Test
    fun `the first attach moves the challenge to ATTACHED and completes the handshake`() = runTest {
        assertTrue(sut.attach())

        assertEquals(PendingChallenge.State.ATTACHED, sut.state)
        sut.awaitAttached() // Returns instead of suspending forever.
    }

    @Test
    fun `attaching again, as a recreated shell does, succeeds and keeps the state`() {
        sut.attach()

        assertTrue(sut.attach())
        assertEquals(PendingChallenge.State.ATTACHED, sut.state)
    }

    @Test
    fun `attaching to a challenge that was already started succeeds and keeps it ISSUED`() {
        sut.attach()
        sut.markIssued()

        assertTrue(sut.attach())
        assertEquals(PendingChallenge.State.ISSUED, sut.state)
    }

    @Test
    fun `attaching to a challenge that already ended fails`() {
        sut.complete(ChallengeResult.Abandoned)

        assertFalse(sut.attach())
        assertEquals(PendingChallenge.State.COMPLETED, sut.state)
    }

    @Test
    fun `markIssued moves an attached challenge to ISSUED`() {
        sut.attach()

        sut.markIssued()

        assertEquals(PendingChallenge.State.ISSUED, sut.state)
    }

    @Test
    fun `markIssued does nothing before the shell attaches`() {
        sut.markIssued()

        assertEquals(PendingChallenge.State.CREATED, sut.state)
    }

    @Test
    fun `markIssued does nothing after the challenge ended`() {
        sut.attach()
        sut.complete(ChallengeResult.Abandoned)

        sut.markIssued()

        assertEquals(PendingChallenge.State.COMPLETED, sut.state)
    }

    @Test
    fun `the first complete decides the outcome`() = runTest {
        val first = ChallengeResult.Completed("server-jwt")

        assertTrue(sut.complete(first))
        assertFalse(sut.complete(ChallengeResult.Abandoned))

        assertSame(first, sut.awaitOutcome())
    }

    @Test
    fun `completeIfNotAttached ends a challenge the shell never attached to`() = runTest {
        val timedOut = ChallengeResult.Failed(IllegalStateException("handshake timed out"))

        assertTrue(sut.completeIfNotAttached(timedOut))

        assertSame(timedOut, sut.awaitOutcome())
        assertFalse(sut.attach())
    }

    @Test
    fun `completeIfNotAttached leaves a challenge the shell attached to running`() {
        sut.attach()

        assertFalse(sut.completeIfNotAttached(ChallengeResult.Abandoned))

        assertEquals(PendingChallenge.State.ATTACHED, sut.state)
    }

    @Test
    fun `PendingChallenges finds a challenge by the token add returned`() {
        val token = PendingChallenges.add(sut).also(tokens::add)

        assertSame(sut, PendingChallenges.find(token))
    }

    @Test
    fun `PendingChallenges returns a distinct token for each challenge`() {
        val first = PendingChallenges.add(sut).also(tokens::add)
        val second = PendingChallenges.add(PendingChallenge("tx", "payload", registrar)).also(tokens::add)

        assertNotEquals(first, second)
    }

    @Test
    fun `PendingChallenges finds nothing for an unknown or missing token`() {
        assertNull(PendingChallenges.find("unknown-token"))
        assertNull(PendingChallenges.find(null))
    }

    @Test
    fun `PendingChallenges finds nothing for a removed token`() {
        val token = PendingChallenges.add(sut)

        PendingChallenges.remove(token)

        assertNull(PendingChallenges.find(token))
    }
}
