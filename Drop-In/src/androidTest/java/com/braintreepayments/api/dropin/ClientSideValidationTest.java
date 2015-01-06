package com.braintreepayments.api.dropin;

import android.app.Activity;
import android.view.KeyEvent;

import com.braintreepayments.api.BraintreeApi;
import com.braintreepayments.api.BraintreeTestUtils;
import com.braintreepayments.api.exceptions.BraintreeException;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.models.PaymentMethod;
import com.braintreepayments.cardform.view.ErrorEditText;
import com.braintreepayments.testutils.CardNumber;
import com.braintreepayments.testutils.TestClientTokenBuilder;

import static com.braintreepayments.api.BraintreeTestUtils.setUpActivityTest;
import static com.braintreepayments.api.utils.PaymentFormHelpers.fillInCardForm;
import static com.braintreepayments.api.utils.PaymentFormHelpers.waitForAddPaymentFormHeader;
import static com.braintreepayments.testutils.ui.Matchers.hasBackgroundResource;
import static com.braintreepayments.testutils.ui.Matchers.withHint;
import static com.braintreepayments.testutils.ui.Matchers.withId;
import static com.braintreepayments.testutils.ui.ViewHelper.closeSoftKeyboard;
import static com.google.android.apps.common.testing.ui.espresso.Espresso.onView;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.click;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.typeText;
import static com.google.android.apps.common.testing.ui.espresso.assertion.ViewAssertions.matches;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ClientSideValidationTest extends BraintreePaymentActivityTestCase {

    public void testMarksFieldsAsErrorWhenSubmitButtonIsClicked()
            throws ErrorWithResponse, BraintreeException {
        String clientToken = new TestClientTokenBuilder().withFakePayPal().build();
        BraintreeApi api = spy(new BraintreeApi(mContext, clientToken));
        BraintreeTestUtils.injectBraintreeApi(clientToken, api);
        setUpActivityTest(this, clientToken);
        Activity activity = getActivity();

        waitForAddPaymentFormHeader();

        onView(withId(R.id.bt_card_form_submit_button)).perform(click());

        ErrorEditText cardNumber = (ErrorEditText) activity.findViewById(R.id.bt_card_form_card_number);
        ErrorEditText expiration = (ErrorEditText) activity.findViewById(R.id.bt_card_form_expiration);
        ErrorEditText cvv = (ErrorEditText) activity.findViewById(R.id.bt_card_form_cvv);
        ErrorEditText postalCode = (ErrorEditText) activity.findViewById(R.id.bt_card_form_postal_code);

        assertTrue(cardNumber.isError());
        assertTrue(expiration.isError());
        assertTrue(cvv.isError());
        assertTrue(postalCode.isError());

        verify(api, never()).create((PaymentMethod.Builder) anyObject());
    }

    public void testShowsSubmitButtonAsDisabledWhenAFieldBecomesInvalid() {
        setUpActivityTest(this, new TestClientTokenBuilder().build());
        getActivity();

        waitForAddPaymentFormHeader();

        onView(withId(R.id.bt_card_form_submit_button)).check(matches(
                hasBackgroundResource(mContext, R.color.bt_button_disabled_color)));

        fillInCardForm(mContext);

        onView(withId(R.id.bt_card_form_submit_button)).check(
                matches(hasBackgroundResource(mContext, R.drawable.bt_submit_button_background)));

        onView(withId(R.id.bt_card_form_expiration)).perform(click());
        sendKeys(KeyEvent.KEYCODE_DEL, KeyEvent.KEYCODE_DEL, KeyEvent.KEYCODE_DEL);

        onView(withId(R.id.bt_card_form_submit_button)).check(
                matches(hasBackgroundResource(mContext, R.color.bt_button_disabled_color)));
    }

    public void testSubmitsToServerWhenFieldsPassClientValidation()
            throws ErrorWithResponse, BraintreeException {
        String clientToken = new TestClientTokenBuilder().withFakePayPal().build();
        BraintreeApi api = spy(new BraintreeApi(mContext, clientToken));
        BraintreeTestUtils.injectBraintreeApi(clientToken, api);
        setUpActivityTest(this, clientToken);
        getActivity();

        waitForAddPaymentFormHeader();

        onView(withHint("Card Number")).perform(typeText(CardNumber.VISA));
        onView(withHint("Expiration")).perform(typeText("0619"), closeSoftKeyboard());
        onView(withHint("CVV")) .perform(typeText("123"), closeSoftKeyboard());
        onView(withHint("Postal Code")).perform(typeText("12345"));
        onView(withId(R.id.bt_card_form_submit_button)).perform(click());

        verify(api, times(1)).create((PaymentMethod.Builder) anyObject());
    }

}
