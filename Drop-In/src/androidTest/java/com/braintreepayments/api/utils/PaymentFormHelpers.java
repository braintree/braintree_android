package com.braintreepayments.api.utils;

import android.view.View;

import com.braintreepayments.api.dropin.R;
import com.google.android.apps.common.testing.ui.espresso.ViewInteraction;

import org.hamcrest.Matcher;

import static com.braintreepayments.api.ui.Matchers.withHint;
import static com.braintreepayments.api.ui.Matchers.withId;
import static com.braintreepayments.api.ui.ViewHelper.TEN_SECONDS;
import static com.braintreepayments.api.ui.ViewHelper.waitForView;
import static com.google.android.apps.common.testing.ui.espresso.Espresso.onView;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.click;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.typeText;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withText;

public class PaymentFormHelpers {

    public static void fillInPayPal() {
        waitForView(withId(R.id.bt_paypal_button)).perform(click());

        waitForView(withHint("Email"));
        onView(withHint("Email")).perform(typeText("bt_buyer_us@paypal.com"));
        onView(withHint("Password")).perform(typeText("11111111"));
        onView(withText("Log In")).perform(click());

        waitForView(withText("Agree"));
        onView(withText("Agree")).perform(click());

        waitForPaymentMethodList(TEN_SECONDS);
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
