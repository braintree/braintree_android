package com.braintreepayments.api;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.braintreepayments.api.exceptions.BraintreeException;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCallback;
import com.braintreepayments.api.models.AnalyticsConfiguration;
import com.braintreepayments.api.models.Authorization;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.PayPalAccountNonce;
import com.braintreepayments.api.models.PayPalConfiguration;
import com.braintreepayments.api.models.PayPalRequest;
import com.braintreepayments.api.models.PaymentMethodBuilder;
import com.braintreepayments.api.models.PostalAddress;
import com.paypal.android.sdk.onetouch.core.AuthorizationRequest;
import com.paypal.android.sdk.onetouch.core.base.ContextInspector;
import com.paypal.android.sdk.onetouch.core.config.Recipe;

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
import org.robolectric.RobolectricGradleTestRunner;

import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(RobolectricGradleTestRunner.class)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*", "org.json.*", "javax.crypto.*" })
@PrepareForTest({ PayPal.class, Recipe.class, AuthorizationRequest.class, TokenizationClient.class })
public class PayPalTest {

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    private MockFragmentBuilder mMockFragmentBuilder;

    @Before
    public void setup() throws Exception {
        spy(PayPal.class);
        doReturn(true).when(PayPal.class, "isManifestValid", any(Context.class));

        spy(Recipe.class);
        doReturn(true).when(Recipe.class, "isValidBrowserTarget", any(Context.class), anyString(), anyString());

        Authorization authorization = mock(Authorization.class);
        when(authorization.toString()).thenReturn("authorization");

        AnalyticsConfiguration analyticsConfiguration = mock(AnalyticsConfiguration.class);
        when(analyticsConfiguration.isEnabled()).thenReturn(true);

        PayPalConfiguration payPalConfiguration = mock(PayPalConfiguration.class);
        when(payPalConfiguration.isEnabled()).thenReturn(true);
        when(payPalConfiguration.getEnvironment()).thenReturn("offline");
        when(payPalConfiguration.shouldUseBillingAgreement()).thenReturn(false);

        Configuration configuration = mock(Configuration.class);
        when(configuration.getAnalytics()).thenReturn(analyticsConfiguration);
        when(configuration.isPayPalEnabled()).thenReturn(true);
        when(configuration.getPayPal()).thenReturn(payPalConfiguration);

        mMockFragmentBuilder = new MockFragmentBuilder()
                .authorization(authorization)
                .configuration(configuration);
    }

    @Test
    public void authorizeAccount_sendsAnalyticsEvent() {
        BraintreeFragment fragment = mMockFragmentBuilder.build();

        PayPal.authorizeAccount(fragment);

        verify(fragment).sendAnalyticsEvent("paypal.future-payments.selected");
    }

    @Test
    public void authorizeAccount_postsExceptionWhenNotEnabled() throws JSONException, InterruptedException {
        BraintreeFragment fragment = new MockFragmentBuilder()
                .configuration(mock(Configuration.class))
                .build();

        PayPal.authorizeAccount(fragment);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(fragment).postCallback(captor.capture());
        assertTrue(captor.getValue() instanceof BraintreeException);
        assertEquals("PayPal is not enabled", captor.getValue().getMessage());
    }

    @Test
    public void authorizeAccount_startsBrowser() {
        BraintreeFragment fragment = mMockFragmentBuilder.build();

        PayPal.authorizeAccount(fragment);

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(fragment).startActivity(captor.capture());
        Intent intent = captor.getValue();

        assertEquals(Intent.ACTION_VIEW, intent.getAction());
        assertEquals("checkout.paypal.com", intent.getData().getHost());
        assertEquals("/one-touch-login/", intent.getData().getPath());
        assertNotNull(intent.getData().getQueryParameter("payload"));
        assertNotNull(intent.getData().getQueryParameter("payloadEnc"));
        assertEquals("com.braintreepayments.api.braintree://onetouch/v1/success", intent.getData().getQueryParameter("x-success"));
        assertEquals("com.braintreepayments.api.braintree://onetouch/v1/cancel", intent.getData().getQueryParameter("x-cancel"));
        assertTrue(intent.getBooleanExtra(BraintreeBrowserSwitchActivity.EXTRA_BROWSER_SWITCH, false));
    }

