package com.braintreepayments.api.dropin;

import android.app.Activity;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import com.braintreepayments.api.BraintreeTestUtils;
import com.braintreepayments.api.TestClientTokenBuilder;
import com.braintreepayments.api.dropin.view.CardEditText;
import com.google.android.apps.common.testing.ui.espresso.ViewAssertion;

import static com.braintreepayments.api.BraintreeTestUtils.injectBraintree;
import static com.braintreepayments.api.CardNumber.AMEX;
import static com.braintreepayments.api.CardNumber.INVALID_AMEX;
import static com.braintreepayments.api.CardNumber.INVALID_VISA;
import static com.braintreepayments.api.CardNumber.VISA;
import static com.braintreepayments.api.ui.Matchers.withHint;
import static com.braintreepayments.api.ui.Matchers.withId;
import static com.braintreepayments.api.ui.ViewHelper.closeSoftKeyboard;
import static com.braintreepayments.api.ui.ViewHelper.waitForView;
import static com.braintreepayments.api.utils.PaymentFormHelpers.waitForAddPaymentFormHeader;
import static com.braintreepayments.api.utils.ViewHintHelper.assertHintsAre;
import static com.braintreepayments.api.utils.ViewHintHelper.theIconHintIs;
import static com.google.android.apps.common.testing.ui.espresso.Espresso.onView;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.clearText;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.click;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.typeText;
import static com.google.android.apps.common.testing.ui.espresso.assertion.ViewAssertions.matches;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.hasFocus;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.isDisplayed;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;

public class AddPaymentMethodFormTest extends BraintreePaymentActivityTestCase {

    public void testCvvHintsShowAndDisappearOnClick() throws InterruptedException {
        BraintreeTestUtils.setUpActivityTest(this);
        getActivity();

        waitForAddPaymentFormHeader();
        onView(withHint("CVV"))
                .check(thereIsNoIconHint())
                .perform(click())
                .check(theIconHintIs(mContext, R.drawable.bt_cvv_highlighted))
                .perform(typeText("123"), closeSoftKeyboard());
        onView(withHint("Postal Code")).perform(typeText("12345"));
        onView(withHint("CVV"))
                .check(thereIsNoIconHint()); // check that the hint is gone after defocusing
    }

    public void testCorrectCardHintsAreDisplayed() {
        BraintreeTestUtils.setUpActivityTest(this);
        getActivity();

        waitForView(withHint("Card Number"))
                .check(theIconHintIs(mContext, R.drawable.bt_card_highlighted))
                .perform(typeText("4")).check(theIconHintIs(mContext, R.drawable.bt_visa))
                .perform(clearText())
                .perform(typeText("51")).check(theIconHintIs(mContext, R.drawable.bt_mastercard))
                .perform(clearText())
                .perform(typeText("37")).check(theIconHintIs(mContext, R.drawable.bt_amex))
                .perform(clearText())
                .perform(typeText("35")).check(theIconHintIs(mContext, R.drawable.bt_jcb))
                .perform(clearText())
                .perform(typeText("5018")).check(theIconHintIs(mContext, R.drawable.bt_maestro))
                .perform(clearText())
                .perform(typeText("1234"))
                .check(theIconHintIs(mContext, R.drawable.bt_card_highlighted));
    }

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

    public void testSetsIMEActionAsGoForExpirationIfCvvAndPostalAreNotPresent() {
        BraintreeTestUtils
                .setUpActivityTest(this, new TestClientTokenBuilder().withoutCvvChallenge()
                        .withoutPostalCodeChallenge().build());
        Activity activity = getActivity();

        waitForAddPaymentFormHeader();

        assertEquals(EditorInfo.IME_ACTION_GO,
                ((TextView) activity.findViewById(R.id.bt_card_form_expiration)).getImeOptions());
    }

    public void testSetsIMEActionAsGoForCvvIfCvvIsPresentAndPostalIsNot() {
        BraintreeTestUtils.setUpActivityTest(this, new TestClientTokenBuilder()
                .withoutPostalCodeChallenge().build());
        Activity activity = getActivity();

        waitForAddPaymentFormHeader();

        assertEquals(EditorInfo.IME_ACTION_NEXT,
                ((TextView) activity.findViewById(R.id.bt_card_form_expiration)).getImeOptions());
        assertEquals(EditorInfo.IME_ACTION_GO,
                ((TextView) activity.findViewById(R.id.bt_card_form_cvv)).getImeOptions());
    }

    public void testSetsIMEActionAsGoForPostalAndNextForExpirationIfCvvIsNotPresent() {
        BraintreeTestUtils.setUpActivityTest(this, new TestClientTokenBuilder()
                .withoutCvvChallenge().build());
        Activity activity = getActivity();

        waitForAddPaymentFormHeader();

        assertEquals(EditorInfo.IME_ACTION_NEXT,
                ((TextView) activity.findViewById(R.id.bt_card_form_expiration)).getImeOptions());
        assertEquals(EditorInfo.IME_ACTION_GO,
                ((TextView) activity.findViewById(R.id.bt_card_form_postal_code)).getImeOptions());
    }

