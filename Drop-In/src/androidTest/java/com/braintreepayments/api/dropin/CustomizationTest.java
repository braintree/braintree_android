package com.braintreepayments.api.dropin;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Build.VERSION_CODES;
import android.widget.ImageView;

import com.braintreepayments.api.BraintreeApi;
import com.braintreepayments.api.TestClientTokenBuilder;
import com.braintreepayments.api.dropin.Customization.CustomizationBuilder;
import com.braintreepayments.api.exceptions.BraintreeException;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.models.CardBuilder;

import static com.braintreepayments.api.BraintreeTestUtils.assertBitmapsEqual;
import static com.braintreepayments.api.ui.Matchers.withId;
import static com.braintreepayments.api.ui.ViewHelper.waitForView;
import static com.braintreepayments.api.utils.PaymentFormHelpers.waitForAddPaymentFormHeader;
import static com.braintreepayments.api.utils.PaymentFormHelpers.waitForPaymentMethodList;
import static com.google.android.apps.common.testing.ui.espresso.Espresso.onView;
import static com.google.android.apps.common.testing.ui.espresso.assertion.ViewAssertions.matches;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.isDisplayed;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.endsWith;

public class CustomizationTest extends BraintreePaymentActivityTestCase {

    public void testDescriptionIsNotNecessary() {
        Intent intent = createIntent();
        String clientToken = new TestClientTokenBuilder().build();
        intent.putExtra(BraintreePaymentActivity.EXTRA_CLIENT_TOKEN, clientToken);
        setActivityIntent(intent);
        getActivity();

        waitForAddPaymentFormHeader();
        onView(withId(R.id.primary_description)).check(matches(not(isDisplayed())));
    }

    public void testSubmitButtonUsesDefaultTextIfNoCustomizationProvided() {
        Intent intent = createIntent();
        intent.putExtra(BraintreePaymentActivity.EXTRA_CLIENT_TOKEN, new TestClientTokenBuilder().build());
        setActivityIntent(intent);
        getActivity();

        waitForAddPaymentFormHeader();
        onView(withId(R.id.card_form_complete_button)).check(matches(withText(R.string.default_submit_button_text)));
    }

    public void testSubmitButtonUsesCustomizationForCardFormIfIncludedAsAnExtra() {
        Intent intent = createIntent();
        Customization customization = new CustomizationBuilder()
                .submitButtonText("Subscribe")
                .amount("$19")
                .build();
        intent.putExtra(BraintreePaymentActivity.EXTRA_CLIENT_TOKEN, new TestClientTokenBuilder().build());
        intent.putExtra(BraintreePaymentActivity.EXTRA_CUSTOMIZATION, customization);
        setActivityIntent(intent);
        getActivity();

        waitForAddPaymentFormHeader();
        onView(withId(R.id.card_form_complete_button)).check(matches(withText("Subscribe - $19")));
    }

    public void testSubmitButtonUsesCustomizationForSelectPaymentMethodIfIncludedAsAnExtra()
            throws ErrorWithResponse, BraintreeException {
        String clientToken = new TestClientTokenBuilder().build();
        BraintreeApi api = new BraintreeApi(getInstrumentation().getContext(), clientToken);
        api.create(new CardBuilder()
                        .cardNumber("4111111111111111")
                        .expirationDate("08/19"));

        Intent intent = createIntent();
        Customization customization = new CustomizationBuilder()
                .submitButtonText("Subscribe")
                .amount("$19")
                .build();
        intent.putExtra(BraintreePaymentActivity.EXTRA_CLIENT_TOKEN, clientToken);
        intent.putExtra(BraintreePaymentActivity.EXTRA_CUSTOMIZATION, customization);
        setActivityIntent(intent);
        getActivity();

        waitForPaymentMethodList();
        onView(withId(R.id.select_payment_method_button)).check(matches(withText("Subscribe - $19")));
    }

