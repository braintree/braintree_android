package com.braintreepayments.demo

import android.content.Context
import androidx.lifecycle.ViewModel
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.shopperinsights.ButtonOrder
import com.braintreepayments.api.shopperinsights.ButtonType
import com.braintreepayments.api.shopperinsights.ExperimentType
import com.braintreepayments.api.shopperinsights.PageType
import com.braintreepayments.api.shopperinsights.PresentmentDetails
import com.braintreepayments.api.shopperinsights.v2.PaymentOptions
import com.braintreepayments.api.shopperinsights.v2.ShopperInsightsClientV2
import java.security.MessageDigest
import kotlinx.coroutines.flow.MutableStateFlow

@OptIn(ExperimentalBetaApi::class)
class ShopperInsightsV2ViewModel : ViewModel() {
    private lateinit var shopperInsightsClient: ShopperInsightsClientV2

    var sessionId = MutableStateFlow<String>("")
    @OptIn(ExperimentalBetaApi::class)
    var recommendations = MutableStateFlow<List<PaymentOptions>>(emptyList())
    var isInPayPalNetwork = MutableStateFlow<Boolean>(false)

    fun initShopperInsightsClient(context: Context, authString: String) {
        shopperInsightsClient = ShopperInsightsClientV2(context, authString)
    }

    fun sendButtonPresentedEvent(buttonType: ButtonType, sessionId: String) {
        shopperInsightsClient.sendPresentedEvent(
            buttonType,
            PresentmentDetails(ExperimentType.CONTROL, ButtonOrder.FIRST, PageType.OTHER),
            sessionId
        )
    }
}

fun String.sha256(): String {
    return hashString(this, "SHA-256")
}

private fun hashString(input: String, algorithm: String = "SHA-256"): String {
    return MessageDigest
        .getInstance(algorithm)
        .digest(input.toByteArray())
        .toString()
}
