package com.braintreepayments.api;

import android.content.Intent;

import com.braintreepayments.api.exceptions.BraintreeException;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.ThreeDSecurePrepareLookupListener;
import com.braintreepayments.api.internal.ManifestValidator;
import com.braintreepayments.api.models.Authorization;
import com.braintreepayments.api.models.CardBuilder;
import com.braintreepayments.api.models.CardNonce;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.ThreeDSecureLookup;
import com.braintreepayments.api.models.ThreeDSecureRequest;
import com.braintreepayments.testutils.TestConfigurationBuilder;
import com.cardinalcommerce.cardinalmobilesdk.Cardinal;
import com.cardinalcommerce.cardinalmobilesdk.enums.CardinalEnvironment;
import com.cardinalcommerce.cardinalmobilesdk.models.CardinalActionCode;
import com.cardinalcommerce.cardinalmobilesdk.models.CardinalConfigurationParameters;
import com.cardinalcommerce.cardinalmobilesdk.models.ValidateResponse;
import com.cardinalcommerce.cardinalmobilesdk.services.CardinalInitService;
import com.cardinalcommerce.shared.userinterfaces.TextBoxCustomization;
import com.cardinalcommerce.shared.userinterfaces.UiCustomization;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;

import static android.app.Activity.RESULT_OK;
import static com.braintreepayments.api.BraintreePowerMockHelper.MockManifestValidator.mockUrlSchemeDeclaredInAndroidManifest;
import static com.braintreepayments.api.BraintreePowerMockHelper.MockStaticCardinal;
import static com.braintreepayments.api.BraintreePowerMockHelper.MockStaticTokenizationClient;
import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(RobolectricTestRunner.class)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*", "org.json.*", "javax.crypto.*"})
@PrepareForTest({Cardinal.class, ManifestValidator.class, TokenizationClient.class})
public class ThreeDSecureV2UnitTest {
    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    private MockFragmentBuilder mMockFragmentBuilder;
    private BraintreeFragment mFragment;
    private ThreeDSecureRequest mBasicRequest;

    @Before
    public void setup() throws Exception {
        mockUrlSchemeDeclaredInAndroidManifest(true);

        Configuration configuration = new TestConfigurationBuilder()
                .threeDSecureEnabled(true)
                .cardinalAuthenticationJWT("cardinal_authentication_jwt")
                .buildConfiguration();

        mMockFragmentBuilder = new MockFragmentBuilder()
                .authorization(Authorization.fromString(stringFromFixture("base_64_client_token.txt")))
                .configuration(configuration);
        mFragment = mMockFragmentBuilder.build();

        TextBoxCustomization textBoxCustomization = new TextBoxCustomization();
        textBoxCustomization.setBorderWidth(12);

        UiCustomization uiCustomization = new UiCustomization();
        uiCustomization.setTextBoxCustomization(textBoxCustomization);

        mBasicRequest = new ThreeDSecureRequest()
                .nonce("a-nonce")
                .amount("1.00")
                .versionRequested(ThreeDSecureRequest.VERSION_2)
                .uiCustomization(uiCustomization);
    }

    @Test
    public void prepareLookup_returnsValidLookupJSONString() {
        MockStaticCardinal.initCompletesSuccessfully("fake-df");

        ThreeDSecure.prepareLookup(mFragment, mBasicRequest, new ThreeDSecurePrepareLookupListener() {
            @Override
            public void onPrepareLookupComplete(ThreeDSecureRequest request, String clientData) {
                try {
                    JSONObject lookup = new JSONObject(clientData);
                    assertEquals(lookup.getString("authorizationFingerprint"),
                            mFragment.getAuthorization().getBearer());
                    assertEquals(lookup.getString("braintreeLibraryVersion"), "Android-" + BuildConfig.VERSION_NAME);
                    assertEquals(lookup.getString("dfReferenceId"), "fake-df");
                    assertEquals(lookup.getString("nonce"), "a-nonce");

                    JSONObject clientMetaData = lookup.getJSONObject("clientMetadata");
                    assertEquals(clientMetaData.getString("requestedThreeDSecureVersion"), "2");
                    assertEquals(clientMetaData.getString("sdkVersion"), BuildConfig.VERSION_NAME);
                } catch (JSONException e) {
                    fail();
                }
            }
        });
    }

