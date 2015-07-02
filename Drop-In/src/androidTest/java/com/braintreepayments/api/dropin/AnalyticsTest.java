package com.braintreepayments.api.dropin;

import android.app.Activity;
import android.os.SystemClock;
import android.view.KeyEvent;

import com.braintreepayments.api.Braintree;
import com.braintreepayments.api.BraintreeTestUtils;
import com.braintreepayments.api.TestUtils;
import com.braintreepayments.api.exceptions.AuthenticationException;
import com.braintreepayments.api.exceptions.DownForMaintenanceException;
import com.braintreepayments.api.exceptions.ServerException;
import com.braintreepayments.testutils.TestClientTokenBuilder;

import org.json.JSONException;

import java.util.Map;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isEnabled;
import static com.braintreepayments.api.BraintreeTestUtils.setClientTokenExtraForTest;
import static com.braintreepayments.api.TestDependencyInjector.injectBraintree;
import static com.braintreepayments.api.TestDependencyInjector.injectSlowNonSetupBraintree;
import static com.braintreepayments.api.utils.PaymentFormHelpers.waitForAddPaymentFormHeader;
import static com.braintreepayments.testutils.ActivityResultHelper.getActivityResult;
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

        verify(mBraintree, times(1)).sendAnalyticsEvent("add-card.success");
    }

    public void testAddsEventOnAddCardFailed() {
        String clientToken = new TestClientTokenBuilder().withCvvVerification().withAnalytics().build();
        mBraintree = spy(injectBraintree(mContext, clientToken, clientToken));
        injectBraintree(clientToken, mBraintree);
        setClientTokenExtraForTest(this, clientToken);
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
        sendKeys(KeyEvent.KEYCODE_BACK);
        verify(mBraintree, times(1)).sendAnalyticsEvent("add-paypal.start");
    }

    public void testAddsEventOnAddPayPalSucceeded() throws JSONException {
        setupActivity();
        mBraintree.create(TestUtils.fakePayPalAccountBuilder());
        SystemClock.sleep(3000); // This timer is to allow the request to complete
        verify(mBraintree, times(1)).sendAnalyticsEvent("add-paypal.success");
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

    public void testDoesntCrashWhenUserExitsRightAfterDropInIsLaunched() {
        String clientToken = new TestClientTokenBuilder().withAnalytics().build();
        injectSlowNonSetupBraintree(mContext, clientToken, 5000);
        setClientTokenExtraForTest(this, clientToken);
        mActivity = getActivity();

        waitForView(withId(R.id.bt_loading_progress_bar));
        sendKeys(KeyEvent.KEYCODE_BACK);

        waitForActivityToFinish(mActivity);
        Map<String, Object> result = getActivityResult(mActivity);
        assertEquals(Activity.RESULT_CANCELED, result.get("resultCode"));
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

    /* helpers */
    private void setupActivity() {
        String clientToken = new TestClientTokenBuilder().withFakePayPal().withAnalytics().build();
        mBraintree = spy(injectBraintree(mContext, clientToken, clientToken));
        injectBraintree(clientToken, mBraintree);
        setClientTokenExtraForTest(this, clientToken);
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
