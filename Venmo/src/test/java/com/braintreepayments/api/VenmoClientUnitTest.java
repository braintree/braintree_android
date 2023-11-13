package com.braintreepayments.api;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.endsWith;
import static org.mockito.ArgumentMatchers.eq;
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
    private VenmoTokenizeCallback venmoTokenizeCallback;
    private VenmoPaymentAuthRequestCallback venmoPaymentAuthRequestCallback;
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
        venmoTokenizeCallback = mock(VenmoTokenizeCallback.class);
        venmoPaymentAuthRequestCallback = mock(VenmoPaymentAuthRequestCallback.class);
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

    @Test
    public void createPaymentAuthRequest_whenCreatePaymentContextFails_collectAddressWithEcdDisabled() {
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

        ArgumentCaptor<VenmoPaymentAuthRequest> captor =
                ArgumentCaptor.forClass(VenmoPaymentAuthRequest.class);

        VenmoRequest request = new VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE);
        request.setProfileId("sample-venmo-merchant");
        request.setCollectCustomerBillingAddress(true);

        VenmoClient sut =
                new VenmoClient(braintreeClient, venmoApi, sharedPrefsWriter, deviceInspector);
        sut.createPaymentAuthRequest(activity, request, venmoPaymentAuthRequestCallback);

        verify(venmoPaymentAuthRequestCallback).onVenmoPaymentAuthRequest(captor.capture());
        verify(braintreeClient).sendAnalyticsEvent("pay-with-venmo.app-switch.failed");
        VenmoPaymentAuthRequest paymentAuthRequest = captor.getValue();
        assertTrue(paymentAuthRequest instanceof VenmoPaymentAuthRequest.Failure);
        assertEquals(
                "Cannot collect customer data when ECD is disabled. Enable this feature in the Control Panel to collect this data.",
                ((VenmoPaymentAuthRequest.Failure) paymentAuthRequest).getError().getMessage());
    }

    @Test
    public void createPaymentAuthRequest_whenCreatePaymentContextSucceeds_createsVenmoAuthChallenge() {
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
        sut.createPaymentAuthRequest(activity, request, venmoPaymentAuthRequestCallback);

        InOrder inOrder = Mockito.inOrder(venmoPaymentAuthRequestCallback, braintreeClient);

        ArgumentCaptor<VenmoPaymentAuthRequest> captor =
                ArgumentCaptor.forClass(VenmoPaymentAuthRequest.class);
        inOrder.verify(venmoPaymentAuthRequestCallback).onVenmoPaymentAuthRequest(captor.capture());

        inOrder.verify(braintreeClient).sendAnalyticsEvent("pay-with-venmo.app-switch.started");

        VenmoPaymentAuthRequest paymentAuthRequest = captor.getValue();
        assertTrue(paymentAuthRequest instanceof VenmoPaymentAuthRequest.ReadyToLaunch);
        VenmoPaymentAuthRequestParams params = ((VenmoPaymentAuthRequest.ReadyToLaunch) paymentAuthRequest).getRequestParams();
        assertEquals("sample-venmo-merchant", params.getProfileId());
        assertEquals("venmo-payment-context-id", params.getPaymentContextId());
        assertEquals("session-id", params.getSessionId());
        assertEquals("custom", params.getIntegrationType());
        assertEquals(venmoEnabledConfiguration, params.getConfiguration());
    }

    @Test
    public void createPaymentAuthRequest_whenConfigurationException_forwardsExceptionToListener() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configurationError(new Exception("Configuration fetching error"))
                .build();

        VenmoRequest request = new VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE);
        request.setProfileId(null);
        request.setShouldVault(false);

        VenmoClient sut =
                new VenmoClient(braintreeClient, venmoApi, sharedPrefsWriter, deviceInspector);
        sut.createPaymentAuthRequest(activity, request, venmoPaymentAuthRequestCallback);

        ArgumentCaptor<VenmoPaymentAuthRequest> captor =
                ArgumentCaptor.forClass(VenmoPaymentAuthRequest.class);
        verify(venmoPaymentAuthRequestCallback).onVenmoPaymentAuthRequest(captor.capture());
        VenmoPaymentAuthRequest paymentAuthRequest = captor.getValue();
        assertTrue(paymentAuthRequest instanceof VenmoPaymentAuthRequest.Failure);
        assertEquals("Configuration fetching error", ((VenmoPaymentAuthRequest.Failure) paymentAuthRequest).getError().getMessage());
        verify(braintreeClient).sendAnalyticsEvent("pay-with-venmo.app-switch.failed");
    }

    @Test
    public void createPaymentAuthRequest_whenVenmoNotEnabled_forwardsExceptionToListener() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(venmoDisabledConfiguration)
                .build();

        VenmoRequest request = new VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE);
        request.setProfileId(null);
        request.setShouldVault(false);

        VenmoClient sut =
                new VenmoClient(braintreeClient, venmoApi, sharedPrefsWriter, deviceInspector);
        sut.createPaymentAuthRequest(activity, request, venmoPaymentAuthRequestCallback);

        ArgumentCaptor<VenmoPaymentAuthRequest> captor =
                ArgumentCaptor.forClass(VenmoPaymentAuthRequest.class);
        verify(venmoPaymentAuthRequestCallback).onVenmoPaymentAuthRequest(captor.capture());
        VenmoPaymentAuthRequest paymentAuthRequest = captor.getValue();
        assertTrue(paymentAuthRequest instanceof VenmoPaymentAuthRequest.Failure);
        assertEquals("Venmo is not enabled", ((VenmoPaymentAuthRequest.Failure) paymentAuthRequest).getError().getMessage());
        verify(braintreeClient).sendAnalyticsEvent("pay-with-venmo.app-switch.failed");
    }

    @Test
    public void createPaymentAuthRequest_whenVenmoNotInstalled_forwardsExceptionToListener() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(venmoEnabledConfiguration)
                .build();

        VenmoRequest request = new VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE);
        request.setProfileId(null);
        request.setShouldVault(false);

        when(deviceInspector.isVenmoAppSwitchAvailable(activity)).thenReturn(false);

        VenmoClient sut =
                new VenmoClient(braintreeClient, venmoApi, sharedPrefsWriter, deviceInspector);
        sut.createPaymentAuthRequest(activity, request, venmoPaymentAuthRequestCallback);

        verify(deviceInspector).isVenmoAppSwitchAvailable(same(activity));

        ArgumentCaptor<VenmoPaymentAuthRequest> captor =
                ArgumentCaptor.forClass(VenmoPaymentAuthRequest.class);
        verify(venmoPaymentAuthRequestCallback).onVenmoPaymentAuthRequest(captor.capture());
        VenmoPaymentAuthRequest paymentAuthRequest = captor.getValue();
        assertTrue(paymentAuthRequest instanceof VenmoPaymentAuthRequest.Failure);
        assertEquals("Venmo is not installed", ((VenmoPaymentAuthRequest.Failure) paymentAuthRequest).getError().getMessage());
        verify(braintreeClient).sendAnalyticsEvent("pay-with-venmo.app-switch.failed");
    }

    @Test
    public void createPaymentAuthRequest_whenProfileIdIsNull_appSwitchesWithMerchantId() {
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
        sut.createPaymentAuthRequest(activity, request, venmoPaymentAuthRequestCallback);

        ArgumentCaptor<VenmoPaymentAuthRequest> captor =
                ArgumentCaptor.forClass(VenmoPaymentAuthRequest.class);
        verify(venmoPaymentAuthRequestCallback).onVenmoPaymentAuthRequest(captor.capture());
        VenmoPaymentAuthRequest paymentAuthRequest = captor.getValue();
        assertTrue(paymentAuthRequest instanceof VenmoPaymentAuthRequest.ReadyToLaunch);
        VenmoPaymentAuthRequestParams params = ((VenmoPaymentAuthRequest.ReadyToLaunch) paymentAuthRequest).getRequestParams();
        assertEquals("merchant-id", params.getProfileId());
        assertEquals("venmo-payment-context-id", params.getPaymentContextId());
        assertEquals("session-id", params.getSessionId());
        assertEquals(venmoEnabledConfiguration, params.getConfiguration());
    }

    @Test
    public void createPaymentAuthRequest_whenProfileIdIsSpecified_appSwitchesWithProfileIdAndAccessToken() {
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
        sut.createPaymentAuthRequest(activity, request, venmoPaymentAuthRequestCallback);

        ArgumentCaptor<VenmoPaymentAuthRequest> captor =
                ArgumentCaptor.forClass(VenmoPaymentAuthRequest.class);
        verify(venmoPaymentAuthRequestCallback).onVenmoPaymentAuthRequest(captor.capture());
        VenmoPaymentAuthRequest paymentAuthRequest = captor.getValue();
        assertTrue(paymentAuthRequest instanceof VenmoPaymentAuthRequest.ReadyToLaunch);
        VenmoPaymentAuthRequestParams params = ((VenmoPaymentAuthRequest.ReadyToLaunch) paymentAuthRequest).getRequestParams();
        assertEquals("second-pwv-profile-id", params.getProfileId());
        assertEquals("venmo-payment-context-id", params.getPaymentContextId());
        assertEquals("session-id", params.getSessionId());
        assertEquals(venmoEnabledConfiguration, params.getConfiguration());
    }

    @Test
    public void createPaymentAuthRequest_sendsAnalyticsEvent() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(venmoEnabledConfiguration)
                .build();

        VenmoRequest request = new VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE);
        request.setProfileId(null);
        request.setShouldVault(false);

        VenmoClient sut =
                new VenmoClient(braintreeClient, venmoApi, sharedPrefsWriter, deviceInspector);
        sut.createPaymentAuthRequest(activity, request, venmoPaymentAuthRequestCallback);

        verify(braintreeClient).sendAnalyticsEvent("pay-with-venmo.selected");
    }

    @Test
    public void createPaymentAuthRequest_sendsAnalyticsEventWhenStarted() {
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
        sut.createPaymentAuthRequest(activity, request, venmoPaymentAuthRequestCallback);

        verify(braintreeClient).sendAnalyticsEvent("pay-with-venmo.selected");
        verify(braintreeClient).sendAnalyticsEvent("pay-with-venmo.app-switch.started");
    }

    @Test
    public void createPaymentAuthRequest_whenShouldVaultIsTrue_persistsVenmoVaultTrue() {
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
        sut.createPaymentAuthRequest(activity, request, venmoPaymentAuthRequestCallback);

        verify(sharedPrefsWriter).persistVenmoVaultOption(activity, true);
    }

    @Test
    public void createPaymentAuthRequest_whenShouldVaultIsFalse_persistsVenmoVaultFalse() {
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
        sut.createPaymentAuthRequest(activity, request, venmoPaymentAuthRequestCallback);

        verify(sharedPrefsWriter).persistVenmoVaultOption(activity, false);
    }

    @Test
    public void createPaymentAuthRequest_withTokenizationKey_persistsVenmoVaultFalse() {
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
        sut.createPaymentAuthRequest(activity, request, venmoPaymentAuthRequestCallback);

        verify(sharedPrefsWriter).persistVenmoVaultOption(activity, false);
    }

    @Test
    public void createPaymentAuthRequest_sendsAnalyticsEventWhenUnavailableAndPostException() {
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
        sut.createPaymentAuthRequest(activity, request, venmoPaymentAuthRequestCallback);

        ArgumentCaptor<VenmoPaymentAuthRequest> captor =
                ArgumentCaptor.forClass(VenmoPaymentAuthRequest.class);
        InOrder order = inOrder(braintreeClient);

        verify(venmoPaymentAuthRequestCallback).onVenmoPaymentAuthRequest(captor.capture());
        VenmoPaymentAuthRequest paymentAuthRequest = captor.getValue();
        assertTrue(paymentAuthRequest instanceof VenmoPaymentAuthRequest.Failure);
        assertEquals("Venmo is not installed", ((VenmoPaymentAuthRequest.Failure) paymentAuthRequest).getError().getMessage());

        order.verify(braintreeClient).sendAnalyticsEvent("pay-with-venmo.selected");
        order.verify(braintreeClient).sendAnalyticsEvent("pay-with-venmo.app-switch.failed");
    }

    @Test
    public void createPaymentAuthRequest_whenVenmoApiError_forwardsErrorToListener_andSendsAnalytics() {
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
        sut.createPaymentAuthRequest(activity, request, venmoPaymentAuthRequestCallback);

        ArgumentCaptor<VenmoPaymentAuthRequest> captor =
                ArgumentCaptor.forClass(VenmoPaymentAuthRequest.class);
        verify(venmoPaymentAuthRequestCallback).onVenmoPaymentAuthRequest(captor.capture());
        VenmoPaymentAuthRequest paymentAuthRequest = captor.getValue();
        assertTrue(paymentAuthRequest instanceof VenmoPaymentAuthRequest.Failure);
        assertEquals(graphQLError, ((VenmoPaymentAuthRequest.Failure) paymentAuthRequest).getError());
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

        verify(callback).onVenmoReadinessResult(any(VenmoReadinessResult.Failure.class));
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

        verify(callback).onVenmoReadinessResult(VenmoReadinessResult.NotReadyToPay.INSTANCE);
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

        verify(callback).onVenmoReadinessResult(VenmoReadinessResult.NotReadyToPay.INSTANCE);
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

        verify(callback).onVenmoReadinessResult(VenmoReadinessResult.ReadyToPay.INSTANCE);
    }

    @Test
    public void tokenize_withPaymentContextId_requestFromVenmoApi() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(venmoEnabledConfiguration)
                .authorizationSuccess(clientToken)
                .build();

        VenmoClient sut =
                new VenmoClient(braintreeClient, venmoApi, sharedPrefsWriter, deviceInspector);

        VenmoPaymentAuthResult venmoPaymentAuthResult =
                new VenmoPaymentAuthResult("payment-context-id", "payment-context-id",
                        "venmo-username", null);
        sut.tokenize(venmoPaymentAuthResult, venmoTokenizeCallback);

        verify(venmoApi).createNonceFromPaymentContext(eq("payment-context-id"),
                any(VenmoInternalCallback.class));
    }

    @Test
    public void tokenize_onGraphQLPostSuccess_returnsNonceToListener_andSendsAnalytics()
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

        VenmoPaymentAuthResult venmoPaymentAuthResult =
                new VenmoPaymentAuthResult("payment-context-id", "venmo-nonce", "venmo-username",
                        null);
        sut.tokenize(venmoPaymentAuthResult, venmoTokenizeCallback);

        ArgumentCaptor<VenmoResult> captor = ArgumentCaptor.forClass(VenmoResult.class);
        verify(venmoTokenizeCallback).onVenmoResult(captor.capture());

        VenmoResult result = captor.getValue();
        assertTrue(result instanceof VenmoResult.Success);
        VenmoAccountNonce nonce = ((VenmoResult.Success) result).getNonce();
        assertEquals("fake-venmo-nonce", nonce.getString());
        assertEquals("venmojoe", nonce.getUsername());

        verify(braintreeClient).sendAnalyticsEvent("pay-with-venmo.app-switch.success");
    }

    @Test
    public void tokenize_onGraphQLPostFailure_forwardsExceptionToListener_andSendsAnalytics() {
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

        VenmoPaymentAuthResult venmoPaymentAuthResult =
                new VenmoPaymentAuthResult("payment-context-id", "venmo-nonce", "venmo-username",
                        null);
        sut.tokenize(venmoPaymentAuthResult, venmoTokenizeCallback);

        ArgumentCaptor<VenmoResult> captor = ArgumentCaptor.forClass(VenmoResult.class);
        verify(venmoTokenizeCallback).onVenmoResult(captor.capture());

        VenmoResult result = captor.getValue();
        assertTrue(result instanceof VenmoResult.Failure);
        assertEquals(graphQLError, ((VenmoResult.Failure) result).getError());
        verify(braintreeClient).sendAnalyticsEvent("pay-with-venmo.app-switch.failure");
    }

    @Test
    public void tokenize_withPaymentContext_performsVaultRequestIfRequestPersisted() {
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

        VenmoPaymentAuthResult venmoPaymentAuthResult =
                new VenmoPaymentAuthResult("payment-context-id", "some-nonce", "venmo-username",
                        null);
        sut.tokenize(venmoPaymentAuthResult, venmoTokenizeCallback);

        verify(venmoApi).vaultVenmoAccountNonce(eq("some-nonce"), any(VenmoInternalCallback.class));
    }

    @Test
    public void tokenize_postsPaymentMethodNonceOnSuccess() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .authorizationSuccess(clientToken)
                .build();

        VenmoClient sut =
                new VenmoClient(braintreeClient, venmoApi, sharedPrefsWriter, deviceInspector);

        VenmoPaymentAuthResult venmoPaymentAuthResult =
                new VenmoPaymentAuthResult("payment-context-id", "payment-context-id",
                        "venmo-username", null);
        sut.tokenize(venmoPaymentAuthResult, venmoTokenizeCallback);

        verify(venmoApi).createNonceFromPaymentContext(eq("payment-context-id"),
                any(VenmoInternalCallback.class));
    }

    @Test
    public void tokenize_sendsAnalyticsEventOnSuccess() {
        VenmoClient sut =
                new VenmoClient(braintreeClient, venmoApi, sharedPrefsWriter, deviceInspector);

        VenmoPaymentAuthResult venmoPaymentAuthResult =
                new VenmoPaymentAuthResult("payment-context-id", "some-nonce", "venmo-username",
                        null);
        sut.tokenize(venmoPaymentAuthResult, venmoTokenizeCallback);

        verify(braintreeClient).sendAnalyticsEvent("pay-with-venmo.app-switch.success");
    }

    @Test
    public void tokenize_sendsAnalyticsEventOnCancel() {
        VenmoClient sut =
                new VenmoClient(braintreeClient, venmoApi, sharedPrefsWriter, deviceInspector);

        VenmoPaymentAuthResult venmoPaymentAuthResult =
                new VenmoPaymentAuthResult("payment-context-id", null, null,
                        new UserCanceledException("User canceled Venmo."));
        sut.tokenize(venmoPaymentAuthResult, venmoTokenizeCallback);

        verify(braintreeClient).sendAnalyticsEvent("pay-with-venmo.app-switch.canceled");
    }

    @Test
    public void tokenize_forwardsExceptionToCallbackOnCancel() {
        VenmoClient sut =
                new VenmoClient(braintreeClient, venmoApi, sharedPrefsWriter, deviceInspector);

        UserCanceledException error = new UserCanceledException("User canceled Venmo.");

        VenmoPaymentAuthResult venmoPaymentAuthResult =
                new VenmoPaymentAuthResult("payment-context-id", null, null, error);
        sut.tokenize(venmoPaymentAuthResult, venmoTokenizeCallback);

        ArgumentCaptor<VenmoResult> captor = ArgumentCaptor.forClass(VenmoResult.class);
        verify(venmoTokenizeCallback).onVenmoResult(captor.capture());

        VenmoResult result = captor.getValue();
        assertTrue(result instanceof VenmoResult.Cancel);
    }

    @Test
    public void tokenize_performsVaultRequestIfRequestPersisted() throws JSONException {
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

        VenmoPaymentAuthResult venmoPaymentAuthResult =
                new VenmoPaymentAuthResult("payment-context-id", "sample-nonce", "venmo-username",
                        null);
        sut.tokenize(venmoPaymentAuthResult, venmoTokenizeCallback);

        verify(venmoApi).vaultVenmoAccountNonce(eq("fake-venmo-nonce"),
                any(VenmoInternalCallback.class));
    }

    @Test
    public void tokenize_doesNotPerformRequestIfTokenizationKeyUsed() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .sessionId("another-session-id")
                .authorizationSuccess(tokenizationKey)
                .build();

        when(deviceInspector.isVenmoAppSwitchAvailable(activity)).thenReturn(true);
        when(sharedPrefsWriter.getVenmoVaultOption(activity)).thenReturn(true);

        VenmoClient sut =
                new VenmoClient(braintreeClient, venmoApi, sharedPrefsWriter, deviceInspector);

        VenmoPaymentAuthResult venmoPaymentAuthResult =
                new VenmoPaymentAuthResult("payment-context-id", "sample-nonce", "venmo-username",
                        null);
        sut.tokenize(venmoPaymentAuthResult, venmoTokenizeCallback);

        verify(venmoApi, never()).vaultVenmoAccountNonce(anyString(),
                any(VenmoInternalCallback.class));
    }

    @Test
    public void tokenize_withSuccessfulVaultCall_forwardsResultToActivityResultListener_andSendsAnalytics() {
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

        VenmoPaymentAuthResult venmoPaymentAuthResult =
                new VenmoPaymentAuthResult(null, "sample-nonce", "venmo-username", null);
        sut.tokenize(venmoPaymentAuthResult, venmoTokenizeCallback);

        ArgumentCaptor<VenmoResult> captor = ArgumentCaptor.forClass(VenmoResult.class);
        verify(venmoTokenizeCallback).onVenmoResult(captor.capture());

        VenmoResult result = captor.getValue();
        assertTrue(result instanceof VenmoResult.Success);
        VenmoAccountNonce nonce = ((VenmoResult.Success) result).getNonce();
        assertEquals(venmoAccountNonce, nonce);
        verify(braintreeClient).sendAnalyticsEvent(endsWith("pay-with-venmo.vault.success"));
    }

    @Test
    public void tokenize_withPaymentContext_withSuccessfulVaultCall_forwardsNonceToCallback_andSendsAnalytics()
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

        VenmoPaymentAuthResult venmoPaymentAuthResult =
                new VenmoPaymentAuthResult("payment-context-id", "sample-nonce", "venmo-username",
                        null);
        sut.tokenize(venmoPaymentAuthResult, venmoTokenizeCallback);

        ArgumentCaptor<VenmoResult> captor = ArgumentCaptor.forClass(VenmoResult.class);
        verify(venmoTokenizeCallback).onVenmoResult(captor.capture());

        VenmoResult result = captor.getValue();
        assertTrue(result instanceof VenmoResult.Success);
        VenmoAccountNonce nonce = ((VenmoResult.Success) result).getNonce();
        assertEquals(venmoAccountNonce, nonce);
        verify(braintreeClient).sendAnalyticsEvent(endsWith("pay-with-venmo.vault.success"));
    }

    @Test
    public void tokenize_withFailedVaultCall_forwardsErrorToActivityResultListener_andSendsAnalytics() {
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

        VenmoPaymentAuthResult venmoPaymentAuthResult =
                new VenmoPaymentAuthResult(null, "sample-nonce", "venmo-username", null);
        sut.tokenize(venmoPaymentAuthResult, venmoTokenizeCallback);

        ArgumentCaptor<VenmoResult> captor = ArgumentCaptor.forClass(VenmoResult.class);
        verify(venmoTokenizeCallback).onVenmoResult(captor.capture());

        VenmoResult result = captor.getValue();
        assertTrue(result instanceof VenmoResult.Failure);
        assertEquals(error, ((VenmoResult.Failure) result).getError());
        verify(braintreeClient).sendAnalyticsEvent(endsWith("pay-with-venmo.vault.failed"));
    }

    @Test
    public void tokenize_withPaymentContext_withFailedVaultCall_forwardsErrorToCallback_andSendsAnalytics()
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

        VenmoPaymentAuthResult venmoPaymentAuthResult =
                new VenmoPaymentAuthResult("payment-context-id", "sample-nonce", "venmo-username",
                        null);
        sut.tokenize(venmoPaymentAuthResult, venmoTokenizeCallback);

        ArgumentCaptor<VenmoResult> captor = ArgumentCaptor.forClass(VenmoResult.class);
        verify(venmoTokenizeCallback).onVenmoResult(captor.capture());

        VenmoResult result = captor.getValue();
        assertTrue(result instanceof VenmoResult.Failure);
        assertEquals(error, ((VenmoResult.Failure) result).getError());
        verify(braintreeClient).sendAnalyticsEvent(endsWith("pay-with-venmo.vault.failed"));
    }
}