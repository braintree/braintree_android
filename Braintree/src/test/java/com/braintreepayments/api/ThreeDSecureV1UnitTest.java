package com.braintreepayments.api;

import org.json.JSONObject;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.internal.ManifestValidator;
import com.braintreepayments.api.models.Authorization;
import com.braintreepayments.api.models.CardBuilder;
import com.braintreepayments.api.models.CardNonce;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.api.models.ThreeDSecureLookup;
import com.braintreepayments.api.models.ThreeDSecurePostalAddress;
import com.braintreepayments.api.models.ThreeDSecureRequest;
import com.braintreepayments.testutils.TestConfigurationBuilder;
import com.cardinalcommerce.cardinalmobilesdk.Cardinal;
import com.cardinalcommerce.cardinalmobilesdk.models.response.CardinalActionCode;
import com.cardinalcommerce.cardinalmobilesdk.models.response.ValidateResponse;

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

import static com.braintreepayments.api.BraintreePowerMockHelper.*;
import static androidx.appcompat.app.AppCompatActivity.RESULT_OK;
import static com.braintreepayments.api.BraintreePowerMockHelper.MockStaticCardinal;
import static com.braintreepayments.api.BraintreePowerMockHelper.MockStaticTokenizationClient;
import static com.braintreepayments.api.test.Assertions.assertIsANonce;
import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(RobolectricTestRunner.class)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*", "org.json.*", "javax.crypto.*" })
@PrepareForTest({ ManifestValidator.class, TokenizationClient.class })
public class ThreeDSecureV1UnitTest {

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
                .amount("1.00");
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
    public void performVerification_sendsMinimumParamatersInLookupRequest() throws JSONException {
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
    public void performVerification_sendsPartialParamatersInLookupRequest() throws JSONException {
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
    public void performVerification_sendsAnalyticsEvent() {
        mMockFragmentBuilder.successResponse(stringFromFixture("three_d_secure/lookup_response_with_version_number1.json"));
        mFragment = mMockFragmentBuilder.build();

        ThreeDSecure.performVerification(mFragment, mBasicRequest);

        verify(mFragment).sendAnalyticsEvent(eq("three-d-secure.verification-flow.3ds-version.1.0.2"));
    }

}
