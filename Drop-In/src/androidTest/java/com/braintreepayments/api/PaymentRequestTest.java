package com.braintreepayments.api;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.text.TextUtils;
import android.widget.ImageView;

import com.braintreepayments.api.test.BraintreePaymentActivityTestRunner;
import com.braintreepayments.testutils.TestClientTokenBuilder;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.util.Map;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.braintreepayments.api.test.ActivityResultHelper.getActivityResult;
import static com.braintreepayments.api.test.WaitForActivityHelper.waitForActivityToFinish;
import static com.braintreepayments.api.utils.PaymentFormHelpers.submitAndWaitForCompletion;
import static com.braintreepayments.api.utils.PaymentFormHelpers.waitForAddPaymentFormHeader;
import static com.braintreepayments.api.utils.PaymentFormHelpers.waitForPaymentMethodNonceList;
import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static com.braintreepayments.testutils.TestTokenizationKey.TOKENIZATION_KEY;
import static com.braintreepayments.testutils.ui.Matchers.withId;
import static com.braintreepayments.testutils.ui.ViewHelper.waitForView;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.startsWith;

public class PaymentRequestTest extends BraintreePaymentActivityTestRunner {

    @Test(timeout = 1000)
    public void getIntent_includesClientToken() {
        PaymentRequest paymentRequest = new PaymentRequest()
                .clientToken(stringFromFixture("client_token.json"));

        assertEquals(stringFromFixture("client_token.json") , paymentRequest.getAuthorization());
    }

    @Test(timeout = 1000)
    public void getIntent_includesClientKey() {
        PaymentRequest paymentRequest = new PaymentRequest()
                .tokenizationKey(TOKENIZATION_KEY);

        assertEquals(TOKENIZATION_KEY, paymentRequest.getAuthorization());
    }

    @Test(timeout = 30000)
    public void returnsDeviceDataStringWhenRequested() throws JSONException {
        Intent intent = new PaymentRequest()
                .tokenizationKey(TOKENIZATION_KEY)
                .collectDeviceData(true)
                .getIntent(getTargetContext());
        Activity activity = getActivity(intent);

        submitAndWaitForCompletion();
        waitForActivityToFinish(activity);

        Map<String, Object> result = getActivityResult(activity);
        String deviceData = ((Intent) result.get("resultData"))
                .getStringExtra(BraintreePaymentActivity.EXTRA_DEVICE_DATA);

        assertEquals(Activity.RESULT_OK, result.get("resultCode"));
        assertFalse(TextUtils.isEmpty(deviceData));
        JSONObject json = new JSONObject(deviceData);
        assertFalse(TextUtils.isEmpty(json.getString("device_session_id")));
        assertEquals("600000", json.getString("fraud_merchant_id"));
        assertFalse(TextUtils.isEmpty(json.getString("correlation_id")));
    }

    @Test(timeout = 30000)
    public void descriptionIsNotNecessary() {
        Intent intent = new PaymentRequest()
                .tokenizationKey(TOKENIZATION_KEY)
                .getIntent(getTargetContext());
        getActivity(intent);

        waitForAddPaymentFormHeader();
        onView(withId(com.braintreepayments.api.dropin.R.id.bt_primary_description)).check(matches(not(isDisplayed())));
    }

    @Test(timeout = 30000)
    public void submitButtonUsesDefaultTextIfNoCustomizationProvided() {
        Intent intent = new PaymentRequest()
                .tokenizationKey(TOKENIZATION_KEY)
                .getIntent(getTargetContext());
        getActivity(intent);

        waitForAddPaymentFormHeader();
        onView(withId(com.braintreepayments.api.dropin.R.id.bt_card_form_submit_button)).check(matches(withText(
                com.braintreepayments.api.dropin.R.string.bt_default_submit_button_text)));
    }

    @Test(timeout = 30000)
    public void submitButtonUsesCustomizationForCardFormIfIncludedAsAnExtra() {
        Intent intent = new PaymentRequest()
                .tokenizationKey(TOKENIZATION_KEY)
                .submitButtonText("Subscribe")
                .amount("$19")
                .getIntent(getTargetContext());
        getActivity(intent);

        waitForAddPaymentFormHeader();
        onView(withId(com.braintreepayments.api.dropin.R.id.bt_card_form_submit_button)).check(matches(withText("$19 - SUBSCRIBE")));
    }

