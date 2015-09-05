package com.braintreepayments.api.utils;

import com.braintreepayments.api.dropin.R;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.braintreepayments.testutils.ui.Matchers.withId;

public class Assertions {

    public static void assertSelectedPaymentMethodIs(int string) {
        onView(withText(R.string.bt_choose_payment_method)).check(doesNotExist());
        onView(withId(R.id.bt_payment_method_type)).check(matches(withText(string)));
    }
}
