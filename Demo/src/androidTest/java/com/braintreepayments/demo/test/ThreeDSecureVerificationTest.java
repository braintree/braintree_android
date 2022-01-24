package com.braintreepayments.demo.test;

import androidx.preference.PreferenceManager;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import com.braintreepayments.demo.test.utilities.TestHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.braintreepayments.AutomatorAction.click;
import static com.braintreepayments.AutomatorAction.setText;
import static com.braintreepayments.AutomatorAssertion.text;
import static com.braintreepayments.DeviceAutomator.onDevice;
import static com.braintreepayments.UiObjectMatcher.withContentDescription;
import static com.braintreepayments.UiObjectMatcher.withText;
import static com.braintreepayments.UiObjectMatcher.withTextContaining;
import static com.braintreepayments.UiObjectMatcher.withTextStartingWith;
import static com.braintreepayments.api.CardNumber.THREE_D_SECURE_AUTHENTICATION_FAILED;
import static com.braintreepayments.api.CardNumber.THREE_D_SECURE_ISSUER_DOES_NOT_PARTICIPATE;
import static com.braintreepayments.api.CardNumber.THREE_D_SECURE_ISSUER_DOWN;
import static com.braintreepayments.api.CardNumber.THREE_D_SECURE_LOOKUP_ERROR;
import static com.braintreepayments.api.CardNumber.THREE_D_SECURE_LOOKUP_TIMEOUT;
import static com.braintreepayments.api.CardNumber.THREE_D_SECURE_MPI_SERVICE_ERROR;
import static com.braintreepayments.api.CardNumber.THREE_D_SECURE_SIGNATURE_VERIFICATION_FAILURE;
import static com.braintreepayments.api.CardNumber.THREE_D_SECURE_VERIFICATON;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.endsWith;

@RunWith(AndroidJUnit4ClassRunner.class)
public class ThreeDSecureVerificationTest extends TestHelper {

    @Before
    public void setup() {
        super.setup();
        PreferenceManager.getDefaultSharedPreferences(ApplicationProvider.getApplicationContext())
                .edit()
                .putBoolean("enable_three_d_secure", true)
                .commit();
        onDevice(withText("Credit or Debit Cards")).waitForEnabled().perform(click());
    }

    @Test(timeout = 40000)
    public void threeDSecure_authenticates() {
        // fails
        onDevice(withText("Card Number")).perform(setText(THREE_D_SECURE_VERIFICATON));
        onDevice(withText("Expiration Date")).perform(setText(validExpirationText()));
        onDevice(withText("CVV")).perform(setText("123"));
        onDevice(withText("Postal Code")).perform(setText("12345"));
        onDevice(withText("Purchase")).perform(click());

        enterThreeDSecurePasswordAndReturnToApp();

        getNonceDetails().check(text(containsString("Card Last Two: 02")));
        getNonceDetails().check(text(containsString("isLiabilityShifted: true")));
        getNonceDetails().check(text(containsString("isLiabilityShiftPossible: true")));

        onDevice(withText("Create a Transaction")).perform(click());
        onDevice(withTextStartingWith("created")).check(text(endsWith("authorized")));
    }

    @Test(timeout = 40000)
    public void threeDSecure_authenticationFailed() {
        // fails
        onDevice(withText("Card Number")).perform(setText(THREE_D_SECURE_AUTHENTICATION_FAILED));
        onDevice(withText("Expiration Date")).perform(setText(validExpirationText()));
        onDevice(withText("CVV")).perform(setText("123"));
        onDevice(withText("Postal Code")).perform(setText("12345"));
        onDevice(withText("Purchase")).perform(click());

        enterThreeDSecurePasswordAndReturnToApp();

        onDevice(withTextStartingWith("An error occurred")).check(text(containsString("Failed to authenticate, please try a different form of payment")));
    }

