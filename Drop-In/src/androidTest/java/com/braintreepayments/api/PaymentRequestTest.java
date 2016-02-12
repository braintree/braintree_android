package com.braintreepayments.api;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Parcel;
import android.test.suitebuilder.annotation.LargeTest;
import android.test.suitebuilder.annotation.SmallTest;
import android.text.TextUtils;
import android.widget.ImageView;

import com.braintreepayments.api.test.BraintreePaymentActivityTestRunner;
import com.braintreepayments.testutils.TestClientTokenBuilder;
import com.google.android.gms.wallet.Cart;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.braintreepayments.api.utils.PaymentFormHelpers.submitAndWaitForCompletion;
import static com.braintreepayments.api.utils.PaymentFormHelpers.waitForAddPaymentFormHeader;
import static com.braintreepayments.api.utils.PaymentFormHelpers.waitForPaymentMethodNonceList;
import static com.braintreepayments.api.test.ActivityResultHelper.getActivityResult;
import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static com.braintreepayments.testutils.TestTokenizationKey.TOKENIZATION_KEY;
import static com.braintreepayments.testutils.ui.Matchers.withId;
import static com.braintreepayments.testutils.ui.ViewHelper.waitForView;
import static com.braintreepayments.api.test.WaitForActivityHelper.waitForActivityToFinish;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.startsWith;

public class PaymentRequestTest extends BraintreePaymentActivityTestRunner {

    @SmallTest
    @Test(timeout = 1000)
    public void includesAllOptions() {
        Cart cart = Cart.newBuilder()
                .setTotalPrice("5.00")
                .build();
        Intent intent = new PaymentRequest()
                .tokenizationKey(TOKENIZATION_KEY)
                .amount("1.00")
                .currencyCode("USD")
                .collectDeviceData(true)
                .androidPayCart(cart)
                .androidPayShippingAddressRequired(true)
                .androidPayPhoneNumberRequired(true)
                .androidPayRequestCode(1)
                .disableAndroidPay()
                .paypalAdditionalScopes(Collections.singletonList(PayPal.SCOPE_ADDRESS))
                .disablePayPal()
                .disableVenmo()
                .actionBarTitle("title")
                .actionBarLogo(com.braintreepayments.cardform.R.drawable.bt_amex)
                .primaryDescription("primary description")
                .secondaryDescription("secondary description")
                .submitButtonText("submit")
                .getIntent(getTargetContext());

        PaymentRequest paymentRequest = intent.getParcelableExtra(
                BraintreePaymentActivity.EXTRA_CHECKOUT_REQUEST);

        assertEquals(BraintreePaymentActivity.class.getName(), intent.getComponent().getClassName());
        assertEquals(TOKENIZATION_KEY, paymentRequest.getAuthorization());
        assertEquals("1.00", paymentRequest.getAmount());
        assertEquals("USD", paymentRequest.getCurrencyCode());
        assertTrue(paymentRequest.shouldCollectDeviceData());
        assertEquals("5.00", paymentRequest.getAndroidPayCart().getTotalPrice());
        assertTrue(paymentRequest.isAndroidPayShippingAddressRequired());
        assertTrue(paymentRequest.isAndroidPayPhoneNumberRequired());
        assertEquals(1, paymentRequest.getAndroidPayRequestCode());
        assertFalse(paymentRequest.isAndroidPayEnabled());
        assertEquals(Collections.singletonList(PayPal.SCOPE_ADDRESS), paymentRequest.getPayPalAdditionalScopes());
        assertFalse(paymentRequest.isPayPalEnabled());
        assertFalse(paymentRequest.isVenmoEnabled());
        assertEquals("title", paymentRequest.getActionBarTitle());
        assertEquals(com.braintreepayments.cardform.R.drawable.bt_amex, paymentRequest.getActionBarLogo());
        assertEquals("primary description", paymentRequest.getPrimaryDescription());
        assertEquals("secondary description", paymentRequest.getSecondaryDescription());
        assertEquals("submit", paymentRequest.getSubmitButtonText());
    }

