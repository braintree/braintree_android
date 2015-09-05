package com.braintreepayments.api.dropin;

import android.app.Activity;
import android.content.Intent;
import android.view.KeyEvent;

import com.braintreepayments.api.Braintree;
import com.braintreepayments.api.exceptions.AuthenticationException;
import com.braintreepayments.api.exceptions.AuthorizationException;
import com.braintreepayments.api.exceptions.BraintreeException;
import com.braintreepayments.api.exceptions.DownForMaintenanceException;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.exceptions.ServerException;
import com.braintreepayments.api.exceptions.UnexpectedException;
import com.braintreepayments.api.exceptions.UpgradeRequiredException;
import com.braintreepayments.testutils.TestClientTokenBuilder;

import java.util.Map;

import static com.braintreepayments.api.BraintreeTestUtils.postUnrecoverableErrorFromBraintree;
import static com.braintreepayments.api.BraintreeTestUtils.setClientTokenExtraForTest;
import static com.braintreepayments.api.BraintreeTestUtils.unexpectedExceptionThrowingApi;
import static com.braintreepayments.api.TestDependencyInjector.injectBraintree;
import static com.braintreepayments.testutils.ActivityResultHelper.getActivityResult;
import static com.braintreepayments.testutils.ui.Matchers.withId;
import static com.braintreepayments.testutils.ui.ViewHelper.waitForView;
import static com.braintreepayments.testutils.ui.WaitForActivityHelper.waitForActivityToFinish;

public class UnsuccessfulResultTest extends BraintreePaymentActivityTestCase {

    private Braintree mBraintree;
    private BraintreePaymentActivity mActivity;

    public void testReturnsServerErrorOnSetupException()
            throws ErrorWithResponse, BraintreeException {
        injectBraintree("test_client_token", unexpectedExceptionThrowingApi(mContext));
        setClientTokenExtraForTest(this, "test_client_token");
        mActivity = getActivity();

        waitForActivityToFinish(mActivity);
        Map<String, Object> result = getActivityResult(mActivity);

        Object exception = ((Intent) result.get("resultData"))
                .getSerializableExtra(BraintreePaymentActivity.EXTRA_ERROR_MESSAGE);
        assertEquals(BraintreePaymentActivity.BRAINTREE_RESULT_SERVER_ERROR,
                result.get("resultCode"));
        assertTrue(exception instanceof UnexpectedException);
        assertEquals("Mocked HTTP request", ((UnexpectedException) exception).getMessage());
    }

    public void testReturnsDeveloperErrorOnAuthenticationException() {
        setupActivityWithBraintree();
        AuthenticationException exception = new AuthenticationException();
        postUnrecoverableErrorFromBraintree(mBraintree, exception);

        waitForActivityToFinish(mActivity);
        Map<String, Object> result = getActivityResult(mActivity);

        assertEquals(BraintreePaymentActivity.BRAINTREE_RESULT_DEVELOPER_ERROR,
                result.get("resultCode"));
        assertEquals(exception, ((Intent) result.get("resultData"))
                .getSerializableExtra(BraintreePaymentActivity.EXTRA_ERROR_MESSAGE));
    }

    public void testReturnsDeveloperErrorOnAuthorizationException() {
        setupActivityWithBraintree();
        AuthorizationException exception = new AuthorizationException();
        postUnrecoverableErrorFromBraintree(mBraintree, exception);

        waitForActivityToFinish(mActivity);
        Map<String, Object> result = getActivityResult(mActivity);

        assertEquals(BraintreePaymentActivity.BRAINTREE_RESULT_DEVELOPER_ERROR,
                result.get("resultCode"));
        assertEquals(exception, ((Intent) result.get("resultData"))
                .getSerializableExtra(BraintreePaymentActivity.EXTRA_ERROR_MESSAGE));
    }

    public void testReturnsDeveloperErrorOnUpgradeRequiredException() {
        setupActivityWithBraintree();
        UpgradeRequiredException exception = new UpgradeRequiredException();
        postUnrecoverableErrorFromBraintree(mBraintree, exception);

        waitForActivityToFinish(mActivity);
        Map<String, Object> result = getActivityResult(mActivity);

        assertEquals(BraintreePaymentActivity.BRAINTREE_RESULT_DEVELOPER_ERROR,
                result.get("resultCode"));
        assertEquals(exception, ((Intent) result.get("resultData"))
                .getSerializableExtra(BraintreePaymentActivity.EXTRA_ERROR_MESSAGE));
    }

    public void testReturnsServerErrorOnServerException() {
        setupActivityWithBraintree();
        ServerException exception = new ServerException();
        postUnrecoverableErrorFromBraintree(mBraintree, exception);

        waitForActivityToFinish(mActivity);
        Map<String, Object> result = getActivityResult(mActivity);

        assertEquals(BraintreePaymentActivity.BRAINTREE_RESULT_SERVER_ERROR,
                result.get("resultCode"));
        assertEquals(exception, ((Intent) result.get("resultData"))
                .getSerializableExtra(BraintreePaymentActivity.EXTRA_ERROR_MESSAGE));
    }

    public void testReturnsServerUnavailableOnDownForMaintenanceException() {
        setupActivityWithBraintree();
        DownForMaintenanceException exception = new DownForMaintenanceException();
        postUnrecoverableErrorFromBraintree(mBraintree, exception);

        waitForActivityToFinish(mActivity);
        Map<String, Object> result = getActivityResult(mActivity);

        assertEquals(BraintreePaymentActivity.BRAINTREE_RESULT_SERVER_UNAVAILABLE,
                result.get("resultCode"));
        assertEquals(exception, ((Intent) result.get("resultData"))
                .getSerializableExtra(BraintreePaymentActivity.EXTRA_ERROR_MESSAGE));
    }

    public void testReturnsServerErrorOnUnexpectedException() {
        setupActivityWithBraintree();
        UnexpectedException exception = new UnexpectedException();
        postUnrecoverableErrorFromBraintree(mBraintree, exception);

        waitForActivityToFinish(mActivity);
        Map<String, Object> result = getActivityResult(mActivity);

        assertEquals(BraintreePaymentActivity.BRAINTREE_RESULT_SERVER_ERROR,
                result.get("resultCode"));
        assertEquals(exception, ((Intent) result.get("resultData"))
                .getSerializableExtra(BraintreePaymentActivity.EXTRA_ERROR_MESSAGE));
    }

    public void testReturnsUserCanceledOnBackButtonPress() {
        setupActivityWithBraintree();
        sendKeys(KeyEvent.KEYCODE_BACK);

        waitForActivityToFinish(mActivity);
        Map<String, Object> result = getActivityResult(mActivity);

        assertEquals(Activity.RESULT_CANCELED, result.get("resultCode"));
    }

    /* helper */
    private void setupActivityWithBraintree() {
        String clientToken = new TestClientTokenBuilder().build();
        mBraintree = injectBraintree(mContext, clientToken, clientToken);
        setClientTokenExtraForTest(this, clientToken);
        mActivity = getActivity();

        waitForView(withId(R.id.bt_card_form_header));
    }
}