    @Test(timeout = 40000)
    public void threeDSecure_lookupError() {
        onDevice(withText("Card Number")).perform(setText(THREE_D_SECURE_LOOKUP_ERROR));
        onDevice(withText("Expiration Date")).perform(setText(validExpirationText()));
        onDevice(withText("CVV")).perform(setText("123"));
        onDevice(withText("Postal Code")).perform(setText("12345"));
        onDevice(withText("Purchase")).perform(click());

        getNonceDetails().check(text(containsString("Card Last Two: 77")));
        getNonceDetails().check(text(containsString("isLiabilityShifted: false")));
        getNonceDetails().check(text(containsString("isLiabilityShiftPossible: false")));

        onDevice(withText("Create a Transaction")).perform(click());
        onDevice(withTextStartingWith("created")).check(text(endsWith("authorized")));
    }

    @Test(timeout = 40000)
    public void threeDSecure_whenIssuerDoesNotParticipate_returnsASuccessfulAuthentication() {
        onDevice(withText("Card Number")).perform(setText(THREE_D_SECURE_ISSUER_DOES_NOT_PARTICIPATE));
        onDevice(withText("Expiration Date")).perform(setText(validExpirationText()));
        onDevice(withText("CVV")).perform(setText("123"));
        onDevice(withText("Postal Code")).perform(setText("12345"));
        onDevice(withText("Purchase")).perform(click());

        getNonceDetails().check(text(containsString("Card Last Two: 01")));
        getNonceDetails().check(text(containsString("isLiabilityShifted: true")));
        getNonceDetails().check(text(containsString("isLiabilityShiftPossible: true")));
        getNonceDetails().check(text(containsString("wasVerified: true")));

        onDevice(withText("Create a Transaction")).perform(click());
        onDevice(withTextStartingWith("created")).check(text(endsWith("authorized")));
    }

    @Test(timeout = 40000)
    public void threeDSecure_whenSignatureVerificationFails_returnsAFailedAuthentication() {
        // fails
        onDevice(withText("Card Number")).perform(setText(THREE_D_SECURE_SIGNATURE_VERIFICATION_FAILURE));
        onDevice(withText("Expiration Date")).perform(setText(validExpirationText()));
        onDevice(withText("CID")).perform(setText("1234"));
        onDevice(withText("Postal Code")).perform(setText("12345"));
        onDevice(withText("Purchase")).perform(click());

        enterThreeDSecurePasswordAndReturnToApp();

        onDevice(withTextStartingWith("An error occurred")).check(text(containsString("Failed to authenticate, please try a different form of payment")));
    }

    @Test(timeout = 40000)
    public void threeDSecure_whenRequired_requestsAuthentication() {
        // fails
        onDevice(withText("Card Number")).perform(setText(THREE_D_SECURE_VERIFICATON));
        onDevice(withText("Expiration Date")).perform(setText(validExpirationText()));
        onDevice(withText("CVV")).perform(setText("123"));
        onDevice(withText("Postal Code")).perform(setText("12345"));
        onDevice(withText("Purchase")).perform(click());

        enterThreeDSecurePasswordAndReturnToApp();

        getNonceDetails().check(text(containsString("Card Last Two: 02")));
        getNonceDetails().check(text(containsString("isLiabilityShifted: true")));
        getNonceDetails().check(text(containsString("isLiabilityShiftPossible: true")));
        getNonceDetails().check(text(containsString("wasVerified: true")));

        onDevice(withText("Create a Transaction")).perform(click());
        onDevice(withTextStartingWith("created")).check(text(endsWith("authorized")));
    }

    @Test(timeout = 40000)
    public void threeDSecure_whenCardinalReturnsError_returnsAnError() {
        // fails
        onDevice(withText("Card Number")).perform(setText(THREE_D_SECURE_MPI_SERVICE_ERROR));
        onDevice(withText("Expiration Date")).perform(setText(validExpirationText()));
        onDevice(withText("CVV")).perform(setText("123"));
        onDevice(withText("Postal Code")).perform(setText("12345"));
        onDevice(withText("Purchase")).perform(click());

        enterThreeDSecurePasswordAndReturnToApp();

        onDevice(withTextStartingWith("An error occurred")).check(text(containsString("An unexpected error occurred")));
    }

