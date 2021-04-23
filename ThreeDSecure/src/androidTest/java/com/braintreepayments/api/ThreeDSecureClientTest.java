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
    public final BraintreeActivityTestRule<TestActivity> activityTestRule =
            new BraintreeActivityTestRule<>(TestActivity.class);

    private AppCompatActivity activity;
    private CountDownLatch countDownLatch;

    private ThreeDSecureClient threeDSecureClient;

    @Before
    public void setUp() {
        activity = activityTestRule.getActivity();
        countDownLatch = new CountDownLatch(1);
    }

    @Test(timeout = 10000)
    public void performVerification_acceptsAThreeDSecureRequest_postsPaymentMethodNonceToListenersWhenLookupReturnsACard()
            throws InterruptedException, InvalidArgumentException {
        String clientToken = new TestClientTokenBuilder().build();

        BraintreeClient braintreeClient = new BraintreeClient(activity, clientToken);
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

                threeDSecureClient.performVerification(activity, request, new ThreeDSecureResultCallback() {
                    @Override
                    public void onResult(@Nullable ThreeDSecureResult threeDSecureResult, @Nullable Exception error) {
                        CardNonce cardNonce = threeDSecureResult.getTokenizedCard();

                        assertIsANonce(cardNonce.getString());
                        assertEquals("51", cardNonce.getLastTwo());

                        ThreeDSecureInfo threeDSecureInfo = cardNonce.getThreeDSecureInfo();
                        assertFalse(threeDSecureInfo.isLiabilityShifted());
                        assertFalse(threeDSecureInfo.isLiabilityShiftPossible());
                        assertTrue(cardNonce.getThreeDSecureInfo().wasVerified());
                        countDownLatch.countDown();
                    }
                });
            }
        });

        countDownLatch.await();
    }
}
