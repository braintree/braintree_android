package com.braintreepayments.api;

import com.braintreepayments.api.internal.ManifestValidator;
import com.braintreepayments.api.models.Authorization;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.ThreeDSecurePostalAddress;
import com.braintreepayments.api.models.ThreeDSecureRequest;
import com.braintreepayments.testutils.TestConfigurationBuilder;
import com.cardinalcommerce.cardinalmobilesdk.Cardinal;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;

import static com.braintreepayments.api.BraintreePowerMockHelper.MockManifestValidator;
import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(RobolectricTestRunner.class)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*", "org.json.*", "javax.crypto.*" })
@PrepareForTest({ ManifestValidator.class, TokenizationClient.class, Cardinal.class })
public class ThreeDSecureV1UnitTest {

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    private MockFragmentBuilder mMockFragmentBuilder;
    private BraintreeFragment mFragment;
    private ThreeDSecureRequest mBasicRequest;

    @Before
    public void setup() throws Exception {
        MockManifestValidator.mockUrlSchemeDeclaredInAndroidManifest(true);

        Configuration configuration = new TestConfigurationBuilder()
                .threeDSecureEnabled(true)
                .buildConfiguration();

        mMockFragmentBuilder = new MockFragmentBuilder()
                .authorization(Authorization.fromString(stringFromFixture("base_64_client_token.txt")))
                .configuration(configuration);
        mFragment = mMockFragmentBuilder.build();

        mBasicRequest = new ThreeDSecureRequest()
                .nonce("a-nonce")
                .amount("amount")
                .billingAddress(new ThreeDSecurePostalAddress()
                        .givenName("billing-given-name"));
    }

    @Test
    public void performVerification_sendsAnalyticsEvent() {
        mMockFragmentBuilder.successResponse(stringFromFixture("three_d_secure/lookup_response_with_version_number1.json"));
        mFragment = mMockFragmentBuilder.build();

        ThreeDSecure.performVerification(mFragment, mBasicRequest);

        verify(mFragment).sendAnalyticsEvent(eq("three-d-secure.verification-flow.3ds-version.1.0.2"));
    }

    @Test
    public void performVerification_whenVersion1IsRequested_doesNotUseCardinalMobileSDK() {
        ThreeDSecure.performVerification(mFragment, mBasicRequest);

        mockStatic(Cardinal.class);
        verifyStatic(times(0));
        //noinspection ResultOfMethodCallIgnored
        Cardinal.getInstance();
    }
}
