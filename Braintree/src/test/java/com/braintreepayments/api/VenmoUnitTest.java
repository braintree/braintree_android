package com.braintreepayments.api;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.braintreepayments.api.exceptions.AppSwitchNotAvailableException;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.VenmoAccountNonce;
import com.braintreepayments.api.models.VenmoConfiguration;
import com.braintreepayments.api.test.VenmoMockContext;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.robolectric.RobolectricGradleTestRunner;

import static com.braintreepayments.api.internal.SignatureVerificationUnitTestUtils.disableSignatureVerification;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricGradleTestRunner.class)
public class VenmoUnitTest {

    @Test
    public void packageIsCorrect() {
        assertEquals("com.venmo", Venmo.PACKAGE_NAME);
    }

    @Test
    public void appSwitchActivityIsCorrect() {
        assertEquals("controller.SetupMerchantActivity", Venmo.APP_SWITCH_ACTIVITY);
    }

    @Test
    public void certificateSubjectIsCorrect() {
        assertEquals("CN=Andrew Kortina,OU=Engineering,O=Venmo,L=Philadelphia,ST=PA,C=US", Venmo.CERTIFICATE_SUBJECT);
    }

    @Test
    public void certificateIssuerIsCorrect() {
        assertEquals("CN=Andrew Kortina,OU=Engineering,O=Venmo,L=Philadelphia,ST=PA,C=US", Venmo.CERTIFICATE_ISSUER);
    }

    @Test
    public void publicKeyHashCodeIsCorrect() {
        assertEquals(-129711843, Venmo.PUBLIC_KEY_HASH_CODE);
    }

    @Test
    public void isVenmoInstalled_returnsTrueWhenInstalled() {
        Context context = new VenmoMockContext()
                .whitelistValue("true")
                .venmoInstalled()
                .build();
        disableSignatureVerification();

        assertTrue(Venmo.isVenmoInstalled(context));
    }

    @Test
    public void containsCorrectVenmoExtras() throws JSONException {
        Configuration configuration = mock(Configuration.class);
        VenmoConfiguration venmoConfiguration = mock(VenmoConfiguration.class);
        when(configuration.getPayWithVenmo()).thenReturn(venmoConfiguration);
        when(configuration.getMerchantId()).thenReturn("merchant_id");
        when(venmoConfiguration.getAccessToken()).thenReturn("access-token");
        when(configuration.getEnvironment()).thenReturn("environment");

        Intent intent = Venmo.getLaunchIntent(configuration);

        assertEquals(new ComponentName("com.venmo", "com.venmo.controller.SetupMerchantActivity"),
                intent.getComponent());
        assertEquals("merchant_id", intent.getStringExtra(Venmo.EXTRA_MERCHANT_ID));
        assertEquals("access-token", intent.getStringExtra(Venmo.EXTRA_ACCESS_TOKEN));
        assertEquals(BuildConfig.VERSION_NAME, intent.getStringExtra(Venmo.EXTRA_SDK_VERSION));
        assertEquals("environment", intent.getStringExtra(Venmo.EXTRA_ENVIRONMENT));
    }

    @Test
    public void authorizeAccount_postsExceptionWhenNotEnabled() {
        Configuration configuration = mock(Configuration.class);
        VenmoConfiguration venmoConfiguration = mock(VenmoConfiguration.class);
        BraintreeFragment fragment = new MockFragmentBuilder()
                .configuration(configuration)
                .build();
        when(configuration.getMerchantId()).thenReturn("merchant_id");
        when(configuration.getEnvironment()).thenReturn("environment");
        when(configuration.getPayWithVenmo()).thenReturn(venmoConfiguration);

        Venmo.authorizeAccount(fragment, configuration);

        ArgumentCaptor<AppSwitchNotAvailableException> captor =
                ArgumentCaptor.forClass(AppSwitchNotAvailableException.class);
        verify(fragment).postCallback(captor.capture());
        assertEquals("Venmo is not enabled", captor.getValue().getMessage());
    }

