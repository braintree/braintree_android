package com.braintreepayments.api.dropin;

import android.app.Activity;
import android.view.KeyEvent;

import com.braintreepayments.api.Braintree;
import com.braintreepayments.api.TestClientTokenBuilder;
import com.braintreepayments.api.TestUtils;
import com.braintreepayments.api.exceptions.AuthenticationException;
import com.braintreepayments.api.exceptions.AuthorizationException;
import com.braintreepayments.api.exceptions.DownForMaintenanceException;
import com.braintreepayments.api.exceptions.ServerException;
import com.braintreepayments.api.exceptions.UnexpectedException;
import com.braintreepayments.api.exceptions.UpgradeRequiredException;

import java.util.Map;

import static com.braintreepayments.api.TestUtils.injectBraintree;
import static com.braintreepayments.api.TestUtils.setUpActivityTest;
import static com.braintreepayments.api.utils.TestHelper.waitForActivity;
import static com.braintreepayments.api.utils.ViewHelper.waitForView;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withId;

public class UnsuccessfulResultTest extends BraintreePaymentActivityTestCase {

    private Braintree mBraintree;
    private BraintreePaymentActivity mActivity;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        String clientToken = new TestClientTokenBuilder().withPayPal().build();
        mBraintree = injectBraintree(getInstrumentation().getContext(), clientToken);
        setUpActivityTest(this, clientToken);
        mActivity = getActivity();

        waitForView(withId(R.id.form_header));
    }

    public void testReturnsDeveloperErrorOnAuthenticationException() {
        TestUtils.postUnrecoverableErrorFromBraintree(mBraintree, new AuthenticationException());

        waitForActivity(mActivity);
        Map<String, Object> result = TestUtils.getActivityResult(mActivity);

        assertEquals(BraintreePaymentActivity.BRAINTREE_RESULT_DEVELOPER_ERROR,
                result.get("resultCode"));
    }

    public void testReturnsDeveloperErrorOnAuthorizationException() {
        TestUtils.postUnrecoverableErrorFromBraintree(mBraintree, new AuthorizationException());

        waitForActivity(mActivity);
        Map<String, Object> result = TestUtils.getActivityResult(mActivity);

        assertEquals(BraintreePaymentActivity.BRAINTREE_RESULT_DEVELOPER_ERROR,
                result.get("resultCode"));
    }

    public void testReturnsDeveloperErrorOnUpgradeRequiredException() {
        TestUtils.postUnrecoverableErrorFromBraintree(mBraintree, new UpgradeRequiredException());

        waitForActivity(mActivity);
        Map<String, Object> result = TestUtils.getActivityResult(mActivity);

        assertEquals(BraintreePaymentActivity.BRAINTREE_RESULT_DEVELOPER_ERROR,
                result.get("resultCode"));
    }

    public void testReturnsServerErrorOnServerException() {
        TestUtils.postUnrecoverableErrorFromBraintree(mBraintree, new ServerException());

        waitForActivity(mActivity);
        Map<String, Object> result = TestUtils.getActivityResult(mActivity);

        assertEquals(BraintreePaymentActivity.BRAINTREE_RESULT_SERVER_ERROR,
                result.get("resultCode"));
    }

    public void testReturnsServerUnavailableOnDownForMaintenanceException() {
        TestUtils.postUnrecoverableErrorFromBraintree(mBraintree, new DownForMaintenanceException());

        waitForActivity(mActivity);
        Map<String, Object> result = TestUtils.getActivityResult(mActivity);

        assertEquals(BraintreePaymentActivity.BRAINTREE_RESULT_SERVER_UNAVAILABLE,
                result.get("resultCode"));
    }

    public void testReturnsServerErrorOnUnexpectedException() {
        TestUtils.postUnrecoverableErrorFromBraintree(mBraintree, new UnexpectedException());

        waitForActivity(mActivity);
        Map<String, Object> result = TestUtils.getActivityResult(mActivity);

        assertEquals(BraintreePaymentActivity.BRAINTREE_RESULT_SERVER_ERROR,
                result.get("resultCode"));
    }

    public void testReturnsUserCanceledOnBackButtonPress() {
        sendKeys(KeyEvent.KEYCODE_BACK);

        waitForActivity(mActivity);
        Map<String, Object> result = TestUtils.getActivityResult(mActivity);

        assertEquals(Activity.RESULT_CANCELED, result.get("resultCode"));

    }
}
