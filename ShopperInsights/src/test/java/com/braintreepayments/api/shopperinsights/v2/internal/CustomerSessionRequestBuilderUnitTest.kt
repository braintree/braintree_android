package com.braintreepayments.api.shopperinsights.v2.internal

import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.shopperinsights.v2.CustomerSessionRequest
import com.braintreepayments.api.shopperinsights.v2.PayPalCampaign
import com.braintreepayments.api.shopperinsights.v2.PurchaseUnit
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert
import kotlin.test.assertNull

@ExperimentalBetaApi
class CustomerSessionRequestBuilderUnitTest {

    private val requestBuilder = CustomerSessionRequestBuilder()

    @Test
    fun `createRequestObjects builds correct JSON objects when purchaseUnits are provided`() {
        val customerSessionRequest = CustomerSessionRequest(
            hashedEmail = "hashedEmail",
            hashedPhoneNumber = "hashedPhoneNumber",
            payPalAppInstalled = true,
            venmoAppInstalled = false,
            purchaseUnits = listOf(
                PurchaseUnit(amount = "100.00", currencyCode = "USD"),
                PurchaseUnit(amount = "200.00", currencyCode = "EUR")
            )
        )

        val result = requestBuilder.createRequestObjects(customerSessionRequest)

        val expectedCustomer = JSONObject().apply {
            put("hashedEmail", "hashedEmail")
            put("hashedPhoneNumber", "hashedPhoneNumber")
            put("paypalAppInstalled", true)
            put("venmoAppInstalled", false)
        }

        val expectedPurchaseUnits = JSONArray().apply {
            put(JSONObject().apply {
                put("amount", JSONObject().apply {
                    put("value", "100.00")
                    put("currencyCode", "USD")
                })
            })
            put(JSONObject().apply {
                put("amount", JSONObject().apply {
                    put("value", "200.00")
                    put("currencyCode", "EUR")
                })
            })
        }

        JSONAssert.assertEquals(expectedCustomer, result.customer, false)
        JSONAssert.assertEquals(expectedPurchaseUnits, result.purchaseUnits, false)
    }

    @Test
    fun `createRequestObjects builds correct JSON objects when purchaseUnits are null`() {
        val customerSessionRequest = CustomerSessionRequest(
            hashedEmail = "hashedEmail",
            hashedPhoneNumber = "hashedPhoneNumber",
            payPalAppInstalled = true,
            venmoAppInstalled = false,
            purchaseUnits = null
        )

        val result = requestBuilder.createRequestObjects(customerSessionRequest)

        val expectedCustomer = JSONObject().apply {
            put("hashedEmail", "hashedEmail")
            put("hashedPhoneNumber", "hashedPhoneNumber")
            put("paypalAppInstalled", true)
            put("venmoAppInstalled", false)
        }

        JSONAssert.assertEquals(expectedCustomer, result.customer, false)
        assertNull(result.purchaseUnits)
    }

    @Test
    fun `createRequestObjects includes payPalCampaigns when provided`() {
        val customerSessionRequest = CustomerSessionRequest(
            hashedEmail = "hashedEmail",
            hashedPhoneNumber = "hashedPhoneNumber",
            payPalAppInstalled = true,
            venmoAppInstalled = false,
            purchaseUnits = null,
            payPalCampaigns = listOf(
                PayPalCampaign(id = "PPL-TT-10OFF"),
                PayPalCampaign(id = "PPL-OTHER-MOCK")
            )
        )

        val result = requestBuilder.createRequestObjects(customerSessionRequest)

        val expectedCampaigns = JSONArray().apply {
            put(JSONObject().put("id", "PPL-TT-10OFF"))
            put(JSONObject().put("id", "PPL-OTHER-MOCK"))
        }

        JSONAssert.assertEquals(expectedCampaigns, result.payPalCampaigns, false)
    }

    @Test
    fun `createRequestObjects omits payPalCampaigns when null or empty`() {
        val withNull = CustomerSessionRequest(
            hashedEmail = "e",
            hashedPhoneNumber = "p",
            payPalAppInstalled = false,
            venmoAppInstalled = false,
            purchaseUnits = null,
            payPalCampaigns = null
        )
        assertNull(requestBuilder.createRequestObjects(withNull).payPalCampaigns)

        val withEmpty = withNull.copy(payPalCampaigns = emptyList())
        assertNull(requestBuilder.createRequestObjects(withEmpty).payPalCampaigns)
    }
}
