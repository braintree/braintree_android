package com.braintreepayments.api;

import android.app.Activity;
import android.support.test.runner.AndroidJUnit4;

import com.braintreepayments.api.exceptions.AuthorizationException;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.BraintreeCancelListener;
import com.braintreepayments.api.interfaces.BraintreeErrorListener;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCreatedListener;
import com.braintreepayments.api.models.Authorization;
import com.braintreepayments.api.models.BraintreeRequestCodes;
import com.braintreepayments.api.models.CardBuilder;
import com.braintreepayments.api.models.CardNonce;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.api.test.BraintreeActivityTestRule;
import com.braintreepayments.api.test.TestClientTokenBuilder;
import com.braintreepayments.demo.test.DemoTestActivity;
import com.lukekorth.deviceautomator.AutomatorAction;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static com.braintreepayments.api.test.Assertions.assertIsANonce;
import static com.braintreepayments.api.test.Matchers.withId;
import static com.braintreepayments.api.test.ViewHelper.waitForView;
import static com.braintreepayments.testutils.CardNumber.THREE_D_SECURE_AUTHENTICATION_FAILED;
import static com.braintreepayments.testutils.CardNumber.THREE_D_SECURE_AUTHENTICATION_UNAVAILABLE;
import static com.braintreepayments.testutils.CardNumber.THREE_D_SECURE_ISSUER_DOES_NOT_PARTICIPATE;
import static com.braintreepayments.testutils.CardNumber.THREE_D_SECURE_ISSUER_DOWN;
import static com.braintreepayments.testutils.CardNumber.THREE_D_SECURE_LOOKUP_ERROR;
import static com.braintreepayments.testutils.CardNumber.THREE_D_SECURE_LOOKUP_TIMEOUT;
import static com.braintreepayments.testutils.CardNumber.THREE_D_SECURE_MPI_LOOKUP_ERROR;
import static com.braintreepayments.testutils.CardNumber.THREE_D_SECURE_MPI_SERVICE_ERROR;
import static com.braintreepayments.testutils.CardNumber.THREE_D_SECURE_SIGNATURE_VERIFICATION_FAILURE;
import static com.braintreepayments.testutils.CardNumber.THREE_D_SECURE_VERIFICATON;
import static com.braintreepayments.testutils.CardNumber.THREE_D_SECURE_VERIFICATON_NOT_REQUIRED;
import static com.braintreepayments.testutils.SharedPreferencesHelper.writeMockConfiguration;
import static com.braintreepayments.testutils.TestTokenizationKey.TOKENIZATION_KEY;
import static com.lukekorth.deviceautomator.DeviceAutomator.onDevice;
import static com.lukekorth.deviceautomator.UiObjectMatcher.withText;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

@RunWith(AndroidJUnit4.class)
public class ThreeDSecureVerificationTest {

    private static final String TEST_AMOUNT = "1";

    @Rule
    public final BraintreeActivityTestRule<DemoTestActivity> mActivityTestRule =
            new BraintreeActivityTestRule<>(DemoTestActivity.class);

    private Activity mActivity;
    private CountDownLatch mCountDownLatch;

    @Before
    public void setUp() {
        mActivity = mActivityTestRule.getActivity();
        mCountDownLatch = new CountDownLatch(1);
    }

    @Test(timeout = 30000)
    public void performVerification_callsCancelListenerWhenUpIsPressed() throws InterruptedException {
        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber(THREE_D_SECURE_VERIFICATON)
                .expirationDate("12/30");
        BraintreeFragment fragment = getFragment();
        fragment.addListener(new BraintreeCancelListener() {
            @Override
            public void onCancel(int requestCode) {
                assertEquals(BraintreeRequestCodes.THREE_D_SECURE, requestCode);
                mCountDownLatch.countDown();
            }
        });
        ThreeDSecure.performVerification(getFragment(), cardBuilder, TEST_AMOUNT);

        waitForView(withId(android.R.id.widget_frame));
        onView(withContentDescription("Navigate up")).perform(click());

        mCountDownLatch.await();
    }

