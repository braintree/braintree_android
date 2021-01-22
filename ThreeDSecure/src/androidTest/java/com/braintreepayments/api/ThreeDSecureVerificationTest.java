package com.braintreepayments.api;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import com.braintreepayments.api.exceptions.AuthorizationException;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCallback;
import com.braintreepayments.api.models.Authorization;
import com.braintreepayments.api.models.CardBuilder;
import com.braintreepayments.api.models.CardNonce;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.api.test.BraintreeActivityTestRule;
import com.braintreepayments.api.test.TestActivity;
import com.braintreepayments.api.test.TestClientTokenBuilder;
import com.braintreepayments.testutils.ExpirationDateHelper;
import com.braintreepayments.testutils.Fixtures;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;

import static com.braintreepayments.testutils.Assertions.assertIsANonce;
import static com.braintreepayments.testutils.CardNumber.THREE_D_SECURE_AUTHENTICATION_UNAVAILABLE;
import static com.braintreepayments.testutils.CardNumber.THREE_D_SECURE_LOOKUP_ERROR;
import static com.braintreepayments.testutils.CardNumber.THREE_D_SECURE_MPI_LOOKUP_ERROR;
import static com.braintreepayments.testutils.CardNumber.THREE_D_SECURE_VERIFICATON;
import static com.braintreepayments.testutils.CardNumber.THREE_D_SECURE_VERIFICATON_NOT_REQUIRED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4ClassRunner.class)
public class ThreeDSecureVerificationTest {

    private static final String TEST_AMOUNT = "1";

    @Rule
    public final BraintreeActivityTestRule<TestActivity> mActivityTestRule =
            new BraintreeActivityTestRule<>(TestActivity.class);

    private String returnUrlScheme;
    private AppCompatActivity mActivity;
    private CountDownLatch mCountDownLatch;

    @Before
    public void setUp() {
        mActivity = mActivityTestRule.getActivity();
        mCountDownLatch = new CountDownLatch(1);

        returnUrlScheme = "com.braintreepayments.api.test.braintree";
    }

    @Test(timeout = 10000)
    public void performVerification_doesALookupAndReturnsACardAndANullACSUrlWhenAuthenticationIsNotRequired()
            throws InterruptedException, InvalidArgumentException {

        final CardBuilder cardBuilder = new CardBuilder()
                .cardNumber(THREE_D_SECURE_VERIFICATON_NOT_REQUIRED)
                .expirationDate("12/20");

        BraintreeClient braintreeClient = getBraintreeClient();
        TokenizationClient tokenizationClient = new TokenizationClient(braintreeClient);
        final ThreeDSecureClient threeDSecureClient = new ThreeDSecureClient(braintreeClient, returnUrlScheme);

        tokenizationClient.tokenize(cardBuilder, new PaymentMethodNonceCallback() {
            @Override
            public void success(PaymentMethodNonce paymentMethodNonce) {
                ThreeDSecureRequest request = new ThreeDSecureRequest()
                        .nonce(paymentMethodNonce.getNonce())
                        .amount(TEST_AMOUNT);
                threeDSecureClient.performVerification(mActivity, request, new ThreeDSecureResultCallback() {
                    @Override
                    public void onResult(@Nullable PaymentMethodNonce paymentMethodNonce, @Nullable Exception error) {
                        CardNonce cardNonce = (CardNonce) paymentMethodNonce;

                        assertNotNull(cardNonce);
                        assertIsANonce(cardNonce.getNonce());
                        assertEquals("51", cardNonce.getLastTwo());
                        assertFalse(cardNonce.getThreeDSecureInfo().isLiabilityShifted());
                        assertFalse(cardNonce.getThreeDSecureInfo().isLiabilityShiftPossible());
                        assertTrue(cardNonce.getThreeDSecureInfo().wasVerified());

                        mCountDownLatch.countDown();
                    }
                });
            }

            @Override
            public void failure(Exception exception) {
                fail("Tokenization should succeed");
            }
        });

        mCountDownLatch.await();
    }

