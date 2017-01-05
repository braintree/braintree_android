package com.braintreepayments.api.models;

import android.os.Parcel;

import com.visa.checkout.VisaPaymentSummary;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import static junit.framework.Assert.assertEquals;

@RunWith(RobolectricGradleTestRunner.class)
public class VisaCheckoutBuilderUnitTest {

    @Test
    public void build_withNullVisaPaymentSummary_buildsEmptyPaymentMethod() throws JSONException {
        JSONObject base = new JSONObject();
        JSONObject paymentMethodNonceJson = new JSONObject();

        JSONObject expectedBase = new JSONObject();
        JSONObject expectedPaymentMethodNonce = new JSONObject();
        expectedPaymentMethodNonce.put("callId", null);
        expectedPaymentMethodNonce.put("encryptedKey", null);
        expectedPaymentMethodNonce.put("encryptedPaymentData", null);
        expectedBase.put("visaCheckoutCard", expectedPaymentMethodNonce);

        VisaCheckoutBuilder visaCheckoutBuilder = new VisaCheckoutBuilder(null);
        visaCheckoutBuilder.build(base, paymentMethodNonceJson);

        JSONAssert.assertEquals(expectedBase, base, JSONCompareMode.STRICT);
    }

    @Test
    public void build_withVisaPaymentSummary_buildsExpectedPaymentMethod() throws JSONException {
        JSONObject base = new JSONObject();
        JSONObject paymentMethodNonceJson = new JSONObject();

        Parcel in = Parcel.obtain();
        in.writeLong(1);
        in.writeString("US");
        in.writeString("90210");
        in.writeString("1234");
        in.writeString("VISA");
        in.writeString("Credit");
        in.writeString("stubbedEncPaymentData");
        in.writeString("stubbedEncKey");
        in.writeString("stubbedCallId");
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
        assertEquals(VisaCheckoutPaymentMethodNonce.TYPE,
                new VisaCheckoutBuilder(null).getResponsePaymentMethodType());
    }
}
