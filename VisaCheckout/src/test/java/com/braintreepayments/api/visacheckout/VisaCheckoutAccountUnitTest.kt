package com.braintreepayments.api.visacheckout

import com.braintreepayments.api.visacheckout.VisaCheckoutAccount
import com.visa.checkout.VisaPaymentSummary
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import org.json.JSONException
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode

@RunWith(RobolectricTestRunner::class)
class VisaCheckoutAccountUnitTest {

    private lateinit var visaPaymentSummary: VisaPaymentSummary

    @Before
    fun beforeEach() {
        visaPaymentSummary = mockk(relaxed = true)
    }

    @Test
    @Throws(JSONException::class)
    fun build_withNullVisaPaymentSummary_buildsEmptyPaymentMethod() {
        val expectedBase = JSONObject()
            .put("visaCheckoutCard", JSONObject())
            .put(
                "_meta", JSONObject()
                    .put("source", "form")
                    .put("integration", "custom")
                    .put("sessionId", "1234")
                    .put("platform", "android")
            )
        val visaCheckoutAccount = VisaCheckoutAccount(null)
        visaCheckoutAccount.setSessionId("1234")
        val json = visaCheckoutAccount.buildJSON()
        JSONAssert.assertEquals(expectedBase, json, JSONCompareMode.STRICT)
    }

    @Test
    @Throws(JSONException::class)
    fun build_withVisaPaymentSummary_buildsExpectedPaymentMethod() {
        every { visaPaymentSummary.callId } returns "stubbedCallId"
        every { visaPaymentSummary.encKey } returns "stubbedEncKey"
        every { visaPaymentSummary.encPaymentData } returns "stubbedEncPaymentData"
        val visaCheckoutAccount = VisaCheckoutAccount(visaPaymentSummary)
        visaCheckoutAccount.setSessionId("1234")
        val json = visaCheckoutAccount.buildJSON()
        val expectedBase = JSONObject()
        val expectedPaymentMethodNonce = JSONObject()
        expectedPaymentMethodNonce.put("callId", "stubbedCallId")
        expectedPaymentMethodNonce.put("encryptedKey", "stubbedEncKey")
        expectedPaymentMethodNonce.put("encryptedPaymentData", "stubbedEncPaymentData")
        expectedBase.put("visaCheckoutCard", expectedPaymentMethodNonce)
        expectedBase.put(
            "_meta", JSONObject()
                .put("source", "form")
                .put("integration", "custom")
                .put("sessionId", "1234")
                .put("platform", "android")
        )
        JSONAssert.assertEquals(expectedBase, json, JSONCompareMode.STRICT)
    }

    @Test
    fun apiPath_returnsCorrectApiPath() {
        assertEquals("visa_checkout_cards", VisaCheckoutAccount(null).apiPath)
    }
}