    @Test
    public void prepareLookup_returnsValidLookupJSONString_whenCardinalSetupFails() throws JSONException {
        MockStaticCardinal.initCallsOnValidated();

        ThreeDSecure.prepareLookup(mFragment, mBasicRequest, new ThreeDSecurePrepareLookupListener() {
            @Override
            public void onPrepareLookupComplete(ThreeDSecureRequest request, String clientData) {
                try {
                    JSONObject lookup = new JSONObject(clientData);
                    assertEquals(lookup.getString("authorizationFingerprint"),
                            mFragment.getAuthorization().getBearer());
                    assertEquals(lookup.getString("braintreeLibraryVersion"), "Android-" + BuildConfig.VERSION_NAME);
                    assertEquals(lookup.getString("nonce"), "a-nonce");
                    assertNull(lookup.optString("dfReferenceId", null));

                    JSONObject clientMetaData = lookup.getJSONObject("clientMetadata");
                    assertEquals(clientMetaData.getString("requestedThreeDSecureVersion"), "2");
                    assertEquals(clientMetaData.getString("sdkVersion"), BuildConfig.VERSION_NAME);
                } catch (JSONException e) {}
            }
        });
    }

    @Test
    public void prepareLookup_setsCardinalConfigurationParameters() {
        MockStaticCardinal.initCompletesSuccessfully("fake-df");

        ThreeDSecure.prepareLookup(mFragment, mBasicRequest, new ThreeDSecurePrepareLookupListener() {
            @Override
            public void onPrepareLookupComplete(ThreeDSecureRequest request, String clientData) {
                ArgumentCaptor<CardinalConfigurationParameters> captor = ArgumentCaptor.forClass(CardinalConfigurationParameters.class);
                verify(Cardinal.getInstance()).configure(eq(mFragment.getApplicationContext()), captor.capture());

                CardinalConfigurationParameters actualParams = captor.getValue();
                assertEquals(actualParams.getTimeout(), 8000);
                assertFalse(actualParams.isEnableQuickAuth());
                assertTrue(actualParams.isEnableDFSync());
                assertEquals(actualParams.getUICustomization().getTextBoxCustomization().getBorderWidth(), 12);
            }
        });
    }

    @Test
    public void prepareLookup_initializesCardinal() {
        MockStaticCardinal.initCompletesSuccessfully("fake-df");

        ThreeDSecure.prepareLookup(mFragment, mBasicRequest, new ThreeDSecurePrepareLookupListener() {
            @Override
            public void onPrepareLookupComplete(ThreeDSecureRequest request, String clientData) {

            }
        });

        verify(Cardinal.getInstance()).init(eq("cardinal_authentication_jwt"), any(CardinalInitService.class));
    }

    @Test
    public void prepareLookup_withoutCardinalJWT_postsException() throws Exception {
        Configuration configuration = new TestConfigurationBuilder()
                .threeDSecureEnabled(true)
                .buildConfiguration();

        MockFragmentBuilder mockFragmentBuilder = new MockFragmentBuilder()
                .authorization(Authorization.fromString(stringFromFixture("base_64_client_token.txt")))
                .configuration(configuration);
        BraintreeFragment fragment = mockFragmentBuilder.build();

        ThreeDSecure.prepareLookup(fragment, mBasicRequest, new ThreeDSecurePrepareLookupListener() {
            @Override
            public void onPrepareLookupComplete(ThreeDSecureRequest request, String clientData) {

            }
        });

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(fragment).postCallback(captor.capture());
        assertTrue(captor.getValue() instanceof BraintreeException);
        assertEquals(captor.getValue().getMessage(), "Merchant is not configured for 3DS 2.0. " +
                "Please contact Braintree Support for assistance.");
    }

