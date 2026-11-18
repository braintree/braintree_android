package com.braintreepayments.api.uicomponents.compose

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import com.braintreepayments.api.core.Authorization
import com.braintreepayments.api.core.BraintreeRequestCodes
import com.braintreepayments.api.core.Configuration
import com.braintreepayments.api.paypal.PayPalCheckoutRequest
import com.braintreepayments.api.paypal.PayPalResult
import com.braintreepayments.api.paypal.PayPalTokenizeCallback
import com.braintreepayments.api.testutils.SharedPreferencesHelper
import com.braintreepayments.api.uicomponents.PayPalButtonColor
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Regression coverage for the authorization transition of the compose [PayPalButton]. The button
 * rebuilds its client when the `authorization` parameter changes, while an in-flight flow is
 * completed by the client that started it.
 *
 * Two client tokens authorize two merchants whose client API URLs are served by a local
 * MockWebServer, so the endpoints a flow touches identify the authorization it used. The
 * configuration cache is seeded per authorization (no gateway calls), the browser launch fails
 * deterministically because the test app declares no browser switch deep link, and the return is
 * simulated by seeding the pending request repository and pausing/resuming with the return deep
 * link on the activity intent.
 */
class PayPalButtonAuthorizationTest {

    private val activityScenarioRule =
        ActivityScenarioRule<ButtonFlowTestActivity>(payPalReturnLaunchIntent())

    @get:Rule
    val composeTestRule = AndroidComposeTestRule(activityScenarioRule, ::activityFromRule)

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val mockWebServer = MockWebServer()
    private val servedRequestPaths = CopyOnWriteArrayList<String>()
    private val tokenizeResults = CopyOnWriteArrayList<PayPalResult>()
    private val composedAuthorizations = CopyOnWriteArrayList<String>()
    private val authorization = mutableStateOf("")
    private lateinit var pendingRequestRepository: PendingRequestRepository
    private lateinit var clientTokenA: String
    private lateinit var clientTokenB: String

    @Before
    fun setUp() {
        mockWebServer.dispatcher = servePayPalFlowRequests()
        mockWebServer.start()

        val baseUrl = mockWebServer.url("/").toString()
        clientTokenA = clientToken("${baseUrl}configuration/$MERCHANT_A", "fingerprint-paypal-$MERCHANT_A")
        clientTokenB = clientToken("${baseUrl}configuration/$MERCHANT_B", "fingerprint-paypal-$MERCHANT_B")
        seedConfigurationCache(clientTokenA, "${baseUrl}merchants/$MERCHANT_A/client_api")
        seedConfigurationCache(clientTokenB, "${baseUrl}merchants/$MERCHANT_B/client_api")
        pendingRequestRepository = PendingRequestRepository(
            context = context,
            moduleName = "paypal",
            dataStore = InMemoryDataStore()
        )
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun inFlightFlowAfterAuthorizationChange_tokenizesWithOriginalAuthorization_andNextFlowUsesNewOne() {
        authorization.value = clientTokenA
        composePayPalButton()

        payPalButton().performClick()
        awaitLaunchFailure(MERCHANT_A)

        authorization.value = clientTokenB
        composeTestRule.waitUntil(WAIT_TIMEOUT_MILLIS) {
            composedAuthorizations.contains(clientTokenB)
        }

        deliverBrowserSwitchReturn(payPalPendingRequest())

        val success = awaitTokenizeSuccess()
        assertEquals(NONCE_A, success.nonce.string)

        payPalButton().performClick()
        awaitLaunchFailure(MERCHANT_B)

        assertTrue(composedAuthorizations.contains(clientTokenA))
        assertTrue(composedAuthorizations.contains(clientTokenB))
        assertEquals(
            listOf(
                "/merchants/$MERCHANT_A/client_api/v1/paypal_hermes/create_payment_resource",
                "/merchants/$MERCHANT_A/client_api/v1/payment_methods/paypal_accounts",
                "/merchants/$MERCHANT_B/client_api/v1/paypal_hermes/create_payment_resource"
            ),
            servedRequestPaths.toList()
        )
    }

    private fun composePayPalButton() {
        composeTestRule.setContent {
            PayPalButton(
                style = PayPalButtonColor.Blue,
                payPalRequest = PayPalCheckoutRequest(
                    amount = "1.00",
                    hasUserLocationConsent = true
                ).apply {
                    riskCorrelationId = RISK_CORRELATION_ID
                },
                authorization = authorization.value,
                appLinkReturnUrl = Uri.parse(APP_LINK_RETURN_URL),
                deepLinkFallbackUrlScheme = RETURN_URL_SCHEME,
                pendingRequestRepository = pendingRequestRepository,
                paypalTokenizeCallback = PayPalTokenizeCallback { tokenizeResults.add(it) }
            )
            SideEffect { composedAuthorizations.add(authorization.value) }
        }
    }

    private fun payPalButton() = composeTestRule.onNodeWithContentDescription(BUTTON_DESCRIPTION)

    /**
     * Waits until request creation succeeded for [merchant] and the browser launch failed with
     * the manifest configuration error the test app's undeclared deep link scheme produces.
     */
    private fun awaitLaunchFailure(merchant: String) {
        val failuresBefore = tokenizeResults.count { it is PayPalResult.Failure }
        val checkoutPath = "/merchants/$merchant/client_api/v1/paypal_hermes/create_payment_resource"
        composeTestRule.waitUntil(WAIT_TIMEOUT_MILLIS) {
            tokenizeResults.count { it is PayPalResult.Failure } > failuresBefore &&
                servedRequestPaths.contains(checkoutPath)
        }
        val failure = tokenizeResults.filterIsInstance<PayPalResult.Failure>().last()
        assertTrue(failure.error.message?.contains(MANIFEST_ERROR) == true)
    }

    private fun awaitTokenizeSuccess(): PayPalResult.Success {
        composeTestRule.waitUntil(WAIT_TIMEOUT_MILLIS) {
            tokenizeResults.any { it is PayPalResult.Success }
        }
        return tokenizeResults.filterIsInstance<PayPalResult.Success>().first()
    }

    /**
     * Simulates the app returning from the browser: the pending request the flow would have
     * stored is seeded, then a pause/resume cycle re-runs the resume handler, which reads the
     * deep link the activity was launched with.
     */
    private fun deliverBrowserSwitchReturn(pendingRequest: String) {
        runBlocking { pendingRequestRepository.storePendingRequest(pendingRequest) }
        val scenario = composeTestRule.activityRule.scenario
        scenario.moveToState(Lifecycle.State.CREATED)
        scenario.moveToState(Lifecycle.State.RESUMED)
    }

    /**
     * The activity is launched with the return deep link already on its intent: the button reads
     * the activity intent on resume, and launching with the deep link (instead of swapping the
     * intent mid-test) keeps ActivityScenario's lifecycle tracking intact.
     */
    private fun payPalReturnLaunchIntent(): Intent {
        return Intent(
            InstrumentationRegistry.getInstrumentation().targetContext,
            ButtonFlowTestActivity::class.java
        ).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse(RETURN_URI_A)
        }
    }

