package com.braintreepayments.api;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.braintreepayments.api.BaseCardBuilder.BILLING_ADDRESS_KEY;
import static com.braintreepayments.api.BaseCardBuilder.CREDIT_CARD_KEY;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;

@RunWith(RobolectricTestRunner.class)
public class UnionPayCardBuilderUnitTest {

    @Test
    public void getApiPath_returnsExpected() {
        assertEquals("credit_cards", new UnionPayCardBuilder().getApiPath());
    }

    @Test
    public void getResponsePaymentMethodType_returnsExpected() {
        assertEquals("CreditCard", new UnionPayCardBuilder().getResponsePaymentMethodType());
    }

    @Test
    public void cardNumber_addsToJson() throws JSONException {
        UnionPayCardBuilder sut = new UnionPayCardBuilder();
        sut.cardNumber("myCardNumber");

        assertEquals("myCardNumber", sut.buildEnrollment()
                .getJSONObject("unionPayEnrollment")
                .getString("number"));
    }

    @Test
    public void expirationMonth_addsToJson() throws JSONException {
        UnionPayCardBuilder sut = new UnionPayCardBuilder();
        sut.expirationMonth("12");

        assertEquals("12", sut.buildEnrollment()
                .getJSONObject("unionPayEnrollment")
                .getString("expirationMonth"));
    }

    @Test
    public void expirationYear_addsToJson() throws JSONException {
        UnionPayCardBuilder sut = new UnionPayCardBuilder();
        sut.expirationYear("2020");

        assertEquals("2020", sut.buildEnrollment()
                .getJSONObject("unionPayEnrollment")
                .getString("expirationYear"));
    }

    @Test
    public void expirationDate_addsToJsonAsExpirationMonthAndExpirationYear() throws JSONException {
        UnionPayCardBuilder sut = new UnionPayCardBuilder();
        sut.expirationDate("12/2020");

        JSONObject enrollment = sut.buildEnrollment().getJSONObject("unionPayEnrollment");
        assertEquals("12", enrollment.getString("expirationMonth"));
        assertEquals("2020", enrollment.getString("expirationYear"));
    }

    @Test
    public void mobileCountryCode_addsToJson() throws JSONException {
        UnionPayCardBuilder sut = new UnionPayCardBuilder();
        sut.mobileCountryCode("1");

        assertEquals("1", sut.buildEnrollment()
                .getJSONObject("unionPayEnrollment")
                .getString("mobileCountryCode"));
    }

    @Test
    public void mobilePhoneNumber_addsToJson() throws JSONException {
        UnionPayCardBuilder sut = new UnionPayCardBuilder();
        sut.mobilePhoneNumber("867-5309");

        assertEquals("867-5309", sut.buildEnrollment()
                .getJSONObject("unionPayEnrollment")
                .getString("mobileNumber"));
    }

    @Test
    public void smsCode_addsToOptionsJson() throws JSONException {
        UnionPayCardBuilder sut = new UnionPayCardBuilder();
        sut.smsCode("mySmsCode");

        JSONObject jsonObject = new JSONObject(sut.build());
        assertEquals("mySmsCode", jsonObject.getJSONObject("creditCard")
                .getJSONObject("options")
                .getJSONObject("unionPayEnrollment")
                .getString("smsCode"));
    }

    @Test
    public void enrollmentId_addsToOptionsJson() throws JSONException {
        UnionPayCardBuilder sut = new UnionPayCardBuilder();
        sut.enrollmentId("myEnrollmentId");

        JSONObject jsonObject = new JSONObject(sut.build());
        assertEquals("myEnrollmentId", jsonObject.getJSONObject("creditCard")
                .getJSONObject("options")
                .getJSONObject("unionPayEnrollment")
                .getString("id"));
    }