    @Test(timeout = 10000)
    public void performVerification_doesALookupAndReturnsACardWhenAuthenticationIsUnavailable()
            throws InterruptedException, InvalidArgumentException {

        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber(THREE_D_SECURE_AUTHENTICATION_UNAVAILABLE)
                .expirationMonth("12")
                .expirationYear(ExpirationDateHelper.validExpirationYear());

        BraintreeClient braintreeClient = getBraintreeClient();
        TokenizationClient tokenizationClient = new TokenizationClient(braintreeClient);
        final ThreeDSecureClient threeDSecureClient = new ThreeDSecureClient(braintreeClient, returnUrlScheme);

        tokenizationClient.tokenize(cardBuilder, new PaymentMethodNonceCallback() {
            @Override
            public void success(PaymentMethodNonce paymentMethodNonce) {
                ThreeDSecureRequest request = new ThreeDSecureRequest()
                        .nonce(paymentMethodNonce.getNonce())
                        .amount(TEST_AMOUNT);
                threeDSecureClient.performVerification(mActivity, request, new ThreeDSecureResultCallback() {
                    @Override
                    public void onResult(@Nullable PaymentMethodNonce paymentMethodNonce, @Nullable Exception error) {
                        CardNonce cardNonce = (CardNonce) paymentMethodNonce;
                        assertNotNull(cardNonce);
                        assertIsANonce(cardNonce.getNonce());

                        assertEquals("69", cardNonce.getLastTwo());
                        assertFalse(cardNonce.getThreeDSecureInfo().isLiabilityShifted());
                        assertFalse(cardNonce.getThreeDSecureInfo().isLiabilityShiftPossible());
                        assertTrue(cardNonce.getThreeDSecureInfo().wasVerified());

                        mCountDownLatch.countDown();
                    }
                });
            }

            @Override
            public void failure(Exception exception) {
                fail("Tokenization should succeed");
            }
        });

        mCountDownLatch.await();
    }

    @Test(timeout = 10000)
    public void performVerification_failsWithATokenizationKey() throws InterruptedException, InvalidArgumentException {

        final CardBuilder cardBuilder = new CardBuilder()
                .cardNumber(THREE_D_SECURE_VERIFICATON)
                .expirationDate("12/20");

        BraintreeClient braintreeClient = getBraintreeClient(Fixtures.TOKENIZATION_KEY);
        TokenizationClient tokenizationClient = new TokenizationClient(braintreeClient);
        final ThreeDSecureClient threeDSecureClient = new ThreeDSecureClient(braintreeClient, returnUrlScheme);

        tokenizationClient.tokenize(cardBuilder, new PaymentMethodNonceCallback() {
            @Override
            public void success(PaymentMethodNonce paymentMethodNonce) {
                ThreeDSecureRequest request = new ThreeDSecureRequest()
                        .nonce(paymentMethodNonce.getNonce())
                        .amount(TEST_AMOUNT);
                threeDSecureClient.performVerification(mActivity, request, new ThreeDSecureResultCallback() {
                    @Override
                    public void onResult(@Nullable PaymentMethodNonce paymentMethodNonce, @Nullable Exception error) {
                        assertTrue(error instanceof AuthorizationException);
                        assertEquals(
                                "Tokenization key authorization not allowed for this endpoint. Please use an authentication method with upgraded permissions",
                                error.getMessage());
                        mCountDownLatch.countDown();
                    }
                });
            }

            @Override
            public void failure(Exception exception) {
                fail("Tokenization should succeed");
            }
        });

        mCountDownLatch.await();
    }

