package com.braintreepayments.api.dropin;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.SystemClock;
import android.view.KeyEvent;

import com.braintreepayments.api.Braintree;
import com.braintreepayments.api.BraintreeTestUtils;
import com.braintreepayments.api.exceptions.AuthenticationException;
import com.braintreepayments.api.exceptions.DownForMaintenanceException;
import com.braintreepayments.api.exceptions.ServerException;
import com.braintreepayments.api.models.CoinbaseAccountBuilder;
import com.braintreepayments.testutils.TestClientTokenBuilder;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isEnabled;
import static com.braintreepayments.api.BraintreeTestUtils.injectBraintree;
import static com.braintreepayments.api.BraintreeTestUtils.setUpActivityTest;
import static com.braintreepayments.api.utils.PaymentFormHelpers.performPayPalAdd;
import static com.braintreepayments.api.utils.PaymentFormHelpers.waitForAddPaymentFormHeader;
import static com.braintreepayments.testutils.CardNumber.VISA;
import static com.braintreepayments.testutils.ui.Matchers.withHint;
import static com.braintreepayments.testutils.ui.Matchers.withId;
import static com.braintreepayments.testutils.ui.ViewHelper.closeSoftKeyboard;
import static com.braintreepayments.testutils.ui.ViewHelper.waitForView;
import static com.braintreepayments.testutils.ui.WaitForActivityHelper.waitForActivityToFinish;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class AnalyticsTest extends BraintreePaymentActivityTestCase {

    private Braintree mBraintree;
    private BraintreePaymentActivity mActivity;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        System.setProperty("dexmaker.dexcache", mContext.getCacheDir().getPath());
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
        waitForActivityToFinish(mActivity);

        verify(mBraintree, times(1)).sendAnalyticsEvent("sdk.initialized");

        verify(mBraintree, times(1)).sendAnalyticsEvent("add-card.success");
    }

    public void testAddsEventOnAddCardFailed() {
        String clientToken = new TestClientTokenBuilder().withCvvVerification().withAnalytics().build();
        mBraintree = spy(Braintree.getInstance(mContext, clientToken));
        injectBraintree(clientToken, mBraintree);
        setUpActivityTest(this, clientToken);
        mActivity = getActivity();

        waitForAddPaymentFormHeader();

        onView(withHint("Card Number")).perform(typeText(VISA));
        onView(withHint("Expiration")).perform(typeText("0619"), closeSoftKeyboard());
        onView(withHint("CVV")).perform(typeText("200"), closeSoftKeyboard());
        onView(withHint("Postal Code")).perform(typeText("12345"));
        onView(withId(R.id.bt_card_form_submit_button)).perform(click());

        waitForView(withId(R.id.bt_card_form_submit_button), isEnabled());

        verify(mBraintree, times(1)).sendAnalyticsEvent("add-card.failed");
    }

    public void testAddsEventOnAddPayPalStarted() {
        setupActivity();
        onView(withId(R.id.bt_paypal_button)).perform(click());
        waitForView(withHint("Email"));
        sendKeys(KeyEvent.KEYCODE_BACK);

        verify(mBraintree, times(1)).sendAnalyticsEvent("add-paypal.start");
    }

    public void testAddsEventOnAddPayPalSucceeded() {
        setupActivity();
        performPayPalAdd();

        verify(mBraintree, times(1)).sendAnalyticsEvent("add-paypal.success");
    }

    public void testAddsEventOnCoinbaseSucceeded() {
        try {
            setupCoinbaseActivity();

            mBraintree.create(new CoinbaseAccountBuilder().code("coinbase-code").storeInVault(true));

            SystemClock.sleep(1000);

            verify(mBraintree, times(1)).sendAnalyticsEvent("sdk.initialized");

            verify(mBraintree, times(1)).sendAnalyticsEvent("add-card.start");

            verify(mBraintree, times(1)).sendAnalyticsEvent("add-coinbase.success");
        } finally {
            TestClientTokenBuilder.enableCoinbase(false);
        }
    }

    public void testAddsEventsOnCoinbaseWebswitchSucceeded() {
        try {
            setupCoinbaseActivity();

            Intent broadcastIntent = new Intent();
            broadcastIntent.putExtra("com.braintreepayments.api.BraintreeBrowserSwitchActivity.EXTRA_REDIRECT_URL"
                    , Uri.parse("com.braintreepayments.api.dropin.test.braintree://coinbase?code=coinbase-code"));
            mBraintree.finishPayWithCoinbase(Activity.RESULT_OK, broadcastIntent);

            SystemClock.sleep(1000);

            verify(mBraintree, times(1)).sendAnalyticsEvent("sdk.initialized");

            verify(mBraintree, times(1)).sendAnalyticsEvent("add-card.start");

            verify(mBraintree, times(1)).sendAnalyticsEvent("coinbase.webswitch.authorized");

            verify(mBraintree, times(1)).sendAnalyticsEvent("add-coinbase.success");

            verify(mBraintree, times(1)).sendAnalyticsEvent("coinbase.tokenize.succeeded");
        } finally {
            TestClientTokenBuilder.enableCoinbase(false);
        }
    }

    public void testAddsEventsOnCoinbaseWebswitchDenied() {
        try {
            setupCoinbaseActivity();

            Intent broadcastIntent = new Intent();
            broadcastIntent.putExtra("com.braintreepayments.api.BraintreeBrowserSwitchActivity.EXTRA_REDIRECT_URL"
                    , Uri.parse("com.braintreepayments.api.dropin.test.braintree://coinbase?error=access_denied&error_description=User+denied+access"));
            mBraintree.finishPayWithCoinbase(Activity.RESULT_OK, broadcastIntent);

            SystemClock.sleep(1000);

            verify(mBraintree, times(1)).sendAnalyticsEvent("sdk.initialized");

            verify(mBraintree, times(1)).sendAnalyticsEvent("add-card.start");

            verify(mBraintree, times(1)).sendAnalyticsEvent("coinbase.webswitch.denied");

        } finally {
            TestClientTokenBuilder.enableCoinbase(false);
        }
    }

    public void testAddsEventsOnCoinbaseWebswitchFailed() {
        try {
            setupCoinbaseActivity();

            Intent broadcastIntent = new Intent();
            broadcastIntent.putExtra("com.braintreepayments.api.BraintreeBrowserSwitchActivity.EXTRA_REDIRECT_URL"
                    , Uri.parse("com.braintreepayments.api.dropin.test.braintree://coinbase?error=a_random_error&error_description=Something+happened"));
            mBraintree.finishPayWithCoinbase(Activity.RESULT_OK, broadcastIntent);

            SystemClock.sleep(1000);

            verify(mBraintree, times(1)).sendAnalyticsEvent("sdk.initialized");

            verify(mBraintree, times(1)).sendAnalyticsEvent("add-card.start");

            verify(mBraintree, times(1)).sendAnalyticsEvent("coinbase.webswitch.failed");

        } finally {
            TestClientTokenBuilder.enableCoinbase(false);
        }
    }

    public void testAddsEventsOnCoinbaseWebswitchSucceeded() {
        try {
            setupCoinbaseActivity();

            Intent broadcastIntent = new Intent();
            broadcastIntent.putExtra("com.braintreepayments.api.BraintreeBrowserSwitchActivity.EXTRA_REDIRECT_URL"
                    , Uri.parse("com.braintreepayments.api.dropin.test.braintree://coinbase?code=coinbase-code"));
            mBraintree.finishPayWithCoinbase(Activity.RESULT_OK, broadcastIntent);

            SystemClock.sleep(1000);

            verify(mBraintree, times(1)).sendAnalyticsEvent("sdk.initialized");

            verify(mBraintree, times(1)).sendAnalyticsEvent("add-card.start");

            verify(mBraintree, times(1)).sendAnalyticsEvent("coinbase.webswitch.authorized");

            verify(mBraintree, times(1)).sendAnalyticsEvent("add-coinbase.success");

            verify(mBraintree, times(1)).sendAnalyticsEvent("coinbase.tokenize.succeeded");
        } finally {
            TestClientTokenBuilder.enableCoinbase(false);
        }
    }

    public void testAddsEventsOnCoinbaseWebswitchDenied() {
        try {
            setupCoinbaseActivity();

            Intent broadcastIntent = new Intent();
            broadcastIntent.putExtra("com.braintreepayments.api.BraintreeBrowserSwitchActivity.EXTRA_REDIRECT_URL"
                    , Uri.parse("com.braintreepayments.api.dropin.test.braintree://coinbase?error=access_denied&error_description=User+denied+access"));
            mBraintree.finishPayWithCoinbase(Activity.RESULT_OK, broadcastIntent);

            SystemClock.sleep(1000);

            verify(mBraintree, times(1)).sendAnalyticsEvent("sdk.initialized");

            verify(mBraintree, times(1)).sendAnalyticsEvent("add-card.start");

            verify(mBraintree, times(1)).sendAnalyticsEvent("coinbase.webswitch.denied");

        } finally {
            TestClientTokenBuilder.enableCoinbase(false);
        }
    }

    public void testAddsEventsOnCoinbaseWebswitchFailed() {
        try {
            setupCoinbaseActivity();

            Intent broadcastIntent = new Intent();
            broadcastIntent.putExtra("com.braintreepayments.api.BraintreeBrowserSwitchActivity.EXTRA_REDIRECT_URL"
                    , Uri.parse("com.braintreepayments.api.dropin.test.braintree://coinbase?error=a_random_error&error_description=Something+happened"));
            mBraintree.finishPayWithCoinbase(Activity.RESULT_OK, broadcastIntent);

            SystemClock.sleep(1000);

            verify(mBraintree, times(1)).sendAnalyticsEvent("sdk.initialized");

            verify(mBraintree, times(1)).sendAnalyticsEvent("add-card.start");

            verify(mBraintree, times(1)).sendAnalyticsEvent("coinbase.webswitch.failed");

        } finally {
            TestClientTokenBuilder.enableCoinbase(false);
        }
    }

    public void testAddsEventOnSDKExitWithSuccess() {
        setupActivity();
        fillInCreditCard();
        waitForActivityToFinish(mActivity);

        verify(mBraintree, times(1)).sendAnalyticsEvent("sdk.exit.success");
    }

    public void testAddsEventOnSDKExitWithUserCanceled() {
        setupActivity();
        sendKeys(KeyEvent.KEYCODE_BACK);
        waitForActivityToFinish(mActivity);

        verify(mBraintree, times(1)).sendAnalyticsEvent("sdk.exit.user-canceled");
    }

    public void testAddsEventOnSDKExitWithDeveloperError() {
        setupActivity();
        BraintreeTestUtils
                .postUnrecoverableErrorFromBraintree(mBraintree, new AuthenticationException());
        waitForActivityToFinish(mActivity);

        verify(mBraintree, times(1)).sendAnalyticsEvent("sdk.exit.developer-error");
    }

    public void testAddsEventOnSDKExitWithServerError() {
        setupActivity();
        BraintreeTestUtils.postUnrecoverableErrorFromBraintree(mBraintree, new ServerException());
        waitForActivityToFinish(mActivity);

        verify(mBraintree, times(1)).sendAnalyticsEvent("sdk.exit.server-error");
    }

    public void testAddsEventOnSDKExitWithServerUnavailableError() {
        setupActivity();
        BraintreeTestUtils
                .postUnrecoverableErrorFromBraintree(mBraintree, new DownForMaintenanceException());
        waitForActivityToFinish(mActivity);

        verify(mBraintree, times(1)).sendAnalyticsEvent("sdk.exit.server-unavailable");
    }

    private void setupActivity() {
        String clientToken = new TestClientTokenBuilder().withFakePayPal().withAnalytics().build();
        mBraintree = spy(Braintree.getInstance(mContext, clientToken));
        injectBraintree(clientToken, mBraintree);
        setUpActivityTest(this, clientToken);
        mActivity = getActivity();
        waitForAddPaymentFormHeader();
    }

    private void setupCoinbaseActivity() {
        String clientToken = new TestClientTokenBuilder().withFakePayPal().withAnalytics().withCoinbase().build();
        mBraintree = spy(Braintree.getInstance(mContext, clientToken));
        injectBraintree(clientToken, mBraintree);
        setUpActivityTest(this, clientToken);
        mActivity = getActivity();
        waitForAddPaymentFormHeader();
    }

    private void fillInCreditCard() {
        onView(withHint("Card Number")).perform(typeText(VISA));
        onView(withHint("Expiration")).perform(typeText("0619"), closeSoftKeyboard());
        onView(withHint("CVV")).perform(typeText("123"), closeSoftKeyboard());
        onView(withHint("Postal Code")).perform(typeText("12345"));
        onView(withId(R.id.bt_card_form_submit_button)).perform(click());
    }

}
