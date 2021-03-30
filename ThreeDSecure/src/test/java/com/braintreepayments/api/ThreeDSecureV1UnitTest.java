package com.braintreepayments.api;

import android.content.Context;
import android.net.Uri;

import androidx.fragment.app.FragmentActivity;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.robolectric.RobolectricTestRunner;

import static com.braintreepayments.api.BraintreeRequestCodes.THREE_D_SECURE;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(RobolectricTestRunner.class)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*", "org.json.*", "javax.crypto.*"})
public class ThreeDSecureV1UnitTest {

    private FragmentActivity activity;
    private CardinalClient cardinalClient;
    private ThreeDSecureV1BrowserSwitchHelper browserSwitchHelper;

    private ThreeDSecureRequest mThreeDSecureRequest;
    private ThreeDSecureResult mThreeDSecureResult;
    private String mThreeDSecureLookupResponse;

    private Configuration threeDSecureEnabledConfig;

    @Before
    public void setup() throws JSONException {
        activity = mock(FragmentActivity.class);
        cardinalClient = mock(CardinalClient.class);
        browserSwitchHelper = mock(ThreeDSecureV1BrowserSwitchHelper.class);

        threeDSecureEnabledConfig = new TestConfigurationBuilder()
                .threeDSecureEnabled(true)
                .assetsUrl("https://www.some-assets.com")
                .buildConfiguration();

        mThreeDSecureRequest = new ThreeDSecureRequest();
        mThreeDSecureRequest.setVersionRequested(ThreeDSecureRequest.VERSION_1);
        mThreeDSecureRequest.setNonce("a-nonce");
        mThreeDSecureRequest.setAmount("amount");

        ThreeDSecurePostalAddress billingAddress = new ThreeDSecurePostalAddress();
        billingAddress.setGivenName("billing-given-name");
        mThreeDSecureRequest.setBillingAddress(billingAddress);

        mThreeDSecureLookupResponse = Fixtures.THREE_D_SECURE_V1_LOOKUP_RESPONSE;
        mThreeDSecureResult = ThreeDSecureResult.fromJson(mThreeDSecureLookupResponse);
    }

    @Test
    public void continuePerformVerification_sendsAnalyticsEvent() throws InvalidArgumentException, JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .authorization(Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
                .configuration(threeDSecureEnabledConfig)
                .build();

        when(braintreeClient.canPerformBrowserSwitch(activity, THREE_D_SECURE)).thenReturn(true);
        when(browserSwitchHelper.getUrl(anyString(), anyString(), any(ThreeDSecureRequest.class), any(ThreeDSecureLookup.class))).thenReturn("https://example.com");

        ThreeDSecureClient sut = new ThreeDSecureClient(braintreeClient, cardinalClient, browserSwitchHelper);

        ThreeDSecureResult threeDSecureResult = ThreeDSecureResult.fromJson(Fixtures.THREE_D_SECURE_V1_LOOKUP_RESPONSE);
        sut.continuePerformVerification(activity, mThreeDSecureRequest, threeDSecureResult, mock(ThreeDSecureResultCallback.class));

        verify(braintreeClient).sendAnalyticsEvent("three-d-secure.verification-flow.3ds-version.1.0.2");
    }

    @Test
    public void performVerification_whenVersion1IsRequested_doesNotUseCardinalMobileSDK() throws InvalidArgumentException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .authorization(Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
                .sendPOSTSuccessfulResponse(Fixtures.THREE_D_SECURE_V1_LOOKUP_RESPONSE)
                .configuration(threeDSecureEnabledConfig)
                .build();

        when(braintreeClient.canPerformBrowserSwitch(activity, THREE_D_SECURE)).thenReturn(true);
        when(browserSwitchHelper.getUrl(anyString(), anyString(), any(ThreeDSecureRequest.class), any(ThreeDSecureLookup.class))).thenReturn("https://example.com");

        ThreeDSecureClient sut = new ThreeDSecureClient(braintreeClient, cardinalClient, browserSwitchHelper);
        sut.performVerification(activity, mThreeDSecureRequest, mock(ThreeDSecureResultCallback.class));

        verify(cardinalClient, never()).initialize(any(Context.class), any(Configuration.class), any(ThreeDSecureRequest.class), any(CardinalInitializeCallback.class));
    }

