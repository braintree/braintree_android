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
public class ThreeDSecureClientTest {

    @Rule
    public final BraintreeActivityTestRule<TestActivity> mActivityTestRule =
            new BraintreeActivityTestRule<>(TestActivity.class);

    private AppCompatActivity mActivity;
    private CountDownLatch mCountDownLatch;

    private String returnUrlScheme;
    private ThreeDSecureClient threeDSecureClient;

    @Before
    public void setUp() {
        mActivity = mActivityTestRule.getActivity();
        mCountDownLatch = new CountDownLatch(1);

        returnUrlScheme = "com.braintreepayments.api.test.braintree";
    }

    @Test(timeout = 10000)
    public void performVerification_acceptsAThreeDSecureRequest_postsPaymentMethodNonceToListenersWhenLookupReturnsACard()
            throws InterruptedException, InvalidArgumentException {
        String clientToken = new TestClientTokenBuilder().build();

        Authorization authorization = Authorization.fromString(clientToken);
        BraintreeClient braintreeClient = new BraintreeClient(authorization, mActivity, returnUrlScheme);
        TokenizationClient tokenizationClient = new TokenizationClient(braintreeClient);
        threeDSecureClient = new ThreeDSecureClient(braintreeClient, returnUrlScheme);

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
                    public void onResult(@Nullable PaymentMethodNonce paymentMethodNonce, @Nullable Exception error) {
                        CardNonce cardNonce = (CardNonce) paymentMethodNonce;

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
