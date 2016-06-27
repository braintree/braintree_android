package com.braintreepayments.api;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.braintreepayments.api.exceptions.AppSwitchNotAvailableException;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.models.Authorization;
import com.braintreepayments.api.models.ClientToken;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.VenmoAccountNonce;
import com.braintreepayments.api.test.VenmoMockContext;
import com.braintreepayments.testutils.TestConfigurationBuilder;
import com.braintreepayments.testutils.TestConfigurationBuilder.TestVenmoConfigurationBuilder;
import com.braintreepayments.testutils.TestTokenizationKey;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.robolectric.RobolectricGradleTestRunner;

import static com.braintreepayments.api.internal.SignatureVerificationUnitTestUtils.disableSignatureVerification;
import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
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
    public void getLaunchIntent_containsCorrectVenmoExtras() throws JSONException, InvalidArgumentException {
        Configuration configuration = new TestConfigurationBuilder()
                .payWithVenmo(new TestVenmoConfigurationBuilder()
                        .accessToken("access-token")
                        .merchantId("merchant_id")
                        .environment("environment"))
                .buildConfiguration();

        BraintreeFragment fragment = new MockFragmentBuilder()
                .authorization(Authorization.fromString(stringFromFixture("client_token.json")))
                .configuration(configuration)
                .build();
        when(fragment.getSessionId()).thenReturn("session-id");
        when(fragment.getIntegrationType()).thenReturn("custom");

        Intent intent = Venmo.getLaunchIntent(configuration.getPayWithVenmo(), fragment, true);

        assertEquals(new ComponentName("com.venmo", "com.venmo.controller.SetupMerchantActivity"),
                intent.getComponent());
        assertEquals("merchant_id", intent.getStringExtra(Venmo.EXTRA_MERCHANT_ID));
        assertEquals("access-token", intent.getStringExtra(Venmo.EXTRA_ACCESS_TOKEN));
        assertEquals("environment", intent.getStringExtra(Venmo.EXTRA_ENVIRONMENT));

        JSONObject braintreeData = new JSONObject(intent.getStringExtra(Venmo.EXTRA_BRAINTREE_DATA));
        assertEquals("authorization_fingerprint", braintreeData.getString("authorization_fingerprint"));
        assertTrue(braintreeData.getBoolean("validate"));

        JSONObject meta = braintreeData.getJSONObject("_meta");
        assertNotNull(meta);
        assertEquals("session-id", meta.getString("sessionId"));
        assertEquals("custom", meta.getString("integration"));
        assertEquals(BuildConfig.VERSION_NAME, meta.getString("version"));
        assertEquals("android", meta.getString("platform"));
    }

    @Test
    public void authorizeAccount_postsExceptionWhenNotEnabled() throws JSONException {
        Configuration configuration = new TestConfigurationBuilder().buildConfiguration();

        BraintreeFragment fragment = new MockFragmentBuilder()
                .configuration(configuration)
                .build();

        Venmo.authorizeAccount(fragment, false);

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

        Venmo.authorizeAccount(fragment, false);

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

        Venmo.authorizeAccount(fragment, false);

        ArgumentCaptor<AppSwitchNotAvailableException> captor =
                ArgumentCaptor.forClass(AppSwitchNotAvailableException.class);
        verify(fragment).postCallback(captor.capture());
        assertEquals("Venmo is not whitelisted", captor.getValue().getMessage());
    }

    @Test
    public void performAppSwitch_appSwitchesWithVenmoLaunchIntent() throws JSONException, InvalidArgumentException {
        Configuration configuration = new TestConfigurationBuilder()
                .payWithVenmo(new TestVenmoConfigurationBuilder()
                        .accessToken("access-token")
                        .merchantId("merchant_id")
                        .environment("environment"))
                .buildConfiguration();

        ClientToken clientToken = (ClientToken) Authorization.fromString(stringFromFixture("base_64_client_token.txt"));

        Context context = new VenmoMockContext()
                .venmoInstalled()
                .whitelistValue("true")
                .build();
        disableSignatureVerification();
        BraintreeFragment fragment = new MockFragmentBuilder()
                .context(context)
                .authorization(clientToken)
                .configuration(configuration)
                .build();

        when(fragment.getSessionId()).thenReturn("a-session-id");
        when(fragment.getIntegrationType()).thenReturn("custom");

        Venmo.authorizeAccount(fragment, false);

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(fragment).startActivityForResult(captor.capture(), eq(Venmo.VENMO_REQUEST_CODE));
        assertEquals("com.venmo/com.venmo.controller.SetupMerchantActivity",
                captor.getValue().getComponent().flattenToString());
        Bundle extras = captor.getValue().getExtras();
        assertEquals("merchant_id", extras.getString(Venmo.EXTRA_MERCHANT_ID));
        assertEquals("access-token", extras.getString(Venmo.EXTRA_ACCESS_TOKEN));
        assertEquals("environment", extras.getString(Venmo.EXTRA_ENVIRONMENT));

        JSONObject braintreeData = new JSONObject(extras.getString(Venmo.EXTRA_BRAINTREE_DATA));
        assertEquals(clientToken.getAuthorizationFingerprint(), braintreeData.getString("authorization_fingerprint"));
        assertFalse(braintreeData.getBoolean("validate"));

        JSONObject meta = braintreeData.getJSONObject("_meta");
        assertNotNull(meta);
        assertEquals(fragment.getSessionId(), meta.getString("sessionId"));
        assertEquals(fragment.getIntegrationType(), meta.getString("integration"));
        assertEquals(BuildConfig.VERSION_NAME, meta.getString("version"));
        assertEquals("android", meta.getString("platform"));
    }

    @Test
    public void getLaunchIntent_doesNotContainAuthFingerprintWhenUsingTokenziationkey()
            throws JSONException, InvalidArgumentException {
        Configuration configuration = new TestConfigurationBuilder()
                .payWithVenmo(new TestVenmoConfigurationBuilder()
                        .accessToken("access-token")
                        .merchantId("merchant_id")
                        .environment("environment"))
                .buildConfiguration();

        Authorization clientToken = Authorization.fromString(TestTokenizationKey.TOKENIZATION_KEY);

        Context context = new VenmoMockContext()
                .venmoInstalled()
                .whitelistValue("true")
                .build();
        disableSignatureVerification();
        BraintreeFragment fragment = new MockFragmentBuilder()
                .context(context)
                .authorization(clientToken)
                .configuration(configuration)
                .build();

        when(fragment.getSessionId()).thenReturn("a-session-id");
        when(fragment.getIntegrationType()).thenReturn("custom");

        Venmo.authorizeAccount(fragment,  false);

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(fragment).startActivityForResult(captor.capture(), eq(Venmo.VENMO_REQUEST_CODE));
        assertEquals("com.venmo/com.venmo.controller.SetupMerchantActivity",
                captor.getValue().getComponent().flattenToString());
        Bundle extras = captor.getValue().getExtras();
        assertEquals("merchant_id", extras.getString(Venmo.EXTRA_MERCHANT_ID));
        assertEquals("access-token", extras.getString(Venmo.EXTRA_ACCESS_TOKEN));
        assertEquals("environment", extras.getString(Venmo.EXTRA_ENVIRONMENT));

        JSONObject braintreeData = new JSONObject(extras.getString(Venmo.EXTRA_BRAINTREE_DATA));
        assertNull(braintreeData.optString("authorization_fingerprint", null));
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

        Venmo.authorizeAccount(fragment, false);

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

        Venmo.authorizeAccount(fragment, false);

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
