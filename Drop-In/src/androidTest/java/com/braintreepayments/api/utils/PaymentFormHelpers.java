package com.braintreepayments.api.utils;

import android.app.Activity;
import android.content.Intent;
import android.os.SystemClock;
import android.support.test.espresso.PerformException;
import android.support.test.espresso.ViewInteraction;
import android.util.Log;
import android.view.View;

import com.braintreepayments.api.BraintreePaymentActivity;
import com.braintreepayments.api.dropin.R;
import com.braintreepayments.api.dropin.view.LoadingHeader;
import com.braintreepayments.api.dropin.view.LoadingHeader.HeaderState;
import com.braintreepayments.api.models.CardNonce;
import com.braintreepayments.api.models.PaymentMethodNonce;

import org.hamcrest.Matcher;

import java.util.Map;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.braintreepayments.testutils.ActivityResultHelper.getActivityResult;
import static com.braintreepayments.testutils.CardNumber.VISA;
import static com.braintreepayments.testutils.ui.Matchers.withHint;
import static com.braintreepayments.testutils.ui.Matchers.withId;
import static com.braintreepayments.testutils.ui.ViewHelper.FIFTEEN_SECONDS;
import static com.braintreepayments.testutils.ui.ViewHelper.TEN_SECONDS;
import static com.braintreepayments.testutils.ui.ViewHelper.closeSoftKeyboard;
import static com.braintreepayments.testutils.ui.ViewHelper.waitForView;
import static com.braintreepayments.testutils.ui.WaitForActivityHelper.waitForActivityToFinish;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

public class PaymentFormHelpers {

    private static final String TAG = PaymentFormHelpers.class.getSimpleName();

    public static void fillInCardForm() {
        waitForAddPaymentFormHeader();

        onView(withHint(R.string.bt_form_hint_card_number))
                .perform(typeText(VISA), closeSoftKeyboard());
        onView(withHint(R.string.bt_form_hint_expiration))
                .perform(typeText("0619"), closeSoftKeyboard());

        try {
            onView(withHint(R.string.bt_form_hint_cvv))
                    .perform(typeText("123"), closeSoftKeyboard());
            onView(withHint(R.string.bt_form_hint_postal_code))
                    .perform(typeText("12345"), closeSoftKeyboard());
        } catch (PerformException e) {
            Log.i(TAG, e.getMessage());
        }
    }

    public static void addCardAndAssertSuccess(Activity activity) {
        fillInCardForm();
        onView(withId(R.id.bt_card_form_submit_button)).perform(click());

        waitForView(withId(R.id.bt_header_container));

        LoadingHeader loadingHeader = (LoadingHeader) activity.findViewById(R.id.bt_header_container);
        SystemClock.sleep(1000);
        assertEquals(HeaderState.SUCCESS, loadingHeader.getCurrentState());
        onView(withId(R.id.bt_header_container)).check(matches(isDisplayed()));

        waitForActivityToFinish(activity);

        Map<String, Object> result = getActivityResult(activity);
        PaymentMethodNonce response = ((Intent) result.get("resultData"))
                .getParcelableExtra(BraintreePaymentActivity.EXTRA_PAYMENT_METHOD_NONCE);

        assertEquals(Activity.RESULT_OK, result.get("resultCode"));
        assertNotNull(response.getNonce());
        assertEquals("11", ((CardNonce) response).getLastTwo());
    }

    public static void fillInOfflinePayPal() {
        waitForView(withId(R.id.bt_paypal_button)).perform(click());

        waitForView(withHint("Email"), FIFTEEN_SECONDS).perform(typeText("test@paypal.com"));
        onView(withHint("Password")).perform(typeText("11111111"));
        onView(withHint("Log In")).perform(click());

        waitForView(withText("Agree")).perform(click());
    }

    public static void performPayPalAdd() {
        fillInOfflinePayPal();
        waitForPaymentMethodNonceList();
    }

    public static ViewInteraction waitForPaymentMethodNonceList() {
        return waitForView(withId(R.id.bt_change_payment_method_link), TEN_SECONDS);
    }

    public static ViewInteraction waitForPaymentMethodNonceList(int timeout) {
        return waitForView(withId(R.id.bt_change_payment_method_link), timeout);
    }

    public static ViewInteraction waitForAddPaymentFormHeader() {
        return waitForView(addPaymentFormHeader(), TEN_SECONDS);
    }

    public static ViewInteraction waitForAddPaymentFormHeader(long timeout) {
        return waitForView(addPaymentFormHeader(), timeout);
    }

    public static ViewInteraction onAddPaymentFormHeader() {
        return onView(addPaymentFormHeader());
    }

    public static Matcher<View> addPaymentFormHeader() {
        return withText(R.string.bt_form_pay_with_card_header);
    }

    public static ViewInteraction onCardField() {
        return onView(withHint("Card Number"));
    }

    public static ViewInteraction onExpirationField() {
        return onView(withHint("Expiration"));
    }

    public static ViewInteraction onCvvField() {
        return onView(withHint("CVV"));
    }

    public static ViewInteraction onPostalCodeField() {
        return onView(withHint("Postal Code"));
    }

}
