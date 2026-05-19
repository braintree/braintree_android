package com.braintreepayments.api.paypalmessaging

import androidx.test.core.app.ApplicationProvider
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import androidx.test.platform.app.InstrumentationRegistry
import com.braintreepayments.api.core.Authorization
import com.braintreepayments.api.core.Configuration
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.testutils.Fixtures
import com.braintreepayments.api.testutils.SharedPreferencesHelper
import com.paypal.messages.PayPalMessageView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch

@OptIn(ExperimentalBetaApi::class)
@RunWith(AndroidJUnit4ClassRunner::class)
class PayPalMessagingViewTest {

    @Before
    fun setUp() {
        val authorization = Authorization.fromString(Fixtures.TOKENIZATION_KEY)
        val configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL)
        SharedPreferencesHelper.overrideConfigurationCache(
            ApplicationProvider.getApplicationContext(),
            authorization,
            configuration
        )
    }

    @Test(timeout = 10000)
    fun start_whenConfigHasNoPayPalClientId_callsOnFailure() {
        val authorization = Authorization.fromString(Fixtures.TOKENIZATION_KEY)
        val configuration =
            Configuration.fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL_NO_CLIENT_ID)
        SharedPreferencesHelper.overrideConfigurationCache(
            ApplicationProvider.getApplicationContext(),
            authorization,
            configuration
        )

        val countDownLatch = CountDownLatch(1)
        val sut = PayPalMessagingView(
            ApplicationProvider.getApplicationContext(),
            Fixtures.TOKENIZATION_KEY
        )

        var capturedError: Exception? = null
        sut.setListener(object : PayPalMessagingListener {
            override fun onPayPalMessagingClick() = Unit
            override fun onPayPalMessagingApply() = Unit
            override fun onPayPalMessagingLoading() = Unit
            override fun onPayPalMessagingSuccess() = Unit
            override fun onPayPalMessagingFailure(error: Exception) {
                capturedError = error
                countDownLatch.countDown()
            }
        })

        sut.start()
        countDownLatch.await()

        assertNotNull(capturedError)
        assertEquals(
            "Could not find PayPal client ID in Braintree configuration.",
            capturedError?.message
        )
    }

    @Test(timeout = 10000)
    fun start_whenConfigFetchFails_callsOnFailure() {
        val countDownLatch = CountDownLatch(1)
        val sut = PayPalMessagingView(
            ApplicationProvider.getApplicationContext(),
            "sandbox_invalid_key_xxxxxxxx"
        )

        var capturedError: Exception? = null
        sut.setListener(object : PayPalMessagingListener {
            override fun onPayPalMessagingClick() = Unit
            override fun onPayPalMessagingApply() = Unit
            override fun onPayPalMessagingLoading() = Unit
            override fun onPayPalMessagingSuccess() = Unit
            override fun onPayPalMessagingFailure(error: Exception) {
                capturedError = error
                countDownLatch.countDown()
            }
        })

        sut.start()
        countDownLatch.await()

        assertNotNull(capturedError)
    }

    @Test(timeout = 10000)
    fun start_whenConfigHasPayPalClientId_addsPayPalMessageView() {
        val countDownLatch = CountDownLatch(1)
        val sut = PayPalMessagingView(
            ApplicationProvider.getApplicationContext(),
            Fixtures.TOKENIZATION_KEY
        )

        sut.setListener(object : PayPalMessagingListener {
            override fun onPayPalMessagingClick() = Unit
            override fun onPayPalMessagingApply() = Unit
            override fun onPayPalMessagingLoading() {
                countDownLatch.countDown()
            }
            override fun onPayPalMessagingSuccess() {
                countDownLatch.countDown()
            }
            override fun onPayPalMessagingFailure(error: Exception) {
                countDownLatch.countDown()
            }
        })

        sut.start()
        countDownLatch.await()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        assertEquals(1, sut.childCount)
        assertTrue(sut.getChildAt(0) is PayPalMessageView)
    }

    @Test(timeout = 15000)
    fun start_calledTwice_doesNotDuplicateChildViews() {
        val firstLatch = CountDownLatch(1)
        val sut = PayPalMessagingView(
            ApplicationProvider.getApplicationContext(),
            Fixtures.TOKENIZATION_KEY
        )

        sut.setListener(object : PayPalMessagingListener {
            override fun onPayPalMessagingClick() = Unit
            override fun onPayPalMessagingApply() = Unit
            override fun onPayPalMessagingLoading() {
                firstLatch.countDown()
            }
            override fun onPayPalMessagingSuccess() {
                firstLatch.countDown()
            }
            override fun onPayPalMessagingFailure(error: Exception) {
                firstLatch.countDown()
            }
        })

        sut.start()
        firstLatch.await()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        val secondLatch = CountDownLatch(1)
        sut.setListener(object : PayPalMessagingListener {
            override fun onPayPalMessagingClick() = Unit
            override fun onPayPalMessagingApply() = Unit
            override fun onPayPalMessagingLoading() {
                secondLatch.countDown()
            }
            override fun onPayPalMessagingSuccess() {
                secondLatch.countDown()
            }
            override fun onPayPalMessagingFailure(error: Exception) {
                secondLatch.countDown()
            }
        })

        sut.start()
        secondLatch.await()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        assertEquals(1, sut.childCount)
    }
}
