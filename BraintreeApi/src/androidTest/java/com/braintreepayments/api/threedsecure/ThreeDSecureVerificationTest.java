package com.braintreepayments.api.threedsecure;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.Build.VERSION_CODES;
import android.os.SystemClock;
import android.test.ActivityInstrumentationTestCase2;
import android.view.KeyEvent;

import com.braintreepayments.api.BraintreeApi;
import com.braintreepayments.api.exceptions.BraintreeException;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.models.CardBuilder;
import com.braintreepayments.api.models.ThreeDSecureAuthenticationResponse;
import com.braintreepayments.api.models.ThreeDSecureLookup;
import com.braintreepayments.api.ThreeDSecureAuthenticationTestActivity;
import com.braintreepayments.testutils.TestClientTokenBuilder;

import org.json.JSONException;

import java.util.Map;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static com.braintreepayments.testutils.ActivityResultHelper.getActivityResult;
import static com.braintreepayments.testutils.ui.Matchers.withId;
import static com.braintreepayments.testutils.ui.ViewHelper.waitForView;
import static com.braintreepayments.testutils.ui.WaitForActivityHelper.waitForActivityToFinish;

public class ThreeDSecureVerificationTest extends ActivityInstrumentationTestCase2<ThreeDSecureAuthenticationTestActivity> {

    private static final String TEST_AMOUNT = "5";

    private String mClientToken;
    private BraintreeApi mBraintreeApi;

