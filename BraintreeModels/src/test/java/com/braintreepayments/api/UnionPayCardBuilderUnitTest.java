package com.braintreepayments.api;

import com.braintreepayments.api.UnionPayCardBuilder;

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
        UnionPayCardBuilder unionPayCardBuilder = new UnionPayCardBuilder().cardNumber("myCardNumber");

        assertEquals("myCardNumber", unionPayCardBuilder.buildEnrollment()
                .getJSONObject("unionPayEnrollment")
                .getString("number"));
    }

    @Test
    public void expirationMonth_addsToJson() throws JSONException {
        UnionPayCardBuilder unionPayCardBuilder = new UnionPayCardBuilder().expirationMonth("12");

        assertEquals("12", unionPayCardBuilder.buildEnrollment()
                .getJSONObject("unionPayEnrollment")
                .getString("expirationMonth"));
    }

    @Test
    public void expirationYear_addsToJson() throws JSONException {
        UnionPayCardBuilder unionPayCardBuilder = new UnionPayCardBuilder().expirationYear("2020");

        assertEquals("2020", unionPayCardBuilder.buildEnrollment()
                .getJSONObject("unionPayEnrollment")
                .getString("expirationYear"));
    }

    @Test
    public void expirationDate_addsToJsonAsExpirationMonthAndExpirationYear() throws JSONException {
        UnionPayCardBuilder unionPayCardBuilder = new UnionPayCardBuilder().expirationDate("12/2020");

        JSONObject enrollment = unionPayCardBuilder.buildEnrollment().getJSONObject("unionPayEnrollment");
        assertEquals("12", enrollment.getString("expirationMonth"));
        assertEquals("2020", enrollment.getString("expirationYear"));
    }

    @Test
    public void mobileCountryCode_addsToJson() throws JSONException {
        UnionPayCardBuilder unionPayCardBuilder = new UnionPayCardBuilder().mobileCountryCode("1");

        assertEquals("1", unionPayCardBuilder.buildEnrollment()
                .getJSONObject("unionPayEnrollment")
                .getString("mobileCountryCode"));
    }

    @Test
    public void mobilePhoneNumber_addsToJson() throws JSONException {
        UnionPayCardBuilder unionPayCardBuilder = new UnionPayCardBuilder().mobilePhoneNumber("867-5309");

        assertEquals("867-5309", unionPayCardBuilder.buildEnrollment()
                .getJSONObject("unionPayEnrollment")
                .getString("mobileNumber"));
    }

    @Test
    public void smsCode_addsToOptionsJson() throws JSONException {
        JSONObject jsonObject = new JSONObject(new UnionPayCardBuilder().smsCode("mySmsCode").build());

        assertEquals("mySmsCode", jsonObject.getJSONObject("creditCard")
                .getJSONObject("options")
                .getJSONObject("unionPayEnrollment")
                .getString("smsCode"));
    }

    @Test
    public void enrollmentId_addsToOptionsJson() throws JSONException {
        JSONObject jsonObject = new JSONObject(new UnionPayCardBuilder().enrollmentId("myEnrollmentId").build());

        assertEquals("myEnrollmentId", jsonObject.getJSONObject("creditCard")
                .getJSONObject("options")
                .getJSONObject("unionPayEnrollment")
                .getString("id"));
    }

    @Test
    public void doesNotIncludeEmptyStrings() throws JSONException {
        UnionPayCardBuilder unionPayCardBuilder = new UnionPayCardBuilder()
                .cardNumber("")
                .expirationDate("")
                .expirationMonth("")
                .expirationYear("")
                .cvv("")
                .postalCode("")
                .cardholderName("")
                .firstName("")
                .lastName("")
                .streetAddress("")
                .locality("")
                .postalCode("")
                .region("")
                .enrollmentId("")
                .mobileCountryCode("")
                .mobilePhoneNumber("")
                .smsCode("");

        assertEquals("{\"options\":{\"unionPayEnrollment\":{}}}",
                new JSONObject(unionPayCardBuilder.build()).getJSONObject(CREDIT_CARD_KEY).toString());
        assertFalse(new JSONObject(unionPayCardBuilder.build()).has(BILLING_ADDRESS_KEY));
    }

    @Test
    public void buildEnrollment_createsUnionPayEnrollmentJson() throws JSONException {
        UnionPayCardBuilder unionPayCardBuilder = new UnionPayCardBuilder()
                .cvv("123")
                .enrollmentId("enrollment-id")
                .expirationYear("expiration-year")
                .expirationMonth("expiration-month")
                .cardNumber("card-number")
                .mobileCountryCode("mobile-country-code")
                .mobilePhoneNumber("mobile-phone-number")
                .smsCode("sms-code")
                .integration("integration")
                .setSessionId("session-id")
                .source("source")
                .validate(true);
        JSONObject unionPayEnrollment = unionPayCardBuilder.buildEnrollment().getJSONObject("unionPayEnrollment");

        assertEquals("card-number", unionPayEnrollment.getString("number"));
        assertEquals("expiration-month", unionPayEnrollment.getString("expirationMonth"));
        assertEquals("expiration-year", unionPayEnrollment.getString("expirationYear"));
        assertEquals("mobile-country-code", unionPayEnrollment.getString("mobileCountryCode"));
        assertEquals("mobile-phone-number", unionPayEnrollment.getString("mobileNumber"));
    }

    @Test
    public void build_createsUnionPayTokenizeJson() throws JSONException {
        UnionPayCardBuilder unionPayCardBuilder = new UnionPayCardBuilder()
                .cvv("123")
                .enrollmentId("enrollment-id")
                .expirationYear("expiration-year")
                .expirationMonth("expiration-month")
                .cardNumber("card-number")
                .mobileCountryCode("mobile-country-code")
                .mobilePhoneNumber("mobile-phone-number")
                .smsCode("sms-code")
                .integration("integration")
                .setSessionId("session-id")
                .source("source");
        JSONObject tokenizePayload = new JSONObject(unionPayCardBuilder.build());

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
        UnionPayCardBuilder unionPayCardBuilderValidateTrue = new UnionPayCardBuilder()
                .validate(true);
        UnionPayCardBuilder unionPayCardBuilderValidateFalse = new UnionPayCardBuilder()
                .validate(false);

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
        UnionPayCardBuilder sut = new UnionPayCardBuilder()
                .cardNumber("someCardNumber")
                .expirationMonth("expirationMonth")
                .expirationYear("expirationYear")
                .cvv("cvv")
                .enrollmentId("enrollmentId")
                .smsCode("smsCode")
                .validate(true);

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
        UnionPayCardBuilder sut = new UnionPayCardBuilder()
                .cardNumber("someCardNumber")
                .expirationMonth("expirationMonth")
                .expirationYear("expirationYear")
                .cvv("cvv")
                .enrollmentId("enrollmentId")
                .validate(true);

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
        UnionPayCardBuilder unionPayCardBuilder = new UnionPayCardBuilder()
                .cardNumber("some-card-number")
                .cvv("123");

        JSONObject unionPayEnrollmentPayload = new JSONObject(unionPayCardBuilder.build());
        assertFalse(unionPayEnrollmentPayload.has("cvv"));
    }

    @Test
    public void buildEnrollment_basicPayload() throws JSONException {
        UnionPayCardBuilder unionPayCardBuilder = new UnionPayCardBuilder()
                .cardNumber("someCardNumber")
                .expirationMonth("expirationMonth")
                .expirationYear("expirationYear")
                .mobileCountryCode("mobileCountryCode")
                .mobilePhoneNumber("mobilePhoneNumber");

        JSONObject result = new JSONObject(unionPayCardBuilder.buildEnrollment().toString());
        JSONObject unionPayEnrollment = result.getJSONObject("unionPayEnrollment");

        assertEquals("someCardNumber", unionPayEnrollment.getString("number"));
        assertEquals("expirationMonth", unionPayEnrollment.getString("expirationMonth"));
        assertEquals("expirationYear", unionPayEnrollment.getString("expirationYear"));
        assertEquals("mobileCountryCode", unionPayEnrollment.getString("mobileCountryCode"));
        assertEquals("mobilePhoneNumber", unionPayEnrollment.getString("mobileNumber"));
    }
}