    @Test(timeout = 30000)
    public void submitButtonUsesCustomizationForSelectPaymentMethodIfIncludedAsAnExtra() {
        Intent intent = new PaymentRequest()
                .clientToken(new TestClientTokenBuilder().build())
                .submitButtonText("Subscribe")
                .amount("$19")
                .getIntent(getTargetContext())
                .putExtra(BraintreePaymentTestActivity.GET_PAYMENT_METHODS,
                        stringFromFixture("responses/get_payment_methods_two_cards_response.json"));
        getActivity(intent);

        waitForPaymentMethodNonceList();
        onView(withId(com.braintreepayments.api.dropin.R.id.bt_select_payment_method_submit_button)).check(matches(withText("$19 - SUBSCRIBE")));
    }

    @Test(timeout = 30000)
    public void descriptionsAreDisplayedIfIncludedAsAnExtra() {
        Intent intent = new PaymentRequest()
                .tokenizationKey(TOKENIZATION_KEY)
                .primaryDescription("Hello, World!")
                .secondaryDescription("Some stuffz")
                .amount("$1,000,000,000.00")
                .getIntent(getTargetContext());
        getActivity(intent);

        waitForView(withId(com.braintreepayments.api.dropin.R.id.bt_primary_description)).check(matches(withText("Hello, World!")));
        onView(withId(com.braintreepayments.api.dropin.R.id.bt_secondary_description)).check(matches(withText("Some stuffz")));
        onView(withId(com.braintreepayments.api.dropin.R.id.bt_description_amount)).check(matches(withText("$1,000,000,000.00")));
        onView(withId(com.braintreepayments.api.dropin.R.id.bt_card_form_submit_button)).check(matches(withText(
                startsWith("$1,000,000,000.00"))));
    }

    @Test(timeout = 30000)
    public void defaultButtonTextIsUsedWhenCustomizationIsPresentWithoutSpecifyingButtonText() {
        Intent intent = new PaymentRequest()
                .tokenizationKey(TOKENIZATION_KEY)
                .amount("$19")
                .getIntent(getTargetContext());
        getActivity(intent);

        waitForAddPaymentFormHeader();
        onView(withId(com.braintreepayments.api.dropin.R.id.bt_card_form_submit_button)).check(matches(withText("$19 - PURCHASE")));
    }

    @Test(timeout = 30000)
    public void actionBarTitleAndLogoAreUsedIfIncludedAsAnExtra() {
        Intent intent = new PaymentRequest()
                .tokenizationKey(TOKENIZATION_KEY)
                .actionBarTitle("This is a title")
                .actionBarLogo(android.R.drawable.ic_delete)
                .getIntent(getTargetContext());
        Activity activity = getActivity(intent);

        waitForAddPaymentFormHeader();

        assertEquals("This is a title", activity.getActionBar().getTitle());

        if (VERSION.SDK_INT < VERSION_CODES.LOLLIPOP) {
            ImageView actual = (ImageView) activity.findViewById(android.R.id.home);
            assertBitmapsEqual(actual.getDrawable(),
                    getTargetContext().getResources().getDrawable(android.R.drawable.ic_delete)
            );
        }
    }

    @Test(timeout = 30000)
    public void defaultActionBarTitleAndLogoAreUsedWhenCustomizationIsPresentWithoutSpecifyingTitleAndLogo() {
        Intent intent = new PaymentRequest()
                .tokenizationKey(TOKENIZATION_KEY)
                .primaryDescription("Description")
                .getIntent(getTargetContext());
        Activity activity = getActivity(intent);

        waitForAddPaymentFormHeader();

        assertEquals("Purchase", activity.getActionBar().getTitle());

        if (VERSION.SDK_INT < VERSION_CODES.LOLLIPOP) {
            ImageView actual = (ImageView) activity.findViewById(android.R.id.home);
            ColorDrawable expected = new ColorDrawable(getTargetContext().getResources().getColor(
                    android.R.color.transparent));
            assertEquals(actual.getDrawable().getOpacity(), expected.getOpacity());
        }
    }

    /* helpers */
    private static void assertBitmapsEqual(Drawable d1, Drawable d2) {
        if (d1 == null || d2 == null) {
            assertEquals(d1, d2);
        } else {
            assertTrue(((BitmapDrawable) d1).getBitmap().sameAs(((BitmapDrawable) d2).getBitmap()));
        }
    }
}