    public void testDescriptionsAreDisplayedIfIncludedAsAnExtra() {
        Intent intent = createIntent();
        String clientToken = new TestClientTokenBuilder().build();
        Customization customization = new CustomizationBuilder()
                .primaryDescription("Hello, World!")
                .secondaryDescription("Some stuffz")
                .amount("$1,000,000,000.00")
                .build();
        intent.putExtra(BraintreePaymentActivity.EXTRA_CLIENT_TOKEN, clientToken);
        intent.putExtra(BraintreePaymentActivity.EXTRA_CUSTOMIZATION, customization);
        setActivityIntent(intent);
        getActivity();

        waitForView(withId(R.id.primary_description)).check(matches(withText("Hello, World!")));
        onView(withId(R.id.secondary_description)).check(matches(withText("Some stuffz")));
        onView(withId(R.id.description_amount)).check(matches(withText("$1,000,000,000.00")));
        onView(withId(R.id.card_form_complete_button)).check(matches(withText(
                endsWith("$1,000,000,000.00"))));
    }

    public void testDefaultButtonTextIsUsedWhenCustomizationIsPresentWithoutSpecifyingButtonText() {
        Intent intent = createIntent();
        String clientToken = new TestClientTokenBuilder().build();
        Customization customization = new CustomizationBuilder()
                .amount("$19")
                .build();
        intent.putExtra(BraintreePaymentActivity.EXTRA_CLIENT_TOKEN, clientToken);
        intent.putExtra(BraintreePaymentActivity.EXTRA_CUSTOMIZATION, customization);
        setActivityIntent(intent);
        getActivity();

        waitForAddPaymentFormHeader();
        onView(withId(R.id.card_form_complete_button)).check(matches(withText("Purchase - $19")));
    }

    @TargetApi(VERSION_CODES.HONEYCOMB)
    public void testActionBarTitleAndLogoAreUsedIfIncludedAsAnExtra() {
        Intent intent = createIntent();
        Customization customization = new CustomizationBuilder()
                .actionBarTitle("This is a title")
                .actionBarLogo(android.R.drawable.ic_delete)
                .build();
        intent.putExtra(BraintreePaymentActivity.EXTRA_CLIENT_TOKEN, new TestClientTokenBuilder().build());
        intent.putExtra(BraintreePaymentActivity.EXTRA_CUSTOMIZATION, customization);
        setActivityIntent(intent);
        Activity activity = getActivity();

        assertEquals("This is a title", activity.getActionBar().getTitle());

        ImageView actual = (ImageView) activity.findViewById(android.R.id.home);
        assertBitmapsEqual(actual.getDrawable(),
                getInstrumentation().getContext().getResources()
                        .getDrawable(android.R.drawable.ic_delete)
        );
    }

    @TargetApi(VERSION_CODES.HONEYCOMB)
    public void testDefaultActionBarTitleAndLogoAreUsedWhenCustomizationIsPresentWithoutSpecifyingTitleAndLogo() {
        Intent intent = createIntent();
        Customization customization = new CustomizationBuilder()
                .primaryDescription("Description")
                .build();
        intent.putExtra(BraintreePaymentActivity.EXTRA_CLIENT_TOKEN, new TestClientTokenBuilder().build());
        intent.putExtra(BraintreePaymentActivity.EXTRA_CUSTOMIZATION, customization);
        setActivityIntent(intent);
        Activity activity = getActivity();

        assertEquals("Purchase", activity.getActionBar().getTitle());
        ImageView actual = (ImageView) activity.findViewById(android.R.id.home);
        ColorDrawable expected = new ColorDrawable(getInstrumentation().getContext().getResources()
                        .getColor(android.R.color.transparent));
        assertEquals(actual.getDrawable().getOpacity(), expected.getOpacity());
    }

    private Intent createIntent() {
        return new Intent(getInstrumentation().getContext(), BraintreePaymentActivity.class);
    }

}
