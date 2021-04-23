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
import static com.braintreepayments.api.CardNumber.THREE_D_SECURE_AUTHENTICATION_UNAVAILABLE;
import static com.braintreepayments.api.CardNumber.THREE_D_SECURE_LOOKUP_ERROR;
import static com.braintreepayments.api.CardNumber.THREE_D_SECURE_MPI_LOOKUP_ERROR;
import static com.braintreepayments.api.CardNumber.THREE_D_SECURE_VERIFICATON;
import static com.braintreepayments.api.CardNumber.THREE_D_SECURE_VERIFICATON_NOT_REQUIRED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4ClassRunner.class)
public class ThreeDSecureVerificationTest {

    private static final String TEST_AMOUNT = "1";

    @Rule
    public final BraintreeActivityTestRule<TestActivity> activityTestRule =
            new BraintreeActivityTestRule<>(TestActivity.class);

    private AppCompatActivity activity;
    private CountDownLatch countDownLatch;

    @Before
    public void setUp() {
        activity = activityTestRule.getActivity();
        countDownLatch = new CountDownLatch(1);
    }

    @Test(timeout = 10000)
    public void performVerification_doesALookupAndReturnsACardAndANullACSUrlWhenAuthenticationIsNotRequired()
            throws InterruptedException, InvalidArgumentException {

        final Card card = new Card();
        card.setNumber(THREE_D_SECURE_VERIFICATON_NOT_REQUIRED);
        card.setExpirationDate("12/20");

        BraintreeClient braintreeClient = getBraintreeClient();
        TokenizationClient tokenizationClient = new TokenizationClient(braintreeClient);
        final ThreeDSecureClient threeDSecureClient = new ThreeDSecureClient(braintreeClient);

        tokenizationClient.tokenize(card, new TokenizeCallback() {
            @Override
            public void onResult(JSONObject tokenizationResponse, Exception exception) {
                if (exception != null) {
                    fail(exception.getMessage());
                }

                ThreeDSecureRequest request = new ThreeDSecureRequest();
                try {
                    CardNonce cardNonce = CardNonce.fromJSON(tokenizationResponse);
                    request.setNonce(cardNonce.getString());
                    request.setAmount(TEST_AMOUNT);

                    threeDSecureClient.performVerification(activity, request, new ThreeDSecureResultCallback() {
                        @Override
                        public void onResult(ThreeDSecureResult threeDSecureResult, Exception error) {
                            CardNonce cardNonce = threeDSecureResult.getTokenizedCard();

                            assertNotNull(cardNonce);
                            assertIsANonce(cardNonce.getString());
                            assertEquals("51", cardNonce.getLastTwo());
                            assertFalse(cardNonce.getThreeDSecureInfo().isLiabilityShifted());
                            assertFalse(cardNonce.getThreeDSecureInfo().isLiabilityShiftPossible());
                            assertTrue(cardNonce.getThreeDSecureInfo().wasVerified());

                            countDownLatch.countDown();
                        }
                    });
                } catch (JSONException e) {
                    fail("This should not fail");
                }
            }
        });

        countDownLatch.await();
    }

    @Test(timeout = 10000)
    public void performVerification_doesALookupAndReturnsACardWhenAuthenticationIsUnavailable()
            throws InterruptedException, InvalidArgumentException {

        Card card = new Card();
        card.setNumber(THREE_D_SECURE_AUTHENTICATION_UNAVAILABLE);
        card.setExpirationMonth("12");
        card.setExpirationYear(ExpirationDateHelper.validExpirationYear());

        BraintreeClient braintreeClient = getBraintreeClient();
        TokenizationClient tokenizationClient = new TokenizationClient(braintreeClient);
        final ThreeDSecureClient threeDSecureClient = new ThreeDSecureClient(braintreeClient);

        tokenizationClient.tokenize(card, new TokenizeCallback() {
            @Override
            public void onResult(JSONObject tokenizationResponse, Exception exception) {
                if (exception != null) {
                    fail(exception.getMessage());
                }

                ThreeDSecureRequest request = new ThreeDSecureRequest();
                try {
                    CardNonce cardNonce = CardNonce.fromJSON(tokenizationResponse);
                    request.setNonce(cardNonce.getString());
                    request.setAmount(TEST_AMOUNT);

                    threeDSecureClient.performVerification(activity, request, new ThreeDSecureResultCallback() {
                        @Override
                        public void onResult(ThreeDSecureResult threeDSecureResult, Exception error) {
                            CardNonce cardNonce = threeDSecureResult.getTokenizedCard();
                            assertNotNull(cardNonce);
                            assertIsANonce(cardNonce.getString());

                            assertEquals("69", cardNonce.getLastTwo());
                            assertFalse(cardNonce.getThreeDSecureInfo().isLiabilityShifted());
                            assertFalse(cardNonce.getThreeDSecureInfo().isLiabilityShiftPossible());
                            assertTrue(cardNonce.getThreeDSecureInfo().wasVerified());

                            countDownLatch.countDown();
                        }
                    });
                } catch (JSONException e) {
                    fail("This should not fail");
                }
            }
        });

        countDownLatch.await();
    }