    @Test
    public void doesNotIncludeEmptyStrings() throws JSONException {
        UnionPayCardBuilder sut = new UnionPayCardBuilder();
        sut.cardNumber("");
        sut.expirationDate("");
        sut.expirationMonth("");
        sut.expirationYear("");
        sut.cvv("");
        sut.postalCode("");
        sut.cardholderName("");
        sut.firstName("");
        sut.lastName("");
        sut.streetAddress("");
        sut.locality("");
        sut.postalCode("");
        sut.region("");
        sut.enrollmentId("");
        sut.mobileCountryCode("");
        sut.mobilePhoneNumber("");
        sut.smsCode("");

        assertEquals("{\"options\":{\"unionPayEnrollment\":{}}}",
                new JSONObject(sut.build()).getJSONObject(CREDIT_CARD_KEY).toString());
        assertFalse(new JSONObject(sut.build()).has(BILLING_ADDRESS_KEY));
    }

    @Test
    public void buildEnrollment_createsUnionPayEnrollmentJson() throws JSONException {
        UnionPayCardBuilder sut = new UnionPayCardBuilder();
        sut.cvv("123");
        sut.enrollmentId("enrollment-id");
        sut.expirationYear("expiration-year");
        sut.expirationMonth("expiration-month");
        sut.cardNumber("card-number");
        sut.mobileCountryCode("mobile-country-code");
        sut.mobilePhoneNumber("mobile-phone-number");
        sut.smsCode("sms-code");
        sut.integration("integration");
        sut.setSessionId("session-id");
        sut.source("source");
        sut.validate(true);

        JSONObject unionPayEnrollment = sut.buildEnrollment().getJSONObject("unionPayEnrollment");

        assertEquals("card-number", unionPayEnrollment.getString("number"));
        assertEquals("expiration-month", unionPayEnrollment.getString("expirationMonth"));
        assertEquals("expiration-year", unionPayEnrollment.getString("expirationYear"));
        assertEquals("mobile-country-code", unionPayEnrollment.getString("mobileCountryCode"));
        assertEquals("mobile-phone-number", unionPayEnrollment.getString("mobileNumber"));
    }

    @Test
    public void build_createsUnionPayTokenizeJson() throws JSONException {
        UnionPayCardBuilder sut = new UnionPayCardBuilder();
        sut.cvv("123");
        sut.enrollmentId("enrollment-id");
        sut.expirationYear("expiration-year");
        sut.expirationMonth("expiration-month");
        sut.cardNumber("card-number");
        sut.mobileCountryCode("mobile-country-code");
        sut.mobilePhoneNumber("mobile-phone-number");
        sut.smsCode("sms-code");
        sut.integration("integration");
        sut.setSessionId("session-id");
        sut.source("source");

        JSONObject tokenizePayload = new JSONObject(sut.build());

        JSONObject creditCard = tokenizePayload.getJSONObject("creditCard");
        assertEquals("card-number", creditCard.getString("number"));
        assertEquals("expiration-month", creditCard.getString("expirationMonth"));
        assertEquals("expiration-year", creditCard.getString("expirationYear"));
        assertEquals("123", creditCard.getString("cvv"));

        JSONObject options = creditCard.getJSONObject("options");
        JSONObject unionPayEnrollment = options.getJSONObject("unionPayEnrollment");
        assertEquals("enrollment-id", unionPayEnrollment.getString("id"));
        assertEquals("sms-code", unionPayEnrollment.getString("smsCode"));
    }

    @Test
    public void build_doesNotIncludeValidate() throws JSONException {
        UnionPayCardBuilder unionPayCardBuilderValidateTrue = new UnionPayCardBuilder();
        unionPayCardBuilderValidateTrue.validate(true);

        UnionPayCardBuilder unionPayCardBuilderValidateFalse = new UnionPayCardBuilder();
        unionPayCardBuilderValidateFalse.validate(false);

        JSONObject optionsValidateTrue = new JSONObject(unionPayCardBuilderValidateTrue.build())
                .getJSONObject("creditCard")
                .getJSONObject("options");
        JSONObject optionsValidateFalse = new JSONObject(unionPayCardBuilderValidateFalse.build())
                .getJSONObject("creditCard")
                .getJSONObject("options");

        assertFalse(optionsValidateTrue.has("validate"));
        assertFalse(optionsValidateFalse.has("validate"));
    }

