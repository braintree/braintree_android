package com.braintreepayments.api.dropin;

import android.content.Context;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.SystemClock;

import com.braintreepayments.api.Braintree;
import com.braintreepayments.api.BraintreeApi;
import com.braintreepayments.api.BraintreeTestUtils;
import com.braintreepayments.api.TestClientTokenBuilder;
import com.braintreepayments.api.exceptions.BraintreeException;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.models.CardBuilder;

import static com.braintreepayments.api.BraintreeTestUtils.assertSelectedPaymentMethodIs;
import static com.braintreepayments.api.BraintreeTestUtils.injectBraintree;
import static com.braintreepayments.api.BraintreeTestUtils.injectSlowBraintree;
import static com.braintreepayments.api.ui.Matchers.hasBackgroundResource;
import static com.braintreepayments.api.ui.Matchers.withId;
import static com.braintreepayments.api.ui.RotationHelper.rotateToLandscape;
import static com.braintreepayments.api.ui.RotationHelper.rotateToPortrait;
import static com.braintreepayments.api.ui.ViewHelper.FOUR_SECONDS;
import static com.braintreepayments.api.ui.ViewHelper.THREE_SECONDS;
import static com.braintreepayments.api.ui.ViewHelper.TWO_SECONDS;
import static com.braintreepayments.api.ui.ViewHelper.closeSoftKeyboard;
import static com.braintreepayments.api.ui.ViewHelper.waitForView;
import static com.braintreepayments.api.utils.PaymentFormHelpers.onCardField;
import static com.braintreepayments.api.utils.PaymentFormHelpers.onCvvField;
import static com.braintreepayments.api.utils.PaymentFormHelpers.onExpirationField;
import static com.braintreepayments.api.utils.PaymentFormHelpers.onPostalCodeField;
import static com.braintreepayments.api.utils.PaymentFormHelpers.waitForAddPaymentFormHeader;
import static com.braintreepayments.api.utils.PaymentFormHelpers.waitForPaymentMethodList;
import static com.google.android.apps.common.testing.ui.espresso.Espresso.onView;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.click;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.typeText;
import static com.google.android.apps.common.testing.ui.espresso.assertion.ViewAssertions.matches;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.isDisplayed;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.isEnabled;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ActivityRotationTest extends BraintreePaymentActivityTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN_MR2) {
            rotateToPortrait(this);
        }
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN_MR2) {
            rotateToPortrait(this);
        }
    }

    public void testAddPaymentViewIsRestoredOnRotation() {
        if (VERSION.SDK_INT < VERSION_CODES.JELLY_BEAN_MR2) {
            return;
        }

        BraintreeTestUtils.setUpActivityTest(this);

        getActivity();
        waitForAddPaymentFormHeader();
        onCardField().perform(typeText("378282246310005"));
        onExpirationField().perform(typeText("12/18"), closeSoftKeyboard());
        onCvvField().perform(typeText("1234"), closeSoftKeyboard());
        onPostalCodeField().perform(typeText("12345"), closeSoftKeyboard());
        onView(withId(R.id.bt_card_form_submit_button)).check(matches(isEnabled()));

        rotateToLandscape(this);
        waitForAddPaymentFormHeader();
        onCardField().check(matches(withText("378282246310005")));
        onExpirationField().check(matches(withText("12/18")));
        onCvvField().check(matches(withText("1234")));
        onPostalCodeField().check(matches(withText("12345")));
        onView(withId(R.id.bt_card_form_submit_button)).check(matches(isEnabled()));
    }

    public void testSelectPaymentViewIsRestoredOnRotation()
            throws InterruptedException, ErrorWithResponse, BraintreeException {
        if (VERSION.SDK_INT < VERSION_CODES.JELLY_BEAN_MR2) {
            return;
        }
        String clientToken = BraintreeTestUtils.setUpActivityTest(this);
        BraintreeApi api = new BraintreeApi(getContext(), clientToken);
        api.create(new CardBuilder()
                .cardNumber("4111111111111111")
                .expirationMonth("02")
                .expirationYear("18"));

        SystemClock.sleep(1000);

        api.create(new CardBuilder()
                .cardNumber("378282246310005")
                .expirationMonth("02")
                .expirationYear("18"));

        getActivity();

        waitForPaymentMethodList();
        onView(withId(R.id.bt_payment_method_type)).check(matches(withText(R.string.bt_descriptor_amex)));

        onView(withId(R.id.bt_selected_payment_method_view)).perform(click());
        onView(withText(R.string.bt_descriptor_visa)).perform(click());
        assertSelectedPaymentMethodIs(R.string.bt_descriptor_visa);

        rotateToLandscape(this);
        waitForPaymentMethodList();
        assertSelectedPaymentMethodIs(R.string.bt_descriptor_visa);
    }

    public void testDoesntReloadPaymentMethodsOnRotate() {
        if (VERSION.SDK_INT < VERSION_CODES.JELLY_BEAN_MR2) {
            return;
        }

        String clientToken = new TestClientTokenBuilder().build();
        Braintree braintree = mock(Braintree.class);
        when(braintree.hasCachedCards()).thenReturn(false).thenReturn(true);
        injectBraintree(clientToken, braintree);
        BraintreeTestUtils.setUpActivityTest(this, clientToken);

        getActivity();
        waitForAddPaymentFormHeader();
        verify(braintree, times(1)).getPaymentMethods();
        rotateToLandscape(this);

        waitForAddPaymentFormHeader();
        verify(braintree, times(1)).getPaymentMethods();
    }

    public void testWhenRotatingDeviceWhileLoadingSendsEventToNewActivity() {
        if (VERSION.SDK_INT < VERSION_CODES.JELLY_BEAN_MR2) {
            return;
        }

        String clientToken = new TestClientTokenBuilder().build();
        injectSlowBraintree(getContext(), clientToken, TWO_SECONDS);
        BraintreeTestUtils.setUpActivityTest(this, clientToken);

        getActivity();
        rotateToLandscape(this);
        waitForAddPaymentFormHeader();
    }

    public void testCardFieldsStillDisabledDuringSubmitOnRotation() {
        if (VERSION.SDK_INT < VERSION_CODES.JELLY_BEAN_MR2) {
            return;
        }

        int timeout = FOUR_SECONDS;
        String clientToken = new TestClientTokenBuilder().build();
        injectSlowBraintree(getContext(), clientToken, timeout);
        BraintreeTestUtils.setUpActivityTest(this, clientToken);

        getActivity();

        waitForAddPaymentFormHeader(timeout * 4);
        onView(withId(R.id.bt_card_form_card_number)).perform(typeText("4111111111111111"));
        onView(withId(R.id.bt_card_form_expiration)).perform(typeText("0119"), closeSoftKeyboard());
        onView(withId(R.id.bt_card_form_cvv)).perform(typeText("123"), closeSoftKeyboard());
        onView(withId(R.id.bt_card_form_postal_code)).perform(typeText("12345"), closeSoftKeyboard());
        onView(withId(R.id.bt_card_form_submit_button)).perform(click());

        onView(withId(R.id.bt_card_form_card_number)).check(matches(not(isEnabled())));
        rotateToLandscape(this);
        onView(withId(R.id.bt_card_form_card_number)).check(matches(not(isEnabled())));
    }

    public void testSubmitButtonIsDisabledDuringSubmitOnRotate() {
        if (VERSION.SDK_INT < VERSION_CODES.JELLY_BEAN_MR2) {
            return;
        }

        String clientToken = new TestClientTokenBuilder().build();
        injectSlowBraintree(getContext(), clientToken, THREE_SECONDS);
        BraintreeTestUtils.setUpActivityTest(this, clientToken);

        getActivity();
        waitForAddPaymentFormHeader();
        rotateToLandscape(this);
        onCardField().perform(typeText("378282246310005"), closeSoftKeyboard());
        onExpirationField().perform(typeText("12/18"), closeSoftKeyboard());
        onCvvField().perform(typeText("1234"), closeSoftKeyboard());
        onPostalCodeField().perform(typeText("12345"), closeSoftKeyboard());
        onView(withId(R.id.bt_card_form_submit_button)).perform(click());
        rotateToPortrait(this);

        onView(withId(R.id.bt_card_form_submit_button)).check(matches(not(isEnabled())));
    }

    public void testSubmittingStateIsPersistedAcrossRotations() {
        if (VERSION.SDK_INT < VERSION_CODES.JELLY_BEAN_MR2) {
            return;
        }

        String clientToken = new TestClientTokenBuilder().build();
        injectSlowBraintree(getContext(), clientToken, THREE_SECONDS);
        BraintreeTestUtils.setUpActivityTest(this, clientToken);

        getActivity();
        waitForAddPaymentFormHeader();
        onCardField().perform(typeText("378282246310005"), closeSoftKeyboard());
        onExpirationField().perform(typeText("12/18"), closeSoftKeyboard());
        onCvvField().perform(typeText("1234"), closeSoftKeyboard());
        onPostalCodeField().perform(typeText("12345"), closeSoftKeyboard());
        onView(withId(R.id.bt_card_form_submit_button)).perform(click());

        waitForView(withId(R.id.bt_header_container));
        onView(withId(R.id.bt_header_loading_spinner)).check(matches(isDisplayed()));

        rotateToLandscape(this);

        onView(withId(R.id.bt_card_form_submit_button)).check(matches(not(isEnabled())));
        onView(withId(R.id.bt_header_loading_spinner)).check(matches(isDisplayed()));
    }

    public void testSubmitButtonIsBlueAfterRotationIfFieldsAreValid() {
        if (VERSION.SDK_INT < VERSION_CODES.JELLY_BEAN_MR2) {
            return;
        }

        BraintreeTestUtils.setUpActivityTest(this);
        getActivity();
        waitForAddPaymentFormHeader();
        onCardField().perform(typeText("378282246310005"), closeSoftKeyboard());
        onExpirationField().perform(typeText("12/18"), closeSoftKeyboard());
        onCvvField().perform(typeText("1234"), closeSoftKeyboard());
        onPostalCodeField().perform(typeText("12345"), closeSoftKeyboard());


        onView(withId(R.id.bt_card_form_submit_button)).check(
                matches(hasBackgroundResource(getContext(), R.drawable.bt_submit_button_background)));
        rotateToLandscape(this);
        waitForAddPaymentFormHeader();
        onView(withId(R.id.bt_card_form_submit_button)).check(
                matches(hasBackgroundResource(getContext(), R.drawable.bt_submit_button_background)));
    }

    private Context getContext() {
        return getInstrumentation().getContext();
    }

}
