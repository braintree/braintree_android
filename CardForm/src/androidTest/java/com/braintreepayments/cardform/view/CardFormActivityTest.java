package com.braintreepayments.cardform.view;

import com.braintreepayments.cardform.R;
import com.braintreepayments.cardform.test.TestActivityTestCase;
import com.braintreepayments.testutils.CardNumber;
import com.braintreepayments.testutils.ui.Matchers;
import com.google.android.apps.common.testing.ui.espresso.action.ViewActions;

import static com.braintreepayments.testutils.CardNumber.AMEX;
import static com.braintreepayments.testutils.CardNumber.INVALID_AMEX;
import static com.braintreepayments.testutils.CardNumber.INVALID_VISA;
import static com.braintreepayments.testutils.CardNumber.VISA;
import static com.braintreepayments.testutils.ui.Assertions.assertHintsAre;
import static com.braintreepayments.testutils.ui.Assertions.theIconHintIs;
import static com.braintreepayments.testutils.ui.Matchers.withHint;
import static com.braintreepayments.testutils.ui.Matchers.withId;
import static com.braintreepayments.testutils.ui.RotationHelper.rotateToLandscape;
import static com.braintreepayments.testutils.ui.RotationHelper.rotateToPortrait;
import static com.braintreepayments.testutils.ui.ViewHelper.closeSoftKeyboard;
import static com.google.android.apps.common.testing.ui.espresso.Espresso.onView;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.clearText;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.click;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.typeText;
import static com.google.android.apps.common.testing.ui.espresso.assertion.ViewAssertions.matches;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.hasFocus;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withText;

public class CardFormActivityTest extends TestActivityTestCase {

