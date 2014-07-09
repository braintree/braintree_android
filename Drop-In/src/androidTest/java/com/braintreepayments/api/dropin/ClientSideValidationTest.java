package com.braintreepayments.api.dropin;

import android.app.Activity;
import android.os.SystemClock;

import com.braintreepayments.api.BraintreeApi;
import com.braintreepayments.api.TestClientTokenBuilder;
import com.braintreepayments.api.TestUtils;
import com.braintreepayments.api.dropin.view.BraintreeEditText;
import com.braintreepayments.api.exceptions.BraintreeException;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.models.PaymentMethod;

import static com.braintreepayments.api.TestUtils.injectBraintreeApi;
import static com.braintreepayments.api.utils.Matchers.withHint;
import static com.braintreepayments.api.utils.ViewHelper.waitForAddPaymentFormHeader;
import static com.braintreepayments.api.utils.ViewHelper.waitForKeyboardToClose;
import static com.braintreepayments.api.utils.ViewHelper.waitForView;
import static com.google.android.apps.common.testing.ui.espresso.Espresso.onView;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.click;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.closeSoftKeyboard;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.typeText;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withId;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ClientSideValidationTest extends BraintreePaymentActivityTestCase {

    public void testMarksFieldsAsErrorWhenSubmitButtonIsClicked()
            throws ErrorWithResponse, BraintreeException {
        String clientToken = new TestClientTokenBuilder().withPayPal().build();
        BraintreeApi api = spy(new BraintreeApi(getInstrumentation().getContext(), clientToken));
        injectBraintreeApi(clientToken, api);
        TestUtils.setUpActivityTest(this, clientToken);
        Activity activity = getActivity();

        waitForAddPaymentFormHeader();

        onView(withId(R.id.card_form_complete_button)).perform(click());

        BraintreeEditText cardNumber = (BraintreeEditText) activity.findViewById(R.id.card_form_card_number);
        BraintreeEditText expiration = (BraintreeEditText) activity.findViewById(R.id.card_form_card_number);
        BraintreeEditText cvv = (BraintreeEditText) activity.findViewById(R.id.card_form_card_number);
        BraintreeEditText postalCode = (BraintreeEditText) activity.findViewById(R.id.card_form_card_number);

        assertTrue(cardNumber.isError());
        assertTrue(expiration.isError());
        assertTrue(cvv.isError());
        assertTrue(postalCode.isError());

        verify(api, never()).create((PaymentMethod.Builder) anyObject());
    }

    public void testSubmitsToServerWhenFieldsPassClientValidation()
            throws ErrorWithResponse, BraintreeException {
        String clientToken = new TestClientTokenBuilder().withPayPal().build();
        BraintreeApi api = spy(new BraintreeApi(getInstrumentation().getContext(), clientToken));
        injectBraintreeApi(clientToken, api);
        TestUtils.setUpActivityTest(this, clientToken);
        getActivity();

        waitForAddPaymentFormHeader();

        onView(withHint("Card Number")).perform(typeText("4111111111111111"));
        onView(withHint("Expiration")).perform(typeText("0619"), closeSoftKeyboard(), waitForKeyboardToClose());
        onView(withHint("CVV")) .perform(typeText("123"), closeSoftKeyboard(), waitForKeyboardToClose());
        onView(withHint("Postal Code")).perform(typeText("12345"));
        onView(withId(R.id.card_form_complete_button)).perform(click());

        verify(api, times(1)).create((PaymentMethod.Builder) anyObject());
    }

    public void testMarksCardNumberAsErrorWhenFocusChangesAndCardNumberFailsClientValidation() {
        TestUtils.setUpActivityTest(this);
        BraintreePaymentActivity activity = getActivity();

        waitForView(withId(R.id.card_form_card_number)).perform(typeText("4"));
        BraintreeEditText cardEditText = (BraintreeEditText) activity.findViewById(R.id.card_form_card_number);
        assertTrue(cardEditText.isFocused());
        assertFalse(cardEditText.isError());

        onView(withId(R.id.card_form_expiration)).perform(click(), closeSoftKeyboard(), waitForKeyboardToClose());
        SystemClock.sleep(1000);
        assertFalse(cardEditText.isFocused());
        assertTrue(cardEditText.isError());
    }

    public void testMarksExpirationAsErrorWhenFocusChangesAndExpirationFailsClientValidation() {
        TestUtils.setUpActivityTest(this);
        BraintreePaymentActivity activity = getActivity();

        waitForView(withId(R.id.card_form_expiration)).perform(typeText("1"));
        BraintreeEditText expirationEditText = (BraintreeEditText) activity.findViewById(R.id.card_form_expiration);
        assertTrue(expirationEditText.isFocused());
        assertFalse(expirationEditText.isError());

        onView(withId(R.id.card_form_card_number)).perform(click(), closeSoftKeyboard(), waitForKeyboardToClose());
        assertFalse(expirationEditText.isFocused());
        assertTrue(expirationEditText.isError());
    }

    public void testMarksCvvAsErrorWhenFocusChangesAndCvvNotProperLength() {
        TestUtils.setUpActivityTest(this);
        BraintreePaymentActivity activity = getActivity();

        waitForView(withId(R.id.card_form_cvv)).perform(typeText("1"), closeSoftKeyboard());
        BraintreeEditText cvvEditText = (BraintreeEditText) activity.findViewById(R.id.card_form_cvv);
        assertTrue(cvvEditText.isFocused());
        assertFalse(cvvEditText.isError());

        onView(withId(R.id.card_form_card_number)).perform(click());
        SystemClock.sleep(1000);
        assertFalse(cvvEditText.isFocused());
        assertTrue(cvvEditText.isError());
    }

    public void testMarksCvvAsErrorWhenCardChangesToAmex() {
        TestUtils.setUpActivityTest(this);
        BraintreePaymentActivity activity = getActivity();

        waitForView(withId(R.id.card_form_cvv)).perform(typeText("111"), closeSoftKeyboard(), waitForKeyboardToClose());
        onView(withId(R.id.card_form_card_number)).perform(click());
        BraintreeEditText cvvEditText = (BraintreeEditText) activity.findViewById(R.id.card_form_cvv);
        assertFalse(cvvEditText.isFocused());
        assertFalse(cvvEditText.isError());

        onView(withId(R.id.card_form_card_number)).perform(typeText("378282246310005"));
        onView(withId(R.id.card_form_cvv)).perform(click(), closeSoftKeyboard(), waitForKeyboardToClose());
        onView(withId(R.id.card_form_card_number)).perform(click());
        assertFalse(cvvEditText.isFocused());
        assertTrue(cvvEditText.isError());
    }

    public void testMarksPostalCodeWhenFocusChangesAndPostalCodeBlank() {
        TestUtils.setUpActivityTest(this);
        BraintreePaymentActivity activity = getActivity();

        waitForView(withId(R.id.card_form_postal_code)).perform(click());
        BraintreeEditText postalEditText = (BraintreeEditText) activity.findViewById(R.id.card_form_postal_code);
        assertTrue(postalEditText.isFocused());
        assertFalse(postalEditText.isError());

        onView(withId(R.id.card_form_cvv)).perform(click());
        assertFalse(postalEditText.isFocused());
        assertTrue(postalEditText.isError());
    }

}