    public void testSetsIMEActionAsGoForPostalCodeIfCvvAndPostalArePresent() {
        BraintreeTestUtils.setUpActivityTest(this, new TestClientTokenBuilder().build());
        Activity activity = getActivity();

        waitForAddPaymentFormHeader();

        assertEquals(EditorInfo.IME_ACTION_NEXT,
                ((TextView) activity.findViewById(R.id.bt_card_form_expiration)).getImeOptions());
        assertEquals(EditorInfo.IME_ACTION_NEXT,
                ((TextView) activity.findViewById(R.id.bt_card_form_cvv)).getImeOptions());
        assertEquals(EditorInfo.IME_ACTION_GO,
                ((TextView) activity.findViewById(R.id.bt_card_form_postal_code)).getImeOptions());
    }

    public void testCardNumberAdvancesToExpirationWhenCompleteAndValid() {
        BraintreeTestUtils.setUpActivityTest(this);
        getActivity();

        waitForAddPaymentFormHeader();

        onView(withId(R.id.bt_card_form_card_number)).perform(typeText(VISA), closeSoftKeyboard());
        onView(withId(R.id.bt_card_form_expiration)).check(matches(hasFocus()));

        onView(withId(R.id.bt_card_form_card_number)).perform(clearText(), typeText(AMEX), closeSoftKeyboard());
        onView(withId(R.id.bt_card_form_expiration)).check(matches(hasFocus()));
    }

    public void testCardNumberDoesntAdvanceWhenCompleteAndInvalid() {
        BraintreeTestUtils.setUpActivityTest(this);
        BraintreePaymentActivity activity = getActivity();

        waitForAddPaymentFormHeader();

        CardEditText cardEditText =
                (CardEditText) activity.findViewById(R.id.bt_card_form_card_number);
        onView(withId(R.id.bt_card_form_card_number)).perform(typeText(INVALID_VISA),
                closeSoftKeyboard());
        assertTrue(cardEditText.isError());
        onView(withId(R.id.bt_card_form_card_number)).check(matches(hasFocus()));

        onView(withId(R.id.bt_card_form_card_number)).perform(clearText(), typeText(
               INVALID_AMEX), closeSoftKeyboard());
        assertTrue(cardEditText.isError());
        onView(withId(R.id.bt_card_form_card_number)).check(matches(hasFocus()));
    }

    public void testExpirationAdvancesToCvvWhenComplete() {
        BraintreeTestUtils.setUpActivityTest(this, new TestClientTokenBuilder().build());
        getActivity();

        waitForAddPaymentFormHeader();

        onView(withId(R.id.bt_card_form_expiration)).perform(typeText("0616"), closeSoftKeyboard());
        onView(withId(R.id.bt_card_form_cvv)).check(matches(hasFocus()));

        onView(withId(R.id.bt_card_form_expiration)).perform(clearText(), typeText("062015"),
                closeSoftKeyboard());
        onView(withId(R.id.bt_card_form_expiration)).check(matches(withText("062015")));
        onView(withId(R.id.bt_card_form_cvv)).check(matches(withText("")));
        onView(withId(R.id.bt_card_form_cvv)).check(matches(hasFocus()));
    }

    public void testCvvAdvancesToPostalWhenComplete() {
        BraintreeTestUtils.setUpActivityTest(this, new TestClientTokenBuilder().build());
        getActivity();

        waitForAddPaymentFormHeader();

        onView(withId(R.id.bt_card_form_card_number)).perform(typeText(VISA), closeSoftKeyboard());
        onView(withId(R.id.bt_card_form_cvv)).perform(typeText("123"), closeSoftKeyboard());
        onView(withId(R.id.bt_card_form_postal_code)).check(matches(hasFocus()));
        onView(withId(R.id.bt_card_form_cvv)).check(matches(withText("123")));

        onView(withId(R.id.bt_card_form_card_number)).perform(clearText(), typeText(AMEX), closeSoftKeyboard());
        onView(withId(R.id.bt_card_form_cvv)).perform(clearText(), typeText("1234"), closeSoftKeyboard());
        onView(withId(R.id.bt_card_form_postal_code)).check(matches(hasFocus()));
        onView(withId(R.id.bt_card_form_cvv)).check(matches(withText("1234")));
    }

    public void testAdvancingDoesNotCrashWhenThereIsNotANextField() {
        BraintreeTestUtils.setUpActivityTest(this);
        getActivity();

        waitForAddPaymentFormHeader();

        onView(withId(R.id.bt_card_form_expiration)).perform(typeText("0615"));
        onView(withId(R.id.bt_card_form_expiration)).check(matches(withText("0615")));
    }

    private static ViewAssertion thereIsNoIconHint() {
        return assertHintsAre(null, null, null, null);
    }

}
