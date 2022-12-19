package com.braintreepayments.demo.test;

import androidx.preference.PreferenceManager;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import com.braintreepayments.demo.test.utilities.TestHelper;

import org.junit.Before;
import org.junit.Ignore;
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
import static com.braintreepayments.testutils.CardNumber.THREE_D_SECURE_AUTHENTICATION_FAILED;
import static com.braintreepayments.testutils.CardNumber.THREE_D_SECURE_ISSUER_DOES_NOT_PARTICIPATE;
import static com.braintreepayments.testutils.CardNumber.THREE_D_SECURE_ISSUER_DOWN;
import static com.braintreepayments.testutils.CardNumber.THREE_D_SECURE_LOOKUP_ERROR;
import static com.braintreepayments.testutils.CardNumber.THREE_D_SECURE_LOOKUP_TIMEOUT;
import static com.braintreepayments.testutils.CardNumber.THREE_D_SECURE_MPI_SERVICE_ERROR;
import static com.braintreepayments.testutils.CardNumber.THREE_D_SECURE_SIGNATURE_VERIFICATION_FAILURE;
import static com.braintreepayments.testutils.CardNumber.THREE_D_SECURE_VERIFICATON;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.notNullValue;

@RunWith(AndroidJUnit4ClassRunner.class)
@Ignore("TODO: determine if 3DS v1 tests are still needed")
public class ThreeDSecureVerificationTest extends TestHelper {

    @Before
    public void setup() {
        super.setup();
        onDevice(withText("Credit or Debit Cards")).waitForEnabled().perform(click());

        PreferenceManager.getDefaultSharedPreferences(ApplicationProvider.getApplicationContext())
                .edit()
                .putBoolean("enable_three_d_secure", true)
                .commit();
    }

    @Test(timeout = 40000)
    public void threeDSecure_authenticates() {
        onDevice(withText("Card Number")).perform(setText(THREE_D_SECURE_VERIFICATON));
        fillInExpiration();
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
        onDevice(withText("Card Number")).perform(setText(THREE_D_SECURE_AUTHENTICATION_FAILED));
        fillInExpiration();
        onDevice(withText("CVV")).perform(setText("123"));
        onDevice(withText("Postal Code")).perform(setText("12345"));
        onDevice(withText("Purchase")).perform(click());

        enterThreeDSecurePasswordAndReturnToApp();

        onDevice(withTextContaining("Failed to authenticate, please try a different form of payment")).waitForExists();
    }

    @Test(timeout = 40000)
    public void threeDSecure_lookupError() {
        onDevice(withText("Card Number")).perform(setText(THREE_D_SECURE_LOOKUP_ERROR));
        fillInExpiration();
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
        fillInExpiration();
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
        onDevice(withText("Card Number")).perform(setText(THREE_D_SECURE_SIGNATURE_VERIFICATION_FAILURE));
        fillInExpiration();
        onDevice(withText("CVC")).perform(setText("123"));
        onDevice(withText("Postal Code")).perform(setText("12345"));
        onDevice(withText("Purchase")).perform(click());

        enterThreeDSecurePasswordAndReturnToApp();

        onDevice(withTextContaining("Failed to authenticate, please try a different form of payment")).waitForExists();
    }

    @Test(timeout = 40000)
    public void threeDSecure_whenRequired_requestsAuthentication() {
        onDevice(withText("Card Number")).perform(setText(THREE_D_SECURE_VERIFICATON));
        fillInExpiration();
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
        onDevice(withText("Card Number")).perform(setText(THREE_D_SECURE_MPI_SERVICE_ERROR));
        fillInExpiration();
        onDevice(withText("CVV")).perform(setText("123"));
        onDevice(withText("Postal Code")).perform(setText("12345"));
        onDevice(withText("Purchase")).perform(click());

        enterThreeDSecurePasswordAndReturnToApp();

        onDevice(withTextContaining("An unexpected error occurred")).waitForExists();
    }

    @Test(timeout = 40000)
    public void threeDSecure_whenWebViewIsClosed_callsCancelListener() {
        onDevice(withText("Card Number")).perform(setText(THREE_D_SECURE_VERIFICATON));
        fillInExpiration();
        onDevice(withText("CVV")).perform(setText("123"));
        onDevice(withText("Postal Code")).perform(setText("12345"));
        onDevice(withText("Purchase")).perform(click());

        onDevice(withText("Authentication")).waitForExists();
        onDevice(withContentDescription("Close tab")).perform(click());

        onDevice(withText("3DS canceled")).waitForExists();
    }

    @Test(timeout = 40000)
    public void threeDSecure_whenBackIsPressed_callsCancelListener() {
        onDevice(withText("Card Number")).perform(setText(THREE_D_SECURE_VERIFICATON));
        fillInExpiration();
        onDevice(withText("CVV")).perform(setText("123"));
        onDevice(withText("Postal Code")).perform(setText("12345"));
        onDevice(withText("Purchase")).perform(click());

        onDevice(withText("Authentication")).waitForExists();
        onDevice().pressBack();

        onDevice(withText("3DS canceled")).waitForExists();
    }

    @Test(timeout = 40000)
    public void threeDSecure_whenIssuerDown_returnsAnUnexpectedError() {
        onDevice(withText("Card Number")).perform(setText(THREE_D_SECURE_ISSUER_DOWN));
        fillInExpiration();
        onDevice(withText("CVV")).perform(setText("123"));
        onDevice(withText("Postal Code")).perform(setText("12345"));
        onDevice(withText("Purchase")).perform(click());

        enterThreeDSecurePasswordAndReturnToApp();

        onDevice(withTextContaining("An unexpected error occurred")).waitForExists();
    }

    @Test(timeout = 50000)
    public void threeDSecure_doesALookupAndReturnsACardAfterATimeout() {
        onDevice(withText("Card Number")).perform(setText(THREE_D_SECURE_LOOKUP_TIMEOUT));
        fillInExpiration();
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
        fillInExpiration();
        onDevice(withText("CVV")).perform(setText("123"));
        onDevice(withText("Postal Code")).perform(setText("12345"));
        onDevice(withText("Purchase")).perform(click());

        onDevice(withText("Authentication")).waitForExists();
        onDevice().typeText("1234");
        onDevice(withText("Submit")).perform(click());

        onDevice(withText("wasVerified: true")).exists();
    }

    private void enterThreeDSecurePasswordAndReturnToApp() {
        onDevice(withText("Authentication")).waitForExists();
        onDevice().typeText("1234");
        onDevice(withText("Submit")).perform(click());
    }
}
