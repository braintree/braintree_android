package com.braintreepayments.api;

import android.net.Uri;

import com.braintreepayments.api.exceptions.BraintreeException;
import com.braintreepayments.api.exceptions.ConfigurationException;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCallback;
import com.braintreepayments.api.internal.BraintreeHttpClient;
import com.braintreepayments.api.models.CardNonce;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.UnionPayCapabilities;
import com.braintreepayments.api.models.UnionPayCardBuilder;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;

import static com.braintreepayments.testutils.CardNumber.UNIONPAY_CREDIT;
import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(RobolectricTestRunner.class)
@PowerMockIgnore({"org.json.*", "org.mockito.*", "org.robolectric.*", "android.*", "com.google.gms.*"})
@PrepareForTest(TokenizationClient.class)
public class UnionPayUnitTest {

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    private BraintreeFragment mBraintreeFragment;
    private Configuration mConfigurationWithUnionPay;

    @Before
    public void setup() throws JSONException {
        mConfigurationWithUnionPay = Configuration.fromJson(stringFromFixture("configuration/with_unionpay.json"));
        mBraintreeFragment = new MockFragmentBuilder()
                .configuration(mConfigurationWithUnionPay)
                .build();
    }

    @Test
    public void tokenize_sendsAnalyticsEventOnTokenizeResult() {
        mockSuccessCallback();
        UnionPayCardBuilder unionPayCardBuilder = new UnionPayCardBuilder();

        UnionPay.tokenize(mBraintreeFragment, unionPayCardBuilder);

        verify(mBraintreeFragment).sendAnalyticsEvent("union-pay.nonce-received");
    }

    @Test
    public void tokenize_callsListenerWithErrorOnFailure() {
        mockFailureCallback();
        UnionPay.tokenize(mBraintreeFragment, null);

        verify(mBraintreeFragment).postCallback(any(ErrorWithResponse.class));
    }

    @Test
    public void tokenize_sendsAnalyticsEventOnFailure() {
        mockFailureCallback();
        UnionPay.tokenize(mBraintreeFragment, null);

        verify(mBraintreeFragment).sendAnalyticsEvent("union-pay.nonce-failed");
    }

    @Test
    public void tokenize_sendsPayloadToEndpoint() throws JSONException {
        UnionPayCardBuilder unionPayCardBuilder = new UnionPayCardBuilder()
                .cardNumber("someCardNumber")
                .expirationMonth("expirationMonth")
                .expirationYear("expirationYear")
                .cvv("cvv")
                .enrollmentId("enrollmentId")
                .smsCode("smsCode")
                .validate(true);

        BraintreeHttpClient httpClient = mock(BraintreeHttpClient.class);
        doNothing().when(httpClient).get(anyString(), any(HttpResponseCallback.class));
        when(mBraintreeFragment.getHttpClient()).thenReturn(httpClient);
        ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);

        UnionPay.tokenize(mBraintreeFragment, unionPayCardBuilder);

        verify(httpClient).post(eq("/v1/payment_methods/credit_cards"), argumentCaptor.capture(),
                any(HttpResponseCallback.class));

        JSONObject tokenizePayload = new JSONObject(argumentCaptor.getValue());
        JSONObject creditCardPayload = tokenizePayload.getJSONObject("creditCard");
        JSONObject optionsPayload = creditCardPayload.getJSONObject("options");
        JSONObject unionPayEnrollmentPayload = optionsPayload.getJSONObject("unionPayEnrollment");

        assertEquals("someCardNumber", creditCardPayload.getString("number"));
        assertEquals("expirationMonth", creditCardPayload.getString("expirationMonth"));
        assertEquals("expirationYear", creditCardPayload.getString("expirationYear"));
        assertEquals("cvv", creditCardPayload.getString("cvv"));

