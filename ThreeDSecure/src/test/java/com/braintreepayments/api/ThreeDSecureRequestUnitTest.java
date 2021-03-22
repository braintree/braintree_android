package com.braintreepayments.api;

import android.os.Parcel;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.braintreepayments.api.ThreeDSecureRequest.VERSION_1;
import static com.braintreepayments.api.ThreeDSecureRequest.VERSION_2;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertNull;

@RunWith(RobolectricTestRunner.class)
public class ThreeDSecureRequestUnitTest {

    @Test
    public void constructor_noVersionRequested_defaultsToVersion2() {
        ThreeDSecureRequest request = new ThreeDSecureRequest();

        assertEquals(VERSION_2, request.getVersionRequested());
    }

    @Test
    public void writeToParcel() {
        ThreeDSecureAdditionalInformation additionalInformation = new ThreeDSecureAdditionalInformation();
        additionalInformation.accountId("account-id");

        ThreeDSecurePostalAddress billingAddress = new ThreeDSecurePostalAddress()
                .givenName("Joe")
                .surname("Guy")
                .phoneNumber("12345678")
                .streetAddress("555 Smith St.")
                .extendedAddress("#5")
                .line3("Suite C")
                .locality("Oakland")
                .region("CA")
                .countryCodeAlpha2("US")
                .postalCode("54321");

        ThreeDSecureV2LabelCustomization labelCustomization = new ThreeDSecureV2LabelCustomization();
        labelCustomization.setHeadingTextColor("#FFA5FF");

        ThreeDSecureV2UiCustomization v2UiCustomization = new ThreeDSecureV2UiCustomization();
        v2UiCustomization.setLabelCustomization(labelCustomization);

        ThreeDSecureV1UiCustomization v1UiCustomization = new ThreeDSecureV1UiCustomization();
        v1UiCustomization.setRedirectButtonText("return-button-text");
        v1UiCustomization.setRedirectDescription("return-label-text");

        ThreeDSecureRequest expected = new ThreeDSecureRequest()
                .nonce("a-nonce")
                .amount("1.00")
                .mobilePhoneNumber("5151234321")
                .email("tester@example.com")
                .shippingMethod(ThreeDSecureShippingMethod.PRIORITY)
                .versionRequested(VERSION_2)
                .billingAddress(billingAddress)
                .additionalInformation(additionalInformation)
                .challengeRequested(true)
                .dataOnlyRequested(true)
                .exemptionRequested(true)
                .v2UiCustomization(v2UiCustomization)
                .v1UiCustomization(v1UiCustomization)
                .accountType(ThreeDSecureRequest.CREDIT);

        Parcel parcel = Parcel.obtain();
        expected.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        ThreeDSecureRequest actual = new ThreeDSecureRequest(parcel);

        assertEquals(expected.getAmount(), actual.getAmount());
        assertEquals(expected.getAccountType(), actual.getAccountType());
        assertEquals(expected.getNonce(), actual.getNonce());
        assertEquals(expected.getMobilePhoneNumber(), actual.getMobilePhoneNumber());
        assertEquals(expected.getEmail(), actual.getEmail());
        assertEquals(expected.getShippingMethod(), actual.getShippingMethod());
        assertEquals(expected.getVersionRequested(), actual.getVersionRequested());
        assertEquals(expected.getBillingAddress().getGivenName(), actual.getBillingAddress().getGivenName());
        assertEquals(expected.getBillingAddress().getSurname(), actual.getBillingAddress().getSurname());
        assertEquals(expected.getBillingAddress().getPhoneNumber(), actual.getBillingAddress().getPhoneNumber());
        assertEquals(expected.getBillingAddress().getStreetAddress(), actual.getBillingAddress().getStreetAddress());
        assertEquals(expected.getBillingAddress().getExtendedAddress(), actual.getBillingAddress().getExtendedAddress());
        assertEquals(expected.getBillingAddress().getLine3(), actual.getBillingAddress().getLine3());
        assertEquals(expected.getBillingAddress().getLocality(), actual.getBillingAddress().getLocality());
        assertEquals(expected.getBillingAddress().getRegion(), actual.getBillingAddress().getRegion());
        assertEquals(expected.getBillingAddress().getCountryCodeAlpha2(), actual.getBillingAddress().getCountryCodeAlpha2());
        assertEquals(expected.getBillingAddress().getPostalCode(), actual.getBillingAddress().getPostalCode());
        assertEquals(expected.getAdditionalInformation().getAccountId(), actual.getAdditionalInformation().getAccountId());
        assertEquals(expected.isChallengeRequested(), actual.isChallengeRequested());
        assertEquals(expected.isDataOnlyRequested(), actual.isDataOnlyRequested());
        assertEquals(expected.isExemptionRequested(), actual.isExemptionRequested());

        assertEquals(expected.getV2UiCustomization().getLabelCustomization().getHeadingTextColor(),
                actual.getV2UiCustomization().getLabelCustomization().getHeadingTextColor());

        assertEquals(expected.getV1UiCustomization().getRedirectButtonText(),
                actual.getV1UiCustomization().getRedirectButtonText());

        assertEquals(expected.getV1UiCustomization().getRedirectDescription(),
                actual.getV1UiCustomization().getRedirectDescription());
    }

