package com.braintreepayments.api;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import androidx.test.runner.AndroidJUnit4;

import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.BraintreeErrorListener;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCreatedListener;
import com.braintreepayments.api.models.CardBuilder;
import com.braintreepayments.api.models.CardNonce;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.api.models.ThreeDSecureAuthenticationResponse;
import com.braintreepayments.api.models.ThreeDSecureInfo;
import com.braintreepayments.api.models.ThreeDSecureRequest;
import com.braintreepayments.api.test.BraintreeActivityTestRule;
import com.braintreepayments.api.test.TestActivity;
import com.braintreepayments.api.test.TestClientTokenBuilder;
import com.braintreepayments.api.threedsecure.ThreeDSecureWebViewActivity;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;

import static com.braintreepayments.api.BraintreeFragmentTestUtils.getFragmentWithAuthorization;
import static com.braintreepayments.api.BraintreeFragmentTestUtils.tokenize;
import static com.braintreepayments.api.test.Assertions.assertIsANonce;
import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;

@RunWith(AndroidJUnit4.class)
public class ThreeDSecureTest {

    @Rule
    public final BraintreeActivityTestRule<TestActivity> mActivityTestRule =
            new BraintreeActivityTestRule<>(TestActivity.class);

    private Activity mActivity;
    private CountDownLatch mCountDownLatch;

    @Before
    public void setUp() {
        mActivity = mActivityTestRule.getActivity();
        mCountDownLatch = new CountDownLatch(1);
    }

    @Test(timeout = 10000)
    public void performVerification_postsPaymentMethodNonceToListenersWhenLookupReturnsACard()
            throws InterruptedException {
        String clientToken = new TestClientTokenBuilder().withThreeDSecure().build();
        BraintreeFragment fragment = getFragmentWithAuthorization(mActivity, clientToken);
        String nonce = tokenize(fragment, new CardBuilder()
                .cardNumber("4000000000000051")
                .expirationDate("12/20")).getNonce();
        fragment.addListener(new PaymentMethodNonceCreatedListener() {
            @Override
            public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
                assertIsANonce(paymentMethodNonce.getNonce());
                assertEquals("51", ((CardNonce) paymentMethodNonce).getLastTwo());
                assertTrue(((CardNonce) paymentMethodNonce).getThreeDSecureInfo().wasVerified());
                mCountDownLatch.countDown();
            }
        });

        ThreeDSecure.performVerification(fragment, nonce, "5");