    @SmallTest
    @Test(timeout = 1000)
    public void isParcelable() {
        Cart cart = Cart.newBuilder()
                .setTotalPrice("5.00")
                .build();
        PaymentRequest paymentRequest = new PaymentRequest()
                .tokenizationKey(TOKENIZATION_KEY)
                .amount("1.00")
                .currencyCode("USD")
                .collectDeviceData(true)
                .androidPayCart(cart)
                .androidPayShippingAddressRequired(true)
                .androidPayPhoneNumberRequired(true)
                .androidPayRequestCode(1)
                .disableAndroidPay()
                .paypalAdditionalScopes(Collections.singletonList(PayPal.SCOPE_ADDRESS))
                .disablePayPal()
                .disableVenmo()
                .actionBarTitle("title")
                .actionBarLogo(com.braintreepayments.cardform.R.drawable.bt_amex)
                .primaryDescription("primary description")
                .secondaryDescription("secondary description")
                .submitButtonText("submit");

        Parcel parcel = Parcel.obtain();
        paymentRequest.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        PaymentRequest parceledPaymentRequest = PaymentRequest.CREATOR.createFromParcel(parcel);

        assertEquals(TOKENIZATION_KEY, parceledPaymentRequest.getAuthorization());
        assertEquals("1.00", parceledPaymentRequest.getAmount());
        assertEquals("USD", parceledPaymentRequest.getCurrencyCode());
        assertTrue(parceledPaymentRequest.shouldCollectDeviceData());
        assertEquals("5.00", parceledPaymentRequest.getAndroidPayCart().getTotalPrice());
        assertTrue(parceledPaymentRequest.isAndroidPayShippingAddressRequired());
        assertTrue(parceledPaymentRequest.isAndroidPayPhoneNumberRequired());
        assertEquals(1, parceledPaymentRequest.getAndroidPayRequestCode());
        assertFalse(parceledPaymentRequest.isAndroidPayEnabled());
        assertEquals(Collections.singletonList(PayPal.SCOPE_ADDRESS), parceledPaymentRequest.getPayPalAdditionalScopes());
        assertFalse(parceledPaymentRequest.isPayPalEnabled());
        assertFalse(parceledPaymentRequest.isVenmoEnabled());
        assertEquals("title", parceledPaymentRequest.getActionBarTitle());
        assertEquals(com.braintreepayments.cardform.R.drawable.bt_amex,
                parceledPaymentRequest.getActionBarLogo());
        assertEquals("primary description", parceledPaymentRequest.getPrimaryDescription());
        assertEquals("secondary description", parceledPaymentRequest.getSecondaryDescription());
        assertEquals("submit", parceledPaymentRequest.getSubmitButtonText());
    }

    @SmallTest
    @Test(timeout = 1000)
    public void getIntent_includesClientToken() {
        PaymentRequest paymentRequest = new PaymentRequest()
                .clientToken(stringFromFixture("client_token.json"));

        assertEquals(stringFromFixture("client_token.json") , paymentRequest.getAuthorization());
    }

    @SmallTest
    @Test(timeout = 1000)
    public void getIntent_includesClientKey() {
        PaymentRequest paymentRequest = new PaymentRequest()
                .tokenizationKey(TOKENIZATION_KEY);

        assertEquals(TOKENIZATION_KEY, paymentRequest.getAuthorization());
    }

    @LargeTest
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

    @LargeTest
    @Test(timeout = 30000)
    public void descriptionIsNotNecessary() {
        Intent intent = new PaymentRequest()
                .tokenizationKey(TOKENIZATION_KEY)
                .getIntent(getTargetContext());
        getActivity(intent);

        waitForAddPaymentFormHeader();
        onView(withId(com.braintreepayments.api.dropin.R.id.bt_primary_description)).check(matches(not(isDisplayed())));
    }

    @LargeTest
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

    @LargeTest
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

    @LargeTest
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

    @LargeTest
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

    @LargeTest
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

    @LargeTest
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

    @LargeTest
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
