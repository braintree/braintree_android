package com.braintreepayments.api.paypal

import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PendingPaymentStoreUnitTest {

    private lateinit var sut: PendingPaymentStore

    @Before
    fun setup() {
        sut = PendingPaymentStore()
    }

    @Test
    fun `initial state is IDLE`() {
        assertEquals(PendingPaymentStore.State.IDLE, sut.state)
    }

    @Test
    fun `initial pendingSession is null`() {
        assertNull(sut.pendingSession)
    }

    @Test
    fun `initial autoLinkNonce is null`() {
        assertNull(sut.autoLinkNonce)
    }

    @Test
    fun `PendingSession isExpired returns false when within TTL`() {
        val session = PendingPaymentStore.PendingSession(
            baToken = "BA-123",
            clientMetadataId = "metadata-id",
            merchantAccountId = null,
            intent = null,
            paymentType = "billing-agreement",
            timestampMs = System.currentTimeMillis(),
            ttlMs = PendingPaymentStore.TTL_MS
        )
        assertFalse(session.isExpired())
    }

    @Test
    fun `PendingSession isExpired returns true when past TTL`() {
        val session = PendingPaymentStore.PendingSession(
            baToken = "BA-123",
            clientMetadataId = null,
            merchantAccountId = null,
            intent = null,
            paymentType = "billing-agreement",
            timestampMs = System.currentTimeMillis() - PendingPaymentStore.TTL_MS - 1,
            ttlMs = PendingPaymentStore.TTL_MS
        )
        assertTrue(session.isExpired())
    }

    @Test
    fun `getOrCreateDeferred creates new deferred on first call`() {
        val (deferred, isInitiator) = sut.getOrCreateDeferred()
        assertTrue(isInitiator)
        assertSame(deferred, sut.tokenizeDeferred)
    }

    @Test
    fun `getOrCreateDeferred returns existing deferred on second call`() {
        val (first, isFirstInitiator) = sut.getOrCreateDeferred()
        val (second, isSecondInitiator) = sut.getOrCreateDeferred()

        assertTrue(isFirstInitiator)
        assertFalse(isSecondInitiator)
        assertSame(first, second)
    }

    @Test
    fun `clear resets all state`() {
        val nonce = mockk<PayPalAccountNonce>()
        sut.state = PendingPaymentStore.State.AWAITING_RETURN
        sut.pendingSession = PendingPaymentStore.PendingSession(
            baToken = "BA-123",
            clientMetadataId = null,
            merchantAccountId = null,
            intent = null,
            paymentType = "billing-agreement"
        )
        sut.tokenizeDeferred = CompletableDeferred()
        sut.autoLinkNonce = nonce

        sut.clear()

        assertEquals(PendingPaymentStore.State.IDLE, sut.state)
        assertNull(sut.pendingSession)
        assertNull(sut.tokenizeDeferred)
        assertNull(sut.autoLinkNonce)
    }

    @Test
    fun `clear cancels active deferred`() {
        val deferred = CompletableDeferred<PayPalAccountNonce>()
        sut.tokenizeDeferred = deferred

        sut.clear()

        assertTrue(deferred.isCancelled)
    }

    @Test
    fun `getOrCreateDeferred creates fresh deferred after clear`() {
        val (first, _) = sut.getOrCreateDeferred()
        sut.clear()
        val (second, isInitiator) = sut.getOrCreateDeferred()

        assertTrue(isInitiator)
        assertFalse(first === second)
    }
}