    @Test
    public void authorizeAccount_postsExceptionWhenNotInstalled() {
        VenmoConfiguration venmoConfiguration = mock(VenmoConfiguration.class);
        when(venmoConfiguration.getAccessToken()).thenReturn("access-token");
        when(venmoConfiguration.isAccessTokenValid()).thenReturn(true);
        when(venmoConfiguration.isVenmoWhitelisted(any(ContentResolver.class))).thenReturn(true);
        Configuration configuration = mock(Configuration.class);
        when(configuration.getMerchantId()).thenReturn("merchant_id");
        when(configuration.getEnvironment()).thenReturn("environment");
        when(configuration.getPayWithVenmo()).thenReturn(venmoConfiguration);
        BraintreeFragment fragment = new MockFragmentBuilder()
                .configuration(configuration)
                .build();

        Venmo.authorizeAccount(fragment, configuration);

        ArgumentCaptor<AppSwitchNotAvailableException> captor =
                ArgumentCaptor.forClass(AppSwitchNotAvailableException.class);
        verify(fragment).postCallback(captor.capture());
        assertEquals("Venmo is not installed", captor.getValue().getMessage());
    }

    @Test
    public void authorizeAccount_postsExceptionWhenNotWhitelisted() {
        Configuration configuration = mock(Configuration.class);
        VenmoConfiguration venmoConfiguration = mock(VenmoConfiguration.class);
        when(venmoConfiguration.getAccessToken()).thenReturn("access-token");
        when(venmoConfiguration.isAccessTokenValid()).thenReturn(true);
        when(configuration.getMerchantId()).thenReturn("merchant_id");
        when(configuration.getEnvironment()).thenReturn("environment");
        when(configuration.getPayWithVenmo()).thenReturn(venmoConfiguration);
        Context context = new VenmoMockContext()
                .venmoInstalled()
                .whitelistValue("true")
                .build();
        disableSignatureVerification();
        BraintreeFragment fragment = new MockFragmentBuilder()
                .context(context)
                .configuration(configuration)
                .build();

        Venmo.authorizeAccount(fragment, configuration);

        ArgumentCaptor<AppSwitchNotAvailableException> captor =
                ArgumentCaptor.forClass(AppSwitchNotAvailableException.class);
        verify(fragment).postCallback(captor.capture());
        assertEquals("Venmo is not whitelisted", captor.getValue().getMessage());
    }

    @Test(timeout = 1000)
    public void performAppSwitch_appSwitchesWithVenmoLaunchIntent() {
        VenmoConfiguration venmoConfiguration = mock(VenmoConfiguration.class);
        when(venmoConfiguration.getAccessToken()).thenReturn("access-token");
        when(venmoConfiguration.isAccessTokenValid()).thenReturn(true);
        when(venmoConfiguration.isVenmoWhitelisted(any(ContentResolver.class))).thenReturn(true);
        Configuration configuration = mock(Configuration.class);
        when(configuration.getMerchantId()).thenReturn("merchant_id");
        when(configuration.getEnvironment()).thenReturn("environment");
        when(configuration.getPayWithVenmo()).thenReturn(venmoConfiguration);
        Context context = new VenmoMockContext()
                .venmoInstalled()
                .whitelistValue("true")
                .build();
        disableSignatureVerification();
        BraintreeFragment fragment = new MockFragmentBuilder()
                .context(context)
                .configuration(configuration)
                .build();

        Venmo.authorizeAccount(fragment, configuration);

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(fragment).startActivityForResult(captor.capture(), eq(Venmo.VENMO_REQUEST_CODE));
        assertEquals("com.venmo/com.venmo.controller.SetupMerchantActivity",
                captor.getValue().getComponent().flattenToString());
        Bundle extras = captor.getValue().getExtras();
        assertEquals("merchant_id", extras.getString(Venmo.EXTRA_MERCHANT_ID));
        assertEquals("access-token", extras.getString(Venmo.EXTRA_ACCESS_TOKEN));
        assertEquals(BuildConfig.VERSION_NAME, extras.getString(Venmo.EXTRA_SDK_VERSION));
        assertEquals("environment", extras.getString(Venmo.EXTRA_ENVIRONMENT));
    }