    @Test(timeout = 40000)
    public void threeDSecure_whenWebViewIsClosed_callsCancelListener() {
        // fails
        onDevice(withText("Card Number")).perform(setText(THREE_D_SECURE_VERIFICATON));
        onDevice(withText("Expiration Date")).perform(setText(validExpirationText()));
        onDevice(withText("CVV")).perform(setText("123"));
        onDevice(withText("Postal Code")).perform(setText("12345"));
        onDevice(withText("Purchase")).perform(click());

        onDevice(withText("Authentication")).waitForExists();
        onDevice(withContentDescription("Close tab")).perform(click());

        onDevice(withTextStartingWith("An error occurred")).check(text(containsString("User canceled 3DS.")));
    }

    @Test(timeout = 40000)
    public void threeDSecure_whenBackIsPressed_callsCancelListener() {
        // fails
        onDevice(withText("Card Number")).perform(setText(THREE_D_SECURE_VERIFICATON));
        onDevice(withText("Expiration Date")).perform(setText(validExpirationText()));
        onDevice(withText("CVV")).perform(setText("123"));
        onDevice(withText("Postal Code")).perform(setText("12345"));
        onDevice(withText("Purchase")).perform(click());

        onDevice(withText("Authentication")).waitForExists();
        onDevice().pressBack();

        onDevice(withTextStartingWith("An error occurred")).check(text(containsString("User canceled 3DS.")));
    }

    @Test(timeout = 40000)
    public void threeDSecure_whenIssuerDown_returnsAnUnexpectedError() {
        onDevice(withText("Card Number")).perform(setText(THREE_D_SECURE_ISSUER_DOWN));
        onDevice(withText("Expiration Date")).perform(setText(validExpirationText()));
        onDevice(withText("CVV")).perform(setText("123"));
        onDevice(withText("Postal Code")).perform(setText("12345"));
        onDevice(withText("Purchase")).perform(click());

        enterThreeDSecurePasswordAndReturnToApp();

        onDevice(withTextStartingWith("An error occurred")).check(text(containsString("An unexpected error occurred")));
    }

    @Test(timeout = 50000)
    public void threeDSecure_doesALookupAndReturnsACardAfterATimeout() {
        onDevice(withText("Card Number")).perform(setText(THREE_D_SECURE_LOOKUP_TIMEOUT));
        onDevice(withText("Expiration Date")).perform(setText(validExpirationText()));
        onDevice(withText("CVV")).perform(setText("123"));
        onDevice(withText("Postal Code")).perform(setText("12345"));
        onDevice(withText("Purchase")).perform(click());

        onDevice(withText("Credit or Debit Cards")).waitForExists(20000);

        getNonceDetails().check(text(containsString("Card Last Two: 44")));
        getNonceDetails().check(text(containsString("isLiabilityShifted: false")));
        getNonceDetails().check(text(containsString("isLiabilityShiftPossible: false")));
        getNonceDetails().check(text(containsString("wasVerified: true")));

        onDevice(withText("Create a Transaction")).perform(click());
        onDevice(withTextStartingWith("created")).check(text(endsWith("authorized")));
    }

    @Test
    public void threeDSecure_automaticallyRedirectsBackToAppWith3DSResult() {
        onDevice(withText("Card Number")).perform(setText(THREE_D_SECURE_VERIFICATON));
        onDevice(withText("Expiration Date")).perform(setText(validExpirationText()));
        onDevice(withText("CVV")).perform(setText("123"));
        onDevice(withText("Postal Code")).perform(setText("12345"));
        onDevice(withText("Purchase")).perform(click());

        onDevice(withText("Authentication")).waitForExists();
        onDevice().typeText("1234");
        onDevice(withText("Submit")).perform(click());

        onDevice(withTextStartingWith("Card Last Two")).check(text(containsString("wasVerified: true")));
    }

    private void enterThreeDSecurePasswordAndReturnToApp() {
        onDevice(withText("Authentication")).waitForExists();
        onDevice().typeText("1234");
        onDevice(withText("Submit")).perform(click());
    }
}
