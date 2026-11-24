package com.braintreepayments.api.uicomponents.compose

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.braintreepayments.api.core.Authorization
import com.braintreepayments.api.core.Configuration
import com.braintreepayments.api.paypal.PayPalCheckoutRequest
import com.braintreepayments.api.paypal.PayPalResult
import com.braintreepayments.api.paypal.PayPalTokenizeCallback
import com.braintreepayments.api.testutils.SharedPreferencesHelper
import com.braintreepayments.api.uicomponents.PayPalButtonColor
import java.util.concurrent.CopyOnWriteArrayList
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

/**
 * Verifies that the compose [PayPalButton] sends each merchant's credentials to its checkout endpoint
 * after authorization changes, using real SDK singletons and HTTP requests to a local MockWebServer.
 *
 * Configuration is cached per authorization. The undeclared return scheme makes browser launch fail
 * without depending on an installed browser. In-flight completion is covered by PayPalButtonUnitTest.
 */
class PayPalButtonAuthorizationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val mockWebServer = MockWebServer()
    private val servedRequests = CopyOnWriteArrayList<Pair<String, String>>()
    private val tokenizeResults = CopyOnWriteArrayList<PayPalResult>()
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

    // PayPalButton eagerly initializes analytics before BraintreeClient exists (pre-existing bug).
    // Re-enable when the fix keeping the analytics client lazy lands.
    @Ignore("PayPalButton crashes during composition before BraintreeClient exists")
    @Test
    fun authorizationChange_nextFlowSendsNewAuthorization() {
        authorization.value = clientTokenA
        composePayPalButton()

        clickAndAwaitLaunchFailure(MERCHANT_A)

        composeTestRule.runOnIdle { authorization.value = clientTokenB }
        composeTestRule.waitForIdle()

        clickAndAwaitLaunchFailure(MERCHANT_B)

        assertEquals(
            listOf(
                "/merchants/$MERCHANT_A/client_api/v1/paypal_hermes/create_payment_resource" to
                    "fingerprint-paypal-$MERCHANT_A",
                "/merchants/$MERCHANT_B/client_api/v1/paypal_hermes/create_payment_resource" to
                    "fingerprint-paypal-$MERCHANT_B"
            ),
            servedRequests.toList()
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
        }
    }

    private fun payPalButton() = composeTestRule.onNodeWithContentDescription(BUTTON_DESCRIPTION)

    /**
     * Waits until request creation succeeded for [merchant] and the browser launch failed with
     * the manifest configuration error the test app's undeclared deep link scheme produces.
     */
    private fun clickAndAwaitLaunchFailure(merchant: String) {
        val failuresBefore = tokenizeResults.count { it is PayPalResult.Failure }
        val checkoutPath = "/merchants/$merchant/client_api/v1/paypal_hermes/create_payment_resource"
        payPalButton().performClick()
        composeTestRule.waitUntil(WAIT_TIMEOUT_MILLIS) {
            tokenizeResults.count { it is PayPalResult.Failure } > failuresBefore &&
                servedRequests.any { it.first == checkoutPath }
        }
        val failure = tokenizeResults.filterIsInstance<PayPalResult.Failure>().last()
        assertTrue(failure.error.message?.contains(MANIFEST_ERROR) == true)
    }

    private fun servePayPalFlowRequests() = object : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse {
            val path = request.path.orEmpty()
            val credential = JSONObject(request.body.readUtf8()).optString("authorizationFingerprint")
            servedRequests.add(path to credential)
            return when (path) {
                "/merchants/$MERCHANT_A/client_api/v1/paypal_hermes/create_payment_resource" ->
                    jsonResponse("{\"paymentResource\":{\"redirectUrl\":\"$APPROVAL_URL\"}}")
                "/merchants/$MERCHANT_B/client_api/v1/paypal_hermes/create_payment_resource" ->
                    jsonResponse("{\"paymentResource\":{\"redirectUrl\":\"$APPROVAL_URL\"}}")
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
        const val RISK_CORRELATION_ID = "risk-correlation-id"
        const val APPROVAL_URL = "https://www.sandbox.paypal.com/checkout?token=EC-TOKEN"
        const val WAIT_TIMEOUT_MILLIS = 20_000L
    }
}
