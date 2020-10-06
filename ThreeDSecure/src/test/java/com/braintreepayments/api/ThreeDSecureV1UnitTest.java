package com.braintreepayments.api;

import com.braintreepayments.api.internal.ManifestValidator;
import com.braintreepayments.api.internal.ThreeDSecureV1BrowserSwitchHelper;
import com.braintreepayments.api.models.Authorization;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.ThreeDSecureLookup;
import com.braintreepayments.api.models.ThreeDSecurePostalAddress;
import com.braintreepayments.api.models.ThreeDSecureRequest;
import com.braintreepayments.testutils.TestConfigurationBuilder;
import com.cardinalcommerce.cardinalmobilesdk.Cardinal;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;

import static com.braintreepayments.api.BraintreePowerMockHelper.MockManifestValidator;
import static com.braintreepayments.api.models.BraintreeRequestCodes.THREE_D_SECURE;
import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(RobolectricTestRunner.class)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*", "org.json.*", "javax.crypto.*" })
@PrepareForTest({ ManifestValidator.class, TokenizationClient.class, Cardinal.class, ThreeDSecureV1BrowserSwitchHelper.class })
public class ThreeDSecureV1UnitTest {

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    private MockFragmentBuilder mMockFragmentBuilder;
    private BraintreeFragment mMockFragment;

    private ThreeDSecureRequest mThreeDSecureRequest;
    private ThreeDSecureLookup mThreeDSecureLookup;
    private String mThreeDSecureLookupResponse;

    @Before
    public void setup() throws Exception {
        MockManifestValidator.mockUrlSchemeDeclaredInAndroidManifest(true);

        Configuration configuration = new TestConfigurationBuilder()
                .threeDSecureEnabled(true)
                .assetsUrl("https://www.some-assets.com")
                .buildConfiguration();

        mMockFragmentBuilder = new MockFragmentBuilder()
                .authorization(Authorization.fromString(stringFromFixture("base_64_client_token.txt")))
                .configuration(configuration);
        mMockFragment = mMockFragmentBuilder.build();

        mThreeDSecureRequest = new ThreeDSecureRequest()
                .nonce("a-nonce")
                .amount("amount")
                .billingAddress(new ThreeDSecurePostalAddress()
                        .givenName("billing-given-name"));

        mThreeDSecureLookupResponse = stringFromFixture("three_d_secure/lookup_response_with_version_number1.json");
        mThreeDSecureLookup = ThreeDSecureLookup.fromJson(mThreeDSecureLookupResponse);
    }

    @Test
    public void performVerification_sendsAnalyticsEvent() {
        mMockFragmentBuilder.successResponse(stringFromFixture("three_d_secure/lookup_response_with_version_number1.json"));
        mMockFragment = mMockFragmentBuilder.build();

        ThreeDSecure.performVerification(mMockFragment, mThreeDSecureRequest);

        verify(mMockFragment).sendAnalyticsEvent(eq("three-d-secure.verification-flow.3ds-version.1.0.2"));
    }

    @Test
    public void performVerification_whenVersion1IsRequested_doesNotUseCardinalMobileSDK() {
        ThreeDSecure.performVerification(mMockFragment, mThreeDSecureRequest);

        mockStatic(Cardinal.class);
        verifyStatic(times(0));
        //noinspection ResultOfMethodCallIgnored
        Cardinal.getInstance();
    }

    @Test
    public void continuePerformVerification_whenV1Flow_launchesBrowserSwitch() {
        mockStatic(ThreeDSecureV1BrowserSwitchHelper.class);

        String urlScheme = "com.braintreepayments.api.braintree";
        String assetsUrl = "https://www.some-assets.com";

        when(ThreeDSecureV1BrowserSwitchHelper.getUrl(urlScheme, assetsUrl, mThreeDSecureRequest, mThreeDSecureLookup))
                .thenReturn("https://browser.switch.url.com");

        ThreeDSecure.continuePerformVerification(mMockFragment, mThreeDSecureRequest, mThreeDSecureLookup);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mMockFragment).browserSwitch(eq(THREE_D_SECURE), captor.capture());

        assertEquals("https://browser.switch.url.com", captor.getValue());
    }

    @Test
    public void initializeChallengeWithLookupResponse_whenV1Flow_launchesBrowserSwitch() {
        mockStatic(ThreeDSecureV1BrowserSwitchHelper.class);

        String urlScheme = "com.braintreepayments.api.braintree";
        String assetsUrl = "https://www.some-assets.com";

        when(ThreeDSecureV1BrowserSwitchHelper.getUrl(eq(urlScheme), eq(assetsUrl), isNull(ThreeDSecureRequest.class), any(ThreeDSecureLookup.class)))
                .thenReturn("https://browser.switch.url.com");

        ThreeDSecure.initializeChallengeWithLookupResponse(mMockFragment, mThreeDSecureLookupResponse);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mMockFragment).browserSwitch(eq(THREE_D_SECURE), captor.capture());

        assertEquals("https://browser.switch.url.com", captor.getValue());
    }

    @Test
    public void initializeChallengeWithLookupResponse_whenV1Flow_and3DSecureRequestIsProvided_launchesBrowserSwitch() {
        mockStatic(ThreeDSecureV1BrowserSwitchHelper.class);

        String urlScheme = "com.braintreepayments.api.braintree";
        String assetsUrl = "https://www.some-assets.com";

        when(ThreeDSecureV1BrowserSwitchHelper.getUrl(eq(urlScheme), eq(assetsUrl), eq(mThreeDSecureRequest), any(ThreeDSecureLookup.class)))
                .thenReturn("https://browser.switch.url.com");

        ThreeDSecure.initializeChallengeWithLookupResponse(mMockFragment, mThreeDSecureRequest, mThreeDSecureLookupResponse);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mMockFragment).browserSwitch(eq(THREE_D_SECURE), captor.capture());

        assertEquals("https://browser.switch.url.com", captor.getValue());
    }
}
