package com.braintreepayments.demo.test;

import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiObjectNotFoundException;

import com.braintreepayments.demo.test.utilities.TestHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.lukekorth.deviceautomator.AutomatorAction.check;
import static com.lukekorth.deviceautomator.AutomatorAction.clearTextField;
import static com.lukekorth.deviceautomator.AutomatorAction.click;
import static com.lukekorth.deviceautomator.AutomatorAssertion.text;
import static com.lukekorth.deviceautomator.DeviceAutomator.onDevice;
import static com.lukekorth.deviceautomator.UiObjectMatcher.withContentDescription;
import static com.lukekorth.deviceautomator.UiObjectMatcher.withResourceId;
import static com.lukekorth.deviceautomator.UiObjectMatcher.withText;
import static com.lukekorth.deviceautomator.UiObjectMatcher.withTextStartingWith;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.StringEndsWith.endsWith;

@RunWith(AndroidJUnit4.class)
public class VisaCheckoutTest extends TestHelper {

    private static final String VISA_CHECKOUT_USERNAME = "no-reply-visa-checkout@getbraintree.com";
    private static final String VISA_CHECKOUT_PASSWORD = "12345678";

    @Before
    public void setup() {
        super.setup();
        onDevice(withText("Visa Checkout")).waitForEnabled().perform(click());
        onDevice(withContentDescription("Visa Checkout")).waitForExists().waitForEnabled().perform(click());
    }

    @Test(timeout = 60000)
    public void cancelsVisaCheckout_whenPressingBack() {
        if (onDevice(withText("New to Visa Checkout?")).exists()) {
            onDevice(withText("Sign In")).perform(click());
        }

        onDevice().pressBack(); // Dismiss keyboard
        onDevice().pressBack();
        onDevice(withText("OK")).perform(click());
        onDevice(withText("Reset")).waitForExists();
    }

    @Test(timeout = 60000)
    public void cancelsVisaCheckout_whenClickingUpButton() {
        onDevice(withResourceId("com.braintreepayments.demo:id/vco_header_right_btn")).perform(click());
        onDevice(withText("OK")).perform(click());
        onDevice(withText("Reset")).waitForExists();
    }

    @Test(timeout = 120000)
    public void tokenizesVisaCheckout() throws UiObjectNotFoundException {
        if (onDevice(withText("New to Visa Checkout?")).exists()) {
            onDevice(withText("Sign In")).perform(click());
        }

        try {
            onDevice(withResourceId("com.braintreepayments.demo:id/com_visa_checkout_cbSignInUsernamePreferences"))
                    .perform(check(false));
        } catch (RuntimeException ignored) {}

        onDevice(withContentDescription("Email or Mobile Number")).perform(click(), clearTextField());
        onDevice(withContentDescription("Email or Mobile Number")).typeText(VISA_CHECKOUT_USERNAME);
        onDevice(withContentDescription("Password")).perform(click());
        onDevice().typeText(VISA_CHECKOUT_PASSWORD);
        onDevice(withText("Sign In")).perform(click());
        onDevice(withText("Pay with")).waitForExists();
        onDevice(withText("Continue")).perform(click());
        onDevice(withText("Create a Transaction")).waitForExists();
        onDevice(withText("Nonce:")).waitForExists();
        getNonceDetails().check(text(containsString("First name: NoReply")),
                text(containsString("Last name: VisaCheckout")),
                text(containsString("User name: no-reply-visa-checkout@getbraintree.com")),
                text(containsString("Email: no-reply-visa-checkout@getbraintree.com")),
                text(containsString("Billing Address: NoReply VisaCheckout 123 Townsend Street  San Francisco 94107 CA US")),
                text(containsString("Shipping Address: NoReply VisaCheckout 123 Townsend Street  San Francisco 94107 CA US")));

        onDevice(withText("Create a Transaction")).perform(click());
        onDevice(withTextStartingWith("created")).check(text(endsWith("authorized")));
    }
}
