package com.braintreepayments.api.dropin;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.SystemClock;
import android.support.test.espresso.NoMatchingViewException;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.braintreepayments.api.Braintree;
import com.braintreepayments.api.exceptions.UnexpectedException;
import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.internal.BraintreeHttpClient;
import com.braintreepayments.api.models.Card;
import com.braintreepayments.api.models.CardBuilder;
import com.braintreepayments.api.models.PayPalAccountBuilder;
import com.braintreepayments.api.models.PaymentMethod;
import com.braintreepayments.testutils.TestClientTokenBuilder;

import org.json.JSONException;

import java.util.Map;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.hasSibling;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.braintreepayments.api.DropInTestUtils.create;
import static com.braintreepayments.api.DropInTestUtils.setBraintreeHttpClient;
import static com.braintreepayments.api.DropInTestUtils.setUpActivityTest;
import static com.braintreepayments.api.TestDependencyInjector.injectBraintree;
import static com.braintreepayments.api.TestDependencyInjector.injectSlowBraintree;
import static com.braintreepayments.api.utils.Assertions.assertSelectedPaymentMethodIs;
import static com.braintreepayments.api.utils.PaymentFormHelpers.onAddPaymentFormHeader;
import static com.braintreepayments.api.utils.PaymentFormHelpers.performPayPalAdd;
import static com.braintreepayments.api.utils.PaymentFormHelpers.waitForAddPaymentFormHeader;
import static com.braintreepayments.api.utils.PaymentFormHelpers.waitForPaymentMethodList;
import static com.braintreepayments.testutils.ActivityResultHelper.getActivityResult;
import static com.braintreepayments.testutils.CardNumber.AMEX;
import static com.braintreepayments.testutils.CardNumber.VISA;
import static com.braintreepayments.testutils.CardNumber.VISA_2;
import static com.braintreepayments.testutils.ui.Matchers.withId;
import static com.braintreepayments.testutils.ui.WaitForActivityHelper.waitForActivityToFinish;
import static org.hamcrest.CoreMatchers.allOf;

@SuppressLint("NewApi")
public class ListPaymentMethodTest extends BraintreePaymentActivityTestCase {