    @Test
    public void toJson() throws JSONException {
        ThreeDSecureAdditionalInformation additionalInformation = new ThreeDSecureAdditionalInformation();
        additionalInformation.accountId("account-id");

        ThreeDSecurePostalAddress billingAddress = new ThreeDSecurePostalAddress()
                .givenName("billing-given-name")
                .surname("billing-surname")
                .streetAddress("billing-line1")
                .extendedAddress("billing-line2")
                .line3("billing-line3")
                .locality("billing-city")
                .region("billing-state")
                .postalCode("billing-postal-code")
                .countryCodeAlpha2("billing-country-code")
                .phoneNumber("billing-phone-number");

        ThreeDSecureRequest request = new ThreeDSecureRequest()
                .versionRequested(VERSION_2)
                .amount("amount")
                .mobilePhoneNumber("mobile-phone-number")
                .email("email")
                .shippingMethod(ThreeDSecureShippingMethod.SAME_DAY)
                .billingAddress(billingAddress)
                .additionalInformation(additionalInformation)
                .challengeRequested(true)
                .dataOnlyRequested(true)
                .exemptionRequested(true)
                .accountType(ThreeDSecureRequest.CREDIT);

        JSONObject json = new JSONObject(request.build("df-reference-id"));
        JSONObject additionalInfoJson = json.getJSONObject("additional_info");

        assertEquals("df-reference-id", json.get("df_reference_id"));
        assertEquals("amount", json.get("amount"));
        assertEquals("credit", json.get("account_type"));
        assertTrue(json.getBoolean("challenge_requested"));
        assertTrue(json.getBoolean("exemption_requested"));
        assertTrue(json.getBoolean("data_only_requested"));

        assertEquals("billing-given-name", additionalInfoJson.get("billing_given_name"));
        assertEquals("billing-surname", additionalInfoJson.get("billing_surname"));
        assertEquals("billing-line1", additionalInfoJson.get("billing_line1"));
        assertEquals("billing-line2", additionalInfoJson.get("billing_line2"));
        assertEquals("billing-line3", additionalInfoJson.get("billing_line3"));
        assertEquals("billing-city", additionalInfoJson.get("billing_city"));
        assertEquals("billing-state", additionalInfoJson.get("billing_state"));
        assertEquals("billing-postal-code", additionalInfoJson.get("billing_postal_code"));
        assertEquals("billing-country-code", additionalInfoJson.get("billing_country_code"));
        assertEquals("billing-phone-number", additionalInfoJson.get("billing_phone_number"));

        assertEquals("mobile-phone-number", additionalInfoJson.get("mobile_phone_number"));
        assertEquals("email", additionalInfoJson.get("email"));
        assertEquals("01", additionalInfoJson.get("shipping_method"));

        assertEquals("account-id", additionalInfoJson.get("account_id"));
    }

