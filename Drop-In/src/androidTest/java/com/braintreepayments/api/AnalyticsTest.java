package com.braintreepayments.api;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.test.suitebuilder.annotation.LargeTest;
import android.view.KeyEvent;

import com.braintreepayments.api.exceptions.AuthenticationException;
import com.braintreepayments.api.exceptions.DownForMaintenanceException;
import com.braintreepayments.api.exceptions.ServerException;
import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.models.PayPalAccountNonce;
import com.braintreepayments.api.test.BraintreePaymentActivityTestRunner;
import com.braintreepayments.cardform.R;
import com.braintreepayments.testutils.TestClientTokenBuilder;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.InstrumentationRegistry.getTargetContext;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.matcher.ViewMatchers.isEnabled;
import static com.braintreepayments.api.utils.PaymentFormHelpers.fillInCardForm;
import static com.braintreepayments.api.utils.PaymentFormHelpers.waitForAddPaymentFormHeader;
import static com.braintreepayments.api.test.ActivityResultHelper.getActivityResult;
import static com.braintreepayments.testutils.CardNumber.VISA;
import static com.braintreepayments.testutils.ui.Matchers.withHint;
import static com.braintreepayments.testutils.ui.Matchers.withId;
import static com.braintreepayments.testutils.ui.ViewHelper.closeSoftKeyboard;
import static com.braintreepayments.testutils.ui.ViewHelper.waitForView;
import static com.braintreepayments.api.test.WaitForActivityHelper.waitForActivityToFinish;
import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.contains;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@LargeTest
public class AnalyticsTest extends BraintreePaymentActivityTestRunner {

    private BraintreeFragment mFragment;
    private BraintreePaymentActivity mActivity;

    @Test(timeout = 30000)
    public void addsEventOnSDKInitialized() {
        setupActivity(new TestClientTokenBuilder().withAnalytics().build());
        waitForAddPaymentFormHeader();

        verifyAnalyticsEvent("dropin.appeared");
    }

    @Test(timeout = 30000)
    public void addsEventOnAddCardStarted() {
        setupActivity(new TestClientTokenBuilder().withAnalytics().build());

        waitForView(withId(R.id.bt_card_form_card_number)).perform(click());

        verifyAnalyticsEvent("dropin.card.form.focused");
    }

    @Test(timeout = 30000)
    public void addsEventOnLocalCardValidationSuccess() {
        setupActivity(new TestClientTokenBuilder().withAnalytics().build());
        waitForAddPaymentFormHeader();

        fillInCardForm();
        onView(withId(com.braintreepayments.api.dropin.R.id.bt_card_form_submit_button)).perform(click());

        verifyAnalyticsEvent("dropin.card.form.submitted.succeeded");
    }

    @Test(timeout = 30000)
    public void addsEventOnLocalCardValidationFailure() {
        setupActivity(new TestClientTokenBuilder().withAnalytics().build());

        waitForView(withId(com.braintreepayments.api.dropin.R.id.bt_card_form_submit_button)).perform(click());

        verifyAnalyticsEvent("dropin.card.form.submitted.failed");
    }

    @Test(timeout = 30000)
    public void addsEventOnAddCardSucceeded() {
        setupActivity(new TestClientTokenBuilder().withAnalytics().build());

        fillInCardForm();
        onView(withId(com.braintreepayments.api.dropin.R.id.bt_card_form_submit_button)).perform(click());
        waitForActivityToFinish(mActivity);

        verifyAnalyticsEvent("card.nonce-received");
    }

    @Test(timeout = 30000)
    public void addsEventOnAddCardFailed() {
        setupActivity(new TestClientTokenBuilder()
                .withCvvVerification()
                .withAnalytics()
                .build());
        waitForAddPaymentFormHeader();

        onView(withHint(R.string.bt_form_hint_card_number)).perform(typeText(VISA));
        onView(withHint(R.string.bt_form_hint_expiration)).perform(typeText("0619"), closeSoftKeyboard());
        onView(withHint(R.string.bt_form_hint_cvv)).perform(typeText("200"), closeSoftKeyboard());
        onView(withId(com.braintreepayments.api.dropin.R.id.bt_card_form_submit_button)).perform(click());
        waitForView(withId(com.braintreepayments.api.dropin.R.id.bt_card_form_submit_button),
                isEnabled());

        verifyAnalyticsEvent("add-card.failed");
    }

