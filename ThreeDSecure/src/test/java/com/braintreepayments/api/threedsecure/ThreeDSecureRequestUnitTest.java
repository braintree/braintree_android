package com.braintreepayments.api.threedsecure;

import android.os.Parcel;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

import com.braintreepayments.api.threedsecure.ThreeDSecureAdditionalInformation;
import com.braintreepayments.api.threedsecure.ThreeDSecurePostalAddress;
import com.braintreepayments.api.threedsecure.ThreeDSecureRequest;
import com.braintreepayments.api.threedsecure.ThreeDSecureShippingMethod;
import com.braintreepayments.api.threedsecure.ThreeDSecureV2ButtonCustomization;
import com.braintreepayments.api.threedsecure.ThreeDSecureV2LabelCustomization;
import com.braintreepayments.api.threedsecure.ThreeDSecureV2TextBoxCustomization;
import com.braintreepayments.api.threedsecure.ThreeDSecureV2ToolbarCustomization;
import com.braintreepayments.api.threedsecure.ThreeDSecureV2UiCustomization;

import java.util.HashMap;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
public class ThreeDSecureRequestUnitTest {

    @Test
    public void writeToParcel() {
        ThreeDSecureAdditionalInformation additionalInformation =
                new ThreeDSecureAdditionalInformation();
        additionalInformation.setAccountId("account-id");

        ThreeDSecurePostalAddress billingAddress = new ThreeDSecurePostalAddress();
        billingAddress.setGivenName("Joe");
        billingAddress.setSurname("Guy");
        billingAddress.setPhoneNumber("12345678");
        billingAddress.setStreetAddress("555 Smith St.");
        billingAddress.setExtendedAddress("#5");
        billingAddress.setLine3("Suite C");
        billingAddress.setLocality("Oakland");
        billingAddress.setRegion("CA");
        billingAddress.setCountryCodeAlpha2("US");
        billingAddress.setPostalCode("54321");

        ThreeDSecureV2LabelCustomization labelCustomization =
                new ThreeDSecureV2LabelCustomization();
        labelCustomization.setHeadingTextColor("#FFA5FF");

        ThreeDSecureV2TextBoxCustomization textBoxCustomization =
                new ThreeDSecureV2TextBoxCustomization();
        textBoxCustomization.setBorderColor("#000000");

        ThreeDSecureV2ButtonCustomization buttonCustomization =
                new ThreeDSecureV2ButtonCustomization();
        buttonCustomization.setBackgroundColor("#FFFFFF");

        ThreeDSecureV2ToolbarCustomization toolbarCustomization =
                new ThreeDSecureV2ToolbarCustomization();
        toolbarCustomization.setTextColor("#F0F0F0");
        toolbarCustomization.setButtonText("TEST");

        ThreeDSecureV2UiCustomization v2UiCustomization = new ThreeDSecureV2UiCustomization();
        v2UiCustomization.setLabelCustomization(labelCustomization);
        v2UiCustomization.setTextBoxCustomization(textBoxCustomization);
        v2UiCustomization.setButtonCustomization(buttonCustomization);
        v2UiCustomization.setButtonType(ThreeDSecureV2ButtonType.BUTTON_TYPE_VERIFY);
        v2UiCustomization.setToolbarCustomization(toolbarCustomization);

        Map<String, String> customFields = new HashMap<>();
        customFields.put("custom_key1", "custom_value1");
        customFields.put("custom_key2", "custom_value2");
        customFields.put("custom_key3", "custom_value3");

        ThreeDSecureRequest expected = new ThreeDSecureRequest();
        expected.setNonce("a-nonce");
        expected.setAmount("1.00");
        expected.setMobilePhoneNumber("5151234321");
        expected.setEmail("tester@example.com");
        expected.setShippingMethod(ThreeDSecureShippingMethod.PRIORITY);
        expected.setBillingAddress(billingAddress);
        expected.setAdditionalInformation(additionalInformation);
        expected.setChallengeRequested(true);
        expected.setDataOnlyRequested(true);
        expected.setExemptionRequested(true);
        expected.setRequestedExemptionType(ThreeDSecureRequestedExemptionType.LOW_VALUE);
        expected.setCardAddChallengeRequested(true);
        expected.setV2UiCustomization(v2UiCustomization);
        expected.setAccountType(ThreeDSecureAccountType.CREDIT);
        expected.setCustomFields(customFields);

        Parcel parcel = Parcel.obtain();
        expected.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        ThreeDSecureRequest actual = ThreeDSecureRequest.CREATOR.createFromParcel(parcel);

        assertEquals(expected.getAmount(), actual.getAmount());
        assertEquals(expected.getAccountType(), actual.getAccountType());
        assertEquals(expected.getNonce(), actual.getNonce());
        assertEquals(expected.getMobilePhoneNumber(), actual.getMobilePhoneNumber());
        assertEquals(expected.getEmail(), actual.getEmail());
        assertEquals(expected.getShippingMethod(), actual.getShippingMethod());
        assertEquals(expected.getBillingAddress().getGivenName(),
                actual.getBillingAddress().getGivenName());
        assertEquals(expected.getBillingAddress().getSurname(),
                actual.getBillingAddress().getSurname());
        assertEquals(expected.getBillingAddress().getPhoneNumber(),
                actual.getBillingAddress().getPhoneNumber());
        assertEquals(expected.getBillingAddress().getStreetAddress(),
                actual.getBillingAddress().getStreetAddress());
        assertEquals(expected.getBillingAddress().getExtendedAddress(),
                actual.getBillingAddress().getExtendedAddress());
        assertEquals(expected.getBillingAddress().getLine3(),
                actual.getBillingAddress().getLine3());
        assertEquals(expected.getBillingAddress().getLocality(),
                actual.getBillingAddress().getLocality());
        assertEquals(expected.getBillingAddress().getRegion(),
                actual.getBillingAddress().getRegion());
        assertEquals(expected.getBillingAddress().getCountryCodeAlpha2(),
                actual.getBillingAddress().getCountryCodeAlpha2());
        assertEquals(expected.getBillingAddress().getPostalCode(),
                actual.getBillingAddress().getPostalCode());
        assertEquals(expected.getAdditionalInformation().getAccountId(),
                actual.getAdditionalInformation().getAccountId());
        assertEquals(expected.getChallengeRequested(), actual.getChallengeRequested());
        assertEquals(expected.getDataOnlyRequested(), actual.getDataOnlyRequested());
        assertEquals(expected.getExemptionRequested(), actual.getExemptionRequested());
        assertEquals(expected.getRequestedExemptionType(), actual.getRequestedExemptionType());
        assertEquals(expected.getCardAddChallengeRequested(), actual.getCardAddChallengeRequested());

        assertEquals(expected.getV2UiCustomization().getLabelCustomization().getHeadingTextColor(),
                actual.getV2UiCustomization().getLabelCustomization().getHeadingTextColor());

        assertEquals(expected.getV2UiCustomization().getTextBoxCustomization().getBorderColor(),
                actual.getV2UiCustomization().getTextBoxCustomization().getBorderColor());

        assertEquals(expected.getV2UiCustomization().getButtonCustomization().getBackgroundColor(),
                actual.getV2UiCustomization().getButtonCustomization().getBackgroundColor());

        assertEquals(expected.getV2UiCustomization().getToolbarCustomization().getTextColor(),
                actual.getV2UiCustomization().getToolbarCustomization().getTextColor());
        assertEquals(expected.getV2UiCustomization().getToolbarCustomization().getButtonText(),
                actual.getV2UiCustomization().getToolbarCustomization().getButtonText());

        assertEquals(3, actual.getCustomFields().size());
        assertEquals("custom_value1", actual.getCustomFields().get("custom_key1"));
        assertEquals("custom_value2", actual.getCustomFields().get("custom_key2"));
        assertEquals("custom_value3", actual.getCustomFields().get("custom_key3"));
    }

