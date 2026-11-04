package com.braintreepayments.api.uicomponents

import com.braintreepayments.api.BrowserSwitchException
import com.braintreepayments.api.paypal.PayPalLauncher
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class RestorePendingRequestUnitTest {

    private val payPalLauncher = mockk<PayPalLauncher>()

    @Test
    fun `restorePendingRequest does nothing when there is no stored request`() {
        val error = restorePendingRequest(payPalLauncher, "")

        assertNull(error)
        verify(exactly = 0) { payPalLauncher.restorePendingRequest(any()) }
    }

    @Test
    fun `restorePendingRequest gives the stored request to the launcher`() {
        every { payPalLauncher.restorePendingRequest(any()) } just runs

        val error = restorePendingRequest(payPalLauncher, "pending-request")

        assertNull(error)
        verify(exactly = 1) { payPalLauncher.restorePendingRequest("pending-request") }
    }

    @Test
    fun `restorePendingRequest returns the error when the stored request cannot be restored`() {
        val restoreError = BrowserSwitchException("invalid pending request")
        every { payPalLauncher.restorePendingRequest(any()) } throws restoreError

        val error = restorePendingRequest(payPalLauncher, "pending-request")

        assertSame(restoreError, error)
    }
}
