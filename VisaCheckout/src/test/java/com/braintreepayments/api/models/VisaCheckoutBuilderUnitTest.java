package com.braintreepayments.api.models;

import android.os.Parcel;

import com.visa.checkout.VisaPaymentSummary;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import static junit.framework.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class VisaCheckoutBuilderUnitTest {

    @Test
    public void build_withNullVisaPaymentSummary_buildsEmptyPaymentMethod() throws JSONException {
        JSONObject base = new JSONObject();
        JSONObject paymentMethodNonceJson = new JSONObject();
        JSONObject expectedBase = new JSONObject("{\"visaCheckoutCard\":{}}");

        VisaCheckoutBuilder visaCheckoutBuilder = new VisaCheckoutBuilder(null);
        visaCheckoutBuilder.build(base, paymentMethodNonceJson);

        JSONAssert.assertEquals(expectedBase, base, JSONCompareMode.STRICT);
    }

    @Test
    public void build_withVisaPaymentSummary_buildsExpectedPaymentMethod() throws JSONException {
        JSONObject base = new JSONObject();
        JSONObject paymentMethodNonceJson = new JSONObject();

        JSONObject summaryJson = new JSONObject()
                .put("encPaymentData", "stubbedEncPaymentData")
                .put("encKey", "stubbedEncKey")
                .put("callid", "stubbedCallId");

        Parcel in = Parcel.obtain();
        in.writeString("SUCCESS");
        in.writeString(summaryJson.toString());
        in.setDataPosition(0);

        VisaPaymentSummary visaPaymentSummary = VisaPaymentSummary.CREATOR.createFromParcel(in);

        JSONObject expectedBase = new JSONObject();
        JSONObject expectedPaymentMethodNonce = new JSONObject();
        expectedPaymentMethodNonce.put("callId", "stubbedCallId");
        expectedPaymentMethodNonce.put("encryptedKey", "stubbedEncKey");
        expectedPaymentMethodNonce.put("encryptedPaymentData", "stubbedEncPaymentData");
        expectedBase.put("visaCheckoutCard", expectedPaymentMethodNonce);

        VisaCheckoutBuilder visaCheckoutBuilder = new VisaCheckoutBuilder(visaPaymentSummary);
        visaCheckoutBuilder.build(base, paymentMethodNonceJson);

        JSONAssert.assertEquals(expectedBase, base, JSONCompareMode.STRICT);
    }

    @Test
    public void getApiPath_returnsCorrectApiPath() {
        assertEquals("visa_checkout_cards", new VisaCheckoutBuilder(null).getApiPath());
    }

    @Test
    public void getResponsePaymentMethodType_returnsCorrectPaymentMethodType() {
        assertEquals(VisaCheckoutNonce.TYPE,
                new VisaCheckoutBuilder(null).getResponsePaymentMethodType());
    }
}
