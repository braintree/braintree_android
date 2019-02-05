package com.braintreepayments.api;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.interfaces.BraintreeErrorListener;
import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.internal.ManifestValidator;
import com.braintreepayments.api.models.Authorization;
import com.braintreepayments.api.models.CardNonce;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.PaymentMethodNonce;
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
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;

import androidx.appcompat.app.AppCompatActivity;

import static com.braintreepayments.api.test.Assertions.assertIsANonce;
import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.spy;

@RunWith(RobolectricTestRunner.class)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*", "org.json.*", "javax.crypto.*" })
@PrepareForTest({ ManifestValidator.class })
public class ThreeDSecureUnitTest {

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    private MockFragmentBuilder mMockFragmentBuilder;
    private BraintreeFragment mFragment;
    public ThreeDSecureRequest mBasicRequest;

    @Before
    public void setup() throws Exception {
        mockUrlSchemeDeclaredInAndroidManifest(true);

        Configuration configuration = new TestConfigurationBuilder()
                .threeDSecureEnabled(true)
                .buildConfiguration();

        mMockFragmentBuilder = new MockFragmentBuilder()
                .authorization(Authorization.fromString(stringFromFixture("base_64_client_token.txt")))
                .configuration(configuration);
        mFragment = mMockFragmentBuilder.build();

        mBasicRequest = new ThreeDSecureRequest()
                .nonce("a-nonce")
                .amount("1.00");
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
    public void performVerification_sendsAllParamatersInLookupRequest() throws InterruptedException, JSONException {
        ThreeDSecureRequest request = new ThreeDSecureRequest()
                .nonce("a-nonce")
                .amount("1.00")
                .shippingMethod("01")
                .mobilePhoneNumber("8101234567")
                .email("test@example.com")
                .billingAddress(new ThreeDSecurePostalAddress()
                        .firstName("Joe")
                        .lastName("Guy")
                        .streetAddress("555 Smith Street")
                        .extendedAddress("#5")
                        .locality("Oakland")
                        .region("CA")
                        .postalCode("12345")
                        .countryCodeAlpha2("US")
                        .phoneNumber("12345678"));

        ThreeDSecure.performVerification(mFragment, request);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mFragment.getHttpClient()).post(anyString(), captor.capture(), any(HttpResponseCallback.class));

        JSONObject body = new JSONObject(captor.getValue());

        assertEquals("1.00", body.getString("amount"));

        JSONObject jsonAdditionalInformation = body.getJSONObject("additionalInformation");

        assertEquals("8101234567", jsonAdditionalInformation.getString("mobilePhoneNumber"));
        assertEquals("test@example.com", jsonAdditionalInformation.getString("email"));
        assertEquals("01", jsonAdditionalInformation.getString("shippingMethod"));
        assertEquals("Joe", jsonAdditionalInformation.getString("firstName"));
        assertEquals("Guy", jsonAdditionalInformation.getString("lastName"));
        assertEquals("12345678", jsonAdditionalInformation.getString("phoneNumber"));

        JSONObject jsonBillingAddress = jsonAdditionalInformation.getJSONObject("billingAddress");

        assertEquals("555 Smith Street", jsonBillingAddress.getString("line1"));
        assertEquals("#5", jsonBillingAddress.getString("line2"));
        assertEquals("Oakland", jsonBillingAddress.getString("city"));
        assertEquals("CA", jsonBillingAddress.getString("state"));
        assertEquals("12345", jsonBillingAddress.getString("postalCode"));
        assertEquals("US", jsonBillingAddress.getString("countryCode"));
    }

    @Test
    public void performVerification_sendsMinimumParamatersInLookupRequest() throws InterruptedException, JSONException {
        ThreeDSecure.performVerification(mFragment, mBasicRequest);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mFragment.getHttpClient()).post(anyString(), captor.capture(), any(HttpResponseCallback.class));

        JSONObject body = new JSONObject(captor.getValue());

        assertEquals("1.00", body.getString("amount"));

        JSONObject jsonAdditionalInformation = body.getJSONObject("additionalInformation");