    @Test
    public void performAppSwitch_sendsAnalyticsEvent() {
        VenmoConfiguration venmoConfiguration = mock(VenmoConfiguration.class);
        when(venmoConfiguration.getAccessToken()).thenReturn("access-token");
        Configuration configuration = mock(Configuration.class);
        when(configuration.getPayWithVenmo()).thenReturn(venmoConfiguration);
        BraintreeFragment fragment = new MockFragmentBuilder()
                .configuration(configuration)
                .build();

        Venmo.authorizeAccount(fragment);

        verify(fragment).sendAnalyticsEvent("pay-with-venmo.selected");
    }

    @Test
    public void performAppSwitch_sendsAnalyticsEventWhenStarted() {
        VenmoConfiguration venmoConfiguration = mock(VenmoConfiguration.class);
        when(venmoConfiguration.isAccessTokenValid()).thenReturn(true);
        when(venmoConfiguration.isVenmoWhitelisted(any(ContentResolver.class))).thenReturn(true);
        Configuration configuration = mock(Configuration.class);
        when(configuration.getPayWithVenmo()).thenReturn(venmoConfiguration);
        Context context = new VenmoMockContext()
                .whitelistValue("true")
                .venmoInstalled()
                .build();
        BraintreeFragment fragment = new MockFragmentBuilder()
                .context(context)
                .configuration(configuration)
                .build();

        Venmo.authorizeAccount(fragment, configuration);

        verify(fragment).sendAnalyticsEvent("pay-with-venmo.selected");
        verify(fragment).sendAnalyticsEvent("pay-with-venmo.app-switch.started");
    }

    @Test
    public void performAppSwitch_sendsAnalyticsEventWhenUnavailableAndPostException() {
        VenmoConfiguration venmoConfiguration = mock(VenmoConfiguration.class);
        when(venmoConfiguration.isAccessTokenValid()).thenReturn(true);
        Configuration configuration = mock(Configuration.class);
        when(configuration.getPayWithVenmo()).thenReturn(venmoConfiguration);
        BraintreeFragment fragment = new MockFragmentBuilder()
                .configuration(configuration)
                .build();

        Venmo.authorizeAccount(fragment, configuration);

        ArgumentCaptor<AppSwitchNotAvailableException> captor =
                ArgumentCaptor.forClass(AppSwitchNotAvailableException.class);
        InOrder order = inOrder(fragment);
        order.verify(fragment).sendAnalyticsEvent("pay-with-venmo.selected");
        order.verify(fragment).sendAnalyticsEvent("pay-with-venmo.app-switch.failed");
        verify(fragment).postCallback(captor.capture());
        assertEquals("Venmo is not installed", captor.getValue().getMessage());
    }

    @Test
    public void onActivityResult_postsPaymentMethodNonceOnSuccess() {
        BraintreeFragment fragment = new MockFragmentBuilder()
                .build();
        Intent intent = new Intent()
                .putExtra(Venmo.EXTRA_PAYMENT_METHOD_NONCE, "123456-12345-12345-a-adfa")
                .putExtra(Venmo.EXTRA_USERNAME, "username");

        Venmo.onActivityResult(fragment, Activity.RESULT_OK, intent);

        ArgumentCaptor<VenmoAccountNonce> captor = ArgumentCaptor.forClass(VenmoAccountNonce.class);
        verify(fragment).postCallback(captor.capture());
        assertEquals("123456-12345-12345-a-adfa", captor.getValue().getNonce());
        assertEquals("username", captor.getValue().getDescription());
        assertEquals("username", captor.getValue().getUsername());
    }

    @Test
    public void onActivityResult_sendsAnalyticsEventOnSuccess() {
        BraintreeFragment fragment = new MockFragmentBuilder()
                .build();
        Intent intent = new Intent()
                .putExtra(Venmo.EXTRA_PAYMENT_METHOD_NONCE, "123456-12345-12345-a-adfa")
                .putExtra(Venmo.EXTRA_USERNAME, "username");

        Venmo.onActivityResult(fragment, Activity.RESULT_OK, intent);

        verify(fragment).sendAnalyticsEvent("pay-with-venmo.app-switch.success");
    }

    @Test
    public void onActivityResult_sendsAnalyticsEventOnCancel() {
        BraintreeFragment fragment = new MockFragmentBuilder()
                .build();

        Venmo.onActivityResult(fragment, Activity.RESULT_CANCELED, new Intent());

        verify(fragment).sendAnalyticsEvent("pay-with-venmo.app-switch.canceled");
    }
}
