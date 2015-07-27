package com.braintreepayments.api;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.test.suitebuilder.annotation.LargeTest;
import android.widget.ImageView;

import com.braintreepayments.api.dropin.Customization;
import com.braintreepayments.api.dropin.Customization.CustomizationBuilder;
import com.braintreepayments.api.test.BraintreePaymentActivityTestRunner;
import com.braintreepayments.testutils.TestClientTokenBuilder;

import org.junit.Test;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.braintreepayments.api.utils.PaymentFormHelpers.waitForAddPaymentFormHeader;
import static com.braintreepayments.api.utils.PaymentFormHelpers.waitForPaymentMethodList;
import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static com.braintreepayments.testutils.ui.Assertions.assertBitmapsEqual;
import static com.braintreepayments.testutils.ui.Matchers.withId;
import static com.braintreepayments.testutils.ui.ViewHelper.waitForView;
import static junit.framework.Assert.assertEquals;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.startsWith;

@LargeTest
public class CustomizationTest extends BraintreePaymentActivityTestRunner {

    @Test(timeout = 30000)
    public void descriptionIsNotNecessary() {
        getActivity(new TestClientTokenBuilder().build(), new Intent());

        waitForAddPaymentFormHeader();
        onView(withId(com.braintreepayments.api.dropin.R.id.bt_primary_description)).check(matches(not(isDisplayed())));
    }

    @Test(timeout = 30000)
    public void submitButtonUsesDefaultTextIfNoCustomizationProvided() {
        getActivity(new TestClientTokenBuilder().build(), new Intent());

        waitForAddPaymentFormHeader();
        onView(withId(com.braintreepayments.api.dropin.R.id.bt_card_form_submit_button)).check(matches(withText(
                com.braintreepayments.api.dropin.R.string.bt_default_submit_button_text)));
    }

    @Test(timeout = 30000)
    public void submitButtonUsesCustomizationForCardFormIfIncludedAsAnExtra() {
        Customization customization = new CustomizationBuilder()
                .submitButtonText("Subscribe")
                .amount("$19")
                .build();
        Intent intent = new Intent()
                .putExtra(BraintreePaymentActivity.EXTRA_CUSTOMIZATION, customization);
        getActivity(new TestClientTokenBuilder().build(), intent);

        waitForAddPaymentFormHeader();
        onView(withId(com.braintreepayments.api.dropin.R.id.bt_card_form_submit_button)).check(matches(withText("$19 - SUBSCRIBE")));
    }

    @Test(timeout = 30000)
    public void submitButtonUsesCustomizationForSelectPaymentMethodIfIncludedAsAnExtra() {
        Customization customization = new CustomizationBuilder()
                .submitButtonText("Subscribe")
                .amount("$19")
                .build();
        Intent intent = new Intent()
                .putExtra(BraintreePaymentActivity.EXTRA_CUSTOMIZATION, customization)
                .putExtra(BraintreePaymentTestActivity.GET_PAYMENT_METHODS,
                        stringFromFixture("responses/get_payment_methods_two_cards_response.json"));
        getActivity(new TestClientTokenBuilder().build(), intent);

        waitForPaymentMethodList();
        onView(withId(com.braintreepayments.api.dropin.R.id.bt_select_payment_method_submit_button)).check(matches(withText("$19 - SUBSCRIBE")));
    }

    @Test(timeout = 30000)
    public void descriptionsAreDisplayedIfIncludedAsAnExtra() {
        Customization customization = new CustomizationBuilder()
                .primaryDescription("Hello, World!")
                .secondaryDescription("Some stuffz")
                .amount("$1,000,000,000.00")
                .build();
        Intent intent = new Intent()
                .putExtra(BraintreePaymentActivity.EXTRA_CUSTOMIZATION, customization);
        getActivity(new TestClientTokenBuilder().build(), intent);

        waitForView(withId(com.braintreepayments.api.dropin.R.id.bt_primary_description)).check(matches(withText("Hello, World!")));
        onView(withId(com.braintreepayments.api.dropin.R.id.bt_secondary_description)).check(matches(withText("Some stuffz")));
        onView(withId(com.braintreepayments.api.dropin.R.id.bt_description_amount)).check(matches(withText("$1,000,000,000.00")));
        onView(withId(com.braintreepayments.api.dropin.R.id.bt_card_form_submit_button)).check(matches(withText(
                startsWith("$1,000,000,000.00"))));
    }

    @Test(timeout = 30000)
    public void defaultButtonTextIsUsedWhenCustomizationIsPresentWithoutSpecifyingButtonText() {
        Customization customization = new CustomizationBuilder()
                .amount("$19")
                .build();
        Intent intent = new Intent()
                .putExtra(BraintreePaymentActivity.EXTRA_CUSTOMIZATION, customization);
        getActivity(new TestClientTokenBuilder().build(), intent);

        waitForAddPaymentFormHeader();
        onView(withId(com.braintreepayments.api.dropin.R.id.bt_card_form_submit_button)).check(matches(withText("$19 - PURCHASE")));
    }

    @Test(timeout = 30000)
    public void actionBarTitleAndLogoAreUsedIfIncludedAsAnExtra() {
        Customization customization = new CustomizationBuilder()
                .actionBarTitle("This is a title")
                .actionBarLogo(android.R.drawable.ic_delete)
                .build();
        Intent intent = new Intent()
                .putExtra(BraintreePaymentActivity.EXTRA_CUSTOMIZATION, customization);
        Activity activity = getActivity(new TestClientTokenBuilder().build(), intent);

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
        Customization customization = new CustomizationBuilder()
                .primaryDescription("Description")
                .build();
        Intent intent = new Intent()
                .putExtra(BraintreePaymentActivity.EXTRA_CUSTOMIZATION, customization);
        Activity activity = getActivity(new TestClientTokenBuilder().build(), intent);

        assertEquals("Purchase", activity.getActionBar().getTitle());

        if (VERSION.SDK_INT < VERSION_CODES.LOLLIPOP) {
            ImageView actual = (ImageView) activity.findViewById(android.R.id.home);
            ColorDrawable expected = new ColorDrawable(getTargetContext().getResources().getColor(
                    android.R.color.transparent));
            assertEquals(actual.getDrawable().getOpacity(), expected.getOpacity());
        }
    }
}
