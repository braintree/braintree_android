package com.braintreepayments.api.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;

import com.braintreepayments.api.BraintreeTestUtils;
import com.braintreepayments.api.dropin.BraintreePaymentActivity;
import com.braintreepayments.api.dropin.R;
import com.braintreepayments.api.dropin.view.LoadingHeader;
import com.braintreepayments.api.dropin.view.LoadingHeader.HeaderState;
import com.braintreepayments.api.models.Card;
import com.braintreepayments.api.models.PaymentMethod;
import com.google.android.apps.common.testing.ui.espresso.ViewInteraction;

import org.hamcrest.Matcher;

import java.util.Map;

import static com.braintreepayments.api.CardNumber.VISA;
import static com.braintreepayments.api.ui.Matchers.withHint;
import static com.braintreepayments.api.ui.Matchers.withId;
import static com.braintreepayments.api.ui.ViewHelper.FIFTEEN_SECONDS;
import static com.braintreepayments.api.ui.ViewHelper.TEN_SECONDS;
import static com.braintreepayments.api.ui.ViewHelper.closeSoftKeyboard;
import static com.braintreepayments.api.ui.ViewHelper.waitForView;
import static com.braintreepayments.api.ui.WaitForActivityHelper.waitForActivity;
import static com.google.android.apps.common.testing.ui.espresso.Espresso.onView;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.click;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.typeText;
import static com.google.android.apps.common.testing.ui.espresso.assertion.ViewAssertions.matches;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.isDisplayed;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withText;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

public class PaymentFormHelpers {

    public static void fillInCardForm(Context context) {
        waitForAddPaymentFormHeader();

        onView(withHint(context.getString(R.string.bt_form_hint_card_number)))
                .perform(typeText(VISA), closeSoftKeyboard());
        onView(withHint(context.getString(R.string.bt_form_hint_expiration)))
                .perform(typeText("0619"), closeSoftKeyboard());
        onView(withHint(context.getString(R.string.bt_form_hint_cvv)))
                .perform(typeText("123"), closeSoftKeyboard());
        onView(withHint(context.getString(R.string.bt_form_hint_postal_code)))
                .perform(typeText("12345"), closeSoftKeyboard());
    }

    public static void addCardAndAssertSuccess(Activity activity) {
        fillInCardForm(activity);
        onView(withId(R.id.bt_card_form_submit_button)).perform(click());

        waitForView(withId(R.id.bt_header_status_icon));

        LoadingHeader loadingHeader = (LoadingHeader) activity.findViewById(R.id.bt_header_container);
        assertEquals(HeaderState.SUCCESS, loadingHeader.getCurrentState());
        onView(withId(R.id.bt_header_container)).check(matches(isDisplayed()));

        waitForActivity(activity);

        Map<String, Object> result = BraintreeTestUtils.getActivityResult(activity);
        PaymentMethod response =
                (PaymentMethod) ((Intent) result.get("resultData")).getSerializableExtra(
                        BraintreePaymentActivity.EXTRA_PAYMENT_METHOD);

        assertEquals(Activity.RESULT_OK, result.get("resultCode"));
        assertNotNull(response.getNonce());
        assertEquals("11", ((Card) response).getLastTwo());
    }

    public static void fillInOfflinePayPal() {
        waitForView(withId(R.id.bt_paypal_button)).perform(click());

        waitForView(withHint("Email"), FIFTEEN_SECONDS).perform(typeText("test@paypal.com"));
        onView(withHint("Password")).perform(typeText("11111111"));
        onView(withText("Log In")).perform(click());

        waitForView(withText("Agree")).perform(click());
    }

    public static void performPayPalAdd() {
        fillInOfflinePayPal();
        waitForPaymentMethodList();
    }

    public static ViewInteraction waitForPaymentMethodList() {
        return waitForView(withId(R.id.bt_change_payment_method_link), TEN_SECONDS);
    }

    public static ViewInteraction waitForPaymentMethodList(int timeout) {
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
