package com.braintreepayments.api.googlepay

import android.content.Context
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContract
import androidx.fragment.app.FragmentActivity
import androidx.test.core.app.ApplicationProvider
import com.braintreepayments.api.core.UserCanceledException
import com.google.android.gms.common.api.Status
import com.google.android.gms.tasks.Task
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.PaymentDataRequest
import com.google.android.gms.wallet.contract.ApiTaskResult
import com.google.android.gms.wallet.contract.TaskResultContracts
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GooglePayLauncherUnitTest {

    private val activityResultLauncher = mockk<ActivityResultLauncher<Task<PaymentData>>>(relaxed = true)
    private val callback = mockk<GooglePayLauncherCallback>(relaxed = true)
    private val activityResultRegistry = mockk<ActivityResultRegistry>(relaxed = true)

    @Before
    fun beforeEach() {
        every {
            activityResultRegistry.register(
                any(),
                any(),
                any<ActivityResultContract<Task<PaymentData>, Any>>(),
                any()
            )
        } returns activityResultLauncher
    }

    @Test
    fun `when GooglePayLauncher is constructed, activity result launcher is registered`() {
        val expectedKey = "com.braintreepayments.api.GooglePay.RESULT"
        val lifecycleOwner = FragmentActivity()
        val context = ApplicationProvider.getApplicationContext<Context>()

        val registry = mockk<ActivityResultRegistry>(relaxed = true)
        GooglePayLauncher(registry, lifecycleOwner, context, callback = callback)

        verify {
            registry.register(
                eq(expectedKey), eq(lifecycleOwner),
                any<TaskResultContracts.GetPaymentDataResult>(),
                any()
            )
        }
    }

    @Test
    fun `when launch is called with ready to launch request, activity result launcher launches task`() {
        val lifecycleOwner = FragmentActivity()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val mockTask = mockk<Task<PaymentData>>(relaxed = true)
        val internalGooglePayClient = MockkGooglePayInternalClientBuilder()
            .loadPaymentDataTask(mockTask)
            .build()

        val sut = GooglePayLauncher(
            activityResultRegistry, lifecycleOwner, context, internalGooglePayClient, callback
        )

        val googlePayRequest = GooglePayRequest("USD", "1.00", GooglePayTotalPriceStatus.TOTAL_PRICE_STATUS_FINAL)
        val paymentDataRequest = PaymentDataRequest.fromJson(googlePayRequest.toJson())
        val intentData = GooglePayPaymentAuthRequestParams(1, paymentDataRequest)

        sut.launch(GooglePayPaymentAuthRequest.ReadyToLaunch(intentData))
        verify { activityResultLauncher.launch(mockTask) }
    }

    @Test
    fun `when activity result callback receives a successful status, payment data is passed to callback`() {
        val callbackSlot = slot<ActivityResultCallback<ApiTaskResult<PaymentData>>>()
        val registry = mockk<ActivityResultRegistry>(relaxed = true)
        every {
            registry.register(any(), any(), any<TaskResultContracts.GetPaymentDataResult>(), capture(callbackSlot))
        } returns mockk(relaxed = true)
        val lifecycleOwner = FragmentActivity()
        val context = ApplicationProvider.getApplicationContext<Context>()
        GooglePayLauncher(registry, lifecycleOwner, context, callback = callback)

        val paymentData = mockk<PaymentData>(relaxed = true)
        val status = mockk<Status>(relaxed = true)
        every { status.isSuccess } returns true
        val apiTaskResult = mockk<ApiTaskResult<PaymentData>>(relaxed = true)
        every { apiTaskResult.status } returns status
        every { apiTaskResult.result } returns paymentData

        callbackSlot.captured.onActivityResult(apiTaskResult)

        val resultSlot = slot<GooglePayPaymentAuthResult>()
        verify { callback.onGooglePayLauncherResult(capture(resultSlot)) }
        assertEquals(paymentData, resultSlot.captured.paymentData)
        assertNull(resultSlot.captured.error)
    }

    @Test
    fun `when activity result callback receives a canceled status, UserCanceledException is passed to callback`() {
        val callbackSlot = slot<ActivityResultCallback<ApiTaskResult<PaymentData>>>()
        val registry = mockk<ActivityResultRegistry>(relaxed = true)
        every {
            registry.register(any(), any(), any<TaskResultContracts.GetPaymentDataResult>(), capture(callbackSlot))
        } returns mockk(relaxed = true)
        val lifecycleOwner = FragmentActivity()
        val context = ApplicationProvider.getApplicationContext<Context>()
        GooglePayLauncher(registry, lifecycleOwner, context, callback = callback)

        val status = mockk<Status>(relaxed = true)
        every { status.isSuccess } returns false
        every { status.isCanceled } returns true
        val apiTaskResult = mockk<ApiTaskResult<PaymentData>>(relaxed = true)
        every { apiTaskResult.status } returns status

        callbackSlot.captured.onActivityResult(apiTaskResult)

        val resultSlot = slot<GooglePayPaymentAuthResult>()
        verify { callback.onGooglePayLauncherResult(capture(resultSlot)) }
        assertNull(resultSlot.captured.paymentData)
        assertTrue(resultSlot.captured.error is UserCanceledException)
        assertEquals("User canceled Google Pay.", resultSlot.captured.error!!.message)
    }

    @Test
    fun `when activity result callback receives an error status, GooglePayException is passed to callback`() {
        val callbackSlot = slot<ActivityResultCallback<ApiTaskResult<PaymentData>>>()
        val registry = mockk<ActivityResultRegistry>(relaxed = true)
        every {
            registry.register(any(), any(), any<TaskResultContracts.GetPaymentDataResult>(), capture(callbackSlot))
        } returns mockk(relaxed = true)
        val lifecycleOwner = FragmentActivity()
        val context = ApplicationProvider.getApplicationContext<Context>()
        GooglePayLauncher(registry, lifecycleOwner, context, callback = callback)

        val status = mockk<Status>(relaxed = true)
        every { status.isSuccess } returns false
        every { status.isCanceled } returns false
        val apiTaskResult = mockk<ApiTaskResult<PaymentData>>(relaxed = true)
        every { apiTaskResult.status } returns status

        callbackSlot.captured.onActivityResult(apiTaskResult)

        val resultSlot = slot<GooglePayPaymentAuthResult>()
        verify { callback.onGooglePayLauncherResult(capture(resultSlot)) }
        assertNull(resultSlot.captured.paymentData)
        assertTrue(resultSlot.captured.error is GooglePayException)
        assertEquals(
            "An error was encountered during the Google Pay " +
                "flow. See the status object in this exception for more details.",
            resultSlot.captured.error!!.message
        )
    }
}
