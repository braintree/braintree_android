package com.braintreepayments.api.dropin;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.widget.ImageView;
import android.widget.TextView;

import com.braintreepayments.api.dropin.Customization.CustomizationBuilder;
import com.braintreepayments.api.exceptions.BraintreeException;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.models.CardBuilder;
import com.braintreepayments.testutils.TestClientTokenBuilder;

import org.json.JSONException;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.braintreepayments.api.DropInTestUtils.create;
import static com.braintreepayments.api.TestDependencyInjector.injectBraintree;
import static com.braintreepayments.api.utils.PaymentFormHelpers.waitForAddPaymentFormHeader;
import static com.braintreepayments.api.utils.PaymentFormHelpers.waitForPaymentMethodList;
import static com.braintreepayments.testutils.CardNumber.VISA;
import static com.braintreepayments.testutils.ui.Assertions.assertBitmapsEqual;
import static com.braintreepayments.testutils.ui.Matchers.withId;
import static com.braintreepayments.testutils.ui.ViewHelper.waitForView;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.startsWith;

public class CustomizationTest extends BraintreePaymentActivityTestCase {

    public void testDescriptionIsNotNecessary() {
        Intent intent = createIntent();
        String clientToken = new TestClientTokenBuilder().build();
        intent.putExtra(BraintreePaymentActivity.EXTRA_CLIENT_TOKEN, clientToken);
        setActivityIntent(intent);
        getActivity();

        waitForAddPaymentFormHeader();
        onView(withId(R.id.bt_primary_description)).check(matches(not(isDisplayed())));
    }

    public void testSubmitButtonUsesDefaultTextIfNoCustomizationProvided() {
        Intent intent = createIntent();
        intent.putExtra(BraintreePaymentActivity.EXTRA_CLIENT_TOKEN, new TestClientTokenBuilder().build());
        setActivityIntent(intent);
        getActivity();

        waitForAddPaymentFormHeader();
        onView(withId(R.id.bt_card_form_submit_button)).check(matches(withText(R.string.bt_default_submit_button_text)));
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
        onView(withId(R.id.bt_card_form_submit_button)).check(matches(withText("$19 - SUBSCRIBE")));
    }

    public void testSubmitButtonUsesCustomizationForSelectPaymentMethodIfIncludedAsAnExtra()
            throws ErrorWithResponse, BraintreeException, JSONException, InterruptedException {
        String clientToken = new TestClientTokenBuilder().build();
        create(injectBraintree(mContext, clientToken), new CardBuilder()
                .cardNumber(VISA)
                .expirationDate("0819"));

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
        onView(withId(R.id.bt_select_payment_method_submit_button)).check(matches(withText("$19 - SUBSCRIBE")));
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

        waitForView(withId(R.id.bt_primary_description)).check(matches(withText("Hello, World!")));
        onView(withId(R.id.bt_secondary_description)).check(matches(withText("Some stuffz")));
        onView(withId(R.id.bt_description_amount)).check(matches(withText("$1,000,000,000.00")));
        onView(withId(R.id.bt_card_form_submit_button)).check(matches(withText(
                startsWith("$1,000,000,000.00"))));
    }

    public void testSecondaryDescriptionCanBeGreaterThanOneLine() throws InterruptedException {
        String actual = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Curabitur at sollicitudin tellus, sed pellentesque ante. Phasellus aliquam nec mauris ac tincidunt. Curabitur at sollicitudin tellus, sed pellentesque ante.";

        Intent intent = createIntent();
        String clientToken = new TestClientTokenBuilder().build();
        Customization customization = new CustomizationBuilder()
                .primaryDescription("Hello, World!")
                .secondaryDescription(actual)
                .amount("$1,000,000,000.00")
                .build();
        intent.putExtra(BraintreePaymentActivity.EXTRA_CLIENT_TOKEN, clientToken);
        intent.putExtra(BraintreePaymentActivity.EXTRA_CUSTOMIZATION, customization);
        setActivityIntent(intent);
        getActivity();
        waitForAddPaymentFormHeader();

        TextView secondaryDescription = (TextView) getActivity().findViewById(R.id.bt_secondary_description);
        int textHeight = secondaryDescription.getLineHeight();
        int height = secondaryDescription.getHeight();

        int approxVisibleLines = height / textHeight;
        assertEquals(3, approxVisibleLines);
    }

    public void testSecondaryDescriptionRespectsLineBreaks() throws InterruptedException {
        String actual = "Some stuff\nSome more stuff\nEven more yet";

        Intent intent = createIntent();
        String clientToken = new TestClientTokenBuilder().build();
        Customization customization = new CustomizationBuilder()
                .primaryDescription("Hello, World!")
                .secondaryDescription(actual)
                .amount("$1,000,000,000.00")
                .build();
        intent.putExtra(BraintreePaymentActivity.EXTRA_CLIENT_TOKEN, clientToken);
        intent.putExtra(BraintreePaymentActivity.EXTRA_CUSTOMIZATION, customization);
        setActivityIntent(intent);
        getActivity();
        waitForAddPaymentFormHeader();

        TextView secondaryDescription = (TextView) getActivity().findViewById(R.id.bt_secondary_description);
        int textHeight = secondaryDescription.getLineHeight();
        int height = secondaryDescription.getHeight();

        int approxVisibleLines = height / textHeight;
        assertEquals(3, approxVisibleLines);
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
        onView(withId(R.id.bt_card_form_submit_button)).check(matches(withText("$19 - PURCHASE")));
    }

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

        if (VERSION.SDK_INT < VERSION_CODES.LOLLIPOP) {
            ImageView actual = (ImageView) activity.findViewById(android.R.id.home);
            assertBitmapsEqual(actual.getDrawable(),
                    mContext.getResources().getDrawable(android.R.drawable.ic_delete)
            );
        }
    }

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

        if (VERSION.SDK_INT < VERSION_CODES.LOLLIPOP) {
            ImageView actual = (ImageView) activity.findViewById(android.R.id.home);
            ColorDrawable expected = new ColorDrawable(mContext.getResources().getColor(
                    android.R.color.transparent));
            assertEquals(actual.getDrawable().getOpacity(), expected.getOpacity());
        }
    }

    private Intent createIntent() {
        return new Intent(mContext, BraintreePaymentActivity.class);
    }

}
