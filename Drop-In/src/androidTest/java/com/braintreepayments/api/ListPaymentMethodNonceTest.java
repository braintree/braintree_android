package com.braintreepayments.api;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.support.test.espresso.NoMatchingViewException;
import android.test.suitebuilder.annotation.LargeTest;
import android.widget.ImageView;

import com.braintreepayments.api.exceptions.UnexpectedException;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.api.test.BraintreePaymentActivityTestRunner;
import com.braintreepayments.testutils.TestClientTokenBuilder;

import org.junit.Test;

import java.util.Map;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.hasSibling;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.braintreepayments.api.utils.Assertions.assertSelectedPaymentMethodIs;
import static com.braintreepayments.api.utils.PaymentFormHelpers.onAddPaymentFormHeader;
import static com.braintreepayments.api.utils.PaymentFormHelpers.performPayPalAdd;
import static com.braintreepayments.api.utils.PaymentFormHelpers.waitForAddPaymentFormHeader;
import static com.braintreepayments.api.utils.PaymentFormHelpers.waitForPaymentMethodNonceList;
import static com.braintreepayments.testutils.ActivityResultHelper.getActivityResult;
import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static com.braintreepayments.testutils.ui.Matchers.withId;
import static com.braintreepayments.testutils.ui.ViewHelper.FOUR_SECONDS;
import static com.braintreepayments.testutils.ui.WaitForActivityHelper.waitForActivityToFinish;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.Matchers.allOf;

@SuppressLint("NewApi")
@LargeTest
public class ListPaymentMethodNonceTest extends BraintreePaymentActivityTestRunner {

    @Test(timeout = 30000)
    public void displaysALoadingViewWhileGettingPaymentMethods() {
        getActivity(new TestClientTokenBuilder().build(), FOUR_SECONDS);

        onView(withId(com.braintreepayments.api.dropin.R.id.bt_loading_progress_bar)).check(matches(isDisplayed()));
    }

    @Test(timeout = 30000)
    public void loadingPaymentMethodsTimesOutAfterTenSecondsAndDropsToAddPaymentMethodForm() {
        String clientToken = new TestClientTokenBuilder().build();
        Intent intent = new PaymentRequest()
                .clientToken(clientToken)
                .getIntent(getTargetContext())
                .putExtra(BraintreePaymentTestActivity.MOCK_CONFIGURATION, clientToken)
                .putExtra(BraintreePaymentTestActivity.EXTRA_DELAY, 11000);
        getActivity(intent);

        waitForAddPaymentFormHeader(10500).check(matches(isDisplayed()));
    }

    @Test(timeout = 30000)
    public void fallsBackToAddPaymentMethodFormIfLoadingPaymentMethodsBlowsUp() {
        long testStartTime = System.currentTimeMillis();
        Intent intent = new PaymentRequest()
                .clientToken(new TestClientTokenBuilder().build())
                .getIntent(getTargetContext())
                .putExtra(BraintreePaymentTestActivity.GET_PAYMENT_METHODS_ERROR, new UnexpectedException("Get Payment Methods Error"));
        getActivity(intent);

        waitForAddPaymentFormHeader();

        long elapsedTestTime = System.currentTimeMillis() - testStartTime;
        assertTrue(elapsedTestTime < 5000);
    }

    @Test(timeout = 30000)
    public void displaysACard() {
        Activity activity = getActivity(getSinglePaymentMethodNonceIntent(
                new TestClientTokenBuilder().build()));

        waitForPaymentMethodNonceList();

        assertSelectedPaymentMethodIs(com.braintreepayments.api.dropin.R.string.bt_descriptor_visa);
        onView(withId(com.braintreepayments.api.dropin.R.id.bt_payment_method_description)).check(matches(withText("ends in 11")));

        ImageView iv = ((ImageView) activity.findViewById(com.braintreepayments.api.dropin.R.id.bt_payment_method_icon));
        Bitmap actual = ((BitmapDrawable) iv.getDrawable()).getBitmap();

        Bitmap expected = ((BitmapDrawable) activity.getResources().getDrawable(
                com.braintreepayments.api.dropin.R.drawable.bt_visa))
                .getBitmap();

        assertTrue(expected.sameAs(actual));
    }