    @Test(timeout = 10000)
    public void performVerification_doesALookupAndReturnsACardWhenThereIsALookupError() throws InterruptedException, InvalidArgumentException {
        final CardBuilder cardBuilder = new CardBuilder()
                .cardNumber(THREE_D_SECURE_LOOKUP_ERROR)
                .expirationDate("12/20");

        BraintreeClient braintreeClient = getBraintreeClient();
        TokenizationClient tokenizationClient = new TokenizationClient(braintreeClient);
        final ThreeDSecureClient threeDSecureClient = new ThreeDSecureClient(braintreeClient, returnUrlScheme);

        tokenizationClient.tokenize(cardBuilder, new PaymentMethodNonceCallback() {
            @Override
            public void success(PaymentMethodNonce paymentMethodNonce) {
                ThreeDSecureRequest request = new ThreeDSecureRequest()
                        .nonce(paymentMethodNonce.getNonce())
                        .amount(TEST_AMOUNT);
                threeDSecureClient.performVerification(mActivity, request, new ThreeDSecureResultCallback() {
                    @Override
                    public void onResult(@Nullable PaymentMethodNonce paymentMethodNonce, @Nullable Exception error) {
                        CardNonce cardNonce = (CardNonce) paymentMethodNonce;

                        assertNotNull(cardNonce);
                        assertIsANonce(cardNonce.getNonce());

                        assertEquals("77", cardNonce.getLastTwo());
                        assertFalse(cardNonce.getThreeDSecureInfo().isLiabilityShifted());
                        assertFalse(cardNonce.getThreeDSecureInfo().isLiabilityShiftPossible());
                        assertTrue(cardNonce.getThreeDSecureInfo().wasVerified());

                        assertEquals("lookup_error", cardNonce.getThreeDSecureInfo().getStatus());

                        mCountDownLatch.countDown();
                    }
                });
            }

            @Override
            public void failure(Exception exception) {
                fail("Tokenization should succeed");
            }
        });

        mCountDownLatch.await();
    }

    @Test(timeout = 10000)
    public void performVerification_doesALookupAndReturnsACardWhenThereIsAMPILookupError() throws InterruptedException, InvalidArgumentException {
        final CardBuilder cardBuilder = new CardBuilder()
                .cardNumber(THREE_D_SECURE_MPI_LOOKUP_ERROR)
                .expirationDate("12/20");

        BraintreeClient braintreeClient = getBraintreeClient();
        TokenizationClient tokenizationClient = new TokenizationClient(braintreeClient);
        final ThreeDSecureClient threeDSecureClient = new ThreeDSecureClient(braintreeClient, returnUrlScheme);

        tokenizationClient.tokenize(cardBuilder, new PaymentMethodNonceCallback() {
            @Override
            public void success(PaymentMethodNonce paymentMethodNonce) {
                ThreeDSecureRequest request = new ThreeDSecureRequest()
                        .nonce(paymentMethodNonce.getNonce())
                        .amount(TEST_AMOUNT);
                threeDSecureClient.performVerification(mActivity, request, new ThreeDSecureResultCallback() {
                    @Override
                    public void onResult(@Nullable PaymentMethodNonce paymentMethodNonce, @Nullable Exception error) {
                        CardNonce cardNonce = (CardNonce) paymentMethodNonce;

                        assertNotNull(cardNonce);
                        assertIsANonce(cardNonce.getNonce());

                        assertEquals("85", cardNonce.getLastTwo());
                        assertFalse(cardNonce.getThreeDSecureInfo().isLiabilityShifted());
                        assertFalse(cardNonce.getThreeDSecureInfo().isLiabilityShiftPossible());
                        assertTrue(cardNonce.getThreeDSecureInfo().wasVerified());

                        mCountDownLatch.countDown();
                    }
                });
            }

            @Override
            public void failure(Exception exception) {
                fail("Tokenization should succeed");
            }
        });

        mCountDownLatch.await();
    }

    /* helpers */
    private BraintreeClient getBraintreeClient() throws InvalidArgumentException {
        String clientToken = new TestClientTokenBuilder().build();
        return getBraintreeClient(clientToken);
    }

    private BraintreeClient getBraintreeClient(String authorization) throws InvalidArgumentException {
        return new BraintreeClient(Authorization.fromString(authorization), mActivity, returnUrlScheme);
    }
}
