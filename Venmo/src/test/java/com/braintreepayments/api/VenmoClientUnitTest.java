package com.braintreepayments.api;

import static com.braintreepayments.api.VenmoClient.EXTRA_ACCESS_TOKEN;
import static com.braintreepayments.api.VenmoClient.EXTRA_BRAINTREE_DATA;
import static com.braintreepayments.api.VenmoClient.EXTRA_ENVIRONMENT;
import static com.braintreepayments.api.VenmoClient.EXTRA_MERCHANT_ID;
import static com.braintreepayments.api.VenmoClient.EXTRA_PAYMENT_METHOD_NONCE;
import static com.braintreepayments.api.VenmoClient.EXTRA_RESOURCE_ID;
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

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

@RunWith(RobolectricTestRunner.class)
public class VenmoClientUnitTest {

    private FragmentActivity activity;
    private Lifecycle lifecycle;
    private BraintreeClient braintreeClient;
    private VenmoListener listener;

    private Configuration venmoEnabledConfiguration;
    private Configuration venmoDisabledConfiguration;
    private VenmoTokenizeAccountCallback venmoTokenizeAccountCallback;
    private VenmoSharedPrefsWriter sharedPrefsWriter;
    private DeviceInspector deviceInspector;

    private ApiClient apiClient;
    private VenmoAPI venmoAPI;
    private VenmoOnActivityResultCallback onActivityResultCallback;

    private Authorization clientToken;
    private Authorization tokenizationKey;

