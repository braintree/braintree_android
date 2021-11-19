package com.braintreepayments.api

import androidx.fragment.app.FragmentActivity
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wallet.IsReadyToPayRequest
import com.google.android.gms.wallet.PaymentsClient
import com.google.android.gms.wallet.Wallet
import com.google.android.gms.wallet.WalletConstants
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import junit.framework.TestCase.assertEquals
import org.junit.Before
import org.junit.Test

class GoolgePayInternalClientUnitTestKT {

    private val activity = mockk<FragmentActivity>(relaxed = true)
    private val isReadyToPayCallback = mockk<GooglePayIsReadyToPayCallback>(relaxed = true)
    private val paymentsClient = mockk<PaymentsClient>(relaxed = true)
    private val isReadyToPayRequest = IsReadyToPayRequest.fromJson("{}")

    @Before
    fun beforeEach() {
        mockkStatic(Wallet::class)
    }

    @Test
    fun `isReadyToPay requests Sandbox Wallet environment when configuration environment is Sandbox`() {
        val configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_GOOGLE_PAY)
        every { paymentsClient.isReadyToPay(isReadyToPayRequest) } returns Tasks.forResult(true)

        val walletOptionsSlot = slot<Wallet.WalletOptions>()
        every { Wallet.getPaymentsClient(any(), capture(walletOptionsSlot)) } returns paymentsClient

        val sut = GooglePayInternalClient()
        sut.isReadyToPay(activity, configuration, isReadyToPayRequest, isReadyToPayCallback)

        assertEquals(WalletConstants.ENVIRONMENT_TEST, walletOptionsSlot.captured.environment)
    }
}