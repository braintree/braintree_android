package com.braintreepayments.api.googlepay

import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContract
import androidx.fragment.app.FragmentActivity
import androidx.test.core.app.ApplicationProvider
import com.google.android.gms.tasks.Task
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.PaymentDataRequest
import com.google.android.gms.wallet.contract.TaskResultContracts
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
    fun constructor_createsActivityLauncher() {
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
    fun launch_launchesTask() {
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
}
