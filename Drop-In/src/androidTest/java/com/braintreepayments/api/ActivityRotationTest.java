package com.braintreepayments.api;

import android.content.Intent;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.test.suitebuilder.annotation.LargeTest;

import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.test.BraintreePaymentActivityTestRunner;
import com.braintreepayments.testutils.TestClientTokenBuilder;

import org.json.JSONException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isEnabled;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.braintreepayments.api.test.RotationHelper.rotateToLandscape;
import static com.braintreepayments.api.test.RotationHelper.rotateToPortrait;
import static com.braintreepayments.api.utils.Assertions.assertSelectedPaymentMethodIs;
import static com.braintreepayments.api.utils.PaymentFormHelpers.addCardAndAssertSuccess;
import static com.braintreepayments.api.utils.PaymentFormHelpers.fillInCardForm;
import static com.braintreepayments.api.utils.PaymentFormHelpers.onCardField;
import static com.braintreepayments.api.utils.PaymentFormHelpers.onCvvField;
import static com.braintreepayments.api.utils.PaymentFormHelpers.onExpirationField;
import static com.braintreepayments.api.utils.PaymentFormHelpers.onPostalCodeField;
import static com.braintreepayments.api.utils.PaymentFormHelpers.waitForAddPaymentFormHeader;
import static com.braintreepayments.api.utils.PaymentFormHelpers.waitForPaymentMethodNonceList;
import static com.braintreepayments.testutils.CardNumber.AMEX;
import static com.braintreepayments.testutils.CardNumber.VISA;
import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static com.braintreepayments.testutils.ui.Matchers.hasBackgroundResource;
import static com.braintreepayments.testutils.ui.Matchers.withId;
import static com.braintreepayments.testutils.ui.ViewHelper.FOUR_SECONDS;
import static com.braintreepayments.testutils.ui.ViewHelper.TWO_SECONDS;
import static com.braintreepayments.testutils.ui.ViewHelper.closeSoftKeyboard;
import static com.braintreepayments.testutils.ui.ViewHelper.waitForView;
import static org.hamcrest.Matchers.not;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.startsWith;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@LargeTest
public class ActivityRotationTest extends BraintreePaymentActivityTestRunner {

    @Before
    public void setUp() {
        assumeTrue(VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN_MR2);
        rotateToPortrait();
    }

    @After
    public void tearDown() {
        rotateToPortrait();
    }

    @Test(timeout = 30000)
    public void addPaymentViewIsRestoredOnRotation() throws JSONException {
        getActivity(new TestClientTokenBuilder().withCvvAndPostalCodeVerification().build());

        fillInCardForm();
        onView(withId(com.braintreepayments.api.dropin.R.id.bt_card_form_submit_button)).check(
                matches(isEnabled()));

        rotateToLandscape();
        waitForAddPaymentFormHeader();
        onCardField().check(matches(withText(VISA)));
        onExpirationField().check(matches(withText("0619")));
        onCvvField().check(matches(withText("123")));
        onPostalCodeField().check(matches(withText("12345")));
        onView(withId(
                com.braintreepayments.api.dropin.R.id.bt_card_form_submit_button)).check(matches(isEnabled()));
    }

    @Test(timeout = 30000)
    public void addPaymentViewIsResumedOnRotationWhenThereAreExistingPaymentMethods() {
        Intent intent = new PaymentRequest()
                .clientToken(new TestClientTokenBuilder().build())
                .getIntent(getTargetContext())
                .putExtra(BraintreePaymentTestActivity.GET_PAYMENT_METHODS,
                        stringFromFixture("responses/get_payment_methods_visa_response.json"));
        getActivity(intent);

        waitForPaymentMethodNonceList();
        onView(withId(com.braintreepayments.api.dropin.R.id.bt_change_payment_method_link)).perform(click());
        waitForAddPaymentFormHeader();
        onCardField().perform(typeText(AMEX), closeSoftKeyboard());

        rotateToLandscape();

        waitForAddPaymentFormHeader();
        onCardField().check(matches(withText(AMEX)));
    }

    @Test(timeout = 30000)
    public void selectPaymentViewIsRestoredOnRotation() {
        Intent intent = new PaymentRequest()
                .clientToken(new TestClientTokenBuilder().build())
                .getIntent(getTargetContext())
                .putExtra(BraintreePaymentTestActivity.GET_PAYMENT_METHODS,
                        stringFromFixture("responses/get_payment_methods_two_cards_response.json"));
        getActivity(intent);

        waitForPaymentMethodNonceList();
        onView(withId(com.braintreepayments.api.dropin.R.id.bt_payment_method_type)).check(matches(withText(
                com.braintreepayments.api.dropin.R.string.bt_descriptor_visa)));

        onView(withId(com.braintreepayments.api.dropin.R.id.bt_selected_payment_method_view)).perform(click());
        onView(withText(com.braintreepayments.api.dropin.R.string.bt_descriptor_amex)).perform(
                click());
        assertSelectedPaymentMethodIs(com.braintreepayments.api.dropin.R.string.bt_descriptor_amex);

        rotateToLandscape();
        waitForPaymentMethodNonceList();
        assertSelectedPaymentMethodIs(com.braintreepayments.api.dropin.R.string.bt_descriptor_amex);
    }