    @Test(timeout = 30000)
    public void addsEventOnAddPayPalStarted() {
        String clientToken = new TestClientTokenBuilder()
                .withPayPal()
                .withAnalytics()
                .build();
        Intent intent = new PaymentRequest()
                .clientToken(clientToken)
                .getIntent(getTargetContext())
                .putExtra(BraintreePaymentTestActivity.MOCK_CONFIGURATION, clientToken);
        mActivity = getActivity(intent);
        mFragment = mActivity.mBraintreeFragment;
        waitForAddPaymentFormHeader();

        onView(withId(com.braintreepayments.api.dropin.R.id.bt_paypal_button)).perform(click());

        verifyAnalyticsEvent("paypal.future-payments.selected");
    }

    @Test(timeout = 30000)
    public void addsEventOnAddPayPalSucceeded() throws InterruptedException {
        Looper.prepare();

        String clientToken = new TestClientTokenBuilder()
                .withPayPal()
                .withAnalytics()
                .build();
        Intent intent = new PaymentRequest()
                .clientToken(clientToken)
                .getIntent(getTargetContext())
                .putExtra(BraintreePaymentTestActivity.MOCK_CONFIGURATION, clientToken);
        mActivity = getActivity(intent);
        mActivity.mBraintreeFragment = spy(mActivity.mBraintreeFragment);
        mFragment = mActivity.mBraintreeFragment;
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        PayPalAccountNonce payPalAccountNonce = new PayPalAccountNonce();
                        mFragment.postCallback(payPalAccountNonce);
                        countDownLatch.countDown();
                    }
                }, 500);
                return null;
            }
        }).when(mFragment).startActivity(any(Intent.class));

        PayPal.authorizeAccount(mFragment);

        countDownLatch.await();
        verifyAnalyticsEvent("add-paypal.success");
    }

    @Test(timeout = 30000)
    public void addsEventOnSDKExitWithSuccess() {
        setupActivity(new TestClientTokenBuilder().withAnalytics().build());

        fillInCardForm();
        onView(withId(com.braintreepayments.api.dropin.R.id.bt_card_form_submit_button)).perform(
                click());
        waitForActivityToFinish(mActivity);

        verifyAnalyticsEvent("sdk.exit.success");
    }

    @Test(timeout = 30000)
    public void addsEventOnSDKExitWithUserCanceled() {
        setupActivity(new TestClientTokenBuilder().withAnalytics().build());
        waitForAddPaymentFormHeader();

        getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
        waitForActivityToFinish(mActivity);

        verifyAnalyticsEvent("sdk.exit.user-canceled");
    }

    @Test(timeout = 30000)
    public void doesNotCrashWhenUserExitsRightAfterDropInIsLaunched() {
        setupActivity(new TestClientTokenBuilder().withAnalytics().build());

        waitForView(withId(com.braintreepayments.api.dropin.R.id.bt_loading_progress_bar));
        getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);

        waitForActivityToFinish(mActivity);
        Map<String, Object> result = getActivityResult(mActivity);
        assertEquals(Activity.RESULT_CANCELED, result.get("resultCode"));
    }

    @Test(timeout = 30000)
    public void addsEventOnSDKExitWithDeveloperError() {
        setupActivity(new TestClientTokenBuilder().withAnalytics().build());
        waitForAddPaymentFormHeader();

        mFragment.postCallback(new AuthenticationException(""));
        waitForActivityToFinish(mActivity);

        verifyAnalyticsEvent("sdk.exit.developer-error");
    }

    @Test(timeout = 30000)
    public void addsEventOnSDKExitWithServerError() {
        setupActivity(new TestClientTokenBuilder().withAnalytics().build());
        waitForAddPaymentFormHeader();

        mFragment.postCallback(new ServerException(""));
        waitForActivityToFinish(mActivity);

        verifyAnalyticsEvent("sdk.exit.server-error");
    }

    @Test(timeout = 30000)
    public void addsEventOnSDKExitWithServerUnavailableError() {
        setupActivity(new TestClientTokenBuilder().withAnalytics().build());
        waitForAddPaymentFormHeader();

        mFragment.postCallback(new DownForMaintenanceException(""));
        waitForActivityToFinish(mActivity);

        verifyAnalyticsEvent("sdk.exit.server-unavailable");
    }

    /* helpers */
    private void setupActivity(String clientToken) {
        mActivity = getActivity(clientToken);
        mFragment = mActivity.mBraintreeFragment;
    }

    private void verifyAnalyticsEvent(String event) {
        AnalyticsManager.flushEvents(mFragment);
        verify(mFragment.getHttpClient(), times(1)).post(
                eq(mFragment.getConfiguration().getAnalytics().getUrl()),
                contains(event),
                isNull(HttpResponseCallback.class));
    }
}
