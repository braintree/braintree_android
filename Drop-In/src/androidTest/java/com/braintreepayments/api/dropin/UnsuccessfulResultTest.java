package com.braintreepayments.api.dropin;

import android.app.Activity;
import android.content.Intent;
import android.view.KeyEvent;

import com.braintreepayments.api.Braintree;
import com.braintreepayments.api.exceptions.AuthenticationException;
import com.braintreepayments.api.exceptions.AuthorizationException;
import com.braintreepayments.api.exceptions.DownForMaintenanceException;
import com.braintreepayments.api.exceptions.ServerException;
import com.braintreepayments.api.exceptions.UnexpectedException;
import com.braintreepayments.api.exceptions.UpgradeRequiredException;
import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.internal.BraintreeHttpClient;
import com.braintreepayments.testutils.TestClientTokenBuilder;

import org.json.JSONException;

import java.util.Map;

import static com.braintreepayments.api.DropInTestUtils.postUnrecoverableErrorFromBraintree;
import static com.braintreepayments.api.DropInTestUtils.setBraintreeHttpClient;
import static com.braintreepayments.api.DropInTestUtils.setClientTokenExtraForTest;
import static com.braintreepayments.api.TestDependencyInjector.injectBraintree;
import static com.braintreepayments.testutils.ActivityResultHelper.getActivityResult;
import static com.braintreepayments.testutils.ui.Matchers.withId;
import static com.braintreepayments.testutils.ui.ViewHelper.waitForView;
import static com.braintreepayments.testutils.ui.WaitForActivityHelper.waitForActivityToFinish;

public class UnsuccessfulResultTest extends BraintreePaymentActivityTestCase {

    private Braintree mBraintree;
    private BraintreePaymentActivity mActivity;

    public void testReturnsServerErrorOnSetupException() throws JSONException {
        String clientToken = new TestClientTokenBuilder().build();
        Braintree braintree = injectBraintree(mContext, clientToken, null);
        setBraintreeHttpClient(braintree, new BraintreeHttpClient("") {
            @Override
            public void get(String path, HttpResponseCallback callback) {
                if (callback != null) {
                    callback.failure(new UnexpectedException("Mock HTTP request"));
                }
            }

            @Override
            public void post(String path, String data, HttpResponseCallback callback) {
                if (callback != null) {
                    callback.failure(new UnexpectedException("Mock HTTP request"));
                }
            }
        });

        setClientTokenExtraForTest(this, clientToken);
        mActivity = getActivity();

        waitForActivityToFinish(mActivity);
        Map<String, Object> result = getActivityResult(mActivity);

        Object exception = ((Intent) result.get("resultData"))
                .getSerializableExtra(BraintreePaymentActivity.EXTRA_ERROR_MESSAGE);
        assertEquals(BraintreePaymentActivity.BRAINTREE_RESULT_SERVER_ERROR,
                result.get("resultCode"));
        assertTrue(exception instanceof UnexpectedException);
        assertEquals("Mock HTTP request", ((UnexpectedException) exception).getMessage());
    }

    public void testReturnsDeveloperErrorOnAuthenticationException() throws JSONException {
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

    public void testReturnsDeveloperErrorOnAuthorizationException() throws JSONException {
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

    public void testReturnsDeveloperErrorOnUpgradeRequiredException() throws JSONException {
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

    public void testReturnsServerErrorOnServerException() throws JSONException {
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

    public void testReturnsServerUnavailableOnDownForMaintenanceException() throws JSONException {
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

    public void testReturnsServerErrorOnUnexpectedException() throws JSONException {
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

    public void testReturnsUserCanceledOnBackButtonPress() throws JSONException {
        setupActivityWithBraintree();
        sendKeys(KeyEvent.KEYCODE_BACK);

        waitForActivityToFinish(mActivity);
        Map<String, Object> result = getActivityResult(mActivity);

        assertEquals(Activity.RESULT_CANCELED, result.get("resultCode"));
    }

    /* helper */
    private void setupActivityWithBraintree() throws JSONException {
        String clientToken = new TestClientTokenBuilder().build();
        mBraintree = injectBraintree(mContext, clientToken, clientToken);
        setClientTokenExtraForTest(this, clientToken);
        mActivity = getActivity();

        waitForView(withId(R.id.bt_card_form_header));
    }
}
