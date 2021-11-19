package com.braintreepayments.api

import androidx.fragment.app.FragmentActivity
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wallet.IsReadyToPayRequest
import com.google.android.gms.wallet.PaymentsClient
import com.google.android.gms.wallet.Wallet
import com.google.android.gms.wallet.WalletConstants
import io.mockk.*
import junit.framework.TestCase.*
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch

class GoolgePayInternalClientUnitTestKT {

    private val activity = mockk<FragmentActivity>(relaxed = true)
    private val isReadyToPayCallback = mockk<GooglePayIsReadyToPayCallback>(relaxed = true)
    private val paymentsClient = mockk<PaymentsClient>()
    private val isReadyToPayRequest = IsReadyToPayRequest.fromJson("{}")

    @Before
    fun beforeEach() {
        mockkStatic(Wallet::class)
    }

    @Test
    fun `isReadyToPay requests Test Wallet environment when configuration environment is Sandbox`() {
        val configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_GOOGLE_PAY)
        every { paymentsClient.isReadyToPay(isReadyToPayRequest) } returns Tasks.forResult(true)

        val walletOptionsSlot = slot<Wallet.WalletOptions>()
        every { Wallet.getPaymentsClient(any(), capture(walletOptionsSlot)) } returns paymentsClient

        val sut = GooglePayInternalClient()
        sut.isReadyToPay(activity, configuration, isReadyToPayRequest, isReadyToPayCallback)

        assertEquals(WalletConstants.ENVIRONMENT_TEST, walletOptionsSlot.captured.environment)
    }

    @Test
    fun `isReadyToPay requests Production Wallet environment when configuration environment is Production`() {
        val configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_GOOGLE_PAY_PRODUCTION)
        every { paymentsClient.isReadyToPay(isReadyToPayRequest) } returns Tasks.forResult(true)

        val walletOptionsSlot = slot<Wallet.WalletOptions>()
        every { Wallet.getPaymentsClient(any(), capture(walletOptionsSlot)) } returns paymentsClient

        val sut = GooglePayInternalClient()
        sut.isReadyToPay(activity, configuration, isReadyToPayRequest, isReadyToPayCallback)

        assertEquals(WalletConstants.ENVIRONMENT_PRODUCTION, walletOptionsSlot.captured.environment)
    }

    @Test
    fun `isReadyToPay forwards success result to callback`() {
        val countDownLatch = CountDownLatch(1)
        val configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_GOOGLE_PAY)
        every { Wallet.getPaymentsClient(any(), any()) } returns paymentsClient

        // TODO: determine why callback is not being fired here
        coEvery { paymentsClient.isReadyToPay(isReadyToPayRequest) } coAnswers {
            Tasks.forResult(true)
        }

        val sut = GooglePayInternalClient()
        sut.isReadyToPay(activity, configuration, isReadyToPayRequest) { isReadyToPay, error ->
            assertFalse(isReadyToPay)
            assertNull(error)
            countDownLatch.countDown()
        }

        countDownLatch.await()
    }
}