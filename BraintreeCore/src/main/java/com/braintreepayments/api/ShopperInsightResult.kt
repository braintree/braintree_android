package com.braintreepayments.api

sealed class ShopperInsightResult {

    class Success(val response: ShopperInsightResponse) : ShopperInsightResult()

    class Failure(val error: Exception) : ShopperInsightResult()
}
