package com.braintreepayments.demo

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.shopperinsights.ButtonOrder
import com.braintreepayments.api.shopperinsights.ButtonType
import com.braintreepayments.api.shopperinsights.ExperimentType
import com.braintreepayments.api.shopperinsights.PageType
import com.braintreepayments.api.shopperinsights.PresentmentDetails
import com.braintreepayments.api.shopperinsights.v2.CustomerRecommendationsResult
import com.braintreepayments.api.shopperinsights.v2.CustomerSessionRequest
import com.braintreepayments.api.shopperinsights.v2.CustomerSessionResult
import com.braintreepayments.api.shopperinsights.v2.PayPalCampaign
import com.braintreepayments.api.shopperinsights.v2.PaymentOptions
import com.braintreepayments.api.shopperinsights.v2.ShopperInsightsClientV2
import java.security.MessageDigest
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalBetaApi::class)
class ShopperInsightsV2ViewModel : ViewModel() {
    private lateinit var shopperInsightsClient: ShopperInsightsClientV2

    private val _sessionId = MutableStateFlow<String>("94f0b2db-5323-4d86-add3-paypal000000")
    val sessionId: StateFlow<String> = _sessionId

    private val _payPalCampaigns = MutableStateFlow<List<String>>(emptyList())
    val payPalCampaigns: StateFlow<List<String>> = _payPalCampaigns
    @OptIn(ExperimentalBetaApi::class)
    var recommendations = MutableStateFlow<List<PaymentOptions>>(emptyList())
    var isInPayPalNetwork = MutableStateFlow<Boolean>(false)
    var recommendationsCompleted = MutableStateFlow<Boolean>(false)

    private val _userErrors = MutableSharedFlow<String>()
    val userErrors: SharedFlow<String> = _userErrors.asSharedFlow()

    fun initShopperInsightsClient(context: Context, authString: String) {
        shopperInsightsClient = ShopperInsightsClientV2(context, authString)
    }

    fun handleCreateCustomerSession(emailText: String, nationalNumberText: String) {
        val customerSessionRequest = CustomerSessionRequest(
            hashedEmail = emailText.sha256Hex(),
            hashedPhoneNumber = nationalNumberText.sha256Hex(),
            payPalCampaigns = _payPalCampaigns.value.map { PayPalCampaign(it) }
        )

        _sessionId.update { "" }

        shopperInsightsClient.createCustomerSession(customerSessionRequest) { result ->
            when (result) {
                is CustomerSessionResult.Success -> {
                    _sessionId.update { result.sessionId }
                }
                is CustomerSessionResult.Failure -> {
                    viewModelScope.launch {
                        _userErrors.emit("CreateCustomerSession failed: ${result.error}")
                    }
                }
            }
        }
    }

    fun handleUpdateCustomerSession(
        emailText: String,
        nationalNumberText: String,
        sessionId: String
    ) {
        val customerSessionRequest = CustomerSessionRequest(
            hashedEmail = emailText.sha256Hex(),
            hashedPhoneNumber = nationalNumberText.sha256Hex(),
            payPalCampaigns = _payPalCampaigns.value.map { PayPalCampaign(it) }
        )

        _sessionId.update { "" }

        shopperInsightsClient.updateCustomerSession(customerSessionRequest, sessionId) { result ->
            when (result) {
                is CustomerSessionResult.Success -> {
                    _sessionId.update { result.sessionId }
                }
                is CustomerSessionResult.Failure -> {
                    viewModelScope.launch {
                        _userErrors.emit("UpdateCustomerSession failed: ${result.error}")
                    }
                }
            }
        }
    }

    fun handleGetRecommendations(sessionId: String) {
        shopperInsightsClient.generateCustomerRecommendations(
            sessionId = sessionId,
            payPalCampaigns = _payPalCampaigns.value.map { PayPalCampaign(it) }
        ) { result ->
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

                    viewModelScope.launch {
                        _userErrors.emit("GetRecommendations failed: ${result.error}")
                    }
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

    fun addPayPalCampaign(campaign: String) {
        val trimmed = campaign.trim()
        if (trimmed.isNotEmpty()) {
            _payPalCampaigns.update { it + trimmed }
        }
    }

    fun removePayPalCampaignAt(index: Int) {
        _payPalCampaigns.update { list -> list.filterIndexed { i, _ -> i != index } }
    }
}

private fun String.sha256Hex(): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(toByteArray(Charsets.UTF_8))
    return digest.joinToString("") { byte -> "%02x".format(0xFF and byte.toInt()) }
}
