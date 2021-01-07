package com.braintreepayments.api;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCallback;
import com.braintreepayments.api.models.Authorization;
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

import static com.braintreepayments.testutils.Assertions.assertIsANonce;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4ClassRunner.class)
public class ThreeDSecureTest {

    @Rule
    public final BraintreeActivityTestRule<TestActivity> mActivityTestRule =
            new BraintreeActivityTestRule<>(TestActivity.class);

    private AppCompatActivity mActivity;
    private CountDownLatch mCountDownLatch;

    private String returnUrlScheme;
    private ThreeDSecure threeDSecure;

    @Before
    public void setUp() {
        mActivity = mActivityTestRule.getActivity();
        mCountDownLatch = new CountDownLatch(1);

        returnUrlScheme = "com.braintreepayments.api.test.braintree";
    }

    @Test(timeout = 10000)
    public void performVerification_postsPaymentMethodNonceToListenersWhenLookupReturnsACard()
            throws InterruptedException, InvalidArgumentException {
        String clientToken = new TestClientTokenBuilder().build();

        Authorization authorization = Authorization.fromString(clientToken);
        BraintreeClient braintreeClient = new BraintreeClient(authorization, mActivity, returnUrlScheme);
        TokenizationClient tokenizationClient = new TokenizationClient(braintreeClient);
        threeDSecure = new ThreeDSecure(braintreeClient, returnUrlScheme);

        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber("4000000000000051")
                .expirationDate("12/20");

        tokenizationClient.tokenize(mActivity, cardBuilder, new PaymentMethodNonceCallback() {
            @Override
            public void success(PaymentMethodNonce paymentMethodNonce) {
                String nonce = paymentMethodNonce.getNonce();
                threeDSecure.performVerification(mActivity, nonce, "5", new ThreeDSecureVerificationCallback() {
                    @Override
                    public void onResult(@Nullable PaymentMethodNonce paymentMethodNonce, @Nullable Exception error) {
                        assertIsANonce(paymentMethodNonce.getNonce());
                        assertEquals("51", ((CardNonce) paymentMethodNonce).getLastTwo());
                        assertTrue(((CardNonce) paymentMethodNonce).getThreeDSecureInfo().wasVerified());
                        mCountDownLatch.countDown();
                    }
                });
            }

            @Override
            public void failure(Exception exception) {
                fail(exception.getMessage());
            }
        });

        mCountDownLatch.await();
    }

    @Test(timeout = 10000)
    public void performVerification_acceptsAThreeDSecureRequest_postsPaymentMethodNonceToListenersWhenLookupReturnsACard()
            throws InterruptedException, InvalidArgumentException {
        String clientToken = new TestClientTokenBuilder().build();

        Authorization authorization = Authorization.fromString(clientToken);
        BraintreeClient braintreeClient = new BraintreeClient(authorization, mActivity, returnUrlScheme);
        TokenizationClient tokenizationClient = new TokenizationClient(braintreeClient);
        threeDSecure = new ThreeDSecure(braintreeClient, returnUrlScheme);

        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber("4000000000000051")
                .expirationDate("12/20");

        tokenizationClient.tokenize(mActivity, cardBuilder, new PaymentMethodNonceCallback() {
            @Override
            public void success(PaymentMethodNonce paymentMethodNonce) {
                String nonce = paymentMethodNonce.getNonce();
                ThreeDSecureRequest request = new ThreeDSecureRequest()
                        .nonce(nonce)
                        .amount("5");
                threeDSecure.performVerification(mActivity, request, new ThreeDSecureVerificationCallback() {
                    @Override
                    public void onResult(@Nullable PaymentMethodNonce paymentMethodNonce, @Nullable Exception error) {
                        assertIsANonce(paymentMethodNonce.getNonce());
                        assertEquals("51", ((CardNonce) paymentMethodNonce).getLastTwo());

                        ThreeDSecureInfo threeDSecureInfo = ((CardNonce) paymentMethodNonce).getThreeDSecureInfo();
                        assertFalse(threeDSecureInfo.isLiabilityShifted());
                        assertFalse(threeDSecureInfo.isLiabilityShiftPossible());
                        assertTrue(((CardNonce) paymentMethodNonce).getThreeDSecureInfo().wasVerified());
                        mCountDownLatch.countDown();
                    }
                });
            }

            @Override
            public void failure(Exception exception) {
                fail(exception.getMessage());
            }
        });

        mCountDownLatch.await();
    }

    @Test(timeout = 10000)
    public void performVerification_acceptsACardBuilderAndPostsAPaymentMethodNonceToListener()
            throws InterruptedException, InvalidArgumentException {
        String clientToken = new TestClientTokenBuilder().build();

        Authorization authorization = Authorization.fromString(clientToken);
        BraintreeClient braintreeClient = new BraintreeClient(authorization, mActivity, returnUrlScheme);
        threeDSecure = new ThreeDSecure(braintreeClient, returnUrlScheme);

        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber("4000000000000051")
                .expirationDate("12/20");

        threeDSecure.performVerification(mActivity, cardBuilder, "5", new ThreeDSecureVerificationCallback() {
            @Override
            public void onResult(@Nullable PaymentMethodNonce paymentMethodNonce, @Nullable Exception error) {
                assertIsANonce(paymentMethodNonce.getNonce());
                assertEquals("51", ((CardNonce) paymentMethodNonce).getLastTwo());
                assertTrue(((CardNonce) paymentMethodNonce).getThreeDSecureInfo().wasVerified());
                mCountDownLatch.countDown();
            }
        });

        mCountDownLatch.await();
    }
}
