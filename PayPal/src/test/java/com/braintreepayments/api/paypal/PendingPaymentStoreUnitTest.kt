package com.braintreepayments.api.paypal

import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.Callable
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class PendingPaymentStoreUnitTest {

    private lateinit var sut: PendingPaymentStore

    @Before
    fun setup() {
        sut = PendingPaymentStore()
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

    @Test
    fun `getOrCreateDeferred returns same deferred and exactly one initiator under concurrent access`() {
        val executor = Executors.newFixedThreadPool(2)
        try {
            repeat(500) {
                val store = PendingPaymentStore()
                val barrier = CyclicBarrier(2)

                val task = Callable {
                    barrier.await()
                    store.getOrCreateDeferred()
                }

                val future1 = executor.submit(task)
                val future2 = executor.submit(task)

                val result1 = future1.get(5, TimeUnit.SECONDS)
                val result2 = future2.get(5, TimeUnit.SECONDS)

                assertSame(result1.first, result2.first)
                assertTrue(result1.second != result2.second)
                assertTrue(result1.second || result2.second)
            }
        } finally {
            executor.shutdown()
        }
    }
}
