package com.braintreepayments.api;

import com.braintreepayments.api.TestConfigurationBuilder.TestVisaCheckoutConfigurationBuilder;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Arrays;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ VisaCheckoutConfiguration.class, ClassHelper.class })
public class VisaCheckoutConfigurationUnitTest {

    private JSONObject mVisaCheckoutConfigurationJson;

    @Before
    public void setup() throws JSONException {
        JSONObject configuration = new JSONObject(Fixtures.CONFIGURATION_WITH_VISA_CHECKOUT);
        mVisaCheckoutConfigurationJson = configuration.getJSONObject("visaCheckout");
    }

    @Test
    public void isEnabled_returnsFalseWhenConfigurationApiKeyDoesntExist() throws JSONException {
        JSONObject blankVisaCheckoutJson = new JSONObject("{\"visaCheckout\":{}}");
        VisaCheckoutConfiguration visaCheckoutConfiguration = VisaCheckoutConfiguration.fromJson(blankVisaCheckoutJson);

        assertFalse(visaCheckoutConfiguration.isEnabled());
    }

    @Test
    public void isEnabled_returnsTrueWhenConfigurationApiKeyExists() {
        VisaCheckoutConfiguration visaCheckoutConfiguration = VisaCheckoutConfiguration.fromJson(
                mVisaCheckoutConfigurationJson);

        assertTrue(visaCheckoutConfiguration.isEnabled());
    }

    @Test
    public void getApiKey_returnsApiKeyWhenConfigurationExists() {
        VisaCheckoutConfiguration visaCheckoutConfiguration = VisaCheckoutConfiguration.fromJson(
                mVisaCheckoutConfigurationJson);

        assertEquals("gwApikey", visaCheckoutConfiguration.getApiKey());
    }

    @Test
    public void getExternalClientId_returnsExternalClientIdWhenConfigurationExists() {
        VisaCheckoutConfiguration visaCheckoutConfiguration = VisaCheckoutConfiguration.fromJson(
                mVisaCheckoutConfigurationJson);

        assertEquals("gwExternalClientId", visaCheckoutConfiguration.getExternalClientId());
    }

    @Test
    public void getApiKey_returnsEmptyStringWhenConfigurationDoesntExist() {
        VisaCheckoutConfiguration visaCheckoutConfiguration = VisaCheckoutConfiguration.fromJson(null);

        assertTrue(visaCheckoutConfiguration.getApiKey().isEmpty());
    }

    @Test
    public void getExternalClientId_returnsEmptyStringWhenConfigurationDoesntExist() {
        VisaCheckoutConfiguration visaCheckoutConfiguration = VisaCheckoutConfiguration.fromJson(null);

        assertTrue(visaCheckoutConfiguration.getExternalClientId().isEmpty());
    }

    @Test
    public void getAcceptedCardBrands_whenVisa_returnsElectronAndVisa() throws JSONException {
        List<String> expected = Arrays.asList("VISA");
        String visaCheckoutConfigurationJson = new TestVisaCheckoutConfigurationBuilder()
                .supportedCardTypes("Visa")
                .build();

        VisaCheckoutConfiguration visaCheckoutConfiguration = VisaCheckoutConfiguration.fromJson(
                new JSONObject(visaCheckoutConfigurationJson));

        assertEquals(expected, visaCheckoutConfiguration.getAcceptedCardBrands());
    }

    @Test
    public void getAcceptedCardBrands_whenMastercard_returnsMastercard() throws JSONException {
        List<String> expected = Arrays.asList("MASTERCARD");
        String visaCheckoutConfigurationJson = new TestVisaCheckoutConfigurationBuilder()
                .supportedCardTypes("MasterCard")
                .build();

        VisaCheckoutConfiguration visaCheckoutConfiguration = VisaCheckoutConfiguration.fromJson(
                new JSONObject(visaCheckoutConfigurationJson));

        assertEquals(expected, visaCheckoutConfiguration.getAcceptedCardBrands());
    }

    @Test
    public void getAcceptedCardBrands_whenDiscover_returnsDiscover() throws JSONException {
        List<String> expected = Arrays.asList("DISCOVER");
        String visaCheckoutConfigurationJson = new TestVisaCheckoutConfigurationBuilder()
                .supportedCardTypes("Discover")
                .build();

        VisaCheckoutConfiguration visaCheckoutConfiguration = VisaCheckoutConfiguration.fromJson(
                new JSONObject(visaCheckoutConfigurationJson));

        assertEquals(expected, visaCheckoutConfiguration.getAcceptedCardBrands());
    }

    @Test
    public void getAcceptedCardBrands_whenAmericanExpress_returnsAmex() throws JSONException {
        List<String> expected = Arrays.asList("AMEX");
        String visaCheckoutConfigurationJson = new TestVisaCheckoutConfigurationBuilder()
                .supportedCardTypes("American Express")
                .build();

        VisaCheckoutConfiguration visaCheckoutConfiguration = VisaCheckoutConfiguration.fromJson(
                new JSONObject(visaCheckoutConfigurationJson));

        assertEquals(expected, visaCheckoutConfiguration.getAcceptedCardBrands());
    }

    @Test
    public void getAcceptedCardBrands_whenEmpty_returnsEmpty() throws JSONException {
        String visaCheckoutConfigurationJson = new TestVisaCheckoutConfigurationBuilder()
                .build();

        VisaCheckoutConfiguration visaCheckoutConfiguration = VisaCheckoutConfiguration.fromJson(
                new JSONObject(visaCheckoutConfigurationJson));

        assertTrue(visaCheckoutConfiguration.getAcceptedCardBrands().isEmpty());
    }
}