    @Test
    public void toJson_whenAccountTypeNotSet_doesNotIncludeAccountType() throws JSONException {
        JSONObject json = new JSONObject(new ThreeDSecureRequest()
                .build("df-reference-id"));

        assertFalse(json.has("account_type"));
    }

    @Test
    public void toJson_whenDataOnlyRequestedNotSet_defaultsToFalse() throws JSONException {
        JSONObject json = new JSONObject(new ThreeDSecureRequest()
                .build("df-reference-id"));

        assertFalse(json.getBoolean("data_only_requested"));
    }

    @Test
    public void build_withVersion1_doesNotContainDfReferenceId() throws JSONException {
        JSONObject json = new JSONObject(new ThreeDSecureRequest()
                .versionRequested(VERSION_1)
                .build("df-reference-id"));

        assertFalse(json.has("df_reference_id"));
    }

    @Test
    public void build_withVersion2_containsDfReferenceId() throws JSONException {
        JSONObject json = new JSONObject(new ThreeDSecureRequest()
                .versionRequested(VERSION_2)
                .build("df-reference-id"));

        assertEquals("df-reference-id", json.getString("df_reference_id"));
    }

    @Test
    public void build_whenShippingMethodIsSameDay_returns01() throws JSONException {
        JSONObject json = new JSONObject(new ThreeDSecureRequest()
                .shippingMethod(ThreeDSecureShippingMethod.SAME_DAY)
                .build("df-reference-id"));

        assertEquals("01", json.getJSONObject("additional_info").getString("shipping_method"));
    }

    @Test
    public void build_whenShippingMethodIsExpedited_returns02() throws JSONException {
        JSONObject json = new JSONObject(new ThreeDSecureRequest()
                .shippingMethod(ThreeDSecureShippingMethod.EXPEDITED)
                .build("df-reference-id"));

        assertEquals("02", json.getJSONObject("additional_info").getString("shipping_method"));
    }

    @Test
    public void build_whenShippingMethodIsPriority_returns03() throws JSONException {
        JSONObject json = new JSONObject(new ThreeDSecureRequest()
                .shippingMethod(ThreeDSecureShippingMethod.PRIORITY)
                .build("df-reference-id"));

        assertEquals("03", json.getJSONObject("additional_info").getString("shipping_method"));
    }

    @Test
    public void build_whenShippingMethodIsGround_returns04() throws JSONException {
        JSONObject json = new JSONObject(new ThreeDSecureRequest()
                .shippingMethod(ThreeDSecureShippingMethod.GROUND)
                .build("df-reference-id"));

        assertEquals("04", json.getJSONObject("additional_info").getString("shipping_method"));
    }

    @Test
    public void build_whenShippingMethodIsElectronicDelivery_returns05() throws JSONException {
        JSONObject json = new JSONObject(new ThreeDSecureRequest()
                .shippingMethod(ThreeDSecureShippingMethod.ELECTRONIC_DELIVERY)
                .build("df-reference-id"));

        assertEquals("05", json.getJSONObject("additional_info").getString("shipping_method"));
    }

    @Test
    public void build_whenShippingMethodIsShipToStore_returns06() throws JSONException {
        JSONObject json = new JSONObject(new ThreeDSecureRequest()
                .shippingMethod(ThreeDSecureShippingMethod.SHIP_TO_STORE)
                .build("df-reference-id"));

        assertEquals("06", json.getJSONObject("additional_info").getString("shipping_method"));
    }

    @Test
    public void build_whenShippingMethodIsNotSet_doesNotSetShippingMethod() throws JSONException {
        JSONObject json = new JSONObject(new ThreeDSecureRequest()
                .build("df-reference-id"));

        assertFalse(json.getJSONObject("additional_info").has("shipping_method"));
    }
}
