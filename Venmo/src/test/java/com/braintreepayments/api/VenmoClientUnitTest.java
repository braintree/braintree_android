package com.braintreepayments.api;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.endsWith;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Intent;

import androidx.fragment.app.FragmentActivity;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class VenmoClientUnitTest {

    private FragmentActivity activity;
    private BraintreeClient braintreeClient;

    private Configuration venmoEnabledConfiguration;
    private Configuration venmoDisabledConfiguration;
    private VenmoResultCallback venmoResultCallback;
    private VenmoAuthChallengeCallback venmoAuthChallengeCallback;
    private VenmoSharedPrefsWriter sharedPrefsWriter;
    private DeviceInspector deviceInspector;

    private VenmoApi venmoApi;
    private Authorization clientToken;
    private Authorization tokenizationKey;

    @Before
    public void beforeEach() throws JSONException {
        activity = mock(FragmentActivity.class);
        braintreeClient = mock(BraintreeClient.class);
        venmoApi = mock(VenmoApi.class);
        deviceInspector = mock(DeviceInspector.class);

        venmoEnabledConfiguration =
                Configuration.fromJson(Fixtures.CONFIGURATION_WITH_PAY_WITH_VENMO);
        venmoDisabledConfiguration =
                Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_ACCESS_TOKEN);
        venmoResultCallback = mock(VenmoResultCallback.class);
        venmoAuthChallengeCallback = mock(VenmoAuthChallengeCallback.class);
        sharedPrefsWriter = mock(VenmoSharedPrefsWriter.class);

        clientToken = Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN);
        tokenizationKey = Authorization.fromString(Fixtures.TOKENIZATION_KEY);
    }

    @Test
    public void showVenmoInGooglePlayStore_opensVenmoAppStoreURL() {
        VenmoClient sut =
                new VenmoClient(braintreeClient, venmoApi, sharedPrefsWriter, deviceInspector);

        sut.showVenmoInGooglePlayStore(activity);

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);

        verify(activity).startActivity(captor.capture());
        assertEquals(captor.getValue().getData().toString(),
                "https://play.google.com/store/apps/details?id=com.venmo");
    }

    @Test
    public void showVenmoInGooglePlayStore_sendsAnalyticsEvent() {
        VenmoClient sut =
                new VenmoClient(braintreeClient, venmoApi, sharedPrefsWriter, deviceInspector);

        sut.showVenmoInGooglePlayStore(activity);

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);

        verify(activity).startActivity(captor.capture());
        verify(braintreeClient).sendAnalyticsEvent("android.pay-with-venmo.app-store.invoked");
    }