        assertTrue(jsonAdditionalInformation.isNull("mobilePhoneNumber"));
        assertTrue(jsonAdditionalInformation.isNull("email"));
        assertTrue(jsonAdditionalInformation.isNull("shippingMethod"));
        assertTrue(jsonAdditionalInformation.isNull("billingAddress"));
    }

    @Test
    public void performVerification_sendsPartialParamatersInLookupRequest() throws InterruptedException, JSONException {
        ThreeDSecureRequest request = new ThreeDSecureRequest()
                .nonce("a-nonce")
                .amount("1.00")
                .email("test@example.com")
                .billingAddress(new ThreeDSecurePostalAddress()
                        .firstName("Joe")
                        .lastName("Guy")
                        .streetAddress("555 Smith Street")
                        .locality("Oakland")
                        .region("CA")
                        .postalCode("12345")
                        .countryCodeAlpha2("US"));

        ThreeDSecure.performVerification(mFragment, request);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mFragment.getHttpClient()).post(anyString(), captor.capture(), any(HttpResponseCallback.class));

        JSONObject body = new JSONObject(captor.getValue());

        assertEquals("1.00", body.getString("amount"));

        JSONObject jsonAdditionalInformation = body.getJSONObject("additionalInformation");

        assertTrue(jsonAdditionalInformation.isNull("mobilePhoneNumber"));
        assertEquals("test@example.com", jsonAdditionalInformation.getString("email"));
        assertTrue(jsonAdditionalInformation.isNull("shippingMethod"));
        assertEquals("Joe", jsonAdditionalInformation.getString("firstName"));
        assertEquals("Guy", jsonAdditionalInformation.getString("lastName"));
        assertTrue(jsonAdditionalInformation.isNull("phoneNumber"));

        JSONObject jsonBillingAddress = jsonAdditionalInformation.getJSONObject("billingAddress");

        assertEquals("555 Smith Street", jsonBillingAddress.getString("line1"));
        assertTrue(jsonBillingAddress.isNull("line2"));
        assertEquals("Oakland", jsonBillingAddress.getString("city"));
        assertEquals("CA", jsonBillingAddress.getString("state"));
        assertEquals("12345", jsonBillingAddress.getString("postalCode"));
        assertEquals("US", jsonBillingAddress.getString("countryCode"));
    }

    @Test
    public void performVerification_whenBrowserSwitchNotSetup_postsException() {
        mockUrlSchemeDeclaredInAndroidManifest(false);

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
        mockUrlSchemeDeclaredInAndroidManifest(false);

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

        ThreeDSecure.onActivityResult(mFragment, AppCompatActivity.RESULT_OK, data);

        ArgumentCaptor<PaymentMethodNonce> captor = ArgumentCaptor.forClass(PaymentMethodNonce.class);
        verify(mFragment).postCallback(captor.capture());
        PaymentMethodNonce paymentMethodNonce = captor.getValue();

        assertIsANonce(paymentMethodNonce.getNonce());
        assertEquals("11", ((CardNonce) paymentMethodNonce).getLastTwo());
        assertTrue(((CardNonce) paymentMethodNonce).getThreeDSecureInfo().wasVerified());
    }

    @Test
    public void onActivityResult_whenFailure_postsException() throws Exception {
        JSONObject json = new JSONObject();
        json.put("success", false);

        Uri uri = Uri.parse("https://.com?auth_response=" + json.toString());
        Intent data = new Intent();
        data.setData(uri);

        ThreeDSecure.onActivityResult(mFragment, AppCompatActivity.RESULT_OK, data);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(mFragment).postCallback(captor.capture());

        ErrorWithResponse error = (ErrorWithResponse) captor.getValue();
        assertEquals(422, error.getStatusCode());
    }

    private void mockUrlSchemeDeclaredInAndroidManifest(boolean returnValue) {
        spy(ManifestValidator.class);
        try {
            doReturn(returnValue).when(ManifestValidator.class,
                    "isUrlSchemeDeclaredInAndroidManifest", any(Context.class),
                    anyString(), any(Class.class));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
