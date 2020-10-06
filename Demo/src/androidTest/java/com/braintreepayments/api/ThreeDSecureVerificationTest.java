package com.braintreepayments.api;

import androidx.appcompat.app.AppCompatActivity;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import com.braintreepayments.api.exceptions.AuthorizationException;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.BraintreeErrorListener;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCreatedListener;
import com.braintreepayments.api.models.Authorization;
import com.braintreepayments.api.models.CardBuilder;
import com.braintreepayments.api.models.CardNonce;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.api.test.BraintreeActivityTestRule;
import com.braintreepayments.api.test.TestClientTokenBuilder;
import com.braintreepayments.demo.test.DemoTestActivity;

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
import static com.braintreepayments.testutils.SharedPreferencesHelper.writeMockConfiguration;
import static com.braintreepayments.testutils.TestTokenizationKey.TOKENIZATION_KEY;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

@RunWith(AndroidJUnit4ClassRunner.class)
public class ThreeDSecureVerificationTest {

    private static final String TEST_AMOUNT = "1";

    @Rule
    public final BraintreeActivityTestRule<DemoTestActivity> mActivityTestRule =
            new BraintreeActivityTestRule<>(DemoTestActivity.class);

    private AppCompatActivity mActivity;
    private CountDownLatch mCountDownLatch;

    @Before
    public void setUp() {
        mActivity = mActivityTestRule.getActivity();
        mCountDownLatch = new CountDownLatch(1);
    }

    @Test(timeout = 10000)
    public void performVerification_doesALookupAndReturnsACardAndANullACSUrlWhenAuthenticationIsNotRequired()
            throws InterruptedException {
        BraintreeFragment fragment = getFragment();
        fragment.addListener(new PaymentMethodNonceCreatedListener() {
            @Override
            public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
                assertIsANonce(paymentMethodNonce.getNonce());
                CardNonce cardNonce = (CardNonce) paymentMethodNonce;

                assertEquals("51", cardNonce.getLastTwo());
                assertFalse(cardNonce.getThreeDSecureInfo().isLiabilityShifted());
                assertFalse(cardNonce.getThreeDSecureInfo().isLiabilityShiftPossible());
                assertTrue(((CardNonce) paymentMethodNonce).getThreeDSecureInfo().wasVerified());

                mCountDownLatch.countDown();
            }
        });
        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber(THREE_D_SECURE_VERIFICATON_NOT_REQUIRED)
                .expirationDate("12/20");

        ThreeDSecure.performVerification(fragment, cardBuilder, TEST_AMOUNT);

        mCountDownLatch.await();
    }

    @Test(timeout = 10000)
    public void performVerification_doesALookupAndReturnsACardWhenAuthenticationIsUnavailable()
            throws InterruptedException {
        BraintreeFragment fragment = getFragment();
        fragment.addListener(new PaymentMethodNonceCreatedListener() {
            @Override
            public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
                assertIsANonce(paymentMethodNonce.getNonce());
                CardNonce cardNonce = (CardNonce) paymentMethodNonce;

                assertEquals("69", cardNonce.getLastTwo());
                assertFalse(cardNonce.getThreeDSecureInfo().isLiabilityShifted());
                assertFalse(cardNonce.getThreeDSecureInfo().isLiabilityShiftPossible());
                assertTrue(((CardNonce) paymentMethodNonce).getThreeDSecureInfo().wasVerified());

                mCountDownLatch.countDown();
            }
        });
        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber(THREE_D_SECURE_AUTHENTICATION_UNAVAILABLE)
                .expirationDate("12/20");

        ThreeDSecure.performVerification(fragment, cardBuilder, TEST_AMOUNT);

        mCountDownLatch.await();
    }

    @Test(timeout = 10000)
    public void performVerification_failsWithATokenizationKey() throws InterruptedException {
        String clientToken = new TestClientTokenBuilder().build();
        BraintreeFragment fragment = getFragment(TOKENIZATION_KEY, clientToken);
        fragment.addListener(new BraintreeErrorListener() {
            @Override
            public void onError(Exception error) {
                assertTrue(error instanceof AuthorizationException);
                assertEquals(
                        "Tokenization key authorization not allowed for this endpoint. Please use an authentication method with upgraded permissions",
                        error.getMessage());
                mCountDownLatch.countDown();
            }
        });
        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber(THREE_D_SECURE_VERIFICATON)
                .expirationDate("12/20");

        ThreeDSecure.performVerification(fragment, cardBuilder, TEST_AMOUNT);

        mCountDownLatch.await();
    }

    @Test(timeout = 10000)
    public void performVerification_doesALookupAndReturnsACardWhenThereIsALookupError() throws InterruptedException {
        BraintreeFragment fragment = getFragment();
        fragment.addListener(new PaymentMethodNonceCreatedListener() {
            @Override
            public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
                assertIsANonce(paymentMethodNonce.getNonce());
                CardNonce cardNonce = (CardNonce) paymentMethodNonce;

                assertEquals("77", cardNonce.getLastTwo());
                assertFalse(cardNonce.getThreeDSecureInfo().isLiabilityShifted());
                assertFalse(cardNonce.getThreeDSecureInfo().isLiabilityShiftPossible());
                assertTrue(((CardNonce) paymentMethodNonce).getThreeDSecureInfo().wasVerified());

                mCountDownLatch.countDown();
            }
        });

        fragment.addListener(new BraintreeErrorListener() {
            @Override
            public void onError(Exception error) {
                fail(error.getMessage());
            }
        });

        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber(THREE_D_SECURE_LOOKUP_ERROR)
                .expirationDate("12/20");

        ThreeDSecure.performVerification(fragment, cardBuilder, TEST_AMOUNT);

        mCountDownLatch.await();
    }

    @Test(timeout = 10000)
    public void performVerification_doesALookupAndReturnsACardWhenThereIsAMPILookupError() throws InterruptedException {
        BraintreeFragment fragment = getFragment();
        fragment.addListener(new PaymentMethodNonceCreatedListener() {
            @Override
            public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
                assertIsANonce(paymentMethodNonce.getNonce());
                CardNonce cardNonce = (CardNonce) paymentMethodNonce;

                assertEquals("85", cardNonce.getLastTwo());
                assertFalse(cardNonce.getThreeDSecureInfo().isLiabilityShifted());
                assertFalse(cardNonce.getThreeDSecureInfo().isLiabilityShiftPossible());
                assertTrue(((CardNonce) paymentMethodNonce).getThreeDSecureInfo().wasVerified());

                mCountDownLatch.countDown();
            }
        });

        fragment.addListener(new BraintreeErrorListener() {
            @Override
            public void onError(Exception error) {
                fail(error.getMessage());
            }
        });

        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber(THREE_D_SECURE_MPI_LOOKUP_ERROR)
                .expirationDate("12/20");

        ThreeDSecure.performVerification(fragment, cardBuilder, TEST_AMOUNT);

        mCountDownLatch.await();
    }

    /* helpers */
    private BraintreeFragment getFragment() {
        String clientToken = new TestClientTokenBuilder().build();
        return getFragment(clientToken, clientToken);
    }

    private BraintreeFragment getFragment(String authorization, String configuration) {
        try {
            Authorization auth = Authorization.fromString(authorization);
            writeMockConfiguration(ApplicationProvider.getApplicationContext(), auth.getConfigUrl(), auth.getBearer(), configuration);

            BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity, authorization);

            while (!fragment.isAdded()) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ignored) {}
            }

            return fragment;
        } catch (InvalidArgumentException e) {
            fail(e.getMessage());
            return new BraintreeFragment();
        }
    }
}