    private fun activityFromRule(
        rule: ActivityScenarioRule<ButtonFlowTestActivity>
    ): ButtonFlowTestActivity {
        var activity: ButtonFlowTestActivity? = null
        rule.scenario.onActivity { activity = it }
        return checkNotNull(activity)
    }

    private fun payPalPendingRequest(): String {
        val metadata = JSONObject()
            .put("approval-url", APPROVAL_URL_A)
            .put("success-url", "$RETURN_URL_SCHEME://onetouch/v1/success")
            .put("payment-type", "single-payment")
            .put("client-metadata-id", CLIENT_METADATA_ID)
            .put("merchant-account-id", null)
            .put("source", "paypal-browser")
            .put("intent", "authorize")
        return browserSwitchPendingRequest(
            requestCode = BraintreeRequestCodes.PAYPAL.code,
            url = APPROVAL_URL_A,
            returnUrlScheme = RETURN_URL_SCHEME,
            metadata = metadata
        )
    }

    private fun servePayPalFlowRequests() = object : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse {
            val path = request.path.orEmpty()
            servedRequestPaths.add(path)
            return when (path) {
                "/merchants/$MERCHANT_A/client_api/v1/paypal_hermes/create_payment_resource" ->
                    jsonResponse("{\"paymentResource\":{\"redirectUrl\":\"$APPROVAL_URL_A\"}}")
                "/merchants/$MERCHANT_A/client_api/v1/payment_methods/paypal_accounts" ->
                    jsonResponse(TOKENIZE_RESPONSE)
                "/merchants/$MERCHANT_B/client_api/v1/paypal_hermes/create_payment_resource" ->
                    jsonResponse("{\"paymentResource\":{\"redirectUrl\":\"$APPROVAL_URL_B\"}}")
                else -> MockResponse().setResponseCode(404)
            }
        }
    }

    private fun jsonResponse(body: String) = MockResponse().setResponseCode(200).setBody(body)

    private fun seedConfigurationCache(clientToken: String, clientApiUrl: String) {
        val configurationJson = """
            {
              "clientApiUrl": "$clientApiUrl",
              "environment": "sandbox",
              "merchantId": "merchant-id",
              "paypalEnabled": true
            }
        """.trimIndent()
        SharedPreferencesHelper.overrideConfigurationCache(
            context,
            Authorization.fromString(clientToken),
            Configuration.fromJson(configurationJson)
        )
    }

    private companion object {
        const val BUTTON_DESCRIPTION = "PayPal"
        const val MANIFEST_ERROR = "AndroidManifest.xml is incorrectly configured"
        const val RETURN_URL_SCHEME = "com.braintreepayments.api.uicomponents.paypal.flow.test"
        const val APP_LINK_RETURN_URL = "https://merchant.example.com/paypal-return"
        const val MERCHANT_A = "merchant-a"
        const val MERCHANT_B = "merchant-b"
        const val EC_TOKEN_A = "EC-TOKEN-FROM-MERCHANT-A"
        const val EC_TOKEN_B = "EC-TOKEN-FROM-MERCHANT-B"
        const val NONCE_A = "nonce-from-merchant-a"
        const val CLIENT_METADATA_ID = "client-metadata-id"
        const val RISK_CORRELATION_ID = "risk-correlation-id"
        const val APPROVAL_URL_A = "https://www.sandbox.paypal.com/checkout?token=$EC_TOKEN_A"
        const val APPROVAL_URL_B = "https://www.sandbox.paypal.com/checkout?token=$EC_TOKEN_B"
        const val RETURN_URI_A =
            "$RETURN_URL_SCHEME://onetouch/v1/success?token=$EC_TOKEN_A&PayerID=FAKE-PAYER-ID"
        const val WAIT_TIMEOUT_MILLIS = 20_000L
        const val TOKENIZE_RESPONSE = """
            {"paypalAccounts": [{"nonce": "$NONCE_A", "default": false, "details": {}}]}
        """
    }
}
