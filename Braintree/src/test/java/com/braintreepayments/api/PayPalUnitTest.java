package com.braintreepayments.api;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;

import com.braintreepayments.api.exceptions.BraintreeException;
import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.interfaces.PayPalApprovalCallback;
import com.braintreepayments.api.interfaces.PayPalApprovalHandler;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCallback;
import com.braintreepayments.api.internal.BraintreeSharedPreferences;
import com.braintreepayments.api.models.Authorization;
import com.braintreepayments.api.models.BraintreeRequestCodes;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.PayPalAccountBuilder;
import com.braintreepayments.api.models.PayPalAccountNonce;
import com.braintreepayments.api.models.PayPalProductAttributes;
import com.paypal.android.sdk.onetouch.core.PayPalLineItem;
import com.braintreepayments.api.models.PayPalRequest;
import com.braintreepayments.api.models.PaymentMethodBuilder;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.api.models.PostalAddress;
import com.braintreepayments.testutils.TestConfigurationBuilder;
import com.braintreepayments.testutils.TestConfigurationBuilder.TestPayPalConfigurationBuilder;
import com.paypal.android.sdk.onetouch.core.BillingAgreementRequest;
import com.paypal.android.sdk.onetouch.core.CheckoutRequest;
import com.paypal.android.sdk.onetouch.core.PayPalOneTouchCore;
import com.paypal.android.sdk.onetouch.core.Request;
import com.paypal.android.sdk.onetouch.core.config.Recipe;
import com.paypal.android.sdk.onetouch.core.enums.ResultType;

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
import org.robolectric.RuntimeEnvironment;
import org.skyscreamer.jsonassert.JSONAssert;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

import static androidx.appcompat.app.AppCompatActivity.RESULT_OK;
import static androidx.appcompat.app.AppCompatActivity.RESULT_CANCELED;

import static com.braintreepayments.api.BraintreePowerMockHelper.*;
import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.contains;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(RobolectricTestRunner.class)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*", "org.json.*", "javax.crypto.*" })
@PrepareForTest({ PayPal.class, Recipe.class, TokenizationClient.class, PayPalOneTouchCore.class })
public class PayPalUnitTest {

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
        when(authorization.getBearer()).thenReturn("authorization");
        when(authorization.toString()).thenReturn("authorization");

        Configuration configuration = new TestConfigurationBuilder()
                .withAnalytics()
                .paypal(new TestPayPalConfigurationBuilder(true)
                        .environment("offline")
                        .billingAgreementsEnabled(false))
                .buildConfiguration();

        mMockFragmentBuilder = new MockFragmentBuilder()
                .authorization(authorization)
                .configuration(configuration);
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
    public void requestBillingAgreement_startsBrowser() {
        BraintreeFragment fragment = mMockFragmentBuilder
                .successResponse(stringFromFixture("paypal_hermes_billing_agreement_response.json"))
                .build();

        PayPal.requestBillingAgreement(fragment, new PayPalRequest());

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(fragment).browserSwitch(eq(BraintreeRequestCodes.PAYPAL), captor.capture());
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
    }

    @Test
    public void requestBillingAgreement_startsBrowserWithPayPalCredit() {
        BraintreeFragment fragment = mMockFragmentBuilder
                .successResponse(stringFromFixture("paypal_hermes_billing_agreement_response.json"))
                .build();

        PayPal.requestBillingAgreement(fragment, new PayPalRequest().offerCredit(true));

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(fragment).browserSwitch(eq(BraintreeRequestCodes.PAYPAL), captor.capture());
        Intent intent = captor.getValue();

        assertEquals(Intent.ACTION_VIEW, intent.getAction());
        assertEquals("checkout.paypal.com", intent.getData().getHost());
        assertEquals("/one-touch-login-sandbox/index.html", intent.getData().getPath());
        assertEquals("create_payment_resource", intent.getData().getQueryParameter("action"));
        assertEquals("63cc461306c35080ce674a3372bffe1580b4130c7fd33d33968aa76bb93cdd06|created_at=2015-10-13T18:49:48.371382792+0000&merchant_id=dcpspy2brwdjr3qn&public_key=9wwrzqk3vr3t4nc8",
                intent.getData().getQueryParameter("authorization_fingerprint"));
        assertFalse(intent.getData().getQueryParameterNames().contains("amount"));
        assertEquals("true", intent.getData().getQueryParameter("offer_paypal_credit"));
        assertFalse(intent.getData().getQueryParameterNames().contains("currency_iso_code"));
        assertEquals("false", intent.getData().getQueryParameter("experience_profile[address_override]"));
        assertEquals("false", intent.getData().getQueryParameter("experience_profile[no_shipping]"));
        assertEquals("dcpspy2brwdjr3qn", intent.getData().getQueryParameter("merchant_id"));
        assertEquals("com.braintreepayments.api.test.braintree://onetouch/v1/success", intent.getData().getQueryParameter("return_url"));
        assertEquals("com.braintreepayments.api.test.braintree://onetouch/v1/cancel", intent.getData().getQueryParameter("cancel_url"));
    }

    @Test
    public void requestBillingAgreement_defaultPostParamsIncludeCorrectValues() throws JSONException {
        BraintreeFragment fragment = mMockFragmentBuilder.build();

        PayPal.requestBillingAgreement(fragment, new PayPalRequest());

        ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(fragment.getHttpClient()).post(contains("/paypal_hermes/setup_billing_agreement"), dataCaptor.capture(),
                any(HttpResponseCallback.class));

        JSONObject expected = new JSONObject()
                .put("client_key", "authorization")
                .put("return_url", "com.braintreepayments.api.braintree://onetouch/v1/success")
                .put("cancel_url", "com.braintreepayments.api.braintree://onetouch/v1/cancel")
                .put("experience_profile", new JSONObject()
                        .put("address_override", false)
                        .put("brand_name", "displayName")
                        .put("no_shipping", true))
                .put("offer_paypal_credit", false);
        JSONObject actual = new JSONObject(dataCaptor.getValue());
        JSONAssert.assertEquals(expected, actual, true);
    }

    @Test
    public void requestBillingAgreement_sendsPayPalCreditOfferedAnalyticsEvent() {
        BraintreeFragment fragment = mMockFragmentBuilder.build();

        PayPal.requestBillingAgreement(fragment, new PayPalRequest().offerCredit(true));

        verify(fragment).sendAnalyticsEvent("paypal.billing-agreement.credit.offered");
    }

