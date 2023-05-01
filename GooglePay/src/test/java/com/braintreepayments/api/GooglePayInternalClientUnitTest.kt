package com.braintreepayments.api

import android.app.Activity
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Status
import com.google.android.gms.tasks.*
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
import java.util.concurrent.Executor

@RunWith(RobolectricTestRunner::class)
class GooglePayInternalClientUnitTest {

    // Ref: https://stackoverflow.com/a/75834762
    abstract class MockBooleanTask : Task<Boolean>() {
        override fun addOnFailureListener(p0: OnFailureListener): Task<Boolean> = this
        override fun addOnFailureListener(p0: Activity, p1: OnFailureListener): Task<Boolean> = this
        override fun addOnFailureListener(p0: Executor, p1: OnFailureListener): Task<Boolean> = this
        override fun getException(): Exception? = null
        override fun isCanceled(): Boolean = false
        override fun isComplete(): Boolean = true
        override fun isSuccessful(): Boolean = true

        override fun addOnSuccessListener(
            p0: Executor,
            p1: OnSuccessListener<in Boolean>
        ): Task<Boolean> = this

        override fun addOnSuccessListener(
            p0: Activity,
            p1: OnSuccessListener<in Boolean>
        ): Task<Boolean> = this

        override fun addOnSuccessListener(p0: OnSuccessListener<in Boolean>): Task<Boolean> = this

        override fun addOnCompleteListener(p0: OnCompleteListener<Boolean>): Task<Boolean> {
            p0.onComplete(this)
            return this
        }
    }

    class SuccessfulBooleanTask(private val result: Boolean): MockBooleanTask() {
        override fun getResult(): Boolean = result
        override fun <X : Throwable?> getResult(p0: Class<X>): Boolean = result
    }

    class FailingBooleanTask(private val apiException: ApiException): MockBooleanTask() {
        override fun getResult(): Boolean = false

        override fun <X : Throwable?> getResult(p0: Class<X>): Boolean {
            throw apiException
        }
    }

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
        val configuration =
            Configuration.fromJson(Fixtures.CONFIGURATION_WITH_GOOGLE_PAY_PRODUCTION)
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

        every { paymentsClient.isReadyToPay(isReadyToPayRequest) } returns SuccessfulBooleanTask(true)

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
        val failedTask: Task<Boolean> = FailingBooleanTask(expectedError)
        every { paymentsClient.isReadyToPay(isReadyToPayRequest) } returns failedTask

        val sut = GooglePayInternalClient()
        sut.isReadyToPay(activity, configuration, isReadyToPayRequest) { isReadyToPay, error ->
            assertFalse(isReadyToPay)
            assertSame(expectedError, error)
            countDownLatch.countDown()
        }
        countDownLatch.await()
    }
}