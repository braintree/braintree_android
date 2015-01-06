package com.braintreepayments.api.dropin;

import com.braintreepayments.api.BraintreeTestUtils;
import com.braintreepayments.testutils.TestClientTokenBuilder;

import static com.braintreepayments.api.BraintreeTestUtils.injectBraintree;
import static com.braintreepayments.testutils.ui.Matchers.withId;
import static com.braintreepayments.api.utils.PaymentFormHelpers.waitForAddPaymentFormHeader;
import static com.google.android.apps.common.testing.ui.espresso.Espresso.onView;
import static com.google.android.apps.common.testing.ui.espresso.assertion.ViewAssertions.matches;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.isDisplayed;
import static org.hamcrest.Matchers.not;

public class AddPaymentMethodFormTest extends BraintreePaymentActivityTestCase {

    public void testCvvAndPostalCodeFieldsAreShownIfChallengesArePresent() {
        String clientToken = new TestClientTokenBuilder().build();
        BraintreeTestUtils.setUpActivityTest(this, clientToken);

        getActivity();
        waitForAddPaymentFormHeader();

        onView(withId(R.id.bt_card_form_cvv)).check(matches(isDisplayed()));
        onView(withId(R.id.bt_card_form_postal_code)).check(matches(isDisplayed()));
    }

    public void testCvvAndPostalCodeFieldsAreNotShownIfChallengesAreNotPresent() {
        String clientToken = new TestClientTokenBuilder()
                .withoutCvvChallenge()
                .withoutPostalCodeChallenge()
                .build();
        injectBraintree(mContext, clientToken);
        BraintreeTestUtils.setUpActivityTest(this, clientToken);

        getActivity();
        waitForAddPaymentFormHeader();

        onView(withId(R.id.bt_card_form_cvv)).check(matches(not(isDisplayed())));
        onView(withId(R.id.bt_card_form_postal_code)).check(matches(not(isDisplayed())));
    }

}
