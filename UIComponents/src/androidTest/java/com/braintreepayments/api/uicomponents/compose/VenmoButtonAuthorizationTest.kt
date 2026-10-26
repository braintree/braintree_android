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
import com.braintreepayments.api.testutils.SharedPreferencesHelper
import com.braintreepayments.api.uicomponents.VenmoButtonColor
import com.braintreepayments.api.venmo.VenmoRequest
import com.braintreepayments.api.venmo.VenmoPaymentMethodUsage
import com.braintreepayments.api.venmo.VenmoResult
import com.braintreepayments.api.venmo.VenmoTokenizeCallback
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
 * Regression coverage for the authorization transition of the compose [VenmoButton]. The button
 * rebuilds its client when the `authorization` parameter changes, while an in-flight flow is
 * completed by the client that started it.
 *
 * Two client tokens authorize two merchants whose GraphQL URLs are served by a local
 * MockWebServer, so the endpoints a flow touches identify the authorization it used. The
 * configuration cache is seeded per authorization (no gateway calls), the browser launch fails
 * deterministically because the test app declares no browser switch deep link, and the return is
 * simulated by seeding the pending request repository and pausing/resuming with the return deep
 * link on the activity intent.
 */
class VenmoButtonAuthorizationTest {

    private val activityScenarioRule =
        ActivityScenarioRule<ButtonFlowTestActivity>(venmoReturnLaunchIntent())

    @get:Rule
    val composeTestRule = AndroidComposeTestRule(activityScenarioRule, ::activityFromRule)

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val mockWebServer = MockWebServer()
    private val servedRequestPaths = CopyOnWriteArrayList<String>()
    private val tokenizeResults = CopyOnWriteArrayList<VenmoResult>()
    private val composedAuthorizations = CopyOnWriteArrayList<String>()
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
    fun inFlightFlowAfterAuthorizationChange_tokenizesWithOriginalAuthorization_andNextFlowUsesNewOne() {
        authorization.value = clientTokenA
        composeVenmoButton()

        venmoButton().performClick()
        awaitLaunchFailure(MERCHANT_A)

        authorization.value = clientTokenB
        composeTestRule.waitUntil(WAIT_TIMEOUT_MILLIS) {
            composedAuthorizations.contains(clientTokenB)
        }

        deliverBrowserSwitchReturn(venmoPendingRequest())

        val success = awaitTokenizeSuccess()
        assertEquals(NONCE_A, success.nonce.string)
        assertEquals(USERNAME_A, success.nonce.username)

        venmoButton().performClick()
        awaitLaunchFailure(MERCHANT_B)

        assertTrue(composedAuthorizations.contains(clientTokenA))
        assertTrue(composedAuthorizations.contains(clientTokenB))
        assertEquals(
            listOf(
                "/merchants/$MERCHANT_A/graphql",
                "/merchants/$MERCHANT_A/graphql",
                "/merchants/$MERCHANT_B/graphql"
            ),
            servedRequestPaths.toList()
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
            SideEffect { composedAuthorizations.add(authorization.value) }
        }
    }

    private fun venmoButton() = composeTestRule.onNodeWithContentDescription(BUTTON_DESCRIPTION)

    /**
     * Waits until a payment context was created for [merchant] and the browser launch failed
     * with the manifest configuration error the test app's undeclared deep link scheme produces.
     */
    private fun awaitLaunchFailure(merchant: String) {
        val failuresBefore = tokenizeResults.count { it is VenmoResult.Failure }
        val graphQLPath = "/merchants/$merchant/graphql"
        composeTestRule.waitUntil(WAIT_TIMEOUT_MILLIS) {
            tokenizeResults.count { it is VenmoResult.Failure } > failuresBefore &&
                servedRequestPaths.contains(graphQLPath)
        }
        val failure = tokenizeResults.filterIsInstance<VenmoResult.Failure>().last()
        assertTrue(failure.error.message?.contains(MANIFEST_ERROR) == true)
    }

    private fun awaitTokenizeSuccess(): VenmoResult.Success {
        composeTestRule.waitUntil(WAIT_TIMEOUT_MILLIS) {
            tokenizeResults.any { it is VenmoResult.Success }
        }
        return tokenizeResults.filterIsInstance<VenmoResult.Success>().first()
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
    private fun venmoReturnLaunchIntent(): Intent {
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

    private fun venmoPendingRequest(): String = browserSwitchPendingRequest(
        requestCode = BraintreeRequestCodes.VENMO.code,
        url = APPROVAL_URL_A,
        returnUrlScheme = RETURN_URL_SCHEME,
        metadata = JSONObject()
    )

    private fun serveVenmoFlowRequests() = object : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse {
            val path = request.path.orEmpty()
            servedRequestPaths.add(path)
            if (path != "/merchants/$MERCHANT_A/graphql" && path != "/merchants/$MERCHANT_B/graphql") {
                return MockResponse().setResponseCode(404)
            }
            val requestBody = request.body.readUtf8()
            return when {
                requestBody.contains("createVenmoPaymentContext") ->
                    jsonResponse(paymentContextResponse(path))
                else -> jsonResponse(paymentContextNonceResponse(path))
            }
        }
    }

    private fun paymentContextResponse(path: String): String {
        val resourceId = if (path.contains(MERCHANT_A)) RESOURCE_ID_A else RESOURCE_ID_B
        return """{"data":{"createVenmoPaymentContext":{"venmoPaymentContext":{"id":"$resourceId"}}}}"""
    }

    private fun paymentContextNonceResponse(path: String): String {
        return if (path.contains(MERCHANT_A)) {
            """{"data":{"node":{"paymentMethodId":"$NONCE_A","userName":"$USERNAME_A"}}}"""
        } else {
            """{"data":{"node":{"paymentMethodId":"$NONCE_B","userName":"$USERNAME_B"}}}"""
        }
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
        const val NONCE_A = "nonce-from-merchant-a"
        const val NONCE_B = "nonce-from-merchant-b"
        const val USERNAME_A = "venmo-joe-from-merchant-a"
        const val USERNAME_B = "venmo-joe-from-merchant-b"
        const val APPROVAL_URL_A = "https://venmo.com/go/checkout?resource_id=$RESOURCE_ID_A"
        const val RETURN_URI_A =
            "$RETURN_URL_SCHEME://x-callback-url/vzero/auth/venmo/success?resource_id=$RESOURCE_ID_A"
        const val WAIT_TIMEOUT_MILLIS = 20_000L
    }
}
