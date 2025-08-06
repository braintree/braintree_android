package com.braintreepayments.api.paypal

import com.braintreepayments.api.core.IntegrationType
import com.braintreepayments.api.core.MetadataBuilder
import org.json.JSONException
import org.json.JSONObject
import org.junit.Test
import org.junit.Assert.assertNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode

class PayPalAccountUnitTest {
    val PAYPAL_KEY = "paypalAccount"

    @Test
    @Throws(JSONException::class)
    fun `correctly builds a PayPal account`() {
        val sut = PayPalAccount(
            clientMetadataId = "correlation_id",
            urlResponseData = JSONObject(),
            intent = PayPalPaymentIntent.SALE,
            merchantAccountId = "alt_merchant_account_id",
            paymentType = "single-payment",
            sessionId = "session_id",
            source = "paypal-sdk",
            integration = IntegrationType.CUSTOM
        )

        val jsonObject = sut.buildJSON()
        val jsonAccount = jsonObject.getJSONObject(PAYPAL_KEY)
        val jsonMetadata = jsonObject.getJSONObject(MetadataBuilder.META_KEY)

        assertNull(jsonAccount.opt("details"))
        assertEquals("correlation_id", jsonAccount.getString("correlationId"))
        assertEquals(PayPalPaymentIntent.SALE, PayPalPaymentIntent.fromString(jsonAccount.getString("intent")))
        assertEquals("custom", jsonMetadata.getString("integration"))
        assertEquals("paypal-sdk", jsonMetadata.getString("source"))
        assertEquals("alt_merchant_account_id", jsonObject.getString("merchant_account_id"))
        assertFalse(jsonAccount.getJSONObject("options").getBoolean("validate"))
    }

    @Test
    @Throws(JSONException::class)
    fun `builds a PayPal account and uses correct metadata`() {
        val sut = PayPalAccount(
            clientMetadataId = "correlation_id",
            urlResponseData = JSONObject(),
            intent = PayPalPaymentIntent.SALE,
            merchantAccountId = "alt_merchant_account_id",
            paymentType = "single-payment",
            sessionId = "session_id",
            source = "paypal-app",
            integration = IntegrationType.CUSTOM
        )

        val jsonObject = sut.buildJSON()
        val jsonMetadata = jsonObject.getJSONObject(MetadataBuilder.META_KEY)

        assertEquals("custom", jsonMetadata.getString("integration"))
        assertEquals("paypal-app", jsonMetadata.getString("source"))
    }

    @Test
    @Throws(JSONException::class)
    fun `builds a PayPal account and sets integration method`() {
        val sut = PayPalAccount(
            clientMetadataId = "correlation_id",
            urlResponseData = JSONObject(),
            intent = PayPalPaymentIntent.SALE,
            merchantAccountId = "alt_merchant_account_id",
            paymentType = "single-payment",
            sessionId = "session_id",
            source = "form"
        )
        sut.integration = IntegrationType.CUSTOM

        val jsonObject = sut.buildJSON()
        val jsonMetadata = jsonObject.getJSONObject(MetadataBuilder.META_KEY)

        assertEquals(IntegrationType.CUSTOM.stringValue, jsonMetadata.getString("integration"))
    }

    @Test
    @Throws(JSONException::class)
    fun `builds a PayPal account with single payment and sets options validate as false`() {
        val sut = PayPalAccount(
            clientMetadataId = "correlation_id",
            urlResponseData = JSONObject(),
            intent = PayPalPaymentIntent.SALE,
            merchantAccountId = "alt_merchant_account_id",
            paymentType = "single-payment",
            sessionId = "session_id",
            source = "form",
            integration = IntegrationType.CUSTOM
        )

        val jsonObject = sut.buildJSON()
        val jsonAccount = jsonObject.getJSONObject(PAYPAL_KEY)

        assertFalse(jsonAccount.getJSONObject("options").getBoolean("validate"))
    }

    @Test
    @Throws(JSONException::class)
    fun `builds a PayPal account with billing agreement and does not set options validate`() {
        val sut = PayPalAccount(
            clientMetadataId = "correlation_id",
            urlResponseData = JSONObject(),
            intent = PayPalPaymentIntent.SALE,
            merchantAccountId = "alt_merchant_account_id",
            paymentType = "billing-agreement",
            sessionId = "session_id",
            source = "form",
            integration = IntegrationType.CUSTOM
        )

        val jsonObject = sut.buildJSON()
        val jsonAccount = jsonObject.getJSONObject(PAYPAL_KEY)

        assertFalse(jsonAccount.has("options"))
    }

    @Test
    @Throws(JSONException::class)
    fun `builds a PayPal account as an empty object and does not include it when serializing`() {
        val sut = PayPalAccount(
            clientMetadataId = null,
            urlResponseData = JSONObject(),
            intent = null,
            merchantAccountId = null,
            paymentType = null,
            sessionId = null,
            source = null,
            integration = null
        )

        val jsonObject = sut.buildJSON()
        val jsonAccount = jsonObject.getJSONObject(PAYPAL_KEY)

        assertEquals(0, jsonAccount.length())
    }

    @Test
    @Throws(JSONException::class)
    fun `builds a PayPal account and adds URL response data`() {
        val urlResponseData = JSONObject()
            .put("data1", "data1")
            .put("data2", "data2")
            .put("data3", "data3")

        val sut = PayPalAccount(
            clientMetadataId = null,
            urlResponseData = urlResponseData,
            intent = null,
            merchantAccountId = null,
            paymentType = null,
            sessionId = null,
            source = null,
            integration = null
        )

        val jsonObject = sut.buildJSON()
        val jsonAccount = jsonObject.getJSONObject(PAYPAL_KEY)

        val expectedPaymentMethodNonceJSON = JSONObject()
            .put("data1", "data1")
            .put("data2", "data2")
            .put("data3", "data3")

        JSONAssert.assertEquals(expectedPaymentMethodNonceJSON, jsonAccount, JSONCompareMode.NON_EXTENSIBLE)
    }

    @Test
    @Throws(JSONException::class)
    fun `builds a PayPal account with an empty intent and does not include it when serializing`() {
        val sut = PayPalAccount(
            clientMetadataId = "correlation_id",
            urlResponseData = JSONObject(),
            intent = null,
            merchantAccountId = "alt_merchant_account_id",
            paymentType = "billing-agreement",
            sessionId = "session_id",
            source = "form",
            integration = IntegrationType.CUSTOM
        )

        val jsonObject = sut.buildJSON()
        val jsonAccount = jsonObject.getJSONObject(PAYPAL_KEY)

        assertFalse(jsonAccount.has("intent"))
    }
}