    @Test(timeout = 30000)
    public void displaysAddPaymentMethodIfOnlyOnePaymentMethodIsAvailable() {
        getActivity(getSinglePaymentMethodNonceIntent(new TestClientTokenBuilder().build()));

        waitForPaymentMethodNonceList();

        onView(withId(com.braintreepayments.api.dropin.R.id.bt_change_payment_method_link)).check(
                matches(withText(com.braintreepayments.api.dropin.R.string.bt_add_payment_method)));
    }

    @Test(timeout = 30000)
    public void displaysChangePaymentMethodIfMoreThanOnePaymentMethodIsAvailable() {
        getActivity(getMultiplePaymentMethodNoncesIntent(new TestClientTokenBuilder().build()));

        waitForPaymentMethodNonceList();

        onView(withId(com.braintreepayments.api.dropin.R.id.bt_change_payment_method_link)).check(
                matches(withText(
                        com.braintreepayments.api.dropin.R.string.bt_change_payment_method)));
    }

    @Test(timeout = 30000)
    public void noopsWhenClickingPaymentMethodAndOnlyOneExists() {
        getActivity(getSinglePaymentMethodNonceIntent(new TestClientTokenBuilder().build()));

        waitForPaymentMethodNonceList();

        onView(withId(com.braintreepayments.api.dropin.R.id.bt_selected_payment_method_view)).perform(
                click());

        try {
            onView(withText(com.braintreepayments.api.dropin.R.string.bt_choose_payment_method)).check(doesNotExist());
            onView(withId(com.braintreepayments.api.dropin.R.id.bt_card_form_card_number)).check(doesNotExist());
        } catch (NoMatchingViewException ignored) {}
    }

    @Test(timeout = 30000)
    public void skipsPaymentMethodListWhenOnlyOnePaymentMethodExistsAndClickingAddPaymentMethodLink() {
        getActivity(getSinglePaymentMethodNonceIntent(new TestClientTokenBuilder().build()));

        waitForPaymentMethodNonceList();

        onView(withId(com.braintreepayments.api.dropin.R.id.bt_change_payment_method_link)).perform(click());

        onView(withId(com.braintreepayments.api.dropin.R.id.bt_card_form_card_number)).check(
                matches(isDisplayed()));
    }

    @Test(timeout = 30000)
    public void displaysCorrectActivePaymentMethod() {
        Activity activity = getActivity(getSinglePaymentMethodNonceIntent(
                new TestClientTokenBuilder().build()));

        waitForPaymentMethodNonceList();

        assertSelectedPaymentMethodIs(com.braintreepayments.api.dropin.R.string.bt_descriptor_visa);
        onView(withId(com.braintreepayments.api.dropin.R.id.bt_payment_method_description)).check(
                matches(withText("ends in 11")));

        ImageView iv = ((ImageView) activity.findViewById(com.braintreepayments.api.dropin.R.id.bt_payment_method_icon));
        Bitmap actual = ((BitmapDrawable) iv.getDrawable()).getBitmap();

        Bitmap expected = ((BitmapDrawable) activity.getResources()
                .getDrawable(com.braintreepayments.api.dropin.R.drawable.bt_visa)).getBitmap();

        assertTrue(expected.sameAs(actual));
    }

    @Test(timeout = 30000)
    public void setsCreatedPaymentMethodAsSelectedPaymentMethod() {
        String clientToken = new TestClientTokenBuilder().withPayPal().build();
        Intent intent = getSinglePaymentMethodNonceIntent(clientToken)
                .putExtra(BraintreePaymentTestActivity.MOCK_CONFIGURATION, clientToken);
        getActivity(intent);

        waitForPaymentMethodNonceList();

        assertSelectedPaymentMethodIs(com.braintreepayments.api.dropin.R.string.bt_descriptor_visa);

        onView(withText(com.braintreepayments.api.dropin.R.string.bt_add_payment_method)).perform(
                click());
        performPayPalAdd();

        assertSelectedPaymentMethodIs(com.braintreepayments.api.dropin.R.string.bt_descriptor_paypal);
    }