    @Test
    public void requestBillingAgreement_containsOfferPayPalCreditParam() throws JSONException {
        BraintreeFragment fragment = mMockFragmentBuilder.build();

        PayPal.requestBillingAgreement(fragment, new PayPalRequest().offerCredit(true));

        ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(fragment.getHttpClient()).post(contains("/paypal_hermes/setup_billing_agreement"), dataCaptor.capture(),
                any(HttpResponseCallback.class));

        JSONObject json = new JSONObject(dataCaptor.getValue());
        assertTrue(json.getBoolean("offer_paypal_credit"));
    }

    @Test
    public void requestBillingAgreement_containsPayPalTwoFactorAuthParams() throws JSONException {
        BraintreeFragment fragment = mMockFragmentBuilder.build();

        PayPalProductAttributes productAttributes = new PayPalProductAttributes()
                .name("name")
                .chargePattern("chargePattern")
                .productCode("productCode");

        PayPal.requestBillingAgreement(fragment, new PayPalRequest().productAttributes(productAttributes));

        ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(fragment.getHttpClient()).post(contains("/paypal_hermes/setup_billing_agreement"), dataCaptor.capture(),
                any(HttpResponseCallback.class));

        JSONObject json = new JSONObject(dataCaptor.getValue());
        JSONObject productAttributesJSON = json.getJSONObject("product_attributes");
        assertEquals(productAttributesJSON.getString("name"), "name");
        assertEquals(productAttributesJSON.getString("product_code"), "productCode");
        assertEquals(productAttributesJSON.getString("charge_pattern"), "chargePattern");
    }

