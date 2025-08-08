package com.braintreepayments.demo

import android.content.Context
import androidx.lifecycle.ViewModel
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.shopperinsights.ButtonOrder
import com.braintreepayments.api.shopperinsights.ButtonType
import com.braintreepayments.api.shopperinsights.ExperimentType
import com.braintreepayments.api.shopperinsights.PageType
import com.braintreepayments.api.shopperinsights.PresentmentDetails
import com.braintreepayments.api.shopperinsights.v2.CustomerRecommendationsResult
import com.braintreepayments.api.shopperinsights.v2.CustomerSessionRequest
import com.braintreepayments.api.shopperinsights.v2.CustomerSessionResult
import com.braintreepayments.api.shopperinsights.v2.PaymentOptions
import com.braintreepayments.api.shopperinsights.v2.ShopperInsightsClientV2
import java.security.MessageDigest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

@OptIn(ExperimentalBetaApi::class)
class ShopperInsightsV2ViewModel : ViewModel() {
    private lateinit var shopperInsightsClient: ShopperInsightsClientV2

    private val _sessionId = MutableStateFlow<String>("94f0b2db-5323-4d86-add3-paypal000000")
    val sessionId: StateFlow<String> = _sessionId
    @OptIn(ExperimentalBetaApi::class)
    var recommendations = MutableStateFlow<List<PaymentOptions>>(emptyList())
    var isInPayPalNetwork = MutableStateFlow<Boolean>(false)
    var error = MutableStateFlow<String>("")
    var recommendationsCompleted = MutableStateFlow<Boolean>(false)

    fun initShopperInsightsClient(context: Context, authString: String) {
        shopperInsightsClient = ShopperInsightsClientV2(context, authString)
    }

    fun handleCreateCustomerSession(emailText: String, countryCodeText: String, nationalNumberText: String) {
        val customerSessionRequest = CustomerSessionRequest(
            hashedEmail = emailText.sha256(),
            hashedPhoneNumber = nationalNumberText.sha256()
        )

        _sessionId.update { "" }

        shopperInsightsClient.createCustomerSession(customerSessionRequest) { result ->
            when (result) {
                is CustomerSessionResult.Success -> {
                    _sessionId.update { result.sessionId }
                }
                is CustomerSessionResult.Failure -> {
                    this@ShopperInsightsV2ViewModel.error.update { "CreateCustomerSession failed: ${result.error}" }
                }
            }
        }
    }

    fun handleUpdateCustomerSession(
        emailText: String,
        countryCodeText: String,
        nationalNumberText: String,
        sessionId: String
    ) {
        val customerSessionRequest = CustomerSessionRequest(
            hashedEmail = emailText.sha256(),
            hashedPhoneNumber = nationalNumberText.sha256()
        )

        _sessionId.update { "" }

        shopperInsightsClient.updateCustomerSession(customerSessionRequest, sessionId) { result ->
            when (result) {
                is CustomerSessionResult.Success -> {
                    _sessionId.update { result.sessionId }
                }
                is CustomerSessionResult.Failure -> {
                    error.update { "UpdateCustomerSession failed: ${result.error}" }
                }
            }
        }
    }

    fun handleGetRecommendations(sessionId: String) {
        shopperInsightsClient.generateCustomerRecommendations(sessionId = sessionId) { result ->
            when (result) {
                is CustomerRecommendationsResult.Success -> {
                    isInPayPalNetwork.update { result.customerRecommendations.isInPayPalNetwork == true }
                    result.customerRecommendations.paymentRecommendations?.let { recommendations ->
                        this@ShopperInsightsV2ViewModel.recommendations.update { recommendations }
                    }

                    recommendationsCompleted.value = true

                    if (isInPayPalNetwork.value == true) {
                        val paymentOption = this@ShopperInsightsV2ViewModel.recommendations.value.first().paymentOption
                        val buttonType = if (paymentOption == "PAYPAL") {
                            ButtonType.PAYPAL
                        } else if (paymentOption == "VENMO") {
                            ButtonType.VENMO
                        } else {
                            ButtonType.OTHER
                        }
                        sendButtonPresentedEvent(buttonType, sessionId)
                    }
                }
                is CustomerRecommendationsResult.Failure -> {
                    recommendationsCompleted.value = true

                    this@ShopperInsightsV2ViewModel.error.update { "GetRecommendations failed: ${result.error}" }
                }
            }
        }
    }

    fun sendButtonPresentedEvent(buttonType: ButtonType, sessionId: String) {
        shopperInsightsClient.sendPresentedEvent(
            buttonType,
            PresentmentDetails(ExperimentType.CONTROL, ButtonOrder.FIRST, PageType.OTHER),
            sessionId
        )
    }

    fun sendSelectedEvent(buttonType: ButtonType, sessionId: String) {
        shopperInsightsClient.sendSelectedEvent(
            buttonType,
            sessionId
        )
    }

    fun resetRecommendationsCompleted() {
        recommendationsCompleted.value = false
    }
}

private fun String.sha256(): String {
    return hashString(this, "SHA-256")
}

private fun hashString(input: String, algorithm: String = "SHA-256"): String {
    return MessageDigest
        .getInstance(algorithm)
        .digest(input.toByteArray())
        .toString()
}
