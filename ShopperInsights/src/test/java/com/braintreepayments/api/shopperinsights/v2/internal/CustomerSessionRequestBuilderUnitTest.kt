package com.braintreepayments.api.shopperinsights.v2.internal

import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.shopperinsights.v2.CustomerSessionRequest
import com.braintreepayments.api.shopperinsights.v2.ShopperInsightsCampaign
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
            purchaseUnits = null,
            payPalCampaigns = null
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
        assertNull(result.campaigns)
    }

    @Test
    fun `createRequestObjects builds correct JSON array when campaigns are provided`() {
        val customerSessionRequest = CustomerSessionRequest(
            payPalCampaigns = listOf(
                ShopperInsightsCampaign(id = "campaign-1"),
                ShopperInsightsCampaign(id = "campaign-2")
            )
        )

        val result = requestBuilder.createRequestObjects(customerSessionRequest)

        val expectedCampaigns = JSONArray().apply {
            put(JSONObject().put("id", "campaign-1"))
            put(JSONObject().put("id", "campaign-2"))
        }

        JSONAssert.assertEquals(expectedCampaigns, result.campaigns, true)
    }

    @Test
    fun `createRequestObjects returns null campaigns when list is empty`() {
        val customerSessionRequest = CustomerSessionRequest(payPalCampaigns = emptyList())

        val result = requestBuilder.createRequestObjects(customerSessionRequest)

        assertNull(result.campaigns)
    }

    @Test
    fun `createRequestObjects returns null campaigns when list is null`() {
        val customerSessionRequest = CustomerSessionRequest(payPalCampaigns = null)

        val result = requestBuilder.createRequestObjects(customerSessionRequest)

        assertNull(result.campaigns)
    }
}