    @Test
    public void performVerification_withCardBuilder_tokenizesAndPerformsVerification() {
        CardNonce cardNonce = mock(CardNonce.class);
        when(cardNonce.getNonce()).thenReturn("card-nonce");
        MockStaticTokenizationClient.mockTokenizeSuccess(cardNonce);

        MockStaticCardinal.initCompletesSuccessfully("fake-df");

        CardBuilder cardBuilder = new CardBuilder();
        ThreeDSecureRequest request = new ThreeDSecureRequest()
                .amount("10");

        ThreeDSecure.performVerification(mFragment, cardBuilder, request);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);

        verifyStatic();
        //noinspection ResultOfMethodCallIgnored
        TokenizationClient.versionedPath(captor.capture());

        assertTrue(captor.getValue().contains("card-nonce"));
    }

    @Test
    public void performVerification_setsCardinalConfigurationParameters() {
        MockStaticCardinal.initCompletesSuccessfully("df-reference-id");

        ThreeDSecure.performVerification(mFragment, mBasicRequest);

        ArgumentCaptor<CardinalConfigurationParameters> captor = ArgumentCaptor.forClass(CardinalConfigurationParameters.class);
        verify(Cardinal.getInstance()).configure(eq(mFragment.getApplicationContext()), captor.capture());

        CardinalConfigurationParameters actualParams = captor.getValue();
        assertEquals(actualParams.getTimeout(), 8000);
        assertFalse(actualParams.isEnableQuickAuth());
        assertTrue(actualParams.isEnableDFSync());
        assertEquals(actualParams.getUICustomization().getTextBoxCustomization().getBorderWidth(), 12);
    }

    @Test
    public void performVerification_whenEnvironmentIsNonProd_setsCardinalConfigurationEnvironmentParameter() {
        MockStaticCardinal.initCompletesSuccessfully("df-reference-id");

        ThreeDSecure.performVerification(mFragment, mBasicRequest);

        ArgumentCaptor<CardinalConfigurationParameters> captor = ArgumentCaptor.forClass(CardinalConfigurationParameters.class);
        verify(Cardinal.getInstance()).configure(eq(mFragment.getApplicationContext()), captor.capture());

        CardinalConfigurationParameters actualParams = captor.getValue();
        assertEquals(actualParams.getEnvironment(), CardinalEnvironment.STAGING);
    }

    @Test
    public void performVerification_whenEnvironmentIsProd_setsCardinalConfigurationEnvironmentParameter() throws InvalidArgumentException {
        MockStaticCardinal.initCompletesSuccessfully("df-reference-id");

        Configuration prodConfig = new TestConfigurationBuilder()
                .threeDSecureEnabled(true)
                .cardinalAuthenticationJWT("cardinal-jwt")
                .environment("production")
                .buildConfiguration();

        mFragment = new MockFragmentBuilder()
                .authorization(Authorization.fromString(stringFromFixture("base_64_client_token.txt")))
                .configuration(prodConfig)
                .build();

        ThreeDSecure.performVerification(mFragment, mBasicRequest);

        ArgumentCaptor<CardinalConfigurationParameters> captor = ArgumentCaptor.forClass(CardinalConfigurationParameters.class);
        verify(Cardinal.getInstance()).configure(eq(mFragment.getApplicationContext()), captor.capture());

        CardinalConfigurationParameters actualParams = captor.getValue();
        assertEquals(actualParams.getEnvironment(), CardinalEnvironment.PRODUCTION);
    }

    @Test
    public void performVerification_initializesCardinal() {
        MockStaticCardinal.initCompletesSuccessfully("df-reference-id");

        ThreeDSecure.performVerification(mFragment, mBasicRequest);

        verify(Cardinal.getInstance()).init(eq("cardinal_authentication_jwt"), any(CardinalInitService.class));
    }

    @Test
    public void performVerification_whenCardinalSetupCompleted_sendsAnalyticEvent() {
        MockStaticCardinal.initCompletesSuccessfully("df-reference-id");

        ThreeDSecure.performVerification(mFragment, mBasicRequest);

        verify(mFragment).sendAnalyticsEvent(eq("three-d-secure.cardinal-sdk.init.setup-completed"));
    }

    @Test
    public void performVerification_whenCardinalSetupFailed_sendsAnalyticEvent() {
        MockStaticCardinal.initCallsOnValidated();

        ThreeDSecure.performVerification(mFragment, mBasicRequest);

        verify(mFragment).sendAnalyticsEvent(eq("three-d-secure.cardinal-sdk.init.setup-failed"));
    }

    @Test
    public void performVerification_whenAuthenticatingWithCardinal_sendsAnalyticsEvent() {
        MockStaticCardinal.initCompletesSuccessfully("reference-id");

        mMockFragmentBuilder.successResponse(stringFromFixture("three_d_secure/lookup_response_with_version_number2.json"));
        mFragment = mMockFragmentBuilder.build();

        ThreeDSecure.performVerification(mFragment, mBasicRequest);

        verify(mFragment).sendAnalyticsEvent(eq("three-d-secure.verification-flow.started"));
    }

    @Test
    public void performVerification_whenChallengeIsPresented_sendsAnalyticsEvent() {
        MockStaticCardinal.initCompletesSuccessfully("reference-id");

        mMockFragmentBuilder.successResponse(stringFromFixture("three_d_secure/lookup_response.json"));
        mFragment = mMockFragmentBuilder.build();

        ThreeDSecure.performVerification(mFragment, mBasicRequest);

        verify(mFragment).sendAnalyticsEvent(eq("three-d-secure.verification-flow.challenge-presented.true"));
    }

    @Test
    public void performVerification_whenChallengeIsNotPresented_sendsAnalyticsEvent() {
        MockStaticCardinal.initCompletesSuccessfully("reference-id");

        mMockFragmentBuilder.successResponse(stringFromFixture("three_d_secure/lookup_response_noAcsUrl.json"));
        mFragment = mMockFragmentBuilder.build();

        ThreeDSecure.performVerification(mFragment, mBasicRequest);

        verify(mFragment).sendAnalyticsEvent(eq("three-d-secure.verification-flow.challenge-presented.false"));
    }

    @Test
    public void performVerification_when3DSVersionIsVersion2_sendsAnalyticsEvent() {
        MockStaticCardinal.initCompletesSuccessfully("reference-id");

        mMockFragmentBuilder.successResponse(stringFromFixture("three_d_secure/lookup_response_with_version_number2.json"));
        mFragment = mMockFragmentBuilder.build();

        ThreeDSecure.performVerification(mFragment, mBasicRequest);

        verify(mFragment).sendAnalyticsEvent(eq("three-d-secure.verification-flow.3ds-version.2.1.0"));
    }

    @Test
    public void performVerification_withoutCardinalJWT_postsException() throws Exception {
        Configuration configuration = new TestConfigurationBuilder()
                .threeDSecureEnabled(true)
                .buildConfiguration();

        MockFragmentBuilder mockFragmentBuilder = new MockFragmentBuilder()
                .authorization(Authorization.fromString(stringFromFixture("base_64_client_token.txt")))
                .configuration(configuration);
        BraintreeFragment fragment = mockFragmentBuilder.build();

        ThreeDSecure.performVerification(fragment, mBasicRequest);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(fragment).postCallback(captor.capture());
        assertTrue(captor.getValue() instanceof BraintreeException);
        assertEquals(captor.getValue().getMessage(), "Merchant is not configured for 3DS 2.0. " +
                "Please contact Braintree Support for assistance.");
    }

    @Test
    public void initializeChallengeWithLookupResponse_postsExceptionForBadJSON() {
        BraintreeFragment fragment = mMockFragmentBuilder.build();

        ThreeDSecure.initializeChallengeWithLookupResponse(fragment, "{bad:}");

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(fragment).postCallback(captor.capture());
        assertTrue(captor.getValue() instanceof JSONException);
    }

    @Test
    public void authenticateCardinalJWT_whenSuccess_sendsAnalyticsEvent() throws JSONException {
        ThreeDSecureLookup threeDSecureLookup = ThreeDSecureLookup.fromJson(stringFromFixture("three_d_secure/lookup_response_with_version_number2.json"));

        mMockFragmentBuilder.successResponse(stringFromFixture("three_d_secure/authentication_response.json"));
        mFragment = mMockFragmentBuilder.build();

        ThreeDSecure.authenticateCardinalJWT(mFragment, threeDSecureLookup, "jwt");

        verify(mFragment).sendAnalyticsEvent(eq("three-d-secure.verification-flow.upgrade-payment-method.started"));
        verify(mFragment).sendAnalyticsEvent(eq("three-d-secure.verification-flow.upgrade-payment-method.succeeded"));
    }

    @Test
    public void authenticateCardinalJWT_whenSuccess_returnsThreeDSecureCardNonce() throws JSONException {
        ThreeDSecureLookup threeDSecureLookup = ThreeDSecureLookup.fromJson(stringFromFixture("three_d_secure/lookup_response_with_version_number2.json"));

        String authResponseJson = stringFromFixture("three_d_secure/authentication_response.json");
        mMockFragmentBuilder.successResponse(authResponseJson);
        mFragment = mMockFragmentBuilder.build();

        ThreeDSecure.authenticateCardinalJWT(mFragment, threeDSecureLookup, "jwt");

        ArgumentCaptor<CardNonce> captor = ArgumentCaptor.forClass(CardNonce.class);
        verify(mFragment).postCallback(captor.capture());

        CardNonce cardNonce = captor.getValue();

        assertTrue(cardNonce.getThreeDSecureInfo().isLiabilityShifted());
        assertTrue(cardNonce.getThreeDSecureInfo().isLiabilityShiftPossible());
        assertEquals("12345678-1234-1234-1234-123456789012", cardNonce.getNonce());
    }

    @Test
    public void authenticateCardinalJWT_whenCustomerFailsAuthentication_sendsAnalyticsEvent() throws JSONException {
        ThreeDSecureLookup threeDSecureLookup = ThreeDSecureLookup.fromJson(stringFromFixture("three_d_secure/lookup_response_with_version_number2.json"));

        mMockFragmentBuilder.successResponse(stringFromFixture("three_d_secure/2.0/authentication_response_with_error.json"));
        mFragment = mMockFragmentBuilder.build();

        ThreeDSecure.authenticateCardinalJWT(mFragment, threeDSecureLookup, "jwt");

        verify(mFragment).sendAnalyticsEvent(eq("three-d-secure.verification-flow.upgrade-payment-method.started"));
        verify(mFragment).sendAnalyticsEvent(eq("three-d-secure.verification-flow.upgrade-payment-method.failure.returned-lookup-nonce"));
    }

    @Test
    public void authenticateCardinalJWT_whenCustomerFailsAuthentication_returnsLookupCardNonce() throws JSONException {
        ThreeDSecureLookup threeDSecureLookup = ThreeDSecureLookup.fromJson(
                stringFromFixture("three_d_secure/2.0/lookup_response_without_liability_with_liability_shift_possible.json"));

        String authResponseJson = stringFromFixture("three_d_secure/2.0/authentication_response_with_error.json");
        mMockFragmentBuilder.successResponse(authResponseJson);
        mFragment = mMockFragmentBuilder.build();

        ThreeDSecure.authenticateCardinalJWT(mFragment, threeDSecureLookup, "jwt");

        ArgumentCaptor<CardNonce> captor = ArgumentCaptor.forClass(CardNonce.class);
        verify(mFragment).postCallback(captor.capture());

        CardNonce cardNonce = captor.getValue();

        assertFalse(cardNonce.getThreeDSecureInfo().isLiabilityShifted());
        assertTrue(cardNonce.getThreeDSecureInfo().isLiabilityShiftPossible());
        assertEquals("123456-12345-12345-a-adfa", cardNonce.getNonce());
        assertEquals("Failed to authenticate, please try a different form of payment.", cardNonce.getThreeDSecureInfo().getErrorMessage());
        assertEquals(authResponseJson, cardNonce.getThreeDSecureInfo().getThreeDSecureAuthenticationResponse().getErrors());
    }

    @Test
    public void authenticateCardinalJWT_whenExceptionOccurs_sendsAnalyticsEvent() throws JSONException {
        ThreeDSecureLookup threeDSecureLookup = ThreeDSecureLookup.fromJson(stringFromFixture("three_d_secure/lookup_response_with_version_number2.json"));

        mMockFragmentBuilder.errorResponse(new BraintreeException("an error occurred!"));
        mFragment = mMockFragmentBuilder.build();

        ThreeDSecure.authenticateCardinalJWT(mFragment, threeDSecureLookup, "jwt");

        verify(mFragment).sendAnalyticsEvent(eq("three-d-secure.verification-flow.upgrade-payment-method.started"));
        verify(mFragment).sendAnalyticsEvent(eq("three-d-secure.verification-flow.upgrade-payment-method.errored"));
    }

    @Test
    public void authenticateCardinalJWT_whenExceptionOccurs_returnsException() throws JSONException {
        ThreeDSecureLookup threeDSecureLookup = ThreeDSecureLookup.fromJson(stringFromFixture("three_d_secure/lookup_response_with_version_number2.json"));

        mMockFragmentBuilder.errorResponse(new BraintreeException("an error occurred!"));
        mFragment = mMockFragmentBuilder.build();

        ThreeDSecure.authenticateCardinalJWT(mFragment, threeDSecureLookup, "jwt");

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(mFragment).postCallback(captor.capture());

        assertEquals("an error occurred!", captor.getValue().getMessage());
    }

    @Test
    public void onActivityResult_whenCardinalCardVerificationReportsSuccess_sendsAnalyticsEvent() throws JSONException {
        mFragment = mMockFragmentBuilder.build();

        ThreeDSecureLookup threeDSecureLookup = ThreeDSecureLookup.fromJson(stringFromFixture("three_d_secure/lookup_response_with_version_number2.json"));

        ValidateResponse validateResponse = mock(ValidateResponse.class);
        when(validateResponse.getActionCode()).thenReturn(CardinalActionCode.SUCCESS);

        Intent data = new Intent();
        data.putExtra(ThreeDSecureActivity.EXTRA_VALIDATION_RESPONSE, validateResponse);
        data.putExtra(ThreeDSecureActivity.EXTRA_THREE_D_SECURE_LOOKUP, threeDSecureLookup);

        ThreeDSecure.onActivityResult(mFragment, RESULT_OK, data);

        verify(mFragment).sendAnalyticsEvent(eq("three-d-secure.verification-flow.completed"));
        verify(mFragment).sendAnalyticsEvent(eq("three-d-secure.verification-flow.cardinal-sdk.action-code.success"));
    }

    @Test
    public void onActivityResult_whenCardinalCardVerificationReportsNoAction_sendsAnalyticsEvent() throws JSONException {
        mFragment = mMockFragmentBuilder.build();

        ThreeDSecureLookup threeDSecureLookup = ThreeDSecureLookup.fromJson(stringFromFixture("three_d_secure/lookup_response_with_version_number2.json"));

        ValidateResponse validateResponse = mock(ValidateResponse.class);
        when(validateResponse.getActionCode()).thenReturn(CardinalActionCode.NOACTION);

        Intent data = new Intent();
        data.putExtra(ThreeDSecureActivity.EXTRA_VALIDATION_RESPONSE, validateResponse);
        data.putExtra(ThreeDSecureActivity.EXTRA_THREE_D_SECURE_LOOKUP, threeDSecureLookup);

        ThreeDSecure.onActivityResult(mFragment, RESULT_OK, data);

        verify(mFragment).sendAnalyticsEvent(eq("three-d-secure.verification-flow.completed"));
        verify(mFragment).sendAnalyticsEvent(eq("three-d-secure.verification-flow.cardinal-sdk.action-code.noaction"));
    }

    @Test
    public void onActivityResult_whenCardinalCardVerificationReportsFailure_sendsAnalyticsEvent() throws JSONException {
        mFragment = mMockFragmentBuilder.build();

        ThreeDSecureLookup threeDSecureLookup = ThreeDSecureLookup.fromJson(stringFromFixture("three_d_secure/lookup_response_with_version_number2.json"));

        ValidateResponse validateResponse = mock(ValidateResponse.class);
        when(validateResponse.getActionCode()).thenReturn(CardinalActionCode.FAILURE);

        Intent data = new Intent();
        data.putExtra(ThreeDSecureActivity.EXTRA_VALIDATION_RESPONSE, validateResponse);
        data.putExtra(ThreeDSecureActivity.EXTRA_THREE_D_SECURE_LOOKUP, threeDSecureLookup);

        ThreeDSecure.onActivityResult(mFragment, RESULT_OK, data);

        verify(mFragment).sendAnalyticsEvent(eq("three-d-secure.verification-flow.completed"));
        verify(mFragment).sendAnalyticsEvent(eq("three-d-secure.verification-flow.cardinal-sdk.action-code.failure"));
    }

    @Test
    public void onActivityResult_whenCardinalCardVerificationIsCanceled_sendsAnalyticsEvent() {
        mFragment = mMockFragmentBuilder.build();

        ValidateResponse validateResponse = mock(ValidateResponse.class);
        when(validateResponse.getActionCode()).thenReturn(CardinalActionCode.CANCEL);

        Intent data = new Intent();
        data.putExtra(ThreeDSecureActivity.EXTRA_VALIDATION_RESPONSE, validateResponse);

        ThreeDSecure.onActivityResult(mFragment, RESULT_OK, data);

        verify(mFragment).sendAnalyticsEvent(eq("three-d-secure.verification-flow.canceled"));
    }

    @Test
    public void onActivityResult_whenCardinalCardVerificationErrors_sendsAnalyticsEvent() {
        mFragment = mMockFragmentBuilder.build();

        ValidateResponse validateResponse = mock(ValidateResponse.class);
        when(validateResponse.getActionCode()).thenReturn(CardinalActionCode.ERROR);

        Intent data = new Intent();
        data.putExtra(ThreeDSecureActivity.EXTRA_VALIDATION_RESPONSE, validateResponse);

        ThreeDSecure.onActivityResult(mFragment, RESULT_OK, data);

        verify(mFragment).sendAnalyticsEvent(eq("three-d-secure.verification-flow.failed"));
    }

    @Test
    public void onActivityResult_whenCardinalCardVerificationTimeout_sendsAnalyticsEvent() {
        mFragment = mMockFragmentBuilder.build();

        ValidateResponse validateResponse = mock(ValidateResponse.class);
        when(validateResponse.getActionCode()).thenReturn(CardinalActionCode.TIMEOUT);

        Intent data = new Intent();
        data.putExtra(ThreeDSecureActivity.EXTRA_VALIDATION_RESPONSE, validateResponse);

        ThreeDSecure.onActivityResult(mFragment, RESULT_OK, data);

        verify(mFragment).sendAnalyticsEvent(eq("three-d-secure.verification-flow.failed"));
    }
}
