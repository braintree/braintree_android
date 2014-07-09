package com.braintreepayments.api.utils;

import com.braintreepayments.api.dropin.R;

import static com.braintreepayments.api.utils.Matchers.withHint;
import static com.braintreepayments.api.utils.ViewHelper.waitForPaymentMethodList;
import static com.braintreepayments.api.utils.ViewHelper.waitForView;
import static com.braintreepayments.api.utils.ViewHelper.TEN_SECONDS;
import static com.google.android.apps.common.testing.ui.espresso.Espresso.onView;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.click;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.typeText;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withId;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withText;

public class FormHelpers {

    public static void fillInPayPal() {
        onView(withId(R.id.paypal_appswitch_button)).perform(click());

        waitForView(withHint("Email"));
        onView(withHint("Email")).perform(typeText("bt_buyer_us@paypal.com"));
        onView(withHint("Password")).perform(typeText("11111111"));
        onView(withText("Log In")).perform(click());

        waitForView(withText("Agree"));
        onView(withText("Agree")).perform(click());

        waitForPaymentMethodList(TEN_SECONDS);
    }

}