        assertFalse(optionsPayload.has("validate"));
        assertEquals("enrollmentId", unionPayEnrollmentPayload.getString("id"));
        assertEquals("smsCode", unionPayEnrollmentPayload.getString("smsCode"));
    }

    @Test
    public void tokenize_optionalSmsCode_sendsPayloadToEndpoint() throws JSONException {
        UnionPayCardBuilder unionPayCardBuilder = new UnionPayCardBuilder()
                .cardNumber("someCardNumber")
                .expirationMonth("expirationMonth")
                .expirationYear("expirationYear")
                .cvv("cvv")
                .enrollmentId("enrollmentId")
                .validate(true);

        BraintreeHttpClient httpClient = mock(BraintreeHttpClient.class);
        doNothing().when(httpClient).get(anyString(), any(HttpResponseCallback.class));
        when(mBraintreeFragment.getHttpClient()).thenReturn(httpClient);
        ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);

        UnionPay.tokenize(mBraintreeFragment, unionPayCardBuilder);

        verify(httpClient).post(eq("/v1/payment_methods/credit_cards"), argumentCaptor.capture(),
                any(HttpResponseCallback.class));

        JSONObject tokenizePayload = new JSONObject(argumentCaptor.getValue());
        JSONObject creditCardPayload = tokenizePayload.getJSONObject("creditCard");
        JSONObject optionsPayload = creditCardPayload.getJSONObject("options");
        JSONObject unionPayEnrollmentPayload = optionsPayload.getJSONObject("unionPayEnrollment");

        assertEquals("someCardNumber", creditCardPayload.getString("number"));
        assertEquals("expirationMonth", creditCardPayload.getString("expirationMonth"));
        assertEquals("expirationYear", creditCardPayload.getString("expirationYear"));
        assertEquals("cvv", creditCardPayload.getString("cvv"));

        assertFalse(optionsPayload.has("validate"));
        assertEquals("enrollmentId", unionPayEnrollmentPayload.getString("id"));
        assertFalse(unionPayEnrollmentPayload.has("smsCode"));
    }

    @Test
    public void enroll_callsListenerWithUnionPayEnrollmentIdAdded() throws JSONException {
        String expectedEnrollmentId = "some-enrollment-id";
        boolean expectedSmsCodeRequired = true;
        JSONObject response = new JSONObject();
        response.put("unionPayEnrollmentId", expectedEnrollmentId);
        response.put("smsCodeRequired", expectedSmsCodeRequired);

        BraintreeFragment fragment = new MockFragmentBuilder()
                .configuration(mConfigurationWithUnionPay)
                .successResponse(response.toString())
                .build();

        UnionPayCardBuilder unionPayCardBuilder = new UnionPayCardBuilder();
        UnionPay.enroll(fragment, unionPayCardBuilder);
        verify(fragment).postUnionPayCallback(expectedEnrollmentId, expectedSmsCodeRequired);
    }

    @Test
    public void enroll_failsIfUnionPayIsDisabled() throws JSONException {
        UnionPayCardBuilder unionPayCardBuilder = new UnionPayCardBuilder()
                .cardNumber("some-card-number");

        BraintreeFragment fragment = new MockFragmentBuilder()
                .configuration(Configuration.fromJson(stringFromFixture("configuration/configuration.json")))
                .build();

        UnionPay.enroll(fragment, unionPayCardBuilder);

        ArgumentCaptor<ConfigurationException> argumentCaptor = ArgumentCaptor.forClass(ConfigurationException.class);
        verify(fragment).postCallback(argumentCaptor.capture());
        assertEquals("UnionPay is not enabled", argumentCaptor.getValue().getMessage());
    }

    @Test
    public void enroll_sendsAnalyticsEventOnFailure() throws JSONException {
        UnionPayCardBuilder unionPayCardBuilder = new UnionPayCardBuilder()
                .cardNumber("some-card-number");

        BraintreeFragment braintreeFragment = new MockFragmentBuilder()
                .configuration(mConfigurationWithUnionPay)
                .errorResponse(new BraintreeException())
                .build();

        UnionPay.enroll(braintreeFragment, unionPayCardBuilder);

        verify(braintreeFragment).sendAnalyticsEvent("union-pay.enrollment-failed");
    }

    @Test
    public void enroll_sendsAnalyticsEventOnSuccess() throws JSONException {
        UnionPayCardBuilder unionPayCardBuilder = new UnionPayCardBuilder()
                .cardNumber("some-card-number");

        JSONObject successObject = new JSONObject()
                .put("unionPayEnrollmentId", "unionPayEnrollmentId")
                .put("smsCodeRequired", true);

        BraintreeFragment braintreeFragment = new MockFragmentBuilder()
                .configuration(mConfigurationWithUnionPay)
                .successResponse(successObject.toString())
                .build();

        UnionPay.enroll(braintreeFragment, unionPayCardBuilder);

        verify(braintreeFragment).sendAnalyticsEvent("union-pay.enrollment-succeeded");
    }

    @Test
    public void enroll_doesNotPassCvvToEnrollmentPayloadIfCvvExists() throws JSONException {
        UnionPayCardBuilder unionPayCardBuilder = new UnionPayCardBuilder()
                .cardNumber("some-card-number")
                .cvv("123");

        BraintreeHttpClient httpClient = mock(BraintreeHttpClient.class);
        doNothing().when(httpClient).get(anyString(), any(HttpResponseCallback.class));
        when(mBraintreeFragment.getHttpClient()).thenReturn(httpClient);
        ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);

        UnionPay.enroll(mBraintreeFragment, unionPayCardBuilder);

        verify(httpClient).post(eq("/v1/union_pay_enrollments"), argumentCaptor.capture(),
                any(HttpResponseCallback.class));

        JSONObject enrollmentPayload = new JSONObject(argumentCaptor.getValue());
        JSONObject unionPayEnrollmentPayload = enrollmentPayload.getJSONObject("unionPayEnrollment");

        assertFalse(unionPayEnrollmentPayload.has("cvv"));
    }

    @Test
    public void enroll_sendsPayloadToEndpoint() throws JSONException {
        UnionPayCardBuilder unionPayCardBuilder = new UnionPayCardBuilder()
                .cardNumber("someCardNumber")
                .expirationMonth("expirationMonth")
                .expirationYear("expirationYear")
                .mobileCountryCode("mobileCountryCode")
                .mobilePhoneNumber("mobilePhoneNumber");

        BraintreeHttpClient httpClient = mock(BraintreeHttpClient.class);
        doNothing().when(httpClient).get(anyString(), any(HttpResponseCallback.class));
        when(mBraintreeFragment.getHttpClient()).thenReturn(httpClient);
        ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);

        UnionPay.enroll(mBraintreeFragment, unionPayCardBuilder);

        verify(httpClient).post(eq("/v1/union_pay_enrollments"), argumentCaptor.capture(),
                any(HttpResponseCallback.class));

        JSONObject enrollPayload = new JSONObject(argumentCaptor.getValue());
        JSONObject unionPayEnrollment = enrollPayload.getJSONObject("unionPayEnrollment");
        assertEquals("someCardNumber", unionPayEnrollment.getString("number"));
        assertEquals("expirationMonth", unionPayEnrollment.getString("expirationMonth"));
        assertEquals("expirationYear", unionPayEnrollment.getString("expirationYear"));
        assertEquals("mobileCountryCode", unionPayEnrollment.getString("mobileCountryCode"));
        assertEquals("mobilePhoneNumber", unionPayEnrollment.getString("mobileNumber"));
    }

    @Test
    public void fetchCapabilities_sendsPayloadToEndpoint() {
        BraintreeFragment fragment = new MockFragmentBuilder()
                .configuration(mConfigurationWithUnionPay)
                .build();

        BraintreeHttpClient httpClient = mock(BraintreeHttpClient.class);
        doNothing().when(httpClient).get(anyString(), any(HttpResponseCallback.class));

        when(fragment.getHttpClient()).thenReturn(httpClient);

        UnionPay.fetchCapabilities(fragment, UNIONPAY_CREDIT);

        String expectedUrl = Uri.parse("/v1/payment_methods/credit_cards/capabilities")
                .buildUpon()
                .appendQueryParameter("creditCard[number]", UNIONPAY_CREDIT)
                .build()
                .toString();

        verify(httpClient).get(eq(expectedUrl), any(HttpResponseCallback.class));
    }

    @Test
    public void fetchCapabilities_callsListenerWithErrorOnFailure() {
        RuntimeException expected = new RuntimeException("expected runtime exception");
        BraintreeFragment fragment = new MockFragmentBuilder()
                .configuration(mConfigurationWithUnionPay)
                .errorResponse(expected)
                .build();

        UnionPay.fetchCapabilities(fragment, UNIONPAY_CREDIT);

        verify(fragment).postCallback(expected);
    }

    @Test
    public void fetchCapabilities_sendsAnalyticsEventOnFailure() {
        BraintreeFragment fragment = new MockFragmentBuilder()
                .configuration(mConfigurationWithUnionPay)
                .errorResponse(new RuntimeException("expected runtime exception"))
                .build();

        UnionPay.fetchCapabilities(fragment, UNIONPAY_CREDIT);

        verify(fragment).sendAnalyticsEvent("union-pay.capabilities-failed");
    }

    @Test
    public void fetchCapabilities_callsListenerWithCapabilitiesOnSuccess() throws JSONException {
        BraintreeFragment fragment = new MockFragmentBuilder()
                .configuration(mConfigurationWithUnionPay)
                .successResponse(stringFromFixture("unionpay_capabilities_success_response.json"))
                .build();

        UnionPay.fetchCapabilities(fragment, UNIONPAY_CREDIT);

        ArgumentCaptor<UnionPayCapabilities> argumentCaptor = ArgumentCaptor.forClass(UnionPayCapabilities.class);
        verify(fragment).postCallback(argumentCaptor.capture());

        UnionPayCapabilities capabilities = argumentCaptor.getValue();
        assertNotNull(capabilities);
        assertTrue(capabilities.isUnionPay());
        assertFalse(capabilities.isDebit());
        assertTrue(capabilities.supportsTwoStepAuthAndCapture());
        assertTrue(capabilities.isSupported());
    }

    @Test
    public void fetchCapabilities_sendsAnalyticsEventOnSuccess() throws JSONException {
        BraintreeFragment fragment = new MockFragmentBuilder()
                .configuration(mConfigurationWithUnionPay)
                .successResponse(stringFromFixture("unionpay_capabilities_success_response.json"))
                .build();

        UnionPay.fetchCapabilities(fragment, UNIONPAY_CREDIT);

        verify(fragment).sendAnalyticsEvent("union-pay.capabilities-received");
    }

    @Test
    public void fetchCapabilities_failsIfUnionPayIsDisabled() throws JSONException {
        BraintreeFragment fragment = new MockFragmentBuilder()
                .configuration(Configuration.fromJson(stringFromFixture("configuration/configuration.json")))
                .build();

        UnionPay.fetchCapabilities(fragment, UNIONPAY_CREDIT);

        ArgumentCaptor<ConfigurationException> argumentCaptor = ArgumentCaptor.forClass(ConfigurationException.class);
        verify(fragment).postCallback(argumentCaptor.capture());
        assertEquals("UnionPay is not enabled", argumentCaptor.getValue().getMessage());
    }

    /* helpers */
    private void mockSuccessCallback() {
        mockStatic(TokenizationClient.class);
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                UnionPayCardBuilder cardBuilder = (UnionPayCardBuilder) invocation.getArguments()[1];
                CardNonce cardNonce = mock(CardNonce.class);
                boolean hasSmsCode = false;
                try {
                    hasSmsCode = new JSONObject(cardBuilder.build()).getJSONObject("options").has("smsCode");
                } catch (JSONException ignored) {}

                if (hasSmsCode) {
                    when(cardNonce.getNonce()).thenReturn("nonce");
                }
                ((PaymentMethodNonceCallback) invocation.getArguments()[2]).success(cardNonce);
                return null;
            }
        }).when(TokenizationClient.class);
        TokenizationClient.tokenize(any(BraintreeFragment.class), any(UnionPayCardBuilder.class),
                any(PaymentMethodNonceCallback.class));
    }

    private void mockFailureCallback() {
        mockStatic(TokenizationClient.class);
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                ((PaymentMethodNonceCallback) invocation.getArguments()[2]).failure(new ErrorWithResponse(422, ""));
                return null;
            }
        }).when(TokenizationClient.class);
        TokenizationClient.tokenize(any(BraintreeFragment.class), any(UnionPayCardBuilder.class),
                any(PaymentMethodNonceCallback.class));
    }
}
