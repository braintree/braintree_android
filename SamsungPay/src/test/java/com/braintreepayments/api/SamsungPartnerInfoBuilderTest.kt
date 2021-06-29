package com.braintreepayments.api

import org.junit.Assert.assertEquals
import org.junit.Test

class SamsungPartnerInfoBuilderTest {

    // language=JSON
    val samsungConfigJSON = """
        {
          "clientApiUrl": "client_api_url",
          "environment": "test",
          "merchantId": "integration_merchant_id",
          "merchantAccountId": "integration_merchant_account_id",
          "creditCards": {
            "supportedCardTypes": [
              "American Express",
              "Discover",
              "JCB",
              "MasterCard",
              "Visa"
            ]
          },
          "samsungPay": {
            "displayName": "some example merchant",
            "serviceId": "samsung-service-id",
            "environment": "SANDBOX",
            "supportedCardBrands": [
              "american_express",
              "discover",
              "mastercard",
              "visa"
            ],
            "samsungAuthorization": "example-samsung-authorization"
          }
        }    
    """

    @Test
    fun build_setsSamsungServiceIdFromConfiguration() {
        val configuration = Configuration.fromJson(samsungConfigJSON)

        val partnerInfo = SamsungPartnerInfoBuilder()
                .setConfiguration(configuration)
                .build()

        assertEquals("samsung-service-id", partnerInfo.serviceId)
    }
}