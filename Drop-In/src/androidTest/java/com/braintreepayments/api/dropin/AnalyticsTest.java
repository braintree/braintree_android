package com.braintreepayments.api.dropin;

import android.view.KeyEvent;

import com.braintreepayments.api.Braintree;
import com.braintreepayments.api.BraintreeTestUtils;
import com.braintreepayments.api.TestClientTokenBuilder;
import com.braintreepayments.api.exceptions.AuthenticationException;
import com.braintreepayments.api.exceptions.DownForMaintenanceException;
import com.braintreepayments.api.exceptions.ServerException;

import static com.braintreepayments.api.BraintreeTestUtils.injectBraintree;
import static com.braintreepayments.api.BraintreeTestUtils.setUpActivityTest;
import static com.braintreepayments.api.ui.Matchers.withHint;
import static com.braintreepayments.api.ui.Matchers.withId;
import static com.braintreepayments.api.ui.ViewHelper.waitForKeyboardToClose;
import static com.braintreepayments.api.ui.ViewHelper.waitForView;
import static com.braintreepayments.api.ui.WaitForActivityHelper.waitForActivity;
import static com.braintreepayments.api.utils.PaymentFormHelpers.fillInPayPal;
import static com.braintreepayments.api.utils.PaymentFormHelpers.waitForAddPaymentFormHeader;
import static com.google.android.apps.common.testing.ui.espresso.Espresso.onView;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.click;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.closeSoftKeyboard;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.typeText;
import static com.google.android.apps.common.testing.ui.espresso.assertion.ViewAssertions.matches;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.isDisplayed;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.isEnabled;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class AnalyticsTest extends BraintreePaymentActivityTestCase {

    private Braintree mBraintree;
    private BraintreePaymentActivity mActivity;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        System.setProperty("dexmaker.dexcache",
                getInstrumentation().getContext().getCacheDir().getPath());
    }

    public void testAddsEventOnSDKInitialized() {
        setupActivity();
        verify(mBraintree, times(1)).sendAnalyticsEvent("sdk.initialized");
    }

    public void testAddsEventOnAddCardStarted() {
        setupActivity();
        onView(withId(R.id.bt_card_form_header)).check(matches(isDisplayed()));
        verify(mBraintree, times(1)).sendAnalyticsEvent("add-card.start");
    }

    public void testAddsEventOnAddCardSucceeded() {
        setupActivity();
        fillInCreditCard();
        waitForActivity(mActivity);

        verify(mBraintree, times(1)).sendAnalyticsEvent("add-card.success");
    }

    public void testAddsEventOnAddCardFailed() {
        String clientToken = new TestClientTokenBuilder().withCvvVerification().withAnalytics().build();
        mBraintree = spy(Braintree.getInstance(getInstrumentation().getContext(),
                clientToken));
        injectBraintree(clientToken, mBraintree);
        setUpActivityTest(this, clientToken);
        mActivity = getActivity();

        waitForAddPaymentFormHeader();

        onView(withHint("Card Number")).perform(typeText("4111111111111111"));
        onView(withHint("Expiration")).perform(typeText("0619"), closeSoftKeyboard(), waitForKeyboardToClose());
        onView(withHint("CVV")).perform(typeText("200"), closeSoftKeyboard(),
                waitForKeyboardToClose());
        onView(withHint("Postal Code")).perform(typeText("12345"));
        onView(withId(R.id.bt_card_form_submit_button)).perform(click());

        waitForView(withId(R.id.bt_card_form_submit_button), isEnabled());

        verify(mBraintree, times(1)).sendAnalyticsEvent("add-card.failed");
    }

    public void pendingAddsEventOnAddPayPalStarted() {
        setupActivity();
        onView(withId(R.id.bt_paypal_button)).perform(click());
        waitForView(withHint("Email"));
        sendKeys(KeyEvent.KEYCODE_BACK);

        verify(mBraintree, times(1)).sendAnalyticsEvent("add-paypal.start");
    }

    public void testAddsEventOnAddPayPalSucceeded() {
        setupActivity();
        fillInPayPal();

        verify(mBraintree, times(1)).sendAnalyticsEvent("add-paypal.success");
    }

    public void testAddsEventOnSDKExitWithSuccess() {
        setupActivity();
        fillInCreditCard();
        waitForActivity(mActivity);

        verify(mBraintree, times(1)).sendAnalyticsEvent("sdk.exit.success");
    }

    public void testAddsEventOnSDKExitWithUserCanceled() {
        setupActivity();
        sendKeys(KeyEvent.KEYCODE_BACK);
        waitForActivity(mActivity);

        verify(mBraintree, times(1)).sendAnalyticsEvent("sdk.exit.user-canceled");
    }

    public void testAddsEventOnSDKExitWithDeveloperError() {
        setupActivity();
        BraintreeTestUtils
                .postUnrecoverableErrorFromBraintree(mBraintree, new AuthenticationException());
        waitForActivity(mActivity);

        verify(mBraintree, times(1)).sendAnalyticsEvent("sdk.exit.developer-error");
    }

    public void testAddsEventOnSDKExitWithServerError() {
        setupActivity();
        BraintreeTestUtils.postUnrecoverableErrorFromBraintree(mBraintree, new ServerException());
        waitForActivity(mActivity);

        verify(mBraintree, times(1)).sendAnalyticsEvent("sdk.exit.server-error");
    }

    public void testAddsEventOnSDKExitWithServerUnavailableError() {
        setupActivity();
        BraintreeTestUtils
                .postUnrecoverableErrorFromBraintree(mBraintree, new DownForMaintenanceException());
        waitForActivity(mActivity);

        verify(mBraintree, times(1)).sendAnalyticsEvent("sdk.exit.server-unavailable");
    }

    private void setupActivity() {
        String clientToken = new TestClientTokenBuilder().withPayPal().withAnalytics().build();
        mBraintree = spy(Braintree.getInstance(getInstrumentation().getContext(),
                clientToken));
        injectBraintree(clientToken, mBraintree);
        setUpActivityTest(this, clientToken);
        mActivity = getActivity();
        waitForAddPaymentFormHeader();
    }

    private void fillInCreditCard() {
        onView(withHint("Card Number")).perform(typeText("4111111111111111"));
        onView(withHint("Expiration")).perform(typeText("0619"), closeSoftKeyboard(), waitForKeyboardToClose());
        onView(withHint("CVV")).perform(typeText("123"), closeSoftKeyboard(),
                waitForKeyboardToClose());
        onView(withHint("Postal Code")).perform(typeText("12345"));
        onView(withId(R.id.bt_card_form_submit_button)).perform(click());
    }
}
