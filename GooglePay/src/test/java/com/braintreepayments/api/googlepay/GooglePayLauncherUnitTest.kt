package com.braintreepayments.api.googlepay

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContract
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.wallet.PaymentDataRequest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GooglePayLauncherUnitTest {

    private val activityResultLauncher =
        mockk<ActivityResultLauncher<GooglePayPaymentAuthRequestParams>>(relaxed = true)
    private val callback = mockk<GooglePayLauncherCallback>(relaxed = true)
    private val activityResultRegistry = mockk<ActivityResultRegistry>(relaxed = true)

    @Before
    fun beforeEach() {
        every {
            activityResultRegistry.register(
                any(),
                any(),
                any<ActivityResultContract<GooglePayPaymentAuthRequestParams, Any>>(),
                any()
            )
        } returns activityResultLauncher
    }

    @Test
    fun constructor_createsActivityLauncher() {
        val expectedKey = "com.braintreepayments.api.GooglePay.RESULT"
        val lifecycleOwner = FragmentActivity()

        val registry = mockk<ActivityResultRegistry>(relaxed = true)
        GooglePayLauncher(registry, lifecycleOwner, callback)

        verify {
            registry.register(
                eq(expectedKey), eq(lifecycleOwner),
                any<ActivityResultContract<GooglePayPaymentAuthRequestParams, GooglePayPaymentAuthResult>>(),
                any()
            )
        }
    }

    @Test
    fun launch_launchesActivity() {
        val lifecycleOwner = FragmentActivity()
        val sut = GooglePayLauncher(
            activityResultRegistry, lifecycleOwner,
            callback
        )

        val googlePayRequest = GooglePayRequest("USD", "1.00", GooglePayTotalPriceStatus.TOTAL_PRICE_STATUS_FINAL)
        val paymentDataRequest = PaymentDataRequest.fromJson(googlePayRequest.toJson())
        val intentData = GooglePayPaymentAuthRequestParams(1, paymentDataRequest)

        sut.launch(GooglePayPaymentAuthRequest.ReadyToLaunch(intentData))
        verify { activityResultLauncher.launch(intentData) }
    }
}
