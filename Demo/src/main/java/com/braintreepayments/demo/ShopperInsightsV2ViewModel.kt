package com.braintreepayments.demo

import androidx.lifecycle.ViewModel
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.shopperinsights.v2.PaymentOptions
import kotlinx.coroutines.flow.MutableStateFlow

class ShopperInsightsV2ViewModel : ViewModel() {
    var sessionId = MutableStateFlow<String>("")
    @OptIn(ExperimentalBetaApi::class)
    var recommendations = MutableStateFlow<List<PaymentOptions>>(emptyList())
    var isInPayPalNetwork = MutableStateFlow<Boolean>(false)
}
