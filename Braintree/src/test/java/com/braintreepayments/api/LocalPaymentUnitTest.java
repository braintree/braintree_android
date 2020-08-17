package com.braintreepayments.api;

import android.content.Intent;
import android.net.Uri;

import androidx.appcompat.app.AppCompatActivity;

import com.braintreepayments.api.exceptions.BraintreeException;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.BraintreeResponseListener;
import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.internal.BraintreeHttpClient;
import com.braintreepayments.api.models.Authorization;
import com.braintreepayments.api.models.BraintreeRequestCodes;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.LocalPaymentRequest;
import com.braintreepayments.api.models.LocalPaymentResult;
import com.braintreepayments.api.models.PostalAddress;
import com.braintreepayments.testutils.TestConfigurationBuilder;
import com.braintreepayments.testutils.TestConfigurationBuilder.TestPayPalConfigurationBuilder;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;

import java.util.concurrent.CountDownLatch;

import static com.braintreepayments.testutils.FixturesHelper.base64EncodedClientTokenFromFixture;
import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.doAnswer;

@RunWith(RobolectricTestRunner.class)
@PowerMockIgnore({"org.json.*", "org.mockito.*", "org.robolectric.*", "android.*", "com.google.gms.*"})
public class LocalPaymentUnitTest {

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    private Configuration mConfiguration;
    private BraintreeFragment mBraintreeFragment;
    private BraintreeHttpClient mMockHttpClient;

    @Before
    public void setUp() {
        mConfiguration = new TestConfigurationBuilder()
                .assetsUrl("http://assets.example.com")
                .paypal(new TestPayPalConfigurationBuilder(true)
                        .environment("offline")
                        .billingAgreementsEnabled(false))
                .buildConfiguration();

        mBraintreeFragment = getMockFragment(base64EncodedClientTokenFromFixture("client_token.json"), mConfiguration);

        mMockHttpClient = mock(BraintreeHttpClient.class);
        when(mBraintreeFragment.getHttpClient()).thenReturn(mMockHttpClient);
    }

    private BraintreeFragment getMockFragment(String authorization, Configuration configuration) {
        try {
            return new MockFragmentBuilder()
                    .authorization(Authorization.fromString(authorization))
                    .configuration(configuration)
                    .build();
        } catch (InvalidArgumentException e) {
            fail(e.getMessage());
            return null;
        }
    }

