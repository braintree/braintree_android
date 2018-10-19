package com.braintreepayments.api;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.braintreepayments.api.exceptions.AppSwitchNotAvailableException;
import com.braintreepayments.api.exceptions.AuthorizationException;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCallback;
import com.braintreepayments.api.models.Authorization;
import com.braintreepayments.api.models.BraintreeRequestCodes;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.api.models.VenmoAccountBuilder;
import com.braintreepayments.api.models.VenmoAccountNonce;
import com.braintreepayments.api.test.VenmoInstalledContextFactory;
import com.braintreepayments.testutils.SharedPreferencesHelper;
import com.braintreepayments.testutils.TestConfigurationBuilder;
import com.braintreepayments.testutils.TestTokenizationKey;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.security.NoSuchAlgorithmException;

import static com.braintreepayments.api.internal.SignatureVerificationUnitTestUtils.disableSignatureVerification;
import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static com.braintreepayments.testutils.SharedPreferencesHelper.clearSharedPreferences;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.endsWith;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(RobolectricTestRunner.class)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*", "org.json.*" })
@PrepareForTest({ Venmo.class, TokenizationClient.class })
public class VenmoUnitTest {

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    @Before
    public void setup() {
        clearSharedPreferences(RuntimeEnvironment.application);
    }

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
        disableSignatureVerification();
        assertTrue(Venmo.isVenmoInstalled(VenmoInstalledContextFactory.venmoInstalledContext(true)));
    }

    @Test
    public void openVenmoAppPageInGooglePlay_opensVenmoAppStoreURL()
            throws JSONException {
        Configuration configuration = new TestConfigurationBuilder().buildConfiguration();

        BraintreeFragment fragment = new MockFragmentBuilder()
                .configuration(configuration)
                .build();

        Venmo.openVenmoAppPageInGooglePlay(fragment);

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);

        verify(fragment).startActivity(captor.capture());
        assertEquals(captor.getValue().getData().toString(),"https://play.google.com/store/apps/details?id=com.venmo");
    }

    @Test
    public void openVenmoAppPageInGooglePlay_sendsAnalyticsEvent()
            throws JSONException {
        Configuration configuration = new TestConfigurationBuilder().buildConfiguration();

        BraintreeFragment fragment = new MockFragmentBuilder()
                .configuration(configuration)
                .build();

        Venmo.openVenmoAppPageInGooglePlay(fragment);

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);

        verify(fragment).startActivity(captor.capture());
        verify(fragment).sendAnalyticsEvent("android.pay-with-venmo.app-store.invoked");
    }

    @Test
    public void getLaunchIntent_containsCorrectVenmoExtras() throws JSONException, InvalidArgumentException {
        Configuration configuration = getConfigurationFromFixture();

        BraintreeFragment fragment = new MockFragmentBuilder()
                .authorization(Authorization.fromString(stringFromFixture("client_token.json")))
                .configuration(configuration)
                .sessionId("session-id")
                .build();
        when(fragment.getIntegrationType()).thenReturn("custom");

        Intent intent = Venmo.getLaunchIntent(configuration.getPayWithVenmo(),
                configuration.getPayWithVenmo().getMerchantId(), fragment);

        assertEquals(new ComponentName("com.venmo", "com.venmo.controller.SetupMerchantActivity"),
                intent.getComponent());
        assertEquals("merchant-id", intent.getStringExtra(Venmo.EXTRA_MERCHANT_ID));
        assertEquals("access-token", intent.getStringExtra(Venmo.EXTRA_ACCESS_TOKEN));
        assertEquals("environment", intent.getStringExtra(Venmo.EXTRA_ENVIRONMENT));

        JSONObject braintreeData = new JSONObject(intent.getStringExtra(Venmo.EXTRA_BRAINTREE_DATA));
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
        Configuration configuration = getConfigurationFromFixture();

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
    public void performAppSwitch_appSwitchesWithVenmoLaunchIntent() throws InvalidArgumentException, JSONException {
        Configuration configuration = getConfigurationFromFixture();

        Authorization clientToken = Authorization.fromString(stringFromFixture("base_64_client_token.txt"));
        disableSignatureVerification();

        BraintreeFragment fragment = new MockFragmentBuilder()
                .context(VenmoInstalledContextFactory.venmoInstalledContext(true, RuntimeEnvironment.application))
                .authorization(clientToken)
                .configuration(configuration)
                .build();

        when(fragment.getSessionId()).thenReturn("a-session-id");
        when(fragment.getIntegrationType()).thenReturn("custom");

        Venmo.authorizeAccount(fragment, false);

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(fragment).startActivityForResult(captor.capture(), eq(BraintreeRequestCodes.VENMO));
        assertEquals("com.venmo/com.venmo.controller.SetupMerchantActivity",
                captor.getValue().getComponent().flattenToString());
        Bundle extras = captor.getValue().getExtras();
        assertEquals("merchant-id", extras.getString(Venmo.EXTRA_MERCHANT_ID));
        assertEquals("access-token", extras.getString(Venmo.EXTRA_ACCESS_TOKEN));
        assertEquals("environment", extras.getString(Venmo.EXTRA_ENVIRONMENT));

        JSONObject braintreeData = new JSONObject(extras.getString(Venmo.EXTRA_BRAINTREE_DATA));

        JSONObject meta = braintreeData.getJSONObject("_meta");
        assertNotNull(meta);
        assertEquals(fragment.getSessionId(), meta.getString("sessionId"));
        assertEquals(fragment.getIntegrationType(), meta.getString("integration"));
        assertEquals(BuildConfig.VERSION_NAME, meta.getString("version"));
        assertEquals("android", meta.getString("platform"));
    }

    @Test
    public void performAppSwitch_whenProfileIdIsNull_appSwitchesWithMerchantId() throws InvalidArgumentException, JSONException {
        Configuration configuration = getConfigurationFromFixture();

        Authorization clientToken = Authorization.fromString(stringFromFixture("base_64_client_token.txt"));
        disableSignatureVerification();

        BraintreeFragment fragment = new MockFragmentBuilder()
                .context(VenmoInstalledContextFactory.venmoInstalledContext(true, RuntimeEnvironment.application))
                .authorization(clientToken)
                .configuration(configuration)
                .build();

        when(fragment.getSessionId()).thenReturn("a-session-id");
        when(fragment.getIntegrationType()).thenReturn("custom");

        Venmo.authorizeAccount(fragment, false, null);

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(fragment).startActivityForResult(captor.capture(), eq(BraintreeRequestCodes.VENMO));
        assertEquals("com.venmo/com.venmo.controller.SetupMerchantActivity",
                captor.getValue().getComponent().flattenToString());
        Bundle extras = captor.getValue().getExtras();
        assertEquals("merchant-id", extras.getString(Venmo.EXTRA_MERCHANT_ID));
        assertEquals("access-token", extras.getString(Venmo.EXTRA_ACCESS_TOKEN));
    }

    @Test
    public void performAppSwitch_whenProfileIdIsSpecified_appSwitchesWithProfileIdAndAccessToken() throws InvalidArgumentException, JSONException {
        Configuration configuration = getConfigurationFromFixture();

        Authorization clientToken = Authorization.fromString(stringFromFixture("base_64_client_token.txt"));
        disableSignatureVerification();

        BraintreeFragment fragment = new MockFragmentBuilder()
                .context(VenmoInstalledContextFactory.venmoInstalledContext(true, RuntimeEnvironment.application))
                .authorization(clientToken)
                .configuration(configuration)
                .build();

        when(fragment.getSessionId()).thenReturn("a-session-id");
        when(fragment.getIntegrationType()).thenReturn("custom");

        Venmo.authorizeAccount(fragment, false, "second-pwv-profile-id");

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(fragment).startActivityForResult(captor.capture(), eq(BraintreeRequestCodes.VENMO));
        assertEquals("com.venmo/com.venmo.controller.SetupMerchantActivity",
                captor.getValue().getComponent().flattenToString());
        Bundle extras = captor.getValue().getExtras();
        assertEquals("second-pwv-profile-id", extras.getString(Venmo.EXTRA_MERCHANT_ID));
        assertEquals("access-token", extras.getString(Venmo.EXTRA_ACCESS_TOKEN));
    }

    @Test
    public void getLaunchIntent_doesNotContainAuthFingerprintWhenUsingTokenziationkey()
            throws JSONException, InvalidArgumentException {
        Configuration configuration = getConfigurationFromFixture();

        Authorization clientToken = Authorization.fromString(TestTokenizationKey.TOKENIZATION_KEY);
        disableSignatureVerification();

        BraintreeFragment fragment = new MockFragmentBuilder()
                .context(VenmoInstalledContextFactory.venmoInstalledContext(true, RuntimeEnvironment.application))
                .authorization(clientToken)
                .configuration(configuration)
                .build();

        when(fragment.getSessionId()).thenReturn("a-session-id");
        when(fragment.getIntegrationType()).thenReturn("custom");

        Venmo.authorizeAccount(fragment,  false);

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(fragment).startActivityForResult(captor.capture(), eq(BraintreeRequestCodes.VENMO));
        assertEquals("com.venmo/com.venmo.controller.SetupMerchantActivity",
                captor.getValue().getComponent().flattenToString());
        Bundle extras = captor.getValue().getExtras();
        assertEquals("merchant-id", extras.getString(Venmo.EXTRA_MERCHANT_ID));
        assertEquals("access-token", extras.getString(Venmo.EXTRA_ACCESS_TOKEN));
        assertEquals("environment", extras.getString(Venmo.EXTRA_ENVIRONMENT));

        JSONObject braintreeData = new JSONObject(extras.getString(Venmo.EXTRA_BRAINTREE_DATA));
        assertNull(Json.optString(braintreeData, "authorization_fingerprint", null));
    }

    @Test
    public void performAppSwitch_sendsAnalyticsEvent() {
        Configuration configuration = getConfigurationFromFixture();

        BraintreeFragment fragment = new MockFragmentBuilder()
                .configuration(configuration)
                .build();

        Venmo.authorizeAccount(fragment);

        verify(fragment).sendAnalyticsEvent("pay-with-venmo.selected");
    }

    @Test
    public void performAppSwitch_sendsAnalyticsEventWhenStarted() {
        Configuration configuration = getConfigurationFromFixture();

        BraintreeFragment fragment = new MockFragmentBuilder()
                .context(VenmoInstalledContextFactory.venmoInstalledContext(true, RuntimeEnvironment.application))
                .configuration(configuration)
                .build();

        Venmo.authorizeAccount(fragment, false);

        verify(fragment).sendAnalyticsEvent("pay-with-venmo.selected");
        verify(fragment).sendAnalyticsEvent("pay-with-venmo.app-switch.started");
    }

    @Test
    public void performAppSwitch_persistsIfVaultTrue() throws InvalidArgumentException {
        Configuration configuration = getConfigurationFromFixture();

        Authorization clientToken = Authorization.fromString(stringFromFixture("base_64_client_token.txt"));
        disableSignatureVerification();
        BraintreeFragment fragment = new MockFragmentBuilder()
                .context(VenmoInstalledContextFactory.venmoInstalledContext(true, RuntimeEnvironment.application))
                .configuration(configuration)
                .authorization(clientToken)
                .build();

        Venmo.authorizeAccount(fragment, true);

        SharedPreferences prefs = SharedPreferencesHelper.getSharedPreferences(fragment.getApplicationContext());
        assertTrue(prefs.getBoolean("com.braintreepayments.api.Venmo.VAULT_VENMO_KEY", false));
    }

    @Test
    public void performAppSwitch_persistsIfVaultFalse() throws InvalidArgumentException {
        Configuration configuration = getConfigurationFromFixture();

        Authorization clientToken = Authorization.fromString(stringFromFixture("base_64_client_token.txt"));
        disableSignatureVerification();

        BraintreeFragment fragment = new MockFragmentBuilder()
                .context(VenmoInstalledContextFactory.venmoInstalledContext(true, RuntimeEnvironment.application))
                .configuration(configuration)
                .authorization(clientToken)
                .build();

        Venmo.authorizeAccount(fragment, false);

        SharedPreferences prefs = SharedPreferencesHelper.getSharedPreferences(fragment.getApplicationContext());
        assertFalse(prefs.getBoolean("com.braintreepayments.api.Venmo.VAULT_VENMO_KEY", true));
    }

    @Test
    public void performAppSwitch_persistsFalseIfTokenizationKeyUsed() throws InvalidArgumentException {
        Configuration configuration = getConfigurationFromFixture();

        Authorization tokenizationkey = Authorization.fromString("sandbox_tk_abcd");
        disableSignatureVerification();
        BraintreeFragment fragment = new MockFragmentBuilder()
                .context(VenmoInstalledContextFactory.venmoInstalledContext(true, RuntimeEnvironment.application))
                .configuration(configuration)
                .authorization(tokenizationkey)
                .sessionId("session-id")
                .build();

        Venmo.authorizeAccount(fragment, true);

        SharedPreferences prefs = SharedPreferencesHelper.getSharedPreferences(fragment.getApplicationContext());
        assertFalse(prefs.getBoolean("com.braintreepayments.api.Venmo.VAULT_VENMO_KEY", true));
    }

    @Test
    public void performAppSwitch_sendsAnalyticsEventWhenUnavailableAndPostException() {
        Configuration configuration = getConfigurationFromFixture();

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

    @Test
    public void onActivityResult_performsVaultRequestIfRequestPersisted()
            throws InvalidArgumentException, NoSuchAlgorithmException {
        Configuration configuration = getConfigurationFromFixture();

        Authorization clientToken = Authorization.fromString(stringFromFixture("base_64_client_token.txt"));
        disableSignatureVerification();
        BraintreeFragment fragment = new MockFragmentBuilder()
                .context(VenmoInstalledContextFactory.venmoInstalledContext(true, RuntimeEnvironment.application))
                .configuration(configuration)
                .authorization(clientToken)
                .sessionId("session-id")
                .build();

        Venmo.authorizeAccount(fragment, true);

        mockStatic(TokenizationClient.class);
        Venmo.onActivityResult(fragment, Activity.RESULT_OK, new Intent());

        verifyStatic();
        TokenizationClient.tokenize(eq(fragment), any(VenmoAccountBuilder.class), any(PaymentMethodNonceCallback.class));
    }

    @Test
    public void onActivityResult_doesNotPerformRequestIfTokenizationKeyUsed()
            throws NoSuchAlgorithmException, InvalidArgumentException {
        Configuration configuration = getConfigurationFromFixture();

        mockStatic(TokenizationClient.class);
        BraintreeFragment fragment = new MockFragmentBuilder()
                .context(VenmoInstalledContextFactory.venmoInstalledContext(true, RuntimeEnvironment.application))
                .configuration(configuration)
                .authorization(Authorization.fromString("sandbox_tk_abcd"))
                .sessionId("another-session-id")
                .build();

        Venmo.authorizeAccount(fragment, true);
        Venmo.onActivityResult(fragment, Activity.RESULT_OK, new Intent());

        verifyStatic(times(0));
        TokenizationClient.tokenize(eq(fragment), any(VenmoAccountBuilder.class), any(PaymentMethodNonceCallback.class));
    }

    @Test
    public void onActivityResult_withSuccessfulVaultCall_returnsVenmoAccountNonce() throws InvalidArgumentException {
        Configuration configuration = getConfigurationFromFixture();

        Authorization clientToken = Authorization.fromString(stringFromFixture("base_64_client_token.txt"));
        disableSignatureVerification();
        BraintreeFragment fragment = new MockFragmentBuilder()
                .context(VenmoInstalledContextFactory.venmoInstalledContext(true, RuntimeEnvironment.application))
                .configuration(configuration)
                .authorization(clientToken)
                .sessionId("session-id")
                .successResponse(stringFromFixture("payment_methods/venmo_account_response.json"))
                .build();

        Venmo.authorizeAccount(fragment, true);

        ArgumentCaptor<PaymentMethodNonce> responseCaptor = ArgumentCaptor.forClass(PaymentMethodNonce.class);

        Intent responseIntent = new Intent()
                .putExtra(Venmo.EXTRA_PAYMENT_METHOD_NONCE, "nonce");
        Venmo.onActivityResult(fragment, Activity.RESULT_OK, responseIntent);

        verify(fragment).postCallback(responseCaptor.capture());
        PaymentMethodNonce capturedNonce = responseCaptor.getValue();

        assertTrue(capturedNonce instanceof VenmoAccountNonce);
        VenmoAccountNonce venmoAccountNonce = (VenmoAccountNonce) capturedNonce;

        assertEquals("Venmo", venmoAccountNonce.getTypeLabel());
        assertEquals("fake-venmo-nonce", venmoAccountNonce.getNonce());
        assertEquals("venmojoe", venmoAccountNonce.getUsername());
    }

    @Test
    public void onActivityResult_withSuccessfulVaultCall_sendsAnalyticsEvent() throws InvalidArgumentException {
        Configuration configuration = getConfigurationFromFixture();

        Authorization clientToken = Authorization.fromString(stringFromFixture("base_64_client_token.txt"));
        disableSignatureVerification();
        BraintreeFragment fragment = new MockFragmentBuilder()
                .context(VenmoInstalledContextFactory.venmoInstalledContext(true, RuntimeEnvironment.application))
                .configuration(configuration)
                .authorization(clientToken)
                .sessionId("session-id")
                .successResponse(stringFromFixture("payment_methods/venmo_account_response.json"))
                .build();

        Venmo.authorizeAccount(fragment, true);

        Intent responseIntent = new Intent()
                .putExtra(Venmo.EXTRA_PAYMENT_METHOD_NONCE, "nonce");
        Venmo.onActivityResult(fragment, Activity.RESULT_OK, responseIntent);

        verify(fragment).sendAnalyticsEvent(endsWith("pay-with-venmo.vault.success"));
    }

    @Test
    public void onActivityResult_withFailedVaultCall_postsCallbackToErrorListener() throws InvalidArgumentException {
        Configuration configuration = getConfigurationFromFixture();

        Authorization clientToken = Authorization.fromString(stringFromFixture("base_64_client_token.txt"));

        disableSignatureVerification();
        BraintreeFragment fragment = new MockFragmentBuilder()
                .context(VenmoInstalledContextFactory.venmoInstalledContext(true, RuntimeEnvironment.application))
                .configuration(configuration)
                .authorization(clientToken)
                .sessionId("session-id")
                .errorResponse(new AuthorizationException("Bad fingerprint"))
                .build();

        ArgumentCaptor<Exception> responseCaptor = ArgumentCaptor.forClass(Exception.class);

        Venmo.authorizeAccount(fragment, true);

        Intent responseIntent = new Intent()
                .putExtra(Venmo.EXTRA_PAYMENT_METHOD_NONCE, "nonce");
        Venmo.onActivityResult(fragment, Activity.RESULT_OK, responseIntent);

        verify(fragment).postCallback(responseCaptor.capture());
        Exception exception = responseCaptor.getValue();
        assertTrue(exception instanceof AuthorizationException);
        assertEquals("Bad fingerprint", exception.getMessage());
    }

    @Test
    public void onActivityResult_withFailedVaultCall_sendsAnalyticsEvent() throws InvalidArgumentException {
        Configuration configuration = getConfigurationFromFixture();

        Authorization clientToken = Authorization.fromString(stringFromFixture("base_64_client_token.txt"));

        disableSignatureVerification();
        BraintreeFragment fragment = new MockFragmentBuilder()
                .context(VenmoInstalledContextFactory.venmoInstalledContext(true, RuntimeEnvironment.application))
                .configuration(configuration)
                .authorization(clientToken)
                .sessionId("session-id")
                .errorResponse(new AuthorizationException("Bad fingerprint"))
                .build();

        Venmo.authorizeAccount(fragment, true);

        Intent responseIntent = new Intent()
                .putExtra(Venmo.EXTRA_PAYMENT_METHOD_NONCE, "nonce");
        Venmo.onActivityResult(fragment, Activity.RESULT_OK, responseIntent);

        verify(fragment).sendAnalyticsEvent(endsWith("pay-with-venmo.vault.failed"));
    }

    Configuration getConfigurationFromFixture() {
        try {
            return Configuration.fromJson(stringFromFixture("configuration/with_pay_with_venmo.json"));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
}
