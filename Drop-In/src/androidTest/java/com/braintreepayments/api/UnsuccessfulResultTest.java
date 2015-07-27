package com.braintreepayments.api;

import android.app.Activity;
import android.content.Intent;
import android.test.suitebuilder.annotation.LargeTest;
import android.view.KeyEvent;

import com.braintreepayments.api.exceptions.AuthenticationException;
import com.braintreepayments.api.exceptions.AuthorizationException;
import com.braintreepayments.api.exceptions.DownForMaintenanceException;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.exceptions.ServerException;
import com.braintreepayments.api.exceptions.UnexpectedException;
import com.braintreepayments.api.exceptions.UpgradeRequiredException;
import com.braintreepayments.api.test.BraintreePaymentActivityTestRunner;
import com.braintreepayments.testutils.TestClientTokenBuilder;

import org.junit.Test;

import java.util.Map;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static com.braintreepayments.testutils.ActivityResultHelper.getActivityResult;
import static com.braintreepayments.testutils.ui.Matchers.withId;
import static com.braintreepayments.testutils.ui.ViewHelper.waitForView;
import static com.braintreepayments.testutils.ui.WaitForActivityHelper.waitForActivityToFinish;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

@LargeTest
public class UnsuccessfulResultTest extends BraintreePaymentActivityTestRunner {

    private BraintreePaymentActivity mActivity;
    private BraintreeFragment mFragment;

    @Test(timeout = 30000)
    public void returnsInvalidArgumentExceptionOnInvalidClientToken() {
        mActivity = getActivity("no-json");

        waitForActivityToFinish(mActivity);

        Map<String, Object> result = getActivityResult(mActivity);
        Object exception = ((Intent) result.get("resultData"))
                .getSerializableExtra(BraintreePaymentActivity.EXTRA_ERROR_MESSAGE);
        assertEquals(BraintreePaymentActivity.BRAINTREE_RESULT_DEVELOPER_ERROR, result.get("resultCode"));
        assertTrue(exception instanceof InvalidArgumentException);
        assertEquals("Client token was invalid json. Value no-json of type java.lang.String cannot be converted to JSONObject",
                ((InvalidArgumentException) exception).getMessage());
    }

    @Test(timeout = 30000)
    public void returnsServerErrorOnConfigurationException() {
        Intent intent = new Intent()
                .putExtra(BraintreePaymentTestActivity.CONFIGURATION_ERROR, new UnexpectedException("Configuration Error"));
        mActivity = getActivity(new TestClientTokenBuilder().build(), intent);

        waitForActivityToFinish(mActivity);

        Map<String, Object> result = getActivityResult(mActivity);
        Object exception = ((Intent) result.get("resultData"))
                .getSerializableExtra(BraintreePaymentActivity.EXTRA_ERROR_MESSAGE);
        assertEquals(BraintreePaymentActivity.BRAINTREE_RESULT_SERVER_ERROR,
                result.get("resultCode"));
        assertTrue(exception instanceof UnexpectedException);
        assertEquals("Configuration Error", ((UnexpectedException) exception).getMessage());
    }

    @Test(timeout = 30000)
    public void returnsDeveloperErrorOnAuthenticationException() {
        setupActivityWithBraintree();
        AuthenticationException exception = new AuthenticationException();
        mFragment.postCallback(exception);

        waitForActivityToFinish(mActivity);
        Map<String, Object> result = getActivityResult(mActivity);

        assertEquals(BraintreePaymentActivity.BRAINTREE_RESULT_DEVELOPER_ERROR,
                result.get("resultCode"));
        assertEquals(exception, ((Intent) result.get("resultData"))
                .getSerializableExtra(BraintreePaymentActivity.EXTRA_ERROR_MESSAGE));
    }

    @Test(timeout = 30000)
    public void returnsDeveloperErrorOnAuthorizationException() {
        setupActivityWithBraintree();
        AuthorizationException exception = new AuthorizationException();
        mFragment.postCallback(exception);

        waitForActivityToFinish(mActivity);
        Map<String, Object> result = getActivityResult(mActivity);

        assertEquals(BraintreePaymentActivity.BRAINTREE_RESULT_DEVELOPER_ERROR,
                result.get("resultCode"));
        assertEquals(exception, ((Intent) result.get("resultData"))
                .getSerializableExtra(BraintreePaymentActivity.EXTRA_ERROR_MESSAGE));
    }

    @Test(timeout = 30000)
    public void returnsDeveloperErrorOnUpgradeRequiredException() {
        setupActivityWithBraintree();
        UpgradeRequiredException exception = new UpgradeRequiredException();
        mFragment.postCallback(exception);

        waitForActivityToFinish(mActivity);
        Map<String, Object> result = getActivityResult(mActivity);

        assertEquals(BraintreePaymentActivity.BRAINTREE_RESULT_DEVELOPER_ERROR,
                result.get("resultCode"));
        assertEquals(exception, ((Intent) result.get("resultData"))
                .getSerializableExtra(BraintreePaymentActivity.EXTRA_ERROR_MESSAGE));
    }

    @Test(timeout = 30000)
    public void returnsServerErrorOnServerException() {
        setupActivityWithBraintree();
        ServerException exception = new ServerException();
        mFragment.postCallback(exception);

        waitForActivityToFinish(mActivity);
        Map<String, Object> result = getActivityResult(mActivity);

        assertEquals(BraintreePaymentActivity.BRAINTREE_RESULT_SERVER_ERROR,
                result.get("resultCode"));
        assertEquals(exception, ((Intent) result.get("resultData"))
                .getSerializableExtra(BraintreePaymentActivity.EXTRA_ERROR_MESSAGE));
    }

    @Test(timeout = 30000)
    public void returnsServerUnavailableOnDownForMaintenanceException() {
        setupActivityWithBraintree();
        DownForMaintenanceException exception = new DownForMaintenanceException();
        mFragment.postCallback(exception);

        waitForActivityToFinish(mActivity);
        Map<String, Object> result = getActivityResult(mActivity);

        assertEquals(BraintreePaymentActivity.BRAINTREE_RESULT_SERVER_UNAVAILABLE,
                result.get("resultCode"));
        assertEquals(exception, ((Intent) result.get("resultData"))
                .getSerializableExtra(BraintreePaymentActivity.EXTRA_ERROR_MESSAGE));
    }

    @Test(timeout = 30000)
    public void returnsServerErrorOnUnexpectedException() {
        setupActivityWithBraintree();
        UnexpectedException exception = new UnexpectedException();
        mFragment.postCallback(exception);

        waitForActivityToFinish(mActivity);
        Map<String, Object> result = getActivityResult(mActivity);

        assertEquals(BraintreePaymentActivity.BRAINTREE_RESULT_SERVER_ERROR,
                result.get("resultCode"));
        assertEquals(exception, ((Intent) result.get("resultData"))
                .getSerializableExtra(BraintreePaymentActivity.EXTRA_ERROR_MESSAGE));
    }

    @Test(timeout = 30000)
    public void returnsUserCanceledOnBackButtonPress() {
        setupActivityWithBraintree();
        getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);

        waitForActivityToFinish(mActivity);
        Map<String, Object> result = getActivityResult(mActivity);
        assertEquals(Activity.RESULT_CANCELED, result.get("resultCode"));
    }

    /* helper */
    private void setupActivityWithBraintree() {
        mActivity = getActivity(new TestClientTokenBuilder().build());
        mFragment = mActivity.mBraintreeFragment;
        waitForView(withId(com.braintreepayments.api.dropin.R.id.bt_card_form_header));
    }
}