    private String mClientToken;
    private Braintree mBraintree;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        mClientToken = new TestClientTokenBuilder().withPayPal().build();
        setUpActivityTest(this, mClientToken);
        mBraintree = injectBraintree(mContext, mClientToken);
    }

    private String createVisa() throws InterruptedException {
        return create(mBraintree, new CardBuilder()
                .cardNumber(VISA)
                .expirationMonth("01")
                .expirationYear("2018")).getNonce();
    }

    private String createAmex() throws InterruptedException {
        return create(mBraintree, new CardBuilder()
                .cardNumber(AMEX)
                .expirationMonth("01")
                .expirationYear("2019")).getNonce();
    }

    public void testDisplaysALoadingViewWhileGettingPaymentMethods() throws JSONException {
        String clientToken = new TestClientTokenBuilder().build();
        injectSlowBraintree(mContext, clientToken, 4000);
        setUpActivityTest(this, clientToken);
        Activity activity = getActivity();

        assertEquals(View.VISIBLE,
                activity.findViewById(R.id.bt_loading_progress_bar).getVisibility());
    }

    public void testLoadingPaymentMethodsTimesOutAfterTenSecondsAndDropsToAddPaymentMethodForm()
            throws JSONException {
        String clientToken = new TestClientTokenBuilder().build();
        injectSlowBraintree(mContext, clientToken, 11000);
        setUpActivityTest(this, clientToken);
        getActivity();

        waitForAddPaymentFormHeader(10500).check(matches(isDisplayed()));
    }

    public void testFallsBackToAddPaymentMethodFormIfLoadingPaymentMethodsBlowsUp()
            throws JSONException {
        setBraintreeHttpClient(mBraintree, new BraintreeHttpClient("") {
            @Override
            public void get(String path, HttpResponseCallback callback) {
                callback.failure(new UnexpectedException("Mocked HTTP request"));
            }

            @Override
            public void post(String path, String data, HttpResponseCallback callback) {
                callback.failure(new UnexpectedException("Mocked HTTP request"));
            }
        });
        setUpActivityTest(this, mClientToken);
        long testStartTime = System.currentTimeMillis();

        getActivity();
        waitForAddPaymentFormHeader();

        long elapsedTestTime = System.currentTimeMillis() - testStartTime;
        assertTrue(elapsedTestTime < 5000);
    }

    public void testDisplaysACard() throws InterruptedException {
        createVisa();
        getActivity();

        waitForPaymentMethodList();

        assertSelectedPaymentMethodIs(R.string.bt_descriptor_visa);
        onView(withId(R.id.bt_payment_method_description)).check(matches(withText("ends in 11")));

        ImageView iv = ((ImageView) getActivity().findViewById(R.id.bt_payment_method_icon));
        Bitmap actual = ((BitmapDrawable) iv.getDrawable()).getBitmap();

        Bitmap expected = ((BitmapDrawable) getActivity().getResources()
                .getDrawable(R.drawable.bt_visa)).getBitmap();

        assertTrue(expected.sameAs(actual));
    }

    public void testDisplaysAddPaymentMethodIfOnlyOnePaymentMethodIsAvailable()
            throws InterruptedException {
        createVisa();
        getActivity();

        waitForPaymentMethodList();

        onView(withId(R.id.bt_change_payment_method_link)).check(
                matches(withText(R.string.bt_add_payment_method)));
    }

    public void testDisplaysChangePaymentMethodIfMoreThanOnePaymentMethodIsAvailable()
            throws InterruptedException {
        createVisa();
        createAmex();
        getActivity();

        waitForPaymentMethodList();

        onView(withId(R.id.bt_change_payment_method_link)).check(
                matches(withText(R.string.bt_change_payment_method)));
    }

    public void testNoopsWhenClickingPaymentMethodAndOnlyOneExists() throws InterruptedException {
        createVisa();
        getActivity();

        waitForPaymentMethodList();

        onView(withId(R.id.bt_selected_payment_method_view)).perform(click());

        try {
            onView(withText(R.string.bt_choose_payment_method)).check(doesNotExist());
            onView(withId(R.id.bt_card_form_card_number)).check(doesNotExist());
        } catch (NoMatchingViewException ignored) {
        }
    }

    public void testSkipsPaymentMethodListWhenClickingAddPaymentMethodLink()
            throws InterruptedException {
        createVisa();
        getActivity();

        waitForPaymentMethodList();

        onView(withId(R.id.bt_change_payment_method_link)).perform(click());

        onView(withId(R.id.bt_card_form_card_number)).check(matches(isDisplayed()));
    }

    public void testDisplaysCorrectActivePaymentMethod() throws InterruptedException {
        createAmex();
        getActivity();

        waitForPaymentMethodList();

        assertSelectedPaymentMethodIs(R.string.bt_descriptor_amex);
        onView(withId(R.id.bt_payment_method_description)).check(matches(withText("ends in 05")));

        ImageView iv = ((ImageView) getActivity().findViewById(R.id.bt_payment_method_icon));
        Bitmap actual = ((BitmapDrawable) iv.getDrawable()).getBitmap();

        Bitmap expected = ((BitmapDrawable) getActivity().getResources()
                .getDrawable(R.drawable.bt_amex)).getBitmap();

        assertTrue(expected.sameAs(actual));
    }

    public void testSetsCreatedPaymentMethodAsSelectedPaymentMethod() throws InterruptedException {
        createVisa();
        getActivity();
        waitForPaymentMethodList();

        assertSelectedPaymentMethodIs(R.string.bt_descriptor_visa);

        onView(withText(R.string.bt_add_payment_method)).perform(click());
        performPayPalAdd();

        assertSelectedPaymentMethodIs(R.string.bt_descriptor_paypal);
    }

    public void testChangePaymentMethodShowsChooserDialog() throws InterruptedException {
        createVisa();
        createAmex();
        getActivity();

        waitForPaymentMethodList();
        onView(withId(R.id.bt_change_payment_method_link)).perform(click());

        onView(withText("Choose Payment Method")).check(matches(isDisplayed()));
    }

    public void testShowsAllPaymentMethodsInDialog() throws InterruptedException {
        createVisa();
        createAmex();
        SystemClock.sleep(1000);
        create(mBraintree, new PayPalAccountBuilder().consentCode("fake_auth_code"));
        getActivity();

        waitForPaymentMethodList();

        assertSelectedPaymentMethodIs(R.string.bt_descriptor_paypal);
        onView(withId(R.id.bt_selected_payment_method_view)).perform(click());

        onView(withText(R.string.bt_descriptor_visa)).check(matches(
                allOf(hasSibling(withText("ends in 11")), isDisplayed())));
        onView(withText(R.string.bt_descriptor_amex)).check(
                matches(allOf(hasSibling(withText("ends in 05")), isDisplayed())));
        onView(withText(R.string.bt_descriptor_paypal)).check(
                matches(allOf(hasSibling(withText("jane.doe@example.com")), isDisplayed())));
    }

    public void testSelectingFromListOfPaymentMethodsUpdatesCurrentPaymentMethod()
            throws InterruptedException {
        createVisa();
        createAmex();
        getActivity();

        waitForPaymentMethodList();
        assertSelectedPaymentMethodIs(R.string.bt_descriptor_amex);
        onView(withId(R.id.bt_selected_payment_method_view)).perform(click());

        onView(withText(R.string.bt_descriptor_visa)).perform(click());

        onView(withText("Choose Payment Method")).check(doesNotExist());
        assertSelectedPaymentMethodIs(R.string.bt_descriptor_visa);
    }

    public void testSelectingFromListReturnsSelectedPaymentMethod() throws InterruptedException {
        String nonce = createAmex();
        create(mBraintree, new CardBuilder()
                .cardNumber(VISA_2)
                .expirationMonth("02")
                .expirationYear("2029"));
        BraintreePaymentActivity activity = getActivity();

        waitForPaymentMethodList();
        assertSelectedPaymentMethodIs(R.string.bt_descriptor_visa);
        onView(withId(R.id.bt_selected_payment_method_view)).perform(click());

        onView(withText(R.string.bt_descriptor_amex)).perform(click());

        onView(withText("Choose Payment Method")).check(doesNotExist());
        onView(withId(R.id.bt_select_payment_method_submit_button)).perform(click());

        waitForActivityToFinish(activity);

        Map<String, Object> result = getActivityResult(activity);
        PaymentMethod response = ((Intent) result.get("resultData"))
                .getParcelableExtra(BraintreePaymentActivity.EXTRA_PAYMENT_METHOD);

        assertEquals(Activity.RESULT_OK, result.get("resultCode"));
        assertEquals(nonce, response.getNonce());
    }

    public void testSubmittingSelectedPaymentMethodReturnsItToCallingActivity()
            throws InterruptedException {
        createVisa();
        BraintreePaymentActivity activity = getActivity();

        waitForPaymentMethodList();
        onView(withId(R.id.bt_select_payment_method_submit_button)).perform(click());

        waitForActivityToFinish(activity);

        Map<String, Object> result = getActivityResult(activity);
        PaymentMethod response = ((Intent) result.get("resultData"))
                .getParcelableExtra(BraintreePaymentActivity.EXTRA_PAYMENT_METHOD);

        assertEquals(Activity.RESULT_OK, result.get("resultCode"));
        assertNotNull(response.getNonce());
        assertEquals("11", ((Card) response).getLastTwo());
    }

    public void testAddNewPaymentMethodOpensPaymentMethodForm() throws InterruptedException {
        createVisa();
        createAmex();
        getActivity();

        waitForPaymentMethodList();
        onView(withId(R.id.bt_selected_payment_method_view)).perform(click());

        onView(withText("Add New Payment Method")).perform(click());

        onAddPaymentFormHeader().check(matches(isDisplayed()));
    }

    public void testBackButtonTakesYouBackToSelectPaymentMethodViewFromAddPaymentMethodView()
            throws InterruptedException {
        createVisa();
        getActivity();

        waitForPaymentMethodList();
        onView(withId(R.id.bt_change_payment_method_link)).perform(click());

        onAddPaymentFormHeader().check(matches(isDisplayed()));
        sendKeys(KeyEvent.KEYCODE_BACK);
        onView(withId(R.id.bt_change_payment_method_link)).check(matches(isDisplayed()));
    }

    public void testActionBarUpTakesYouBackToSelectPaymentMethodViewFromAddPaymentMethodView()
            throws InterruptedException {
        createVisa();
        BraintreePaymentActivity activity = getActivity();

        waitForPaymentMethodList();
        onView(withId(R.id.bt_change_payment_method_link)).perform(click());

        onAddPaymentFormHeader().check(matches(isDisplayed()));
        if (VERSION.SDK_INT > VERSION_CODES.KITKAT) {
            onView(withContentDescription("Navigate up")).perform(click());
        } else {
            onView(withId(android.R.id.home)).perform(click());
        }

        onView(withId(R.id.bt_change_payment_method_link)).check(matches(isDisplayed()));
        assertFalse("Expected up not to be set on action bar",
                (activity.getActionBar().getDisplayOptions() & ActionBar.DISPLAY_HOME_AS_UP) ==
                        ActionBar.DISPLAY_HOME_AS_UP
        );
    }

    public void testSubmittingDisablesSubmitButton() throws InterruptedException {
        createVisa();
        BraintreePaymentActivity activity = getActivity();

        waitForPaymentMethodList();
        onView(withId(R.id.bt_select_payment_method_submit_button)).perform(click());

        Button submitButton = (Button) activity.findViewById(R.id.bt_select_payment_method_submit_button);
        assertFalse(submitButton.isEnabled());
    }
}