    @Test
    public void build_standardPayload() throws JSONException {
        UnionPayCardBuilder sut = new UnionPayCardBuilder();
        sut.cardNumber("someCardNumber");
        sut.expirationMonth("expirationMonth");
        sut.expirationYear("expirationYear");
        sut.cvv("cvv");
        sut.enrollmentId("enrollmentId");
        sut.smsCode("smsCode");
        sut.validate(true);

        String result = sut.build();
        JSONObject tokenizePayload = new JSONObject(result);
        JSONObject creditCardPayload = tokenizePayload.getJSONObject("creditCard");
        JSONObject optionsPayload = creditCardPayload.getJSONObject("options");
        JSONObject unionPayEnrollmentPayload = optionsPayload.getJSONObject("unionPayEnrollment");

        assertEquals("someCardNumber", creditCardPayload.getString("number"));
        assertEquals("expirationMonth", creditCardPayload.getString("expirationMonth"));
        assertEquals("expirationYear", creditCardPayload.getString("expirationYear"));
        assertEquals("cvv", creditCardPayload.getString("cvv"));

        assertFalse(optionsPayload.has("validate"));
        assertEquals("enrollmentId", unionPayEnrollmentPayload.getString("id"));
        assertEquals("smsCode", unionPayEnrollmentPayload.getString("smsCode"));
    }

    @Test
    public void build_optionalSmsCode() throws JSONException {
        UnionPayCardBuilder sut = new UnionPayCardBuilder();
        sut.cardNumber("someCardNumber");
        sut.expirationMonth("expirationMonth");
        sut.expirationYear("expirationYear");
        sut.cvv("cvv");
        sut.enrollmentId("enrollmentId");
        sut.validate(true);

        String result = sut.build();
        JSONObject tokenizePayload = new JSONObject(result);
        JSONObject creditCardPayload = tokenizePayload.getJSONObject("creditCard");
        JSONObject optionsPayload = creditCardPayload.getJSONObject("options");
        JSONObject unionPayEnrollmentPayload = optionsPayload.getJSONObject("unionPayEnrollment");

        assertEquals("someCardNumber", creditCardPayload.getString("number"));
        assertEquals("expirationMonth", creditCardPayload.getString("expirationMonth"));
        assertEquals("expirationYear", creditCardPayload.getString("expirationYear"));
        assertEquals("cvv", creditCardPayload.getString("cvv"));

        assertFalse(optionsPayload.has("validate"));
        assertEquals("enrollmentId", unionPayEnrollmentPayload.getString("id"));
        assertFalse(unionPayEnrollmentPayload.has("smsCode"));
    }

    @Test
    public void build_doesNotIncludeCvv() throws JSONException {
        UnionPayCardBuilder sut = new UnionPayCardBuilder();
        sut.cardNumber("some-card-number");
        sut.cvv("123");

        JSONObject unionPayEnrollmentPayload = new JSONObject(sut.build());
        assertFalse(unionPayEnrollmentPayload.has("cvv"));
    }

    @Test
    public void buildEnrollment_basicPayload() throws JSONException {
        UnionPayCardBuilder sut = new UnionPayCardBuilder();
        sut.cardNumber("someCardNumber");
        sut.expirationMonth("expirationMonth");
        sut.expirationYear("expirationYear");
        sut.mobileCountryCode("mobileCountryCode");
        sut.mobilePhoneNumber("mobilePhoneNumber");

        JSONObject result = new JSONObject(sut.buildEnrollment().toString());
        JSONObject unionPayEnrollment = result.getJSONObject("unionPayEnrollment");

        assertEquals("someCardNumber", unionPayEnrollment.getString("number"));
        assertEquals("expirationMonth", unionPayEnrollment.getString("expirationMonth"));
        assertEquals("expirationYear", unionPayEnrollment.getString("expirationYear"));
        assertEquals("mobileCountryCode", unionPayEnrollment.getString("mobileCountryCode"));
        assertEquals("mobilePhoneNumber", unionPayEnrollment.getString("mobileNumber"));
    }
}
