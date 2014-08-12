package com.braintreepayments.api.dropin;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.braintreepayments.api.BraintreeApi;
import com.braintreepayments.api.TestClientTokenBuilder;
import com.braintreepayments.api.TestUtils;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.exceptions.UnexpectedException;
import com.braintreepayments.api.internal.HttpRequest;
import com.braintreepayments.api.models.Card;
import com.braintreepayments.api.models.CardBuilder;
import com.braintreepayments.api.models.PayPalAccountBuilder;
import com.braintreepayments.api.models.PaymentMethod;
import com.braintreepayments.api.utils.ViewHelper;
import com.google.android.apps.common.testing.ui.espresso.NoMatchingViewException;

import java.io.IOException;
import java.util.Map;

import static com.braintreepayments.api.TestUtils.assertSelectedPaymentMethodIs;
import static com.braintreepayments.api.TestUtils.injectSlowBraintree;
import static com.braintreepayments.api.TestUtils.setUpActivityTest;
import static com.braintreepayments.api.utils.TestHelper.waitForActivity;
import static com.braintreepayments.api.utils.ViewHelper.onAddPaymentFormHeader;
import static com.braintreepayments.api.utils.ViewHelper.waitForPaymentMethodList;
import static com.google.android.apps.common.testing.ui.espresso.Espresso.onView;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.click;
import static com.google.android.apps.common.testing.ui.espresso.assertion.ViewAssertions.doesNotExist;
import static com.google.android.apps.common.testing.ui.espresso.assertion.ViewAssertions.matches;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.hasSibling;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.isDisplayed;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withId;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressLint("NewApi")
public class ListPaymentMethodTest extends BraintreePaymentActivityTestCase {