    @Test(timeout = 30000)
    public void changePaymentMethodShowsChooserDialog() {
        getActivity(getMultiplePaymentMethodNoncesIntent(new TestClientTokenBuilder().build()));

        waitForPaymentMethodNonceList();
        onView(withId(com.braintreepayments.api.dropin.R.id.bt_change_payment_method_link)).perform(
                click());

        onView(withText(com.braintreepayments.api.dropin.R.string.bt_choose_payment_method)).check(matches(isDisplayed()));
    }

    @Test(timeout = 30000)
    public void showsAllPaymentMethodsInDialog() {
        Intent intent = new PaymentRequest()
                .clientToken(new TestClientTokenBuilder().build())
                .getIntent(getTargetContext())
                .putExtra(BraintreePaymentTestActivity.GET_PAYMENT_METHODS,
                        stringFromFixture("responses/get_payment_methods_response.json"));
        getActivity(intent);

        waitForPaymentMethodNonceList();

        assertSelectedPaymentMethodIs(com.braintreepayments.api.dropin.R.string.bt_descriptor_visa);
        onView(withId(com.braintreepayments.api.dropin.R.id.bt_selected_payment_method_view)).perform(
                click());

        onView(withText(com.braintreepayments.api.dropin.R.string.bt_descriptor_visa)).check(
                matches(allOf(hasSibling(withText("ends in 11")), isDisplayed())));
        onView(withText(com.braintreepayments.api.dropin.R.string.bt_descriptor_paypal)).check(
                matches(allOf(hasSibling(withText("paypalaccount@example.com")), isDisplayed())));
        onView(withText(com.braintreepayments.api.dropin.R.string.bt_descriptor_android_pay)).check(
                matches(allOf(hasSibling(withText("ends in 11")), isDisplayed())));
    }

    @Test(timeout = 30000)
    public void selectingFromListOfPaymentMethodsUpdatesCurrentPaymentMethod() {
        getActivity(getMultiplePaymentMethodNoncesIntent(new TestClientTokenBuilder().build()));

        waitForPaymentMethodNonceList();
        assertSelectedPaymentMethodIs(com.braintreepayments.api.dropin.R.string.bt_descriptor_visa);
        onView(withId(com.braintreepayments.api.dropin.R.id.bt_selected_payment_method_view)).perform(
                click());

        onView(withText(com.braintreepayments.api.dropin.R.string.bt_descriptor_amex)).perform(click());

        onView(withText(com.braintreepayments.api.dropin.R.string.bt_choose_payment_method)).check(
                doesNotExist());
        assertSelectedPaymentMethodIs(com.braintreepayments.api.dropin.R.string.bt_descriptor_amex);
    }

    @Test(timeout = 30000)
    public void selectingFromListReturnsSelectedPaymentMethod() {
        BraintreePaymentActivity activity = getActivity(getMultiplePaymentMethodNoncesIntent(
                new TestClientTokenBuilder().build()));

        waitForPaymentMethodNonceList();
        assertSelectedPaymentMethodIs(com.braintreepayments.api.dropin.R.string.bt_descriptor_visa);
        onView(withId(com.braintreepayments.api.dropin.R.id.bt_selected_payment_method_view)).perform(click());

        onView(withText(com.braintreepayments.api.dropin.R.string.bt_descriptor_amex)).perform(click());

        onView(withText(com.braintreepayments.api.dropin.R.string.bt_choose_payment_method)).check(
                doesNotExist());
        onView(withId(com.braintreepayments.api.dropin.R.id.bt_select_payment_method_submit_button)).perform(click());

        waitForActivityToFinish(activity);
        Map<String, Object> result = getActivityResult(activity);
        PaymentMethodNonce response = ((Intent) result.get("resultData"))
                .getParcelableExtra(BraintreePaymentActivity.EXTRA_PAYMENT_METHOD_NONCE);

        assertEquals(Activity.RESULT_OK, result.get("resultCode"));
        assertEquals("123456-12345-12345-a-amex", response.getNonce());
    }

