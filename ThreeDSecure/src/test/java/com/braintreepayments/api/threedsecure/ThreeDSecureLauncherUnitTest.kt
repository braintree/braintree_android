package com.braintreepayments.api.threedsecure

import android.os.TransactionTooLargeException
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContract
import androidx.fragment.app.FragmentActivity
import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.testutils.Fixtures
import com.braintreepayments.api.threedsecure.ThreeDSecureParams.Companion.fromJson
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.json.JSONException
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ThreeDSecureLauncherUnitTest {

    private var activityResultLauncher: ActivityResultLauncher<ThreeDSecureParams?> =
        mockk<ActivityResultLauncher<ThreeDSecureParams?>>(relaxed = true)
    private var callback: ThreeDSecureLauncherCallback? =
        mockk<ThreeDSecureLauncherCallback>(relaxed = true)
    private var activityResultRegistry: ActivityResultRegistry? =
        mockk<ActivityResultRegistry>(relaxed = true)

    @Before
    fun beforeEach() {
        every {
            activityResultRegistry?.register(
                any(),
                any(),
                any<ActivityResultContract<ThreeDSecureParams?, Any>>(),
                any()
            )
        } returns activityResultLauncher
    }

    @Test
    fun constructor_createsActivityLauncher() {
        val expectedKey = "com.braintreepayments.api.ThreeDSecure.RESULT"
        val lifecycleOwner = FragmentActivity()

        val registry = mockk<ActivityResultRegistry>(relaxed = true)
        ThreeDSecureLauncher(registry, lifecycleOwner, callback!!)

        verify {
            registry.register(
                eq(expectedKey), eq(lifecycleOwner),
                any<ActivityResultContract<ThreeDSecureParams, ThreeDSecurePaymentAuthResult>>(),
                any()
            )
        }
    }

    @Test
    fun launch_launchesAuthChallenge() {
        val lifecycleOwner = FragmentActivity()
        val sut = ThreeDSecureLauncher(
            activityResultRegistry!!, lifecycleOwner,
            callback!!
        )
        sut.activityLauncher = activityResultLauncher

        val threeDSecureParams = ThreeDSecureParams(null, null, null)
        val paymentAuthRequest = ThreeDSecurePaymentAuthRequest.ReadyToLaunch(
            threeDSecureParams
        )

        sut.launch(paymentAuthRequest)
        verify { activityResultLauncher.launch(threeDSecureParams) }
    }

    @Test
    @Throws(JSONException::class)
    fun launch_whenTransactionTooLarge_callsBackError() {
        val lifecycleOwner = FragmentActivity()
        val sut = ThreeDSecureLauncher(
            activityResultRegistry!!, lifecycleOwner,
            callback!!
        )
        sut.activityLauncher = activityResultLauncher

        val threeDSecureParams =
            fromJson(Fixtures.THREE_D_SECURE_V2_LOOKUP_RESPONSE)
        val paymentAuthRequest = ThreeDSecurePaymentAuthRequest.ReadyToLaunch(
            threeDSecureParams
        )

        val transactionTooLargeException =
            TransactionTooLargeException()
        val runtimeException = RuntimeException(
            "runtime exception caused by transaction too large", transactionTooLargeException
        )

        every { activityResultLauncher.launch(any()) } throws runtimeException

        sut.launch(paymentAuthRequest)

        val captor = slot<ThreeDSecurePaymentAuthResult>()
        verify { callback!!.onThreeDSecurePaymentAuthResult(capture(captor)) }

        val exception = captor.captured.error
        assert(exception is BraintreeException)
        val expectedMessage = ("The 3D Secure response returned is too large to continue. " +
                "Please contact Braintree Support for assistance.")
        assert(expectedMessage == exception!!.message)
    }
}
