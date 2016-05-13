package com.braintreepayments.api.models;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

@RunWith(RobolectricGradleTestRunner.class)
public class UnionPayCardBuilderUnitTest {

    private UnionPayCardBuilder mUnionPayCardBuilder;
    private UnionPayCardBuilder mFilledUnionPayCardBuilder;

    @Before
    public void setup() {
        mUnionPayCardBuilder = new UnionPayCardBuilder();
        mFilledUnionPayCardBuilder = new UnionPayCardBuilder()
                .cvv("123")
                .enrollmentId("enrollment-id")
                .expirationYear("expiration-year")
                .expirationMonth("expiration-month")
                .cardNumber("card-number")
                .expirationDate("expiration-date")
                .mobileCountryCode("mobile-country-code")
                .mobilePhoneNumber("mobile-phone-number")
                .smsCode("sms-code")
                .integration("integration")
                .setSessionId("session-id")
                .source("source")
                .validate(false);
    }

    @Test
    public void getApiPath_returnsExpected() {
        assertEquals("credit_cards", mUnionPayCardBuilder.getApiPath());
    }

    @Test
    public void getResponsePaymentMethodType_returnsExpected() {
        assertEquals("CreditCard", mUnionPayCardBuilder.getResponsePaymentMethodType());
    }

    @Test
    public void cardNumber_addsToJson() throws JSONException {
        mUnionPayCardBuilder.cardNumber("myCardNumber");

        assertEquals("myCardNumber", mUnionPayCardBuilder.buildEnrollment()
                .getJSONObject("unionPayEnrollment")
                .getString("number"));
    }

    @Test
    public void expirationMonth_addsToJson() throws JSONException {
        mUnionPayCardBuilder.expirationMonth("12");

        assertEquals("12", mUnionPayCardBuilder.buildEnrollment()
                .getJSONObject("unionPayEnrollment")
                .getString("expirationMonth"));
    }

    @Test
    public void expirationYear_addsToJson() throws JSONException {
        mUnionPayCardBuilder.expirationYear("2020");

        assertEquals("2020", mUnionPayCardBuilder.buildEnrollment()
                .getJSONObject("unionPayEnrollment")
                .getString("expirationYear"));
    }

    @Test
    public void expirationDate_addsToJson() throws JSONException {
        mUnionPayCardBuilder.expirationDate("12/2020");

        assertEquals("12/2020", mUnionPayCardBuilder.buildEnrollment()
                .getJSONObject("unionPayEnrollment")
                .getString("expirationDate"));
    }

    @Test
    public void mobileCountryCode_addsToJson() throws JSONException {
        mUnionPayCardBuilder.mobileCountryCode("1");

        assertEquals("1", mUnionPayCardBuilder.buildEnrollment()
                .getJSONObject("unionPayEnrollment")
                .getString("mobileCountryCode"));
    }

    @Test
    public void mobilePhoneNumber_addsToJson() throws JSONException {
        mUnionPayCardBuilder.mobilePhoneNumber("867-5309");

        assertEquals("867-5309", mUnionPayCardBuilder.buildEnrollment()
                .getJSONObject("unionPayEnrollment")
                .getString("mobileNumber"));
    }

    @Test
    public void smsCode_addsToOptionsJson() throws JSONException {
        JSONObject beforeJson = new JSONObject(mUnionPayCardBuilder.build());
        JSONObject afterJson = new JSONObject(mUnionPayCardBuilder.smsCode("mySmsCode").build());

        assertFalse(beforeJson.getJSONObject("options").has("smsCode"));
        assertEquals("mySmsCode", afterJson.getJSONObject("options").getString("smsCode"));
    }

    @Test
    public void enrollmentId_addsToOptionsJson() throws JSONException {
        JSONObject beforeJson = new JSONObject(mUnionPayCardBuilder.build());
        JSONObject afterJson = new JSONObject(mUnionPayCardBuilder.enrollmentId("myEnrollmentId").build());

        assertFalse(beforeJson.getJSONObject("options").has("id"));
        assertEquals("myEnrollmentId", afterJson.getJSONObject("options").getString("id"));
    }

    @Test
    public void buildEnrollment_createsUnionPayEnrollmentJson() throws JSONException {
        JSONObject enrollmentPayload = mFilledUnionPayCardBuilder.buildEnrollment();

        JSONObject unionPayEnrollment = enrollmentPayload.getJSONObject("unionPayEnrollment");
        assertEquals("card-number", unionPayEnrollment.getString("number"));
        assertEquals("expiration-month", unionPayEnrollment.getString("expirationMonth"));
        assertEquals("expiration-year", unionPayEnrollment.getString("expirationYear"));
        assertEquals("expiration-date", unionPayEnrollment.getString("expirationDate"));
        assertEquals("mobile-country-code", unionPayEnrollment.getString("mobileCountryCode"));
        assertEquals("mobile-phone-number", unionPayEnrollment.getString("mobileNumber"));
    }

    @Test
    public void build_createsUnionPayTokenizeJson() throws JSONException {
        JSONObject tokenizePayload = new JSONObject(mFilledUnionPayCardBuilder.build());

        JSONObject creditCard = tokenizePayload.getJSONObject("creditCard");
        assertEquals("card-number", creditCard.getString("number"));
        assertEquals("expiration-month", creditCard.getString("expirationMonth"));
        assertEquals("expiration-year", creditCard.getString("expirationYear"));
        assertEquals("expiration-date", creditCard.getString("expirationDate"));
        assertEquals("123", creditCard.getString("cvv"));

        JSONObject options = tokenizePayload.getJSONObject("options");
        assertEquals("enrollment-id", options.getString("id"));
        assertEquals("sms-code", options.getString("smsCode"));
    }
}
