package com.braintreepayments.api;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.robolectric.RobolectricTestRunner;

import static com.braintreepayments.api.FixturesHelper.base64Encode;
import static com.braintreepayments.api.VenmoClient.EXTRA_ACCESS_TOKEN;
import static com.braintreepayments.api.VenmoClient.EXTRA_BRAINTREE_DATA;
import static com.braintreepayments.api.VenmoClient.EXTRA_ENVIRONMENT;
import static com.braintreepayments.api.VenmoClient.EXTRA_MERCHANT_ID;
import static com.braintreepayments.api.VenmoClient.EXTRA_PAYMENT_METHOD_NONCE;
import static com.braintreepayments.api.VenmoClient.EXTRA_USERNAME;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.endsWith;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class VenmoClientUnitTest {

    private FragmentActivity activity;
    private BraintreeClient braintreeClient;

    private Configuration venmoEnabledConfiguration;
    private Configuration venmoDisabledConfiguration;
    private VenmoTokenizeAccountCallback venmoTokenizeAccountCallback;
    private VenmoSharedPrefsWriter sharedPrefsWriter;
    private DeviceInspector deviceInspector;

    private TokenizationClient tokenizationClient;
    private VenmoOnActivityResultCallback onActivityResultCallback;

    @Before
    public void beforeEach() throws JSONException {
        activity = mock(FragmentActivity.class);
        braintreeClient = mock(BraintreeClient.class);
        tokenizationClient = mock(TokenizationClient.class);
        deviceInspector = mock(DeviceInspector.class);

        venmoEnabledConfiguration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_PAY_WITH_VENMO);
        venmoDisabledConfiguration = Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_ACCESS_TOKEN);
        venmoTokenizeAccountCallback = mock(VenmoTokenizeAccountCallback.class);
        sharedPrefsWriter = mock(VenmoSharedPrefsWriter.class);

        onActivityResultCallback = mock(VenmoOnActivityResultCallback.class);
    }

    @Test
    public void showVenmoInGooglePlayStore_opensVenmoAppStoreURL() {
        VenmoClient sut = new VenmoClient(braintreeClient, tokenizationClient, sharedPrefsWriter, deviceInspector);

        sut.showVenmoInGooglePlayStore(activity);

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);

        verify(activity).startActivity(captor.capture());
        assertEquals(captor.getValue().getData().toString(), "https://play.google.com/store/apps/details?id=com.venmo");
    }

    @Test
    public void showVenmoInGooglePlayStore_sendsAnalyticsEvent() {
        VenmoClient sut = new VenmoClient(braintreeClient, tokenizationClient, sharedPrefsWriter, deviceInspector);

        sut.showVenmoInGooglePlayStore(activity);

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);

        verify(activity).startActivity(captor.capture());
        verify(braintreeClient).sendAnalyticsEvent("android.pay-with-venmo.app-store.invoked");
    }

    @Test
    public void tokenizeVenmoAccount_launchesVenmoWithCorrectVenmoExtras() throws JSONException, InvalidArgumentException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(venmoEnabledConfiguration)
                .sessionId("session-id")
                .integration("custom")
                .authorization(Authorization.fromString(base64Encode(Fixtures.CLIENT_TOKEN)))
                .build();

        VenmoRequest request = new VenmoRequest();
        request.setProfileId(null);
        request.setShouldVault(false);

        when(deviceInspector.isVenmoAppSwitchAvailable(activity)).thenReturn(true);

        VenmoClient sut = new VenmoClient(braintreeClient, tokenizationClient, sharedPrefsWriter, deviceInspector);
        sut.tokenizeVenmoAccount(activity, request, venmoTokenizeAccountCallback);

        verify(deviceInspector).isVenmoAppSwitchAvailable(same(activity));

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(activity).startActivityForResult(captor.capture(), eq(BraintreeRequestCodes.VENMO));

        Intent intent = captor.getValue();
        assertEquals(new ComponentName("com.venmo", "com.venmo.controller.SetupMerchantActivity"),
                intent.getComponent());
        assertEquals("merchant-id", intent.getStringExtra(EXTRA_MERCHANT_ID));
        assertEquals("access-token", intent.getStringExtra(EXTRA_ACCESS_TOKEN));
        assertEquals("environment", intent.getStringExtra(EXTRA_ENVIRONMENT));

        JSONObject braintreeData = new JSONObject(intent.getStringExtra(EXTRA_BRAINTREE_DATA));
        JSONObject meta = braintreeData.getJSONObject("_meta");
        assertNotNull(meta);
        assertEquals("session-id", meta.getString("sessionId"));
        assertEquals("custom", meta.getString("integration"));
        assertEquals(BuildConfig.VERSION_NAME, meta.getString("version"));
        assertEquals("android", meta.getString("platform"));
    }

    @Test
    public void tokenizeVenmoAccount_whenConfigurationException_forwardsExceptionToCallback() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configurationError(new Exception("Configuration fetching error"))
                .build();

        VenmoRequest request = new VenmoRequest();
        request.setProfileId(null);
        request.setShouldVault(false);

        VenmoClient sut = new VenmoClient(braintreeClient, tokenizationClient, sharedPrefsWriter, deviceInspector);
        sut.tokenizeVenmoAccount(activity, request, venmoTokenizeAccountCallback);

        ArgumentCaptor<Exception> captor =
                ArgumentCaptor.forClass(Exception.class);
        verify(venmoTokenizeAccountCallback).onResult(captor.capture());
        assertEquals("Configuration fetching error", captor.getValue().getMessage());
        verify(braintreeClient).sendAnalyticsEvent("pay-with-venmo.app-switch.failed");
    }

    @Test
    public void tokenizeVenmoAccount_whenVenmoNotEnabled_postsException() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(venmoDisabledConfiguration)
                .build();

        VenmoRequest request = new VenmoRequest();
        request.setProfileId(null);
        request.setShouldVault(false);

        VenmoClient sut = new VenmoClient(braintreeClient, tokenizationClient, sharedPrefsWriter, deviceInspector);
        sut.tokenizeVenmoAccount(activity, request, venmoTokenizeAccountCallback);

        ArgumentCaptor<AppSwitchNotAvailableException> captor =
                ArgumentCaptor.forClass(AppSwitchNotAvailableException.class);
        verify(venmoTokenizeAccountCallback).onResult(captor.capture());
        assertEquals("Venmo is not enabled", captor.getValue().getMessage());
        verify(braintreeClient).sendAnalyticsEvent("pay-with-venmo.app-switch.failed");
    }

    @Test
    public void tokenizeVenmoAccount_whenVenmoNotInstalled_postsException() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(venmoEnabledConfiguration)
                .build();

        VenmoRequest request = new VenmoRequest();
        request.setProfileId(null);
        request.setShouldVault(false);

        when(deviceInspector.isVenmoAppSwitchAvailable(activity)).thenReturn(false);

        VenmoClient sut = new VenmoClient(braintreeClient, tokenizationClient, sharedPrefsWriter, deviceInspector);
        sut.tokenizeVenmoAccount(activity, request, venmoTokenizeAccountCallback);

        verify(deviceInspector).isVenmoAppSwitchAvailable(same(activity));

        ArgumentCaptor<AppSwitchNotAvailableException> captor =
                ArgumentCaptor.forClass(AppSwitchNotAvailableException.class);
        verify(venmoTokenizeAccountCallback).onResult(captor.capture());
        assertEquals("Venmo is not installed", captor.getValue().getMessage());
        verify(braintreeClient).sendAnalyticsEvent("pay-with-venmo.app-switch.failed");
    }

    @Test
    public void tokenizeVenmoAccount_whenProfileIdIsNull_appSwitchesWithMerchantId() throws InvalidArgumentException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(venmoEnabledConfiguration)
                .sessionId("session-id")
                .integration("custom")
                .authorization(Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
                .build();

        VenmoRequest request = new VenmoRequest();
        request.setProfileId(null);
        request.setShouldVault(false);

        when(deviceInspector.isVenmoAppSwitchAvailable(activity)).thenReturn(true);

        VenmoClient sut = new VenmoClient(braintreeClient, tokenizationClient, sharedPrefsWriter, deviceInspector);
        sut.tokenizeVenmoAccount(activity, request, venmoTokenizeAccountCallback);

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(activity).startActivityForResult(captor.capture(), eq(BraintreeRequestCodes.VENMO));
        assertEquals("com.venmo/com.venmo.controller.SetupMerchantActivity",
                captor.getValue().getComponent().flattenToString());
        Bundle extras = captor.getValue().getExtras();
        assertEquals("merchant-id", extras.getString(EXTRA_MERCHANT_ID));
        assertEquals("access-token", extras.getString(EXTRA_ACCESS_TOKEN));
    }

    @Test
    public void tokenizeVenmoAccount_whenProfileIdIsSpecified_appSwitchesWithProfileIdAndAccessToken() throws InvalidArgumentException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(venmoEnabledConfiguration)
                .sessionId("session-id")
                .integration("custom")
                .authorization(Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
                .build();

        VenmoRequest request = new VenmoRequest();
        request.setProfileId("second-pwv-profile-id");
        request.setShouldVault(false);

        when(deviceInspector.isVenmoAppSwitchAvailable(activity)).thenReturn(true);

        VenmoClient sut = new VenmoClient(braintreeClient, tokenizationClient, sharedPrefsWriter, deviceInspector);
        sut.tokenizeVenmoAccount(activity, request, venmoTokenizeAccountCallback);

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(activity).startActivityForResult(captor.capture(), eq(BraintreeRequestCodes.VENMO));
        assertEquals("com.venmo/com.venmo.controller.SetupMerchantActivity",
                captor.getValue().getComponent().flattenToString());
        Bundle extras = captor.getValue().getExtras();
        assertEquals("second-pwv-profile-id", extras.getString(EXTRA_MERCHANT_ID));
        assertEquals("access-token", extras.getString(EXTRA_ACCESS_TOKEN));
    }

    @Test
    public void getLaunchIntent_doesNotContainAuthFingerprintWhenUsingTokenziationkey()
            throws JSONException, InvalidArgumentException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(venmoEnabledConfiguration)
                .sessionId("session-id")
                .integration("custom")
                .authorization(Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
                .build();

        VenmoRequest request = new VenmoRequest();
        request.setProfileId(null);
        request.setShouldVault(false);

        when(deviceInspector.isVenmoAppSwitchAvailable(activity)).thenReturn(true);

        VenmoClient sut = new VenmoClient(braintreeClient, tokenizationClient, sharedPrefsWriter, deviceInspector);
        sut.tokenizeVenmoAccount(activity, request, venmoTokenizeAccountCallback);

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(activity).startActivityForResult(captor.capture(), eq(BraintreeRequestCodes.VENMO));
        assertEquals("com.venmo/com.venmo.controller.SetupMerchantActivity",
                captor.getValue().getComponent().flattenToString());
        Bundle extras = captor.getValue().getExtras();
        assertEquals("merchant-id", extras.getString(EXTRA_MERCHANT_ID));
        assertEquals("access-token", extras.getString(EXTRA_ACCESS_TOKEN));
        assertEquals("environment", extras.getString(EXTRA_ENVIRONMENT));

        JSONObject braintreeData = new JSONObject(extras.getString(EXTRA_BRAINTREE_DATA));
        assertNull(Json.optString(braintreeData, "authorization_fingerprint", null));
    }

    @Test
    public void tokenizeVenmoAccount_sendsAnalyticsEvent() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(venmoEnabledConfiguration)
                .build();

        VenmoRequest request = new VenmoRequest();
        request.setProfileId(null);
        request.setShouldVault(false);

        VenmoClient sut = new VenmoClient(braintreeClient, tokenizationClient, sharedPrefsWriter, deviceInspector);
        sut.tokenizeVenmoAccount(activity, request, venmoTokenizeAccountCallback);

        verify(braintreeClient).sendAnalyticsEvent("pay-with-venmo.selected");
    }

    @Test
    public void tokenizeVenmoAccount_sendsAnalyticsEventWhenStarted() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(venmoEnabledConfiguration)
                .build();

        VenmoRequest request = new VenmoRequest();
        request.setProfileId(null);
        request.setShouldVault(false);

        when(deviceInspector.isVenmoAppSwitchAvailable(activity)).thenReturn(true);

        VenmoClient sut = new VenmoClient(braintreeClient, tokenizationClient, sharedPrefsWriter, deviceInspector);
        sut.tokenizeVenmoAccount(activity, request, venmoTokenizeAccountCallback);

        verify(braintreeClient).sendAnalyticsEvent("pay-with-venmo.selected");
        verify(braintreeClient).sendAnalyticsEvent("pay-with-venmo.app-switch.started");
    }

    @Test
    public void tokenizeVenmoAccount_persistsIfVaultTrue() throws InvalidArgumentException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(venmoEnabledConfiguration)
                .authorization(Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
                .build();

        VenmoRequest request = new VenmoRequest();
        request.setProfileId(null);
        request.setShouldVault(true);

        when(deviceInspector.isVenmoAppSwitchAvailable(activity)).thenReturn(true);

        VenmoClient sut = new VenmoClient(braintreeClient, tokenizationClient, sharedPrefsWriter, deviceInspector);
        sut.tokenizeVenmoAccount(activity, request, venmoTokenizeAccountCallback);

        verify(sharedPrefsWriter).persistVenmoVaultOption(activity, true);
    }

    @Test
    public void tokenizeVenmoAccount_persistsIfVaultFalse() throws InvalidArgumentException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(venmoEnabledConfiguration)
                .authorization(Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
                .build();

        VenmoRequest request = new VenmoRequest();
        request.setProfileId(null);
        request.setShouldVault(false);

        when(deviceInspector.isVenmoAppSwitchAvailable(activity)).thenReturn(true);

        VenmoClient sut = new VenmoClient(braintreeClient, tokenizationClient, sharedPrefsWriter, deviceInspector);
        sut.tokenizeVenmoAccount(activity, request, venmoTokenizeAccountCallback);

        verify(sharedPrefsWriter).persistVenmoVaultOption(activity, false);
    }

    @Test
    public void tokenizeVenmoAccount_persistsFalseIfTokenizationKeyUsed() throws InvalidArgumentException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(venmoEnabledConfiguration)
                .sessionId("session-id")
                .authorization(Authorization.fromString("sandbox_tk_abcd"))
                .build();

        VenmoRequest request = new VenmoRequest();
        request.setProfileId(null);
        request.setShouldVault(false);

        when(deviceInspector.isVenmoAppSwitchAvailable(activity)).thenReturn(true);

        VenmoClient sut = new VenmoClient(braintreeClient, tokenizationClient, sharedPrefsWriter, deviceInspector);
        sut.tokenizeVenmoAccount(activity, request, venmoTokenizeAccountCallback);

        verify(sharedPrefsWriter).persistVenmoVaultOption(activity, false);
    }

    @Test
    public void tokenizeVenmoAccount_sendsAnalyticsEventWhenUnavailableAndPostException() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(venmoEnabledConfiguration)
                .build();

        VenmoRequest request = new VenmoRequest();
        request.setProfileId(null);
        request.setShouldVault(false);

        when(deviceInspector.isVenmoAppSwitchAvailable(activity)).thenReturn(false);

        VenmoClient sut = new VenmoClient(braintreeClient, tokenizationClient, sharedPrefsWriter, deviceInspector);
        sut.tokenizeVenmoAccount(activity, request, venmoTokenizeAccountCallback);

        ArgumentCaptor<AppSwitchNotAvailableException> captor =
                ArgumentCaptor.forClass(AppSwitchNotAvailableException.class);
        InOrder order = inOrder(braintreeClient);
        order.verify(braintreeClient).sendAnalyticsEvent("pay-with-venmo.selected");
        order.verify(braintreeClient).sendAnalyticsEvent("pay-with-venmo.app-switch.failed");
        verify(venmoTokenizeAccountCallback).onResult(captor.capture());
        assertEquals("Venmo is not installed", captor.getValue().getMessage());
    }

    @Test
    public void onActivityResult_postsPaymentMethodNonceOnSuccess() {
        VenmoClient sut = new VenmoClient(braintreeClient, tokenizationClient, sharedPrefsWriter, deviceInspector);
        Intent intent = new Intent()
                .putExtra(EXTRA_PAYMENT_METHOD_NONCE, "123456-12345-12345-a-adfa")
                .putExtra(EXTRA_USERNAME, "username");

        sut.onActivityResult(activity, AppCompatActivity.RESULT_OK, intent, onActivityResultCallback);

        ArgumentCaptor<VenmoAccountNonce> captor = ArgumentCaptor.forClass(VenmoAccountNonce.class);
        verify(onActivityResultCallback).onResult(captor.capture(), (Exception) isNull());

        VenmoAccountNonce result = captor.getValue();
        assertEquals("123456-12345-12345-a-adfa", result.getNonce());
        assertEquals("username", result.getDescription());
        assertEquals("username", result.getUsername());
    }

    @Test
    public void onActivityResult_sendsAnalyticsEventOnSuccess() {
        VenmoClient sut = new VenmoClient(braintreeClient, tokenizationClient, sharedPrefsWriter, deviceInspector);
        Intent intent = new Intent()
                .putExtra(EXTRA_PAYMENT_METHOD_NONCE, "123456-12345-12345-a-adfa")
                .putExtra(EXTRA_USERNAME, "username");

        sut.onActivityResult(activity, AppCompatActivity.RESULT_OK, intent, onActivityResultCallback);

        verify(braintreeClient).sendAnalyticsEvent("pay-with-venmo.app-switch.success");
    }

    @Test
    public void onActivityResult_sendsAnalyticsEventOnCancel() {
        VenmoClient sut = new VenmoClient(braintreeClient, tokenizationClient, sharedPrefsWriter, deviceInspector);
        sut.onActivityResult(activity, AppCompatActivity.RESULT_CANCELED, new Intent(), onActivityResultCallback);

        verify(braintreeClient).sendAnalyticsEvent("pay-with-venmo.app-switch.canceled");
    }

    @Test
    public void onActivityResult_performsVaultRequestIfRequestPersisted()
            throws InvalidArgumentException, JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(venmoEnabledConfiguration)
                .sessionId("session-id")
                .authorization(Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
                .build();

        VenmoRequest request = new VenmoRequest();
        request.setProfileId(null);
        request.setShouldVault(true);

        when(deviceInspector.isVenmoAppSwitchAvailable(activity)).thenReturn(true);
        when(sharedPrefsWriter.getVenmoVaultOption(activity)).thenReturn(true);

        VenmoClient sut = new VenmoClient(braintreeClient, tokenizationClient, sharedPrefsWriter, deviceInspector);
        sut.tokenizeVenmoAccount(activity, request, venmoTokenizeAccountCallback);

        Intent intent = new Intent()
                .putExtra(EXTRA_PAYMENT_METHOD_NONCE, "sample-nonce");
        sut.onActivityResult(activity, AppCompatActivity.RESULT_OK, intent, onActivityResultCallback);

        ArgumentCaptor<VenmoAccount> accountBuilderCaptor = ArgumentCaptor.forClass(VenmoAccount.class);
        verify(tokenizationClient).tokenize(accountBuilderCaptor.capture(), any(PaymentMethodNonceCallback.class));

        VenmoAccount venmoAccount = accountBuilderCaptor.getValue();
        JSONObject venmoJSON = new JSONObject(venmoAccount.buildJSON());
        assertEquals("sample-nonce", venmoJSON.getJSONObject("venmoAccount").getString("nonce"));
    }

    @Test
    public void onActivityResult_doesNotPerformRequestIfTokenizationKeyUsed() throws InvalidArgumentException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .sessionId("another-session-id")
                .authorization(Authorization.fromString("sandbox_tk_abcd"))
                .build();

        when(deviceInspector.isVenmoAppSwitchAvailable(activity)).thenReturn(true);
        when(sharedPrefsWriter.getVenmoVaultOption(activity)).thenReturn(true);

        VenmoClient sut = new VenmoClient(braintreeClient, tokenizationClient, sharedPrefsWriter, deviceInspector);
        sut.onActivityResult(activity, AppCompatActivity.RESULT_OK, new Intent(), onActivityResultCallback);

        verify(tokenizationClient, never()).tokenize(any(VenmoAccount.class), any(PaymentMethodNonceCallback.class));
    }

    @Test
    public void onActivityResult_withSuccessfulVaultCall_forwardsResultToActivityResultListener() throws InvalidArgumentException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .sessionId("session-id")
                .authorization(Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
                .build();

        when(deviceInspector.isVenmoAppSwitchAvailable(activity)).thenReturn(true);
        when(sharedPrefsWriter.getVenmoVaultOption(activity)).thenReturn(true);

        VenmoClient sut = new VenmoClient(braintreeClient, tokenizationClient, sharedPrefsWriter, deviceInspector);
        sut.onActivityResult(activity, AppCompatActivity.RESULT_OK, new Intent(), onActivityResultCallback);

        ArgumentCaptor<PaymentMethodNonceCallback> callbackCaptor =
                ArgumentCaptor.forClass(PaymentMethodNonceCallback.class);
        verify(tokenizationClient).tokenize(any(VenmoAccount.class), callbackCaptor.capture());

        PaymentMethodNonceCallback tokenizeNonceCallback = callbackCaptor.getValue();
        tokenizeNonceCallback.success(Fixtures.PAYMENT_METHODS_VENMO_ACCOUNT_RESPONSE);

        verify(onActivityResultCallback).onResult(any(VenmoAccountNonce.class), (Exception) isNull());
    }

    @Test
    public void onActivityResult_withSuccessfulVaultCall_sendsAnalyticsEvent() throws InvalidArgumentException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .sessionId("session-id")
                .authorization(Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
                .build();

        when(deviceInspector.isVenmoAppSwitchAvailable(activity)).thenReturn(true);
        when(sharedPrefsWriter.getVenmoVaultOption(activity)).thenReturn(true);

        VenmoClient sut = new VenmoClient(braintreeClient, tokenizationClient, sharedPrefsWriter, deviceInspector);
        sut.onActivityResult(activity, AppCompatActivity.RESULT_OK, new Intent(), onActivityResultCallback);

        ArgumentCaptor<PaymentMethodNonceCallback> callbackCaptor =
                ArgumentCaptor.forClass(PaymentMethodNonceCallback.class);
        verify(tokenizationClient).tokenize(any(VenmoAccount.class), callbackCaptor.capture());

        PaymentMethodNonceCallback tokenizeNonceCallback = callbackCaptor.getValue();
        tokenizeNonceCallback.success(Fixtures.PAYMENT_METHODS_VENMO_ACCOUNT_RESPONSE);

        verify(braintreeClient).sendAnalyticsEvent(endsWith("pay-with-venmo.vault.success"));
    }

    @Test
    public void onActivityResult_withFailedVaultCall_forwardsErrorToActivityResultListener() throws InvalidArgumentException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .sessionId("session-id")
                .authorization(Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
                .build();

        when(deviceInspector.isVenmoAppSwitchAvailable(activity)).thenReturn(true);
        when(sharedPrefsWriter.getVenmoVaultOption(activity)).thenReturn(true);

        VenmoClient sut = new VenmoClient(braintreeClient, tokenizationClient, sharedPrefsWriter, deviceInspector);

        Intent intent = new Intent()
                .putExtra(EXTRA_PAYMENT_METHOD_NONCE, "nonce");
        sut.onActivityResult(activity, AppCompatActivity.RESULT_OK, intent, onActivityResultCallback);

        ArgumentCaptor<PaymentMethodNonceCallback> callbackCaptor =
                ArgumentCaptor.forClass(PaymentMethodNonceCallback.class);
        verify(tokenizationClient).tokenize(any(VenmoAccount.class), callbackCaptor.capture());

        PaymentMethodNonceCallback tokenizeNonceCallback = callbackCaptor.getValue();
        Exception authException = new AuthorizationException("Bad fingerprint");
        tokenizeNonceCallback.failure(authException);

        verify(onActivityResultCallback).onResult(null, authException);
    }

    @Test
    public void onActivityResult_withFailedVaultCall_sendsAnalyticsEvent() throws InvalidArgumentException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .sessionId("session-id")
                .authorization(Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
                .build();

        when(deviceInspector.isVenmoAppSwitchAvailable(activity)).thenReturn(true);
        when(sharedPrefsWriter.getVenmoVaultOption(activity)).thenReturn(true);

        VenmoClient sut = new VenmoClient(braintreeClient, tokenizationClient, sharedPrefsWriter, deviceInspector);

        Intent intent = new Intent()
                .putExtra(EXTRA_PAYMENT_METHOD_NONCE, "nonce");
        sut.onActivityResult(activity, AppCompatActivity.RESULT_OK, intent, onActivityResultCallback);

        ArgumentCaptor<PaymentMethodNonceCallback> callbackCaptor =
                ArgumentCaptor.forClass(PaymentMethodNonceCallback.class);
        verify(tokenizationClient).tokenize(any(VenmoAccount.class), callbackCaptor.capture());

        PaymentMethodNonceCallback tokenizeNonceCallback = callbackCaptor.getValue();
        Exception authException = new AuthorizationException("Bad fingerprint");
        tokenizeNonceCallback.failure(authException);

        verify(braintreeClient).sendAnalyticsEvent(endsWith("pay-with-venmo.vault.failed"));
    }
}