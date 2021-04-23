package com.braintreepayments.api;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.braintreepayments.api.BaseCard.BILLING_ADDRESS_KEY;
import static com.braintreepayments.api.BaseCard.CREDIT_CARD_KEY;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;

@RunWith(RobolectricTestRunner.class)
public class UnionPayCardUnitTest {

    @Test
    public void getApiPath_returnsExpected() {
        assertEquals("credit_cards", new UnionPayCard().getApiPath());
    }

    @Test
    public void getResponsePaymentMethodType_returnsExpected() {
        assertEquals("CreditCard", new UnionPayCard().getResponsePaymentMethodType());
    }

    @Test
    public void cardNumber_addsToJson() throws JSONException {
        UnionPayCard sut = new UnionPayCard();
        sut.setNumber("myCardNumber");

        assertEquals("myCardNumber", sut.buildEnrollment()
                .getJSONObject("unionPayEnrollment")
                .getString("number"));
    }

    @Test
    public void expirationMonth_addsToJson() throws JSONException {
        UnionPayCard sut = new UnionPayCard();
        sut.setExpirationMonth("12");

        assertEquals("12", sut.buildEnrollment()
                .getJSONObject("unionPayEnrollment")
                .getString("expirationMonth"));
    }

    @Test
    public void expirationYear_addsToJson() throws JSONException {
        UnionPayCard sut = new UnionPayCard();
        sut.setExpirationYear("2020");

        assertEquals("2020", sut.buildEnrollment()
                .getJSONObject("unionPayEnrollment")
                .getString("expirationYear"));
    }

    @Test
    public void expirationDate_addsToJsonAsExpirationMonthAndExpirationYear() throws JSONException {
        UnionPayCard sut = new UnionPayCard();
        sut.setExpirationDate("12/2020");

        JSONObject enrollment = sut.buildEnrollment().getJSONObject("unionPayEnrollment");
        assertEquals("12", enrollment.getString("expirationMonth"));
        assertEquals("2020", enrollment.getString("expirationYear"));
    }

    @Test
    public void mobileCountryCode_addsToJson() throws JSONException {
        UnionPayCard sut = new UnionPayCard();
        sut.setMobileCountryCode("1");

        assertEquals("1", sut.buildEnrollment()
                .getJSONObject("unionPayEnrollment")
                .getString("mobileCountryCode"));
    }

    @Test
    public void mobilePhoneNumber_addsToJson() throws JSONException {
        UnionPayCard sut = new UnionPayCard();
        sut.setMobilePhoneNumber("867-5309");

        assertEquals("867-5309", sut.buildEnrollment()
                .getJSONObject("unionPayEnrollment")
                .getString("mobileNumber"));
    }

    @Test
    public void smsCode_addsToOptionsJson() throws JSONException {
        UnionPayCard sut = new UnionPayCard();
        sut.setSmsCode("mySmsCode");

        JSONObject jsonObject = sut.buildJSON();
        assertEquals("mySmsCode", jsonObject.getJSONObject("creditCard")
                .getJSONObject("options")
                .getJSONObject("unionPayEnrollment")
                .getString("smsCode"));
    }

    @Test
    public void enrollmentId_addsToOptionsJson() throws JSONException {
        UnionPayCard sut = new UnionPayCard();
        sut.setEnrollmentId("myEnrollmentId");

        JSONObject jsonObject = sut.buildJSON();
        assertEquals("myEnrollmentId", jsonObject.getJSONObject("creditCard")
                .getJSONObject("options")
                .getJSONObject("unionPayEnrollment")
                .getString("id"));
    }

    @Test
    public void doesNotIncludeEmptyStrings() throws JSONException {
        UnionPayCard sut = new UnionPayCard();
        sut.setNumber("");
        sut.setExpirationDate("");
        sut.setExpirationMonth("");
        sut.setExpirationYear("");
        sut.setCvv("");
        sut.setPostalCode("");
        sut.setCardholderName("");
        sut.setFirstName("");
        sut.setLastName("");
        sut.setStreetAddress("");
        sut.setLocality("");
        sut.setPostalCode("");
        sut.setRegion("");
        sut.setEnrollmentId("");
        sut.setMobileCountryCode("");
        sut.setMobilePhoneNumber("");
        sut.setSmsCode("");

        JSONObject json = sut.buildJSON();
        assertEquals("{\"options\":{\"unionPayEnrollment\":{}}}",
            json.getJSONObject(CREDIT_CARD_KEY).toString());
        assertFalse(json.has(BILLING_ADDRESS_KEY));
    }

