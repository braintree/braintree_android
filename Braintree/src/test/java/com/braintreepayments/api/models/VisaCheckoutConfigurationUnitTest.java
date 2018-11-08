package com.braintreepayments.api.models;

import com.braintreepayments.api.internal.ClassHelper;
import com.braintreepayments.testutils.TestConfigurationBuilder.TestVisaCheckoutConfigurationBuilder;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Arrays;
import java.util.List;

import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ VisaCheckoutConfiguration.class, ClassHelper.class })
public class VisaCheckoutConfigurationUnitTest {

    private JSONObject mVisaCheckoutConfigurationJson;

    @Before
    public void setup() throws JSONException {
        JSONObject configuration = new JSONObject(stringFromFixture("configuration/with_visa_checkout.json"));
        mVisaCheckoutConfigurationJson = configuration.getJSONObject("visaCheckout");
    }

    @Test
    public void isEnabled_returnsFalseWhenConfigurationApiKeyDoesntExist() throws JSONException {
        JSONObject blankVisaCheckoutJson = new JSONObject("{\"visaCheckout\":{}}");
        VisaCheckoutConfiguration visaCheckoutConfiguration = VisaCheckoutConfiguration.fromJson(blankVisaCheckoutJson);

        assertFalse(visaCheckoutConfiguration.isEnabled());
    }

    @Test
    public void isEnabled_returnsTrueWhenConfigurationApiKeyExists() throws JSONException {
        VisaCheckoutConfiguration visaCheckoutConfiguration = VisaCheckoutConfiguration.fromJson(
                mVisaCheckoutConfigurationJson);

        assertTrue(visaCheckoutConfiguration.isEnabled());
    }

    @Test
    public void getApiKey_returnsApiKeyWhenConfigurationExists() throws JSONException {
        VisaCheckoutConfiguration visaCheckoutConfiguration = VisaCheckoutConfiguration.fromJson(
                mVisaCheckoutConfigurationJson);

        assertEquals("gwApikey", visaCheckoutConfiguration.getApiKey());
    }

    @Test
    public void getExternalClientId_returnsExternalClientIdWhenConfigurationExists() throws JSONException {
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
