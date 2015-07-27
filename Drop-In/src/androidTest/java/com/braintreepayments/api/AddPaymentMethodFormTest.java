package com.braintreepayments.api;

import android.test.suitebuilder.annotation.LargeTest;

import com.braintreepayments.api.test.BraintreePaymentActivityTestRunner;
import com.braintreepayments.testutils.TestClientTokenBuilder;

import org.junit.Test;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static com.braintreepayments.api.utils.PaymentFormHelpers.waitForAddPaymentFormHeader;
import static com.braintreepayments.testutils.ui.Matchers.withId;
import static org.hamcrest.CoreMatchers.not;

@LargeTest
public class AddPaymentMethodFormTest extends BraintreePaymentActivityTestRunner {

    @Test(timeout = 30000)
    public void cvvAndPostalCodeFieldsAreShownIfChallengesArePresent() {
        getActivity(new TestClientTokenBuilder().withCvvAndPostalCodeVerification().build());
        waitForAddPaymentFormHeader();

        onView(withId(com.braintreepayments.api.dropin.R.id.bt_card_form_cvv)).check(matches(isDisplayed()));
        onView(withId(com.braintreepayments.api.dropin.R.id.bt_card_form_postal_code)).check(matches(isDisplayed()));
    }

    @Test(timeout = 30000)
    public void cvvAndPostalCodeFieldsAreNotShownIfChallengesAreNotPresent() {
        String clientToken = new TestClientTokenBuilder()
                .withoutCvvChallenge()
                .withoutPostalCodeChallenge()
                .build();
        getActivity(clientToken);
        waitForAddPaymentFormHeader();

        onView(withId(com.braintreepayments.api.dropin.R.id.bt_card_form_cvv)).check(matches(not(isDisplayed())));
        onView(withId(com.braintreepayments.api.dropin.R.id.bt_card_form_postal_code)).check(matches(not(isDisplayed())));
    }
}
