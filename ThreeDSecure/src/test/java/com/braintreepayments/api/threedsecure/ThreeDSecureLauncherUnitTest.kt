package com.braintreepayments.api.threedsecure

import android.os.Bundle
import android.os.TransactionTooLargeException
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.app.ActivityOptionsCompat
import androidx.fragment.app.Fragment
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
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Suppress("MaxLineLength")
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
    fun `when constructed, registers an activity result launcher with the expected key`() {
        val expectedKey = "com.braintreepayments.api.ThreeDSecure.RESULT"
        val lifecycleOwner = FragmentActivity()

        val registry = mockk<ActivityResultRegistry>(relaxed = true)
        ThreeDSecureLauncher(registry, lifecycleOwner, callback = callback!!)

        verify {
            registry.register(
                eq(expectedKey), eq(lifecycleOwner),
                any<ActivityResultContract<ThreeDSecureParams, ThreeDSecurePaymentAuthResult>>(),
                any()
            )
        }
    }

    @Test
    fun `when constructed with a custom result key, registers an activity result launcher with that key`() {
        val customKey = "com.checkout.THREE_D_SECURE"
        val lifecycleOwner = FragmentActivity()

        val registry = mockk<ActivityResultRegistry>(relaxed = true)
        ThreeDSecureLauncher(registry, lifecycleOwner, customKey, callback!!)

        verify {
            registry.register(
                eq(customKey), eq(lifecycleOwner),
                any<ActivityResultContract<ThreeDSecureParams, ThreeDSecurePaymentAuthResult>>(),
                any()
            )
        }
    }

    @Test
    fun `when a default-key launcher and a custom-key launcher share a registry, they do not collide`() {
        val defaultKey = "com.braintreepayments.api.ThreeDSecure.RESULT"
        val customKey = "com.checkout.THREE_D_SECURE"
        val lifecycleOwner = FragmentActivity()
        val registry = mockk<ActivityResultRegistry>(relaxed = true)
        val registeredKeys = mutableListOf<String>()

        every {
            registry.register(
                capture(registeredKeys),
                any(),
                any<ActivityResultContract<ThreeDSecureParams?, Any>>(),
                any()
            )
        } returns activityResultLauncher

        // launcher with a default key
        ThreeDSecureLauncher(registry, lifecycleOwner, callback = callback!!)

        // launcher with the custom key
        ThreeDSecureLauncher(registry, lifecycleOwner, customKey, callback!!)

        assert(registeredKeys.size == 2)
        assert(registeredKeys[0] == defaultKey)
        assert(registeredKeys[1] == customKey)
        assert(registeredKeys[0] != registeredKeys[1])

        verify(exactly = 2) {
            registry.register(
                any(), eq(lifecycleOwner),
                any<ActivityResultContract<ThreeDSecureParams?, Any>>(),
                any()
            )
        }
    }

    @Test
    fun `when two ThreeDSecureLaunchers register with distinct fragment view lifecycle owners and distinct keys on a real registry, results route to the correct callback without collision`() {
        val activityController = Robolectric.buildActivity(FragmentActivity::class.java).setup()
        val activity = activityController.get()

        val requestCodes = mutableListOf<Int>()
        val registry = object : ActivityResultRegistry() {
            override fun <I, O> onLaunch(
                requestCode: Int,
                contract: ActivityResultContract<I, O>,
                input: I,
                options: ActivityOptionsCompat?
            ) {
                requestCodes.add(requestCode)
            }
        }

        val callback1 = mockk<ThreeDSecureLauncherCallback>(relaxed = true)
        val callback2 = mockk<ThreeDSecureLauncherCallback>(relaxed = true)

        val fragment1 = ThreeDSecureLauncherRegisteringFragment(registry, "com.checkout.THREE_D_SECURE_1", callback1)
        val fragment2 = ThreeDSecureLauncherRegisteringFragment(registry, "com.checkout.THREE_D_SECURE_2", callback2)
        activity.supportFragmentManager.beginTransaction().add(fragment1, "fragment1").commitNow()
        activity.supportFragmentManager.beginTransaction().add(fragment2, "fragment2").commitNow()

        val threeDSecureParams = ThreeDSecureParams(null, null, null)
        val paymentAuthRequest = ThreeDSecurePaymentAuthRequest.ReadyToLaunch(threeDSecureParams)

        fragment1.threeDSecureLauncher.launch(paymentAuthRequest)
        fragment2.threeDSecureLauncher.launch(paymentAuthRequest)

        assertEquals(2, requestCodes.size)
        assertTrue(requestCodes[0] != requestCodes[1])

        val result1 = ThreeDSecurePaymentAuthResult(error = BraintreeException("result 1"))
        val result2 = ThreeDSecurePaymentAuthResult(error = BraintreeException("result 2"))

        registry.dispatchResult(requestCodes[0], result1)
        registry.dispatchResult(requestCodes[1], result2)

        verify { callback1.onThreeDSecurePaymentAuthResult(result1) }
        verify { callback2.onThreeDSecurePaymentAuthResult(result2) }
    }

    @Test
    fun `when launch is called with a ready to launch request, launches the auth challenge with the three d secure params`() {
        val lifecycleOwner = FragmentActivity()
        val sut = ThreeDSecureLauncher(
            activityResultRegistry!!, lifecycleOwner,
            callback = callback!!
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
    fun `when the activity launcher throws a transaction too large error, calls back a BraintreeException`() {
        val lifecycleOwner = FragmentActivity()
        val sut = ThreeDSecureLauncher(
            activityResultRegistry!!, lifecycleOwner,
            callback = callback!!
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

/**
 * Registers a [ThreeDSecureLauncher] against its own [viewLifecycleOwner] as soon as the view is
 * created (before the Fragment reaches STARTED).
 */
internal class ThreeDSecureLauncherRegisteringFragment(
    private val registry: ActivityResultRegistry,
    private val resultKey: String,
    private val resultCallback: ThreeDSecureLauncherCallback
) : Fragment() {

    lateinit var threeDSecureLauncher: ThreeDSecureLauncher

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = View(requireContext())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        threeDSecureLauncher = ThreeDSecureLauncher(registry, viewLifecycleOwner, resultKey, resultCallback)
    }
}
