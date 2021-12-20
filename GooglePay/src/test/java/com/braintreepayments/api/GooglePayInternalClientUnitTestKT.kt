package com.braintreepayments.api

import androidx.fragment.app.FragmentActivity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Status
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wallet.IsReadyToPayRequest
import com.google.android.gms.wallet.PaymentsClient
import com.google.android.gms.wallet.Wallet
import com.google.android.gms.wallet.WalletConstants
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import junit.framework.TestCase.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.CountDownLatch


@RunWith(RobolectricTestRunner::class)
class GooglePayInternalClientUnitTestKT {

    private lateinit var activity: FragmentActivity
    private lateinit var isReadyToPayCallback: GooglePayIsReadyToPayCallback
    private lateinit var paymentsClient: PaymentsClient
    private lateinit var isReadyToPayRequest: IsReadyToPayRequest

    @Before
    fun beforeEach() {
        mockkStatic(Wallet::class)
        activity = mockk(relaxed = true)
        isReadyToPayCallback = mockk(relaxed = true)
        paymentsClient = mockk()
        isReadyToPayRequest = IsReadyToPayRequest.fromJson("{}")
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

        every { paymentsClient.isReadyToPay(isReadyToPayRequest) } answers {
            Tasks.forResult(true)
        }

        val sut = GooglePayInternalClient()
        sut.isReadyToPay(activity, configuration, isReadyToPayRequest) { isReadyToPay, error ->
            assertTrue(isReadyToPay)
            assertNull(error)
            countDownLatch.countDown()
        }

        countDownLatch.await()
    }

    @Test
    fun `isReadyToPay forwards failure result to callback`() {
        val countDownLatch = CountDownLatch(1)
        val configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_GOOGLE_PAY)
        every { Wallet.getPaymentsClient(any(), any()) } returns paymentsClient

        val expectedError = ApiException(Status.RESULT_INTERNAL_ERROR)
        every { paymentsClient.isReadyToPay(isReadyToPayRequest) } answers {
            Tasks.forException(expectedError)
        }

        val sut = GooglePayInternalClient()
        sut.isReadyToPay(activity, configuration, isReadyToPayRequest) { isReadyToPay, error ->
            assertFalse(isReadyToPay)
            assertSame(expectedError, error)
            countDownLatch.countDown()
        }
        countDownLatch.await()
    }
}