//
//    @Test
//    public void tokenizeVenmoAccount_whenCreatePaymentContextSucceeds_withObserver_launchesObserverWithVenmoIntentData_andSendsAnalytics() {
//        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
//                .configuration(venmoEnabledConfiguration)
//                .sessionId("session-id")
//                .integration("custom")
//                .authorizationSuccess(clientToken)
//                .build();
//
//        VenmoApi venmoApi = new MockVenmoApiBuilder()
//                .createPaymentContextSuccess("venmo-payment-context-id")
//                .build();
//
//        when(deviceInspector.isVenmoAppSwitchAvailable(activity)).thenReturn(true);
//
//        VenmoRequest request = new VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE);
//        request.setProfileId("sample-venmo-merchant");
//        request.setShouldVault(false);
//
//        VenmoClient sut = new VenmoClient(braintreeClient, venmoApi, sharedPrefsWriter, deviceInspector);
//        VenmoLifecycleObserver observer = mock(VenmoLifecycleObserver.class);
//        sut.observer = observer;
//        sut.requestAuthChallenge(activity, request);
//
//        ArgumentCaptor<VenmoAuthChallenge> captor = ArgumentCaptor.forClass(VenmoAuthChallenge.class);
//        verify(observer).launch(captor.capture());
//
//        VenmoAuthChallenge intent = captor.getValue();
//        assertEquals("venmo-payment-context-id", intent.getPaymentContextId());
//        assertSame(venmoEnabledConfiguration, intent.getConfiguration());
//        assertEquals("custom", intent.getIntegrationType());
//        assertEquals("sample-venmo-merchant", intent.getProfileId());
//        assertEquals("session-id", intent.getSessionId());
//
//        verify(sharedPrefsWriter).persistVenmoVaultOption(activity, false);
//        verify(braintreeClient).sendAnalyticsEvent("pay-with-venmo.app-switch.started");
//    }

    @Test
    public void requestAuthChallenge_whenCreatePaymentContextFails_collectAddressWithEcdDisabled() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(venmoEnabledConfiguration)
                .sessionId("session-id")
                .integration("custom")
                .authorizationSuccess(clientToken)
                .build();

        VenmoApi venmoApi = new MockVenmoApiBuilder()
                .createPaymentContextSuccess("venmo-payment-context-id")
                .build();

        when(deviceInspector.isVenmoAppSwitchAvailable(activity)).thenReturn(true);

        ArgumentCaptor<BraintreeException> captor =
                ArgumentCaptor.forClass(BraintreeException.class);

        VenmoRequest request = new VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE);
        request.setProfileId("sample-venmo-merchant");
        request.setCollectCustomerBillingAddress(true);

        VenmoClient sut =
                new VenmoClient(braintreeClient, venmoApi, sharedPrefsWriter, deviceInspector);
        sut.requestAuthChallenge(activity, request, venmoAuthChallengeCallback);

        verify(venmoAuthChallengeCallback).onVenmoAuthChallenge(isNull(), captor.capture());
        verify(braintreeClient).sendAnalyticsEvent("pay-with-venmo.app-switch.failed");
        assertEquals(
                "Cannot collect customer data when ECD is disabled. Enable this feature in the Control Panel to collect this data.",
                captor.getValue().getMessage());
    }

    @Test
    public void requestAuthChallenge_whenCreatePaymentContextSucceeds_createsVenmoAuthChallenge() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(venmoEnabledConfiguration)
                .sessionId("session-id")
                .integration("custom")
                .authorizationSuccess(clientToken)
                .build();

        VenmoApi venmoApi = new MockVenmoApiBuilder()
                .createPaymentContextSuccess("venmo-payment-context-id")
                .build();

        when(deviceInspector.isVenmoAppSwitchAvailable(activity)).thenReturn(true);

        VenmoRequest request = new VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE);
        request.setProfileId("sample-venmo-merchant");
        request.setShouldVault(false);

        VenmoClient sut =
                new VenmoClient(braintreeClient, venmoApi, sharedPrefsWriter, deviceInspector);
        sut.requestAuthChallenge(activity, request, venmoAuthChallengeCallback);

        InOrder inOrder = Mockito.inOrder(venmoAuthChallengeCallback, braintreeClient);

        ArgumentCaptor<VenmoAuthChallenge> captor =
                ArgumentCaptor.forClass(VenmoAuthChallenge.class);
        inOrder.verify(venmoAuthChallengeCallback).onVenmoAuthChallenge(captor.capture(), isNull());

        inOrder.verify(braintreeClient).sendAnalyticsEvent("pay-with-venmo.app-switch.started");

        VenmoAuthChallenge authChallenge = captor.getValue();
        assertEquals("sample-venmo-merchant", authChallenge.getProfileId());
        assertEquals("venmo-payment-context-id", authChallenge.getPaymentContextId());
        assertEquals("session-id", authChallenge.getSessionId());
        assertEquals("custom", authChallenge.getIntegrationType());
        assertEquals(venmoEnabledConfiguration, authChallenge.getConfiguration());
        assertNull(authChallenge.getError());
    }

    @Test
    public void requestAuthChallenge_whenConfigurationException_forwardsExceptionToListener() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configurationError(new Exception("Configuration fetching error"))
                .build();

        VenmoRequest request = new VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE);
        request.setProfileId(null);
        request.setShouldVault(false);

        VenmoClient sut =
                new VenmoClient(braintreeClient, venmoApi, sharedPrefsWriter, deviceInspector);
        sut.requestAuthChallenge(activity, request, venmoAuthChallengeCallback);

        ArgumentCaptor<Exception> captor =
                ArgumentCaptor.forClass(Exception.class);
        verify(venmoAuthChallengeCallback).onVenmoAuthChallenge(isNull(), captor.capture());
        assertEquals("Configuration fetching error", captor.getValue().getMessage());
        verify(braintreeClient).sendAnalyticsEvent("pay-with-venmo.app-switch.failed");
    }

    @Test
    public void requestAuthChallenge_whenVenmoNotEnabled_forwardsExceptionToListener() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(venmoDisabledConfiguration)
                .build();

        VenmoRequest request = new VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE);
        request.setProfileId(null);
        request.setShouldVault(false);

        VenmoClient sut =
                new VenmoClient(braintreeClient, venmoApi, sharedPrefsWriter, deviceInspector);
        sut.requestAuthChallenge(activity, request, venmoAuthChallengeCallback);

        ArgumentCaptor<AppSwitchNotAvailableException> captor =
                ArgumentCaptor.forClass(AppSwitchNotAvailableException.class);
        verify(venmoAuthChallengeCallback).onVenmoAuthChallenge(isNull(), captor.capture());
        assertEquals("Venmo is not enabled", captor.getValue().getMessage());
        verify(braintreeClient).sendAnalyticsEvent("pay-with-venmo.app-switch.failed");
    }

    @Test
    public void requestAuthChallenge_whenVenmoNotInstalled_forwardsExceptionToListener() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(venmoEnabledConfiguration)
                .build();

        VenmoRequest request = new VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE);
        request.setProfileId(null);
        request.setShouldVault(false);

        when(deviceInspector.isVenmoAppSwitchAvailable(activity)).thenReturn(false);

        VenmoClient sut =
                new VenmoClient(braintreeClient, venmoApi, sharedPrefsWriter, deviceInspector);
        sut.requestAuthChallenge(activity, request, venmoAuthChallengeCallback);

        verify(deviceInspector).isVenmoAppSwitchAvailable(same(activity));

        ArgumentCaptor<AppSwitchNotAvailableException> captor =
                ArgumentCaptor.forClass(AppSwitchNotAvailableException.class);
        verify(venmoAuthChallengeCallback).onVenmoAuthChallenge(isNull(), captor.capture());
        assertEquals("Venmo is not installed", captor.getValue().getMessage());
        verify(braintreeClient).sendAnalyticsEvent("pay-with-venmo.app-switch.failed");
    }

    @Test
    public void requestAuthChallenge_whenProfileIdIsNull_appSwitchesWithMerchantId() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(venmoEnabledConfiguration)
                .sessionId("session-id")
                .integration("custom")
                .authorizationSuccess(clientToken)
                .build();

        VenmoApi venmoApi = new MockVenmoApiBuilder()
                .createPaymentContextSuccess("venmo-payment-context-id")
                .build();

        VenmoRequest request = new VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE);
        request.setProfileId(null);
        request.setShouldVault(false);

        when(deviceInspector.isVenmoAppSwitchAvailable(activity)).thenReturn(true);

        VenmoClient sut =
                new VenmoClient(braintreeClient, venmoApi, sharedPrefsWriter, deviceInspector);
        sut.requestAuthChallenge(activity, request, venmoAuthChallengeCallback);

        ArgumentCaptor<VenmoAuthChallenge> captor =
                ArgumentCaptor.forClass(VenmoAuthChallenge.class);
        verify(venmoAuthChallengeCallback).onVenmoAuthChallenge(captor.capture(), isNull());
        assertEquals("merchant-id", captor.getValue().getProfileId());
        assertEquals("venmo-payment-context-id", captor.getValue().getPaymentContextId());
        assertEquals("session-id", captor.getValue().getSessionId());
        assertEquals(venmoEnabledConfiguration, captor.getValue().getConfiguration());
        assertNull(captor.getValue().getError());
    }

    @Test
    public void requestAuthChallenge_whenProfileIdIsSpecified_appSwitchesWithProfileIdAndAccessToken() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(venmoEnabledConfiguration)
                .sessionId("session-id")
                .integration("custom")
                .authorizationSuccess(clientToken)
                .build();

        VenmoApi venmoApi = new MockVenmoApiBuilder()
                .createPaymentContextSuccess("venmo-payment-context-id")
                .build();

        VenmoRequest request = new VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE);
        request.setProfileId("second-pwv-profile-id");
        request.setShouldVault(false);

        when(deviceInspector.isVenmoAppSwitchAvailable(activity)).thenReturn(true);

        VenmoClient sut =
                new VenmoClient(braintreeClient, venmoApi, sharedPrefsWriter, deviceInspector);
        sut.requestAuthChallenge(activity, request, venmoAuthChallengeCallback);

        ArgumentCaptor<VenmoAuthChallenge> captor =
                ArgumentCaptor.forClass(VenmoAuthChallenge.class);
        verify(venmoAuthChallengeCallback).onVenmoAuthChallenge(captor.capture(), isNull());
        assertEquals("second-pwv-profile-id", captor.getValue().getProfileId());
        assertEquals("venmo-payment-context-id", captor.getValue().getPaymentContextId());
        assertEquals("session-id", captor.getValue().getSessionId());
        assertEquals(venmoEnabledConfiguration, captor.getValue().getConfiguration());
        assertNull(captor.getValue().getError());
    }

    @Test
    public void requestAuthChallenge_sendsAnalyticsEvent() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(venmoEnabledConfiguration)
                .build();

        VenmoRequest request = new VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE);
        request.setProfileId(null);
        request.setShouldVault(false);

        VenmoClient sut =
                new VenmoClient(braintreeClient, venmoApi, sharedPrefsWriter, deviceInspector);
        sut.requestAuthChallenge(activity, request, venmoAuthChallengeCallback);

        verify(braintreeClient).sendAnalyticsEvent("pay-with-venmo.selected");
    }

    @Test
    public void requestAuthChallenge_sendsAnalyticsEventWhenStarted() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(venmoEnabledConfiguration)
                .authorizationSuccess(clientToken)
                .build();

        VenmoApi venmoApi = new MockVenmoApiBuilder()
                .createPaymentContextSuccess("venmo-payment-context-id")
                .build();

        VenmoRequest request = new VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE);
        request.setProfileId(null);
        request.setShouldVault(false);

        when(deviceInspector.isVenmoAppSwitchAvailable(activity)).thenReturn(true);

        VenmoClient sut =
                new VenmoClient(braintreeClient, venmoApi, sharedPrefsWriter, deviceInspector);
        sut.requestAuthChallenge(activity, request, venmoAuthChallengeCallback);

        verify(braintreeClient).sendAnalyticsEvent("pay-with-venmo.selected");
        verify(braintreeClient).sendAnalyticsEvent("pay-with-venmo.app-switch.started");
    }

    @Test
    public void requestAuthChallenge_whenShouldVaultIsTrue_persistsVenmoVaultTrue() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(venmoEnabledConfiguration)
                .authorizationSuccess(clientToken)
                .build();

        VenmoApi venmoApi = new MockVenmoApiBuilder()
                .createPaymentContextSuccess("venmo-payment-context-id")
                .build();

        VenmoRequest request = new VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE);
        request.setProfileId(null);
        request.setShouldVault(true);

        when(deviceInspector.isVenmoAppSwitchAvailable(activity)).thenReturn(true);

        VenmoClient sut =
                new VenmoClient(braintreeClient, venmoApi, sharedPrefsWriter, deviceInspector);
        sut.requestAuthChallenge(activity, request, venmoAuthChallengeCallback);

        verify(sharedPrefsWriter).persistVenmoVaultOption(activity, true);
    }

    @Test
    public void requestAuthChallenge_whenShouldVaultIsFalse_persistsVenmoVaultFalse() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(venmoEnabledConfiguration)
                .authorizationSuccess(clientToken)
                .build();

        VenmoApi venmoApi = new MockVenmoApiBuilder()
                .createPaymentContextSuccess("venmo-payment-context-id")
                .build();

        VenmoRequest request = new VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE);
        request.setProfileId(null);
        request.setShouldVault(false);

        when(deviceInspector.isVenmoAppSwitchAvailable(activity)).thenReturn(true);

        VenmoClient sut =
                new VenmoClient(braintreeClient, venmoApi, sharedPrefsWriter, deviceInspector);
        sut.requestAuthChallenge(activity, request, venmoAuthChallengeCallback);

        verify(sharedPrefsWriter).persistVenmoVaultOption(activity, false);
    }

    @Test
    public void requestAuthChallenge_withTokenizationKey_persistsVenmoVaultFalse() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(venmoEnabledConfiguration)
                .sessionId("session-id")
                .authorizationSuccess(clientToken)
                .build();

        VenmoApi venmoApi = new MockVenmoApiBuilder()
                .createPaymentContextSuccess("venmo-payment-context-id")
                .build();

        VenmoRequest request = new VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE);
        request.setProfileId(null);
        request.setShouldVault(false);

        when(deviceInspector.isVenmoAppSwitchAvailable(activity)).thenReturn(true);

        VenmoClient sut =
                new VenmoClient(braintreeClient, venmoApi, sharedPrefsWriter, deviceInspector);
        sut.requestAuthChallenge(activity, request, venmoAuthChallengeCallback);

        verify(sharedPrefsWriter).persistVenmoVaultOption(activity, false);
    }

    @Test
    public void requestAuthChallenge_sendsAnalyticsEventWhenUnavailableAndPostException() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(venmoEnabledConfiguration)
                .build();

        VenmoApi venmoApi = new MockVenmoApiBuilder()
                .createPaymentContextError(new Exception("Error"))
                .build();

        VenmoRequest request = new VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE);
        request.setProfileId(null);
        request.setShouldVault(false);

        when(deviceInspector.isVenmoAppSwitchAvailable(activity)).thenReturn(false);

        VenmoClient sut =
                new VenmoClient(braintreeClient, venmoApi, sharedPrefsWriter, deviceInspector);
        sut.requestAuthChallenge(activity, request, venmoAuthChallengeCallback);

        ArgumentCaptor<AppSwitchNotAvailableException> captor =
                ArgumentCaptor.forClass(AppSwitchNotAvailableException.class);
        InOrder order = inOrder(braintreeClient);

        verify(venmoAuthChallengeCallback).onVenmoAuthChallenge(isNull(), captor.capture());
        assertEquals("Venmo is not installed", captor.getValue().getMessage());

        order.verify(braintreeClient).sendAnalyticsEvent("pay-with-venmo.selected");
        order.verify(braintreeClient).sendAnalyticsEvent("pay-with-venmo.app-switch.failed");
    }

    @Test
    public void requestAuthChallenge_whenVenmoApiError_forwardsErrorToListener_andSendsAnalytics() {
        BraintreeException graphQLError = new BraintreeException("GraphQL error");
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(venmoEnabledConfiguration)
                .sendGraphQLPOSTErrorResponse(graphQLError)
                .build();

        VenmoApi venmoApi = new MockVenmoApiBuilder()
                .createPaymentContextError(graphQLError)
                .build();

        VenmoRequest request = new VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE);
        when(deviceInspector.isVenmoAppSwitchAvailable(activity)).thenReturn(true);

        VenmoClient sut =
                new VenmoClient(braintreeClient, venmoApi, sharedPrefsWriter, deviceInspector);
        sut.requestAuthChallenge(activity, request, venmoAuthChallengeCallback);

        verify(venmoAuthChallengeCallback).onVenmoAuthChallenge(null, graphQLError);
        verify(braintreeClient).sendAnalyticsEvent("pay-with-venmo.app-switch.failed");
    }

    @Test
    public void isReadyToPay_whenConfigurationFails_callbackFalseAndPropagatesError() {
        Exception configError = new Exception("configuration error");
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configurationError(configError)
                .build();

        VenmoClient sut =
                new VenmoClient(braintreeClient, venmoApi, sharedPrefsWriter, deviceInspector);

        VenmoIsReadyToPayCallback callback = mock(VenmoIsReadyToPayCallback.class);
        sut.isReadyToPay(activity, callback);

        verify(callback).onResult(false, configError);
    }

    @Test
    public void isReadyToPay_whenVenmoDisabled_callbackFalse() throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ANALYTICS))
                .build();

        VenmoClient sut =
                new VenmoClient(braintreeClient, venmoApi, sharedPrefsWriter, deviceInspector);

        VenmoIsReadyToPayCallback callback = mock(VenmoIsReadyToPayCallback.class);
        sut.isReadyToPay(activity, callback);

        verify(callback).onResult(false, null);
    }

    @Test
    public void isReadyToPay_whenVenmoEnabledAndAppSwitchUnavailable_callbackFalse()
            throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(Configuration.fromJson(Fixtures.CONFIGURATION_WITH_PAY_WITH_VENMO))
                .build();

        VenmoClient sut =
                new VenmoClient(braintreeClient, venmoApi, sharedPrefsWriter, deviceInspector);
        when(deviceInspector.isVenmoAppSwitchAvailable(activity)).thenReturn(false);

        VenmoIsReadyToPayCallback callback = mock(VenmoIsReadyToPayCallback.class);
        sut.isReadyToPay(activity, callback);

        verify(callback).onResult(false, null);
    }

    @Test
    public void isReadyToPay_whenVenmoEnabledAndAppSwitchAvailable_callbackTrue()
            throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(Configuration.fromJson(Fixtures.CONFIGURATION_WITH_PAY_WITH_VENMO))
                .build();

        VenmoClient sut =
                new VenmoClient(braintreeClient, venmoApi, sharedPrefsWriter, deviceInspector);
        when(deviceInspector.isVenmoAppSwitchAvailable(activity)).thenReturn(true);

        VenmoIsReadyToPayCallback callback = mock(VenmoIsReadyToPayCallback.class);
        sut.isReadyToPay(activity, callback);

        verify(callback).onResult(true, null);
    }

    @Test
    public void tokenizeVenmoAccount_withPaymentContextId_requestFromVenmoApi() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(venmoEnabledConfiguration)
                .authorizationSuccess(clientToken)
                .build();

        VenmoClient sut =
                new VenmoClient(braintreeClient, venmoApi, sharedPrefsWriter, deviceInspector);

        VenmoAuthChallengeResult venmoAuthChallengeResult =
                new VenmoAuthChallengeResult("payment-context-id", "payment-context-id",
                        "venmo-username", null);
        sut.tokenizeVenmoAccount(venmoAuthChallengeResult, venmoResultCallback);

        verify(venmoApi).createNonceFromPaymentContext(eq("payment-context-id"),
                any(VenmoResultCallback.class));
    }

    @Test
    public void tokenizeVenmoAccount_onGraphQLPostSuccess_returnsNonceToListener_andSendsAnalytics()
            throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(venmoEnabledConfiguration)
                .authorizationSuccess(clientToken)
                .build();

        VenmoApi venmoApi = new MockVenmoApiBuilder()
                .createNonceFromPaymentContextSuccess(VenmoAccountNonce.fromJSON(
                        new JSONObject(Fixtures.PAYMENT_METHODS_VENMO_ACCOUNT_RESPONSE)))
                .build();

        VenmoClient sut =
                new VenmoClient(braintreeClient, venmoApi, sharedPrefsWriter, deviceInspector);

        VenmoAuthChallengeResult venmoAuthChallengeResult =
                new VenmoAuthChallengeResult("payment-context-id", "venmo-nonce", "venmo-username",
                        null);
        sut.tokenizeVenmoAccount(venmoAuthChallengeResult, venmoResultCallback);

        ArgumentCaptor<VenmoAccountNonce> captor = ArgumentCaptor.forClass(VenmoAccountNonce.class);
        verify(venmoResultCallback).onResult(captor.capture(), isNull());

        VenmoAccountNonce nonce = captor.getValue();
        assertEquals("fake-venmo-nonce", nonce.getString());
        assertEquals("venmojoe", nonce.getUsername());

        verify(braintreeClient).sendAnalyticsEvent("pay-with-venmo.app-switch.success");
    }

    @Test
    public void tokenizeVenmoAccount_onGraphQLPostFailure_forwardsExceptionToListener_andSendsAnalytics() {
        BraintreeException graphQLError = new BraintreeException("graphQL error");
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(venmoEnabledConfiguration)
                .authorizationSuccess(clientToken)
                .sendGraphQLPOSTErrorResponse(graphQLError)
                .build();

        VenmoApi venmoApi = new MockVenmoApiBuilder()
                .createNonceFromPaymentContextError(graphQLError)
                .build();

        VenmoClient sut =
                new VenmoClient(braintreeClient, venmoApi, sharedPrefsWriter, deviceInspector);

        VenmoAuthChallengeResult venmoAuthChallengeResult =
                new VenmoAuthChallengeResult("payment-context-id", "venmo-nonce", "venmo-username",
                        null);
        sut.tokenizeVenmoAccount(venmoAuthChallengeResult, venmoResultCallback);

        verify(venmoResultCallback).onResult(null, graphQLError);
        verify(braintreeClient).sendAnalyticsEvent("pay-with-venmo.app-switch.failure");
    }

    @Test
    public void tokenizeVenmoAccount_withPaymentContext_performsVaultRequestIfRequestPersisted() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(venmoEnabledConfiguration)
                .sessionId("session-id")
                .authorizationSuccess(clientToken)
                .build();
        when(braintreeClient.getApplicationContext()).thenReturn(activity);

        VenmoAccountNonce nonce = mock(VenmoAccountNonce.class);
        when(nonce.getString()).thenReturn("some-nonce");

        VenmoApi venmoApi = new MockVenmoApiBuilder()
                .createNonceFromPaymentContextSuccess(nonce)
                .build();

        VenmoRequest request = new VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE);
        request.setProfileId(null);
        request.setShouldVault(true);

        when(deviceInspector.isVenmoAppSwitchAvailable(activity)).thenReturn(true);
        when(sharedPrefsWriter.getVenmoVaultOption(activity)).thenReturn(true);

        VenmoClient sut =
                new VenmoClient(braintreeClient, venmoApi, sharedPrefsWriter, deviceInspector);

        VenmoAuthChallengeResult venmoAuthChallengeResult =
                new VenmoAuthChallengeResult("payment-context-id", "some-nonce", "venmo-username",
                        null);
        sut.tokenizeVenmoAccount(venmoAuthChallengeResult, venmoResultCallback);

        verify(venmoApi).vaultVenmoAccountNonce(eq("some-nonce"), any(VenmoResultCallback.class));
    }

    @Test
    public void tokenizeVenmoAccount_postsPaymentMethodNonceOnSuccess() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .authorizationSuccess(clientToken)
                .build();

        VenmoClient sut =
                new VenmoClient(braintreeClient, venmoApi, sharedPrefsWriter, deviceInspector);

        VenmoAuthChallengeResult venmoAuthChallengeResult =
                new VenmoAuthChallengeResult("payment-context-id", "payment-context-id",
                        "venmo-username", null);
        sut.tokenizeVenmoAccount(venmoAuthChallengeResult, venmoResultCallback);

        verify(venmoApi).createNonceFromPaymentContext(eq("payment-context-id"),
                any(VenmoResultCallback.class));
    }

    @Test
    public void tokenizeVenmoAccount_sendsAnalyticsEventOnSuccess() {
        VenmoClient sut =
                new VenmoClient(braintreeClient, venmoApi, sharedPrefsWriter, deviceInspector);

        VenmoAuthChallengeResult venmoAuthChallengeResult =
                new VenmoAuthChallengeResult("payment-context-id", "some-nonce", "venmo-username",
                        null);
        sut.tokenizeVenmoAccount(venmoAuthChallengeResult, venmoResultCallback);

        verify(braintreeClient).sendAnalyticsEvent("pay-with-venmo.app-switch.success");
    }

    @Test
    public void tokenizeVenmoAccount_sendsAnalyticsEventOnCancel() {
        VenmoClient sut =
                new VenmoClient(braintreeClient, venmoApi, sharedPrefsWriter, deviceInspector);

        VenmoAuthChallengeResult venmoAuthChallengeResult =
                new VenmoAuthChallengeResult("payment-context-id", null, null,
                        new UserCanceledException("User canceled Venmo."));
        sut.tokenizeVenmoAccount(venmoAuthChallengeResult, venmoResultCallback);

        verify(braintreeClient).sendAnalyticsEvent("pay-with-venmo.app-switch.canceled");
    }

    @Test
    public void tokenizeVenmoAccount_forwardsExceptionToCallbackOnCancel() {
        VenmoClient sut =
                new VenmoClient(braintreeClient, venmoApi, sharedPrefsWriter, deviceInspector);

        UserCanceledException error = new UserCanceledException("User canceled Venmo.");

        VenmoAuthChallengeResult venmoAuthChallengeResult =
                new VenmoAuthChallengeResult("payment-context-id", null, null, error);
        sut.tokenizeVenmoAccount(venmoAuthChallengeResult, venmoResultCallback);

        verify(venmoResultCallback).onResult(null, error);
    }

    @Test
    public void tokenizeVenmoAccount_performsVaultRequestIfRequestPersisted() throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(venmoEnabledConfiguration)
                .sessionId("session-id")
                .authorizationSuccess(clientToken)
                .build();
        when(braintreeClient.getApplicationContext()).thenReturn(activity);

        VenmoApi venmoApi = new MockVenmoApiBuilder()
                .createNonceFromPaymentContextSuccess(VenmoAccountNonce.fromJSON(
                        new JSONObject(Fixtures.PAYMENT_METHODS_VENMO_ACCOUNT_RESPONSE)))
                .build();

        VenmoRequest request = new VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE);
        request.setProfileId(null);
        request.setShouldVault(true);

        when(deviceInspector.isVenmoAppSwitchAvailable(activity)).thenReturn(true);
        when(sharedPrefsWriter.getVenmoVaultOption(activity)).thenReturn(true);

        VenmoClient sut =
                new VenmoClient(braintreeClient, venmoApi, sharedPrefsWriter, deviceInspector);

        VenmoAuthChallengeResult venmoAuthChallengeResult =
                new VenmoAuthChallengeResult("payment-context-id", "sample-nonce", "venmo-username",
                        null);
        sut.tokenizeVenmoAccount(venmoAuthChallengeResult, venmoResultCallback);

        verify(venmoApi).vaultVenmoAccountNonce(eq("fake-venmo-nonce"),
                any(VenmoResultCallback.class));
    }

    @Test
    public void tokenizeVenmoAccount_doesNotPerformRequestIfTokenizationKeyUsed() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .sessionId("another-session-id")
                .authorizationSuccess(tokenizationKey)
                .build();

        when(deviceInspector.isVenmoAppSwitchAvailable(activity)).thenReturn(true);
        when(sharedPrefsWriter.getVenmoVaultOption(activity)).thenReturn(true);

        VenmoClient sut =
                new VenmoClient(braintreeClient, venmoApi, sharedPrefsWriter, deviceInspector);

        VenmoAuthChallengeResult venmoAuthChallengeResult =
                new VenmoAuthChallengeResult("payment-context-id", "sample-nonce", "venmo-username",
                        null);
        sut.tokenizeVenmoAccount(venmoAuthChallengeResult, venmoResultCallback);

        verify(venmoApi, never()).vaultVenmoAccountNonce(anyString(),
                any(VenmoResultCallback.class));
    }

    @Test
    public void tokenizeVenmoAccount_withSuccessfulVaultCall_forwardsResultToActivityResultListener_andSendsAnalytics() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .sessionId("session-id")
                .authorizationSuccess(clientToken)
                .build();
        when(braintreeClient.getApplicationContext()).thenReturn(activity);

        VenmoAccountNonce venmoAccountNonce = mock(VenmoAccountNonce.class);

        VenmoApi venmoApi = new MockVenmoApiBuilder()
                .vaultVenmoAccountNonceSuccess(venmoAccountNonce)
                .build();

        when(deviceInspector.isVenmoAppSwitchAvailable(activity)).thenReturn(true);
        when(sharedPrefsWriter.getVenmoVaultOption(activity)).thenReturn(true);

        VenmoClient sut =
                new VenmoClient(braintreeClient, venmoApi, sharedPrefsWriter, deviceInspector);

        VenmoAuthChallengeResult venmoAuthChallengeResult =
                new VenmoAuthChallengeResult(null, "sample-nonce", "venmo-username", null);
        sut.tokenizeVenmoAccount(venmoAuthChallengeResult, venmoResultCallback);

        verify(venmoResultCallback).onResult(venmoAccountNonce, null);
        verify(braintreeClient).sendAnalyticsEvent(endsWith("pay-with-venmo.vault.success"));
    }

    @Test
    public void tokenizeVenmoAccount_withPaymentContext_withSuccessfulVaultCall_forwardsNonceToCallback_andSendsAnalytics()
            throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .sessionId("session-id")
                .authorizationSuccess(clientToken)
                .sendGraphQLPOSTSuccessfulResponse(
                        Fixtures.VENMO_GRAPHQL_GET_PAYMENT_CONTEXT_RESPONSE)
                .build();
        when(braintreeClient.getApplicationContext()).thenReturn(activity);

        VenmoAccountNonce venmoAccountNonce = VenmoAccountNonce.fromJSON(
                new JSONObject(Fixtures.PAYMENT_METHODS_VENMO_ACCOUNT_RESPONSE));

        VenmoApi venmoApi = new MockVenmoApiBuilder()
                .createNonceFromPaymentContextSuccess(venmoAccountNonce)
                .vaultVenmoAccountNonceSuccess(venmoAccountNonce)
                .build();

        when(deviceInspector.isVenmoAppSwitchAvailable(activity)).thenReturn(true);
        when(sharedPrefsWriter.getVenmoVaultOption(activity)).thenReturn(true);

        VenmoClient sut =
                new VenmoClient(braintreeClient, venmoApi, sharedPrefsWriter, deviceInspector);

        VenmoAuthChallengeResult venmoAuthChallengeResult =
                new VenmoAuthChallengeResult("payment-context-id", "sample-nonce", "venmo-username",
                        null);
        sut.tokenizeVenmoAccount(venmoAuthChallengeResult, venmoResultCallback);

        verify(venmoResultCallback).onResult(venmoAccountNonce, null);
        verify(braintreeClient).sendAnalyticsEvent(endsWith("pay-with-venmo.vault.success"));
    }

    @Test
    public void tokenizeVenmoAccount_withFailedVaultCall_forwardsErrorToActivityResultListener_andSendsAnalytics() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .sessionId("session-id")
                .authorizationSuccess(clientToken)
                .build();
        when(braintreeClient.getApplicationContext()).thenReturn(activity);

        Exception error = new Exception("error");

        VenmoApi venmoApi = new MockVenmoApiBuilder()
                .vaultVenmoAccountNonceError(error)
                .build();

        when(deviceInspector.isVenmoAppSwitchAvailable(activity)).thenReturn(true);
        when(sharedPrefsWriter.getVenmoVaultOption(activity)).thenReturn(true);

        VenmoClient sut =
                new VenmoClient(braintreeClient, venmoApi, sharedPrefsWriter, deviceInspector);

        VenmoAuthChallengeResult venmoAuthChallengeResult =
                new VenmoAuthChallengeResult(null, "sample-nonce", "venmo-username", null);
        sut.tokenizeVenmoAccount(venmoAuthChallengeResult, venmoResultCallback);

        verify(venmoResultCallback).onResult(null, error);
        verify(braintreeClient).sendAnalyticsEvent(endsWith("pay-with-venmo.vault.failed"));
    }

    @Test
    public void tokenizeVenmoAccount_withPaymentContext_withFailedVaultCall_forwardsErrorToCallback_andSendsAnalytics()
            throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .sessionId("session-id")
                .authorizationSuccess(clientToken)
                .sendGraphQLPOSTSuccessfulResponse(
                        Fixtures.VENMO_GRAPHQL_GET_PAYMENT_CONTEXT_RESPONSE)
                .build();
        when(braintreeClient.getApplicationContext()).thenReturn(activity);

        VenmoAccountNonce venmoAccountNonce = VenmoAccountNonce.fromJSON(
                new JSONObject(Fixtures.PAYMENT_METHODS_VENMO_ACCOUNT_RESPONSE));
        Exception error = new Exception("error");

        VenmoApi venmoApi = new MockVenmoApiBuilder()
                .createNonceFromPaymentContextSuccess(venmoAccountNonce)
                .vaultVenmoAccountNonceError(error)
                .build();

        when(deviceInspector.isVenmoAppSwitchAvailable(activity)).thenReturn(true);
        when(sharedPrefsWriter.getVenmoVaultOption(activity)).thenReturn(true);

        VenmoClient sut =
                new VenmoClient(braintreeClient, venmoApi, sharedPrefsWriter, deviceInspector);

        VenmoAuthChallengeResult venmoAuthChallengeResult =
                new VenmoAuthChallengeResult("payment-context-id", "sample-nonce", "venmo-username",
                        null);
        sut.tokenizeVenmoAccount(venmoAuthChallengeResult, venmoResultCallback);

        verify(venmoResultCallback).onResult(null, error);
        verify(braintreeClient).sendAnalyticsEvent(endsWith("pay-with-venmo.vault.failed"));
    }
}