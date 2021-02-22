package com.braintreepayments.api;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Arrays;
import java.util.List;

import static junit.framework.Assert.assertFalse;
import static org.junit.Assert.assertEquals;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ SamsungPayConfiguration.class, ClassHelper.class })
public class SamsungPayConfigurationUnitTest {

    @Test
    public void fromJson_parsesFullInput() throws JSONException {
        JSONObject input = new JSONObject()
                .put("displayName", "sample display name")
                .put("serviceId", "sample-service-id")
                .put("samsungAuthorization", "sample-samsung-authorization")
                .put("environment", "SANDBOX")
                .put("supportedCardBrands", new JSONArray()
                        .put("american_express")
                        .put("diners")
                        .put("discover")
                        .put("jcb")
                        .put("maestro")
                        .put("mastercard")
                        .put("visa")
                );
        SamsungPayConfiguration sut = SamsungPayConfiguration.fromJson(input);

        // TODO: remove classpath check for Samsung Library to check if samsung is enabled
        //assertTrue(sut.isEnabled());
        assertEquals("sample display name", sut.getMerchantDisplayName());
        assertEquals("sample-service-id", sut.getServiceId());
        assertEquals("sample-samsung-authorization", sut.getSamsungAuthorization());
        assertEquals("SANDBOX", sut.getEnvironment());

        List<String> expected = Arrays.asList(
                "american_express", "diners", "discover", "jcb", "maestro", "mastercard", "visa");
        assertEquals(expected, sut.getSupportedCardBrands());
    }

    @Test
    public void fromJson_whenInputNull_returnsConfigWithDefaultValues() {
        SamsungPayConfiguration sut = SamsungPayConfiguration.fromJson(null);
        assertFalse(sut.isEnabled());
        assertEquals("", sut.getEnvironment());
        assertEquals("", sut.getMerchantDisplayName());
        assertEquals("", sut.getSamsungAuthorization());
        assertEquals("", sut.getServiceId());
        assertEquals(0, sut.getSupportedCardBrands().size());
    }

    @Test
    public void fromJson_whenInputEmpty_returnsConfigWithDefaultValues() {
        SamsungPayConfiguration sut = SamsungPayConfiguration.fromJson(new JSONObject());
        assertFalse(sut.isEnabled());
        assertEquals("", sut.getEnvironment());
        assertEquals("", sut.getMerchantDisplayName());
        assertEquals("", sut.getSamsungAuthorization());
        assertEquals("", sut.getServiceId());
        assertEquals(0, sut.getSupportedCardBrands().size());
    }
}
