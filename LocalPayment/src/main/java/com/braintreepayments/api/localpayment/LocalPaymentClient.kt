package com.braintreepayments.api.localpayment

import android.content.Context
import android.net.Uri
import androidx.annotation.RestrictTo
import com.braintreepayments.api.BrowserSwitchFinalResult
import com.braintreepayments.api.BrowserSwitchOptions
import com.braintreepayments.api.LaunchType
import com.braintreepayments.api.core.AnalyticsEventParams
import com.braintreepayments.api.core.AnalyticsParamRepository
import com.braintreepayments.api.core.BraintreeClient
import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.core.BraintreeRequestCodes
import com.braintreepayments.api.core.ConfigurationException
import com.braintreepayments.api.datacollector.DataCollector
import com.braintreepayments.api.sharedutils.Json
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException

/**
 * Used to integrate with local payments.
 */
class LocalPaymentClient internal constructor(
    private val braintreeClient: BraintreeClient,
    private val dataCollector: DataCollector = DataCollector(braintreeClient),
    private val localPaymentApi: LocalPaymentApi = LocalPaymentApi(braintreeClient),
    private val analyticsParamRepository: AnalyticsParamRepository = AnalyticsParamRepository.instance,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val coroutineScope: CoroutineScope = CoroutineScope(dispatcher)
) {
    /**
     * Used for linking events from the client to server side request
     * In the Local Payment flow this will be a Payment Token/Order ID
     */
    private var contextId: String? = null

    /**
     * Initializes a new [LocalPaymentClient] instance
     *
     * @param context an Android Context
     * @param authorization a Tokenization Key or Client Token used to authenticate
     * @param returnUrlScheme a custom return url to use for browser and app switching
     */
    constructor(
        context: Context,
        authorization: String,
        returnUrlScheme: String?
    ) : this(BraintreeClient(context, authorization, returnUrlScheme))

    /**
     * Starts the payment flow for a [LocalPaymentRequest] and calls back a
     * [LocalPaymentAuthRequestParams] on success that should be used to launch the user
     * authentication flow.
     *
     * @param request  [LocalPaymentRequest] with the payment details.
     * @param callback [LocalPaymentAuthCallback]
     */
    fun createPaymentAuthRequest(
        request: LocalPaymentRequest,
        callback: LocalPaymentAuthCallback
    ) {
        coroutineScope.launch {
            val result = createPaymentAuthRequest(request)
            callback.onLocalPaymentAuthRequest(result)
        }
    }

    private suspend fun createPaymentAuthRequest(
        request: LocalPaymentRequest,
    ): LocalPaymentAuthRequest {
        analyticsParamRepository.reset()
        braintreeClient.sendAnalyticsEvent(LocalPaymentAnalytics.PAYMENT_STARTED)

        if (request.paymentType == null || request.amount == null) {
            return authRequestFailure(
                BraintreeException(
                    "LocalPaymentRequest is invalid, paymentType and amount are required."
                )
            )
        }

        return try {
            val configuration = braintreeClient.getConfiguration()
            if (!configuration.isPayPalEnabled) {
                return authRequestFailure(
                    ConfigurationException("Local payments are not enabled for this merchant.")
                )
            }

            try {
                val localPaymentResult = localPaymentApi.createPaymentMethod(request)
                val paymentId = localPaymentResult.paymentId
                if (paymentId.isNotEmpty()) {
                    contextId = paymentId
                }
                buildBrowserSwitchOptions(localPaymentResult, request.hasUserLocationConsent)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                val errorMessage =
                    "An error occurred creating the local payment method: " + e.message
                authRequestFailure(BraintreeException(errorMessage))
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            authRequestFailure(e)
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun buildBrowserSwitchOptions(
        localPaymentAuthRequestParams: LocalPaymentAuthRequestParams,
        hasUserLocationConsent: Boolean,
    ): LocalPaymentAuthRequest {
        val browserSwitchOptions = BrowserSwitchOptions()
            .requestCode(BraintreeRequestCodes.LOCAL_PAYMENT.code)
            .returnUrlScheme(braintreeClient.getReturnUrlScheme())
            .launchType(LaunchType.ACTIVITY_CLEAR_TOP)
            .url(Uri.parse(localPaymentAuthRequestParams.approvalUrl))

        try {
            browserSwitchOptions.metadata(
                JSONObject()
                    .put(
                        "merchant-account-id",
                        localPaymentAuthRequestParams.request.merchantAccountId
                    )
                    .put("payment-type", localPaymentAuthRequestParams.request.paymentType)
                    .put("has-user-location-consent", hasUserLocationConsent)
            )
        } catch (e: JSONException) {
            return authRequestFailure(
                BraintreeException("Error parsing local payment request")
            )
        }

        localPaymentAuthRequestParams.browserSwitchOptions = browserSwitchOptions
        sendAnalyticsEvent(LocalPaymentAnalytics.BROWSER_SWITCH_SUCCEEDED)
        return LocalPaymentAuthRequest.ReadyToLaunch(localPaymentAuthRequestParams)
    }

    private fun authRequestFailure(error: Exception): LocalPaymentAuthRequest.Failure {
        sendAnalyticsEvent(
            eventName = LocalPaymentAnalytics.PAYMENT_FAILED,
            errorDescription = error.message
        )
        return LocalPaymentAuthRequest.Failure(error)
    }

    /**
     * After receiving a result from the web authentication flow via
     * [LocalPaymentLauncher.handleReturnToApp], pass the
     * [LocalPaymentAuthResult.Success] returned to this method to tokenize the local
     * payment method and receive a [LocalPaymentNonce] on success.
     *
     * @param context Android Context
     * @param localPaymentAuthResult a [LocalPaymentAuthResult.Success] received from
     * [LocalPaymentLauncher.handleReturnToApp]
     * @param callback [LocalPaymentInternalTokenizeCallback]
     */
    fun tokenize(
        context: Context,
        localPaymentAuthResult: LocalPaymentAuthResult.Success,
        callback: LocalPaymentTokenizeCallback
    ) {
        coroutineScope.launch {
            val result = tokenize(context, localPaymentAuthResult)
            callback.onLocalPaymentResult(result)
        }
    }

    private suspend fun tokenize(
        context: Context,
        localPaymentAuthResult: LocalPaymentAuthResult.Success,
    ): LocalPaymentResult {
        val browserSwitchResult: BrowserSwitchFinalResult.Success = localPaymentAuthResult
            .browserSwitchSuccess

        val metadata: JSONObject? = browserSwitchResult.requestMetadata
        val merchantAccountId = Json.optString(metadata, "merchant-account-id", null)
        val hasUserLocationConsent =
            Json.optBoolean(metadata, "has-user-location-consent", false)

        val deepLinkUri: Uri = browserSwitchResult.returnUrl
        val responseString = deepLinkUri.toString()
        if (responseString.lowercase(Locale.getDefault()).contains(
                LOCAL_PAYMENT_CANCEL.lowercase(Locale.getDefault())
            )
        ) {
            sendAnalyticsEvent(LocalPaymentAnalytics.PAYMENT_CANCELED)
            return LocalPaymentResult.Cancel
        }

        return try {
            val configuration = braintreeClient.getConfiguration()
            val clientMetadataID = dataCollector.getClientMetadataId(
                context,
                configuration,
                hasUserLocationConsent
            )
            val localPaymentNonce = localPaymentApi.tokenize(
                merchantAccountId,
                responseString,
                clientMetadataID
            )
            sendAnalyticsEvent(LocalPaymentAnalytics.PAYMENT_SUCCEEDED)
            LocalPaymentResult.Success(localPaymentNonce)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            tokenizeFailure(e)
        }
    }

    private fun tokenizeFailure(error: Exception): LocalPaymentResult.Failure {
        sendAnalyticsEvent(LocalPaymentAnalytics.PAYMENT_FAILED, errorDescription = error.message)
        return LocalPaymentResult.Failure(error)
    }

    private fun sendAnalyticsEvent(eventName: String, errorDescription: String? = null) {
        val eventParameters = AnalyticsEventParams(
            contextId = contextId,
            errorDescription = errorDescription
        )
        braintreeClient.sendAnalyticsEvent(eventName, eventParameters)
    }

    companion object {
        internal const val LOCAL_PAYMENT_CANCEL: String = "local-payment-cancel"
        internal const val LOCAL_PAYMENT_SUCCESS: String = "local-payment-success"
    }
}