    private BraintreeApi mBraintreeApi;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        mBraintreeApi = new BraintreeApi(getInstrumentation().getContext(),
                setUpActivityTest(this, new TestClientTokenBuilder().withFakePayPal().build()));
        mBraintreeApi.create(new CardBuilder()
                .cardNumber("4111111111111111")
                .expirationMonth("01")
                .expirationYear("2018"));
    }

    private String createAmex() throws IOException, ErrorWithResponse {
        SystemClock.sleep(1000);

        return mBraintreeApi.create(new CardBuilder()
                .cardNumber("378282246310005")
                .expirationMonth("01")
                .expirationYear("2019")).getNonce();
    }

    public void testDisplaysALoadingViewWhileGettingPaymentMethods() {
        String clientToken = new TestClientTokenBuilder().withPayPal().build();
        injectSlowBraintree(getInstrumentation().getContext(), clientToken, 4000);
        setUpActivityTest(this, clientToken);
        Activity activity = getActivity();

        assertEquals(View.VISIBLE, activity.findViewById(R.id.loading_spinner).getVisibility());
    }

    public void testLoadingPaymentMethodsTimesOutAfterTenSecondsAndDropsToAddPaymentMethodForm() {
        String clientToken = new TestClientTokenBuilder().withPayPal().build();
        injectSlowBraintree(getInstrumentation().getContext(), clientToken, 11000);
        setUpActivityTest(this, clientToken);
        getActivity();

        ViewHelper.waitForAddPaymentFormHeader(10500).check(matches(isDisplayed()));
    }

    public void testFallsBackToAddPaymentMethodFormIfLoadingPaymentMethodsBlowsUp()
            throws UnexpectedException {
        String clientToken = new TestClientTokenBuilder().withPayPal().build();
        HttpRequest mockRequest = mock(HttpRequest.class);
        when(mockRequest.get(anyString())).thenThrow(new UnexpectedException("Mocked HTTP request"));
        when(mockRequest.post(anyString(), anyString())).thenThrow(new UnexpectedException("Mocked HTTP request"));
        TestUtils.injectBraintree(getInstrumentation().getContext(), clientToken, mockRequest);

        setUpActivityTest(this, clientToken);
        long testStartTime = System.currentTimeMillis();
        getActivity();

        ViewHelper.waitForAddPaymentFormHeader();
        long elapsedTestTime = System.currentTimeMillis() - testStartTime;
        assertTrue(elapsedTestTime < 5000);
    }

    public void testDisplaysACard() {
        getActivity();

        waitForPaymentMethodList();

        assertSelectedPaymentMethodIs(R.string.descriptor_visa);
        onView(withId(R.id.payment_method_description)).check(matches(withText("ends in 11")));

        ImageView iv = ((ImageView) getActivity().findViewById(R.id.payment_method_icon));
        Bitmap actual = ((BitmapDrawable) iv.getDrawable()).getBitmap();

        Bitmap expected = ((BitmapDrawable) getActivity().getResources()
                .getDrawable(R.drawable.ic_visa)).getBitmap();

        assertTrue(expected.sameAs(actual));
    }

    public void testDisplaysAddPaymentMethodIfOnlyOnePaymentMethodIsAvailable() {
        getActivity();

        waitForPaymentMethodList();

        onView(withId(R.id.change_payment_method_link)).check(
                matches(withText(R.string.add_payment_method)));
    }

    public void testDisplaysChangePaymentMethodIfMoreThanOnePaymentMethodIsAvailable()
            throws IOException, ErrorWithResponse {
        createAmex();
        getActivity();

        waitForPaymentMethodList();

        onView(withId(R.id.change_payment_method_link)).check(matches(withText(R.string.change_payment_method)));
    }

    public void testNoopsWhenClickingPaymentMethodAndOnlyOneExists() {
        getActivity();

        waitForPaymentMethodList();

        onView(withId(R.id.selected_payment_method_view)).perform(click());

        try {
            onView(withText(R.string.choose_payment_method)).check(doesNotExist());
            onView(withId(R.id.card_form_card_number)).check(doesNotExist());
        } catch (NoMatchingViewException e) {
            // noop
        }
    }

    public void testSkipsPaymentMethodListWhenClickingAddPaymentMethodLink() {
        getActivity();

        waitForPaymentMethodList();

        onView(withId(R.id.change_payment_method_link)).perform(click());

        onView(withId(R.id.card_form_card_number)).check(matches(isDisplayed()));
    }

    public void testDisplaysCorrectActivePaymentMethod() throws IOException, ErrorWithResponse {
        createAmex();
        getActivity();

        waitForPaymentMethodList();

        assertSelectedPaymentMethodIs(R.string.descriptor_amex);
        onView(withId(R.id.payment_method_description)).check(matches(withText("ends in 05")));

        ImageView iv = ((ImageView) getActivity().findViewById(R.id.payment_method_icon));
        Bitmap actual = ((BitmapDrawable) iv.getDrawable()).getBitmap();

        Bitmap expected = ((BitmapDrawable) getActivity().getResources()
                .getDrawable(R.drawable.ic_amex)).getBitmap();

        assertTrue(expected.sameAs(actual));
    }

    public void testChangePaymentMethodShowsChooserDialog() throws IOException, ErrorWithResponse {
        createAmex();
        getActivity();

        waitForPaymentMethodList();
        onView(withId(R.id.change_payment_method_link)).perform(click());

        onView(withText("Choose Payment Method")).check(matches(isDisplayed()));
    }

    public void testShowsAllPaymentMethodsInDialog() throws IOException, ErrorWithResponse {
        createAmex();
        SystemClock.sleep(1000);
        mBraintreeApi.create(new PayPalAccountBuilder().authorizationCode("fake_auth_code"));
        getActivity();

        waitForPaymentMethodList();

        assertSelectedPaymentMethodIs(R.string.descriptor_paypal);
        onView(withId(R.id.selected_payment_method_view)).perform(click());

        onView(withText(R.string.descriptor_visa)).check(matches(
                allOf(hasSibling(withText("ends in 11")), isDisplayed())));
        onView(withText(R.string.descriptor_amex)).check(
                matches(allOf(hasSibling(withText("ends in 05")), isDisplayed())));
        onView(withText(R.string.descriptor_paypal)).check(
                matches(allOf(hasSibling(withText("jane.doe@example.com")), isDisplayed())));
    }

    public void testSelectingFromListOfPaymentMethodsUpdatesCurrentPaymentMethod()
            throws IOException, ErrorWithResponse {
        createAmex();
        getActivity();

        waitForPaymentMethodList();
        assertSelectedPaymentMethodIs(R.string.descriptor_amex);
        onView(withId(R.id.selected_payment_method_view)).perform(click());

        onView(withText(R.string.descriptor_visa)).perform(click());

        onView(withText("Choose Payment Method")).check(doesNotExist());
        assertSelectedPaymentMethodIs(R.string.descriptor_visa);
    }

    public void testSelectingFromListSetsActivePaymentForSubmit()
            throws IOException, ErrorWithResponse {
        String nonce = createAmex();
        mBraintreeApi.create(new CardBuilder()
                .cardNumber("4005519200000004")
                .expirationMonth("02")
                .expirationYear("2029"));
        BraintreePaymentActivity activity = getActivity();

        waitForPaymentMethodList();
        assertSelectedPaymentMethodIs(R.string.descriptor_visa);
        onView(withId(R.id.selected_payment_method_view)).perform(click());

        onView(withText(R.string.descriptor_amex)).perform(click());

        onView(withText("Choose Payment Method")).check(doesNotExist());
        onView(withId(R.id.select_payment_method_button)).perform(click());

        assertEquals(nonce, activity.getActivePaymentMethod().getNonce());
    }

    public void testSubmittingSelectedPaymentMethodReturnsItToCallingActivity()
            throws IOException, ErrorWithResponse {
        BraintreePaymentActivity activity = getActivity();

        waitForPaymentMethodList();
        onView(withId(R.id.select_payment_method_button)).perform(click());

        waitForActivity(activity);

        Map<String, Object> result = TestUtils.getActivityResult(activity);
        PaymentMethod response =
                (PaymentMethod) ((Intent) result.get("resultData")).getSerializableExtra(
                        BraintreePaymentActivity.EXTRA_PAYMENT_METHOD);

        assertEquals(Activity.RESULT_OK, result.get("resultCode"));
        assertNotNull(response.getNonce());
        assertEquals("11", ((Card) response).getLastTwo());
    }

    public void testAddNewPaymentMethodOpensPaymentMethodForm()
            throws IOException, ErrorWithResponse {
        createAmex();
        getActivity();

        waitForPaymentMethodList();
        onView(withId(R.id.selected_payment_method_view)).perform(click());

        onView(withText("Add New Payment Method")).perform(click());

        onAddPaymentFormHeader().check(matches(isDisplayed()));
    }

    public void testBackButtonTakesYouBackToSelectPaymentMethodViewFromAddPaymentMethodView() {
        getActivity();

        waitForPaymentMethodList();
        onView(withId(R.id.change_payment_method_link)).perform(click());

        onAddPaymentFormHeader().check(matches(isDisplayed()));
        sendKeys(KeyEvent.KEYCODE_BACK);
        onView(withId(R.id.change_payment_method_link)).check(matches(isDisplayed()));
    }

    public void testActionBarUpTakesYouBackToSelectPaymentMethodViewFromAddPaymentMethodView() {
        BraintreePaymentActivity activity = getActivity();

        waitForPaymentMethodList();
        onView(withId(R.id.change_payment_method_link)).perform(click());

        onAddPaymentFormHeader().check(matches(isDisplayed()));
        onView(withId(android.R.id.home)).perform(click());

        onView(withId(R.id.change_payment_method_link)).check(matches(isDisplayed()));
        assertFalse("Expected up not to be set on action bar",
                (activity.getActionBar().getDisplayOptions() & ActionBar.DISPLAY_HOME_AS_UP) ==
                        ActionBar.DISPLAY_HOME_AS_UP
        );
    }

    public void testSubmittingDisablesSubmitButton() throws IOException, ErrorWithResponse {
        BraintreePaymentActivity activity = getActivity();

        waitForPaymentMethodList();
        onView(withId(R.id.select_payment_method_button)).perform(click());

        Button submitButton = (Button) activity.findViewById(R.id.select_payment_method_button);
        assertFalse(submitButton.isEnabled());
    }

}
