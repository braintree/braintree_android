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
import com.braintreepayments.api.testutils.SharedPreferencesHelper
import com.braintreepayments.api.uicomponents.VenmoButtonColor
import com.braintreepayments.api.venmo.VenmoRequest
import com.braintreepayments.api.venmo.VenmoPaymentMethodUsage
import com.braintreepayments.api.venmo.VenmoResult
import com.braintreepayments.api.venmo.VenmoTokenizeCallback
import java.util.concurrent.CopyOnWriteArrayList
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Verifies that the compose [VenmoButton] sends each merchant's credentials to its GraphQL endpoint
 * after authorization changes, using real SDK singletons and HTTP requests to a local MockWebServer.
 *
 * Configuration is cached per authorization. The undeclared return scheme makes browser launch fail
 * without depending on an installed browser. In-flight completion is covered by VenmoButtonAuthorizationUnitTest.
 */
class VenmoButtonAuthorizationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val mockWebServer = MockWebServer()
    private val servedRequests = CopyOnWriteArrayList<Pair<String, String>>()
    private val tokenizeResults = CopyOnWriteArrayList<VenmoResult>()
    private val authorization = mutableStateOf("")
    private lateinit var pendingRequestRepository: PendingRequestRepository
    private lateinit var clientTokenA: String
    private lateinit var clientTokenB: String

    @Before
    fun setUp() {
        mockWebServer.dispatcher = serveVenmoFlowRequests()
        mockWebServer.start()

        val baseUrl = mockWebServer.url("/").toString()
        clientTokenA = clientToken("${baseUrl}configuration/$MERCHANT_A", "fingerprint-venmo-$MERCHANT_A")
        clientTokenB = clientToken("${baseUrl}configuration/$MERCHANT_B", "fingerprint-venmo-$MERCHANT_B")
        seedConfigurationCache(clientTokenA, "${baseUrl}merchants/$MERCHANT_A")
        seedConfigurationCache(clientTokenB, "${baseUrl}merchants/$MERCHANT_B")
        pendingRequestRepository = PendingRequestRepository(
            context = context,
            moduleName = "venmo",
            dataStore = InMemoryDataStore()
        )
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun authorizationChange_nextFlowSendsNewAuthorization() {
        authorization.value = clientTokenA
        composeVenmoButton()

        clickAndAwaitLaunchFailure(MERCHANT_A)

        composeTestRule.runOnIdle { authorization.value = clientTokenB }
        composeTestRule.waitForIdle()

        clickAndAwaitLaunchFailure(MERCHANT_B)

        assertEquals(
            listOf(
                "/merchants/$MERCHANT_A/graphql" to "Bearer fingerprint-venmo-$MERCHANT_A",
                "/merchants/$MERCHANT_B/graphql" to "Bearer fingerprint-venmo-$MERCHANT_B"
            ),
            servedRequests.toList()
        )
    }

    private fun composeVenmoButton() {
        composeTestRule.setContent {
            VenmoButton(
                style = VenmoButtonColor.Blue,
                venmoRequest = VenmoRequest(
                    paymentMethodUsage = VenmoPaymentMethodUsage.MULTI_USE
                ),
                authorization = authorization.value,
                appLinkReturnUrl = Uri.parse(APP_LINK_RETURN_URL),
                deepLinkFallbackUrlScheme = RETURN_URL_SCHEME,
                pendingRequestRepository = pendingRequestRepository,
                venmoTokenizeCallback = VenmoTokenizeCallback { tokenizeResults.add(it) }
            )
        }
    }

    private fun venmoButton() = composeTestRule.onNodeWithContentDescription(BUTTON_DESCRIPTION)

    /**
     * Waits until a payment context was created for [merchant] and the browser launch failed
     * with the manifest configuration error the test app's undeclared deep link scheme produces.
     */
    private fun clickAndAwaitLaunchFailure(merchant: String) {
        val failuresBefore = tokenizeResults.count { it is VenmoResult.Failure }
        val graphQLPath = "/merchants/$merchant/graphql"
        venmoButton().performClick()
        composeTestRule.waitUntil(WAIT_TIMEOUT_MILLIS) {
            tokenizeResults.count { it is VenmoResult.Failure } > failuresBefore &&
                servedRequests.any { it.first == graphQLPath }
        }
        val failure = tokenizeResults.filterIsInstance<VenmoResult.Failure>().last()
        assertTrue(failure.error.message?.contains(MANIFEST_ERROR) == true)
    }

    private fun serveVenmoFlowRequests() = object : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse {
            val path = request.path.orEmpty()
            servedRequests.add(path to request.getHeader("Authorization").orEmpty())
            if (path != "/merchants/$MERCHANT_A/graphql" && path != "/merchants/$MERCHANT_B/graphql") {
                return MockResponse().setResponseCode(404)
            }
            val requestBody = request.body.readUtf8()
            return when {
                requestBody.contains("createVenmoPaymentContext") ->
                    jsonResponse(paymentContextResponse(path))
                else -> MockResponse().setResponseCode(404)
            }
        }
    }

    private fun paymentContextResponse(path: String): String {
        val resourceId = if (path.contains(MERCHANT_A)) RESOURCE_ID_A else RESOURCE_ID_B
        return """{"data":{"createVenmoPaymentContext":{"venmoPaymentContext":{"id":"$resourceId"}}}}"""
    }

    private fun jsonResponse(body: String) = MockResponse().setResponseCode(200).setBody(body)

    private fun seedConfigurationCache(clientToken: String, merchantPath: String) {
        val configurationJson = """
            {
              "clientApiUrl": "$merchantPath/client_api",
              "environment": "sandbox",
              "merchantId": "merchant-id",
              "graphQL": {"url": "$merchantPath/graphql"},
              "payWithVenmo": {
                "accessToken": "venmo-access-token",
                "environment": "sandbox",
                "merchantId": "venmo-profile-id"
              }
            }
        """.trimIndent()
        SharedPreferencesHelper.overrideConfigurationCache(
            context,
            Authorization.fromString(clientToken),
            Configuration.fromJson(configurationJson)
        )
    }

    private companion object {
        const val BUTTON_DESCRIPTION = "Venmo"
        const val MANIFEST_ERROR = "AndroidManifest.xml is incorrectly configured"
        const val RETURN_URL_SCHEME = "com.braintreepayments.api.uicomponents.venmo.flow.test"
        const val APP_LINK_RETURN_URL = "https://merchant.example.com/venmo-return"
        const val MERCHANT_A = "merchant-a"
        const val MERCHANT_B = "merchant-b"
        const val RESOURCE_ID_A = "resource-id-from-merchant-a"
        const val RESOURCE_ID_B = "resource-id-from-merchant-b"
        const val WAIT_TIMEOUT_MILLIS = 20_000L
    }
}