    @Test
    public void authorizeAccount_whenSuccessfulBrowserSwitch_sendsAnalyticsEvents() throws Exception {
        final BraintreeFragment fragment = mMockFragmentBuilder
                .successResponse(stringFromFixture("paypal_hermes_billing_agreement_response.json"))
                .build();
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                Intent intent = new Intent()
                        .setData(Uri.parse("com.braintreepayments.api.test.braintree://onetouch/v1/success?PayerID=HERMES-SANDBOX-PAYER-ID&paymentId=HERMES-SANDBOX-PAYMENT-ID&ba_token=EC-HERMES-SANDBOX-EC-TOKEN"));
                PayPal.onActivityResult(fragment, RESULT_OK, intent);
                return null;
            }
        }).when(fragment).browserSwitch(eq(BraintreeRequestCodes.PAYPAL), any(Intent.class));

        MockStaticTokenizationClient.mockTokenizeSuccess(new PayPalAccountNonce());

        PayPal.requestBillingAgreement(fragment, new PayPalRequest());

        verify(fragment).sendAnalyticsEvent("paypal.billing-agreement.selected");
        verify(fragment).sendAnalyticsEvent("paypal.billing-agreement.browser-switch.started");
        verify(fragment).sendAnalyticsEvent("paypal.billing-agreement.browser-switch.succeeded");
    }

    @Test
    public void requestBillingAgreement_whenSuccessfulBrowserSwitch_sendsAnalyticsEvents() {
        final BraintreeFragment fragment = mMockFragmentBuilder
                .successResponse(stringFromFixture("paypal_hermes_billing_agreement_response.json"))
                .build();
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                Intent intent = new Intent()
                        .setData(Uri.parse("com.braintreepayments.api.test.braintree://onetouch/v1/success?PayerID=HERMES-SANDBOX-PAYER-ID&paymentId=HERMES-SANDBOX-PAYMENT-ID&ba_token=EC-HERMES-SANDBOX-EC-TOKEN"));
                PayPal.onActivityResult(fragment, RESULT_OK, intent);
                return null;
            }
        }).when(fragment).browserSwitch(eq(BraintreeRequestCodes.PAYPAL), any(Intent.class));

        MockStaticTokenizationClient.mockTokenizeSuccess(new PayPalAccountNonce());

        PayPal.requestBillingAgreement(fragment, new PayPalRequest());

        verify(fragment).sendAnalyticsEvent("paypal.billing-agreement.selected");
        verify(fragment).sendAnalyticsEvent("paypal.billing-agreement.browser-switch.started");
        verify(fragment).sendAnalyticsEvent("paypal.billing-agreement.browser-switch.succeeded");
    }

    @Test
    public void requestBillingAgreement_paypalCreditReturnedInResponse() throws Exception {
        BraintreeFragment fragment = mMockFragmentBuilder
                .successResponse(stringFromFixture("paypal_hermes_billing_agreement_response.json"))
                .build();

        MockStaticTokenizationClient.mockTokenizeSuccess(PayPalAccountNonce.fromJson(stringFromFixture("payment_methods/paypal_account_response.json")));

        PayPal.requestBillingAgreement(fragment, new PayPalRequest().offerCredit(true), new PayPalApprovalHandler() {
            @Override
            public void handleApproval(Request request, PayPalApprovalCallback paypalApprovalCallback) {
                paypalApprovalCallback.onComplete(new Intent()
                        .setData(Uri.parse("com.braintreepayments.api.test.braintree://onetouch/v1/success?PayerID=HERMES-SANDBOX-PAYER-ID&paymentId=HERMES-SANDBOX-PAYMENT-ID&ba_token=EC-HERMES-SANDBOX-EC-TOKEN"))
                );
            }
        });

        ArgumentCaptor<PaymentMethodNonce> nonceCaptor = ArgumentCaptor.forClass(PaymentMethodNonce.class);
        verify(fragment).postCallback(nonceCaptor.capture());
        verify(fragment).sendAnalyticsEvent("paypal.credit.accepted");

        assertTrue(nonceCaptor.getValue() instanceof PayPalAccountNonce);
        PayPalAccountNonce payPalAccountNonce = (PayPalAccountNonce) nonceCaptor.getValue();
        assertNotNull(payPalAccountNonce.getCreditFinancing());
        assertEquals(18, payPalAccountNonce.getCreditFinancing().getTerm());
    }

    @Test
    public void requestBillingAgreement_doesNotCallCancelListenerWhenSuccessful() {
        final BraintreeFragment fragment = mMockFragmentBuilder
                .successResponse(stringFromFixture("paypal_hermes_response.json"))
                .build();
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                Intent intent = new Intent()
                        .setData(Uri.parse("com.braintreepayments.api.test.braintree://onetouch/v1/success?PayerID=HERMES-SANDBOX-PAYER-ID&paymentId=HERMES-SANDBOX-PAYMENT-ID&ba_token=EC-HERMES-SANDBOX-EC-TOKEN"));
                PayPal.onActivityResult(fragment, RESULT_OK, intent);
                return null;
            }
        }).when(fragment).startActivity(any(Intent.class));

        MockStaticTokenizationClient.mockTokenizeSuccess(new PayPalAccountNonce());

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
            public Object answer(InvocationOnMock invocation) {
                Intent intent = new Intent()
                        .setData(Uri.parse("com.braintreepayments.api.test.braintree://onetouch/v1/cancel"));
                PayPal.onActivityResult(fragment, RESULT_OK, intent);
                return null;
            }
        }).when(fragment).browserSwitch(eq(BraintreeRequestCodes.PAYPAL), any(Intent.class));

        PayPal.requestBillingAgreement(fragment, new PayPalRequest());

        verify(fragment).postCancelCallback(BraintreeRequestCodes.PAYPAL);
    }

    @Test
    public void requestBillingAgreement_persistsPayPalRequest() {
        BraintreeFragment braintreeFragment = mMockFragmentBuilder.build();
        SharedPreferences prefs = BraintreeSharedPreferences.getSharedPreferences(RuntimeEnvironment.application);

        PayPal.requestOneTimePayment(braintreeFragment, new PayPalRequest("1").intent(PayPalRequest.INTENT_SALE));

        assertNotNull(prefs.getString("com.braintreepayments.api.PayPal.PAYPAL_REQUEST_KEY", null));
    }

    @Test
    public void requestBillingAgreement_postParamsIncludeAddress() throws JSONException {
        BraintreeFragment fragment = mMockFragmentBuilder.build();

        PostalAddress address = new PostalAddress()
                .streetAddress("123 Fake St.")
                .extendedAddress("Apt. v.0")
                .locality("Oakland")
                .region("CA")
                .postalCode("12345")
                .countryCodeAlpha2("US");
        PayPalRequest request = new PayPalRequest()
                .shippingAddressRequired(true)
                .shippingAddressOverride(address);

        PayPal.requestBillingAgreement(fragment, request);

        ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(fragment.getHttpClient()).post(pathCaptor.capture(), dataCaptor.capture(),
                any(HttpResponseCallback.class));
        assertTrue(pathCaptor.getValue().contains("/paypal_hermes/setup_billing_agreement"));

        JSONObject json = new JSONObject(dataCaptor.getValue());
        JSONObject shippingAddress = json.getJSONObject("shipping_address");

        assertEquals("123 Fake St.", shippingAddress.get("line1"));
        assertEquals("Apt. v.0", shippingAddress.get("line2"));
        assertEquals("Oakland", shippingAddress.get("city"));
        assertEquals("CA", shippingAddress.get("state"));
        assertEquals("12345", shippingAddress.get("postal_code"));
        assertEquals("US", shippingAddress.get("country_code"));
    }

    @Test
    public void requestBillingAgreement_whenEditable_postsAddressOverrideFalse() throws JSONException {
        BraintreeFragment fragment = mMockFragmentBuilder.build();

        PostalAddress address = new PostalAddress()
                .streetAddress("123 Fake St.")
                .extendedAddress("Apt. v.0")
                .locality("Oakland")
                .region("CA")
                .postalCode("12345")
                .countryCodeAlpha2("US");
        PayPalRequest request = new PayPalRequest()
                .shippingAddressEditable(true)
                .shippingAddressOverride(address);

        PayPal.requestBillingAgreement(fragment, request);

        ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(fragment.getHttpClient()).post(pathCaptor.capture(), dataCaptor.capture(),
                any(HttpResponseCallback.class));
        assertTrue(pathCaptor.getValue().contains("/paypal_hermes/setup_billing_agreement"));

        JSONObject json = new JSONObject(dataCaptor.getValue());
        JSONObject shippingAddress = json.getJSONObject("shipping_address");
        JSONObject experienceProfile = json.getJSONObject("experience_profile");

        assertTrue(shippingAddress.length() > 0);
        assertEquals(false, experienceProfile.get("address_override"));
    }

    @Test
    public void requestBillingAgreement_whenShippingRequired_postsNoShippingTrue() throws JSONException {
        BraintreeFragment fragment = mMockFragmentBuilder.build();
        PayPalRequest request = new PayPalRequest()
                .shippingAddressRequired(true);

        PayPal.requestBillingAgreement(fragment, request);

        ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(fragment.getHttpClient()).post(pathCaptor.capture(), dataCaptor.capture(),
                any(HttpResponseCallback.class));
        assertTrue(pathCaptor.getValue().contains("/paypal_hermes/setup_billing_agreement"));

        JSONObject json = new JSONObject(dataCaptor.getValue());
        JSONObject experienceProfile = json.getJSONObject("experience_profile");

        assertEquals(false, experienceProfile.get("no_shipping"));
    }

    @Test
    public void requestBillingAgreement_whenShippingRequiredFalse_postsNoShippingFalse() throws JSONException {
        BraintreeFragment fragment = mMockFragmentBuilder.build();
        PayPalRequest request = new PayPalRequest();

        PayPal.requestBillingAgreement(fragment, request);

        ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(fragment.getHttpClient()).post(pathCaptor.capture(), dataCaptor.capture(),
                any(HttpResponseCallback.class));
        assertTrue(pathCaptor.getValue().contains("/paypal_hermes/setup_billing_agreement"));

        JSONObject json = new JSONObject(dataCaptor.getValue());
        JSONObject experienceProfile = json.getJSONObject("experience_profile");

        assertEquals(true, experienceProfile.get("no_shipping"));
    }

    @Test
    public void requestBillingAgreement_whenEditableFalse_postsAddressOverrideTrue() throws JSONException {
        BraintreeFragment fragment = mMockFragmentBuilder.build();

        PostalAddress address = new PostalAddress()
                .streetAddress("123 Fake St.")
                .extendedAddress("Apt. v.0")
                .locality("Oakland")
                .region("CA")
                .postalCode("12345")
                .countryCodeAlpha2("US");
        PayPalRequest request = new PayPalRequest()
                .shippingAddressOverride(address);

        PayPal.requestBillingAgreement(fragment, request);

        ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(fragment.getHttpClient()).post(pathCaptor.capture(), dataCaptor.capture(),
                any(HttpResponseCallback.class));
        assertTrue(pathCaptor.getValue().contains("/paypal_hermes/setup_billing_agreement"));

        JSONObject json = new JSONObject(dataCaptor.getValue());
        JSONObject shippingAddress = json.getJSONObject("shipping_address");
        JSONObject experienceProfile = json.getJSONObject("experience_profile");

        assertTrue(shippingAddress.length() > 0);
        assertEquals(true, experienceProfile.get("address_override"));
    }

    @Test
    public void requestBillingAgreement_whenMerchantAccountIdPresent_postsParamsIncludeMerchantAccountId() throws JSONException {
        BraintreeFragment fragment = mMockFragmentBuilder.build();

        PayPalRequest request = new PayPalRequest()
                .merchantAccountId("merchant_account_id");

        PayPal.requestBillingAgreement(fragment, request);

        ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(fragment.getHttpClient()).post(pathCaptor.capture(), dataCaptor.capture(),
                any(HttpResponseCallback.class));
        assertTrue(pathCaptor.getValue().contains("/paypal_hermes/setup_billing_agreement"));

        JSONObject json = new JSONObject(dataCaptor.getValue());
        String merchantAccountId = json.getString("merchant_account_id");

        assertEquals("merchant_account_id", merchantAccountId);
    }

    @Test
    public void requestBillingAgreement_whenMerchantAccountIdNotPresent_postsDoNotIncludeMerchantAccountId() throws JSONException {
        BraintreeFragment fragment = mMockFragmentBuilder.build();

        PayPalRequest request = new PayPalRequest();

        PayPal.requestBillingAgreement(fragment, request);

        ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(fragment.getHttpClient()).post(pathCaptor.capture(), dataCaptor.capture(),
                any(HttpResponseCallback.class));
        assertTrue(pathCaptor.getValue().contains("/paypal_hermes/setup_billing_agreement"));

        JSONObject json = new JSONObject(dataCaptor.getValue());

        assertFalse(json.has("merchant_account_id"));
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

    @Test(timeout = 1000)
    public void requestOneTimePayment_customHandlerIsCalledCorrectly() throws InterruptedException {
        BraintreeFragment fragment = mMockFragmentBuilder
                .successResponse(stringFromFixture("paypal_hermes_response.json"))
                .build();

        final CountDownLatch latch = new CountDownLatch(1);
        PayPal.requestOneTimePayment(fragment, new PayPalRequest("1"), new PayPalApprovalHandler() {
            @Override
            public void handleApproval(Request request, PayPalApprovalCallback paypalApprovalCallback) {
                latch.countDown();
            }
        });

        latch.await();
    }

    @Test
    public void requestOneTimePayment_customHandlerCancelCallbackIsInvoked() {
        BraintreeFragment fragment = mMockFragmentBuilder
                .successResponse(stringFromFixture("paypal_hermes_response.json"))
                .build();

        PayPal.requestOneTimePayment(fragment, new PayPalRequest("1"), new PayPalApprovalHandler() {
            @Override
            public void handleApproval(Request request, PayPalApprovalCallback paypalApprovalCallback) {
                paypalApprovalCallback.onCancel();
            }
        });

        verify(fragment).postCancelCallback(BraintreeRequestCodes.PAYPAL);
    }

    @Test
    public void requestOneTimePayment_paypalCreditReturnedInResponse() throws Exception {
        BraintreeFragment fragment = mMockFragmentBuilder
                .successResponse(stringFromFixture("paypal_hermes_response.json"))
                .build();

        MockStaticTokenizationClient.mockTokenizeSuccess(PayPalAccountNonce.fromJson(stringFromFixture("payment_methods/paypal_account_response.json")));

        PayPal.requestOneTimePayment(fragment, new PayPalRequest("1").offerCredit(true), new PayPalApprovalHandler() {
            @Override
            public void handleApproval(Request request, PayPalApprovalCallback paypalApprovalCallback) {
                paypalApprovalCallback.onComplete(new Intent()
                        .setData(Uri.parse("com.braintreepayments.demo.braintree://onetouch/v1/success?PayerID=HERMES-SANDBOX-PAYER-ID&paymentId=HERMES-SANDBOX-PAYMENT-ID&token=EC-HERMES-SANDBOX-EC-TOKEN"))
                );
            }
        });

        ArgumentCaptor<PaymentMethodNonce> nonceCaptor = ArgumentCaptor.forClass(PaymentMethodNonce.class);
        verify(fragment).postCallback(nonceCaptor.capture());
        verify(fragment).sendAnalyticsEvent("paypal.credit.accepted");

        assertTrue(nonceCaptor.getValue() instanceof PayPalAccountNonce);
        PayPalAccountNonce payPalAccountNonce = (PayPalAccountNonce) nonceCaptor.getValue();
        assertNotNull(payPalAccountNonce.getCreditFinancing());
        assertEquals(18, payPalAccountNonce.getCreditFinancing().getTerm());
    }

    @Test
    public void requestOneTimePayment_customHandlerSuccessCallbackIsInvoked() {
        BraintreeFragment fragment = mMockFragmentBuilder
                .successResponse(stringFromFixture("paypal_hermes_response.json"))
                .build();

        MockStaticTokenizationClient.mockTokenizeSuccess(new PayPalAccountNonce());

        PayPal.requestOneTimePayment(fragment, new PayPalRequest("1"), new PayPalApprovalHandler() {
            @Override
            public void handleApproval(Request request, PayPalApprovalCallback paypalApprovalCallback) {
                paypalApprovalCallback.onComplete(new Intent()
                        .setData(Uri.parse("com.braintreepayments.demo.braintree://onetouch/v1/success?PayerID=HERMES-SANDBOX-PAYER-ID&paymentId=HERMES-SANDBOX-PAYMENT-ID&token=EC-HERMES-SANDBOX-EC-TOKEN"))
                );
            }
        });

        ArgumentCaptor<PaymentMethodNonce> nonceCaptor = ArgumentCaptor.forClass(PaymentMethodNonce.class);
        verify(fragment).postCallback(nonceCaptor.capture());

        assertTrue(nonceCaptor.getValue() instanceof PayPalAccountNonce);
    }

    @Test
    public void requestOneTimePayment_sendsPayPalCreditOfferedAnalyticsEvent() {
        BraintreeFragment fragment = mMockFragmentBuilder.build();

        PayPal.requestOneTimePayment(fragment, new PayPalRequest("1").offerCredit(true));

        verify(fragment).sendAnalyticsEvent("paypal.single-payment.credit.offered");
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

        JSONObject actual = new JSONObject(dataCaptor.getValue());
        JSONObject expected = new JSONObject()
                .put("client_key", "authorization")
                .put("return_url", "com.braintreepayments.api.braintree://onetouch/v1/success")
                .put("cancel_url", "com.braintreepayments.api.braintree://onetouch/v1/cancel")
                .put("amount", "1")
                .put("experience_profile", new JSONObject()
                        .put("address_override", false)
                        .put("brand_name", "displayName")
                        .put("no_shipping", true))
                .put("intent", "authorize")
                .put("offer_paypal_credit", false);

        JSONAssert.assertEquals(expected, actual, true);
    }

    @Test
    public void requestOneTimePayment_containsOfferPayPalCreditParam() throws JSONException {
        BraintreeFragment fragment = mMockFragmentBuilder.build();

        PayPal.requestOneTimePayment(fragment, new PayPalRequest("1").offerCredit(true));

        ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(fragment.getHttpClient()).post(contains("/paypal_hermes/create_payment_resource"),
                dataCaptor.capture(), any(HttpResponseCallback.class));

        JSONObject json = new JSONObject(dataCaptor.getValue());
        assertTrue(json.getBoolean("offer_paypal_credit"));
    }

    @Test
    public void requestOneTimePayment_containsLineItemParams() throws JSONException {
        BraintreeFragment fragment = mMockFragmentBuilder.build();

        PayPalRequest request = new PayPalRequest("2");
        ArrayList<PayPalLineItem> lineItems = new ArrayList<PayPalLineItem>();
        lineItems.add(new PayPalLineItem(PayPalLineItem.KIND_DEBIT, "An Item", "1", "2"));
        request.lineItems(lineItems);
        PayPal.requestOneTimePayment(fragment, request);

        ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(fragment.getHttpClient()).post(contains("/paypal_hermes/create_payment_resource"),
                dataCaptor.capture(), any(HttpResponseCallback.class));

        JSONObject json = new JSONObject(dataCaptor.getValue());
        assertNotNull(json.getJSONArray("line_items"));

        JSONObject itemJson = json.getJSONArray("line_items").getJSONObject(0);
        assertEquals("debit", itemJson.getString("kind"));
        assertEquals("An Item", itemJson.getString("name"));
        assertEquals("1", itemJson.getString("quantity"));
        assertEquals("2", itemJson.getString("unit_amount"));
    }

    @Test
    public void requestOneTimePayment_displayName_canBeSet() throws JSONException {
        BraintreeFragment fragment = mMockFragmentBuilder.build();

        PayPal.requestOneTimePayment(fragment, new PayPalRequest("1").displayName("Test Name"));

        ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(fragment.getHttpClient()).post(contains("/paypal_hermes/create_payment_resource"), dataCaptor.capture(),
                any(HttpResponseCallback.class));

        JSONObject json = new JSONObject(dataCaptor.getValue());
        assertEquals("Test Name", json.getJSONObject("experience_profile").get("brand_name"));
    }

    @Test
    public void requestOneTimePayment_intent_canBeSetToSale() throws JSONException {
        BraintreeFragment fragment = mMockFragmentBuilder.build();

        PayPal.requestOneTimePayment(fragment, new PayPalRequest("1").intent(PayPalRequest.INTENT_SALE));

        ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(fragment.getHttpClient()).post(contains("/paypal_hermes/create_payment_resource"), dataCaptor.capture(),
                any(HttpResponseCallback.class));

        JSONObject json = new JSONObject(dataCaptor.getValue());
        assertEquals("1", json.get("amount"));
        assertEquals(true, json.getJSONObject("experience_profile").get("no_shipping"));
        assertEquals(false, json.getJSONObject("experience_profile").get("address_override"));
        assertEquals(PayPalRequest.INTENT_SALE, json.get("intent"));
    }

    @Test
    public void requestOneTimePayment_intent_canBeSetToOrder() throws JSONException {
        BraintreeFragment fragment = mMockFragmentBuilder.build();

        PayPal.requestOneTimePayment(fragment, new PayPalRequest("1").intent(PayPalRequest.INTENT_ORDER));

        ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(fragment.getHttpClient()).post(contains("/paypal_hermes/create_payment_resource"), dataCaptor.capture(),
                any(HttpResponseCallback.class));

        JSONObject json = new JSONObject(dataCaptor.getValue());
        assertEquals("1", json.get("amount"));
        assertEquals(true, json.getJSONObject("experience_profile").get("no_shipping"));
        assertEquals(false, json.getJSONObject("experience_profile").get("address_override"));
        assertEquals(PayPalRequest.INTENT_ORDER, json.get("intent"));
    }

    @Test
    public void requestOneTimePayment_landingPageType_canBeSetToBilling() throws JSONException {
        BraintreeFragment fragment = mMockFragmentBuilder.build();

        PayPal.requestOneTimePayment(fragment, new PayPalRequest("1").landingPageType(PayPalRequest.LANDING_PAGE_TYPE_BILLING));

        ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(fragment.getHttpClient()).post(contains("/paypal_hermes/create_payment_resource"), dataCaptor.capture(),
                any(HttpResponseCallback.class));

        JSONObject json = new JSONObject(dataCaptor.getValue());
        assertEquals("1", json.get("amount"));
        assertEquals(true, json.getJSONObject("experience_profile").get("no_shipping"));
        assertEquals(false, json.getJSONObject("experience_profile").get("address_override"));
        assertEquals(PayPalRequest.LANDING_PAGE_TYPE_BILLING, json.getJSONObject("experience_profile").get("landing_page_type"));
    }

    @Test
    public void requestOneTimePayment_landingPageType_canBeSetToLogin() throws JSONException {
        BraintreeFragment fragment = mMockFragmentBuilder.build();

        PayPal.requestOneTimePayment(fragment, new PayPalRequest("1").landingPageType(PayPalRequest.LANDING_PAGE_TYPE_LOGIN));

        ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(fragment.getHttpClient()).post(contains("/paypal_hermes/create_payment_resource"), dataCaptor.capture(),
                any(HttpResponseCallback.class));

        JSONObject json = new JSONObject(dataCaptor.getValue());
        assertEquals("1", json.get("amount"));
        assertEquals(true, json.getJSONObject("experience_profile").get("no_shipping"));
        assertEquals(false, json.getJSONObject("experience_profile").get("address_override"));
        assertEquals(PayPalRequest.LANDING_PAGE_TYPE_LOGIN, json.getJSONObject("experience_profile").get("landing_page_type"));
    }

    @Test
    public void requestOneTimePayment_userAction_setsUserActionToBlankStringonDefault() {
        BraintreeFragment fragment = mMockFragmentBuilder
                .successResponse(stringFromFixture("paypal_hermes_response.json"))
                .build();

        PayPal.requestOneTimePayment(fragment, new PayPalRequest("1").userAction(PayPalRequest.USER_ACTION_DEFAULT));

        ArgumentCaptor<Intent> dataCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(fragment).browserSwitch(eq(BraintreeRequestCodes.PAYPAL), dataCaptor.capture());

        Uri uri = dataCaptor.getValue().getData();
        assertEquals("", uri.getQueryParameter("useraction"));
    }

    @Test
    public void requestOneTimePayment_userAction_canBeSetToCommit() {
        BraintreeFragment fragment = mMockFragmentBuilder
                .successResponse(stringFromFixture("paypal_hermes_response.json"))
                .build();

        PayPal.requestOneTimePayment(fragment, new PayPalRequest("1").userAction(PayPalRequest.USER_ACTION_COMMIT));

        ArgumentCaptor<Intent> dataCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(fragment).browserSwitch(eq(BraintreeRequestCodes.PAYPAL), dataCaptor.capture());

        Uri uri = dataCaptor.getValue().getData();
        assertEquals("commit", uri.getQueryParameter("useraction"));
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
    public void requestOneTimePayment_postParamsIncludeAddress() throws JSONException {
        BraintreeFragment fragment = mMockFragmentBuilder.build();

        PostalAddress address = new PostalAddress()
                .streetAddress("123 Fake St.")
                .extendedAddress("Apt. v.0")
                .locality("Oakland")
                .region("CA")
                .postalCode("12345")
                .countryCodeAlpha2("US");
        PayPalRequest request = new PayPalRequest("3.43")
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
    }

    @Test
    public void requestOnetimePayment_whenShippingRequired_postsNoShippingFalse() throws JSONException {
        BraintreeFragment fragment = mMockFragmentBuilder.build();

        PayPalRequest request = new PayPalRequest("3.43")
                .shippingAddressRequired(true);

        PayPal.requestOneTimePayment(fragment, request);

        ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(fragment.getHttpClient()).post(pathCaptor.capture(), dataCaptor.capture(),
                any(HttpResponseCallback.class));
        assertTrue(pathCaptor.getValue().contains("/paypal_hermes/create_payment_resource"));

        JSONObject json = new JSONObject(dataCaptor.getValue());
        JSONObject experienceProfile = json.getJSONObject("experience_profile");

        assertFalse(experienceProfile.getBoolean("no_shipping"));
    }

    @Test
    public void requestOnetimePayment_whenShippingRequiredFalse_postsNoShippingTrue() throws JSONException {
        BraintreeFragment fragment = mMockFragmentBuilder.build();

        PayPalRequest request = new PayPalRequest("3.43")
                .shippingAddressRequired(false);

        PayPal.requestOneTimePayment(fragment, request);

        ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(fragment.getHttpClient()).post(pathCaptor.capture(), dataCaptor.capture(),
                any(HttpResponseCallback.class));
        assertTrue(pathCaptor.getValue().contains("/paypal_hermes/create_payment_resource"));

        JSONObject json = new JSONObject(dataCaptor.getValue());
        JSONObject experienceProfile = json.getJSONObject("experience_profile");

        assertTrue(experienceProfile.getBoolean("no_shipping"));
    }

    @Test
    public void requestOneTimePayment_whenEditable_postAddressOverrideFalse() throws JSONException {
        BraintreeFragment fragment = mMockFragmentBuilder.build();

        PostalAddress address = new PostalAddress()
                .streetAddress("123 Fake St.")
                .extendedAddress("Apt. v.0")
                .locality("Oakland")
                .region("CA")
                .postalCode("12345")
                .countryCodeAlpha2("US");
        PayPalRequest request = new PayPalRequest("3.43")
                .shippingAddressEditable(true)
                .shippingAddressOverride(address);

        PayPal.requestOneTimePayment(fragment, request);

        ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(fragment.getHttpClient()).post(pathCaptor.capture(), dataCaptor.capture(),
                any(HttpResponseCallback.class));
        assertTrue(pathCaptor.getValue().contains("/paypal_hermes/create_payment_resource"));

        JSONObject json = new JSONObject(dataCaptor.getValue());
        assertEquals("US", json.get("country_code"));
        assertEquals(false, json.getJSONObject("experience_profile").get("address_override"));
    }

    @Test
    public void requestOneTimePayment_whenMerchantAccountIdPresent_postIncludeMerchantAccountId() throws JSONException {
        BraintreeFragment fragment = mMockFragmentBuilder.build();

        PayPalRequest request = new PayPalRequest("3.43")
                .merchantAccountId("merchant_account_id");

        PayPal.requestOneTimePayment(fragment, request);

        ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(fragment.getHttpClient()).post(pathCaptor.capture(), dataCaptor.capture(),
                any(HttpResponseCallback.class));
        assertTrue(pathCaptor.getValue().contains("/paypal_hermes/create_payment_resource"));

        JSONObject json = new JSONObject(dataCaptor.getValue());
        String merchantAccountId = json.getString("merchant_account_id");

        assertEquals("merchant_account_id", merchantAccountId);
    }

    @Test
    public void requestOneTimePayment_whenMerchantAccountIdNotPresent_postDoesNotIncludeMerchantAccountId() throws JSONException {
        BraintreeFragment fragment = mMockFragmentBuilder.build();

        PayPalRequest request = new PayPalRequest("3.43");

        PayPal.requestOneTimePayment(fragment, request);

        ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(fragment.getHttpClient()).post(pathCaptor.capture(), dataCaptor.capture(),
                any(HttpResponseCallback.class));
        assertTrue(pathCaptor.getValue().contains("/paypal_hermes/create_payment_resource"));

        JSONObject json = new JSONObject(dataCaptor.getValue());

        assertFalse(json.has("merchant_account_id"));
    }

    @Test
    public void requestOneTimePayment_whenEditableFalse_postAddressOverrideTrue() throws JSONException {
        BraintreeFragment fragment = mMockFragmentBuilder.build();

        PostalAddress address = new PostalAddress()
                .streetAddress("123 Fake St.")
                .extendedAddress("Apt. v.0")
                .locality("Oakland")
                .region("CA")
                .postalCode("12345")
                .countryCodeAlpha2("US");
        PayPalRequest request = new PayPalRequest("3.43")
                .shippingAddressOverride(address);

        PayPal.requestOneTimePayment(fragment, request);

        ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(fragment.getHttpClient()).post(pathCaptor.capture(), dataCaptor.capture(),
                any(HttpResponseCallback.class));
        assertTrue(pathCaptor.getValue().contains("/paypal_hermes/create_payment_resource"));

        JSONObject json = new JSONObject(dataCaptor.getValue());
        assertEquals("US", json.get("country_code"));
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
        verify(fragment).browserSwitch(eq(BraintreeRequestCodes.PAYPAL), captor.capture());
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
    }

    @Test
    public void requestOneTimePayment_startsBrowserWithPayPalCredit() {
        BraintreeFragment fragment = mMockFragmentBuilder
                .successResponse(stringFromFixture("paypal_hermes_response.json"))
                .build();

        PayPal.requestOneTimePayment(fragment, new PayPalRequest("1.00").offerCredit(true));

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(fragment).browserSwitch(eq(BraintreeRequestCodes.PAYPAL), captor.capture());
        Intent intent = captor.getValue();

        assertEquals(Intent.ACTION_VIEW, intent.getAction());
        assertEquals("checkout.paypal.com", intent.getData().getHost());
        assertEquals("/one-touch-login-sandbox/index.html", intent.getData().getPath());
        assertEquals("create_payment_resource", intent.getData().getQueryParameter("action"));
        assertEquals("63cc461306c35080ce674a3372bffe1580b4130c7fd33d33968aa76bb93cdd06|created_at=2015-10-13T18:49:48.371382792+0000&merchant_id=dcpspy2brwdjr3qn&public_key=9wwrzqk3vr3t4nc8",
                intent.getData().getQueryParameter("authorization_fingerprint"));
        assertEquals("USD", intent.getData().getQueryParameter("currency_iso_code"));
        assertEquals("true", intent.getData().getQueryParameter("offer_paypal_credit"));
        assertEquals("false", intent.getData().getQueryParameter("experience_profile[address_override]"));
        assertEquals("false", intent.getData().getQueryParameter("experience_profile[no_shipping]"));
        assertEquals("dcpspy2brwdjr3qn", intent.getData().getQueryParameter("merchant_id"));
        assertEquals("com.braintreepayments.api.test.braintree://onetouch/v1/success", intent.getData().getQueryParameter("return_url"));
        assertEquals("com.braintreepayments.api.test.braintree://onetouch/v1/cancel", intent.getData().getQueryParameter("cancel_url"));
    }

    @Test
    public void requestOneTimePayment_isSuccessful() {
        final BraintreeFragment fragment = mMockFragmentBuilder
                .successResponse(stringFromFixture("paypal_hermes_response.json"))
                .build();
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                Intent intent = new Intent()
                        .setData(Uri.parse("com.braintreepayments.api.test.braintree://onetouch/v1/success?PayerID=HERMES-SANDBOX-PAYER-ID&paymentId=HERMES-SANDBOX-PAYMENT-ID&token=EC-HERMES-SANDBOX-EC-TOKEN"));
                PayPal.onActivityResult(fragment, RESULT_OK, intent);
                return null;
            }
        }).when(fragment).browserSwitch(eq(BraintreeRequestCodes.PAYPAL), any(Intent.class));

        MockStaticTokenizationClient.mockTokenizeSuccess(new PayPalAccountNonce());

        PayPal.requestOneTimePayment(fragment, new PayPalRequest("1"));

        verify(fragment).postCallback(any(PayPalAccountNonce.class));

        SharedPreferences prefs = BraintreeSharedPreferences.getSharedPreferences(RuntimeEnvironment.application);
        assertNull(prefs.getString("com.braintreepayments.api.PayPal.REQUEST_KEY", null));
        assertNull(prefs.getString("com.braintreepayments.api.PayPal.REQUEST_TYPE_KEY", null));
    }

    @Test
    public void requestOneTimePayment_whenSuccessfulAppSwitchStart_sendsAnalyticsEvents() {
        MockStaticPayPalOneTouch.getStartIntent("app-switch");

        final BraintreeFragment fragment = mMockFragmentBuilder
                .successResponse(stringFromFixture("paypal_hermes_response.json"))
                .build();

        PayPal.requestOneTimePayment(fragment, new PayPalRequest("1"));

        verify(fragment).sendAnalyticsEvent("paypal.single-payment.selected");
        verify(fragment).sendAnalyticsEvent("paypal.single-payment.app-switch.started");
    }

    @Test
    public void requestOneTimePayment_whenBrowserSwitchSuccessful_sendsAnalyticsEvents() {
        MockStaticTokenizationClient.mockTokenizeSuccess(new PayPalAccountNonce());

        final BraintreeFragment fragment = mMockFragmentBuilder
                .successResponse(stringFromFixture("paypal_hermes_response.json"))
                .build();
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                Intent intent = new Intent()
                        .setData(Uri.parse("com.braintreepayments.api.test.braintree://onetouch/v1/success?PayerID=HERMES-SANDBOX-PAYER-ID&paymentId=HERMES-SANDBOX-PAYMENT-ID&token=EC-HERMES-SANDBOX-EC-TOKEN"));
                PayPal.onActivityResult(fragment, RESULT_OK, intent);
                return null;
            }
        }).when(fragment).browserSwitch(eq(BraintreeRequestCodes.PAYPAL), any(Intent.class));

        PayPal.requestOneTimePayment(fragment, new PayPalRequest("1"));

        verify(fragment).sendAnalyticsEvent("paypal.single-payment.selected");
        verify(fragment).sendAnalyticsEvent("paypal.single-payment.browser-switch.started");
        verify(fragment).sendAnalyticsEvent("paypal.single-payment.browser-switch.succeeded");
    }

    @Test
    public void requestOneTimePayment_containsPaymentIntent() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final BraintreeFragment fragment = mMockFragmentBuilder
                .successResponse(stringFromFixture("paypal_hermes_response.json"))
                .build();
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                Intent intent = new Intent()
                        .setData(Uri.parse("com.braintreepayments.api.test.braintree://onetouch/v1/success?PayerID=HERMES-SANDBOX-PAYER-ID&paymentId=HERMES-SANDBOX-PAYMENT-ID&token=EC-HERMES-SANDBOX-EC-TOKEN"));
                PayPal.onActivityResult(fragment, RESULT_OK, intent);
                return null;
            }
        }).when(fragment).browserSwitch(eq(BraintreeRequestCodes.PAYPAL), any(Intent.class));

        mockStatic(TokenizationClient.class);
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                PayPalAccountBuilder payPalAccountBuilder = (PayPalAccountBuilder) invocation.getArguments()[1];
                JSONObject payload = new JSONObject(payPalAccountBuilder.build());
                assertEquals(PayPalRequest.INTENT_SALE, payload.getJSONObject("paypalAccount").getString("intent"));
                ((PaymentMethodNonceCallback) invocation.getArguments()[2]).success(new PayPalAccountNonce());
                latch.countDown();
                return null;
            }
        }).when(TokenizationClient.class);
        TokenizationClient.tokenize(any(BraintreeFragment.class), any(PaymentMethodBuilder.class),
                any(PaymentMethodNonceCallback.class));
        PayPal.requestOneTimePayment(fragment, new PayPalRequest("1").intent(PayPalRequest.INTENT_SALE));

        SharedPreferences prefs = BraintreeSharedPreferences.getSharedPreferences(RuntimeEnvironment.application);
        assertNull(prefs.getString("com.braintreepayments.api.PayPal.PAYPAL_REQUEST_KEY", null));
        latch.await();
    }

    @Test
    public void requestOneTimePayment_doesNotCallCancelListenerWhenSuccessful() {
        final BraintreeFragment fragment = mMockFragmentBuilder
                .successResponse(stringFromFixture("paypal_hermes_response.json"))
                .build();
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                Intent intent = new Intent()
                        .setData(Uri.parse("com.braintreepayments.api.test.braintree://onetouch/v1/success?PayerID=HERMES-SANDBOX-PAYER-ID&paymentId=HERMES-SANDBOX-PAYMENT-ID&token=EC-HERMES-SANDBOX-EC-TOKEN"));
                PayPal.onActivityResult(fragment, RESULT_OK, intent);
                return null;
            }
        }).when(fragment).startActivity(any(Intent.class));

        MockStaticTokenizationClient.mockTokenizeSuccess(new PayPalAccountNonce());

        PayPal.requestOneTimePayment(fragment, new PayPalRequest("1"));

        verify(fragment, never()).postCancelCallback(anyInt());
    }

    @Test
    public void requestOneTimePayment_persistsPayPalRequest() {
        BraintreeFragment braintreeFragment = mMockFragmentBuilder.build();
        SharedPreferences prefs = BraintreeSharedPreferences.getSharedPreferences(RuntimeEnvironment.application);

        PayPal.requestOneTimePayment(braintreeFragment, new PayPalRequest("1").intent(PayPalRequest.INTENT_SALE));

        assertNotNull(prefs.getString("com.braintreepayments.api.PayPal.PAYPAL_REQUEST_KEY", null));
    }

    @Test
    public void checkout_cancelUrlTriggersCancelListener() {
        final BraintreeFragment fragment = mMockFragmentBuilder
                .successResponse(stringFromFixture("paypal_hermes_response.json"))
                .build();
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                Intent intent = new Intent()
                        .setData(Uri.parse("com.braintreepayments.api.test.braintree://onetouch/v1/cancel"));
                PayPal.onActivityResult(fragment, RESULT_OK, intent);
                return null;
            }
        }).when(fragment).browserSwitch(eq(BraintreeRequestCodes.PAYPAL), any(Intent.class));

        PayPal.requestOneTimePayment(fragment, new PayPalRequest("1"));

        verify(fragment).postCancelCallback(BraintreeRequestCodes.PAYPAL);
    }

    @Test
    public void onActivityResult_postsCancelWhenResultIsCanceled() {
        BraintreeFragment fragment = mMockFragmentBuilder.build();
        Intent intent = new Intent()
                .setData(Uri.parse("com.braintreepayments.api.test.braintree://onetouch/v1/cancel"));

        PayPal.onActivityResult(fragment, RESULT_OK, intent);

        verify(fragment).postCancelCallback(BraintreeRequestCodes.PAYPAL);
    }

    @Test
    public void onActivityResult_postsCancelWhenIntentIsNull() {
        BraintreeFragment fragment = mMockFragmentBuilder.build();

        PayPal.onActivityResult(fragment, RESULT_OK, null);

        verify(fragment).postCancelCallback(BraintreeRequestCodes.PAYPAL);
    }

    @Test
    public void onActivityResult_whenBillingAgreemeentAppSwitchSuccessful_sendsAnalyticsEvent() {
        persistRequest(new BillingAgreementRequest());
        MockStaticPayPalOneTouch.parseResponse(ResultType.Success);

        BraintreeFragment fragment = mMockFragmentBuilder.build();
        Intent data = new Intent();

        PayPal.onActivityResult(fragment, RESULT_OK, data);

        verify(fragment).sendAnalyticsEvent("paypal.billing-agreement.app-switch.succeeded");
    }

    @Test
    public void onActivityResult_whenSinglePaymentAppSwitchSuccessful_sendsAnalyticsEvent() {
        persistRequest(new CheckoutRequest());
        MockStaticPayPalOneTouch.parseResponse(ResultType.Success);

        BraintreeFragment fragment = mMockFragmentBuilder.build();
        Intent data = new Intent();

        PayPal.onActivityResult(fragment, RESULT_OK, data);

        verify(fragment).sendAnalyticsEvent("paypal.single-payment.app-switch.succeeded");
    }

    @Test
    public void onActivityResult_NotOkAndBillingAgreementAndAppSwitch_sendsAnalyticsEvent() {
        persistRequest(new BillingAgreementRequest());
        Intent data = dataForSwitch("app-switch");

        BraintreeFragment fragment = mMockFragmentBuilder.build();

        PayPal.onActivityResult(fragment, RESULT_CANCELED, data);

        verify(fragment).sendAnalyticsEvent("paypal.billing-agreement.app-switch.canceled");
    }

    @Test
    public void onActivityResult_NotOkAndBillingAgreementAndBrowserSwitch_sendsAnalyticsEvent() {
        persistRequest(new BillingAgreementRequest());
        Intent data = dataForSwitch("browser-switch");

        BraintreeFragment fragment = mMockFragmentBuilder.build();

        PayPal.onActivityResult(fragment, RESULT_CANCELED, data);

        verify(fragment).sendAnalyticsEvent("paypal.billing-agreement.browser-switch.canceled");
    }

    @Test
    public void onActivityResult_NotOkAndCheckoutAndAppSwitch_sendsAnalyticsEvent() {
        persistRequest(new CheckoutRequest());
        Intent data = dataForSwitch("app-switch");

        BraintreeFragment fragment = mMockFragmentBuilder.build();

        PayPal.onActivityResult(fragment, RESULT_CANCELED, data);

        verify(fragment).sendAnalyticsEvent("paypal.single-payment.app-switch.canceled");
    }

    @Test
    public void onActivityResult_NotOkAndCheckoutAndBrowserSwitch_sendsAnalyticsEvent() {
        persistRequest(new CheckoutRequest());
        Intent data = dataForSwitch("browser-switch");

        BraintreeFragment fragment = mMockFragmentBuilder.build();

        PayPal.onActivityResult(fragment, RESULT_CANCELED, data);

        verify(fragment).sendAnalyticsEvent("paypal.single-payment.browser-switch.canceled");
    }

    private void persistRequest(Request request) {
        try {
            Method method = PayPal.class.getDeclaredMethod("persistRequest",
                    Context.class, Request.class);
            method.setAccessible(true);
            method.invoke(null, RuntimeEnvironment.application, request);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Intent dataForSwitch(String switchType) {
        return new Intent().putExtra(BraintreeFragment.EXTRA_WAS_BROWSER_SWITCH_RESULT,
                        switchType.equals("browser-switch"));
    }
}
