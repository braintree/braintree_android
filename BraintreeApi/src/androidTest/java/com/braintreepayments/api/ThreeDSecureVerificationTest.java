package com.braintreepayments.api;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.Build.VERSION_CODES;
import android.os.SystemClock;
import android.test.ActivityInstrumentationTestCase2;

import com.braintreepayments.api.exceptions.BraintreeException;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.models.Card;
import com.braintreepayments.api.models.CardBuilder;
import com.braintreepayments.api.models.ThreeDSecureAuthenticationResponse;
import com.braintreepayments.api.test.ThreeDSecureAuthenticationTestActivity;
import com.braintreepayments.testutils.TestClientTokenBuilder;

import org.json.JSONException;

import java.util.Map;

import static com.braintreepayments.testutils.ActivityResultHelper.getActivityResult;
import static com.braintreepayments.testutils.ui.Matchers.withId;
import static com.braintreepayments.testutils.ui.ViewHelper.waitForView;
import static com.braintreepayments.testutils.ui.WaitForActivityHelper.waitForActivityToFinish;
import static com.google.android.apps.common.testing.ui.espresso.Espresso.onView;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.click;

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
        onView(withId(android.R.id.home)).perform(click());

        waitForActivityToFinish(activity);
        Map<String, Object> result = getActivityResult(activity);

        assertEquals(Activity.RESULT_CANCELED, result.get("resultCode"));
    }

    public void testDoesALookupAndReturnsACardWhenAuthenticationIsNotRequired()
            throws ErrorWithResponse, BraintreeException, JSONException {
        String nonce = mBraintreeApi.tokenize(new CardBuilder()
                .cardNumber("4000000000000051")
                .expirationDate("12/20"));

        Card card = mBraintreeApi.startThreeDSecureVerification(null, 0, nonce, TEST_AMOUNT);

        assertNotNull(card);
        assertEquals("51", card.getLastTwo());
    }

    public void testDoesALookupAndReturnsACardWhenThereIsALookupError()
            throws ErrorWithResponse, BraintreeException, JSONException {
        String nonce = mBraintreeApi.tokenize(new CardBuilder()
                .cardNumber("4000000000000077")
                .expirationDate("12/20"));

        Card card = mBraintreeApi.startThreeDSecureVerification(null, 0, nonce, TEST_AMOUNT);

        assertNotNull(card);
        assertEquals("77", card.getLastTwo());
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

        assertEquals(Activity.RESULT_OK, result.get("resultCode"));
        assertFalse(threeDSecureResponse.isSuccess());
        assertFalse(threeDSecureResponse.getThreeDSecureInfo().isLiabilityShifted());
        assertTrue(threeDSecureResponse.getThreeDSecureInfo().isLiabilityShiftPossible());
    }

    public void testReturnsASuccessfulAuthenticationWhenIssueDoesNotParticipate()
            throws ErrorWithResponse, BraintreeException {
        Activity activity = startThreeDSecureTestActivity("4000000000000101");

        SystemClock.sleep(5000);

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

        assertEquals(Activity.RESULT_OK, result.get("resultCode"));
        assertFalse(threeDSecureResponse.isSuccess());
        assertFalse(threeDSecureResponse.getThreeDSecureInfo().isLiabilityShifted());
        assertTrue(threeDSecureResponse.getThreeDSecureInfo().isLiabilityShiftPossible());
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
