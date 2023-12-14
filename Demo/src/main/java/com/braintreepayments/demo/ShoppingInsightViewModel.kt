package com.braintreepayments.demo

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.braintreepayments.api.BuyerPhone
import com.braintreepayments.api.ShopperInsightsRequest
import com.braintreepayments.api.ShopperInsightsResult

/**
 * ViewModel for handling shopping insights.
 */
class ShoppingInsightViewModel : ViewModel() {

    private val shopperInsightsLiveData = MutableLiveData<ShopperInsightsResult>()

    /**
     * Fetches recommended payment methods using the provided buyer details.
     *
     * @param email The email address of the user.
     * @param countryCode The country code for the user's phone number.
     * @param nationalNumber The national number part of the user's phone number.
     * @return MutableLiveData containing ShopperInsightsResult.
     */
    fun getRecommendedPaymentMethods(email: String, countryCode: String, nationalNumber: String): MutableLiveData<ShopperInsightsResult> {
        val request = ShopperInsightsRequest(email, BuyerPhone(countryCode, nationalNumber))

        // TODO: Call Shopping Insight Client
        // shopperInsightsClient.getRecommendedPaymentMethods(request) { result ->
        //     shopperInsightsLiveData.postValue(result)
        // }

        return shopperInsightsLiveData
    }
}
