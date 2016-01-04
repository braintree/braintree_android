package com.braintreepayments.api;

import android.app.Activity;
import android.os.SystemClock;
import android.support.test.espresso.web.webdriver.Locator;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import android.test.suitebuilder.annotation.MediumTest;

import com.braintreepayments.api.exceptions.AuthorizationException;
import com.braintreepayments.api.interfaces.BraintreeCancelListener;
import com.braintreepayments.api.interfaces.BraintreeErrorListener;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCreatedListener;
import com.braintreepayments.api.models.CardBuilder;
import com.braintreepayments.api.models.CardNonce;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.api.test.TestActivity;
import com.braintreepayments.testutils.BraintreeActivityTestRule;
import com.braintreepayments.testutils.TestClientTokenBuilder;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.web.sugar.Web.onWebView;
import static android.support.test.espresso.web.webdriver.DriverAtoms.findElement;
import static android.support.test.espresso.web.webdriver.DriverAtoms.webClick;
import static android.support.test.espresso.web.webdriver.DriverAtoms.webKeys;
import static com.braintreepayments.testutils.TestTokenizationKey.TOKENIZATION_KEY;
import static com.braintreepayments.testutils.ui.Matchers.withId;
import static com.braintreepayments.testutils.ui.ViewHelper.waitForView;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class ThreeDSecureVerificationTest {

    private static final String TEST_AMOUNT = "1";

    @Rule
    public final BraintreeActivityTestRule<TestActivity> mActivityTestRule =
            new BraintreeActivityTestRule<>(TestActivity.class);

    private Activity mActivity;
    private CountDownLatch mCountDownLatch;

    @Before
    public void setUp() {
        mActivity = mActivityTestRule.getActivity();
        mCountDownLatch = new CountDownLatch(1);
    }

    @Test(timeout = 30000)
    @LargeTest
    public void performVerification_callsCancelListenerWhenUpIsPressed()
            throws InterruptedException {
        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber("4000000000000002")
                .expirationDate("12/30");
        BraintreeFragment fragment = getFragment();
        fragment.addListener(new BraintreeCancelListener() {
            @Override
            public void onCancel(int requestCode) {
                assertEquals(ThreeDSecure.THREE_D_SECURE_REQUEST_CODE, requestCode);
                mCountDownLatch.countDown();
            }
        });
        ThreeDSecure.performVerification(getFragment(), cardBuilder, TEST_AMOUNT);

        waitForView(withId(android.R.id.widget_frame));
        onView(withContentDescription("Navigate up")).perform(click());

        mCountDownLatch.await();
    }

    @Test(timeout = 30000)
    @LargeTest
    public void performVerification_callsCancelListenerWhenBackIsPressedOnFirstPage()
            throws InterruptedException {
        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber("4000000000000002")
                .expirationDate("12/30");
        BraintreeFragment fragment = getFragment();
        fragment.addListener(new BraintreeCancelListener() {
            @Override
            public void onCancel(int requestCode) {
                assertEquals(ThreeDSecure.THREE_D_SECURE_REQUEST_CODE, requestCode);
                mCountDownLatch.countDown();
            }
        });
        ThreeDSecure.performVerification(fragment, cardBuilder, TEST_AMOUNT);

        waitForView(withId(android.R.id.widget_frame));
        pressBack();

        mCountDownLatch.await();
    }

    @Test(timeout = 30000)
    @LargeTest
    public void performVerification_callsCancelListenerWhenUserGoesOnePageDeepAndPressesBack()
            throws InterruptedException {
        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber("4000000000000002")
                .expirationDate("12/30");
        BraintreeFragment fragment = getFragment();
        fragment.addListener(new BraintreeCancelListener() {
            @Override
            public void onCancel(int requestCode) {
                assertEquals(ThreeDSecure.THREE_D_SECURE_REQUEST_CODE, requestCode);
                mCountDownLatch.countDown();
            }
        });
        ThreeDSecure.performVerification(fragment, cardBuilder, TEST_AMOUNT);

        waitForView(withId(android.R.id.widget_frame));
        onWebView().withElement(findElement(Locator.LINK_TEXT, "New User / Forgot your password?"))
                .perform(webClick());

        SystemClock.sleep(2000);
        pressBack();
        SystemClock.sleep(2000);
        pressBack();
        pressBack();

        mCountDownLatch.await();
    }

    @Test(timeout = 10000)
    @MediumTest
    public void performVerification_doesALookupAndReturnsACardAndANullACSUrlWhenAuthenticationIsNotRequired()
            throws InterruptedException {
        BraintreeFragment fragment = getFragment();
        fragment.addListener(new PaymentMethodNonceCreatedListener() {
            @Override
            public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
                CardNonce cardNonce = (CardNonce) paymentMethodNonce;

                assertEquals("51", cardNonce.getLastTwo());
                assertTrue(cardNonce.getThreeDSecureInfo().isLiabilityShifted());
                assertTrue(cardNonce.getThreeDSecureInfo().isLiabilityShiftPossible());

                mCountDownLatch.countDown();
            }
        });
        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber("4000000000000051")
                .expirationDate("12/20");

        ThreeDSecure.performVerification(fragment, cardBuilder, TEST_AMOUNT);

        mCountDownLatch.await();
    }

    @Test(timeout = 10000)
    @MediumTest
    public void performVerification_failsWithATokenizationKey() throws InterruptedException {
        BraintreeFragment fragment = BraintreeFragmentTestUtils.getFragment(mActivity,
                TOKENIZATION_KEY);
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
                .cardNumber("4000000000000051")
                .expirationDate("12/20");

        ThreeDSecure.performVerification(fragment, cardBuilder, TEST_AMOUNT);

        mCountDownLatch.await();
    }

    @Test(timeout = 10000)
    @MediumTest
    public void performVerification_doesALookupAndReturnsACardWhenThereIsALookupError()
            throws InterruptedException {
        BraintreeFragment fragment = getFragment();
        fragment.addListener(new PaymentMethodNonceCreatedListener() {
            @Override
            public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
                assertEquals("77", ((CardNonce) paymentMethodNonce).getLastTwo());
                mCountDownLatch.countDown();
            }
        });
        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber("4000000000000077")
                .expirationDate("12/20");

        ThreeDSecure.performVerification(fragment, cardBuilder, TEST_AMOUNT);

        mCountDownLatch.await();
    }

    @Test(timeout = 30000)
    @LargeTest
    public void performVerification_requestsAuthenticationWhenRequired()
            throws InterruptedException {
        BraintreeFragment fragment = getFragment();
        fragment.addListener(new PaymentMethodNonceCreatedListener() {
            @Override
            public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
                CardNonce cardNonce = (CardNonce) paymentMethodNonce;

                assertEquals("02", cardNonce.getLastTwo());
                assertTrue(cardNonce.getThreeDSecureInfo().isLiabilityShifted());
                assertTrue(cardNonce.getThreeDSecureInfo().isLiabilityShiftPossible());

                mCountDownLatch.countDown();
            }
        });
        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber("4000000000000002")
                .expirationDate("12/30");
        ThreeDSecure.performVerification(fragment, cardBuilder, TEST_AMOUNT);

        waitForView(withId(android.R.id.widget_frame));
        onWebView().withElement(findElement(Locator.NAME, "external.field.password"))
                .perform(webKeys("1234"));
        onWebView().withElement(findElement(Locator.NAME, "UsernamePasswordEntry"))
                .perform(webClick());

        mCountDownLatch.await();
    }

    @Test(timeout = 30000)
    @LargeTest
    public void performVerification_returnsAnErrorWhenAuthenticationFails()
            throws InterruptedException {
        BraintreeFragment fragment = getFragment();
        fragment.addListener(new BraintreeErrorListener() {
            @Override
            public void onError(Exception error) {
                assertEquals("Failed to authenticate, please try a different form of payment",
                        error.getMessage());
                mCountDownLatch.countDown();
            }
        });
        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber("4000000000000028")
                .expirationDate("12/30");
        ThreeDSecure.performVerification(fragment, cardBuilder, TEST_AMOUNT);

        waitForView(withId(android.R.id.widget_frame));
        onWebView().withElement(findElement(Locator.NAME, "external.field.password"))
                .perform(webKeys("1234"));
        onWebView().withElement(findElement(Locator.NAME, "UsernamePasswordEntry"))
                .perform(webClick());
        onWebView().withElement(findElement(Locator.NAME, "Submit"))
                .perform(webClick());

        mCountDownLatch.await();
    }

    @Test(timeout = 30000)
    @LargeTest
    public void performVerification_returnsASuccessfulAuthenticationWhenIssuerDoesNotParticipate()
            throws InterruptedException {
        BraintreeFragment fragment = getFragment();
        fragment.addListener(new PaymentMethodNonceCreatedListener() {
            @Override
            public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
                CardNonce cardNonce = (CardNonce) paymentMethodNonce;
                assertEquals("01", cardNonce.getLastTwo());
                assertTrue(cardNonce.getThreeDSecureInfo().isLiabilityShifted());
                assertTrue(cardNonce.getThreeDSecureInfo().isLiabilityShiftPossible());

                mCountDownLatch.countDown();
            }
        });
        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber("4000000000000101")
                .expirationDate("12/30");

        ThreeDSecure.performVerification(fragment, cardBuilder, TEST_AMOUNT);

        mCountDownLatch.await();
    }

    @Test(timeout = 30000)
    @LargeTest
    public void performVerification_returnsAFailedAuthenticationWhenSignatureVerificationFails()
            throws InterruptedException {
        BraintreeFragment fragment = getFragment();
        fragment.addListener(new BraintreeErrorListener() {
            @Override
            public void onError(Exception error) {
                assertEquals("Failed to authenticate, please try a different form of payment",
                        error.getMessage());
                mCountDownLatch.countDown();
            }
        });
        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber("4000000000000010")
                .expirationDate("12/30");
        ThreeDSecure.performVerification(fragment, cardBuilder, TEST_AMOUNT);

        waitForView(withId(android.R.id.widget_frame));
        onWebView().withElement(findElement(Locator.NAME, "external.field.password"))
                .perform(webKeys("1234"));
        onWebView().withElement(findElement(Locator.NAME, "UsernamePasswordEntry"))
                .perform(webClick());

        mCountDownLatch.await();
    }

    @Test(timeout = 30000)
    @LargeTest
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
                .cardNumber("4000000000000036")
                .expirationDate("12/30");
        ThreeDSecure.performVerification(fragment, cardBuilder, TEST_AMOUNT);

        waitForView(withId(android.R.id.widget_frame));
        onWebView().withElement(findElement(Locator.NAME, "Submit"))
                .perform(webClick());

        mCountDownLatch.await();
    }

    @Test(timeout = 30000)
    @LargeTest
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
                .cardNumber("4000000000000093")
                .expirationDate("12/30");
        ThreeDSecure.performVerification(fragment, cardBuilder, TEST_AMOUNT);

        waitForView(withId(android.R.id.widget_frame));
        onWebView().withElement(findElement(Locator.NAME, "external.field.password"))
                .perform(webKeys("1234"));
        onWebView().withElement(findElement(Locator.NAME, "UsernamePasswordEntry"))
                .perform(webClick());

        mCountDownLatch.await();
    }

    /* helpers */
    private BraintreeFragment getFragment() {
        String clientToken = new TestClientTokenBuilder().withThreeDSecure().build();
        return BraintreeFragmentTestUtils.getFragment(mActivity, clientToken);
    }
}
