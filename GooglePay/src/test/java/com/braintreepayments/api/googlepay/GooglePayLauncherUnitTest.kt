package com.braintreepayments.api.googlepay

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.app.ActivityOptionsCompat
import androidx.fragment.app.Fragment
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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

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
    fun `when GooglePayLauncher is constructed via the Compose constructor, activity result launcher is registered`() {
        val expectedKey = "com.braintreepayments.api.GooglePay.RESULT"
        val lifecycleOwner = FragmentActivity()
        val context = ApplicationProvider.getApplicationContext<Context>()

        val registry = mockk<ActivityResultRegistry>(relaxed = true)
        GooglePayLauncher(registry, lifecycleOwner, context, callback)

        verify {
            registry.register(
                eq(expectedKey), eq(lifecycleOwner),
                any<TaskResultContracts.GetPaymentDataResult>(),
                any()
            )
        }
    }

    @Test
    fun `when GooglePayLauncher is constructed with a custom result key, that key is used to register`() {
        val customKey = "com.checkout.GOOGLE_PAY"
        val lifecycleOwner = FragmentActivity()
        val context = ApplicationProvider.getApplicationContext<Context>()

        val registry = mockk<ActivityResultRegistry>(relaxed = true)
        GooglePayLauncher(registry, lifecycleOwner, context, customKey, callback)

        verify {
            registry.register(
                eq(customKey), eq(lifecycleOwner),
                any<TaskResultContracts.GetPaymentDataResult>(),
                any()
            )
        }
    }

    @Test
    fun `when a default-key launcher and a custom-key launcher share a registry, they do not collide`() {
        val defaultKey = "com.braintreepayments.api.GooglePay.RESULT"
        val customKey = "com.checkout.GOOGLE_PAY"
        val lifecycleOwner = FragmentActivity()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val registry = mockk<ActivityResultRegistry>(relaxed = true)
        val registeredKeys = mutableListOf<String>()

        every {
            registry.register(
                capture(registeredKeys),
                any(),
                any<TaskResultContracts.GetPaymentDataResult>(),
                any()
            )
        } returns activityResultLauncher

        // launcher with a default key
        GooglePayLauncher(registry, lifecycleOwner, context, callback = callback)

        // launcher with the custom key
        GooglePayLauncher(registry, lifecycleOwner, context, customKey, callback)

        assertEquals(2, registeredKeys.size)
        assertEquals(defaultKey, registeredKeys[0])
        assertEquals(customKey, registeredKeys[1])
        assertTrue(registeredKeys[0] != registeredKeys[1])

        verify(exactly = 2) {
            registry.register(
                any(), eq(lifecycleOwner),
                any<TaskResultContracts.GetPaymentDataResult>(),
                any()
            )
        }
    }

    @Test
    fun `when two launchers register with distinct fragment lifecycle owners and keys, results do not collide`() {
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

        val callback1 = mockk<GooglePayLauncherCallback>(relaxed = true)
        val callback2 = mockk<GooglePayLauncherCallback>(relaxed = true)
        val mockTask1 = mockk<Task<PaymentData>>(relaxed = true)
        val mockTask2 = mockk<Task<PaymentData>>(relaxed = true)
        val internalClient1 = MockkGooglePayInternalClientBuilder().loadPaymentDataTask(mockTask1).build()
        val internalClient2 = MockkGooglePayInternalClientBuilder().loadPaymentDataTask(mockTask2).build()

        val fragment1 = GooglePayLauncherRegisteringFragment(
            registry, "com.checkout.GOOGLE_PAY_1", internalClient1, callback1
        )
        val fragment2 = GooglePayLauncherRegisteringFragment(
            registry, "com.checkout.GOOGLE_PAY_2", internalClient2, callback2
        )
        activity.supportFragmentManager.beginTransaction().add(fragment1, "fragment1").commitNow()
        activity.supportFragmentManager.beginTransaction().add(fragment2, "fragment2").commitNow()

        val googlePayRequest = GooglePayRequest("USD", "1.00", GooglePayTotalPriceStatus.TOTAL_PRICE_STATUS_FINAL)
        val paymentDataRequest = PaymentDataRequest.fromJson(googlePayRequest.toJson())
        val paymentAuthRequest =
            GooglePayPaymentAuthRequest.ReadyToLaunch(GooglePayPaymentAuthRequestParams(1, paymentDataRequest))

        fragment1.googlePayLauncher.launch(paymentAuthRequest)
        fragment2.googlePayLauncher.launch(paymentAuthRequest)

        assertEquals(2, requestCodes.size)
        assertTrue(requestCodes[0] != requestCodes[1])

        val status = mockk<Status>(relaxed = true)
        every { status.isSuccess } returns true
        val paymentData1 = mockk<PaymentData>(relaxed = true)
        val paymentData2 = mockk<PaymentData>(relaxed = true)
        val result1 = mockk<ApiTaskResult<PaymentData>>(relaxed = true)
        every { result1.status } returns status
        every { result1.result } returns paymentData1
        val result2 = mockk<ApiTaskResult<PaymentData>>(relaxed = true)
        every { result2.status } returns status
        every { result2.result } returns paymentData2

        registry.dispatchResult(requestCodes[0], result1)
        registry.dispatchResult(requestCodes[1], result2)

        val resultSlot1 = slot<GooglePayPaymentAuthResult>()
        verify { callback1.onGooglePayLauncherResult(capture(resultSlot1)) }
        assertEquals(paymentData1, resultSlot1.captured.paymentData)

        val resultSlot2 = slot<GooglePayPaymentAuthResult>()
        verify { callback2.onGooglePayLauncherResult(capture(resultSlot2)) }
        assertEquals(paymentData2, resultSlot2.captured.paymentData)
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
            activityResultRegistry, lifecycleOwner, context, internalGooglePayClient, callback = callback
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

/**
 * Registers a [GooglePayLauncher] against its own [viewLifecycleOwner] as soon as the view is
 * created (before the Fragment reaches STARTED).
 */
internal class GooglePayLauncherRegisteringFragment(
    private val registry: ActivityResultRegistry,
    private val resultKey: String,
    private val internalGooglePayClient: GooglePayInternalClient,
    private val resultCallback: GooglePayLauncherCallback
) : Fragment() {

    lateinit var googlePayLauncher: GooglePayLauncher

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = View(requireContext())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        googlePayLauncher = GooglePayLauncher(
            registry, viewLifecycleOwner, requireContext(), internalGooglePayClient, resultKey, resultCallback
        )
    }
}
