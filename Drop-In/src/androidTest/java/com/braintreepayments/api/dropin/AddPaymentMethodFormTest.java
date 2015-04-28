package com.braintreepayments.api.dropin;

import com.braintreepayments.testutils.TestClientTokenBuilder;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static com.braintreepayments.api.BraintreeTestUtils.setUpActivityTest;
import static com.braintreepayments.api.utils.PaymentFormHelpers.waitForAddPaymentFormHeader;
import static com.braintreepayments.testutils.ui.Matchers.withId;
import static org.hamcrest.CoreMatchers.not;

public class AddPaymentMethodFormTest extends BraintreePaymentActivityTestCase {

    public void testCvvAndPostalCodeFieldsAreShownIfChallengesArePresent() {
        String clientToken = new TestClientTokenBuilder().build();
        setUpActivityTest(this, clientToken);

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
        setUpActivityTest(this, clientToken);

        getActivity();
        waitForAddPaymentFormHeader();

        onView(withId(R.id.bt_card_form_cvv)).check(matches(not(isDisplayed())));
        onView(withId(R.id.bt_card_form_postal_code)).check(matches(not(isDisplayed())));
    }

}