    @Test(timeout = 10000)
    public void performVerification_failsWithATokenizationKey() throws InterruptedException, InvalidArgumentException {

        final Card card = new Card();
        card.setNumber(THREE_D_SECURE_VERIFICATON);
        card.setExpirationDate("12/20");

        BraintreeClient braintreeClient = getBraintreeClient(Fixtures.TOKENIZATION_KEY);
        TokenizationClient tokenizationClient = new TokenizationClient(braintreeClient);
        final ThreeDSecureClient threeDSecureClient = new ThreeDSecureClient(braintreeClient);

        tokenizationClient.tokenize(card, new TokenizeCallback() {
            @Override
            public void onResult(JSONObject tokenizationResponse, Exception exception) {
                if (exception != null) {
                    fail(exception.getMessage());
                }

                ThreeDSecureRequest request = new ThreeDSecureRequest();
                try {
                    CardNonce cardNonce = CardNonce.fromJSON(tokenizationResponse);
                    request.setNonce(cardNonce.getString());
                    request.setAmount(TEST_AMOUNT);

                    threeDSecureClient.performVerification(activity, request, new ThreeDSecureResultCallback() {
                        @Override
                        public void onResult(@Nullable ThreeDSecureResult threeDSecureResult, @Nullable Exception error) {
                            assertTrue(error instanceof AuthorizationException);
                            assertEquals(
                                    "Tokenization key authorization not allowed for this endpoint. Please use an authentication method with upgraded permissions",
                                    error.getMessage());
                            countDownLatch.countDown();
                        }
                    });
                } catch (JSONException e) {
                    fail("This should not fail");
                }
            }
        });

        countDownLatch.await();
    }

    @Test(timeout = 10000)
    public void performVerification_doesALookupAndReturnsACardWhenThereIsALookupError() throws InterruptedException, InvalidArgumentException {
        final Card card = new Card();
        card.setNumber(THREE_D_SECURE_LOOKUP_ERROR);
        card.setExpirationDate("12/20");

        BraintreeClient braintreeClient = getBraintreeClient();
        TokenizationClient tokenizationClient = new TokenizationClient(braintreeClient);
        final ThreeDSecureClient threeDSecureClient = new ThreeDSecureClient(braintreeClient);

        tokenizationClient.tokenize(card, new TokenizeCallback() {
            @Override
            public void onResult(JSONObject tokenizationResponse, Exception exception) {
                if (exception != null) {
                    fail(exception.getMessage());
                }

                ThreeDSecureRequest request = new ThreeDSecureRequest();
                try {
                    CardNonce cardNonce = CardNonce.fromJSON(tokenizationResponse);
                    request.setNonce(cardNonce.getString());
                    request.setAmount(TEST_AMOUNT);

                    threeDSecureClient.performVerification(activity, request, new ThreeDSecureResultCallback() {
                        @Override
                        public void onResult(@Nullable ThreeDSecureResult threeDSecureResult, @Nullable Exception error) {
                            CardNonce cardNonce = threeDSecureResult.getTokenizedCard();

                            assertNotNull(cardNonce);
                            assertIsANonce(cardNonce.getString());

                            assertEquals("77", cardNonce.getLastTwo());
                            assertFalse(cardNonce.getThreeDSecureInfo().isLiabilityShifted());
                            assertFalse(cardNonce.getThreeDSecureInfo().isLiabilityShiftPossible());
                            assertTrue(cardNonce.getThreeDSecureInfo().wasVerified());

                            assertEquals("lookup_error", cardNonce.getThreeDSecureInfo().getStatus());

                            countDownLatch.countDown();
                        }
                    });
                } catch (JSONException e) {
                    fail("This should not fail");
                }
            }
        });

        countDownLatch.await();
    }

    @Test(timeout = 10000)
    public void performVerification_doesALookupAndReturnsACardWhenThereIsAMPILookupError() throws InterruptedException, InvalidArgumentException {
        final Card card = new Card();
        card.setNumber(THREE_D_SECURE_MPI_LOOKUP_ERROR);
        card.setExpirationDate("12/20");

        BraintreeClient braintreeClient = getBraintreeClient();
        TokenizationClient tokenizationClient = new TokenizationClient(braintreeClient);
        final ThreeDSecureClient threeDSecureClient = new ThreeDSecureClient(braintreeClient);

        tokenizationClient.tokenize(card, new TokenizeCallback() {
            @Override
            public void onResult(JSONObject tokenizationResponse, Exception exception) {
                if (exception != null) {
                    fail(exception.getMessage());
                }

                ThreeDSecureRequest request = new ThreeDSecureRequest();
                try {
                    CardNonce cardNonce = CardNonce.fromJSON(tokenizationResponse);
                    request.setNonce(cardNonce.getString());
                    request.setAmount(TEST_AMOUNT);

                    threeDSecureClient.performVerification(activity, request, new ThreeDSecureResultCallback() {
                        @Override
                        public void onResult(@Nullable ThreeDSecureResult threeDSecureResult, @Nullable Exception error) {
                            CardNonce cardNonce = threeDSecureResult.getTokenizedCard();

                            assertNotNull(cardNonce);
                            assertIsANonce(cardNonce.getString());

                            assertEquals("85", cardNonce.getLastTwo());
                            assertFalse(cardNonce.getThreeDSecureInfo().isLiabilityShifted());
                            assertFalse(cardNonce.getThreeDSecureInfo().isLiabilityShiftPossible());
                            assertTrue(cardNonce.getThreeDSecureInfo().wasVerified());

                            countDownLatch.countDown();
                        }
                    });
                } catch (JSONException e) {
                    fail("This should not fail");
                }
            }
        });

        countDownLatch.await();
    }

    /* helpers */
    private BraintreeClient getBraintreeClient() throws InvalidArgumentException {
        String clientToken = new TestClientTokenBuilder().build();
        return getBraintreeClient(clientToken);
    }

    private BraintreeClient getBraintreeClient(String authorization) throws InvalidArgumentException {
        return new BraintreeClient(activity, authorization);
    }
}
