package com.braintreepayments.api;

import android.os.Bundle;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.SmallTest;

import com.braintreepayments.api.models.AndroidPayConfiguration;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.test.TestActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.braintreepayments.api.BraintreeFragmentTestUtils.getMockFragment;
import static junit.framework.Assert.assertEquals;
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
}