        mCountDownLatch.await();
    }

    @Test(timeout = 10000)
    public void performVerification_acceptsAThreeDSecureRequest_postsPaymentMethodNonceToListenersWhenLookupReturnsACard()
            throws InterruptedException, InvalidArgumentException {
        String clientToken = new TestClientTokenBuilder().withThreeDSecure().build();
        BraintreeFragment fragment = getFragmentWithAuthorization(mActivity, clientToken);
        String nonce = tokenize(fragment, new CardBuilder()
                .cardNumber("4000000000000051")
                .expirationDate("12/20")).getNonce();
        fragment.addListener(new PaymentMethodNonceCreatedListener() {
            @Override
            public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
                assertIsANonce(paymentMethodNonce.getNonce());
                assertEquals("51", ((CardNonce) paymentMethodNonce).getLastTwo());

                ThreeDSecureInfo threeDSecureInfo = ((CardNonce) paymentMethodNonce).getThreeDSecureInfo();
                assertFalse(threeDSecureInfo.isLiabilityShifted());
                assertFalse(threeDSecureInfo.isLiabilityShiftPossible());
                assertTrue(((CardNonce) paymentMethodNonce).getThreeDSecureInfo().wasVerified());
                mCountDownLatch.countDown();
            }
        });

        ThreeDSecureRequest request = new ThreeDSecureRequest()
                .nonce(nonce)
                .amount("5");

        ThreeDSecure.performVerification(fragment, request);

        mCountDownLatch.await();
    }

    @Test(timeout = 1000)
    public void performVerification_withInvalidThreeDSecureRequest_postsException()
            throws InterruptedException, InvalidArgumentException {
        String clientToken = new TestClientTokenBuilder().withThreeDSecure().build();
        BraintreeFragment fragment = getFragmentWithAuthorization(mActivity, clientToken);
        fragment.addListener(new BraintreeErrorListener() {
            @Override
            public void onError(Exception error) {
                assertEquals("The ThreeDSecureRequest nonce and amount cannot be null", error.getMessage());
                mCountDownLatch.countDown();
            }
        });

        ThreeDSecureRequest request = new ThreeDSecureRequest()
                .amount("5");

        ThreeDSecure.performVerification(fragment, request);

        mCountDownLatch.await();
    }

    @Test(timeout = 10000)
    public void performVerification_acceptsACardBuilderAndPostsAPaymentMethodNonceToListener()
            throws InterruptedException {
        BraintreeFragment fragment = getFragmentWithAuthorization(mActivity,
                new TestClientTokenBuilder().withThreeDSecure().build());
        fragment.addListener(new PaymentMethodNonceCreatedListener() {
            @Override
            public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
                assertIsANonce(paymentMethodNonce.getNonce());
                assertEquals("51", ((CardNonce) paymentMethodNonce).getLastTwo());
                assertTrue(((CardNonce) paymentMethodNonce).getThreeDSecureInfo().wasVerified());
                mCountDownLatch.countDown();
            }
        });
        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber("4000000000000051")
                .expirationDate("12/20");

        ThreeDSecure.performVerification(fragment, cardBuilder, "5");

        mCountDownLatch.await();
    }

    @Test(timeout = 2000)
    public void onActivityResult_postsPaymentMethodNonceToListener() throws JSONException, InterruptedException {
        BraintreeFragment fragment = getFragmentWithAuthorization(mActivity,
                new TestClientTokenBuilder().withThreeDSecure().build());
        fragment.addListener(new PaymentMethodNonceCreatedListener() {
            @Override
            public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
                assertIsANonce(paymentMethodNonce.getNonce());
                assertEquals("11", ((CardNonce) paymentMethodNonce).getLastTwo());
                assertTrue(((CardNonce) paymentMethodNonce).getThreeDSecureInfo().wasVerified());
                mCountDownLatch.countDown();
            }
        });
        JSONObject authResponse = new JSONObject(
                stringFromFixture("three_d_secure/authentication_response.json"));
        Intent data = new Intent()
                .putExtra(ThreeDSecureWebViewActivity.EXTRA_THREE_D_SECURE_RESULT,
                        ThreeDSecureAuthenticationResponse.fromJson(authResponse.toString()));

        ThreeDSecure.onActivityResult(fragment, Activity.RESULT_OK, data);

        mCountDownLatch.await();
    }

    @Test(timeout = 1000)
    public void onActivityResult_postsPaymentMethodNonceToListener_fromUri() throws InterruptedException {
        BraintreeFragment fragment = getFragmentWithAuthorization(mActivity,
                new TestClientTokenBuilder().withThreeDSecure().build());
        fragment.addListener(new PaymentMethodNonceCreatedListener() {
            @Override
            public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
                assertIsANonce(paymentMethodNonce.getNonce());
                assertEquals("11", ((CardNonce) paymentMethodNonce).getLastTwo());
                assertTrue(((CardNonce) paymentMethodNonce).getThreeDSecureInfo().wasVerified());
                mCountDownLatch.countDown();
            }
        });

        Uri responseUri = Uri.parse("http://demo-app.com")
                .buildUpon()
                .appendQueryParameter("auth_response", stringFromFixture("three_d_secure/authentication_response.json"))
                .build();
        Intent data = new Intent()
                .setData(responseUri);

        ThreeDSecure.onActivityResult(fragment, Activity.RESULT_OK, data);

        mCountDownLatch.await();
    }

    @Test(timeout = 2000)
    public void onActivityResult_postsUnrecoverableErrorsToListeners() throws InterruptedException {
        BraintreeFragment fragment = getFragmentWithAuthorization(mActivity,
                new TestClientTokenBuilder().withThreeDSecure().build());
        fragment.addListener(new BraintreeErrorListener() {
            @Override
            public void onError(Exception error) {
                assertEquals("Error!", error.getMessage());
                mCountDownLatch.countDown();
            }
        });
        ThreeDSecureAuthenticationResponse authResponse =
                ThreeDSecureAuthenticationResponse.fromException("Error!");
        Intent data = new Intent()
                .putExtra(ThreeDSecureWebViewActivity.EXTRA_THREE_D_SECURE_RESULT, authResponse);

        ThreeDSecure.onActivityResult(fragment, Activity.RESULT_OK, data);

        mCountDownLatch.await();
    }

    @Test(timeout = 2000)
    public void onActivityResult_postsRecoverableErrorsToListener() throws InterruptedException {
        BraintreeFragment fragment = getFragmentWithAuthorization(mActivity,
                new TestClientTokenBuilder().withThreeDSecure().build());
        fragment.addListener(new BraintreeErrorListener() {
            @Override
            public void onError(Exception error) {
                assertEquals("Failed to authenticate, please try a different form of payment",
                        error.getMessage());
                mCountDownLatch.countDown();
            }
        });
        Intent data = new Intent()
                .putExtra(ThreeDSecureWebViewActivity.EXTRA_THREE_D_SECURE_RESULT,
                        ThreeDSecureAuthenticationResponse.fromJson(
                                stringFromFixture("errors/three_d_secure_error.json")));

        ThreeDSecure.onActivityResult(fragment, Activity.RESULT_OK, data);

        mCountDownLatch.await();
    }

    @Test(timeout = 5000)
    public void onActivityResult_doesNothingWhenResultCodeNotOk() {
        Intent intent = mock(Intent.class);

        ThreeDSecure.onActivityResult(mock(BraintreeFragment.class), Activity.RESULT_CANCELED,
                intent);

        verifyZeroInteractions(intent);
    }
}
