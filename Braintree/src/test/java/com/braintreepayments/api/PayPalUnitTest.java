package com.braintreepayments.api;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;

import com.braintreepayments.api.exceptions.BraintreeException;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.interfaces.PayPalApprovalCallback;
import com.braintreepayments.api.interfaces.PayPalApprovalHandler;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCallback;
import com.braintreepayments.api.internal.BraintreeSharedPreferences;
import com.braintreepayments.api.models.Authorization;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.PayPalAccountBuilder;
import com.braintreepayments.api.models.PayPalAccountNonce;
import com.braintreepayments.api.models.PayPalConfiguration;
import com.braintreepayments.api.models.PayPalRequest;
import com.braintreepayments.api.models.PaymentMethodBuilder;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.api.models.PostalAddress;
import com.braintreepayments.testutils.TestConfigurationBuilder;
import com.braintreepayments.testutils.TestConfigurationBuilder.TestPayPalConfigurationBuilder;
import com.paypal.android.sdk.onetouch.core.AuthorizationRequest;
import com.paypal.android.sdk.onetouch.core.Request;
import com.paypal.android.sdk.onetouch.core.config.Recipe;
import com.paypal.android.sdk.onetouch.core.encryption.EncryptionUtils;

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
import org.robolectric.RuntimeEnvironment;

import java.util.concurrent.CountDownLatch;