    public ThreeDSecureVerificationTest() {
        super(ThreeDSecureAuthenticationTestActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mClientToken = new TestClientTokenBuilder().withThreeDSecure().build();
        mBraintreeApi = new BraintreeApi(getInstrumentation().getContext(), mClientToken);
    }

    @TargetApi(VERSION_CODES.HONEYCOMB)
    public void testReturnsWithStatusResultCanceledWhenUpIsPressed()
            throws ErrorWithResponse, BraintreeException, InterruptedException {
        Activity activity = startThreeDSecureTestActivity("4000000000000002");

        waitForView(withId(android.R.id.widget_frame));
        onView(withContentDescription("Navigate up")).perform(click());

        waitForActivityToFinish(activity);
        Map<String, Object> result = getActivityResult(activity);

        assertEquals(Activity.RESULT_CANCELED, result.get("resultCode"));
    }

    public void testReturnsWithStatusResultCanceledWhenBackIsPressedOnFirstPage()
            throws ErrorWithResponse, BraintreeException {
        Activity activity = startThreeDSecureTestActivity("4000000000000002");

        waitForView(withId(android.R.id.widget_frame));

        // wait for page to load
        SystemClock.sleep(7000);

        sendKeys(KeyEvent.KEYCODE_BACK);

        waitForActivityToFinish(activity);
        Map<String, Object> result = getActivityResult(activity);

        assertEquals(Activity.RESULT_CANCELED, result.get("resultCode"));
    }

    public void pendingReturnsWithStatusResultCanceledWhenUserGoesOnePageDeepAndPressesBackTwice()
            throws ErrorWithResponse, BraintreeException {
        Activity activity = startThreeDSecureTestActivity("4000000000000002");

        waitForView(withId(android.R.id.widget_frame));

        // wait for page to load and click a link
        SystemClock.sleep(10000);

        sendKeys(KeyEvent.KEYCODE_BACK);
        SystemClock.sleep(2000);
        sendKeys(KeyEvent.KEYCODE_BACK);

        waitForActivityToFinish(activity);
        Map<String, Object> result = getActivityResult(activity);

        assertEquals(Activity.RESULT_CANCELED, result.get("resultCode"));
    }

    public void testDoesALookupAndReturnsACardAndANullACSUrlWhenAuthenticationIsNotRequired()
            throws ErrorWithResponse, BraintreeException, JSONException {
        String nonce = mBraintreeApi.tokenize(new CardBuilder()
                .cardNumber("4000000000000051")
                .expirationDate("12/20"));

        ThreeDSecureLookup threeDSecureLookup = mBraintreeApi.threeDSecureLookup(nonce, TEST_AMOUNT);

        assertEquals("51", threeDSecureLookup.getCard().getLastTwo());
        assertTrue(threeDSecureLookup.getCard().getThreeDSecureInfo().isLiabilityShifted());
        assertTrue(threeDSecureLookup.getCard().getThreeDSecureInfo().isLiabilityShiftPossible());
        assertNull(threeDSecureLookup.getAcsUrl());
    }

    public void testDoesALookupAndReturnsACardWhenThereIsALookupError()
            throws ErrorWithResponse, BraintreeException, JSONException {
        String nonce = mBraintreeApi.tokenize(new CardBuilder()
                .cardNumber("4000000000000077")
                .expirationDate("12/20"));

        ThreeDSecureLookup threeDSecureLookup = mBraintreeApi.threeDSecureLookup(nonce, TEST_AMOUNT);

        assertEquals("77", threeDSecureLookup.getCard().getLastTwo());
    }

    public void pendingRequestsAuthenticationWhenRequired()
            throws ErrorWithResponse, BraintreeException {
        Activity activity = startThreeDSecureTestActivity("4000000000000002");

        // Enter password and click submit
        SystemClock.sleep(10000);

        waitForActivityToFinish(activity);
        Map<String, Object> result = getActivityResult(activity);
        ThreeDSecureAuthenticationResponse threeDSecureResponse = ((Intent) result.get("resultData"))
                .getParcelableExtra(ThreeDSecureWebViewActivity.EXTRA_THREE_D_SECURE_RESULT);

        assertEquals(Activity.RESULT_OK, result.get("resultCode"));
        assertTrue(threeDSecureResponse.isSuccess());
        assertEquals("02", threeDSecureResponse.getCard().getLastTwo());
    }

    public void pendingReturnsAnErrorWhenAuthenticationFails()
            throws ErrorWithResponse, BraintreeException {
        Activity activity = startThreeDSecureTestActivity("4000000000000028");

        // Enter password and click submit
        SystemClock.sleep(20000);

        waitForActivityToFinish(activity);
        Map<String, Object> result = getActivityResult(activity);
        ThreeDSecureAuthenticationResponse threeDSecureResponse = ((Intent) result.get("resultData"))
                .getParcelableExtra(ThreeDSecureWebViewActivity.EXTRA_THREE_D_SECURE_RESULT);
        ErrorWithResponse errors = new ErrorWithResponse(0, threeDSecureResponse.getErrors());

        assertEquals(Activity.RESULT_OK, result.get("resultCode"));
        assertFalse(threeDSecureResponse.isSuccess());
        assertEquals("Failed to authenticate, please try a different form of payment", errors.getMessage());
    }

    public void testReturnsASuccessfulAuthenticationWhenIssuerDoesNotParticipate()
            throws ErrorWithResponse, BraintreeException {
        Activity activity = startThreeDSecureTestActivity("4000000000000101");

        SystemClock.sleep(7000);

        waitForActivityToFinish(activity);
        Map<String, Object> result = getActivityResult(activity);
        ThreeDSecureAuthenticationResponse threeDSecureResponse = ((Intent) result.get("resultData"))
                .getParcelableExtra(ThreeDSecureWebViewActivity.EXTRA_THREE_D_SECURE_RESULT);

        assertEquals(Activity.RESULT_OK, result.get("resultCode"));
        assertTrue(threeDSecureResponse.isSuccess());
        assertEquals("01", threeDSecureResponse.getCard().getLastTwo());
    }

    public void pendingReturnsAFailedAuthenticationWhenSignatureVerificationFails()
            throws ErrorWithResponse, BraintreeException {
        Activity activity = startThreeDSecureTestActivity("4000000000000010");

        // Enter password and click submit
        SystemClock.sleep(10000);

        waitForActivityToFinish(activity);
        Map<String, Object> result = getActivityResult(activity);
        ThreeDSecureAuthenticationResponse threeDSecureResponse = ((Intent) result.get("resultData"))
                .getParcelableExtra(ThreeDSecureWebViewActivity.EXTRA_THREE_D_SECURE_RESULT);
        ErrorWithResponse errors = new ErrorWithResponse(0, threeDSecureResponse.getErrors());

        assertEquals(Activity.RESULT_OK, result.get("resultCode"));
        assertFalse(threeDSecureResponse.isSuccess());
        assertEquals("Failed to authenticate, please try a different form of payment", errors.getMessage());
    }

    public void pendingWhenIssuerIsDown()
            throws ErrorWithResponse, BraintreeException, JSONException {
        Activity activity = startThreeDSecureTestActivity("4000000000000036");

        // Click continue
        SystemClock.sleep(10000);

        waitForActivityToFinish(activity);
        Map<String, Object> result = getActivityResult(activity);
        ThreeDSecureAuthenticationResponse threeDSecureResponse = ((Intent) result.get("resultData"))
                .getParcelableExtra(ThreeDSecureWebViewActivity.EXTRA_THREE_D_SECURE_RESULT);

        assertEquals(Activity.RESULT_OK, result.get("resultCode"));
        assertFalse(threeDSecureResponse.isSuccess());
        assertNull(threeDSecureResponse.getCard());
    }

    public void pendingEarlyTerminationWhenCardinalReturnsError()
            throws ErrorWithResponse, BraintreeException {
        Activity activity = startThreeDSecureTestActivity("4000000000000093");

        // Enter password and click submit
        SystemClock.sleep(10000);

        waitForActivityToFinish(activity);
        Map<String, Object> result = getActivityResult(activity);
        ThreeDSecureAuthenticationResponse threeDSecureResponse = ((Intent) result.get("resultData"))
                .getParcelableExtra(ThreeDSecureWebViewActivity.EXTRA_THREE_D_SECURE_RESULT);

        assertEquals(Activity.RESULT_OK, result.get("resultCode"));
        assertFalse(threeDSecureResponse.isSuccess());
        assertNull(threeDSecureResponse.getCard());
    }

    /* helper */
    private Activity startThreeDSecureTestActivity(String cardNumber)
            throws ErrorWithResponse, BraintreeException {
        String nonce = mBraintreeApi.tokenize(new CardBuilder()
                .cardNumber(cardNumber)
                .expirationDate("12/30"));
        Intent intent = new Intent()
                .putExtra(ThreeDSecureAuthenticationTestActivity.EXTRA_CLIENT_TOKEN, mClientToken)
                .putExtra(ThreeDSecureAuthenticationTestActivity.EXTRA_NONCE, nonce)
                .putExtra(ThreeDSecureAuthenticationTestActivity.EXTRA_AMOUNT, TEST_AMOUNT);
        setActivityIntent(intent);

        return getActivity();
    }

}
