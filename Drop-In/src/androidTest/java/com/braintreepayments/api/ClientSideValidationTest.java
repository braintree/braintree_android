package com.braintreepayments.api;

import android.test.suitebuilder.annotation.LargeTest;
import android.view.KeyEvent;

import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.models.CardBuilder;
import com.braintreepayments.api.test.BraintreePaymentActivityTestRunner;
import com.braintreepayments.cardform.R;
import com.braintreepayments.cardform.view.ErrorEditText;
import com.braintreepayments.testutils.TestClientTokenBuilder;

import org.junit.Test;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static com.braintreepayments.api.utils.PaymentFormHelpers.fillInCardForm;
import static com.braintreepayments.api.utils.PaymentFormHelpers.waitForAddPaymentFormHeader;
import static com.braintreepayments.testutils.ui.Matchers.hasBackgroundResource;
import static com.braintreepayments.testutils.ui.Matchers.withId;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@LargeTest
public class ClientSideValidationTest extends BraintreePaymentActivityTestRunner {

    @Test(timeout = 30000)
    public void marksFieldsAsErrorWhenSubmitButtonIsClicked() {
        BraintreePaymentActivity activity = getActivity(new TestClientTokenBuilder()
                .withCvvAndPostalCodeVerification()
                .build());
        waitForAddPaymentFormHeader();

        onView(withId(com.braintreepayments.api.dropin.R.id.bt_card_form_submit_button)).perform(click());

        ErrorEditText cardNumber = (ErrorEditText) activity.findViewById(R.id.bt_card_form_card_number);
        ErrorEditText expiration = (ErrorEditText) activity.findViewById(R.id.bt_card_form_expiration);
        ErrorEditText cvv = (ErrorEditText) activity.findViewById(R.id.bt_card_form_cvv);
        ErrorEditText postalCode = (ErrorEditText) activity.findViewById(R.id.bt_card_form_postal_code);

        assertTrue(cardNumber.isError());
        assertTrue(expiration.isError());
        assertTrue(cvv.isError());
        assertTrue(postalCode.isError());

        verify(activity.mBraintreeFragment.getHttpClient(), never()).post(
                eq(TokenizationClient.versionedPath(
                        TokenizationClient.PAYMENT_METHOD_ENDPOINT + "/" +
                                new CardBuilder().getApiPath())),
                anyString(),
                any(HttpResponseCallback.class));
    }

    @Test(timeout = 30000)
    public void showsSubmitButtonAsDisabledWhenAFieldBecomesInvalid() {
        getActivity(new TestClientTokenBuilder().build());
        waitForAddPaymentFormHeader();
        onView(withId(com.braintreepayments.api.dropin.R.id.bt_card_form_submit_button))
                .check(matches(hasBackgroundResource(com.braintreepayments.api.dropin.R.color.bt_button_disabled_color)));
        fillInCardForm();
        onView(withId(com.braintreepayments.api.dropin.R.id.bt_card_form_submit_button))
                .check(matches(hasBackgroundResource(com.braintreepayments.api.dropin.R.drawable.bt_submit_button_background)));

        onView(withId(R.id.bt_card_form_expiration)).perform(click());
        getInstrumentation().sendCharacterSync(KeyEvent.KEYCODE_DEL);
        getInstrumentation().sendCharacterSync(KeyEvent.KEYCODE_DEL);
        getInstrumentation().sendCharacterSync(KeyEvent.KEYCODE_DEL);

        onView(withId(com.braintreepayments.api.dropin.R.id.bt_card_form_submit_button))
                .check(matches(hasBackgroundResource(com.braintreepayments.api.dropin.R.color.bt_button_disabled_color)));
    }

    @Test(timeout = 30000)
    public void submitsToServerWhenFieldsPassClientValidation() {
        BraintreePaymentActivity activity = getActivity(new TestClientTokenBuilder().build());
        fillInCardForm();

        onView(withId(com.braintreepayments.api.dropin.R.id.bt_card_form_submit_button)).perform(click());

        verify(activity.mBraintreeFragment.getHttpClient()).post(
                eq(TokenizationClient.versionedPath(
                        TokenizationClient.PAYMENT_METHOD_ENDPOINT + "/" +
                                new CardBuilder().getApiPath())),
                anyString(),
                any(HttpResponseCallback.class));
    }
}