    @Test
    public void authorizeAccount_isSuccessful() throws Exception {
        spy(AuthorizationRequest.class);
        doReturn(true).when(AuthorizationRequest.class, "isValidResponse", any(ContextInspector.class), anyString());

        JSONObject decryptedPayload = mock(JSONObject.class);
        when(decryptedPayload.getString("payment_code")).thenReturn("code");
        when(decryptedPayload.getString("email")).thenReturn("test@paypal.com");
        doReturn(decryptedPayload).when(AuthorizationRequest.class, "getDecryptedPayload", anyString(), anyString());

        final BraintreeFragment fragment = mMockFragmentBuilder.build();
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Intent intent = new Intent()
                        .setData(Uri.parse("com.braintreepayments.api.test.braintree://onetouch/v1/success?payloadEnc=3vsdKACxHUPPfEFpfI9DeJcPw4f%2Bj9Rp5fJjf%2B9h%2FN6GjoBaRQkIa9oUV2Vtm1I%2FiZqjZqd%2FXWQ56sts0iyl7eAVCfEvXHlpfrBg5e89JDINUUSAGAhTYmJWvoNm5YGxkSXmefLHhdvao8bIHZ26ExNL25oKS9E7RWgBtwOx%2BzChE3u0klAlgSN027ex7GSezjk5CsXMrns7%2BmcebLObQoZb3C1XjKik2m4HhSwXSdR5ygRkaRSVO5e1PVz0oiUBxpzGiubNb9aPrRtWvx%2FRwq3RSHNUIa4LuslgrxVx2WIa0isNKR3bBwzFcYClLbS6065Cs60Desg0BZSrudkwgSNJDwKnIzJM8FC1m4Xd2ASd63XnMzBh1RzbouAXqsrdIJFFVcVTrU4yO6mWTFqklw%3D%3D&payload=eyJ2ZXJzaW9uIjozLCJtc2dfR1VJRCI6IjRhMDcwYjhmLTgyMDQtNDczMC05Y2M0LWZiNWQ3ZjE3YWY3OCIsInJlc3BvbnNlX3R5cGUiOiJjb2RlIiwiZW52aXJvbm1lbnQiOiJtb2NrIiwiZXJyb3IiOm51bGx9&x-source=com.braintree.browserswitch"));
                PayPal.onActivityResult(fragment, Activity.RESULT_OK, intent);
                return null;
            }
        }).when(fragment).startActivity(any(Intent.class));

        mockStatic(TokenizationClient.class);
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                ((PaymentMethodNonceCallback) invocation.getArguments()[2]).success(new PayPalAccountNonce());
                return null;
            }
        }).when(TokenizationClient.class);
        TokenizationClient.tokenize(any(BraintreeFragment.class), any(PaymentMethodBuilder.class),
                any(PaymentMethodNonceCallback.class));

        PayPal.authorizeAccount(fragment);

        verify(fragment).postCallback(any(PayPalAccountNonce.class));
    }

    @Test
    public void authorizeAccount_doesNotCallCancelListenerWhenSuccessful() throws Exception {
        spy(AuthorizationRequest.class);
        doReturn(true).when(AuthorizationRequest.class, "isValidResponse", any(ContextInspector.class), anyString());

        JSONObject decryptedPayload = mock(JSONObject.class);
        when(decryptedPayload.getString("payment_code")).thenReturn("code");
        when(decryptedPayload.getString("email")).thenReturn("test@paypal.com");
        doReturn(decryptedPayload).when(AuthorizationRequest.class, "getDecryptedPayload", anyString(), anyString());

        final BraintreeFragment fragment = mMockFragmentBuilder.build();
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Intent intent = new Intent()
                        .setData(Uri.parse("com.braintreepayments.api.test.braintree://onetouch/v1/success?payloadEnc=3vsdKACxHUPPfEFpfI9DeJcPw4f%2Bj9Rp5fJjf%2B9h%2FN6GjoBaRQkIa9oUV2Vtm1I%2FiZqjZqd%2FXWQ56sts0iyl7eAVCfEvXHlpfrBg5e89JDINUUSAGAhTYmJWvoNm5YGxkSXmefLHhdvao8bIHZ26ExNL25oKS9E7RWgBtwOx%2BzChE3u0klAlgSN027ex7GSezjk5CsXMrns7%2BmcebLObQoZb3C1XjKik2m4HhSwXSdR5ygRkaRSVO5e1PVz0oiUBxpzGiubNb9aPrRtWvx%2FRwq3RSHNUIa4LuslgrxVx2WIa0isNKR3bBwzFcYClLbS6065Cs60Desg0BZSrudkwgSNJDwKnIzJM8FC1m4Xd2ASd63XnMzBh1RzbouAXqsrdIJFFVcVTrU4yO6mWTFqklw%3D%3D&payload=eyJ2ZXJzaW9uIjozLCJtc2dfR1VJRCI6IjRhMDcwYjhmLTgyMDQtNDczMC05Y2M0LWZiNWQ3ZjE3YWY3OCIsInJlc3BvbnNlX3R5cGUiOiJjb2RlIiwiZW52aXJvbm1lbnQiOiJtb2NrIiwiZXJyb3IiOm51bGx9&x-source=com.braintree.browserswitch"));
                PayPal.onActivityResult(fragment, Activity.RESULT_OK, intent);
                return null;
            }
        }).when(fragment).startActivity(any(Intent.class));

        mockStatic(TokenizationClient.class);
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                ((PaymentMethodNonceCallback) invocation.getArguments()[2]).success(new PayPalAccountNonce());
                return null;
            }
        }).when(TokenizationClient.class);
        TokenizationClient.tokenize(any(BraintreeFragment.class), any(PaymentMethodBuilder.class),
                any(PaymentMethodNonceCallback.class));

        PayPal.authorizeAccount(fragment);

        verify(fragment, never()).postCancelCallback(anyInt());
    }

    @Test
    public void authorizeAccount_callsCancelListenerWhenCanceled() {
        final BraintreeFragment fragment = mMockFragmentBuilder.build();
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Intent intent = new Intent()
                        .setData(Uri.parse("com.braintreepayments.api.test.braintree://onetouch/v1/cancel"));
                PayPal.onActivityResult(fragment, Activity.RESULT_OK, intent);
                return null;
            }
        }).when(fragment).startActivity(any(Intent.class));

        PayPal.authorizeAccount(fragment);

        verify(fragment).postCancelCallback(PayPal.PAYPAL_REQUEST_CODE);
    }

    @Test
    public void requestBillingAgreement_postsExceptionWhenAmountIsIncluded() {
        BraintreeFragment fragment = mMockFragmentBuilder.build();

        PayPal.requestBillingAgreement(fragment, new PayPalRequest("1"));

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(fragment).postCallback(captor.capture());
        assertTrue(captor.getValue() instanceof BraintreeException);
        assertEquals("There must be no amount specified for the Billing Agreement flow", captor.getValue().getMessage());
    }

    @Test
    public void requestBillingAgreement_startsBrowser() throws InvalidArgumentException {
        BraintreeFragment fragment = mMockFragmentBuilder
                .successResponse(stringFromFixture("paypal_hermes_billing_agreement_response.json"))
                .build();

        PayPal.requestBillingAgreement(fragment, new PayPalRequest());

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(fragment).startActivity(captor.capture());
        Intent intent = captor.getValue();

        assertEquals(Intent.ACTION_VIEW, intent.getAction());
        assertEquals("checkout.paypal.com", intent.getData().getHost());
        assertEquals("/one-touch-login-sandbox/index.html", intent.getData().getPath());
        assertEquals("create_payment_resource", intent.getData().getQueryParameter("action"));
        assertEquals("63cc461306c35080ce674a3372bffe1580b4130c7fd33d33968aa76bb93cdd06|created_at=2015-10-13T18:49:48.371382792+0000&merchant_id=dcpspy2brwdjr3qn&public_key=9wwrzqk3vr3t4nc8",
                intent.getData().getQueryParameter("authorization_fingerprint"));
        assertFalse(intent.getData().getQueryParameterNames().contains("amount"));
        assertFalse(intent.getData().getQueryParameterNames().contains("currency_iso_code"));
        assertEquals("false", intent.getData().getQueryParameter("experience_profile[address_override]"));
        assertEquals("false", intent.getData().getQueryParameter("experience_profile[no_shipping]"));
        assertEquals("dcpspy2brwdjr3qn", intent.getData().getQueryParameter("merchant_id"));
        assertEquals("com.braintreepayments.api.test.braintree://onetouch/v1/success", intent.getData().getQueryParameter("return_url"));
        assertEquals("com.braintreepayments.api.test.braintree://onetouch/v1/cancel", intent.getData().getQueryParameter("cancel_url"));
        assertTrue(intent.getBooleanExtra(BraintreeBrowserSwitchActivity.EXTRA_BROWSER_SWITCH, false));
    }

    @Test
    public void requestBillingAgreement_isSuccessful() {
        final BraintreeFragment fragment = mMockFragmentBuilder
                .successResponse(stringFromFixture("paypal_hermes_response.json"))
                .build();
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Intent intent = new Intent()
                        .setData(Uri.parse("com.braintreepayments.api.test.braintree://onetouch/v1/success?PayerID=HERMES-SANDBOX-PAYER-ID&paymentId=HERMES-SANDBOX-PAYMENT-ID&token=EC-HERMES-SANDBOX-EC-TOKEN"));
                PayPal.onActivityResult(fragment, Activity.RESULT_OK, intent);
                return null;
            }
        }).when(fragment).startActivity(any(Intent.class));

        mockStatic(TokenizationClient.class);
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                ((PaymentMethodNonceCallback) invocation.getArguments()[2]).success(new PayPalAccountNonce());
                return null;
            }
        }).when(TokenizationClient.class);
        TokenizationClient.tokenize(any(BraintreeFragment.class), any(PaymentMethodBuilder.class),
                any(PaymentMethodNonceCallback.class));

        PayPal.requestBillingAgreement(fragment, new PayPalRequest());

        verify(fragment).postCallback(any(PayPalAccountNonce.class));
    }

    @Test
    public void requestBillingAgreement_doesNotCallCancelListenerWhenSuccessful() {
        final BraintreeFragment fragment = mMockFragmentBuilder
                .successResponse(stringFromFixture("paypal_hermes_response.json"))
                .build();
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Intent intent = new Intent()
                        .setData(Uri.parse("com.braintreepayments.api.test.braintree://onetouch/v1/success?PayerID=HERMES-SANDBOX-PAYER-ID&paymentId=HERMES-SANDBOX-PAYMENT-ID&token=EC-HERMES-SANDBOX-EC-TOKEN"));
                PayPal.onActivityResult(fragment, Activity.RESULT_OK, intent);
                return null;
            }
        }).when(fragment).startActivity(any(Intent.class));

        mockStatic(TokenizationClient.class);
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                ((PaymentMethodNonceCallback) invocation.getArguments()[2]).success(new PayPalAccountNonce());
                return null;
            }
        }).when(TokenizationClient.class);
        TokenizationClient.tokenize(any(BraintreeFragment.class), any(PaymentMethodBuilder.class),
                any(PaymentMethodNonceCallback.class));

        PayPal.requestBillingAgreement(fragment, new PayPalRequest());

        verify(fragment, never()).postCancelCallback(anyInt());
    }

    @Test
    public void requestBillingAgreement_cancelUrlTriggersCancelListener() {
        final BraintreeFragment fragment = mMockFragmentBuilder
                .successResponse(stringFromFixture("paypal_hermes_response.json"))
                .build();
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Intent intent = new Intent()
                        .setData(Uri.parse("com.braintreepayments.api.test.braintree://onetouch/v1/cancel"));
                PayPal.onActivityResult(fragment, Activity.RESULT_OK, intent);
                return null;
            }
        }).when(fragment).startActivity(any(Intent.class));

        PayPal.requestBillingAgreement(fragment, new PayPalRequest());

        verify(fragment).postCancelCallback(PayPal.PAYPAL_REQUEST_CODE);
    }

    @Test
    public void requestOneTimePayment_postsExceptionWhenNoAmountIsSet() {
        BraintreeFragment fragment = mMockFragmentBuilder.build();

        PayPal.requestOneTimePayment(fragment, new PayPalRequest());

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(fragment).postCallback(captor.capture());
        assertTrue(captor.getValue() instanceof BraintreeException);
        assertEquals("An amount must be specified for the Single Payment flow.", captor.getValue().getMessage());
    }

    @Test
    public void requestOneTimePayment_defaultPostParamsIncludeCorrectValues() throws JSONException {
        BraintreeFragment fragment = mMockFragmentBuilder.build();

        PayPal.requestOneTimePayment(fragment, new PayPalRequest("1"));

        ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(fragment.getHttpClient()).post(pathCaptor.capture(), dataCaptor.capture(),
                any(HttpResponseCallback.class));
        assertTrue(pathCaptor.getValue().contains("/paypal_hermes/create_payment_resource"));

        JSONObject json = new JSONObject(dataCaptor.getValue());
        assertEquals("1", json.get("amount"));
        assertEquals(true, json.getJSONObject("experience_profile").get("no_shipping"));
        assertEquals(false, json.getJSONObject("experience_profile").get("address_override"));
    }

    @Test
    public void requestOneTimePayment_postParamsIncludeNoShipping() throws JSONException {
        BraintreeFragment fragment = mMockFragmentBuilder.build();

        PayPal.requestOneTimePayment(fragment, new PayPalRequest("1").shippingAddressRequired(false));

        ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(fragment.getHttpClient()).post(pathCaptor.capture(), dataCaptor.capture(),
                any(HttpResponseCallback.class));
        assertTrue(pathCaptor.getValue().contains("/paypal_hermes/create_payment_resource"));

        JSONObject json = new JSONObject(dataCaptor.getValue());
        assertEquals("1", json.get("amount"));
        assertEquals(true, json.getJSONObject("experience_profile").get("no_shipping"));
        assertEquals(false, json.getJSONObject("experience_profile").get("address_override"));
    }

    @Test
    public void requestOneTimePayment_postParamsIncludeAddressAndAddressOverride() throws JSONException {
        BraintreeFragment fragment = mMockFragmentBuilder.build();

        PostalAddress address = new PostalAddress()
                .streetAddress("123 Fake St.")
                .extendedAddress("Apt. v.0")
                .locality("Oakland")
                .region("CA")
                .postalCode("12345")
                .countryCodeAlpha2("US");
        PayPalRequest request = new PayPalRequest("3.43")
                .shippingAddressRequired(true)
                .shippingAddressOverride(address);

        PayPal.requestOneTimePayment(fragment, request);

        ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(fragment.getHttpClient()).post(pathCaptor.capture(), dataCaptor.capture(),
                any(HttpResponseCallback.class));
        assertTrue(pathCaptor.getValue().contains("/paypal_hermes/create_payment_resource"));

        JSONObject json = new JSONObject(dataCaptor.getValue());
        assertEquals("3.43", json.get("amount"));
        assertEquals("123 Fake St.", json.get("line1"));
        assertEquals("Apt. v.0", json.get("line2"));
        assertEquals("Oakland", json.get("city"));
        assertEquals("CA", json.get("state"));
        assertEquals("12345", json.get("postal_code"));
        assertEquals("US", json.get("country_code"));
        assertEquals(false, json.getJSONObject("experience_profile").get("no_shipping"));
        assertEquals(true, json.getJSONObject("experience_profile").get("address_override"));
    }

    @Test
    public void requestOneTimePayment_postParamsIncludeNoShippingAndAddressAndAddressOverride() throws JSONException {
        BraintreeFragment fragment = mMockFragmentBuilder.build();

        PostalAddress address = new PostalAddress()
                .streetAddress("123 Fake St.")
                .extendedAddress("Apt. v.0")
                .locality("Oakland")
                .region("CA")
                .postalCode("12345")
                .countryCodeAlpha2("US");
        PayPalRequest request = new PayPalRequest("3.43")
                .shippingAddressRequired(false)
                .shippingAddressOverride(address);

        PayPal.requestOneTimePayment(fragment, request);

        ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(fragment.getHttpClient()).post(pathCaptor.capture(), dataCaptor.capture(),
                any(HttpResponseCallback.class));
        assertTrue(pathCaptor.getValue().contains("/paypal_hermes/create_payment_resource"));

        JSONObject json = new JSONObject(dataCaptor.getValue());
        assertEquals("3.43", json.get("amount"));
        assertEquals("123 Fake St.", json.get("line1"));
        assertEquals("Apt. v.0", json.get("line2"));
        assertEquals("Oakland", json.get("city"));
        assertEquals("CA", json.get("state"));
        assertEquals("12345", json.get("postal_code"));
        assertEquals("US", json.get("country_code"));
        assertEquals(true, json.getJSONObject("experience_profile").get("no_shipping"));
        assertEquals(true, json.getJSONObject("experience_profile").get("address_override"));
    }

    @Test
    public void requestOneTimePayment_startsBrowser() {
        BraintreeFragment fragment = mMockFragmentBuilder
                .successResponse(stringFromFixture("paypal_hermes_response.json"))
                .build();

        PayPal.requestOneTimePayment(fragment, new PayPalRequest("1.00"));

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(fragment).startActivity(captor.capture());
        Intent intent = captor.getValue();

        assertEquals(Intent.ACTION_VIEW, intent.getAction());
        assertEquals("checkout.paypal.com", intent.getData().getHost());
        assertEquals("/one-touch-login-sandbox/index.html", intent.getData().getPath());
        assertEquals("create_payment_resource", intent.getData().getQueryParameter("action"));
        assertEquals("63cc461306c35080ce674a3372bffe1580b4130c7fd33d33968aa76bb93cdd06|created_at=2015-10-13T18:49:48.371382792+0000&merchant_id=dcpspy2brwdjr3qn&public_key=9wwrzqk3vr3t4nc8",
                intent.getData().getQueryParameter("authorization_fingerprint"));
        assertEquals("USD", intent.getData().getQueryParameter("currency_iso_code"));
        assertEquals("false", intent.getData().getQueryParameter("experience_profile[address_override]"));
        assertEquals("false", intent.getData().getQueryParameter("experience_profile[no_shipping]"));
        assertEquals("dcpspy2brwdjr3qn", intent.getData().getQueryParameter("merchant_id"));
        assertEquals("com.braintreepayments.api.test.braintree://onetouch/v1/success", intent.getData().getQueryParameter("return_url"));
        assertEquals("com.braintreepayments.api.test.braintree://onetouch/v1/cancel", intent.getData().getQueryParameter("cancel_url"));
        assertTrue(intent.getBooleanExtra(BraintreeBrowserSwitchActivity.EXTRA_BROWSER_SWITCH, false));
    }

    @Test
    public void requestOneTimePayment_isSuccessful() {
        final BraintreeFragment fragment = mMockFragmentBuilder
                .successResponse(stringFromFixture("paypal_hermes_response.json"))
                .build();
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Intent intent = new Intent()
                        .setData(Uri.parse("com.braintreepayments.api.test.braintree://onetouch/v1/success?PayerID=HERMES-SANDBOX-PAYER-ID&paymentId=HERMES-SANDBOX-PAYMENT-ID&token=EC-HERMES-SANDBOX-EC-TOKEN"));
                PayPal.onActivityResult(fragment, Activity.RESULT_OK, intent);
                return null;
            }
        }).when(fragment).startActivity(any(Intent.class));

        mockStatic(TokenizationClient.class);
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                ((PaymentMethodNonceCallback) invocation.getArguments()[2]).success(new PayPalAccountNonce());
                return null;
            }
        }).when(TokenizationClient.class);
        TokenizationClient.tokenize(any(BraintreeFragment.class), any(PaymentMethodBuilder.class),
                any(PaymentMethodNonceCallback.class));

        PayPal.requestOneTimePayment(fragment, new PayPalRequest("1"));

        verify(fragment).postCallback(any(PayPalAccountNonce.class));
    }

    @Test
    public void requestOneTimePayment_doesNotCallCancelListenerWhenSuccessful() {
        final BraintreeFragment fragment = mMockFragmentBuilder
                .successResponse(stringFromFixture("paypal_hermes_response.json"))
                .build();
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Intent intent = new Intent()
                        .setData(Uri.parse("com.braintreepayments.api.test.braintree://onetouch/v1/success?PayerID=HERMES-SANDBOX-PAYER-ID&paymentId=HERMES-SANDBOX-PAYMENT-ID&token=EC-HERMES-SANDBOX-EC-TOKEN"));
                PayPal.onActivityResult(fragment, Activity.RESULT_OK, intent);
                return null;
            }
        }).when(fragment).startActivity(any(Intent.class));

        mockStatic(TokenizationClient.class);
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                ((PaymentMethodNonceCallback) invocation.getArguments()[2]).success(new PayPalAccountNonce());
                return null;
            }
        }).when(TokenizationClient.class);
        TokenizationClient.tokenize(any(BraintreeFragment.class), any(PaymentMethodBuilder.class),
                any(PaymentMethodNonceCallback.class));

        PayPal.requestOneTimePayment(fragment, new PayPalRequest("1"));

        verify(fragment, never()).postCancelCallback(anyInt());
    }

    @Test
    public void checkout_cancelUrlTriggersCancelListener() {
        final BraintreeFragment fragment = mMockFragmentBuilder
                .successResponse(stringFromFixture("paypal_hermes_response.json"))
                .build();
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Intent intent = new Intent()
                        .setData(Uri.parse("com.braintreepayments.api.test.braintree://onetouch/v1/cancel"));
                PayPal.onActivityResult(fragment, Activity.RESULT_OK, intent);
                return null;
            }
        }).when(fragment).startActivity(any(Intent.class));

        PayPal.requestOneTimePayment(fragment, new PayPalRequest("1"));

        verify(fragment).postCancelCallback(PayPal.PAYPAL_REQUEST_CODE);
    }

    @Test
    public void onActivityResult_postsCancelWhenResultIsCanceled() {
        BraintreeFragment fragment = mMockFragmentBuilder.build();
        Intent intent = new Intent()
                .setData(Uri.parse("com.braintreepayments.api.test.braintree://onetouch/v1/cancel"));

        PayPal.onActivityResult(fragment, Activity.RESULT_OK, intent);

        verify(fragment).postCancelCallback(PayPal.PAYPAL_REQUEST_CODE);
    }

    @Test
    public void onActivityResult_postsCancelWhenIntentIsNull() {
        BraintreeFragment fragment = mMockFragmentBuilder.build();

        PayPal.onActivityResult(fragment, Activity.RESULT_OK, null);

        verify(fragment).postCancelCallback(PayPal.PAYPAL_REQUEST_CODE);
    }
}
