package com.braintreepayments.api;

import android.os.Bundle;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.SmallTest;

import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.models.AndroidPayConfiguration;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.test.TestActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.braintreepayments.api.BraintreeFragmentTestUtils.getMockFragment;
import static com.braintreepayments.testutils.TestClientKey.CLIENT_KEY;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
public class AndroidPayTest {

    @Rule
    public final ActivityTestRule<TestActivity> mActivityTestRule =
            new ActivityTestRule<>(TestActivity.class);

    @Test(timeout = 1000)
    @SmallTest
    public void getTokenizationParameters_returnsCorrectParameters() {
        AndroidPayConfiguration androidPayConfiguration = mock(AndroidPayConfiguration.class);
        when(androidPayConfiguration.getGoogleAuthorizationFingerprint()).thenReturn("google-auth-fingerprint");
        Configuration configuration = mock(Configuration.class);
        when(configuration.getMerchantId()).thenReturn("android-pay-merchant-id");
        when(configuration.getAndroidPay()).thenReturn(androidPayConfiguration);
        BraintreeFragment fragment = getMockFragment(mActivityTestRule.getActivity(), configuration);

        Bundle tokenizationParameters = AndroidPay.getTokenizationParameters(fragment).getParameters();

        assertEquals("braintree", tokenizationParameters.getString("gateway"));
        assertEquals(configuration.getMerchantId(), tokenizationParameters.getString("braintree:merchantId"));
        assertEquals(androidPayConfiguration.getGoogleAuthorizationFingerprint(), tokenizationParameters.getString("braintree:authorizationFingerprint"));
        assertEquals("v1", tokenizationParameters.getString("braintree:apiVersion"));
        assertEquals(BuildConfig.VERSION_NAME, tokenizationParameters.getString("braintree:sdkVersion"));
    }

    @Test(timeout = 1000)
    @SmallTest
    public void getTokenizationParameters_doesNotIncludeAClientKeyWhenNotPresent() {
        AndroidPayConfiguration androidPayConfiguration = mock(AndroidPayConfiguration.class);
        when(androidPayConfiguration.getGoogleAuthorizationFingerprint()).thenReturn("google-auth-fingerprint");
        Configuration configuration = mock(Configuration.class);
        when(configuration.getMerchantId()).thenReturn("android-pay-merchant-id");
        when(configuration.getAndroidPay()).thenReturn(androidPayConfiguration);
        BraintreeFragment fragment = getMockFragment(mActivityTestRule.getActivity(), configuration);

        Bundle tokenizationParameters = AndroidPay.getTokenizationParameters(fragment).getParameters();

        assertNull(tokenizationParameters.getString("braintree:clientKey"));
    }

    @Test(timeout = 1000)
    @SmallTest
    public void getTokenizationParameters_includesAClientKeyWhenPresent()
            throws InvalidArgumentException {
        AndroidPayConfiguration androidPayConfiguration = mock(AndroidPayConfiguration.class);
        when(androidPayConfiguration.getGoogleAuthorizationFingerprint()).thenReturn(
                "google-auth-fingerprint");
        Configuration configuration = mock(Configuration.class);
        when(configuration.getMerchantId()).thenReturn("android-pay-merchant-id");
        when(configuration.getAndroidPay()).thenReturn(androidPayConfiguration);
        BraintreeFragment fragment = getMockFragment(mActivityTestRule.getActivity(), CLIENT_KEY,
                configuration);

        Bundle tokenizationParameters = AndroidPay.getTokenizationParameters(fragment).getParameters();

        assertEquals(CLIENT_KEY, tokenizationParameters.getString("braintree:clientKey"));
    }
}
