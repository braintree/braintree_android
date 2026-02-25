package com.braintreepayments.api.sepadirectdebit

import android.content.Context
import android.net.Uri
import android.webkit.URLUtil
import com.braintreepayments.api.BrowserSwitchFinalResult
import com.braintreepayments.api.BrowserSwitchOptions
import com.braintreepayments.api.core.AnalyticsEventParams
import com.braintreepayments.api.core.BraintreeClient
import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.core.BraintreeRequestCodes
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject

/**
 * Used to integrate with SEPA Direct Debit.
 */
class SEPADirectDebitClient internal constructor(
    private val braintreeClient: BraintreeClient,
    private val sepaDirectDebitApi: SEPADirectDebitApi = SEPADirectDebitApi(braintreeClient),
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val coroutineScope: CoroutineScope = CoroutineScope(dispatcher)
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
        coroutineScope.launch {
            val result = createPaymentAuthRequest(sepaDirectDebitRequest)
            callback.onSEPADirectDebitPaymentAuthResult(result)
        }
    }

    @Suppress("ReturnCount")
    private suspend fun createPaymentAuthRequest(
        sepaDirectDebitRequest: SEPADirectDebitRequest,
    ): SEPADirectDebitPaymentAuthRequest {
        braintreeClient.sendAnalyticsEvent(SEPADirectDebitAnalytics.TOKENIZE_STARTED)
        try {
            val result = sepaDirectDebitApi.createMandate(
                sepaDirectDebitRequest,
                braintreeClient.getReturnUrlScheme()
            )

            if (URLUtil.isValidUrl(result.approvalUrl)) {
                braintreeClient.sendAnalyticsEvent(SEPADirectDebitAnalytics.CREATE_MANDATE_CHALLENGE_REQUIRED)
                try {
                    val params = SEPADirectDebitPaymentAuthRequestParams(buildBrowserSwitchOptions(result))
                    braintreeClient.sendAnalyticsEvent(SEPADirectDebitAnalytics.CREATE_MANDATE_SUCCEEDED)
                    return SEPADirectDebitPaymentAuthRequest.ReadyToLaunch(params)
                } catch (exception: JSONException) {
                    createMandateFailedAnalyticsEvent(errorDescription = exception.message)
                    return createPaymentAuthFailure(exception)
                }
            } else if (result.approvalUrl == "null") {
                braintreeClient.sendAnalyticsEvent(SEPADirectDebitAnalytics.CREATE_MANDATE_SUCCEEDED)
                // Mandate has already been approved
                try {
                    val sepaDirectDebitNonce = sepaDirectDebitApi.tokenize(
                        ibanLastFour = result.ibanLastFour,
                        customerId = result.customerId,
                        bankReferenceToken = result.bankReferenceToken,
                        mandateType = result.mandateType.toString()
                    )
                    braintreeClient.sendAnalyticsEvent(SEPADirectDebitAnalytics.TOKENIZE_SUCCEEDED)
                    return SEPADirectDebitPaymentAuthRequest.LaunchNotRequired(sepaDirectDebitNonce)
                } catch (error: Exception) {
                    return createPaymentAuthFailure(error)
                }
            } else {
                val errorMessage = "An unexpected error occurred."
                createMandateFailedAnalyticsEvent(errorDescription = errorMessage)
                return createPaymentAuthFailure(BraintreeException(errorMessage))
            }
        } catch (createMandateError: Exception) {
            createMandateFailedAnalyticsEvent(errorDescription = createMandateError.message)
            return createPaymentAuthFailure(createMandateError)
        }
    }

    private fun createMandateFailedAnalyticsEvent(errorDescription: String? = null) {
        braintreeClient.sendAnalyticsEvent(
            SEPADirectDebitAnalytics.CREATE_MANDATE_FAILED,
            AnalyticsEventParams(errorDescription = errorDescription)
        )
    }

    // TODO: - The wording in this docstring is confusing to me. Let's improve & align across all clients.
    /**
     * After receiving a result from the SEPA mandate web flow via
     * [SEPADirectDebitLauncher.handleReturnToApp] , pass the
     * [SEPADirectDebitPaymentAuthResult.Success] returned to this method to tokenize the SEPA
     * account and receive a [SEPADirectDebitNonce] on success.
     *
     * @param paymentAuthResult a [SEPADirectDebitPaymentAuthResult.Success] received from
     * [SEPADirectDebitLauncher.handleReturnToApp]
     * @param callback [SEPADirectDebitTokenizeCallback]
     */
    fun tokenize(
        paymentAuthResult: SEPADirectDebitPaymentAuthResult.Success,
        callback: SEPADirectDebitTokenizeCallback
    ) {
        coroutineScope.launch {
            val result = tokenize(paymentAuthResult)
            callback.onSEPADirectDebitResult(result)
        }
    }

    @Suppress("ReturnCount")
    private suspend fun tokenize(
        paymentAuthResult: SEPADirectDebitPaymentAuthResult.Success,
    ): SEPADirectDebitResult {
        val browserSwitchResult: BrowserSwitchFinalResult.Success =
            paymentAuthResult.browserSwitchSuccess

        val deepLinkUri: Uri = browserSwitchResult.returnUrl
        if (deepLinkUri.path?.contains("success") == true && deepLinkUri.getQueryParameter("success") == "true") {
            braintreeClient.sendAnalyticsEvent(SEPADirectDebitAnalytics.CHALLENGE_SUCCEEDED)
            val metadata: JSONObject? = browserSwitchResult.requestMetadata
            if (metadata != null) {
                val ibanLastFour = metadata.optString(IBAN_LAST_FOUR_KEY)
                val customerId = metadata.optString(CUSTOMER_ID_KEY)
                val bankReferenceToken = metadata.optString(BANK_REFERENCE_TOKEN_KEY)
                val mandateType = metadata.optString(MANDATE_TYPE_KEY)
                try {
                    val sepaDirectDebitNonce = sepaDirectDebitApi.tokenize(
                        ibanLastFour = ibanLastFour,
                        customerId = customerId,
                        bankReferenceToken = bankReferenceToken,
                        mandateType = mandateType
                    )
                    braintreeClient.sendAnalyticsEvent(SEPADirectDebitAnalytics.TOKENIZE_SUCCEEDED)
                    return SEPADirectDebitResult.Success(sepaDirectDebitNonce)
                } catch (error: Exception) {
                    return tokenizeFailure(error)
                }
            } else {
                return tokenizeFailure(BraintreeException("Browser switch return metadata is null."))
            }
        } else if (deepLinkUri.path?.contains("cancel") == true) {
            braintreeClient.sendAnalyticsEvent(SEPADirectDebitAnalytics.CHALLENGE_CANCELED)
            return SEPADirectDebitResult.Cancel
        } else {
            return tokenizeFailure(BraintreeException("Unknown deep link path: ${deepLinkUri.path}"))
        }
    }

    private fun createPaymentAuthFailure(
        error: Exception
    ): SEPADirectDebitPaymentAuthRequest.Failure {
        braintreeClient.sendAnalyticsEvent(
            SEPADirectDebitAnalytics.TOKENIZE_FAILED,
            AnalyticsEventParams(errorDescription = error.message)
        )
        return SEPADirectDebitPaymentAuthRequest.Failure(error)
    }

    private fun tokenizeFailure(
        error: Exception
    ): SEPADirectDebitResult.Failure {
        braintreeClient.sendAnalyticsEvent(
            SEPADirectDebitAnalytics.TOKENIZE_FAILED,
            AnalyticsEventParams(errorDescription = error.message)
        )
        return SEPADirectDebitResult.Failure(error)
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
