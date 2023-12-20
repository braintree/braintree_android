package com.braintreepayments.api

sealed class ShopperInsightsAnalytics(val event: String) {
    object PayPalPresented: ShopperInsightsAnalytics("shopper-insights:paypal-presented")
    object PayPalSelected: ShopperInsightsAnalytics("shopper-insights:paypal-selected")
    object VenmoPresented: ShopperInsightsAnalytics("shopper-insights:venmo-presented")
    object VenmoSelected: ShopperInsightsAnalytics("shopper-insights:venmo-selected")
}