import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static com.braintreepayments.testutils.ReflectionHelper.setField;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.contains;
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
    public void authorizeAccount_sendsAnalyticsEvent() {
        BraintreeFragment fragment = mMockFragmentBuilder.build();

        PayPal.authorizeAccount(fragment);

        verify(fragment).sendAnalyticsEvent("paypal.future-payments.selected");
    }

    @Test
    public void authorizeAccount_postsExceptionWhenNotEnabled() throws JSONException, InterruptedException {
        BraintreeFragment fragment = new MockFragmentBuilder().build();

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
        final AuthorizationRequest request = new AuthorizationRequest(RuntimeEnvironment.application);
        request.environment("test");
        request.successUrl("com.braintreepayments.api.test.braintree", "success");
        setField(AuthorizationRequest.class, "mMsgGuid", request, "c862cf00-f878-4e38-bb83-65bcc4b51831");
        setField(AuthorizationRequest.class, "mEncryptionKey", request, EncryptionUtils.hexStringToByteArray("0481806100DE4EBB5581163579990EE825737255A81A883B791A1BB6F5A7E81C"));

        doAnswer(new Answer<AuthorizationRequest>() {
            @Override
            public AuthorizationRequest answer(InvocationOnMock invocation) throws Throwable {
                return request;
            }
        }).when(PayPal.class, "getAuthorizationRequest", any(Context.class), any(PayPalConfiguration.class), anyString());

        final BraintreeFragment fragment = mMockFragmentBuilder.build();
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Intent intent = new Intent()
                        .setData(Uri.parse("com.braintreepayments.api.test.braintree://onetouch/v1/success?payloadEnc=k7mNFgzs404Wy8VOReO2E%2FTpfDoC44E1iwjptDooIkjjh1TcAupUCM8812g3zmBruc%2BFeIIwZlEhu6ugAXvLs5u6aHG4KU7FuPPLDS9OO87WAw0v3n7QIPp%2Bd5o%2Bk4VZ047w%2FXiijFuFKb4SRe9fg8kYGAYCtUR1IrK%2BhuvB3VCg7rkLk9V0n2YF3WcvmaLUt8SIYok1dbG8Ou4zDIXaZp7%2ByGalcyjN3MW3OLstaehD2jpuxlP6WDG6dkB6LZ2LEnHDV0X7j2vOtmSrrCtYZuFhlB%2FkKNkgsVhBrbHdqsfsBKyc7sHlsgT0Dz0TXc3BHqjJIWLrOuglt78QOM92%2B7GFM6JL5%2BARzJ4Tp9iI%2BU4QyQLTSkOGTA0LgSBUhr2srF41lWTXw65F4A%3D%3D&payload=eyJ2ZXJzaW9uIjozLCJtc2dfR1VJRCI6ImM4NjJjZjAwLWY4NzgtNGUzOC1iYjgzLTY1YmNjNGI1MTgzMSIsInJlc3BvbnNlX3R5cGUiOiJjb2RlIiwiZW52aXJvbm1lbnQiOiJtb2NrIiwiZXJyb3IiOm51bGx9&x-source=com.braintree.browserswitch"));
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
        final AuthorizationRequest request = new AuthorizationRequest(RuntimeEnvironment.application);
        request.environment("test");
        request.successUrl("com.braintreepayments.api.test.braintree", "success");
        setField(AuthorizationRequest.class, "mMsgGuid", request, "c862cf00-f878-4e38-bb83-65bcc4b51831");
        setField(AuthorizationRequest.class, "mEncryptionKey", request, EncryptionUtils.hexStringToByteArray("0481806100DE4EBB5581163579990EE825737255A81A883B791A1BB6F5A7E81C"));

        doAnswer(new Answer<AuthorizationRequest>() {
            @Override
            public AuthorizationRequest answer(InvocationOnMock invocation) throws Throwable {
                return request;
            }
        }).when(PayPal.class, "getAuthorizationRequest", any(Context.class), any(PayPalConfiguration.class), anyString());

        final BraintreeFragment fragment = mMockFragmentBuilder.build();
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Intent intent = new Intent()
                        .setData(Uri.parse("com.braintreepayments.api.test.braintree://onetouch/v1/success?payloadEnc=k7mNFgzs404Wy8VOReO2E%2FTpfDoC44E1iwjptDooIkjjh1TcAupUCM8812g3zmBruc%2BFeIIwZlEhu6ugAXvLs5u6aHG4KU7FuPPLDS9OO87WAw0v3n7QIPp%2Bd5o%2Bk4VZ047w%2FXiijFuFKb4SRe9fg8kYGAYCtUR1IrK%2BhuvB3VCg7rkLk9V0n2YF3WcvmaLUt8SIYok1dbG8Ou4zDIXaZp7%2ByGalcyjN3MW3OLstaehD2jpuxlP6WDG6dkB6LZ2LEnHDV0X7j2vOtmSrrCtYZuFhlB%2FkKNkgsVhBrbHdqsfsBKyc7sHlsgT0Dz0TXc3BHqjJIWLrOuglt78QOM92%2B7GFM6JL5%2BARzJ4Tp9iI%2BU4QyQLTSkOGTA0LgSBUhr2srF41lWTXw65F4A%3D%3D&payload=eyJ2ZXJzaW9uIjozLCJtc2dfR1VJRCI6ImM4NjJjZjAwLWY4NzgtNGUzOC1iYjgzLTY1YmNjNGI1MTgzMSIsInJlc3BvbnNlX3R5cGUiOiJjb2RlIiwiZW52aXJvbm1lbnQiOiJtb2NrIiwiZXJyb3IiOm51bGx9&x-source=com.braintree.browserswitch"));
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
    public void requestBillingAgreement_defaultPostParamsIncludeCorrectValues() throws JSONException {
        BraintreeFragment fragment = mMockFragmentBuilder.build();

        PayPal.requestBillingAgreement(fragment, new PayPalRequest());

        ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(fragment.getHttpClient()).post(contains("/paypal_hermes/setup_billing_agreement"), dataCaptor.capture(),
                any(HttpResponseCallback.class));

        JSONObject json = new JSONObject(dataCaptor.getValue());
        assertNull(json.opt("amount"));
        assertNull(json.opt("intent"));
    }

    @Test
    public void requestBillingAgreement_isSuccessful() {
        final BraintreeFragment fragment = mMockFragmentBuilder
                .successResponse(stringFromFixture("paypal_hermes_billing_agreement_response.json"))
                .build();
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Intent intent = new Intent()
                        .setData(Uri.parse("com.braintreepayments.api.test.braintree://onetouch/v1/success?PayerID=HERMES-SANDBOX-PAYER-ID&paymentId=HERMES-SANDBOX-PAYMENT-ID&ba_token=EC-HERMES-SANDBOX-EC-TOKEN"));
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
    public void requestBillingAgreement_persistsPayPalRequest() {
        BraintreeFragment braintreeFragment = mMockFragmentBuilder.build();
        SharedPreferences prefs = BraintreeSharedPreferences.getSharedPreferences(RuntimeEnvironment.application);

        PayPal.requestOneTimePayment(braintreeFragment, new PayPalRequest("1").intent(PayPalRequest.INTENT_SALE));

        assertNotNull(prefs.getString("com.braintreepayments.api.PayPal.PAYPAL_REQUEST_KEY", null));
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
    public void requestOneTimePayment_customHandlerCancelCallbackIsInvoked() throws InterruptedException {
        BraintreeFragment fragment = mMockFragmentBuilder
                .successResponse(stringFromFixture("paypal_hermes_response.json"))
                .build();

        PayPal.requestOneTimePayment(fragment, new PayPalRequest("1"), new PayPalApprovalHandler() {
            @Override
            public void handleApproval(Request request, PayPalApprovalCallback paypalApprovalCallback) {
                paypalApprovalCallback.onCancel();
            }
        });

        verify(fragment).postCancelCallback(PayPal.PAYPAL_REQUEST_CODE);
    }

    @Test
    public void requestOneTimePayment_paypalCreditReturnedInResponse() throws InterruptedException {
        BraintreeFragment fragment = mMockFragmentBuilder
                .successResponse(stringFromFixture("paypal_hermes_response.json"))
                .build();

        mockStatic(TokenizationClient.class);
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                ((PaymentMethodNonceCallback) invocation.getArguments()[2]).success(PayPalAccountNonce.fromJson(stringFromFixture("payment_methods/paypal_account_response.json")));
                return null;
            }
        }).when(TokenizationClient.class);
        TokenizationClient.tokenize(any(BraintreeFragment.class), any(PaymentMethodBuilder.class),
                any(PaymentMethodNonceCallback.class));

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

        PayPalAccountNonce payPalAccountNonce = (PayPalAccountNonce) nonceCaptor.getValue();
        assertNotNull(payPalAccountNonce.getCreditFinancing());
        assertEquals(18, payPalAccountNonce.getCreditFinancing().getTerm());
    }
    @Test
    public void requestOneTimePayment_customHandlerSuccessCallbackIsInvoked() throws InterruptedException {
        BraintreeFragment fragment = mMockFragmentBuilder
                .successResponse(stringFromFixture("paypal_hermes_response.json"))
                .build();

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
    public void requestOneTimePayment_sendsPayPalCreditOfferedAnalyticsEvent() throws JSONException {
        BraintreeFragment fragment = mMockFragmentBuilder.build();

        PayPal.requestOneTimePayment(fragment, new PayPalRequest("1").offerCredit(true));

        verify(fragment).sendAnalyticsEvent("paypal-single-payment.credit.offered");
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
        assertEquals("displayName", json.getJSONObject("experience_profile").get("brand_name"));
        assertFalse(json.getJSONObject("experience_profile").has("landing_page_type"));
        assertEquals(PayPalRequest.INTENT_AUTHORIZE, json.get("intent"));
        assertFalse(json.getBoolean("offer_paypal_credit"));
    }

    @Test
    public void requestOneTimePayment_containsOfferPayPalCreditParam() throws JSONException {
        BraintreeFragment fragment = mMockFragmentBuilder.build();

        PayPal.requestOneTimePayment(fragment, new PayPalRequest("1").offerCredit(true));

        ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(fragment.getHttpClient()).post(pathCaptor.capture(), dataCaptor.capture(),
                any(HttpResponseCallback.class));
        assertTrue(pathCaptor.getValue().contains("/paypal_hermes/create_payment_resource"));

        JSONObject json = new JSONObject(dataCaptor.getValue());
        assertTrue(json.getBoolean("offer_paypal_credit"));
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
    public void requestOneTimePayment_userAction_setsUserActionToBlankStringonDefault() throws JSONException {
        BraintreeFragment fragment = mMockFragmentBuilder
                .successResponse(stringFromFixture("paypal_hermes_billing_agreement_response.json"))
                .build();

        PayPal.requestOneTimePayment(fragment, new PayPalRequest("1").userAction(PayPalRequest.USER_ACTION_DEFAULT));

        ArgumentCaptor<Intent> dataCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(fragment).startActivity(dataCaptor.capture());

        Uri uri = dataCaptor.getValue().getData();
        assertEquals("", uri.getQueryParameter("useraction"));
    }

    @Test
    public void requestOneTimePayment_userAction_canBeSetToCommit() throws JSONException {
        BraintreeFragment fragment = mMockFragmentBuilder
                .successResponse(stringFromFixture("paypal_hermes_billing_agreement_response.json"))
                .build();

        PayPal.requestOneTimePayment(fragment, new PayPalRequest("1").userAction(PayPalRequest.USER_ACTION_COMMIT));

        ArgumentCaptor<Intent> dataCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(fragment).startActivity(dataCaptor.capture());

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
    public void requestOneTimePayment_startsBrowserWithPayPalCredit() {
        BraintreeFragment fragment = mMockFragmentBuilder
                .successResponse(stringFromFixture("paypal_hermes_response.json"))
                .build();

        PayPal.requestOneTimePayment(fragment, new PayPalRequest("1.00").offerCredit(true));

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
        assertEquals("true", intent.getData().getQueryParameter("offer_paypal_credit"));
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

        SharedPreferences prefs = BraintreeSharedPreferences.getSharedPreferences(RuntimeEnvironment.application);
        assertNull(prefs.getString("com.braintreepayments.api.PayPal.REQUEST_KEY", null));
        assertNull(prefs.getString("com.braintreepayments.api.PayPal.REQUEST_TYPE_KEY", null));
    }

    @Test
    public void requestOneTimePayment_containsPaymentIntent() throws JSONException, InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
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
