package com.braintreepayments.api.models;

import android.os.Parcel;

import com.cardinalcommerce.shared.userinterfaces.LabelCustomization;
import com.cardinalcommerce.shared.userinterfaces.UiCustomization;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.braintreepayments.api.models.ThreeDSecureRequest.VERSION_1;
import static com.braintreepayments.api.models.ThreeDSecureRequest.VERSION_2;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class ThreeDSecureRequestUnitTest {

    @Test
    public void constructor_noVersionRequested_defaultsToVersion1() {
        ThreeDSecureRequest request = new ThreeDSecureRequest();

        assertEquals(VERSION_1, request.getVersionRequested());
    }

    @Test
    public void constructor_defaultsUiCustomizationPropertyToEmptyObject() {
        ThreeDSecureRequest request = new ThreeDSecureRequest();
        assertNotNull(request.getUiCustomization());
    }

    @Test
    public void writeToParcel() {
        ThreeDSecureAdditionalInformation additionalInformation = new ThreeDSecureAdditionalInformation()
                .accountId("account-id");

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

        LabelCustomization labelCustomization = new LabelCustomization();
        labelCustomization.setHeadingTextColor("#FF5A5F");

        UiCustomization uiCustomization = new UiCustomization();
        uiCustomization.setLabelCustomization(labelCustomization);

        ThreeDSecureV1UiCustomization v1UiCustomization = new ThreeDSecureV1UiCustomization()
                .redirectButtonText("return-button-text")
                .redirectDescription("return-label-text");

        ThreeDSecureRequest expected = new ThreeDSecureRequest()
                .nonce("a-nonce")
                .amount("1.00")
                .mobilePhoneNumber("5151234321")
                .email("tester@example.com")
                .shippingMethod("03")
                .versionRequested(VERSION_2)
                .billingAddress(billingAddress)
                .additionalInformation(additionalInformation)
                .challengeRequested(true)
                .exemptionRequested(true)
                .uiCustomization(uiCustomization)
                .v1UiCustomization(v1UiCustomization);

        Parcel parcel = Parcel.obtain();
        expected.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        ThreeDSecureRequest actual = new ThreeDSecureRequest(parcel);

        assertEquals(expected.getAmount(), actual.getAmount());
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
        assertEquals(expected.isExemptionRequested(), actual.isExemptionRequested());

        assertEquals(expected.getUiCustomization().getLabelCustomization().getHeadingTextColor(),
                actual.getUiCustomization().getLabelCustomization().getHeadingTextColor());

        assertEquals(expected.getV1UiCustomization().getRedirectButtonText(),
                actual.getV1UiCustomization().getRedirectButtonText());

        assertEquals(expected.getV1UiCustomization().getRedirectDescription(),
                actual.getV1UiCustomization().getRedirectDescription());
    }

    @Test
    public void toJson() throws JSONException{
        ThreeDSecureAdditionalInformation additionalInformation = new ThreeDSecureAdditionalInformation()
                .accountId("account-id");

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
                .shippingMethod("shipping-method")
                .billingAddress(billingAddress)
                .additionalInformation(additionalInformation)
                .challengeRequested(true)
                .exemptionRequested(true);

        JSONObject json = new JSONObject(request.build("df-reference-id"));
        JSONObject additionalInfoJson = json.getJSONObject("additional_info");

        assertEquals("df-reference-id", json.get("df_reference_id"));
        assertEquals("amount", json.get("amount"));
        assertTrue(json.getBoolean("challenge_requested"));
        assertTrue(json.getBoolean("exemption_requested"));

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
        assertEquals("shipping-method", additionalInfoJson.get("shipping_method"));

        assertEquals("account-id", additionalInfoJson.get("account_id"));
    }

    @Test
    public void build_withVersion1_doesNotContainDfReferenceId() throws JSONException {
        JSONObject json = new JSONObject(new ThreeDSecureRequest()
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
}
