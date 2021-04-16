package com.braintreepayments.api;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import org.json.JSONException;
import org.json.JSONObject;
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

        BraintreeClient braintreeClient = new BraintreeClient(mActivity, clientToken);
        TokenizationClient tokenizationClient = new TokenizationClient(braintreeClient);
        threeDSecureClient = new ThreeDSecureClient(braintreeClient);

        Card card = new Card();
        card.setNumber("4000000000000051");
        card.setExpirationDate("12/20");

        tokenizationClient.tokenize(card, new TokenizeCallback() {
            @Override
            public void onResult(JSONObject tokenizationResponse, Exception exception) {
                if (exception != null) {
                    fail(exception.getMessage());
                }

                CardNonce cardNonce = null;
                try {
                    cardNonce = new CardNonce(tokenizationResponse);
                } catch (JSONException e) {
                    fail("This should not fail");
                }
                String nonce = cardNonce.getString();

                ThreeDSecureRequest request = new ThreeDSecureRequest();
                request.setNonce(nonce);
                request.setAmount("5");

                threeDSecureClient.performVerification(mActivity, request, new ThreeDSecureResultCallback() {
                    @Override
                    public void onResult(@Nullable ThreeDSecureResult threeDSecureResult, @Nullable Exception error) {
                        CardNonce cardNonce = threeDSecureResult.getTokenizedCard();

                        assertIsANonce(cardNonce.getString());
                        assertEquals("51", cardNonce.getLastTwo());

                        ThreeDSecureInfo threeDSecureInfo = cardNonce.getThreeDSecureInfo();
                        assertFalse(threeDSecureInfo.isLiabilityShifted());
                        assertFalse(threeDSecureInfo.isLiabilityShiftPossible());
                        assertTrue(cardNonce.getThreeDSecureInfo().wasVerified());
                        mCountDownLatch.countDown();
                    }
                });
            }
        });

        mCountDownLatch.await();
    }
}
