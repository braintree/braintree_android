package com.braintreepayments.api;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.braintreepayments.api.exceptions.AppSwitchNotAvailableException;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.VenmoAccountNonce;
import com.braintreepayments.api.test.VenmoMockContext;
import com.braintreepayments.testutils.TestConfigurationBuilder;
import com.braintreepayments.testutils.TestConfigurationBuilder.TestVenmoConfigurationBuilder;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.robolectric.RobolectricGradleTestRunner;

import static com.braintreepayments.api.internal.SignatureVerificationUnitTestUtils.disableSignatureVerification;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.inOrder;
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
        Configuration configuration = new TestConfigurationBuilder()
                .payWithVenmo(new TestVenmoConfigurationBuilder()
                        .accessToken("access-token")
                        .merchantId("merchant_id")
                        .environment("environment"))
                .buildConfiguration();

        Intent intent = Venmo.getLaunchIntent(configuration.getPayWithVenmo(), "custom", "session-id");

        assertEquals(new ComponentName("com.venmo", "com.venmo.controller.SetupMerchantActivity"),
                intent.getComponent());
        assertEquals("merchant_id", intent.getStringExtra(Venmo.EXTRA_MERCHANT_ID));
        assertEquals("access-token", intent.getStringExtra(Venmo.EXTRA_ACCESS_TOKEN));
        assertEquals("environment", intent.getStringExtra(Venmo.EXTRA_ENVIRONMENT));

        JSONObject json = new JSONObject(intent.getStringExtra(Venmo.EXTRA_METADATA));
        assertNotNull(json);
        assertEquals("session-id", json.getString("sessionId"));
        assertEquals("custom", json.getString("integration"));
        assertEquals(BuildConfig.VERSION_NAME, json.getString("version"));
        assertEquals("android", json.getString("platform"));
    }

    @Test
    public void authorizeAccount_postsExceptionWhenNotEnabled() throws JSONException {
        Configuration configuration = new TestConfigurationBuilder().buildConfiguration();

        BraintreeFragment fragment = new MockFragmentBuilder()
                .configuration(configuration)
                .build();

        Venmo.authorizeAccount(fragment, configuration.getPayWithVenmo());

        ArgumentCaptor<AppSwitchNotAvailableException> captor =
                ArgumentCaptor.forClass(AppSwitchNotAvailableException.class);
        verify(fragment).postCallback(captor.capture());
        assertEquals("Venmo is not enabled", captor.getValue().getMessage());
    }

    @Test
    public void authorizeAccount_postsExceptionWhenNotInstalled() {
         Configuration configuration = new TestConfigurationBuilder()
                .payWithVenmo(new TestVenmoConfigurationBuilder()
                        .accessToken("access-token")
                        .merchantId("merchant_id")
                        .environment("environment"))
                 .buildConfiguration();

        BraintreeFragment fragment = new MockFragmentBuilder()
                .configuration(configuration)
                .build();

        Venmo.authorizeAccount(fragment, configuration.getPayWithVenmo());

        ArgumentCaptor<AppSwitchNotAvailableException> captor =
                ArgumentCaptor.forClass(AppSwitchNotAvailableException.class);
        verify(fragment).postCallback(captor.capture());
        assertEquals("Venmo is not installed", captor.getValue().getMessage());
    }

    @Test
    public void authorizeAccount_postsExceptionWhenNotWhitelisted() {
        Configuration configuration = new TestConfigurationBuilder()
                .payWithVenmo(new TestVenmoConfigurationBuilder()
                        .accessToken("access-token")
                        .merchantId("merchant_id")
                        .environment("environment"))
                .buildConfiguration();

        Context context = new VenmoMockContext()
                .venmoInstalled()
                .whitelistValue("false")
                .build();
        disableSignatureVerification();
        BraintreeFragment fragment = new MockFragmentBuilder()
                .context(context)
                .configuration(configuration)
                .build();

        Venmo.authorizeAccount(fragment, configuration.getPayWithVenmo());

        ArgumentCaptor<AppSwitchNotAvailableException> captor =
                ArgumentCaptor.forClass(AppSwitchNotAvailableException.class);
        verify(fragment).postCallback(captor.capture());
        assertEquals("Venmo is not whitelisted", captor.getValue().getMessage());
    }

    @Test
    public void performAppSwitch_appSwitchesWithVenmoLaunchIntent() throws JSONException {
        Configuration configuration = new TestConfigurationBuilder()
                .payWithVenmo(new TestVenmoConfigurationBuilder()
                        .accessToken("access-token")
                        .merchantId("merchant_id")
                        .environment("environment"))
                .buildConfiguration();

        Context context = new VenmoMockContext()
                .venmoInstalled()
                .whitelistValue("true")
                .build();
        disableSignatureVerification();
        BraintreeFragment fragment = new MockFragmentBuilder()
                .context(context)
                .configuration(configuration)
                .build();
        when(fragment.getSessionId()).thenReturn("a-session-id");
        when(fragment.getIntegrationType()).thenReturn("custom");

        Venmo.authorizeAccount(fragment, configuration.getPayWithVenmo());

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(fragment).startActivityForResult(captor.capture(), eq(Venmo.VENMO_REQUEST_CODE));
        assertEquals("com.venmo/com.venmo.controller.SetupMerchantActivity",
                captor.getValue().getComponent().flattenToString());
        Bundle extras = captor.getValue().getExtras();
        assertEquals("merchant_id", extras.getString(Venmo.EXTRA_MERCHANT_ID));
        assertEquals("access-token", extras.getString(Venmo.EXTRA_ACCESS_TOKEN));
        assertEquals("environment", extras.getString(Venmo.EXTRA_ENVIRONMENT));

        JSONObject json = new JSONObject(extras.getString(Venmo.EXTRA_METADATA));
        assertNotNull(json);
        assertEquals(fragment.getSessionId(), json.getString("sessionId"));
        assertEquals(fragment.getIntegrationType(), json.getString("integration"));
        assertEquals(BuildConfig.VERSION_NAME, json.getString("version"));
        assertEquals("android", json.getString("platform"));
    }

    @Test
    public void performAppSwitch_sendsAnalyticsEvent() {
        Configuration configuration = new TestConfigurationBuilder()
                .payWithVenmo(new TestVenmoConfigurationBuilder()
                        .accessToken("access-token")
                        .merchantId("merchant_id")
                        .environment("environment"))
                .buildConfiguration();

        BraintreeFragment fragment = new MockFragmentBuilder()
                .configuration(configuration)
                .build();

        Venmo.authorizeAccount(fragment);

        verify(fragment).sendAnalyticsEvent("pay-with-venmo.selected");
    }

    @Test
    public void performAppSwitch_sendsAnalyticsEventWhenStarted() {
        Configuration configuration = new TestConfigurationBuilder()
                .payWithVenmo(new TestVenmoConfigurationBuilder()
                        .accessToken("access-token")
                        .merchantId("merchant_id")
                        .environment("environment"))
                .buildConfiguration();

        Context context = new VenmoMockContext()
                .whitelistValue("true")
                .venmoInstalled()
                .build();
        BraintreeFragment fragment = new MockFragmentBuilder()
                .context(context)
                .configuration(configuration)
                .build();

        Venmo.authorizeAccount(fragment, configuration.getPayWithVenmo());

        verify(fragment).sendAnalyticsEvent("pay-with-venmo.selected");
        verify(fragment).sendAnalyticsEvent("pay-with-venmo.app-switch.started");
    }

    @Test
    public void performAppSwitch_sendsAnalyticsEventWhenUnavailableAndPostException() {
        Configuration configuration = new TestConfigurationBuilder()
                .payWithVenmo(new TestVenmoConfigurationBuilder()
                        .accessToken("access-token")
                        .merchantId("merchant_id")
                        .environment("environment"))
                .buildConfiguration();

        BraintreeFragment fragment = new MockFragmentBuilder()
                .configuration(configuration)
                .build();

        Venmo.authorizeAccount(fragment, configuration.getPayWithVenmo());

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
