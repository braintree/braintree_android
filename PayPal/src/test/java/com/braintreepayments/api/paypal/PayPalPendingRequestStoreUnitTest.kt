package com.braintreepayments.api.paypal

import com.braintreepayments.api.paypal.PayPalPendingRequestStore.Companion.PENDING_REQUEST_KEY
import com.braintreepayments.api.paypal.PayPalPendingRequestStore.Companion.TIMESTAMP_KEY
import com.braintreepayments.api.paypal.PayPalPendingRequestStore.Companion.TIME_TO_LIVE
import com.braintreepayments.api.sharedutils.BraintreeSharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class PayPalPendingRequestStoreUnitTest {

    private val sharedPreferences: BraintreeSharedPreferences = mockk(relaxed = true)
    private val now = 1_000_000_000L
    private lateinit var sut: PayPalPendingRequestStore

    @Before
    fun setup() {
        sut = PayPalPendingRequestStore(sharedPreferences) { now }
    }

    @Test
    fun `save stores the pending request with the current timestamp`() {
        sut.save("pending")

        verify { sharedPreferences.putStringAndLong(PENDING_REQUEST_KEY, "pending", TIMESTAMP_KEY, now) }
    }

    @Test
    fun `get returns the pending request when it has not expired`() {
        every { sharedPreferences.getString(PENDING_REQUEST_KEY, null) } returns "pending"
        every { sharedPreferences.getLong(TIMESTAMP_KEY) } returns now - TIME_TO_LIVE + 1

        assertEquals("pending", sut.get())
    }

    @Test
    fun `get returns null and clears when the pending request has expired`() {
        every { sharedPreferences.getString(PENDING_REQUEST_KEY, null) } returns "pending"
        every { sharedPreferences.getLong(TIMESTAMP_KEY) } returns now - TIME_TO_LIVE - 1

        assertNull(sut.get())
        verify { sharedPreferences.remove(PENDING_REQUEST_KEY, TIMESTAMP_KEY) }
    }

    @Test
    fun `get returns null and clears when the timestamp is in the future`() {
        every { sharedPreferences.getString(PENDING_REQUEST_KEY, null) } returns "pending"
        every { sharedPreferences.getLong(TIMESTAMP_KEY) } returns now + 1

        assertNull(sut.get())
        verify { sharedPreferences.remove(PENDING_REQUEST_KEY, TIMESTAMP_KEY) }
    }

    @Test
    fun `get returns null when nothing is stored`() {
        every { sharedPreferences.getString(PENDING_REQUEST_KEY, null) } returns null

        assertNull(sut.get())
    }

    @Test
    fun `clear removes the pending request and timestamp`() {
        sut.clear()

        verify { sharedPreferences.remove(PENDING_REQUEST_KEY, TIMESTAMP_KEY) }
    }
}