    @Test
    public void writeToParcel_allowCardAddChallengeRequestedToEqualNull() {
        ThreeDSecureRequest expected = new ThreeDSecureRequest();
        expected.setCardAddChallengeRequested(null);

        Parcel parcel = Parcel.obtain();
        expected.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        ThreeDSecureRequest actual = ThreeDSecureRequest.CREATOR.createFromParcel(parcel);
        assertNull(actual.getCardAddChallengeRequested());
    }

    @Test
    public void toJson() throws JSONException {
        ThreeDSecureAdditionalInformation additionalInformation =
                new ThreeDSecureAdditionalInformation();
        additionalInformation.setAccountId("account-id");

        ThreeDSecurePostalAddress billingAddress = new ThreeDSecurePostalAddress();
        billingAddress.setGivenName("billing-given-name");
        billingAddress.setSurname("billing-surname");
        billingAddress.setStreetAddress("billing-line1");
        billingAddress.setExtendedAddress("billing-line2");
        billingAddress.setLine3("billing-line3");
        billingAddress.setLocality("billing-city");
        billingAddress.setRegion("billing-state");
        billingAddress.setPostalCode("billing-postal-code");
        billingAddress.setCountryCodeAlpha2("billing-country-code");
        billingAddress.setPhoneNumber("billing-phone-number");

        ThreeDSecureRequest request = new ThreeDSecureRequest();
        request.setAmount("amount");
        request.setMobilePhoneNumber("mobile-phone-number");
        request.setEmail("email");
        request.setShippingMethod(ThreeDSecureShippingMethod.SAME_DAY);
        request.setBillingAddress(billingAddress);
        request.setAdditionalInformation(additionalInformation);
        request.setChallengeRequested(true);
        request.setDataOnlyRequested(true);
        request.setExemptionRequested(true);
        request.setCardAddChallengeRequested(true);
        request.setAccountType(ThreeDSecureAccountType.CREDIT);

        Map<String, String> customFields = new HashMap<>();
        customFields.put("custom_key1", "custom_value1");
        customFields.put("custom_key2", "123");
        request.setCustomFields(customFields);

        JSONObject json = new JSONObject(request.build("df-reference-id"));
        JSONObject additionalInfoJson = json.getJSONObject("additional_info");

        assertEquals("df-reference-id", json.get("df_reference_id"));
        assertEquals("amount", json.get("amount"));
        assertEquals("credit", json.get("account_type"));
        assertTrue(json.getBoolean("card_add"));
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

        JSONObject customFieldsJson = json.getJSONObject("custom_fields");
        assertEquals("custom_value1", customFieldsJson.getString("custom_key1"));
        assertEquals("123",customFieldsJson.getString("custom_key2"));
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
    public void build_containsDfReferenceId() throws JSONException {
        ThreeDSecureRequest threeDSecureRequest = new ThreeDSecureRequest();
        JSONObject json = new JSONObject(threeDSecureRequest.build("df-reference-id"));

        assertEquals("df-reference-id", json.getString("df_reference_id"));
    }

    @Test
    public void build_whenShippingMethodIsSameDay_returns01() throws JSONException {
        ThreeDSecureRequest threeDSecureRequest = new ThreeDSecureRequest();
        threeDSecureRequest.setShippingMethod(ThreeDSecureShippingMethod.SAME_DAY);

        JSONObject json = new JSONObject(threeDSecureRequest.build("df-reference-id"));
        assertEquals("01", json.getJSONObject("additional_info").getString("shipping_method"));
    }

    @Test
    public void build_whenShippingMethodIsExpedited_returns02() throws JSONException {
        ThreeDSecureRequest threeDSecureRequest = new ThreeDSecureRequest();
        threeDSecureRequest.setShippingMethod(ThreeDSecureShippingMethod.EXPEDITED);

        JSONObject json = new JSONObject(threeDSecureRequest.build("df-reference-id"));
        assertEquals("02", json.getJSONObject("additional_info").getString("shipping_method"));
    }

    @Test
    public void build_whenShippingMethodIsPriority_returns03() throws JSONException {
        ThreeDSecureRequest threeDSecureRequest = new ThreeDSecureRequest();
        threeDSecureRequest.setShippingMethod(ThreeDSecureShippingMethod.PRIORITY);

        JSONObject json = new JSONObject(threeDSecureRequest.build("df-reference-id"));
        assertEquals("03", json.getJSONObject("additional_info").getString("shipping_method"));
    }

    @Test
    public void build_whenShippingMethodIsGround_returns04() throws JSONException {
        ThreeDSecureRequest threeDSecureRequest = new ThreeDSecureRequest();
        threeDSecureRequest.setShippingMethod(ThreeDSecureShippingMethod.GROUND);

        JSONObject json = new JSONObject(threeDSecureRequest.build("df-reference-id"));
        assertEquals("04", json.getJSONObject("additional_info").getString("shipping_method"));
    }

    @Test
    public void build_whenShippingMethodIsElectronicDelivery_returns05() throws JSONException {
        ThreeDSecureRequest threeDSecureRequest = new ThreeDSecureRequest();
        threeDSecureRequest.setShippingMethod(ThreeDSecureShippingMethod.ELECTRONIC_DELIVERY);

        JSONObject json = new JSONObject(threeDSecureRequest.build("df-reference-id"));
        assertEquals("05", json.getJSONObject("additional_info").getString("shipping_method"));
    }

    @Test
    public void build_whenShippingMethodIsShipToStore_returns06() throws JSONException {
        ThreeDSecureRequest threeDSecureRequest = new ThreeDSecureRequest();
        threeDSecureRequest.setShippingMethod(ThreeDSecureShippingMethod.SHIP_TO_STORE);

        JSONObject json = new JSONObject(threeDSecureRequest.build("df-reference-id"));
        assertEquals("06", json.getJSONObject("additional_info").getString("shipping_method"));
    }

    @Test
    public void build_whenShippingMethodIsNotSet_doesNotSetShippingMethod() throws JSONException {
        ThreeDSecureRequest threeDSecureRequest = new ThreeDSecureRequest();

        JSONObject json = new JSONObject(threeDSecureRequest.build("df-reference-id"));
        assertFalse(json.getJSONObject("additional_info").has("shipping_method"));
    }

    @Test
    public void build_whenCardAddChallengeRequestedNotSet_doesNotSetCardAddChallengeRequested()
            throws JSONException {
        ThreeDSecureRequest threeDSecureRequest = new ThreeDSecureRequest();

        JSONObject json = new JSONObject(threeDSecureRequest.build("df-reference-id"));
        assertFalse(json.has("card_add"));
    }

    @Test
    public void build_whenCardAddChallengeRequestedFalse_setsCardAddChallengeRequestedFalse()
            throws JSONException {
        ThreeDSecureRequest threeDSecureRequest = new ThreeDSecureRequest();
        threeDSecureRequest.setCardAddChallengeRequested(false);

        JSONObject json = new JSONObject(threeDSecureRequest.build("df-reference-id"));
        assertFalse(json.getBoolean("card_add"));
    }
}