    @Test
    public void buildEnrollment_createsUnionPayEnrollmentJson() throws JSONException {
        UnionPayCard sut = new UnionPayCard();
        sut.setCvv("123");
        sut.setEnrollmentId("enrollment-id");
        sut.setExpirationYear("expiration-year");
        sut.setExpirationMonth("expiration-month");
        sut.setNumber("card-number");
        sut.setMobileCountryCode("mobile-country-code");
        sut.setMobilePhoneNumber("mobile-phone-number");
        sut.setSmsCode("sms-code");
        sut.setIntegration("integration");
        sut.setSessionId("session-id");
        sut.setSource("source");
        sut.setValidate(true);

        JSONObject unionPayEnrollment = sut.buildEnrollment().getJSONObject("unionPayEnrollment");

        assertEquals("card-number", unionPayEnrollment.getString("number"));
        assertEquals("expiration-month", unionPayEnrollment.getString("expirationMonth"));
        assertEquals("expiration-year", unionPayEnrollment.getString("expirationYear"));
        assertEquals("mobile-country-code", unionPayEnrollment.getString("mobileCountryCode"));
        assertEquals("mobile-phone-number", unionPayEnrollment.getString("mobileNumber"));
    }

    @Test
    public void build_createsUnionPayTokenizeJson() throws JSONException {
        UnionPayCard sut = new UnionPayCard();
        sut.setCvv("123");
        sut.setEnrollmentId("enrollment-id");
        sut.setExpirationYear("expiration-year");
        sut.setExpirationMonth("expiration-month");
        sut.setNumber("card-number");
        sut.setMobileCountryCode("mobile-country-code");
        sut.setMobilePhoneNumber("mobile-phone-number");
        sut.setSmsCode("sms-code");
        sut.setIntegration("integration");
        sut.setSessionId("session-id");
        sut.setSource("source");

        JSONObject tokenizePayload = sut.buildJSON();

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
        UnionPayCard unionPayCardValidateTrue = new UnionPayCard();
        unionPayCardValidateTrue.setValidate(true);

        UnionPayCard unionPayCardValidateFalse = new UnionPayCard();
        unionPayCardValidateFalse.setValidate(false);

        JSONObject optionsValidateTrue = unionPayCardValidateTrue.buildJSON()
                .getJSONObject("creditCard")
                .getJSONObject("options");
        JSONObject optionsValidateFalse = unionPayCardValidateFalse.buildJSON()
                .getJSONObject("creditCard")
                .getJSONObject("options");

        assertFalse(optionsValidateTrue.has("validate"));
        assertFalse(optionsValidateFalse.has("validate"));
    }

    @Test
    public void build_standardPayload() throws JSONException {
        UnionPayCard sut = new UnionPayCard();
        sut.setNumber("someCardNumber");
        sut.setExpirationMonth("expirationMonth");
        sut.setExpirationYear("expirationYear");
        sut.setCvv("cvv");
        sut.setEnrollmentId("enrollmentId");
        sut.setSmsCode("smsCode");
        sut.setValidate(true);

        JSONObject tokenizePayload = sut.buildJSON();
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
        UnionPayCard sut = new UnionPayCard();
        sut.setNumber("someCardNumber");
        sut.setExpirationMonth("expirationMonth");
        sut.setExpirationYear("expirationYear");
        sut.setCvv("cvv");
        sut.setEnrollmentId("enrollmentId");
        sut.setValidate(true);

        JSONObject tokenizePayload = sut.buildJSON();
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
        UnionPayCard sut = new UnionPayCard();
        sut.setNumber("some-card-number");
        sut.setCvv("123");

        JSONObject unionPayEnrollmentPayload = sut.buildJSON();
        assertFalse(unionPayEnrollmentPayload.has("cvv"));
    }

    @Test
    public void buildEnrollment_basicPayload() throws JSONException {
        UnionPayCard sut = new UnionPayCard();
        sut.setNumber("someCardNumber");
        sut.setExpirationMonth("expirationMonth");
        sut.setExpirationYear("expirationYear");
        sut.setMobileCountryCode("mobileCountryCode");
        sut.setMobilePhoneNumber("mobilePhoneNumber");

        JSONObject result = new JSONObject(sut.buildEnrollment().toString());
        JSONObject unionPayEnrollment = result.getJSONObject("unionPayEnrollment");

        assertEquals("someCardNumber", unionPayEnrollment.getString("number"));
        assertEquals("expirationMonth", unionPayEnrollment.getString("expirationMonth"));
        assertEquals("expirationYear", unionPayEnrollment.getString("expirationYear"));
        assertEquals("mobileCountryCode", unionPayEnrollment.getString("mobileCountryCode"));
        assertEquals("mobilePhoneNumber", unionPayEnrollment.getString("mobileNumber"));
    }
}
