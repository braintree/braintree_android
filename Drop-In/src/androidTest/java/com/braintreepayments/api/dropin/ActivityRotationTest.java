package com.braintreepayments.api.dropin;

import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.SystemClock;

import com.braintreepayments.api.Braintree;
import com.braintreepayments.api.BraintreeApi;
import com.braintreepayments.api.exceptions.BraintreeException;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.models.CardBuilder;
import com.braintreepayments.testutils.TestClientTokenBuilder;

import org.json.JSONException;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isEnabled;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.braintreepayments.api.BraintreeTestUtils.setClientTokenExtraForTest;
import static com.braintreepayments.api.BraintreeTestUtils.setUpActivityTest;
import static com.braintreepayments.api.TestDependencyInjector.injectBraintree;
import static com.braintreepayments.api.TestDependencyInjector.injectSlowBraintree;
import static com.braintreepayments.api.utils.Assertions.assertSelectedPaymentMethodIs;
import static com.braintreepayments.api.utils.PaymentFormHelpers.addCardAndAssertSuccess;
import static com.braintreepayments.api.utils.PaymentFormHelpers.onCardField;
import static com.braintreepayments.api.utils.PaymentFormHelpers.onCvvField;
import static com.braintreepayments.api.utils.PaymentFormHelpers.onExpirationField;
import static com.braintreepayments.api.utils.PaymentFormHelpers.onPostalCodeField;
import static com.braintreepayments.api.utils.PaymentFormHelpers.waitForAddPaymentFormHeader;
import static com.braintreepayments.api.utils.PaymentFormHelpers.waitForPaymentMethodList;
import static com.braintreepayments.testutils.CardNumber.AMEX;
import static com.braintreepayments.testutils.CardNumber.VISA;
import static com.braintreepayments.testutils.ui.Matchers.hasBackgroundResource;
import static com.braintreepayments.testutils.ui.Matchers.withId;
import static com.braintreepayments.testutils.ui.RotationHelper.rotateToLandscape;
import static com.braintreepayments.testutils.ui.RotationHelper.rotateToPortrait;
import static com.braintreepayments.testutils.ui.ViewHelper.FOUR_SECONDS;
import static com.braintreepayments.testutils.ui.ViewHelper.THREE_SECONDS;
import static com.braintreepayments.testutils.ui.ViewHelper.TWO_SECONDS;
import static com.braintreepayments.testutils.ui.ViewHelper.closeSoftKeyboard;
import static com.braintreepayments.testutils.ui.ViewHelper.waitForView;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.spy;
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

    public void testAddPaymentViewIsRestoredOnRotation() throws JSONException {
        if (VERSION.SDK_INT < VERSION_CODES.JELLY_BEAN_MR2) {
            return;
        }

        setUpActivityTest(this, new TestClientTokenBuilder().build());

        getActivity();
        waitForAddPaymentFormHeader();
        onCardField().perform(typeText(AMEX));
        onExpirationField().perform(typeText("1218"), closeSoftKeyboard());
        onCvvField().perform(typeText("1234"), closeSoftKeyboard());
        onPostalCodeField().perform(typeText("12345"), closeSoftKeyboard());
        onView(withId(R.id.bt_card_form_submit_button)).check(matches(isEnabled()));

        rotateToLandscape(this);
        waitForAddPaymentFormHeader();
        onCardField().check(matches(withText(AMEX)));
        onExpirationField().check(matches(withText("1218")));
        onCvvField().check(matches(withText("1234")));
        onPostalCodeField().check(matches(withText("12345")));
        onView(withId(R.id.bt_card_form_submit_button)).check(matches(isEnabled()));
    }

    public void testAddPaymentViewIsResumedOnRotationWhenThereAreExistingPaymentMethods()
            throws ErrorWithResponse, BraintreeException, JSONException {
        if (VERSION.SDK_INT < VERSION_CODES.JELLY_BEAN_MR2) {
            return;
        }

        String clientToken = new TestClientTokenBuilder().withPayPal().build();
        setUpActivityTest(this, clientToken);
        BraintreeApi api = new BraintreeApi(mContext, clientToken);
        api.create(new CardBuilder()
                .cardNumber(VISA)
                .expirationMonth("02")
                .expirationYear("18"));

        getActivity();
        waitForPaymentMethodList();
        onView(withId(R.id.bt_change_payment_method_link)).perform(click());
        waitForAddPaymentFormHeader();
        onCardField().perform(typeText(AMEX), closeSoftKeyboard());

        rotateToLandscape(this);

        waitForAddPaymentFormHeader();
        onCardField().check(matches(withText(AMEX)));
    }

    public void testSelectPaymentViewIsRestoredOnRotation()
            throws InterruptedException, ErrorWithResponse, BraintreeException, JSONException {
        if (VERSION.SDK_INT < VERSION_CODES.JELLY_BEAN_MR2) {
            return;
        }
        String clientToken = new TestClientTokenBuilder().withPayPal().build();
        setUpActivityTest(this, clientToken);
        BraintreeApi api = new BraintreeApi(mContext, clientToken);
        api.create(new CardBuilder()
                .cardNumber(VISA)
                .expirationMonth("02")
                .expirationYear("18"));

        SystemClock.sleep(1000);

        api.create(new CardBuilder()
                .cardNumber(AMEX)
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

    public void testDoesntReloadPaymentMethodsOnRotate() throws JSONException {
        if (VERSION.SDK_INT < VERSION_CODES.JELLY_BEAN_MR2) {
            return;
        }

        String clientToken = new TestClientTokenBuilder().build();
        Braintree braintree = spy(injectBraintree(mContext, clientToken, clientToken));
        when(braintree.hasCachedCards()).thenReturn(false).thenReturn(true);
        injectBraintree(clientToken, braintree);
        setClientTokenExtraForTest(this, clientToken);

        getActivity();
        waitForAddPaymentFormHeader();
        verify(braintree, times(1)).getPaymentMethods();
        rotateToLandscape(this);

        waitForAddPaymentFormHeader();
        verify(braintree, times(1)).getPaymentMethods();
    }

    public void testWhenRotatingDeviceWhileLoadingSendsEventToNewActivity() throws JSONException {
        if (VERSION.SDK_INT < VERSION_CODES.JELLY_BEAN_MR2) {
            return;
        }

        String clientToken = new TestClientTokenBuilder().build();
        injectSlowBraintree(mContext, clientToken, TWO_SECONDS);
        setUpActivityTest(this, clientToken);

        getActivity();
        rotateToLandscape(this);
        waitForAddPaymentFormHeader();
    }

    public void testCardFieldsStillDisabledDuringSubmitOnRotation() throws JSONException {
        if (VERSION.SDK_INT < VERSION_CODES.JELLY_BEAN_MR2) {
            return;
        }

        int timeout = FOUR_SECONDS;
        String clientToken = new TestClientTokenBuilder().build();
        injectSlowBraintree(mContext, clientToken, timeout);
        setClientTokenExtraForTest(this, clientToken);
        getActivity();

        waitForAddPaymentFormHeader(timeout * 4);
        onView(withId(R.id.bt_card_form_card_number)).perform(typeText(VISA));
        onView(withId(R.id.bt_card_form_expiration)).perform(typeText("0119"), closeSoftKeyboard());
        onView(withId(R.id.bt_card_form_cvv)).perform(typeText("123"), closeSoftKeyboard());
        onView(withId(R.id.bt_card_form_postal_code)).perform(typeText("12345"), closeSoftKeyboard());
        onView(withId(R.id.bt_card_form_submit_button)).perform(click());

        onView(withId(R.id.bt_card_form_card_number)).check(matches(not(isEnabled())));
        rotateToLandscape(this);
        onView(withId(R.id.bt_card_form_card_number)).check(matches(not(isEnabled())));
    }

    public void testSubmitButtonIsDisabledDuringSubmitOnRotate() throws JSONException {
        if (VERSION.SDK_INT < VERSION_CODES.JELLY_BEAN_MR2) {
            return;
        }

        String clientToken = new TestClientTokenBuilder().build();
        injectSlowBraintree(mContext, clientToken, THREE_SECONDS);
        setClientTokenExtraForTest(this, clientToken);
        getActivity();

        waitForAddPaymentFormHeader();
        rotateToLandscape(this);
        onCardField().perform(typeText(AMEX), closeSoftKeyboard());
        onExpirationField().perform(typeText("1218"), closeSoftKeyboard());
        onCvvField().perform(typeText("1234"), closeSoftKeyboard());
        onPostalCodeField().perform(typeText("12345"), closeSoftKeyboard());
        onView(withId(R.id.bt_card_form_submit_button)).perform(click());
        rotateToPortrait(this);

        onView(withId(R.id.bt_card_form_submit_button)).check(matches(not(isEnabled())));
    }

    public void testSubmittingStateIsPersistedAcrossRotations() throws JSONException {
        if (VERSION.SDK_INT < VERSION_CODES.JELLY_BEAN_MR2) {
            return;
        }

        String clientToken = new TestClientTokenBuilder().build();
        injectSlowBraintree(mContext, clientToken, THREE_SECONDS);
        setClientTokenExtraForTest(this, clientToken);

        getActivity();
        waitForAddPaymentFormHeader();
        onCardField().perform(typeText(AMEX), closeSoftKeyboard());
        onExpirationField().perform(typeText("1218"), closeSoftKeyboard());
        onCvvField().perform(typeText("1234"), closeSoftKeyboard());
        onPostalCodeField().perform(typeText("12345"), closeSoftKeyboard());
        onView(withId(R.id.bt_card_form_submit_button)).perform(click());

        waitForView(withId(R.id.bt_header_container));
        onView(withId(R.id.bt_header_loading_spinner)).check(matches(isDisplayed()));

        rotateToLandscape(this);

        onView(withId(R.id.bt_card_form_submit_button)).check(matches(not(isEnabled())));
        onView(withId(R.id.bt_header_loading_spinner)).check(matches(isDisplayed()));
    }

    public void testSubmitButtonIsBlueAfterRotationIfFieldsAreValid() throws JSONException {
        if (VERSION.SDK_INT < VERSION_CODES.JELLY_BEAN_MR2) {
            return;
        }

        setUpActivityTest(this, new TestClientTokenBuilder().build());
        getActivity();
        waitForAddPaymentFormHeader();
        onCardField().perform(typeText(AMEX), closeSoftKeyboard());
        onExpirationField().perform(typeText("1218"), closeSoftKeyboard());
        onCvvField().perform(typeText("1234"), closeSoftKeyboard());
        onPostalCodeField().perform(typeText("12345"), closeSoftKeyboard());


        onView(withId(R.id.bt_card_form_submit_button)).check(
                matches(hasBackgroundResource(R.drawable.bt_submit_button_background)));
        rotateToLandscape(this);
        waitForAddPaymentFormHeader();
        onView(withId(R.id.bt_card_form_submit_button)).check(
                matches(hasBackgroundResource(R.drawable.bt_submit_button_background)));
    }

    public void testCardFormCreatesAPaymentMethodInLandscape() throws JSONException {
        if (VERSION.SDK_INT < VERSION_CODES.JELLY_BEAN_MR2) {
            return;
        }

        rotateToLandscape(this);
        setUpActivityTest(this, new TestClientTokenBuilder().build());
        BraintreePaymentActivity activity = getActivity();

        addCardAndAssertSuccess(activity);
    }
}