    @Test(timeout = 30000)
    public void submittingSelectedPaymentMethodReturnsItToCallingActivity() {
        BraintreePaymentActivity activity = getActivity(getSinglePaymentMethodNonceIntent(
                new TestClientTokenBuilder().build()));

        waitForPaymentMethodNonceList();
        onView(withId(com.braintreepayments.api.dropin.R.id.bt_select_payment_method_submit_button)).perform(
                click());

        waitForActivityToFinish(activity);
        Map<String, Object> result = getActivityResult(activity);
        PaymentMethodNonce response = ((Intent) result.get("resultData"))
                .getParcelableExtra(BraintreePaymentActivity.EXTRA_PAYMENT_METHOD_NONCE);

        assertEquals(Activity.RESULT_OK, result.get("resultCode"));
        assertEquals("123456-12345-12345-a-adfa", response.getNonce());
    }

    @Test(timeout = 30000)
    public void addNewPaymentMethodOpensPaymentMethodForm() {
        getActivity(getMultiplePaymentMethodNoncesIntent(new TestClientTokenBuilder().build()));

        waitForPaymentMethodNonceList();

        onView(withId(
                com.braintreepayments.api.dropin.R.id.bt_selected_payment_method_view)).perform(
                click());

        onView(withText(com.braintreepayments.api.dropin.R.string.bt_add_new_payment_method)).perform(
                click());

        onAddPaymentFormHeader().check(matches(isDisplayed()));
    }

    @Test(timeout = 30000)
    public void backButtonTakesYouBackToSelectPaymentMethodViewFromAddPaymentMethodView() {
        getActivity(getSinglePaymentMethodNonceIntent(new TestClientTokenBuilder().build()));

        waitForPaymentMethodNonceList();

        onView(withId(com.braintreepayments.api.dropin.R.id.bt_change_payment_method_link)).perform(click());
        onAddPaymentFormHeader().check(matches(isDisplayed()));

        pressBack();

        onView(withId(com.braintreepayments.api.dropin.R.id.bt_change_payment_method_link)).check(
                matches(isDisplayed()));
    }

    @Test(timeout = 30000)
    public void actionBarUpTakesYouBackToSelectPaymentMethodViewFromAddPaymentMethodView() {
        BraintreePaymentActivity activity = getActivity(getSinglePaymentMethodNonceIntent(
                new TestClientTokenBuilder().build()));

        waitForPaymentMethodNonceList();

        onView(withId(com.braintreepayments.api.dropin.R.id.bt_change_payment_method_link)).perform(
                click());
        onAddPaymentFormHeader().check(matches(isDisplayed()));

        onView(withId(android.R.id.home)).perform(click());

        onView(withId(com.braintreepayments.api.dropin.R.id.bt_change_payment_method_link)).check(
                matches(isDisplayed()));
        assertFalse("Expected up not to be set on action bar",
                (activity.getActionBar().getDisplayOptions() & ActionBar.DISPLAY_HOME_AS_UP) ==
                        ActionBar.DISPLAY_HOME_AS_UP
        );
    }

    /* helpers */
    private Intent getSinglePaymentMethodNonceIntent(String clientToken) {
        return new PaymentRequest()
                .clientToken(clientToken)
                .getIntent(getTargetContext())
                .putExtra(BraintreePaymentTestActivity.GET_PAYMENT_METHODS,
                        stringFromFixture("responses/get_payment_methods_visa_response.json"));
    }

    private Intent getMultiplePaymentMethodNoncesIntent(String clientToken) {
        return new PaymentRequest()
                .clientToken(clientToken)
                .getIntent(getTargetContext())
                .putExtra(BraintreePaymentTestActivity.GET_PAYMENT_METHODS,
                        stringFromFixture("responses/get_payment_methods_two_cards_response.json"));
    }
}
