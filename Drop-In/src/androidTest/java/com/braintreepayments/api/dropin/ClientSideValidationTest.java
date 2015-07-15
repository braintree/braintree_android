package com.braintreepayments.api.dropin;

import android.app.Activity;
import android.view.KeyEvent;

import com.braintreepayments.api.Braintree;
import com.braintreepayments.api.BraintreeApi;
import com.braintreepayments.api.exceptions.BraintreeException;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.models.PaymentMethodBuilder;
import com.braintreepayments.cardform.view.ErrorEditText;
import com.braintreepayments.testutils.CardNumber;
import com.braintreepayments.testutils.TestClientTokenBuilder;

import org.json.JSONException;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static com.braintreepayments.api.BraintreeTestUtils.setClientTokenExtraForTest;
import static com.braintreepayments.api.BraintreeTestUtils.setUpActivityTest;
import static com.braintreepayments.api.TestDependencyInjector.injectBraintree;
import static com.braintreepayments.api.utils.PaymentFormHelpers.fillInCardForm;
import static com.braintreepayments.api.utils.PaymentFormHelpers.waitForAddPaymentFormHeader;
import static com.braintreepayments.testutils.ui.Matchers.hasBackgroundResource;
import static com.braintreepayments.testutils.ui.Matchers.withHint;
import static com.braintreepayments.testutils.ui.Matchers.withId;
import static com.braintreepayments.testutils.ui.ViewHelper.closeSoftKeyboard;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class ClientSideValidationTest extends BraintreePaymentActivityTestCase {

    public void testMarksFieldsAsErrorWhenSubmitButtonIsClicked()
            throws ErrorWithResponse, BraintreeException, JSONException {
        String clientToken = new TestClientTokenBuilder().withPayPal().build();
        BraintreeApi api = spy(new BraintreeApi(mContext, clientToken));
        injectBraintree(clientToken, api);
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

        verify(api, never()).create((PaymentMethodBuilder) anyObject());
    }

    public void testShowsSubmitButtonAsDisabledWhenAFieldBecomesInvalid() throws JSONException {
        setUpActivityTest(this, new TestClientTokenBuilder().build());
        getActivity();

        waitForAddPaymentFormHeader();

        onView(withId(R.id.bt_card_form_submit_button)).check(matches(
                hasBackgroundResource(R.color.bt_button_disabled_color)));

        fillInCardForm(mContext);

        onView(withId(R.id.bt_card_form_submit_button)).check(
                matches(hasBackgroundResource(R.drawable.bt_submit_button_background)));

        onView(withId(R.id.bt_card_form_expiration)).perform(click());
        sendKeys(KeyEvent.KEYCODE_DEL, KeyEvent.KEYCODE_DEL, KeyEvent.KEYCODE_DEL);

        onView(withId(R.id.bt_card_form_submit_button)).check(
                matches(hasBackgroundResource(R.color.bt_button_disabled_color)));
    }

    public void testSubmitsToServerWhenFieldsPassClientValidation()
            throws ErrorWithResponse, BraintreeException, JSONException {
        String clientToken = new TestClientTokenBuilder().withPayPal().build();
        Braintree braintree = spy(injectBraintree(mContext, clientToken, clientToken));
        injectBraintree(clientToken, braintree);
        setClientTokenExtraForTest(this, clientToken);
        getActivity();

        waitForAddPaymentFormHeader();

        onView(withHint("Card Number")).perform(typeText(CardNumber.VISA));
        onView(withHint("Expiration")).perform(typeText("0619"), closeSoftKeyboard());
        onView(withHint("CVV")) .perform(typeText("123"), closeSoftKeyboard());
        onView(withHint("Postal Code")).perform(typeText("12345"));
        onView(withId(R.id.bt_card_form_submit_button)).perform(click());

        verify(braintree).create(any(PaymentMethodBuilder.class));
    }
}