    @Test
    public void startPayment_postsParameters_startsBrowserWithProperRequestCode() {
        final CountDownLatch latch = new CountDownLatch(1);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                String requestBody = (String) invocation.getArguments()[1];

                JSONObject json = new JSONObject(requestBody);
                assertEquals("Doe", json.getString("lastName"));
                assertEquals("1.10", json.getString("amount"));
                assertEquals("Den Haag", json.getString("city"));
                assertEquals("2585 GJ", json.getString("postalCode"));
                assertEquals("sale", json.getString("intent"));
                assertEquals("Jon", json.getString("firstName"));
                assertEquals("639847934", json.getString("phone"));
                assertEquals("NL", json.getString("countryCode"));
                assertEquals("EUR", json.getString("currencyIsoCode"));
                assertEquals("ideal", json.getString("fundingSource"));
                assertEquals("jon@getbraintree.com", json.getString("payerEmail"));
                assertEquals("836486 of 22321 Park Lake", json.getString("line1"));
                assertEquals("Apt 2", json.getString("line2"));
                assertEquals("CA", json.getString("state"));
                assertEquals("local-merchant-account-id", json.getString("merchantAccountId"));
                assertTrue(json.getJSONObject("experienceProfile").getBoolean("noShipping"));
                String expectedCancelUrl = Uri.parse(mBraintreeFragment.getReturnUrlScheme() + "://local-payment-cancel").toString();
                String expectedReturnUrl = Uri.parse(mBraintreeFragment.getReturnUrlScheme() + "://local-payment-success").toString();
                assertEquals(expectedCancelUrl, json.getString("cancelUrl"));
                assertEquals(expectedReturnUrl, json.getString("returnUrl"));

                HttpResponseCallback callback = (HttpResponseCallback) invocation.getArguments()[2];
                callback.success(stringFromFixture("payment_methods/local_payment_create_response.json"));
                return null;
            }
        }).when(mMockHttpClient).post(eq("/v1/local_payments/create"), any(String.class), any(HttpResponseCallback.class));

        LocalPayment.startPayment(mBraintreeFragment, getIdealLocalPaymentRequest(), new BraintreeResponseListener<LocalPaymentRequest>() {
            @Override
            public void onResponse(LocalPaymentRequest localPaymentRequest) {
                LocalPayment.approvePayment(mBraintreeFragment, localPaymentRequest);
            }
        });

        verify(mBraintreeFragment)
                .browserSwitch(eq(BraintreeRequestCodes.LOCAL_PAYMENT), eq("https://checkout.paypal.com/latinum?token=payment-token"));
        latch.countDown();
    }

    @Test
    public void startPayment_success_sendsAnalyticsEvent() {
        final CountDownLatch latch = new CountDownLatch(1);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                HttpResponseCallback callback = (HttpResponseCallback) invocation.getArguments()[2];
                callback.success(stringFromFixture("payment_methods/local_payment_create_response.json"));
                return null;
            }
        }).when(mMockHttpClient).post(eq("/v1/local_payments/create"), any(String.class), any(HttpResponseCallback.class));

        LocalPayment.startPayment(mBraintreeFragment, getIdealLocalPaymentRequest(), new BraintreeResponseListener<LocalPaymentRequest>() {
            @Override
            public void onResponse(LocalPaymentRequest localPaymentRequest) {
                LocalPayment.approvePayment(mBraintreeFragment, localPaymentRequest);
            }
        });

        verify(mBraintreeFragment).sendAnalyticsEvent(eq("ideal.local-payment.start-payment.selected"));
        verify(mBraintreeFragment).sendAnalyticsEvent(eq("ideal.local-payment.webswitch.initiate.succeeded"));
        latch.countDown();
    }

    @Test
    public void startPayment_failure_sendsAnalyticsEvent() {
        final CountDownLatch latch = new CountDownLatch(1);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                HttpResponseCallback callback = (HttpResponseCallback) invocation.getArguments()[2];
                callback.success(stringFromFixture("error_response.json"));
                return null;
            }
        }).when(mMockHttpClient).post(eq("/v1/local_payments/create"), any(String.class), any(HttpResponseCallback.class));

        LocalPayment.startPayment(mBraintreeFragment, getIdealLocalPaymentRequest(), new BraintreeResponseListener<LocalPaymentRequest>() {
            @Override
            public void onResponse(LocalPaymentRequest localPaymentRequest) {
                LocalPayment.approvePayment(mBraintreeFragment, localPaymentRequest);
            }
        });

        verify(mBraintreeFragment).sendAnalyticsEvent(eq("ideal.local-payment.start-payment.selected"));
        verify(mBraintreeFragment).sendAnalyticsEvent(eq("ideal.local-payment.webswitch.initiate.failed"));
        latch.countDown();
    }

    @Test
    public void startPayment_setsApprovalUrl_andPaymentId_beforeListener_isCalled() {
        final CountDownLatch latch = new CountDownLatch(1);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                HttpResponseCallback callback = (HttpResponseCallback) invocation.getArguments()[2];
                callback.success(stringFromFixture("payment_methods/local_payment_create_response.json"));
                return null;
            }
        }).when(mMockHttpClient).post(eq("/v1/local_payments/create"), any(String.class), any(HttpResponseCallback.class));

        LocalPayment.startPayment(mBraintreeFragment, getIdealLocalPaymentRequest(), new BraintreeResponseListener<LocalPaymentRequest>() {
            @Override
            public void onResponse(LocalPaymentRequest localPaymentRequest) {
                assertEquals("local-payment-id-123", localPaymentRequest.getPaymentId());
                assertEquals("https://checkout.paypal.com/latinum?token=payment-token", localPaymentRequest.getApprovalUrl());
                LocalPayment.approvePayment(mBraintreeFragment, localPaymentRequest);
            }
        });

        latch.countDown();
    }

    @Test
    public void startPayment_callsExceptionListener_whenApprovalUrlIsSet() {
        LocalPaymentRequest request = getIdealLocalPaymentRequest()
                .approvalUrl("aUrl");

        LocalPayment.startPayment(mBraintreeFragment, request, new BraintreeResponseListener<LocalPaymentRequest>() {
            @Override
            public void onResponse(LocalPaymentRequest localPaymentRequest) {
                LocalPayment.approvePayment(mBraintreeFragment, localPaymentRequest);
            }
        });

        verify(mBraintreeFragment).postCallback(any(BraintreeException.class));
    }

    @Test
    public void startPayment_callsExceptionListener_whenPaymentIdIsSet() {
        LocalPaymentRequest request = getIdealLocalPaymentRequest()
                .paymentId("pid");

        LocalPayment.startPayment(mBraintreeFragment, request, new BraintreeResponseListener<LocalPaymentRequest>() {
            @Override
            public void onResponse(LocalPaymentRequest localPaymentRequest) {
                LocalPayment.approvePayment(mBraintreeFragment, localPaymentRequest);
            }
        });

        verify(mBraintreeFragment).postCallback(any(BraintreeException.class));
    }

    @Test
    public void startPayment_callsExceptionListener_whenListenerIsNull() {
        LocalPayment.startPayment(mBraintreeFragment, getIdealLocalPaymentRequest(), null);

        verify(mBraintreeFragment).postCallback(any(BraintreeException.class));
    }

    @Test
    public void startPayment_callsExceptionListener_amountIsNull() {
        LocalPaymentRequest request = getIdealLocalPaymentRequest()
                .amount(null);

        LocalPayment.startPayment(mBraintreeFragment, request, new BraintreeResponseListener<LocalPaymentRequest>() {
            @Override
            public void onResponse(LocalPaymentRequest localPaymentRequest) {
                LocalPayment.approvePayment(mBraintreeFragment, localPaymentRequest);
            }
        });

        verify(mBraintreeFragment).postCallback(any(BraintreeException.class));
    }

    @Test
    public void startPayment_callsExceptionListener_paymentTypeIsNull() {
        LocalPaymentRequest request = getIdealLocalPaymentRequest()
                .paymentType(null);

        LocalPayment.startPayment(mBraintreeFragment, request, new BraintreeResponseListener<LocalPaymentRequest>() {
            @Override
            public void onResponse(LocalPaymentRequest localPaymentRequest) {
                LocalPayment.approvePayment(mBraintreeFragment, localPaymentRequest);
            }
        });

        verify(mBraintreeFragment).postCallback(any(BraintreeException.class));
    }

    @Test
    public void startPayment_callsExceptionListener_localPaymentRequestIsNull() {
        LocalPayment.startPayment(mBraintreeFragment, null, new BraintreeResponseListener<LocalPaymentRequest>() {
            @Override
            public void onResponse(LocalPaymentRequest localPaymentRequest) {
                LocalPayment.approvePayment(mBraintreeFragment, localPaymentRequest);
            }
        });

        verify(mBraintreeFragment).postCallback(any(BraintreeException.class));
    }

    @Test
    public void startPayment_callsExceptionListenerOnHttpError() {
        final Exception expectedException = new Exception();
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                HttpResponseCallback callback = (HttpResponseCallback) invocation.getArguments()[2];
                callback.failure(expectedException);
                return null;
            }
        }).when(mMockHttpClient).post(eq("/v1/local_payments/create"), any(String.class), any(HttpResponseCallback.class));

        LocalPayment.startPayment(mBraintreeFragment, getIdealLocalPaymentRequest(), new BraintreeResponseListener<LocalPaymentRequest>() {
            @Override
            public void onResponse(LocalPaymentRequest localPaymentRequest) {
                LocalPayment.approvePayment(mBraintreeFragment, localPaymentRequest);
            }
        });

        verify(mBraintreeFragment).postCallback(eq(expectedException));
    }

    @Test
    public void onActivityResult_whenResultOK_uriNull_postsCancelCallbackAlongWithAnalyticsEvent() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                HttpResponseCallback callback = (HttpResponseCallback) invocation.getArguments()[2];
                callback.success(stringFromFixture("payment_methods/local_payment_create_response.json"));
                latch.countDown();
                return null;
            }
        }).when(mMockHttpClient).post(eq("/v1/local_payments/create"), any(String.class), any(HttpResponseCallback.class));

        LocalPayment.startPayment(mBraintreeFragment, getIdealLocalPaymentRequest(), new BraintreeResponseListener<LocalPaymentRequest>() {
            @Override
            public void onResponse(LocalPaymentRequest localPaymentRequest) {
                LocalPayment.approvePayment(mBraintreeFragment, localPaymentRequest);
            }
        });

        latch.await();

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                HttpResponseCallback callback = (HttpResponseCallback) invocation.getArguments()[2];
                callback.success(stringFromFixture("payment_methods/local_payment_response.json"));
                return null;
            }
        }).when(mMockHttpClient).post(eq("/v1/payment_methods/paypal_accounts"), any(String.class), any(HttpResponseCallback.class));

        LocalPayment.onActivityResult(mBraintreeFragment, AppCompatActivity.RESULT_OK, new Intent());

        verify(mBraintreeFragment).sendAnalyticsEvent(eq("ideal.local-payment.webswitch-response.invalid"));

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(mBraintreeFragment).postCallback(captor.capture());

        BraintreeException braintreeException = ((BraintreeException) captor.getValue());
        String expectedMessage = "LocalPayment encountered an error, return URL is invalid.";
        assertEquals(braintreeException.getMessage(), expectedMessage);
    }

    @Test
    public void onActivityResult_whenResultOK_tokenize_sendsAnalyticsEvent() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                HttpResponseCallback callback = (HttpResponseCallback) invocation.getArguments()[2];
                callback.success(stringFromFixture("error_response.json"));
                latch.countDown();
                return null;
            }
        }).when(mMockHttpClient).post(eq("/v1/local_payments/create"), any(String.class), any(HttpResponseCallback.class));

        LocalPayment.startPayment(mBraintreeFragment, getIdealLocalPaymentRequest(), new BraintreeResponseListener<LocalPaymentRequest>() {
            @Override
            public void onResponse(LocalPaymentRequest localPaymentRequest) {
                LocalPayment.approvePayment(mBraintreeFragment, localPaymentRequest);
            }
        });

        latch.await();

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                HttpResponseCallback callback = (HttpResponseCallback) invocation.getArguments()[2];
                callback.success(stringFromFixture("payment_methods/local_payment_response.json"));
                return null;
            }
        }).when(mMockHttpClient).post(eq("/v1/payment_methods/paypal_accounts"), any(String.class), any(HttpResponseCallback.class));

        LocalPayment.onActivityResult(mBraintreeFragment, AppCompatActivity.RESULT_OK, getSuccessResponseIntent());

        verify(mBraintreeFragment).sendAnalyticsEvent(eq("ideal.local-payment.tokenize.succeeded"));
    }

    @Test
    public void onActivityResult_whenResultOK_cancel_sendsAnalyticsEvent() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                HttpResponseCallback callback = (HttpResponseCallback) invocation.getArguments()[2];
                callback.success(stringFromFixture("error_response.json"));
                latch.countDown();
                return null;
            }
        }).when(mMockHttpClient).post(eq("/v1/local_payments/create"), any(String.class), any(HttpResponseCallback.class));

        LocalPayment.startPayment(mBraintreeFragment, getIdealLocalPaymentRequest(), new BraintreeResponseListener<LocalPaymentRequest>() {
            @Override
            public void onResponse(LocalPaymentRequest localPaymentRequest) {
                LocalPayment.approvePayment(mBraintreeFragment, localPaymentRequest);
            }
        });

        latch.await();

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                HttpResponseCallback callback = (HttpResponseCallback) invocation.getArguments()[2];
                callback.success(stringFromFixture("payment_methods/local_payment_response.json"));
                return null;
            }
        }).when(mMockHttpClient).post(eq("/v1/payment_methods/paypal_accounts"), any(String.class), any(HttpResponseCallback.class));

        LocalPayment.onActivityResult(mBraintreeFragment, AppCompatActivity.RESULT_OK, getCancelResponseIntent());

        verify(mBraintreeFragment).sendAnalyticsEvent(eq("ideal.local-payment.webswitch.canceled"));
    }

    @Test
    public void onActivityResult_whenResultOK_returnsResultToFragment()
            throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                HttpResponseCallback callback = (HttpResponseCallback) invocation.getArguments()[2];
                callback.success(stringFromFixture("payment_methods/local_payment_response.json"));
                latch.countDown();
                return null;
            }
        }).when(mMockHttpClient).post(eq("/v1/payment_methods/paypal_accounts"), any(String.class), any(HttpResponseCallback.class));

        LocalPayment.onActivityResult(mBraintreeFragment, AppCompatActivity.RESULT_OK, getSuccessResponseIntent());

        latch.await();

        ArgumentCaptor<LocalPaymentResult> captor = ArgumentCaptor.forClass(LocalPaymentResult.class);
        verify(mBraintreeFragment).postCallback(captor.capture());

        LocalPaymentResult capturedResult = captor.getValue();
        assertEquals("e11c9c39-d6a4-0305-791d-bfe680ef2d5d", capturedResult.getNonce());
    }

    @Test
    public void onActivityResult_whenResultCancel_postsCancelCallback() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                HttpResponseCallback callback = (HttpResponseCallback) invocation.getArguments()[2];
                callback.success(stringFromFixture("error_response.json"));
                latch.countDown();
                return null;
            }
        }).when(mMockHttpClient).post(eq("/v1/local_payments/create"), any(String.class), any(HttpResponseCallback.class));

        LocalPayment.startPayment(mBraintreeFragment, getIdealLocalPaymentRequest(), new BraintreeResponseListener<LocalPaymentRequest>() {
            @Override
            public void onResponse(LocalPaymentRequest localPaymentRequest) {
                LocalPayment.approvePayment(mBraintreeFragment, localPaymentRequest);
            }
        });

        latch.await();

        LocalPayment.onActivityResult(mBraintreeFragment, AppCompatActivity.RESULT_CANCELED, new Intent());

        verify(mBraintreeFragment).postCancelCallback(BraintreeRequestCodes.LOCAL_PAYMENT);
        verify(mBraintreeFragment).sendAnalyticsEvent(eq("ideal.local-payment.webswitch.canceled"));
    }

    private LocalPaymentRequest getIdealLocalPaymentRequest() {
        PostalAddress address = new PostalAddress()
                .streetAddress("836486 of 22321 Park Lake")
                .extendedAddress("Apt 2")
                .countryCodeAlpha2("NL")
                .locality("Den Haag")
                .region("CA")
                .postalCode("2585 GJ");
        LocalPaymentRequest request = new LocalPaymentRequest()
                .paymentType("ideal")
                .amount("1.10")
                .address(address)
                .phone("639847934")
                .email("jon@getbraintree.com")
                .givenName("Jon")
                .surname("Doe")
                .shippingAddressRequired(false)
                .merchantAccountId("local-merchant-account-id")
                .currencyCode("EUR");
        return request;
    }

    private Intent getSuccessResponseIntent() {
        return new Intent().setData(Uri.parse(mBraintreeFragment.getReturnUrlScheme() + "://local-payment-success?paymentToken=successTokenId"));
    }

    private Intent getCancelResponseIntent() {
        return new Intent().setData(Uri.parse(mBraintreeFragment.getReturnUrlScheme() + "://local-payment-cancel?paymentToken=canceled"));
    }
}
