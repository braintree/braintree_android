package com.braintreepayments.api.models;

import com.braintreepayments.api.internal.ClassHelper;
import com.braintreepayments.testutils.Fixtures;
import com.braintreepayments.testutils.TestConfigurationBuilder;
import com.braintreepayments.testutils.TestConfigurationBuilder.TestSamsungPayConfigurationBuilder;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Set;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ SamsungPayConfiguration.class, ClassHelper.class })
public class SamsungPayConfigurationUnitTest {

    SamsungPayConfiguration mSamsungPayConfiguration;

    @Before
    public void setup() throws JSONException {
        mSamsungPayConfiguration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_SAMSUNGPAY)
                .getSamsungPay();
    }

    @Test
    public void testIsEnabled_returnsTrueWhenSamsungAuthorizationPresentAndSamungPayClassAvailable() {
        SamsungPayConfiguration samsungPayConfiguration = ((Configuration) new TestConfigurationBuilder()
                .samsungPay(new TestSamsungPayConfigurationBuilder()
                        .samsungAuthorization("samsung-authorization"))
                .buildConfiguration())
                .getSamsungPay();

        mockStatic(ClassHelper.class);
        when(ClassHelper.isClassAvailable(eq("com.braintreepayments.api.SamsungPay"))).thenReturn(true);

        assertTrue(samsungPayConfiguration.isEnabled());
    }

    @Test
    public void testIsEnabled_returnsFalseWhenSamsungAuthorizationAbsent() {
        SamsungPayConfiguration samsungPayConfiguration = ((Configuration) new TestConfigurationBuilder()
                .samsungPay(new TestSamsungPayConfigurationBuilder())
                .buildConfiguration())
                .getSamsungPay();

        assertFalse(samsungPayConfiguration.isEnabled());
    }

    @Test
    public void testIsEnabled_returnsFaseWhenSamsungPayClassNotAvailable() {
        SamsungPayConfiguration samsungPayConfiguration = ((Configuration) new TestConfigurationBuilder()
                .samsungPay(new TestSamsungPayConfigurationBuilder()
                        .samsungAuthorization("samsung-authorization"))
                .buildConfiguration())
                .getSamsungPay();

        mockStatic(ClassHelper.class);
        when(ClassHelper.isClassAvailable(eq("com.braintreepayments.api.SamsungPay"))).thenReturn(false);

        assertFalse(samsungPayConfiguration.isEnabled());
    }

    @Test
    public void testParsesDisplayName() {
        assertEquals("some example merchant", mSamsungPayConfiguration.getMerchantDisplayName());
    }

    @Test
    public void testParsesServiceId() {
        assertEquals("some-service-id", mSamsungPayConfiguration.getServiceId());
    }

    @Test
    public void testParsesSupportedCardBrands() {
        Set<String> supportedCardBrands = mSamsungPayConfiguration.getSupportedCardBrands();

        supportedCardBrands.contains("american_express");
        supportedCardBrands.contains("diners");
        supportedCardBrands.contains("discover");
        supportedCardBrands.contains("jcb");
        supportedCardBrands.contains("maestro");
        supportedCardBrands.contains("mastercard");
        supportedCardBrands.contains("visa");
    }

    @Test
    public void testParsesSamsungAuthorization() {
        assertEquals("example-samsung-authorization", mSamsungPayConfiguration.getSamsungAuthorization());
    }

    @Test
    public void testParsesEnvironment() {
        assertEquals("SANDBOX", mSamsungPayConfiguration.getEnvironment());
    }
}