    @Before
    public void beforeEach() throws JSONException {
        activity = mock(FragmentActivity.class);
        lifecycle = mock(Lifecycle.class);
        braintreeClient = mock(BraintreeClient.class);
        apiClient = mock(ApiClient.class);
        venmoAPI = mock(VenmoAPI.class);
        deviceInspector = mock(DeviceInspector.class);
        listener = mock(VenmoListener.class);

        venmoEnabledConfiguration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_PAY_WITH_VENMO);
        venmoDisabledConfiguration = Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_ACCESS_TOKEN);
        venmoTokenizeAccountCallback = mock(VenmoTokenizeAccountCallback.class);
        sharedPrefsWriter = mock(VenmoSharedPrefsWriter.class);

        onActivityResultCallback = mock(VenmoOnActivityResultCallback.class);
        clientToken = Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN);
        tokenizationKey = Authorization.fromString(Fixtures.TOKENIZATION_KEY);
    }

    @Test
    public void showVenmoInGooglePlayStore_opensVenmoAppStoreURL() {
        VenmoClient sut = new VenmoClient(braintreeClient, venmoAPI, sharedPrefsWriter, deviceInspector, activity, lifecycle);

        sut.showVenmoInGooglePlayStore(activity);

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);

        verify(activity).startActivity(captor.capture());
        assertEquals(captor.getValue().getData().toString(), "https://play.google.com/store/apps/details?id=com.venmo");
    }

    @Test
    public void showVenmoInGooglePlayStore_sendsAnalyticsEvent() {
        VenmoClient sut = new VenmoClient(braintreeClient, venmoAPI, sharedPrefsWriter, deviceInspector, activity, lifecycle);

        sut.showVenmoInGooglePlayStore(activity);

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);

        verify(activity).startActivity(captor.capture());
        verify(braintreeClient).sendAnalyticsEvent("android.pay-with-venmo.app-store.invoked");
    }

    @Test
    public void tokenizeVenmoAccount_whenCreatePaymentContextSucceeds_startsVenmoActivityAndSendsAnalytics() throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(venmoEnabledConfiguration)
                .sessionId("session-id")
                .integration("custom")
                .authorizationSuccess(clientToken)
                .build();

        VenmoAPI venmoAPI = new MockVenmoApiBuilder()
                .createPaymentContextSuccess("venmo-payment-context-id")
                .build();

        when(deviceInspector.isVenmoAppSwitchAvailable(activity)).thenReturn(true);

        VenmoRequest request = new VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE);
        request.setProfileId("sample-venmo-merchant");
        request.setShouldVault(false);

        VenmoClient sut = new VenmoClient(braintreeClient, venmoAPI, sharedPrefsWriter, deviceInspector, activity, lifecycle);
        sut.tokenizeVenmoAccount(activity, request);

        InOrder inOrder = Mockito.inOrder(activity, braintreeClient);

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        inOrder.verify(activity).startActivityForResult(captor.capture(), eq(BraintreeRequestCodes.VENMO));

        inOrder.verify(braintreeClient).sendAnalyticsEvent("pay-with-venmo.app-switch.started");

        Intent intent = captor.getValue();
        assertEquals(new ComponentName("com.venmo", "com.venmo.controller.SetupMerchantActivity"), intent.getComponent());
        assertEquals("sample-venmo-merchant", intent.getStringExtra(EXTRA_MERCHANT_ID));
        assertEquals("access-token", intent.getStringExtra(EXTRA_ACCESS_TOKEN));
        assertEquals("environment", intent.getStringExtra(EXTRA_ENVIRONMENT));
        assertEquals("venmo-payment-context-id", intent.getStringExtra(EXTRA_RESOURCE_ID));

        JSONObject expectedBraintreeData = new JSONObject()
                .put("_meta", new JSONObject()
                        .put("platform", "android")
                        .put("sessionId", "session-id")
                        .put("integration", "custom")
                        .put("version", BuildConfig.VERSION_NAME)
                );
        JSONAssert.assertEquals(expectedBraintreeData, new JSONObject(intent.getStringExtra(EXTRA_BRAINTREE_DATA)), JSONCompareMode.STRICT);
    }

    @Test
    public void tokenizeVenmoAccount_launchesVenmoWithCorrectVenmoExtras() throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(venmoEnabledConfiguration)
                .sessionId("session-id")
                .integration("custom")
                .authorizationSuccess(clientToken)
                .build();

        VenmoAPI venmoAPI = new MockVenmoApiBuilder()
                .createPaymentContextSuccess("venmo-payment-context-id")
                .build();

        VenmoRequest request = new VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE);
        request.setProfileId(null);
        request.setShouldVault(false);

        when(deviceInspector.isVenmoAppSwitchAvailable(activity)).thenReturn(true);

        VenmoClient sut = new VenmoClient(braintreeClient, venmoAPI, sharedPrefsWriter, deviceInspector, activity, lifecycle);
        sut.tokenizeVenmoAccount(activity, request);

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

        VenmoRequest request = new VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE);
        request.setProfileId(null);
        request.setShouldVault(false);

        VenmoClient sut = new VenmoClient(braintreeClient, venmoAPI, sharedPrefsWriter, deviceInspector, activity, lifecycle);
        sut.setListener(listener);
        sut.tokenizeVenmoAccount(activity, request);

        ArgumentCaptor<Exception> captor =
                ArgumentCaptor.forClass(Exception.class);
        verify(listener).onVenmoFailure(captor.capture());
        assertEquals("Configuration fetching error", captor.getValue().getMessage());
        verify(braintreeClient).sendAnalyticsEvent("pay-with-venmo.app-switch.failed");
    }

    @Test
    public void tokenizeVenmoAccount_whenVenmoNotEnabled_postsException() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(venmoDisabledConfiguration)
                .build();

        VenmoRequest request = new VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE);
        request.setProfileId(null);
        request.setShouldVault(false);

        VenmoClient sut = new VenmoClient(braintreeClient, venmoAPI, sharedPrefsWriter, deviceInspector, activity, lifecycle);
        sut.setListener(listener);
        sut.tokenizeVenmoAccount(activity, request);

        ArgumentCaptor<AppSwitchNotAvailableException> captor =
                ArgumentCaptor.forClass(AppSwitchNotAvailableException.class);
        verify(listener).onVenmoFailure(captor.capture());
        assertEquals("Venmo is not enabled", captor.getValue().getMessage());
        verify(braintreeClient).sendAnalyticsEvent("pay-with-venmo.app-switch.failed");
    }

    @Test
    public void tokenizeVenmoAccount_whenVenmoNotInstalled_postsException() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(venmoEnabledConfiguration)
                .build();

        VenmoRequest request = new VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE);
        request.setProfileId(null);
        request.setShouldVault(false);

        when(deviceInspector.isVenmoAppSwitchAvailable(activity)).thenReturn(false);

        VenmoClient sut = new VenmoClient(braintreeClient, venmoAPI, sharedPrefsWriter, deviceInspector, activity, lifecycle);
        sut.tokenizeVenmoAccount(activity, request, venmoTokenizeAccountCallback);

        verify(deviceInspector).isVenmoAppSwitchAvailable(same(activity));

        ArgumentCaptor<AppSwitchNotAvailableException> captor =
                ArgumentCaptor.forClass(AppSwitchNotAvailableException.class);
        verify(venmoTokenizeAccountCallback).onResult(captor.capture());
        assertEquals("Venmo is not installed", captor.getValue().getMessage());
        verify(braintreeClient).sendAnalyticsEvent("pay-with-venmo.app-switch.failed");
    }

    @Test
    public void tokenizeVenmoAccount_whenProfileIdIsNull_appSwitchesWithMerchantId() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(venmoEnabledConfiguration)
                .sessionId("session-id")
                .integration("custom")
                .authorizationSuccess(clientToken)
                .build();

        VenmoAPI venmoAPI = new MockVenmoApiBuilder()
                .createPaymentContextSuccess("venmo-payment-context-id")
                .build();

        VenmoRequest request = new VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE);
        request.setProfileId(null);
        request.setShouldVault(false);

        when(deviceInspector.isVenmoAppSwitchAvailable(activity)).thenReturn(true);

        VenmoClient sut = new VenmoClient(braintreeClient, venmoAPI, sharedPrefsWriter, deviceInspector, activity, lifecycle);
        sut.tokenizeVenmoAccount(activity, request);

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(activity).startActivityForResult(captor.capture(), eq(BraintreeRequestCodes.VENMO));
        assertEquals("com.venmo/com.venmo.controller.SetupMerchantActivity",
                captor.getValue().getComponent().flattenToString());
        Bundle extras = captor.getValue().getExtras();
        assertEquals("merchant-id", extras.getString(EXTRA_MERCHANT_ID));
        assertEquals("access-token", extras.getString(EXTRA_ACCESS_TOKEN));
    }

    @Test
    public void tokenizeVenmoAccount_whenProfileIdIsSpecified_appSwitchesWithProfileIdAndAccessToken() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(venmoEnabledConfiguration)
                .sessionId("session-id")
                .integration("custom")
                .authorizationSuccess(clientToken)
                .build();

        VenmoAPI venmoAPI = new MockVenmoApiBuilder()
                .createPaymentContextSuccess("venmo-payment-context-id")
                .build();

        VenmoRequest request = new VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE);
        request.setProfileId("second-pwv-profile-id");
        request.setShouldVault(false);

        when(deviceInspector.isVenmoAppSwitchAvailable(activity)).thenReturn(true);

        VenmoClient sut = new VenmoClient(braintreeClient, venmoAPI, sharedPrefsWriter, deviceInspector, activity, lifecycle);
        sut.tokenizeVenmoAccount(activity, request);

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(activity).startActivityForResult(captor.capture(), eq(BraintreeRequestCodes.VENMO));
        assertEquals("com.venmo/com.venmo.controller.SetupMerchantActivity",
                captor.getValue().getComponent().flattenToString());
        Bundle extras = captor.getValue().getExtras();
        assertEquals("second-pwv-profile-id", extras.getString(EXTRA_MERCHANT_ID));
        assertEquals("access-token", extras.getString(EXTRA_ACCESS_TOKEN));
    }

    @Test
    public void getLaunchIntent_doesNotContainAuthFingerprintWhenUsingTokenziationkey() throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(venmoEnabledConfiguration)
                .sessionId("session-id")
                .integration("custom")
                .authorizationSuccess(clientToken)
                .build();

        VenmoAPI venmoAPI = new MockVenmoApiBuilder()
                .createPaymentContextSuccess("venmo-payment-context-id")
                .build();

        VenmoRequest request = new VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE);
        request.setProfileId(null);
        request.setShouldVault(false);

        when(deviceInspector.isVenmoAppSwitchAvailable(activity)).thenReturn(true);

        VenmoClient sut = new VenmoClient(braintreeClient, venmoAPI, sharedPrefsWriter, deviceInspector, activity, lifecycle);
        sut.tokenizeVenmoAccount(activity, request);

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

        VenmoRequest request = new VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE);
        request.setProfileId(null);
        request.setShouldVault(false);

        VenmoClient sut = new VenmoClient(braintreeClient, venmoAPI, sharedPrefsWriter, deviceInspector, activity, lifecycle);
        sut.setListener(listener);
        sut.tokenizeVenmoAccount(activity, request);

        verify(braintreeClient).sendAnalyticsEvent("pay-with-venmo.selected");
    }

    @Test
    public void tokenizeVenmoAccount_sendsAnalyticsEventWhenStarted() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(venmoEnabledConfiguration)
                .authorizationSuccess(clientToken)
                .build();

        VenmoAPI venmoAPI = new MockVenmoApiBuilder()
                .createPaymentContextSuccess("venmo-payment-context-id")
                .build();

        VenmoRequest request = new VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE);
        request.setProfileId(null);
        request.setShouldVault(false);

        when(deviceInspector.isVenmoAppSwitchAvailable(activity)).thenReturn(true);

        VenmoClient sut = new VenmoClient(braintreeClient, venmoAPI, sharedPrefsWriter, deviceInspector, activity, lifecycle);
        sut.tokenizeVenmoAccount(activity, request);

        verify(braintreeClient).sendAnalyticsEvent("pay-with-venmo.selected");
        verify(braintreeClient).sendAnalyticsEvent("pay-with-venmo.app-switch.started");
    }

    @Test
    public void tokenizeVenmoAccount_whenShouldVaultIsTrue_persistsVenmoVaultTrue() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(venmoEnabledConfiguration)
                .authorizationSuccess(clientToken)
                .build();

        VenmoAPI venmoAPI = new MockVenmoApiBuilder()
                .createPaymentContextSuccess("venmo-payment-context-id")
                .build();

        VenmoRequest request = new VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE);
        request.setProfileId(null);
        request.setShouldVault(true);

        when(deviceInspector.isVenmoAppSwitchAvailable(activity)).thenReturn(true);

        VenmoClient sut = new VenmoClient(braintreeClient, venmoAPI, sharedPrefsWriter, deviceInspector, activity, lifecycle);
        sut.tokenizeVenmoAccount(activity, request);

        verify(sharedPrefsWriter).persistVenmoVaultOption(activity, true);
    }

    @Test
    public void tokenizeVenmoAccount_whenShouldVaultIsFalse_persistsVenmoVaultFalse() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(venmoEnabledConfiguration)
                .authorizationSuccess(clientToken)
                .build();

        VenmoAPI venmoAPI = new MockVenmoApiBuilder()
                .createPaymentContextSuccess("venmo-payment-context-id")
                .build();

        VenmoRequest request = new VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE);
        request.setProfileId(null);
        request.setShouldVault(false);

        when(deviceInspector.isVenmoAppSwitchAvailable(activity)).thenReturn(true);

        VenmoClient sut = new VenmoClient(braintreeClient, venmoAPI, sharedPrefsWriter, deviceInspector, activity, lifecycle);
        sut.tokenizeVenmoAccount(activity, request);

        verify(sharedPrefsWriter).persistVenmoVaultOption(activity, false);
    }

    @Test
    public void tokenizeVenmoAccount_withTokenizationKey_persistsVenmoVaultFalse() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(venmoEnabledConfiguration)
                .sessionId("session-id")
                .authorizationSuccess(clientToken)
                .build();

        VenmoAPI venmoAPI = new MockVenmoApiBuilder()
                .createPaymentContextSuccess("venmo-payment-context-id")
                .build();

        VenmoRequest request = new VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE);
        request.setProfileId(null);
        request.setShouldVault(false);

        when(deviceInspector.isVenmoAppSwitchAvailable(activity)).thenReturn(true);

        VenmoClient sut = new VenmoClient(braintreeClient, venmoAPI, sharedPrefsWriter, deviceInspector, activity, lifecycle);
        sut.tokenizeVenmoAccount(activity, request);

        verify(sharedPrefsWriter).persistVenmoVaultOption(activity, false);
    }

    @Test
    public void tokenizeVenmoAccount_sendsAnalyticsEventWhenUnavailableAndPostException() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(venmoEnabledConfiguration)
                .build();

        VenmoAPI venmoAPI = new MockVenmoApiBuilder()
                .createPaymentContextError(new Exception("Error"))
                .build();

        VenmoRequest request = new VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE);
        request.setProfileId(null);
        request.setShouldVault(false);

        when(deviceInspector.isVenmoAppSwitchAvailable(activity)).thenReturn(false);

        VenmoClient sut = new VenmoClient(braintreeClient, venmoAPI, sharedPrefsWriter, deviceInspector, activity, lifecycle);
        sut.setListener(listener);
        sut.tokenizeVenmoAccount(activity, request);

        ArgumentCaptor<AppSwitchNotAvailableException> captor =
                ArgumentCaptor.forClass(AppSwitchNotAvailableException.class);
        InOrder order = inOrder(braintreeClient);

        verify(listener).onVenmoFailure(captor.capture());
        assertEquals("Venmo is not installed", captor.getValue().getMessage());

        order.verify(braintreeClient).sendAnalyticsEvent("pay-with-venmo.selected");
        order.verify(braintreeClient).sendAnalyticsEvent("pay-with-venmo.app-switch.failed");
    }

    @Test
    public void tokenizeVenmoAccount_whenVenmoApiError_forwardsErrorToCallback_andSendsAnalytics() {
        BraintreeException graphQLError = new BraintreeException("GraphQL error");
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(venmoEnabledConfiguration)
                .sendGraphQLPOSTErrorResponse(graphQLError)
                .build();

        VenmoAPI venmoAPI = new MockVenmoApiBuilder()
                .createPaymentContextError(graphQLError)
                .build();

        VenmoRequest request = new VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE);
        when(deviceInspector.isVenmoAppSwitchAvailable(activity)).thenReturn(true);

        VenmoClient sut = new VenmoClient(braintreeClient, venmoAPI, sharedPrefsWriter, deviceInspector, activity, lifecycle);
        sut.setListener(listener);
        sut.tokenizeVenmoAccount(activity, request);

        verify(listener).onVenmoFailure(graphQLError);
        verify(braintreeClient).sendAnalyticsEvent("pay-with-venmo.app-switch.failed");
    }

    @Test
    public void onActivityResult_withPaymentContextId_requestFromVenmoApi() throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(venmoEnabledConfiguration)
                .authorizationSuccess(clientToken)
                .build();

        VenmoClient sut = new VenmoClient(braintreeClient, venmoAPI, sharedPrefsWriter, deviceInspector, activity, lifecycle);

        Intent intent = new Intent();
        intent.putExtra("com.braintreepayments.api.EXTRA_RESOURCE_ID", "payment-context-id");
        sut.onActivityResult(activity, AppCompatActivity.RESULT_OK, intent, onActivityResultCallback);

        verify(venmoAPI).createNonceFromPaymentContext(eq("payment-context-id"),  any(VenmoOnActivityResultCallback.class));
    }

    @Test
    public void onActivityResult_onGraphQLPostSuccess_returnsNonceToCallback_andSendsAnalytics() throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(venmoEnabledConfiguration)
                .authorizationSuccess(clientToken)
                .build();

        VenmoAPI venmoAPI = new MockVenmoApiBuilder()
                .createNonceFromPaymentContextSuccess(VenmoAccountNonce.fromJSON(new JSONObject(Fixtures.PAYMENT_METHODS_VENMO_ACCOUNT_RESPONSE)))
                .build();

        VenmoClient sut = new VenmoClient(braintreeClient, venmoAPI, sharedPrefsWriter, deviceInspector, activity, lifecycle);

        Intent intent = new Intent();
        intent.putExtra("com.braintreepayments.api.EXTRA_RESOURCE_ID", "payment-context-id");
        sut.onActivityResult(activity, AppCompatActivity.RESULT_OK, intent, onActivityResultCallback);

        ArgumentCaptor<VenmoAccountNonce> captor = ArgumentCaptor.forClass(VenmoAccountNonce.class);
        verify(onActivityResultCallback).onResult(captor.capture(), (Exception) isNull());

        VenmoAccountNonce nonce = captor.getValue();
        assertEquals("fake-venmo-nonce", nonce.getString());
        assertEquals("venmojoe", nonce.getUsername());

        verify(braintreeClient).sendAnalyticsEvent("pay-with-venmo.app-switch.success");
    }

    @Test
    public void onActivityResult_onGraphQLPostFailure_forwardsExceptionToCallback_andSendsAnalytics() {
        BraintreeException graphQLError = new BraintreeException("graphQL error");
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(venmoEnabledConfiguration)
                .authorizationSuccess(clientToken)
                .sendGraphQLPOSTErrorResponse(graphQLError)
                .build();

        VenmoAPI venmoAPI = new MockVenmoApiBuilder()
                .createNonceFromPaymentContextError(graphQLError)
                .build();

        VenmoClient sut = new VenmoClient(braintreeClient, venmoAPI, sharedPrefsWriter, deviceInspector, activity, lifecycle);

        Intent intent = new Intent();
        intent.putExtra("com.braintreepayments.api.EXTRA_RESOURCE_ID", "payment-context-id");
        sut.onActivityResult(activity, AppCompatActivity.RESULT_OK, intent, onActivityResultCallback);

        verify(onActivityResultCallback).onResult(null, graphQLError);
        verify(braintreeClient).sendAnalyticsEvent("pay-with-venmo.app-switch.failure");
    }

    @Test
    public void onActivityResult_withPaymentContext_performsVaultRequestIfRequestPersisted() throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(venmoEnabledConfiguration)
                .sessionId("session-id")
                .authorizationSuccess(clientToken)
                .build();

        VenmoAccountNonce nonce = mock(VenmoAccountNonce.class);
        when(nonce.getString()).thenReturn("some-nonce");

        VenmoAPI venmoAPI = new MockVenmoApiBuilder()
                .createNonceFromPaymentContextSuccess(nonce)
                .build();

        VenmoRequest request = new VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE);
        request.setProfileId(null);
        request.setShouldVault(true);

        when(deviceInspector.isVenmoAppSwitchAvailable(activity)).thenReturn(true);
        when(sharedPrefsWriter.getVenmoVaultOption(activity)).thenReturn(true);

        VenmoClient sut = new VenmoClient(braintreeClient, venmoAPI, sharedPrefsWriter, deviceInspector, activity, lifecycle);

        Intent intent = new Intent();
        intent.putExtra("com.braintreepayments.api.EXTRA_RESOURCE_ID", "payment-context-id");
        sut.onActivityResult(activity, AppCompatActivity.RESULT_OK, intent, onActivityResultCallback);

        verify(venmoAPI).vaultVenmoAccountNonce(eq("some-nonce") , any(VenmoOnActivityResultCallback.class));
    }

    @Test
    public void onActivityResult_postsPaymentMethodNonceOnSuccess() throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .authorizationSuccess(clientToken)
                .build();

        VenmoClient sut = new VenmoClient(braintreeClient, venmoAPI, sharedPrefsWriter, deviceInspector, activity, lifecycle);
        Intent intent = new Intent()
                .putExtra(EXTRA_RESOURCE_ID, "123456-12345-12345-a-adfa")
                .putExtra(EXTRA_USERNAME, "username");

        sut.onActivityResult(activity, AppCompatActivity.RESULT_OK, intent, onActivityResultCallback);

        verify(venmoAPI).createNonceFromPaymentContext(eq("123456-12345-12345-a-adfa"), any(VenmoOnActivityResultCallback.class));
    }

    @Test
    public void onActivityResult_sendsAnalyticsEventOnSuccess() {
        VenmoClient sut = new VenmoClient(braintreeClient, venmoAPI, sharedPrefsWriter, deviceInspector, activity, lifecycle);
        Intent intent = new Intent()
                .putExtra(EXTRA_RESOURCE_ID, "123456-12345-12345-a-adfa")
                .putExtra(EXTRA_USERNAME, "username");

        sut.onActivityResult(activity, AppCompatActivity.RESULT_OK, intent, onActivityResultCallback);

        verify(braintreeClient).sendAnalyticsEvent("pay-with-venmo.app-switch.success");
    }

    @Test
    public void onActivityResult_sendsAnalyticsEventOnCancel() {
        VenmoClient sut = new VenmoClient(braintreeClient, venmoAPI, sharedPrefsWriter, deviceInspector, activity, lifecycle);
        sut.onActivityResult(activity, AppCompatActivity.RESULT_CANCELED, new Intent(), onActivityResultCallback);

        verify(braintreeClient).sendAnalyticsEvent("pay-with-venmo.app-switch.canceled");
    }

    @Test
    public void onActivityResult_forwardsExceptionToCallbackOnCancel() {
        VenmoClient sut = new VenmoClient(braintreeClient, venmoAPI, sharedPrefsWriter, deviceInspector, activity, lifecycle);
        sut.onActivityResult(activity, AppCompatActivity.RESULT_CANCELED, new Intent(), onActivityResultCallback);

        ArgumentCaptor<BraintreeException> captor = ArgumentCaptor.forClass(BraintreeException.class);
        verify(onActivityResultCallback).onResult((VenmoAccountNonce) isNull(), captor.capture());

        BraintreeException exception = captor.getValue();
        assertEquals("User canceled Venmo.", exception.getMessage());
    }

    @Test
    public void onActivityResult_performsVaultRequestIfRequestPersisted() throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(venmoEnabledConfiguration)
                .sessionId("session-id")
                .authorizationSuccess(clientToken)
                .build();

        VenmoAPI venmoAPI = new MockVenmoApiBuilder()
                .createPaymentContextSuccess("payment_id")
                .build();

        VenmoRequest request = new VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE);
        request.setProfileId(null);
        request.setShouldVault(true);

        when(deviceInspector.isVenmoAppSwitchAvailable(activity)).thenReturn(true);
        when(sharedPrefsWriter.getVenmoVaultOption(activity)).thenReturn(true);

        VenmoClient sut = new VenmoClient(braintreeClient, venmoAPI, sharedPrefsWriter, deviceInspector, activity, lifecycle);
        sut.tokenizeVenmoAccount(activity, request);

        Intent intent = new Intent()
                .putExtra(EXTRA_PAYMENT_METHOD_NONCE, "sample-nonce");
        sut.onActivityResult(activity, AppCompatActivity.RESULT_OK, intent, onActivityResultCallback);

        verify(venmoAPI).vaultVenmoAccountNonce(eq("sample-nonce"), any(VenmoOnActivityResultCallback.class));
    }

    @Test
    public void onActivityResult_doesNotPerformRequestIfTokenizationKeyUsed() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .sessionId("another-session-id")
                .authorizationSuccess(tokenizationKey)
                .build();

        when(deviceInspector.isVenmoAppSwitchAvailable(activity)).thenReturn(true);
        when(sharedPrefsWriter.getVenmoVaultOption(activity)).thenReturn(true);

        VenmoClient sut = new VenmoClient(braintreeClient, venmoAPI, sharedPrefsWriter, deviceInspector, activity, lifecycle);
        sut.onActivityResult(activity, AppCompatActivity.RESULT_OK, new Intent(), onActivityResultCallback);

        verify(apiClient, never()).tokenizeREST(any(VenmoAccount.class), any(TokenizeCallback.class));
    }

    @Test
    public void onActivityResult_withSuccessfulVaultCall_forwardsResultToActivityResultListener() throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .sessionId("session-id")
                .authorizationSuccess(clientToken)
                .build();

        VenmoAccountNonce venmoAccountNonce = mock(VenmoAccountNonce.class);

        VenmoAPI venmoAPI = new MockVenmoApiBuilder()
                .vaultVenmoAccountNonceSuccess(venmoAccountNonce)
                .build();

        when(deviceInspector.isVenmoAppSwitchAvailable(activity)).thenReturn(true);
        when(sharedPrefsWriter.getVenmoVaultOption(activity)).thenReturn(true);

        Intent intent = new Intent()
                .putExtra(EXTRA_PAYMENT_METHOD_NONCE, "nonce");
        VenmoClient sut = new VenmoClient(braintreeClient, venmoAPI, sharedPrefsWriter, deviceInspector, activity, lifecycle);
        sut.onActivityResult(activity, AppCompatActivity.RESULT_OK, intent, onActivityResultCallback);

        verify(onActivityResultCallback).onResult(same(venmoAccountNonce), (Exception) isNull());
    }

    @Test
    public void onActivityResult_withPaymentContext_withSuccessfulVaultCall_forwardsNonceToCallback_andSendsAnalytics() throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .sessionId("session-id")
                .authorizationSuccess(clientToken)
                .sendGraphQLPOSTSuccessfulResponse(Fixtures.VENMO_GRAPHQL_GET_PAYMENT_CONTEXT_RESPONSE)
                .build();

        VenmoAccountNonce venmoAccountNonce = VenmoAccountNonce.fromJSON(new JSONObject(Fixtures.PAYMENT_METHODS_VENMO_ACCOUNT_RESPONSE));

        VenmoAPI venmoAPI = new MockVenmoApiBuilder()
                .createNonceFromPaymentContextSuccess(venmoAccountNonce)
                .vaultVenmoAccountNonceSuccess(venmoAccountNonce)
                .build();

        when(deviceInspector.isVenmoAppSwitchAvailable(activity)).thenReturn(true);
        when(sharedPrefsWriter.getVenmoVaultOption(activity)).thenReturn(true);

        VenmoClient sut = new VenmoClient(braintreeClient, venmoAPI, sharedPrefsWriter, deviceInspector, activity, lifecycle);

        Intent intent = new Intent();
        intent.putExtra("com.braintreepayments.api.EXTRA_RESOURCE_ID", "payment-context-id");
        sut.onActivityResult(activity, AppCompatActivity.RESULT_OK, intent, onActivityResultCallback);

        verify(onActivityResultCallback).onResult(any(VenmoAccountNonce.class), (Exception) isNull());
        verify(braintreeClient).sendAnalyticsEvent(endsWith("pay-with-venmo.vault.success"));
    }

    @Test
    public void onActivityResult_withSuccessfulVaultCall_sendsAnalyticsEvent() throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .sessionId("session-id")
                .authorizationSuccess(clientToken)
                .build();

        VenmoAccountNonce venmoAccountNonce = VenmoAccountNonce.fromJSON(new JSONObject(Fixtures.PAYMENT_METHODS_VENMO_ACCOUNT_RESPONSE));

        VenmoAPI venmoAPI = new MockVenmoApiBuilder()
                .createNonceFromPaymentContextSuccess(venmoAccountNonce)
                .vaultVenmoAccountNonceSuccess(venmoAccountNonce)
                .build();

        when(deviceInspector.isVenmoAppSwitchAvailable(activity)).thenReturn(true);
        when(sharedPrefsWriter.getVenmoVaultOption(activity)).thenReturn(true);

        Intent intent = new Intent()
                .putExtra(EXTRA_PAYMENT_METHOD_NONCE, "nonce");
        VenmoClient sut = new VenmoClient(braintreeClient, venmoAPI, sharedPrefsWriter, deviceInspector, activity, lifecycle);
        sut.onActivityResult(activity, AppCompatActivity.RESULT_OK, intent, onActivityResultCallback);

        verify(braintreeClient).sendAnalyticsEvent(endsWith("pay-with-venmo.vault.success"));
    }

    @Test
    public void onActivityResult_withFailedVaultCall_forwardsErrorToActivityResultListener() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .sessionId("session-id")
                .authorizationSuccess(clientToken)
                .build();

        Exception error = new Exception("error");

        VenmoAPI venmoAPI = new MockVenmoApiBuilder()
                .createPaymentContextSuccess("payment_id")
                .vaultVenmoAccountNonceError(error)
                .build();

        when(deviceInspector.isVenmoAppSwitchAvailable(activity)).thenReturn(true);
        when(sharedPrefsWriter.getVenmoVaultOption(activity)).thenReturn(true);

        VenmoClient sut = new VenmoClient(braintreeClient, venmoAPI, sharedPrefsWriter, deviceInspector, activity, lifecycle);

        Intent intent = new Intent()
                .putExtra(EXTRA_PAYMENT_METHOD_NONCE, "nonce");
        sut.onActivityResult(activity, AppCompatActivity.RESULT_OK, intent, onActivityResultCallback);

        verify(onActivityResultCallback).onResult(null, error);
    }

    @Test
    public void onActivityResult_withPaymentContext_withFailedVaultCall_forwardsErrorToCallback_andSendsAnalytics() throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .sessionId("session-id")
                .authorizationSuccess(clientToken)
                .sendGraphQLPOSTSuccessfulResponse(Fixtures.VENMO_GRAPHQL_GET_PAYMENT_CONTEXT_RESPONSE)
                .build();

        VenmoAccountNonce venmoAccountNonce = VenmoAccountNonce.fromJSON(new JSONObject(Fixtures.PAYMENT_METHODS_VENMO_ACCOUNT_RESPONSE));
        Exception error = new Exception("error");

        VenmoAPI venmoAPI = new MockVenmoApiBuilder()
                .createNonceFromPaymentContextSuccess(venmoAccountNonce)
                .vaultVenmoAccountNonceError(error)
                .build();

        when(deviceInspector.isVenmoAppSwitchAvailable(activity)).thenReturn(true);
        when(sharedPrefsWriter.getVenmoVaultOption(activity)).thenReturn(true);

        VenmoClient sut = new VenmoClient(braintreeClient, venmoAPI, sharedPrefsWriter, deviceInspector, activity, lifecycle);

        Intent intent = new Intent();
        intent.putExtra("com.braintreepayments.api.EXTRA_RESOURCE_ID", "payment-context-id");
        sut.onActivityResult(activity, AppCompatActivity.RESULT_OK, intent, onActivityResultCallback);

        verify(onActivityResultCallback).onResult(null, error);
        verify(braintreeClient).sendAnalyticsEvent(endsWith("pay-with-venmo.vault.failed"));
    }

    @Test
    public void onActivityResult_withFailedVaultCall_sendsAnalyticsEvent() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .sessionId("session-id")
                .authorizationSuccess(clientToken)
                .build();

        Exception error = new Exception("error");

        VenmoAPI venmoAPI = new MockVenmoApiBuilder()
                .createPaymentContextSuccess("payment_id")
                .vaultVenmoAccountNonceError(error)
                .build();

        when(deviceInspector.isVenmoAppSwitchAvailable(activity)).thenReturn(true);
        when(sharedPrefsWriter.getVenmoVaultOption(activity)).thenReturn(true);

        VenmoClient sut = new VenmoClient(braintreeClient, venmoAPI, sharedPrefsWriter, deviceInspector, activity, lifecycle);

        Intent intent = new Intent()
                .putExtra(EXTRA_PAYMENT_METHOD_NONCE, "nonce");
        sut.onActivityResult(activity, AppCompatActivity.RESULT_OK, intent, onActivityResultCallback);

        verify(braintreeClient).sendAnalyticsEvent(endsWith("pay-with-venmo.vault.failed"));
    }

    @Test
    public void isReadyToPay_whenConfigurationFails_callbackFalseAndPropagatesError() {
        Exception configError = new Exception("configuration error");
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configurationError(configError)
                .build();

        VenmoClient sut = new VenmoClient(braintreeClient, venmoAPI, sharedPrefsWriter, deviceInspector, activity, lifecycle);

        VenmoIsReadyToPayCallback callback = mock(VenmoIsReadyToPayCallback.class);
        sut.isReadyToPay(activity, callback);

        verify(callback).onResult(false, configError);
    }

    @Test
    public void isReadyToPay_whenVenmoDisabled_callbackFalse() throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ANALYTICS))
                .build();

        VenmoClient sut = new VenmoClient(braintreeClient, venmoAPI, sharedPrefsWriter, deviceInspector, activity, lifecycle);

        VenmoIsReadyToPayCallback callback = mock(VenmoIsReadyToPayCallback.class);
        sut.isReadyToPay(activity, callback);

        verify(callback).onResult(false, null);
    }

    @Test
    public void isReadyToPay_whenVenmoEnabledAndAppSwitchUnavailable_callbackFalse() throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(Configuration.fromJson(Fixtures.CONFIGURATION_WITH_PAY_WITH_VENMO))
                .build();

        VenmoClient sut = new VenmoClient(braintreeClient, venmoAPI, sharedPrefsWriter, deviceInspector, activity, lifecycle);
        when(deviceInspector.isVenmoAppSwitchAvailable(activity)).thenReturn(false);

        VenmoIsReadyToPayCallback callback = mock(VenmoIsReadyToPayCallback.class);
        sut.isReadyToPay(activity, callback);

        verify(callback).onResult(false, null);
    }

    @Test
    public void isReadyToPay_whenVenmoEnabledAndAppSwitchAvailable_callbackTrue() throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(Configuration.fromJson(Fixtures.CONFIGURATION_WITH_PAY_WITH_VENMO))
                .build();

        VenmoClient sut = new VenmoClient(braintreeClient, venmoAPI, sharedPrefsWriter, deviceInspector, activity, lifecycle);
        when(deviceInspector.isVenmoAppSwitchAvailable(activity)).thenReturn(true);

        VenmoIsReadyToPayCallback callback = mock(VenmoIsReadyToPayCallback.class);
        sut.isReadyToPay(activity, callback);

        verify(callback).onResult(true, null);
    }
}