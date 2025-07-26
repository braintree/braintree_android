package com.braintreepayments.api.visacheckout

import android.content.Context
import com.braintreepayments.api.core.AnalyticsEventParams
import com.braintreepayments.api.core.ApiClient
import com.braintreepayments.api.core.BraintreeClient
import com.braintreepayments.api.core.Configuration
import com.braintreepayments.api.core.ConfigurationException
import com.braintreepayments.api.visacheckout.VisaCheckoutNonce.Companion.fromJSON
import com.visa.checkout.Environment
import com.visa.checkout.Profile
import com.visa.checkout.Profile.ProfileBuilder
import com.visa.checkout.VisaPaymentSummary
import org.json.JSONException
import org.json.JSONObject

/**
 * Used to create and tokenize Visa Checkout. For more information see the [documentation](https://developer.paypal.com/braintree/docs/guides/secure-remote-commerce/overview)
 */
class VisaCheckoutClient internal constructor(
    private val braintreeClient: BraintreeClient,
    private val apiClient: ApiClient = ApiClient(braintreeClient)
) {
    /**
     * Initializes a new [VisaCheckoutClient] instance
     *
     * @param context an Android Context
     * @param authorization a Tokenization Key or Client Token used to authenticate
     */
    constructor(context: Context, authorization: String) : this(
        BraintreeClient(context, authorization)
    )

    /**
     * Creates a [Profile.ProfileBuilder] with the merchant API key, environment, and other
     * properties to be used with Visa Checkout.
     *
     * In addition to setting the `merchantApiKey` and `environment` the other properties that
     * Braintree will fill in on the ProfileBuilder are:
     *
     * [Profile.ProfileBuilder.setCardBrands] A list of Card brands that your merchant account can
     * transact.
     *
     * [Profile.ProfileBuilder.setDataLevel] - Required to be [Profile.DataLevel.FULL] for Braintree to
     * access card details
     *
     * [Profile.ProfileBuilder.setExternalClientId] -  Allows the encrypted payload to be processable
     * by Braintree.
     *
     * @param callback [VisaCheckoutCreateProfileBuilderCallback]
     */
    fun createProfileBuilder(
        callback: VisaCheckoutCreateProfileBuilderCallback
    ) {
        braintreeClient.getConfiguration { configuration: Configuration?, exception: Exception? ->
            if (configuration != null) {
                val enabledAndSdkAvailable =
                    isVisaCheckoutSDKAvailable && configuration.isVisaCheckoutEnabled
                if (!enabledAndSdkAvailable) {
                    callback.onVisaCheckoutProfileBuilderResult(
                        VisaCheckoutProfileBuilderResult.Failure(
                            ConfigurationException("Visa Checkout is not enabled.")
                        )
                    )
                    return@getConfiguration
                }

                val merchantApiKey = configuration.visaCheckoutApiKey
                val acceptedCardBrands = configuration.visaCheckoutSupportedNetworks
                val environment = if ("production" == configuration.environment) {
                    Environment.PRODUCTION
                } else {
                    Environment.SANDBOX
                }
                val profileBuilder = ProfileBuilder(merchantApiKey, environment).apply {
                    setCardBrands(acceptedCardBrands.toTypedArray<String>())
                    setDataLevel(Profile.DataLevel.FULL)
                    setExternalClientId(configuration.visaCheckoutExternalClientId)
                }
                callback.onVisaCheckoutProfileBuilderResult(
                    VisaCheckoutProfileBuilderResult.Success(profileBuilder)
                )
            } else if (exception != null) {
                callback.onVisaCheckoutProfileBuilderResult(
                    VisaCheckoutProfileBuilderResult.Failure(exception)
                )
            } else {
                callback.onVisaCheckoutProfileBuilderResult(
                    VisaCheckoutProfileBuilderResult.Failure(
                        ConfigurationException("Error getting configuration.")
                    )
                )
            }
        }
    }

    /**
     * Tokenizes the payment summary of the Visa Checkout flow.
     *
     * @param visaPaymentSummary [VisaPaymentSummary] The Visa payment to tokenize.
     * @param callback           [VisaCheckoutTokenizeCallback]
     */
    fun tokenize(
        visaPaymentSummary: VisaPaymentSummary,
        callback: VisaCheckoutTokenizeCallback
    ) {
        braintreeClient.sendAnalyticsEvent(VisaCheckoutAnalytics.TOKENIZE_STARTED)
        apiClient.tokenizeREST(
            VisaCheckoutAccount(visaPaymentSummary)
        ) { tokenizationResponse: JSONObject?, exception: Exception? ->
            if (tokenizationResponse != null) {
                try {
                    val visaCheckoutNonce = fromJSON(tokenizationResponse)
                    callbackTokenizeSuccess(
                        callback,
                        VisaCheckoutResult.Success(visaCheckoutNonce)
                    )
                } catch (e: JSONException) {
                    callbackTokenizeFailure(callback, VisaCheckoutResult.Failure(e))
                }
            } else if (exception != null) {
                callbackTokenizeFailure(callback, VisaCheckoutResult.Failure(exception))
            }
        }
    }

    private fun callbackTokenizeSuccess(
        callback: VisaCheckoutTokenizeCallback,
        result: VisaCheckoutResult.Success
    ) {
        braintreeClient.sendAnalyticsEvent(VisaCheckoutAnalytics.TOKENIZE_SUCCEEDED)
        callback.onVisaCheckoutResult(result)
    }

    private fun callbackTokenizeFailure(
        callback: VisaCheckoutTokenizeCallback,
        result: VisaCheckoutResult.Failure
    ) {
        braintreeClient.sendAnalyticsEvent(
            VisaCheckoutAnalytics.TOKENIZE_FAILED,
            AnalyticsEventParams(errorDescription = result.error.message)
        )
        callback.onVisaCheckoutResult(result)
    }

    private val isVisaCheckoutSDKAvailable: Boolean
        get() {
            return try {
                Class.forName("com.visa.checkout.VisaCheckoutSdk")
                true
            } catch (e: ClassNotFoundException) {
                false
            }
        }
}
