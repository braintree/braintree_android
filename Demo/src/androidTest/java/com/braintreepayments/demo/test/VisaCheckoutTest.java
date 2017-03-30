package com.braintreepayments.demo.test;

import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiSelector;

import com.braintreepayments.demo.test.utilities.TestHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.lukekorth.deviceautomator.AutomatorAction.clearTextField;
import static com.lukekorth.deviceautomator.AutomatorAction.click;
import static com.lukekorth.deviceautomator.AutomatorAssertion.text;
import static com.lukekorth.deviceautomator.DeviceAutomator.onDevice;
import static com.lukekorth.deviceautomator.UiObjectMatcher.withContentDescription;
import static com.lukekorth.deviceautomator.UiObjectMatcher.withResourceId;
import static com.lukekorth.deviceautomator.UiObjectMatcher.withText;
import static org.hamcrest.Matchers.containsString;

@RunWith(AndroidJUnit4.class)
public class VisaCheckoutTest extends TestHelper {

    private static final String VISA_CHECKOUT_USERNAME = "no-reply-visa-checkout@getbraintree.com";
    private static final String VISA_CHECKOUT_PASSWORD = "12345678";

    @Before
    public void setup() {
        super.setup();
        onDevice(withText("Visa Checkout")).waitForEnabled().perform(click());
        onDevice(withContentDescription("Visa Checkout")).waitForExists().perform(click());
    }

    @Test(timeout = 60000)
    public void cancelsVisaCheckout_whenPressingBack() {
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

    @Test(timeout = 60000)
    public void tokenizesVisaCheckout() throws UiObjectNotFoundException {
        String rememberMeResourceId = "com.braintreepayments.demo:id/com_visa_checkout_cbSignInUsernamePreferences";

        if (new UiObject(new UiSelector().text("New To Visa Checkout?")).exists()) {
            // If we land in the sign up page, move to the login page.
            onDevice(withText("Sign In")).perform(click());
        }

        // TODO Device Automator doesn't check for checked inputs
        if (new UiObject(new UiSelector().resourceId(rememberMeResourceId)).isChecked()) {
            onDevice(withResourceId(rememberMeResourceId)).perform(click());
        }

        onDevice(withContentDescription("Email or Mobile Number")).perform(click(), clearTextField());
        onDevice(withContentDescription("Email or Mobile Number")).typeText(VISA_CHECKOUT_USERNAME);
        onDevice(withContentDescription("Password")).perform(click());
        onDevice().typeText(VISA_CHECKOUT_PASSWORD);
        onDevice().pressBack(); // Dismiss keyboard
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
    }
}