    @Test(timeout = 30000)
    public void doesNotReloadPaymentMethodsOnRotate() {
        Intent intent = new PaymentRequest()
                .clientToken(new TestClientTokenBuilder().build())
                .getIntent(getTargetContext())
                .putExtra(BraintreePaymentTestActivity.GET_PAYMENT_METHODS,
                        stringFromFixture("responses/get_payment_methods_two_cards_response.json"));
        BraintreePaymentActivity activity = getActivity(intent);

        waitForPaymentMethodNonceList();
        verify(activity.mBraintreeFragment.getHttpClient() , times(1))
                .get(startsWith(BraintreePaymentTestActivity.GET_PAYMENT_METHODS), any(HttpResponseCallback.class));
        rotateToLandscape();

        waitForPaymentMethodNonceList();
        verifyNoMoreInteractions(activity.mBraintreeFragment.getHttpClient());
    }

    @Test(timeout = 30000)
    public void whenRotatingDeviceWhileLoadingSendsEventToNewActivity() {
        getActivity(new TestClientTokenBuilder().build(), TWO_SECONDS);

        rotateToLandscape();

        waitForAddPaymentFormHeader();
    }

    @Test(timeout = 30000)
    public void cardFieldsStillDisabledDuringSubmitOnRotation() {
        getActivity(new TestClientTokenBuilder().build(), FOUR_SECONDS);

        waitForAddPaymentFormHeader(FOUR_SECONDS * 4);
        fillInCardForm();
        onView(withId(com.braintreepayments.api.dropin.R.id.bt_card_form_submit_button)).perform(
                click());

        onView(withId(com.braintreepayments.api.dropin.R.id.bt_card_form_card_number)).check(
                matches(not(isEnabled())));
        rotateToLandscape();
        onView(withId(com.braintreepayments.api.dropin.R.id.bt_card_form_card_number)).check(matches(not(isEnabled())));
    }

    @Test(timeout = 30000)
    public void submitButtonIsDisabledDuringSubmitOnRotate() {
        getActivity(new TestClientTokenBuilder().build(), TWO_SECONDS);

        waitForAddPaymentFormHeader();
        rotateToLandscape();
        fillInCardForm();
        onView(withId(com.braintreepayments.api.dropin.R.id.bt_card_form_submit_button)).perform(
                click());

        rotateToPortrait();
        onView(withId(com.braintreepayments.api.dropin.R.id.bt_card_form_submit_button)).check(
                matches(not(isEnabled())));
    }

    @Test(timeout = 30000)
    public void submittingStateIsPersistedAcrossRotations() {
        getActivity(new TestClientTokenBuilder().build(), TWO_SECONDS);

        waitForAddPaymentFormHeader();
        fillInCardForm();
        onView(withId(com.braintreepayments.api.dropin.R.id.bt_card_form_submit_button)).perform(
                click());

        waitForView(withId(com.braintreepayments.api.dropin.R.id.bt_header_container));
        onView(withId(com.braintreepayments.api.dropin.R.id.bt_header_loading_spinner)).check(matches(isDisplayed()));

        rotateToLandscape();

        onView(withId(com.braintreepayments.api.dropin.R.id.bt_card_form_submit_button)).check(
                matches(not(isEnabled())));
        onView(withId(com.braintreepayments.api.dropin.R.id.bt_header_loading_spinner)).check(matches(isDisplayed()));
    }

    @Test(timeout = 30000)
    public void submitButtonIsBlueAfterRotationIfFieldsAreValid() {
        getActivity(new TestClientTokenBuilder().build());
        fillInCardForm();

        onView(withId(com.braintreepayments.api.dropin.R.id.bt_card_form_submit_button)).check(
                matches(hasBackgroundResource(com.braintreepayments.api.dropin.R.drawable.bt_submit_button_background)));
        rotateToLandscape();
        waitForAddPaymentFormHeader();
        onView(withId(com.braintreepayments.api.dropin.R.id.bt_card_form_submit_button)).check(
                matches(hasBackgroundResource(com.braintreepayments.api.dropin.R.drawable.bt_submit_button_background)));
    }

    @Test(timeout = 30000)
    public void cardFormCreatesAPaymentMethodInLandscape() {
        rotateToLandscape();
        BraintreePaymentActivity activity = getActivity(new TestClientTokenBuilder().build());

        addCardAndAssertSuccess(activity);
    }
}
