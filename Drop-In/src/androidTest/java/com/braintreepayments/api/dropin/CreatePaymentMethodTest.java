package com.braintreepayments.api.dropin;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Build.VERSION_CODES;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.widget.Button;

import com.braintreepayments.api.BraintreeApi;
import com.braintreepayments.api.BraintreeTestUtils;
import com.braintreepayments.api.dropin.view.LoadingHeader;
import com.braintreepayments.api.dropin.view.LoadingHeader.HeaderState;
import com.braintreepayments.api.exceptions.BraintreeException;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.models.Card;
import com.braintreepayments.api.models.PaymentMethod;
import com.braintreepayments.api.models.PaymentMethod.Builder;
import com.braintreepayments.cardform.view.ErrorEditText;
import com.braintreepayments.testutils.FixturesHelper;
import com.braintreepayments.testutils.TestClientTokenBuilder;

import java.util.Map;

import static com.braintreepayments.api.BraintreeTestUtils.assertSelectedPaymentMethodIs;
import static com.braintreepayments.api.BraintreeTestUtils.injectBraintreeApi;
import static com.braintreepayments.api.BraintreeTestUtils.injectGeneric422ErrorOnCardCreateBraintree;
import static com.braintreepayments.api.BraintreeTestUtils.injectSlowBraintree;
import static com.braintreepayments.api.BraintreeTestUtils.setUpActivityTest;
import static com.braintreepayments.api.utils.PaymentFormHelpers.addCardAndAssertSuccess;
import static com.braintreepayments.api.utils.PaymentFormHelpers.fillInOfflinePayPal;
import static com.braintreepayments.api.utils.PaymentFormHelpers.onAddPaymentFormHeader;
import static com.braintreepayments.api.utils.PaymentFormHelpers.performPayPalAdd;
import static com.braintreepayments.api.utils.PaymentFormHelpers.waitForAddPaymentFormHeader;
import static com.braintreepayments.api.utils.PaymentFormHelpers.waitForPaymentMethodList;
import static com.braintreepayments.testutils.ActivityResultHelper.getActivityResult;
import static com.braintreepayments.testutils.CardNumber.VISA;
import static com.braintreepayments.testutils.ui.Matchers.withHint;
import static com.braintreepayments.testutils.ui.Matchers.withId;
import static com.braintreepayments.testutils.ui.ViewHelper.FIFTEEN_SECONDS;
import static com.braintreepayments.testutils.ui.ViewHelper.FIVE_SECONDS;
import static com.braintreepayments.testutils.ui.ViewHelper.TEN_SECONDS;
import static com.braintreepayments.testutils.ui.ViewHelper.THREE_SECONDS;
import static com.braintreepayments.testutils.ui.ViewHelper.TWO_SECONDS;
import static com.braintreepayments.testutils.ui.ViewHelper.closeSoftKeyboard;
import static com.braintreepayments.testutils.ui.ViewHelper.waitForView;
import static com.braintreepayments.testutils.ui.WaitForActivityHelper.waitForActivityToFinish;
import static com.google.android.apps.common.testing.ui.espresso.Espresso.onView;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.click;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.typeText;
import static com.google.android.apps.common.testing.ui.espresso.assertion.ViewAssertions.matches;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.isDisplayed;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.isEnabled;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CreatePaymentMethodTest extends BraintreePaymentActivityTestCase {

    public void testCardFormCreatesAPaymentMethodWithoutACustomer() throws InterruptedException {
        BraintreeTestUtils.setUpActivityTest(this, new TestClientTokenBuilder().withoutCustomer().build());

        addCardAndAssertSuccess(getActivity());
    }

    public void testCardFormCreatesAPaymentMethodWithACustomer() throws InterruptedException {
        BraintreeTestUtils.setUpActivityTest(this);

        addCardAndAssertSuccess(getActivity());
    }

    public void testCardFormCreatesAPaymentMethodWithoutCvvOrPostalCode() {
        String clientToken = new TestClientTokenBuilder()
                .withoutCvvChallenge()
                .withoutPostalCodeChallenge()
                .build();
        BraintreeTestUtils.setUpActivityTest(this, clientToken);

        BraintreePaymentActivity activity = getActivity();

        waitForAddPaymentFormHeader();
        onView(withHint("Card Number")).perform(typeText(VISA));
        onView(withHint("Expiration")).perform(typeText("0619"), closeSoftKeyboard());
        onView(withId(R.id.bt_card_form_cvv)).check(matches(not(isDisplayed())));
        onView(withId(R.id.bt_card_form_postal_code)).check(matches(not(isDisplayed())));
        onView(withId(R.id.bt_card_form_submit_button)).perform(click());

        waitForActivityToFinish(activity);

        Map<String, Object> result = getActivityResult(activity);
        PaymentMethod response =
                (PaymentMethod) ((Intent) result.get("resultData")).getSerializableExtra(
                        BraintreePaymentActivity.EXTRA_PAYMENT_METHOD);

        assertEquals(Activity.RESULT_OK, result.get("resultCode"));
        assertNotNull(response.getNonce());
        assertEquals("11", ((Card) response).getLastTwo());
    }

    public void testPayPalCreatesAPaymentMethodWithACustomer() {
        BraintreeTestUtils.setUpActivityTest(this,
                new TestClientTokenBuilder().withPayPal().build());
        assertCreatePaymentMethodFromPayPal("jane.doe@example.com");
    }

    public void testPayPalCreatesAPaymentMethodWithoutACustomer() {
        BraintreeTestUtils.setUpActivityTest(this,
                new TestClientTokenBuilder().withoutCustomer().withPayPal().build());
        assertCreatePaymentMethodFromPayPal("bt_buyer_us@paypal.com");
    }

    public void testReturnsToSelectPaymentMethodViewAfterAddingAPayPalAccount() {
        BraintreeTestUtils.setUpActivityTest(this);
        getActivity();

        waitForView(withId(R.id.bt_paypal_button)).perform(click());

        waitForView(withHint("Email"));
        onView(withHint("Email")).perform(typeText("bt_buyer_us@paypal.com"));
        onView(withHint("Password")).perform(typeText("11111111"));
        onView(withText("Log In")).perform(click());

        waitForView(withText("Agree"));
        onView(withText("Agree")).perform(click());

        waitForPaymentMethodList(TEN_SECONDS);
        assertSelectedPaymentMethodIs(R.string.bt_descriptor_paypal);
    }

    public void testDisplaysLoadingViewWhileCreatingAPayPalAccount() {
        String clientToken = new TestClientTokenBuilder().withFakePayPal().build();
        injectSlowBraintree(mContext, clientToken, THREE_SECONDS);
        BraintreeTestUtils.setUpActivityTest(this, clientToken);
        getActivity();

        fillInOfflinePayPal();

        waitForView(withId(R.id.bt_inflated_loading_view)).check(matches(isDisplayed()));
        onView(withId(R.id.bt_card_form_header)).check(matches(not(isDisplayed())));
        waitForPaymentMethodList().check(matches(isDisplayed()));
    }

    public void testDisablesSubmitButtonWhileCreatingPaymentMethod() {
        String clientToken = new TestClientTokenBuilder().build();
        injectSlowBraintree(mContext, clientToken, 200);
        BraintreeTestUtils.setUpActivityTest(this, clientToken);
        BraintreePaymentActivity activity = getActivity();

        waitForAddPaymentFormHeader();
        onView(withHint("Card Number")).perform(typeText(VISA));
        onView(withHint("Expiration")).perform(typeText("0619"), closeSoftKeyboard());
        onView(withHint("CVV")).perform(typeText("123"), closeSoftKeyboard());
        onView(withHint("Postal Code")).perform(typeText("12345"));
        onView(withId(R.id.bt_card_form_submit_button)).perform(click());

        Button submitButton = (Button) activity.findViewById(R.id.bt_card_form_submit_button);
        assertFalse(submitButton.isEnabled());

        onView(withId(R.id.bt_card_form_card_number)).perform(click());
        assertFalse(submitButton.isEnabled());

        waitForActivityToFinish(activity);

        Map<String, Object> result = getActivityResult(activity);
        PaymentMethod response =
                (PaymentMethod) ((Intent) result.get("resultData")).getSerializableExtra(
                        BraintreePaymentActivity.EXTRA_PAYMENT_METHOD);

        assertEquals(Activity.RESULT_OK, result.get("resultCode"));
        assertNotNull(response.getNonce());
        assertEquals("11", ((Card) response).getLastTwo());
    }

    public void testReEnablesSubmitButtonIfThereAreValidationErrorsForThePaymentMethod() {
        String clientToken = new TestClientTokenBuilder().withoutPostalCodeChallenge().withCvvVerification().build();
        injectSlowBraintree(mContext, clientToken, THREE_SECONDS);
        BraintreeTestUtils.setUpActivityTest(this, clientToken);
        getActivity();

        waitForAddPaymentFormHeader();

        onView(withId(R.id.bt_card_form_card_number)).perform(typeText(VISA));
        onView(withId(R.id.bt_card_form_expiration)).perform(typeText("1219"), closeSoftKeyboard());
        onView(withId(R.id.bt_card_form_cvv)).perform(typeText("200"));
        onView(withId(R.id.bt_card_form_submit_button)).perform(click());

        onView(withId(R.id.bt_card_form_submit_button)).check(matches(not(isEnabled())));
        waitForView(withId(R.id.bt_card_form_submit_button), isEnabled());
        onView(withId(R.id.bt_card_form_submit_button)).check(matches(isEnabled()));
    }

    public void testPayPalButtonIsNotShownIfPayPalIsNotSupported() {
        BraintreeTestUtils.setUpActivityTest(this, new TestClientTokenBuilder().build());
        getActivity();

        waitForAddPaymentFormHeader();
        SystemClock.sleep(50);
        onView(withId(R.id.bt_paypal_button)).check(matches(not(isDisplayed())));
    }

    public void testBackButtonExitsTheActivityIfThereAreNoPaymentMethodsToSelectFrom() {
        BraintreeTestUtils.setUpActivityTest(this);
        Activity activity = getActivity();

        waitForAddPaymentFormHeader();
        onAddPaymentFormHeader().check(matches(isDisplayed()));
        assertFalse(activity.isFinishing());
        sendKeys(KeyEvent.KEYCODE_BACK);
        assertTrue(activity.isFinishing());
    }

    public void testBackButtonInPayPalTakesYouBackToAddPaymentMethodView() {
        BraintreeTestUtils.setUpActivityTest(this);
        getActivity();

        waitForView(withId(R.id.bt_paypal_button)).perform(click());
        waitForView(withHint("Email")).check(matches(isDisplayed())).perform(closeSoftKeyboard());
        sendKeys(KeyEvent.KEYCODE_BACK);

        waitForAddPaymentFormHeader().check(matches(isDisplayed()));
    }

    public void testBackButtonDuringCreditCardAddDoesNothing() {
        String clientToken = new TestClientTokenBuilder().build();
        injectSlowBraintree(mContext, clientToken, TWO_SECONDS);
        BraintreeTestUtils.setUpActivityTest(this, clientToken);
        getActivity();

        waitForAddPaymentFormHeader();
        onView(withHint("Card Number")).perform(typeText(VISA));
        onView(withHint("Expiration")).perform(typeText("0619"), closeSoftKeyboard());
        onView(withHint("CVV")).perform(typeText("123"), closeSoftKeyboard());
        onView(withHint("Postal Code")).perform(typeText("12345"));
        onView(withId(R.id.bt_card_form_submit_button)).perform(click());

        onView(withId(R.id.bt_header_container)).check(matches(isDisplayed()));
        sendKeys(KeyEvent.KEYCODE_BACK);
        onView(withId(R.id.bt_header_container)).check(matches(isDisplayed()));
    }

    public void testBackButtonDuringPayPalAddDoesNothing() {
        String clientToken = new TestClientTokenBuilder().withFakePayPal().build();
        injectSlowBraintree(mContext, clientToken, FIVE_SECONDS);
        BraintreeTestUtils.setUpActivityTest(this, clientToken);
        getActivity();

        fillInOfflinePayPal();

        waitForView(withId(R.id.bt_inflated_loading_view)).check(matches(isDisplayed()));
        sendKeys(KeyEvent.KEYCODE_BACK);
        onView(withId(R.id.bt_inflated_loading_view)).check(matches(isDisplayed()));
    }

    public void testUpButtonIsNotShownIfThereAreNoPaymentMethods() {
        BraintreeTestUtils.setUpActivityTest(this);
        BraintreePaymentActivity activity = getActivity();

        waitForAddPaymentFormHeader();

        assertFalse("Expected up not to be present on action bar", checkHomeAsUpEnabled(activity));
    }

    public void testUpButtonIsShownAfterYouAddAPaymentMethod() {
        BraintreeTestUtils.setUpActivityTest(this);
        BraintreePaymentActivity activity = getActivity();

        performPayPalAdd();

        assertFalse("Expected up not to be present on action bar", checkHomeAsUpEnabled(activity));
        onView(withId(R.id.bt_change_payment_method_link)).perform(click());

        assertTrue("Expected up to be present on action bar", checkHomeAsUpEnabled(activity));
    }

    public void testDisplaysAnErrorWhenCardNumberFailsOnServer()
            throws BraintreeException, ErrorWithResponse {
        String clientToken = new TestClientTokenBuilder().build();
        BraintreeApi expirationFailBraintreeApi = mock(BraintreeApi.class);
        when(expirationFailBraintreeApi.create((Builder) anyObject())).thenThrow(
                new ErrorWithResponse(422,
                        FixturesHelper.stringFromFixture(mContext, "errors/card_number_error_response.json")));
        injectBraintreeApi(clientToken, expirationFailBraintreeApi);

        BraintreeTestUtils.setUpActivityTest(this, clientToken);
        BraintreePaymentActivity activity = getActivity();

        waitForAddPaymentFormHeader();
        onView(withId(R.id.bt_card_form_card_number)).perform(typeText(VISA));
        onView(withId(R.id.bt_card_form_expiration)).perform(typeText("0719"));

        onView(withId(R.id.bt_card_form_submit_button)).perform(click());

        waitForView(withId(R.id.bt_card_form_submit_button), isEnabled());

        ErrorEditText editText = (ErrorEditText) activity.findViewById(R.id.bt_card_form_card_number);
        assertTrue(editText.isError());
        assertTrue(editText.isFocused());

        LoadingHeader loadingHeader = (LoadingHeader) activity.findViewById(R.id.bt_header_container);
        assertEquals(HeaderState.ERROR, loadingHeader.getCurrentState());
        onView(withId(R.id.bt_header_container)).check(matches(isDisplayed()));
    }

    public void testDisplaysAnErrorWhenExpirationFailsOnServer()
            throws ErrorWithResponse, BraintreeException {
        String clientToken = new TestClientTokenBuilder().build();
        BraintreeApi expirationFailBraintreeApi = mock(BraintreeApi.class);
        when(expirationFailBraintreeApi.create((Builder) anyObject())).thenThrow(
                new ErrorWithResponse(422,
                        FixturesHelper.stringFromFixture(mContext,
                                "errors/expiration_date_error_response.json")));
        injectBraintreeApi(clientToken, expirationFailBraintreeApi);

        BraintreeTestUtils.setUpActivityTest(this, clientToken);
        BraintreePaymentActivity activity = getActivity();

        waitForView(withId(R.id.bt_card_form_card_number)).perform(typeText(VISA));
        onView(withId(R.id.bt_card_form_expiration)).perform(typeText("0815"), closeSoftKeyboard());
        onView(withId(R.id.bt_card_form_submit_button)).perform(click());

        waitForView(withId(R.id.bt_card_form_submit_button), isEnabled());

        ErrorEditText editText = (ErrorEditText) activity.findViewById(R.id.bt_card_form_expiration);
        assertTrue(editText.isError());
        assertTrue(editText.isFocused());

        LoadingHeader loadingHeader = (LoadingHeader) activity.findViewById(R.id.bt_header_container);
        assertEquals(HeaderState.ERROR, loadingHeader.getCurrentState());
        onView(withId(R.id.bt_header_container)).check(matches(isDisplayed()));
    }

    public void testDisplaysAnErrorWhenPostalCodeFailsOnServer() {
        BraintreeTestUtils.setUpActivityTest(this,
                new TestClientTokenBuilder().withPostalCodeVerification().build());
        BraintreePaymentActivity activity = getActivity();

        waitForView(withId(R.id.bt_card_form_card_number)).perform(typeText(VISA));
        onView(withId(R.id.bt_card_form_expiration)).perform(typeText("0815"), closeSoftKeyboard());
        onView(withId(R.id.bt_card_form_cvv)).perform(typeText("123"), closeSoftKeyboard());
        onView(withId(R.id.bt_card_form_postal_code)).perform(typeText("20000"));
        onView(withId(R.id.bt_card_form_submit_button)).perform(click());

        waitForView(withId(R.id.bt_card_form_submit_button), isEnabled());

        ErrorEditText editText = (ErrorEditText) activity.findViewById(R.id.bt_card_form_postal_code);
        assertTrue(editText.isError());
        assertTrue(editText.isFocused());

        LoadingHeader loadingHeader = (LoadingHeader) activity.findViewById(R.id.bt_header_container);
        assertEquals(HeaderState.ERROR, loadingHeader.getCurrentState());
        onView(withId(R.id.bt_header_container)).check(matches(isDisplayed()));
    }

    public void testReturnsUnexpectedErrorWhenServerReturnsNonCreditCardError() {
        String clientToken = new TestClientTokenBuilder().build();
        injectGeneric422ErrorOnCardCreateBraintree(mContext, clientToken);
        setUpActivityTest(this, clientToken);
        BraintreePaymentActivity activity = getActivity();

        waitForView(withId(R.id.bt_card_form_card_number)).perform(typeText(VISA));
        onView(withHint("Expiration")).perform(typeText("0619"), closeSoftKeyboard());
        onView(withHint("CVV")) .perform(typeText("123"), closeSoftKeyboard());
        onView(withHint("Postal Code")).perform(typeText("12345"));

        onView(withId(R.id.bt_card_form_submit_button)).perform(click());
        waitForActivityToFinish(activity);

        Map<String, Object> result = getActivityResult(activity);
        assertEquals(BraintreePaymentActivity.BRAINTREE_RESULT_SERVER_ERROR,
                result.get("resultCode"));
    }

    public void testErrorIsShownWhenCvvDoesNotMatchForCvvVerificationMerchants() {
        BraintreeTestUtils
                .setUpActivityTest(this, new TestClientTokenBuilder().withCvvVerification().build());

        BraintreePaymentActivity activity = getActivity();
        waitForAddPaymentFormHeader();

        onView(withHint("Card Number")).perform(typeText(VISA));
        onView(withHint("Expiration")).perform(typeText("0619"), closeSoftKeyboard());
        onView(withHint("CVV")) .perform(typeText("200"), closeSoftKeyboard());
        onView(withHint("Postal Code")).perform(typeText("20000"));

        onView(withId(R.id.bt_card_form_submit_button)).perform(click());
        waitForView(withId(R.id.bt_header_message));

        ErrorEditText editText = (ErrorEditText) activity.findViewById(R.id.bt_card_form_cvv);
        assertTrue(editText.isError());
        assertTrue(editText.isFocused());

        LoadingHeader loadingHeader = (LoadingHeader) activity.findViewById(R.id.bt_header_container);
        assertEquals(HeaderState.ERROR, loadingHeader.getCurrentState());
        onView(withId(R.id.bt_header_container)).check(matches(isDisplayed()));
    }

    public void testErrorIsShownWhenPostalCodeDoesNotMatchForPostalCodeVerificationMerchants() {
        BraintreeTestUtils.setUpActivityTest(this,
                new TestClientTokenBuilder().withPostalCodeVerification().build());

        BraintreePaymentActivity activity = getActivity();
        waitForAddPaymentFormHeader();

        onView(withHint("Card Number")).perform(typeText(VISA));
        onView(withHint("Expiration")).perform(typeText("0619"), closeSoftKeyboard());
        onView(withHint("CVV")) .perform(typeText("200"), closeSoftKeyboard());
        onView(withHint("Postal Code")).perform(typeText("20000"));

        onView(withId(R.id.bt_card_form_submit_button)).perform(click());
        waitForView(withId(R.id.bt_header_message));

        ErrorEditText editText = (ErrorEditText) activity.findViewById(R.id.bt_card_form_postal_code);
        assertTrue(editText.isError());
        assertTrue(editText.isFocused());

        LoadingHeader loadingHeader = (LoadingHeader) activity.findViewById(R.id.bt_header_container);
        assertEquals(HeaderState.ERROR, loadingHeader.getCurrentState());
        onView(withId(R.id.bt_header_container)).check(matches(isDisplayed()));
    }

    public void testErrorIsShownWhenCvvAndPostalCodeDoesNotMatchForCvvAndPostalCodeVerificationMerchants() {
        BraintreeTestUtils.setUpActivityTest(this,
                new TestClientTokenBuilder().withCvvAndPostalCodeVerification().build());

        BraintreePaymentActivity activity = getActivity();
        waitForAddPaymentFormHeader();

        onView(withHint("Card Number")).perform(typeText(VISA));
        onView(withHint("Expiration")).perform(typeText("0619"), closeSoftKeyboard());
        onView(withHint("CVV")) .perform(typeText("200"), closeSoftKeyboard());
        onView(withHint("Postal Code")).perform(typeText("20000"));

        onView(withId(R.id.bt_card_form_submit_button)).perform(click());
        waitForView(withId(R.id.bt_header_message));

        ErrorEditText cvvCode = (ErrorEditText) activity.findViewById(R.id.bt_card_form_cvv);
        ErrorEditText postalCode = (ErrorEditText) activity.findViewById(R.id.bt_card_form_postal_code);

        assertTrue(cvvCode.isError());
        assertTrue(postalCode.isError());
        assertTrue(cvvCode.isFocused());

        LoadingHeader loadingHeader = (LoadingHeader) activity.findViewById(R.id.bt_header_container);
        assertEquals(HeaderState.ERROR, loadingHeader.getCurrentState());
        onView(withId(R.id.bt_header_container)).check(matches(isDisplayed()));
    }

    public void testIsSuccessfulWhenCvvAndPostalCodeMatchForCvvAndPostalCodeVerificationMerchants()
            throws InterruptedException {
        BraintreeTestUtils.setUpActivityTest(this,
                new TestClientTokenBuilder().withCvvAndPostalCodeVerification().build());

        addCardAndAssertSuccess(getActivity());
    }

    private void assertCreatePaymentMethodFromPayPal(String descriptionEmail) {
        BraintreePaymentActivity activity = getActivity();

        waitForView(withId(R.id.bt_paypal_button)).perform(click());
        waitForView(withHint("Email"), FIFTEEN_SECONDS).perform(typeText("bt_buyer_us@paypal.com"));
        onView(withHint("Password")).perform(typeText("11111111"));
        onView(withText("Log In")).perform(click());
        waitForPaymentMethodList();

        onView(withId(R.id.bt_payment_method_description)).check(
                matches(withText(descriptionEmail)));
        onView(withId(R.id.bt_select_payment_method_submit_button)).perform(click());

        waitForActivityToFinish(activity);

        Map<String, Object> result = getActivityResult(activity);
        PaymentMethod paymentMethod =
                (PaymentMethod) ((Intent) result.get("resultData")).getSerializableExtra(
                        BraintreePaymentActivity.EXTRA_PAYMENT_METHOD);

        assertEquals(Activity.RESULT_OK, result.get("resultCode"));
        assertNotNull(paymentMethod.getNonce());
    }

    @TargetApi(VERSION_CODES.HONEYCOMB)
    private boolean checkHomeAsUpEnabled(Activity activity) {
        return (activity.getActionBar().getDisplayOptions() & ActionBar.DISPLAY_HOME_AS_UP) ==
            ActionBar.DISPLAY_HOME_AS_UP;
    }

}
