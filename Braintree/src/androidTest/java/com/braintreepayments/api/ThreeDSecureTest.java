package com.braintreepayments.api;

import androidx.appcompat.app.AppCompatActivity;
import androidx.test.runner.AndroidJUnit4;

import com.braintreepayments.api.interfaces.PaymentMethodNonceCreatedListener;
import com.braintreepayments.api.models.CardBuilder;
import com.braintreepayments.api.models.CardNonce;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.api.models.ThreeDSecureInfo;
import com.braintreepayments.api.models.ThreeDSecureRequest;
import com.braintreepayments.api.test.BraintreeActivityTestRule;
import com.braintreepayments.api.test.TestActivity;
import com.braintreepayments.api.test.TestClientTokenBuilder;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;

import static com.braintreepayments.api.BraintreeFragmentTestUtils.getFragmentWithAuthorization;
import static com.braintreepayments.api.BraintreeFragmentTestUtils.tokenize;
import static com.braintreepayments.api.test.Assertions.assertIsANonce;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class ThreeDSecureTest {

    @Rule
    public final BraintreeActivityTestRule<TestActivity> mActivityTestRule =
            new BraintreeActivityTestRule<>(TestActivity.class);

    private AppCompatActivity mActivity;
    private CountDownLatch mCountDownLatch;

    @Before
    public void setUp() {
        mActivity = mActivityTestRule.getActivity();
        mCountDownLatch = new CountDownLatch(1);
    }

    @Test(timeout = 10000)
    public void performVerification_postsPaymentMethodNonceToListenersWhenLookupReturnsACard()
            throws InterruptedException {
        String clientToken = new TestClientTokenBuilder().build();
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
            throws InterruptedException {
        String clientToken = new TestClientTokenBuilder().build();
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

    @Test(timeout = 10000)
    public void performVerification_acceptsACardBuilderAndPostsAPaymentMethodNonceToListener()
            throws InterruptedException {
        BraintreeFragment fragment = getFragmentWithAuthorization(mActivity,
                new TestClientTokenBuilder().build());
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
}
