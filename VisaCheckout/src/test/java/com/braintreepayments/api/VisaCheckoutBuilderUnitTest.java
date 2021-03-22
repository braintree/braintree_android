package com.braintreepayments.api;

import com.visa.checkout.VisaPaymentSummary;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import static junit.framework.Assert.assertEquals;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(RobolectricTestRunner.class)
@PowerMockIgnore({"org.powermock.*", "org.mockito.*", "org.robolectric.*", "android.*", "androidx.*"})
@PrepareForTest({ VisaPaymentSummary.class })
public class VisaCheckoutBuilderUnitTest {

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    private VisaPaymentSummary visaPaymentSummary;

    @Before
    public void beforeEach() throws Exception {
        visaPaymentSummary = PowerMockito.mock(VisaPaymentSummary.class);
        PowerMockito.whenNew(VisaPaymentSummary.class).withAnyArguments().thenReturn(visaPaymentSummary);
    }

    @Test
    public void build_withNullVisaPaymentSummary_buildsEmptyPaymentMethod() throws JSONException {
        JSONObject base = new JSONObject();
        JSONObject paymentMethodNonceJson = new JSONObject();
        JSONObject expectedBase = new JSONObject("{\"visaCheckoutCard\":{}}");

        VisaCheckoutBuilder visaCheckoutBuilder = new VisaCheckoutBuilder(null);
        visaCheckoutBuilder.buildJSON(base, paymentMethodNonceJson);

        JSONAssert.assertEquals(expectedBase, base, JSONCompareMode.STRICT);
    }

    @Test
    public void build_withVisaPaymentSummary_buildsExpectedPaymentMethod() throws JSONException {
        when(visaPaymentSummary.getCallId()).thenReturn("stubbedCallId");
        when(visaPaymentSummary.getEncKey()).thenReturn("stubbedEncKey");
        when(visaPaymentSummary.getEncPaymentData()).thenReturn("stubbedEncPaymentData");

        JSONObject base = new JSONObject();
        JSONObject paymentMethodNonceJson = new JSONObject();

        VisaCheckoutBuilder visaCheckoutBuilder = new VisaCheckoutBuilder(visaPaymentSummary);
        visaCheckoutBuilder.buildJSON(base, paymentMethodNonceJson);

        JSONObject expectedBase = new JSONObject();
        JSONObject expectedPaymentMethodNonce = new JSONObject();
        expectedPaymentMethodNonce.put("callId", "stubbedCallId");
        expectedPaymentMethodNonce.put("encryptedKey", "stubbedEncKey");
        expectedPaymentMethodNonce.put("encryptedPaymentData", "stubbedEncPaymentData");
        expectedBase.put("visaCheckoutCard", expectedPaymentMethodNonce);

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