    public void testCorrectCardHintsAreDisplayed() {
        setupCardForm();

        onView(withHint("Card Number"))
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

    public void testCvvHintsShowAndDisappearOnClick() throws InterruptedException {
        setupCardForm();

        onView(withHint("CVV"))
                .check(assertHintsAre(null, null, null, null))
                .perform(click())
                .check(theIconHintIs(mContext, R.drawable.bt_cvv_highlighted))
                .perform(typeText("123"), closeSoftKeyboard());
        onView(withHint("Postal Code")).perform(typeText("12345"));
        onView(withHint("CVV"))
                .check(assertHintsAre(null, null, null, null)); // check that the hint is gone after defocusing
    }

    public void testCardNumberAdvancesToExpirationWhenCompleteAndValid() {
        setupCardForm();

        onView(withId(R.id.bt_card_form_card_number)).perform(typeText(VISA), closeSoftKeyboard());
        onView(withId(R.id.bt_card_form_expiration)).check(matches(hasFocus()));

        onView(withId(R.id.bt_card_form_card_number)).perform(clearText(), typeText(AMEX), closeSoftKeyboard());
        onView(withId(R.id.bt_card_form_expiration)).check(matches(hasFocus()));
    }

    public void testCardNumberDoesntAdvanceWhenCompleteAndInvalid() {
        setupCardForm();

        CardEditText cardEditText =
                (CardEditText) mActivity.findViewById(R.id.bt_card_form_card_number);
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
        setupCardForm();

        onView(withId(R.id.bt_card_form_expiration)).perform(typeText("0616"), closeSoftKeyboard());
        onView(withId(R.id.bt_card_form_cvv)).check(matches(hasFocus()));

        onView(withId(R.id.bt_card_form_expiration)).perform(clearText(), typeText("062015"),
                closeSoftKeyboard());
        onView(withId(R.id.bt_card_form_expiration)).check(matches(withText("062015")));
        onView(withId(R.id.bt_card_form_cvv)).check(matches(withText("")));
        onView(withId(R.id.bt_card_form_cvv)).check(matches(hasFocus()));
    }

    public void testCvvAdvancesToPostalWhenComplete() {
        setupCardForm();

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
        setupCardForm(true, true, false, false);

        onView(withId(R.id.bt_card_form_expiration)).perform(typeText("0615"));
        onView(withId(R.id.bt_card_form_expiration)).check(matches(withText("0615")));
    }

    public void testValuesAreRestored() {
        setupCardForm();

        onView(withId(R.id.bt_card_form_card_number)).perform(typeText(VISA), closeSoftKeyboard());
        onView(withId(R.id.bt_card_form_expiration)).perform(typeText("1220"), closeSoftKeyboard());
        onView(withId(R.id.bt_card_form_cvv)).perform(typeText("123"), closeSoftKeyboard());
        onView(withId(R.id.bt_card_form_postal_code)).perform(typeText("12345"),
                closeSoftKeyboard());

        rotateToLandscape(this);

        onView(withId(R.id.bt_card_form_card_number)).check(matches(withText(VISA)));
        onView(withId(R.id.bt_card_form_expiration)).check(matches(withText("1220")));
        onView(withId(R.id.bt_card_form_cvv)).check(matches(withText("123")));
        onView(withId(R.id.bt_card_form_postal_code)).check(matches(withText("12345")));

        rotateToPortrait(this);
    }

    public void testMarksCardNumberAsErrorWhenFocusChangesAndCardNumberFailsValidation() {
        setupCardForm();

        onView(withId(R.id.bt_card_form_card_number)).perform(typeText("4"));
        ErrorEditText cardEditText = (ErrorEditText) mActivity.findViewById(R.id.bt_card_form_card_number);
        assertTrue(cardEditText.isFocused());
        assertFalse(cardEditText.isError());

        onView(withId(R.id.bt_card_form_expiration)).perform(click(), closeSoftKeyboard());
        assertFalse(cardEditText.isFocused());
        assertTrue(cardEditText.isError());
    }

    public void testMarksExpirationAsErrorWhenFocusChangesAndExpirationFailsValidation() {
        setupCardForm();

        onView(withId(R.id.bt_card_form_expiration)).perform(typeText("1"), closeSoftKeyboard());
        ErrorEditText expirationEditText = (ErrorEditText) mActivity.findViewById(R.id.bt_card_form_expiration);
        assertTrue(expirationEditText.isFocused());
        assertFalse(expirationEditText.isError());

        onView(withId(R.id.bt_card_form_card_number)).perform(click(), closeSoftKeyboard());
        assertFalse(expirationEditText.isFocused());
        assertTrue(expirationEditText.isError());
    }

    public void testMarksCvvAsErrorWhenFocusChangesAndCvvNotProperLength() {
        setupCardForm();

        onView(withId(R.id.bt_card_form_cvv)).perform(typeText("1"), closeSoftKeyboard());
        ErrorEditText cvvEditText = (ErrorEditText) mActivity.findViewById(R.id.bt_card_form_cvv);
        assertTrue(cvvEditText.isFocused());
        assertFalse(cvvEditText.isError());

        onView(withId(R.id.bt_card_form_card_number)).perform(click());
        assertFalse(cvvEditText.isFocused());
        assertTrue(cvvEditText.isError());
    }

    public void testMarksCvvAsErrorWhenCardChangesToAmex() {
        setupCardForm();

        onView(withId(R.id.bt_card_form_cvv)).perform(typeText("111"), closeSoftKeyboard());
        onView(Matchers.withId(R.id.bt_card_form_card_number)).perform(ViewActions.click());
        ErrorEditText cvvEditText = (ErrorEditText) mActivity.findViewById(R.id.bt_card_form_cvv);
        assertFalse(cvvEditText.isFocused());
        assertFalse(cvvEditText.isError());

        onView(withId(R.id.bt_card_form_card_number)).perform(typeText(CardNumber.AMEX),
                closeSoftKeyboard());
        onView(withId(R.id.bt_card_form_cvv)).perform(click(), closeSoftKeyboard());
        onView(withId(R.id.bt_card_form_card_number)).perform(click());
        assertFalse(cvvEditText.isFocused());
        assertTrue(cvvEditText.isError());
    }

    public void testMarksPostalCodeWhenFocusChangesAndPostalCodeBlank() {
        setupCardForm();

        onView(withId(R.id.bt_card_form_postal_code)).perform(click());
        ErrorEditText postalEditText = (ErrorEditText) mActivity.findViewById(R.id.bt_card_form_postal_code);
        assertTrue(postalEditText.isFocused());
        assertFalse(postalEditText.isError());

        onView(withId(R.id.bt_card_form_cvv)).perform(click());
        assertFalse(postalEditText.isFocused());
        assertTrue(postalEditText.isError());
    }

    public void testDoesNotCrashWhenNoListenersAreSet() {
        setupCardForm();
        onView(withId(R.id.bt_card_form_card_number)).perform(click(), typeText(VISA));
    }

}
