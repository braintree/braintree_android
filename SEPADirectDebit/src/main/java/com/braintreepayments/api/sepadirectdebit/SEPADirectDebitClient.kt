package com.braintreepayments.api.sepadirectdebit

import android.content.Context
import android.net.Uri
import android.webkit.URLUtil
import androidx.annotation.VisibleForTesting
import com.braintreepayments.api.BrowserSwitchFinalResult
import com.braintreepayments.api.BrowserSwitchOptions
import com.braintreepayments.api.core.BraintreeClient
import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.core.BraintreeRequestCodes
import org.json.JSONException
import org.json.JSONObject

/**
 * Used to integrate with SEPA Direct Debit.
 */
class SEPADirectDebitClient @VisibleForTesting internal constructor(
    private val braintreeClient: BraintreeClient,
    private val sepaDirectDebitApi: SEPADirectDebitApi = SEPADirectDebitApi(braintreeClient)
) {
    /**
     * Initializes a new [SEPADirectDebitClient] instance
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
     * Starts the SEPA tokenization process by creating a [SEPADirectDebitPaymentAuthRequestParams] to be used
     * to launch the SEPA mandate flow in
     * [SEPADirectDebitLauncher.launch]
     *
     * @param sepaDirectDebitRequest [SEPADirectDebitRequest]
     * @param callback [SEPADirectDebitPaymentAuthRequestCallback]
     */
    fun createPaymentAuthRequest(
        sepaDirectDebitRequest: SEPADirectDebitRequest,
        callback: SEPADirectDebitPaymentAuthRequestCallback
    ) {
        braintreeClient.sendAnalyticsEvent(SEPADirectDebitAnalytics.TOKENIZE_STARTED)
        sepaDirectDebitApi.createMandate(
            sepaDirectDebitRequest,
            braintreeClient.getReturnUrlScheme()
        ) { result, createMandateError ->
            if (result != null) {
                if (URLUtil.isValidUrl(result.approvalUrl)) {
                    braintreeClient.sendAnalyticsEvent(SEPADirectDebitAnalytics.CREATE_MANDATE_CHALLENGE_REQUIRED)
                    try {
                        val params = SEPADirectDebitPaymentAuthRequestParams(buildBrowserSwitchOptions(result))
                        braintreeClient.sendAnalyticsEvent(SEPADirectDebitAnalytics.CREATE_MANDATE_SUCCEEDED)
                        callback.onSEPADirectDebitPaymentAuthResult(
                            SEPADirectDebitPaymentAuthRequest.ReadyToLaunch(params)
                        )
                    } catch (exception: JSONException) {
                        braintreeClient.sendAnalyticsEvent(SEPADirectDebitAnalytics.CREATE_MANDATE_FAILED)
                        callbackCreatePaymentAuthFailure(
                            callback,
                            SEPADirectDebitPaymentAuthRequest.Failure(exception)
                        )
                    }
                } else if (result.approvalUrl == "null") {
                    braintreeClient.sendAnalyticsEvent(SEPADirectDebitAnalytics.CREATE_MANDATE_SUCCEEDED)
                    // Mandate has already been approved
                    sepaDirectDebitApi.tokenize(
                        ibanLastFour = result.ibanLastFour,
                        customerId = result.customerId,
                        bankReferenceToken = result.bankReferenceToken,
                        mandateType = result.mandateType.toString()
                    ) { sepaDirectDebitNonce, error ->
                            if (sepaDirectDebitNonce != null) {
                                callbackCreatePaymentAuthChallengeNotRequiredSuccess(
                                    callback,
                                    SEPADirectDebitPaymentAuthRequest.LaunchNotRequired(sepaDirectDebitNonce)
                                )
                            } else if (error != null) {
                                callbackCreatePaymentAuthFailure(
                                    callback,
                                    SEPADirectDebitPaymentAuthRequest.Failure(error)
                                )
                            }
                        }
                } else {
                    braintreeClient.sendAnalyticsEvent(SEPADirectDebitAnalytics.CREATE_MANDATE_FAILED)
                    callbackCreatePaymentAuthFailure(
                        callback,
                        SEPADirectDebitPaymentAuthRequest.Failure(BraintreeException("An unexpected error occurred."))
                    )
                }
            } else if (createMandateError != null) {
                braintreeClient.sendAnalyticsEvent(SEPADirectDebitAnalytics.CREATE_MANDATE_FAILED)
                callbackCreatePaymentAuthFailure(
                    callback,
                    SEPADirectDebitPaymentAuthRequest.Failure(createMandateError)
                )
            }
        }
    }

    // TODO: - The wording in this docstring is confusing to me. Let's improve & align across all clients.
    /**
     * After receiving a result from the SEPA mandate web flow via
     * [SEPADirectDebitLauncher.handleReturnToAppFromBrowser] , pass the
     * [SEPADirectDebitPaymentAuthResult.Success] returned to this method to tokenize the SEPA
     * account and receive a [SEPADirectDebitNonce] on success.
     *
     * @param paymentAuthResult a [SEPADirectDebitPaymentAuthResult.Success] received from
     * [SEPADirectDebitLauncher.handleReturnToAppFromBrowser]
     * @param callback [SEPADirectDebitInternalTokenizeCallback]
     */
    fun tokenize(
        paymentAuthResult: SEPADirectDebitPaymentAuthResult.Success,
        callback: SEPADirectDebitTokenizeCallback
    ) {
        val browserSwitchResult: BrowserSwitchFinalResult.Success =
            paymentAuthResult.browserSwitchSuccess

        val deepLinkUri: Uri = browserSwitchResult.returnUrl
        if (deepLinkUri != null) {
            if (deepLinkUri.path?.contains("success") == true && deepLinkUri.getQueryParameter("success") == "true") {
                braintreeClient.sendAnalyticsEvent(SEPADirectDebitAnalytics.CHALLENGE_SUCCEEDED)
                val metadata: JSONObject? = browserSwitchResult.requestMetadata
                if (metadata != null) {
                    val ibanLastFour = metadata.optString(IBAN_LAST_FOUR_KEY)
                    val customerId = metadata.optString(CUSTOMER_ID_KEY)
                    val bankReferenceToken = metadata.optString(BANK_REFERENCE_TOKEN_KEY)
                    val mandateType = metadata.optString(MANDATE_TYPE_KEY)

                    sepaDirectDebitApi.tokenize(
                        ibanLastFour = ibanLastFour,
                        customerId = customerId,
                        bankReferenceToken = bankReferenceToken,
                        mandateType = mandateType
                    ) { sepaDirectDebitNonce, error ->
                            if (sepaDirectDebitNonce != null) {
                                callbackTokenizeSuccess(
                                    callback,
                                    SEPADirectDebitResult.Success(sepaDirectDebitNonce)
                                )
                            } else if (error != null) {
                                callbackTokenizeFailure(callback, SEPADirectDebitResult.Failure(error))
                            }
                        }
                }
            } else if (deepLinkUri.path!!.contains("cancel")) {
                callbackTokenizeCancel(callback)
            }
        } else {
            braintreeClient.sendAnalyticsEvent(SEPADirectDebitAnalytics.CHALLENGE_FAILED)
            callbackTokenizeFailure(
                callback,
                SEPADirectDebitResult.Failure(BraintreeException("Unknown error"))
            )
        }
    }

    private fun callbackCreatePaymentAuthFailure(
        callback: SEPADirectDebitPaymentAuthRequestCallback,
        result: SEPADirectDebitPaymentAuthRequest.Failure
    ) {
        braintreeClient.sendAnalyticsEvent(SEPADirectDebitAnalytics.TOKENIZE_FAILED)
        callback.onSEPADirectDebitPaymentAuthResult(result)
    }

    private fun callbackCreatePaymentAuthChallengeNotRequiredSuccess(
        callback: SEPADirectDebitPaymentAuthRequestCallback,
        result: SEPADirectDebitPaymentAuthRequest.LaunchNotRequired
    ) {
        braintreeClient.sendAnalyticsEvent(SEPADirectDebitAnalytics.TOKENIZE_SUCCEEDED)
        callback.onSEPADirectDebitPaymentAuthResult(result)
    }

    private fun callbackTokenizeCancel(callback: SEPADirectDebitTokenizeCallback) {
        braintreeClient.sendAnalyticsEvent(SEPADirectDebitAnalytics.CHALLENGE_CANCELED)
        callback.onSEPADirectDebitResult(SEPADirectDebitResult.Cancel)
    }

    private fun callbackTokenizeFailure(
        callback: SEPADirectDebitTokenizeCallback,
        result: SEPADirectDebitResult.Failure
    ) {
        braintreeClient.sendAnalyticsEvent(SEPADirectDebitAnalytics.TOKENIZE_FAILED)
        callback.onSEPADirectDebitResult(result)
    }

    private fun callbackTokenizeSuccess(
        callback: SEPADirectDebitTokenizeCallback,
        result: SEPADirectDebitResult.Success
    ) {
        braintreeClient.sendAnalyticsEvent(SEPADirectDebitAnalytics.TOKENIZE_SUCCEEDED)
        callback.onSEPADirectDebitResult(result)
    }

    @Throws(JSONException::class)
    private fun buildBrowserSwitchOptions(createMandateResult: CreateMandateResult): BrowserSwitchOptions {
        val metadata = JSONObject()
            .put(IBAN_LAST_FOUR_KEY, createMandateResult.ibanLastFour)
            .put(CUSTOMER_ID_KEY, createMandateResult.customerId)
            .put(BANK_REFERENCE_TOKEN_KEY, createMandateResult.bankReferenceToken)
            .put(MANDATE_TYPE_KEY, createMandateResult.mandateType.toString())

        val browserSwitchOptions = BrowserSwitchOptions()
            .requestCode(BraintreeRequestCodes.SEPA_DEBIT.code)
            .url(Uri.parse(createMandateResult.approvalUrl))
            .metadata(metadata)
            .returnUrlScheme(braintreeClient.getReturnUrlScheme())

        return browserSwitchOptions
    }

    companion object {
        private const val IBAN_LAST_FOUR_KEY = "ibanLastFour"
        private const val CUSTOMER_ID_KEY = "customerId"
        private const val BANK_REFERENCE_TOKEN_KEY = "bankReferenceToken"
        private const val MANDATE_TYPE_KEY = "mandateType"
    }
}
