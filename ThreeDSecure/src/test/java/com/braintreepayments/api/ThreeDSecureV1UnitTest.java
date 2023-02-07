package com.braintreepayments.api;

import android.content.Context;
import android.net.Uri;

import androidx.activity.result.ActivityResultRegistry;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;

import static com.braintreepayments.api.BraintreeRequestCodes.THREE_D_SECURE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class ThreeDSecureV1UnitTest {

    private FragmentActivity activity;
    private CardinalClient cardinalClient;
    private ThreeDSecureV1BrowserSwitchHelper browserSwitchHelper;
    private ThreeDSecureListener listener;
    private Lifecycle lifecycle;

    private ThreeDSecureRequest threeDSecureRequest;
    private ThreeDSecureResult threeDSecureResult;
    private String threeDSecureLookupResponse;

    private Configuration threeDSecureEnabledConfig;

    @Before
    public void setup() throws JSONException {
        activity = mock(FragmentActivity.class);
        cardinalClient = mock(CardinalClient.class);
        browserSwitchHelper = mock(ThreeDSecureV1BrowserSwitchHelper.class);
        listener = mock(ThreeDSecureListener.class);

        threeDSecureEnabledConfig = new TestConfigurationBuilder()
                .threeDSecureEnabled(true)
                .assetsUrl("https://www.some-assets.com")
                .buildConfiguration();

        threeDSecureRequest = new ThreeDSecureRequest();
        threeDSecureRequest.setVersionRequested(ThreeDSecureRequest.VERSION_1);
        threeDSecureRequest.setNonce("a-nonce");
        threeDSecureRequest.setAmount("amount");

        ThreeDSecurePostalAddress billingAddress = new ThreeDSecurePostalAddress();
        billingAddress.setGivenName("billing-given-name");
        threeDSecureRequest.setBillingAddress(billingAddress);

        threeDSecureLookupResponse = Fixtures.THREE_D_SECURE_V1_LOOKUP_RESPONSE;
        threeDSecureResult = ThreeDSecureResult.fromJson(threeDSecureLookupResponse);

        ActivityResultRegistry resultRegistry = mock(ActivityResultRegistry.class);
        when(activity.getActivityResultRegistry()).thenReturn(resultRegistry);

        lifecycle = mock(Lifecycle.class);
        when(activity.getLifecycle()).thenReturn(lifecycle);

    }

    @Test
    public void continuePerformVerification_sendsAnalyticsEvent() throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .authorizationSuccess(Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
                .configuration(threeDSecureEnabledConfig)
                .returnUrlScheme("sample-url-scheme://")
                .build();

        when(browserSwitchHelper.getUrl(anyString(), anyString(), any(ThreeDSecureRequest.class), any(ThreeDSecureLookup.class))).thenReturn("https://example.com");

        ThreeDSecureClient sut = new ThreeDSecureClient(activity, lifecycle, braintreeClient, cardinalClient, browserSwitchHelper, new ThreeDSecureAPI(braintreeClient));
        sut.setListener(listener);

        ThreeDSecureResult threeDSecureResult = ThreeDSecureResult.fromJson(Fixtures.THREE_D_SECURE_V1_LOOKUP_RESPONSE);
        sut.continuePerformVerification(activity, threeDSecureRequest, threeDSecureResult);

        verify(braintreeClient).sendAnalyticsEvent("three-d-secure.verification-flow.3ds-version.1.0.2");
    }

    @Test
    public void performVerification_whenVersion1IsRequested_doesNotUseCardinalMobileSDK() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .authorizationSuccess(Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
                .sendPOSTSuccessfulResponse(Fixtures.THREE_D_SECURE_V1_LOOKUP_RESPONSE)
                .configuration(threeDSecureEnabledConfig)
                .build();

        when(browserSwitchHelper.getUrl(anyString(), anyString(), any(ThreeDSecureRequest.class), any(ThreeDSecureLookup.class))).thenReturn("https://example.com");

        ThreeDSecureClient sut = new ThreeDSecureClient(activity, lifecycle, braintreeClient, cardinalClient, browserSwitchHelper, new ThreeDSecureAPI(braintreeClient));
        sut.setListener(listener);
        sut.performVerification(activity, threeDSecureRequest, mock(ThreeDSecureResultCallback.class));

        verify(cardinalClient, never()).initialize(any(Context.class), any(Configuration.class), any(ThreeDSecureRequest.class), any(CardinalInitializeCallback.class));
    }

    @Test
    public void continuePerformVerification_whenV1Flow_launchesBrowserSwitch() throws BrowserSwitchException {
        String urlScheme = "sample-scheme";
        String assetsUrl = "https://www.some-assets.com";

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .authorizationSuccess(Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
                .sendPOSTSuccessfulResponse(Fixtures.THREE_D_SECURE_V1_LOOKUP_RESPONSE)
                .configuration(threeDSecureEnabledConfig)
                .returnUrlScheme(urlScheme)
                .build();

        when(browserSwitchHelper.getUrl(urlScheme, assetsUrl, threeDSecureRequest, threeDSecureResult.getLookup()))
                .thenReturn("https://browser.switch.url.com");

        ThreeDSecureClient sut = new ThreeDSecureClient(activity, lifecycle, braintreeClient, cardinalClient, browserSwitchHelper, new ThreeDSecureAPI(braintreeClient));
        sut.setListener(listener);
        sut.continuePerformVerification(activity, threeDSecureRequest, threeDSecureResult);

        ArgumentCaptor<BrowserSwitchOptions> captor = ArgumentCaptor.forClass(BrowserSwitchOptions.class);
        verify(braintreeClient).startBrowserSwitch(same(activity), captor.capture());

        BrowserSwitchOptions browserSwitchOptions = captor.getValue();
        assertEquals(THREE_D_SECURE, browserSwitchOptions.getRequestCode());
        assertEquals(Uri.parse("https://browser.switch.url.com"), browserSwitchOptions.getUrl());
        assertFalse(browserSwitchOptions.isLaunchAsNewTask());
    }

    @Test
    public void continuePerformVerification_whenV1FlowAndDefaultDeepLinkHandlerEnabled_launchesBrowserSwitchAsNewTask() throws BrowserSwitchException {
        String urlScheme = "sample-scheme";
        String assetsUrl = "https://www.some-assets.com";

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .authorizationSuccess(Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
                .sendPOSTSuccessfulResponse(Fixtures.THREE_D_SECURE_V1_LOOKUP_RESPONSE)
                .configuration(threeDSecureEnabledConfig)
                .launchesBrowserSwitchAsNewTask(true)
                .returnUrlScheme(urlScheme)
                .build();

        when(browserSwitchHelper.getUrl(urlScheme, assetsUrl, threeDSecureRequest, threeDSecureResult.getLookup()))
                .thenReturn("https://browser.switch.url.com");

        ThreeDSecureClient sut = new ThreeDSecureClient(activity, lifecycle, braintreeClient, cardinalClient, browserSwitchHelper, new ThreeDSecureAPI(braintreeClient));
        sut.setListener(listener);
        sut.continuePerformVerification(activity, threeDSecureRequest, threeDSecureResult);

        ArgumentCaptor<BrowserSwitchOptions> captor = ArgumentCaptor.forClass(BrowserSwitchOptions.class);
        verify(braintreeClient).startBrowserSwitch(same(activity), captor.capture());

        BrowserSwitchOptions browserSwitchOptions = captor.getValue();
        assertTrue(browserSwitchOptions.isLaunchAsNewTask());
    }

    @Test
    public void continuePerformVerification_whenV1FlowCantStartBrowserSwitch_returnsError() throws BrowserSwitchException {
        String urlScheme = "sample-scheme";
        String assetsUrl = "https://www.some-assets.com";

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .authorizationSuccess(Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
                .sendPOSTSuccessfulResponse(Fixtures.THREE_D_SECURE_V1_LOOKUP_RESPONSE)
                .configuration(threeDSecureEnabledConfig)
                .returnUrlScheme(urlScheme)
                .build();
        BrowserSwitchException expectedError = new BrowserSwitchException("error");

        doThrow(expectedError).when(braintreeClient).startBrowserSwitch(same(activity), any(BrowserSwitchOptions.class));

        when(browserSwitchHelper.getUrl(urlScheme, assetsUrl, threeDSecureRequest, threeDSecureResult.getLookup()))
                .thenReturn("https://browser.switch.url.com");

        ThreeDSecureClient sut = new ThreeDSecureClient(activity, lifecycle, braintreeClient, cardinalClient, browserSwitchHelper, new ThreeDSecureAPI(braintreeClient));
        sut.setListener(listener);
        sut.continuePerformVerification(activity, threeDSecureRequest, threeDSecureResult);

        verify(listener).onThreeDSecureFailure(expectedError);
    }

    @Test
    public void initializeChallengeWithLookupResponse_whenV1Flow_launchesBrowserSwitch() throws BrowserSwitchException {
        String urlScheme = "sample-scheme";
        String assetsUrl = "https://www.some-assets.com";

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .authorizationSuccess(Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
                .sendPOSTSuccessfulResponse(Fixtures.THREE_D_SECURE_V1_LOOKUP_RESPONSE)
                .configuration(threeDSecureEnabledConfig)
                .returnUrlScheme(urlScheme)
                .build();

        when(browserSwitchHelper.getUrl(eq(urlScheme), eq(assetsUrl), isNull(ThreeDSecureRequest.class), any(ThreeDSecureLookup.class)))
                .thenReturn("https://browser.switch.url.com");

        ThreeDSecureClient sut = new ThreeDSecureClient(activity, lifecycle, braintreeClient, cardinalClient, browserSwitchHelper, new ThreeDSecureAPI(braintreeClient));
        sut.setListener(listener);
        sut.initializeChallengeWithLookupResponse(activity, threeDSecureLookupResponse);

        ArgumentCaptor<BrowserSwitchOptions> captor = ArgumentCaptor.forClass(BrowserSwitchOptions.class);
        verify(braintreeClient).startBrowserSwitch(same(activity), captor.capture());

        BrowserSwitchOptions browserSwitchOptions = captor.getValue();
        assertEquals(THREE_D_SECURE, browserSwitchOptions.getRequestCode());
        assertEquals(Uri.parse("https://browser.switch.url.com"), browserSwitchOptions.getUrl());
    }

    @Test
    public void initializeChallengeWithLookupResponse_whenV1Flow_and3DSecureRequestIsProvided_launchesBrowserSwitch() throws BrowserSwitchException {
        String urlScheme = "sample-scheme";
        String assetsUrl = "https://www.some-assets.com";

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .authorizationSuccess(Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
                .sendPOSTSuccessfulResponse(Fixtures.THREE_D_SECURE_V1_LOOKUP_RESPONSE)
                .configuration(threeDSecureEnabledConfig)
                .returnUrlScheme(urlScheme)
                .build();

        when(browserSwitchHelper.getUrl(eq(urlScheme), eq(assetsUrl), eq(threeDSecureRequest), any(ThreeDSecureLookup.class)))
                .thenReturn("https://browser.switch.url.com");

        ThreeDSecureClient sut = new ThreeDSecureClient(activity, lifecycle, braintreeClient, cardinalClient, browserSwitchHelper, new ThreeDSecureAPI(braintreeClient));
        sut.setListener(listener);
        sut.initializeChallengeWithLookupResponse(activity, threeDSecureRequest, threeDSecureLookupResponse);

        ArgumentCaptor<BrowserSwitchOptions> captor = ArgumentCaptor.forClass(BrowserSwitchOptions.class);
        verify(braintreeClient).startBrowserSwitch(same(activity), captor.capture());

        BrowserSwitchOptions browserSwitchOptions = captor.getValue();
        assertEquals(THREE_D_SECURE, browserSwitchOptions.getRequestCode());
        assertEquals(Uri.parse("https://browser.switch.url.com"), browserSwitchOptions.getUrl());
    }
}