    @Test
    public void continuePerformVerification_whenV1Flow_launchesBrowserSwitch() throws InvalidArgumentException, BrowserSwitchException {
        String urlScheme = "sample-scheme";
        String assetsUrl = "https://www.some-assets.com";

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .authorization(Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
                .sendPOSTSuccessfulResponse(Fixtures.THREE_D_SECURE_V1_LOOKUP_RESPONSE)
                .configuration(threeDSecureEnabledConfig)
                .returnUrlScheme(urlScheme)
                .build();

        when(browserSwitchHelper.getUrl(urlScheme, assetsUrl, mThreeDSecureRequest, mThreeDSecureResult.getLookup()))
                .thenReturn("https://browser.switch.url.com");

        when(braintreeClient.canPerformBrowserSwitch(activity, THREE_D_SECURE)).thenReturn(true);

        ThreeDSecureClient sut = new ThreeDSecureClient(braintreeClient, cardinalClient, browserSwitchHelper);
        sut.continuePerformVerification(activity, mThreeDSecureRequest, mThreeDSecureResult, mock(ThreeDSecureResultCallback.class));

        ArgumentCaptor<BrowserSwitchOptions> captor = ArgumentCaptor.forClass(BrowserSwitchOptions.class);
        verify(braintreeClient).startBrowserSwitch(same(activity), captor.capture());

        BrowserSwitchOptions browserSwitchOptions = captor.getValue();
        assertEquals(THREE_D_SECURE, browserSwitchOptions.getRequestCode());
        assertEquals(Uri.parse("https://browser.switch.url.com"), browserSwitchOptions.getUrl());
    }

    @Test
    public void initializeChallengeWithLookupResponse_whenV1Flow_launchesBrowserSwitch() throws InvalidArgumentException, BrowserSwitchException {
        String urlScheme = "sample-scheme";
        String assetsUrl = "https://www.some-assets.com";

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .authorization(Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
                .sendPOSTSuccessfulResponse(Fixtures.THREE_D_SECURE_V1_LOOKUP_RESPONSE)
                .configuration(threeDSecureEnabledConfig)
                .returnUrlScheme(urlScheme)
                .build();

        when(braintreeClient.canPerformBrowserSwitch(activity, THREE_D_SECURE)).thenReturn(true);

        when(browserSwitchHelper.getUrl(eq(urlScheme), eq(assetsUrl), isNull(ThreeDSecureRequest.class), any(ThreeDSecureLookup.class)))
                .thenReturn("https://browser.switch.url.com");

        ThreeDSecureClient sut = new ThreeDSecureClient(braintreeClient, cardinalClient, browserSwitchHelper);
        sut.initializeChallengeWithLookupResponse(activity, mThreeDSecureLookupResponse, mock(ThreeDSecureResultCallback.class));

        ArgumentCaptor<BrowserSwitchOptions> captor = ArgumentCaptor.forClass(BrowserSwitchOptions.class);
        verify(braintreeClient).startBrowserSwitch(same(activity), captor.capture());

        BrowserSwitchOptions browserSwitchOptions = captor.getValue();
        assertEquals(THREE_D_SECURE, browserSwitchOptions.getRequestCode());
        assertEquals(Uri.parse("https://browser.switch.url.com"), browserSwitchOptions.getUrl());
    }

    @Test
    public void initializeChallengeWithLookupResponse_whenV1Flow_and3DSecureRequestIsProvided_launchesBrowserSwitch() throws InvalidArgumentException, BrowserSwitchException {
        String urlScheme = "sample-scheme";
        String assetsUrl = "https://www.some-assets.com";

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .authorization(Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
                .sendPOSTSuccessfulResponse(Fixtures.THREE_D_SECURE_V1_LOOKUP_RESPONSE)
                .configuration(threeDSecureEnabledConfig)
                .returnUrlScheme(urlScheme)
                .build();

        when(braintreeClient.canPerformBrowserSwitch(activity, THREE_D_SECURE)).thenReturn(true);

        when(browserSwitchHelper.getUrl(eq(urlScheme), eq(assetsUrl), eq(mThreeDSecureRequest), any(ThreeDSecureLookup.class)))
                .thenReturn("https://browser.switch.url.com");

        ThreeDSecureClient sut = new ThreeDSecureClient(braintreeClient, cardinalClient, browserSwitchHelper);
        sut.initializeChallengeWithLookupResponse(activity, mThreeDSecureRequest, mThreeDSecureLookupResponse, mock(ThreeDSecureResultCallback.class));

        ArgumentCaptor<BrowserSwitchOptions> captor = ArgumentCaptor.forClass(BrowserSwitchOptions.class);
        verify(braintreeClient).startBrowserSwitch(same(activity), captor.capture());

        BrowserSwitchOptions browserSwitchOptions = captor.getValue();
        assertEquals(THREE_D_SECURE, browserSwitchOptions.getRequestCode());
        assertEquals(Uri.parse("https://browser.switch.url.com"), browserSwitchOptions.getUrl());
    }
}
