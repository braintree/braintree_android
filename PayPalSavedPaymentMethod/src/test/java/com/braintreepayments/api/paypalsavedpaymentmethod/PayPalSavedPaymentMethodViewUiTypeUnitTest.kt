@file:OptIn(ExperimentalBetaApi::class)

package com.braintreepayments.api.paypalsavedpaymentmethod

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.braintreepayments.api.core.AnalyticsEventParams
import com.braintreepayments.api.core.BraintreeClient
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.paypalsavedpaymentmethod.compose.defaultViewModelFactory
import com.braintreepayments.api.paypalsavedpaymentmethod.xml.PayPalSavedPaymentMethodView
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockkConstructor
import io.mockk.unmockkConstructor
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

// Each view's own factory builds the client, so this checks the ui_type each one really sends.
@RunWith(RobolectricTestRunner::class)
class PayPalSavedPaymentMethodViewUiTypeUnitTest {

    private val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
    private val sent = mutableListOf<AnalyticsEventParams>()

    @Before
    fun setUp() {
        mockkConstructor(BraintreeClient::class)
        every { anyConstructed<BraintreeClient>().sendAnalyticsEvent(any(), capture(sent), any()) } just Runs
    }

    @After
    fun tearDown() {
        unmockkConstructor(BraintreeClient::class)
    }

    @Test
    fun `the XML view's events are sent with the xml ui type`() {
        load(PayPalSavedPaymentMethodView(activity).viewModelFactory)

        assertEquals("xml", sent.first().uiType)
    }

    @Test
    fun `the Compose view's events are sent with the compose ui type`() {
        load(defaultViewModelFactory(activity.applicationContext))

        assertEquals("compose", sent.first().uiType)
    }

    // Loading sends render:started first, through the client the factory built.
    private fun load(viewModelFactory: (String, Uri, String?) -> ViewModelProvider.Factory) {
        val viewModel = viewModelFactory(AUTHORIZATION, RETURN_URL, null)
            .create(PayPalSavedPaymentMethodViewModel::class.java, CreationExtras.Empty)
        viewModel.load(PayPalSavedPaymentMethodRequest("10.00", "USD"), showCreditMessaging = false)
    }

    private companion object {
        const val AUTHORIZATION = "sandbox_tk_not_a_real_key"
        val RETURN_URL: Uri = Uri.parse("https://merchant.example.com")
    }
}
