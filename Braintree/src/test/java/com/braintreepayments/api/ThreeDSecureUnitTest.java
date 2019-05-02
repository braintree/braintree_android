package com.braintreepayments.api;

import android.content.Intent;
import android.net.Uri;

import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.interfaces.ThreeDSecureLookupListener;
import com.braintreepayments.api.internal.ManifestValidator;
import com.braintreepayments.api.models.Authorization;
import com.braintreepayments.api.models.CardBuilder;
import com.braintreepayments.api.models.CardNonce;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.api.models.ThreeDSecureInfo;
import com.braintreepayments.api.models.ThreeDSecureLookup;
import com.braintreepayments.api.models.ThreeDSecurePostalAddress;
import com.braintreepayments.api.models.ThreeDSecureRequest;
import com.braintreepayments.testutils.TestConfigurationBuilder;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import androidx.appcompat.app.AppCompatActivity;

import static androidx.appcompat.app.AppCompatActivity.RESULT_OK;
import static com.braintreepayments.api.BraintreePowerMockHelper.MockManifestValidator;
import static com.braintreepayments.api.BraintreePowerMockHelper.MockStaticTokenizationClient;
import static com.braintreepayments.api.test.Assertions.assertIsANonce;
import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(RobolectricTestRunner.class)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*", "org.json.*", "javax.crypto.*" })
@PrepareForTest({ ManifestValidator.class, TokenizationClient.class })
/**
 * This class tests ThreeDSecure content that is unrelated a specific 3DS version.
 */
public class ThreeDSecureUnitTest {

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    private MockFragmentBuilder mMockFragmentBuilder;
    private BraintreeFragment mFragment;
    public ThreeDSecureRequest mBasicRequest;

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
    public void performVerification_sendsParamsInLookupRequest() throws JSONException {
        ThreeDSecure.performVerification(mFragment, mBasicRequest);

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mFragment.getHttpClient()).post(urlCaptor.capture(), captor.capture(), any(HttpResponseCallback.class));

        String url = urlCaptor.getValue();
        JSONObject body = new JSONObject(captor.getValue());

        assertTrue(url.contains("a-nonce"));
        assertEquals("amount", body.getString("amount"));