    @Test(timeout = 30000)
    public void performVerification_callsCancelListenerWhenBackIsPressed() throws InterruptedException {
        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber(THREE_D_SECURE_VERIFICATON)
                .expirationDate("12/30");
        BraintreeFragment fragment = getFragment();
        fragment.addListener(new BraintreeCancelListener() {
            @Override
            public void onCancel(int requestCode) {
                assertEquals(BraintreeRequestCodes.THREE_D_SECURE, requestCode);
                mCountDownLatch.countDown();
            }
        });
        ThreeDSecure.performVerification(fragment, cardBuilder, TEST_AMOUNT);

        waitForView(withId(android.R.id.widget_frame));
        pressBack();

        mCountDownLatch.await();
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

                mCountDownLatch.countDown();
            }
        });
        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber(THREE_D_SECURE_VERIFICATON_NOT_REQUIRED)
                .expirationDate("12/20");

        ThreeDSecure.performVerification(fragment, cardBuilder, TEST_AMOUNT);

        mCountDownLatch.await();
    }

    @Test(timeout = 30000)
    public void performVerification_doesALookupAndReturnsACardAfterATimeout() throws InterruptedException {
        BraintreeFragment fragment = getFragment();
        fragment.addListener(new PaymentMethodNonceCreatedListener() {
            @Override
            public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
                assertIsANonce(paymentMethodNonce.getNonce());
                CardNonce cardNonce = (CardNonce) paymentMethodNonce;

                assertEquals("44", cardNonce.getLastTwo());
                assertFalse(cardNonce.getThreeDSecureInfo().isLiabilityShifted());
                assertFalse(cardNonce.getThreeDSecureInfo().isLiabilityShiftPossible());

                mCountDownLatch.countDown();
            }
        });
        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber(THREE_D_SECURE_LOOKUP_TIMEOUT)
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
        String clientToken = new TestClientTokenBuilder().withThreeDSecure().build();
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

                mCountDownLatch.countDown();
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

                mCountDownLatch.countDown();
            }
        });
        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber(THREE_D_SECURE_MPI_LOOKUP_ERROR)
                .expirationDate("12/20");

        ThreeDSecure.performVerification(fragment, cardBuilder, TEST_AMOUNT);

        mCountDownLatch.await();
    }

    @Test(timeout = 30000)
    public void performVerification_requestsAuthenticationWhenRequired() throws InterruptedException {
        BraintreeFragment fragment = getFragment();
        fragment.addListener(new PaymentMethodNonceCreatedListener() {
            @Override
            public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
                assertIsANonce(paymentMethodNonce.getNonce());
                CardNonce cardNonce = (CardNonce) paymentMethodNonce;

                assertEquals("02", cardNonce.getLastTwo());
                assertTrue(cardNonce.getThreeDSecureInfo().isLiabilityShifted());
                assertTrue(cardNonce.getThreeDSecureInfo().isLiabilityShiftPossible());

                mCountDownLatch.countDown();
            }
        });
        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber(THREE_D_SECURE_VERIFICATON)
                .expirationDate("12/30");
        ThreeDSecure.performVerification(fragment, cardBuilder, TEST_AMOUNT);

        waitForView(withId(android.R.id.widget_frame));

        try {
            onDevice(withText("Password:")).perform(AutomatorAction.click());
        } catch (RuntimeException ignored) {}

        onDevice().pressTab().typeText("1234");
        onDevice(withText("Submit")).perform(AutomatorAction.click());

        mCountDownLatch.await();
    }

    @Test(timeout = 30000)
    public void performVerification_returnsAnErrorWhenAuthenticationFails() throws InterruptedException {
        BraintreeFragment fragment = getFragment();
        fragment.addListener(new BraintreeErrorListener() {
            @Override
            public void onError(Exception error) {
                assertEquals("Failed to authenticate, please try a different form of payment.",
                        error.getMessage());
                mCountDownLatch.countDown();
            }
        });
        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber(THREE_D_SECURE_AUTHENTICATION_FAILED)
                .expirationDate("12/30");
        ThreeDSecure.performVerification(fragment, cardBuilder, TEST_AMOUNT);

        waitForView(withId(android.R.id.widget_frame));

        try {
            onDevice(withText("Password:")).perform(AutomatorAction.click());
        } catch (RuntimeException ignored) {}

        onDevice().pressTab().typeText("1234");
        onDevice(withText("Submit")).perform(AutomatorAction.click());

        mCountDownLatch.await();
    }

    @Test(timeout = 30000)
    public void performVerification_returnsASuccessfulAuthenticationWhenIssuerDoesNotParticipate()
            throws InterruptedException {
        BraintreeFragment fragment = getFragment();
        fragment.addListener(new PaymentMethodNonceCreatedListener() {
            @Override
            public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
                assertIsANonce(paymentMethodNonce.getNonce());
                CardNonce cardNonce = (CardNonce) paymentMethodNonce;

                assertEquals("01", cardNonce.getLastTwo());
                assertTrue(cardNonce.getThreeDSecureInfo().isLiabilityShifted());
                assertTrue(cardNonce.getThreeDSecureInfo().isLiabilityShiftPossible());

                mCountDownLatch.countDown();
            }
        });
        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber(THREE_D_SECURE_ISSUER_DOES_NOT_PARTICIPATE)
                .expirationDate("12/30");

        ThreeDSecure.performVerification(fragment, cardBuilder, TEST_AMOUNT);

        mCountDownLatch.await();
    }

    @Test(timeout = 30000)
    public void performVerification_returnsAFailedAuthenticationWhenSignatureVerificationFails()
            throws InterruptedException {
        BraintreeFragment fragment = getFragment();
        fragment.addListener(new BraintreeErrorListener() {
            @Override
            public void onError(Exception error) {
                assertEquals("Failed to authenticate, please try a different form of payment.",
                        error.getMessage());
                mCountDownLatch.countDown();
            }
        });
        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber(THREE_D_SECURE_SIGNATURE_VERIFICATION_FAILURE)
                .expirationDate("12/30");
        ThreeDSecure.performVerification(fragment, cardBuilder, TEST_AMOUNT);

        waitForView(withId(android.R.id.widget_frame));

        try {
            onDevice(withText("Password:")).perform(AutomatorAction.click());
        } catch (RuntimeException ignored) {}

        onDevice().pressTab().typeText("1234");
        onDevice(withText("Submit")).perform(AutomatorAction.click());

        mCountDownLatch.await();
    }

    @Test(timeout = 30000)
    public void performVerification_returnsAnUnexpectedErrorWhenIssuerIsDown() throws InterruptedException {
        BraintreeFragment fragment = getFragment();
        fragment.addListener(new BraintreeErrorListener() {
            @Override
            public void onError(Exception error) {
                assertEquals("An unexpected error occurred", error.getMessage());
                mCountDownLatch.countDown();
            }
        });
        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber(THREE_D_SECURE_ISSUER_DOWN)
                .expirationDate("12/30");
        ThreeDSecure.performVerification(fragment, cardBuilder, TEST_AMOUNT);

        waitForView(withId(android.R.id.widget_frame));

        try {
            onDevice(withText("Password:")).perform(AutomatorAction.click());
        } catch (RuntimeException ignored) {}

        onDevice().pressTab().typeText("1234");
        onDevice(withText("Submit")).perform(AutomatorAction.click());

        mCountDownLatch.await();
    }

    @Test(timeout = 30000)
    public void performVerification_returnsAnErrorWhenCardinalReturnsError()
            throws InterruptedException {
        BraintreeFragment fragment = getFragment();
        fragment.addListener(new BraintreeErrorListener() {
            @Override
            public void onError(Exception error) {
                assertEquals("An unexpected error occurred", error.getMessage());
                mCountDownLatch.countDown();
            }
        });
        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber(THREE_D_SECURE_MPI_SERVICE_ERROR)
                .expirationDate("12/30");
        ThreeDSecure.performVerification(fragment, cardBuilder, TEST_AMOUNT);

        waitForView(withId(android.R.id.widget_frame));

        try {
            onDevice(withText("Password:")).perform(AutomatorAction.click());
        } catch (RuntimeException ignored) {}

        onDevice().pressTab().typeText("1234");
        onDevice(withText("Submit")).perform(AutomatorAction.click());

        mCountDownLatch.await();
    }

    /* helpers */
    private BraintreeFragment getFragment() {
        String clientToken = new TestClientTokenBuilder().withThreeDSecure().build();
        return getFragment(clientToken, clientToken);
    }

    private BraintreeFragment getFragment(String authorization, String configuration) {
        try {
            Authorization auth = Authorization.fromString(authorization);
            writeMockConfiguration(getTargetContext(), auth.getConfigUrl(), auth.getBearer(), configuration);

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
