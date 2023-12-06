package com.braintreepayments.api

sealed class ShopperInsightResult {

    class Success(val response: ShopperInsightInfo) : ShopperInsightResult()

    class Failure(val error: Exception) : ShopperInsightResult()
}