        assertEquals("billing-given-name", body.getJSONObject("additional_info")
                .getString("billing_given_name"));
    }

    @Test
    public void performVerification_withCardBuilder_errorsWhenNoAmount() {
        MockStaticTokenizationClient.mockTokenizeSuccess(null);

        CardBuilder cardBuilder = new CardBuilder();
        ThreeDSecureRequest request = new ThreeDSecureRequest();

        ThreeDSecure.performVerification(mFragment, cardBuilder, request);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(mFragment).postCallback(captor.capture());

        assertEquals("The ThreeDSecureRequest amount cannot be null",
                captor.getValue().getMessage());
    }

    @Test
    public void performVerification_withCardBuilderFailsToTokenize_postsError() {
        MockStaticTokenizationClient.mockTokenizeFailure(new RuntimeException("Tokenization Failed"));

        CardBuilder cardBuilder = new CardBuilder();
        ThreeDSecureRequest request = new ThreeDSecureRequest()
                .amount("10");

        ThreeDSecure.performVerification(mFragment, cardBuilder, request);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(mFragment).postCallback(captor.capture());

        assertEquals("Tokenization Failed",
                captor.getValue().getMessage());
    }

    @Test
    public void performVerification_withCardBuilder_tokenizesAndPerformsVerification() {
        CardNonce cardNonce = mock(CardNonce.class);
        when(cardNonce.getNonce()).thenReturn("card-nonce");
        MockStaticTokenizationClient.mockTokenizeSuccess(cardNonce);

        CardBuilder cardBuilder = new CardBuilder();
        ThreeDSecureRequest request = new ThreeDSecureRequest()
                .amount("10");

        ThreeDSecure.performVerification(mFragment, cardBuilder, request);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);

        verifyStatic();
        TokenizationClient.versionedPath(captor.capture());

        assertTrue(captor.getValue().contains("card-nonce"));
    }

    @Test
    public void performVerification_callsLookupListener() throws InterruptedException {
        MockStaticTokenizationClient.mockTokenizeSuccess(null);

        final CountDownLatch latch = new CountDownLatch(1);

        ThreeDSecureLookupListener lookupListener = new ThreeDSecureLookupListener() {
            @Override
            public void onLookupComplete(ThreeDSecureRequest request, ThreeDSecureLookup lookup) {
                latch.countDown();
            }
        };

        ThreeDSecure.performVerification(mFragment, mBasicRequest, lookupListener);

        latch.await(1, TimeUnit.SECONDS);
    }

    @Test
    public void performVerification_withInvalidRequest_postsException() {
        ThreeDSecure.performVerification(mFragment, new ThreeDSecureRequest()
                .amount("5"));

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(mFragment).postCallback(captor.capture());

        assertEquals("The ThreeDSecureRequest nonce and amount cannot be null",
                captor.getValue().getMessage());
    }

    @Test
    public void performVerification_whenBrowserSwitchNotSetup_postsException() {
        MockManifestValidator.mockUrlSchemeDeclaredInAndroidManifest(false);

        ThreeDSecure.performVerification(mFragment, mBasicRequest);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(mFragment).postCallback(captor.capture());

        assertEquals("BraintreeBrowserSwitchActivity missing, " +
                "incorrectly configured in AndroidManifest.xml or another app defines the same browser " +
                "switch url as this app. See " +
                "https://developers.braintreepayments.com/guides/client-sdk/android/v2#browser-switch " +
                "for the correct configuration", captor.getValue().getMessage());
    }

    @Test
    public void performVerification_whenBrowserSwitchNotSetup_sendsAnalyticEvent() {
        MockManifestValidator.mockUrlSchemeDeclaredInAndroidManifest(false);

        ThreeDSecure.performVerification(mFragment, mBasicRequest);

        verify(mFragment).sendAnalyticsEvent(eq("three-d-secure.invalid-manifest"));
    }

    @Test
    public void onActivityResult_whenResultNotOk_doesNothing() {
        verifyNoMoreInteractions(mFragment);
        ThreeDSecure.onActivityResult(mFragment, AppCompatActivity.RESULT_CANCELED, null);
        verifyNoMoreInteractions(mFragment);
    }

    @Test
    public void onActivityResult_whenSuccessful_postsPayment() throws Exception {
        Uri uri = Uri.parse("http://demo-app.com")
                .buildUpon()
                .appendQueryParameter("auth_response", stringFromFixture("three_d_secure/authentication_response.json"))
                .build();
        Intent data = new Intent();
        data.setData(uri);

        ThreeDSecure.onActivityResult(mFragment, RESULT_OK, data);

        ArgumentCaptor<PaymentMethodNonce> captor = ArgumentCaptor.forClass(PaymentMethodNonce.class);
        verify(mFragment).postCallback(captor.capture());
        PaymentMethodNonce paymentMethodNonce = captor.getValue();

        assertIsANonce(paymentMethodNonce.getNonce());
        assertEquals("11", ((CardNonce) paymentMethodNonce).getLastTwo());
        assertTrue(((CardNonce) paymentMethodNonce).getThreeDSecureInfo().wasVerified());
    }

    @Test
    public void onActivityResult_whenSuccessful_sendAnalyticsEvents() throws Exception {
        Uri uri = Uri.parse("http://demo-app.com")
                .buildUpon()
                .appendQueryParameter("auth_response", stringFromFixture("three_d_secure/authentication_response.json"))
                .build();
        Intent data = new Intent();
        data.setData(uri);

        ThreeDSecure.onActivityResult(mFragment, RESULT_OK, data);

        ArgumentCaptor<PaymentMethodNonce> captor = ArgumentCaptor.forClass(PaymentMethodNonce.class);

        verify(mFragment).postCallback(captor.capture());

        verify(mFragment).sendAnalyticsEvent(eq("three-d-secure.verification-flow.liability-shifted.true"));
        verify(mFragment).sendAnalyticsEvent(eq("three-d-secure.verification-flow.liability-shift-possible.true"));
    }

    @Test
    public void onActivityResult_whenFailure_postsException() throws Exception {
        JSONObject json = new JSONObject();
        json.put("success", false);

        Uri uri = Uri.parse("https://.com?auth_response=" + json.toString());
        Intent data = new Intent();
        data.setData(uri);

        ThreeDSecure.onActivityResult(mFragment, RESULT_OK, data);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(mFragment).postCallback(captor.capture());

        ErrorWithResponse error = (ErrorWithResponse) captor.getValue();
        assertEquals(422, error.getStatusCode());
    }
}
