package com.braintreepayments.api.localpayment

import android.content.Context
import android.net.Uri
import androidx.annotation.RestrictTo
import com.braintreepayments.api.BrowserSwitchFinalResult
import com.braintreepayments.api.BrowserSwitchOptions
import com.braintreepayments.api.core.AnalyticsEventParams
import com.braintreepayments.api.core.AnalyticsParamRepository
import com.braintreepayments.api.core.BraintreeClient
import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.core.BraintreeRequestCodes
import com.braintreepayments.api.core.Configuration
import com.braintreepayments.api.core.ConfigurationException
import com.braintreepayments.api.datacollector.DataCollector
import com.braintreepayments.api.sharedutils.Json
import org.json.JSONException
import org.json.JSONObject
import java.util.Locale

/**
 * Used to integrate with local payments.
 */
class LocalPaymentClient internal constructor(
    private val braintreeClient: BraintreeClient,
    private val dataCollector: DataCollector = DataCollector(braintreeClient),
    private val localPaymentApi: LocalPaymentApi = LocalPaymentApi(braintreeClient),
    private val analyticsParamRepository: AnalyticsParamRepository = AnalyticsParamRepository.instance
) {
    /**
     * Used for linking events from the client to server side request
     * In the Local Payment flow this will be a Payment Token/Order ID
     */
    private var payPalContextId: String? = null

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
        analyticsParamRepository.reset()
        braintreeClient.sendAnalyticsEvent(LocalPaymentAnalytics.PAYMENT_STARTED)

        var exception: Exception? = null
        if (request.paymentType == null || request.amount == null) {
            exception = BraintreeException(
                "LocalPaymentRequest is invalid, paymentType and amount are required."
            )
        }

        if (exception != null) {
            authRequestFailure(exception, callback)
        } else {
            braintreeClient.getConfiguration { configuration: Configuration?, error: Exception? ->
                if (configuration != null) {
                    if (!configuration.isPayPalEnabled) {
                        val errorMessage = "Local payments are not enabled for this merchant."
                        authRequestFailure(ConfigurationException(errorMessage), callback)
                        return@getConfiguration
                    }

                    localPaymentApi.createPaymentMethod(
                        request
                    ) { localPaymentResult: LocalPaymentAuthRequestParams?, createPaymentMethodError: Exception? ->
                        if (localPaymentResult != null) {
                            val paypalContextId = localPaymentResult.paymentId
                            if (paypalContextId.isNotEmpty()) {
                                payPalContextId = paypalContextId
                            }
                            buildBrowserSwitchOptions(
                                localPaymentResult,
                                request.hasUserLocationConsent,
                                callback
                            )
                        } else if (createPaymentMethodError != null) {
                            val errorMessage =
                                "An error occurred creating the local payment method."
                            authRequestFailure(BraintreeException(errorMessage), callback)
                        }
                    }
                } else if (error != null) {
                    authRequestFailure(error, callback)
                }
            }
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun buildBrowserSwitchOptions(
        localPaymentAuthRequestParams: LocalPaymentAuthRequestParams,
        hasUserLocationConsent: Boolean,
        callback: LocalPaymentAuthCallback
    ) {
        val browserSwitchOptions = BrowserSwitchOptions()
            .requestCode(BraintreeRequestCodes.LOCAL_PAYMENT.code)
            .returnUrlScheme(braintreeClient.getReturnUrlScheme())
            .launchAsNewTask(braintreeClient.launchesBrowserSwitchAsNewTask())
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
            authRequestFailure(
                BraintreeException("Error parsing local payment request"),
                callback
            )
            return
        }

        localPaymentAuthRequestParams.browserSwitchOptions = browserSwitchOptions
        callback.onLocalPaymentAuthRequest(
            LocalPaymentAuthRequest.ReadyToLaunch(localPaymentAuthRequestParams)
        )
        sendAnalyticsEvent(LocalPaymentAnalytics.BROWSER_SWITCH_SUCCEEDED)
    }

    private fun authRequestFailure(error: Exception, callback: LocalPaymentAuthCallback) {
        sendAnalyticsEvent(eventName = LocalPaymentAnalytics.PAYMENT_FAILED, errorDescription = error.message)
        callback.onLocalPaymentAuthRequest(LocalPaymentAuthRequest.Failure(error))
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
        val browserSwitchResult: BrowserSwitchFinalResult.Success = localPaymentAuthResult
            .browserSwitchSuccess

        val metadata: JSONObject? = browserSwitchResult.requestMetadata
        val merchantAccountId = Json.optString(metadata, "merchant-account-id", null)
        val hasUserLocationConsent = Json.optBoolean(metadata, "has-user-location-consent", false)

        val deepLinkUri: Uri = browserSwitchResult.returnUrl
        val responseString = deepLinkUri.toString()
        if (responseString.lowercase(Locale.getDefault()).contains(
                LOCAL_PAYMENT_CANCEL.lowercase(Locale.getDefault())
            )
        ) {
            callbackCancel(callback)
            return
        }
        braintreeClient.getConfiguration { configuration: Configuration?, error: Exception? ->
            if (configuration != null) {
                localPaymentApi.tokenize(
                    merchantAccountId, responseString,
                    dataCollector.getClientMetadataId(
                        context,
                        configuration,
                        hasUserLocationConsent
                    )
                ) { localPaymentNonce: LocalPaymentNonce?, localPaymentError: Exception? ->
                    if (localPaymentNonce != null) {
                        sendAnalyticsEvent(LocalPaymentAnalytics.PAYMENT_SUCCEEDED)
                        callback.onLocalPaymentResult(LocalPaymentResult.Success(localPaymentNonce))
                    } else if (localPaymentError != null) {
                        tokenizeFailure(localPaymentError, callback)
                    }
                }
            } else if (error != null) {
                tokenizeFailure(error, callback)
            }
        }
    }

    private fun callbackCancel(callback: LocalPaymentTokenizeCallback) {
        sendAnalyticsEvent(LocalPaymentAnalytics.PAYMENT_CANCELED)
        callback.onLocalPaymentResult(LocalPaymentResult.Cancel)
    }

    private fun tokenizeFailure(error: Exception, callback: LocalPaymentTokenizeCallback) {
        sendAnalyticsEvent(LocalPaymentAnalytics.PAYMENT_FAILED, errorDescription = error.message)
        callback.onLocalPaymentResult(LocalPaymentResult.Failure(error))
    }

    private fun sendAnalyticsEvent(eventName: String, errorDescription: String? = null) {
        val eventParameters = AnalyticsEventParams(
            payPalContextId = payPalContextId,
            errorDescription = errorDescription
        )
        braintreeClient.sendAnalyticsEvent(eventName, eventParameters)
    }

    companion object {
        internal const val LOCAL_PAYMENT_CANCEL: String = "local-payment-cancel"
        internal const val LOCAL_PAYMENT_SUCCESS: String = "local-payment-success"
    }
}
