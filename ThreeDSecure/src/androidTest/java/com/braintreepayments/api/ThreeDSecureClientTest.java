package com.braintreepayments.api;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;

import static com.braintreepayments.api.Assertions.assertIsANonce;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4ClassRunner.class)
public class ThreeDSecureClientTest {

    @Rule
    public final BraintreeActivityTestRule<TestActivity> mActivityTestRule =
            new BraintreeActivityTestRule<>(TestActivity.class);

    private AppCompatActivity mActivity;
    private CountDownLatch mCountDownLatch;

    private ThreeDSecureClient threeDSecureClient;

    @Before
    public void setUp() {
        mActivity = mActivityTestRule.getActivity();
        mCountDownLatch = new CountDownLatch(1);
    }

    @Test(timeout = 10000)
    public void performVerification_acceptsAThreeDSecureRequest_postsPaymentMethodNonceToListenersWhenLookupReturnsACard()
            throws InterruptedException, InvalidArgumentException {
        String clientToken = new TestClientTokenBuilder().build();

        Authorization authorization = Authorization.fromString(clientToken);
        BraintreeClient braintreeClient = new BraintreeClient(mActivity, authorization);
        TokenizationClient tokenizationClient = new TokenizationClient(braintreeClient);
        threeDSecureClient = new ThreeDSecureClient(braintreeClient);

        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber("4000000000000051")
                .expirationDate("12/20");

        tokenizationClient.tokenize(cardBuilder, new PaymentMethodNonceCallback() {
            @Override
            public void success(PaymentMethodNonce paymentMethodNonce) {
                String nonce = paymentMethodNonce.getNonce();
                ThreeDSecureRequest request = new ThreeDSecureRequest()
                        .nonce(nonce)
                        .amount("5");
                threeDSecureClient.performVerification(mActivity, request, new ThreeDSecureResultCallback() {
                    @Override
                    public void onResult(@Nullable ThreeDSecureResult threeDSecureResult, @Nullable Exception error) {
                        CardNonce cardNonce = threeDSecureResult.getTokenizedCard();

                        assertIsANonce(cardNonce.getNonce());
                        assertEquals("51", cardNonce.getLastTwo());

                        ThreeDSecureInfo threeDSecureInfo = cardNonce.getThreeDSecureInfo();
                        assertFalse(threeDSecureInfo.isLiabilityShifted());
                        assertFalse(threeDSecureInfo.isLiabilityShiftPossible());
                        assertTrue(cardNonce.getThreeDSecureInfo().wasVerified());
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
}
