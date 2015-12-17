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
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.test.BraintreePaymentActivityTestRunner;
import com.braintreepayments.testutils.TestClientTokenBuilder;

import org.json.JSONException;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.InstrumentationRegistry.getTargetContext;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static com.braintreepayments.api.utils.PaymentFormHelpers.waitForAddPaymentFormHeader;
import static com.braintreepayments.testutils.ActivityResultHelper.getActivityResult;
import static com.braintreepayments.testutils.ui.WaitForActivityHelper.waitForActivityToFinish;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

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
        assertEquals("Client token was invalid",
                ((InvalidArgumentException) exception).getMessage());
    }

    @Test(timeout = 30000)
    public void returnsInvalidArgumentExceptionOnInvalidTokenizationKey() {
        mActivity = getActivity("notAnEnv_abcde_merchantId");

        waitForActivityToFinish(mActivity);

        Map<String, Object> result = getActivityResult(mActivity);
        Object exception = ((Intent) result.get("resultData"))
                .getSerializableExtra(BraintreePaymentActivity.EXTRA_ERROR_MESSAGE);
        assertEquals(BraintreePaymentActivity.BRAINTREE_RESULT_DEVELOPER_ERROR, result.get("resultCode"));
        assertTrue(exception instanceof InvalidArgumentException);
        assertEquals("Tokenization Key contained invalid environment",
                ((InvalidArgumentException) exception).getMessage());
    }

    @Test(timeout = 30000)
    public void returnsServerErrorOnConfigurationException() {
        Intent intent = new PaymentRequest()
                .clientToken(new TestClientTokenBuilder().build())
                .getIntent(getTargetContext())
                .putExtra(BraintreePaymentTestActivity.CONFIGURATION_ERROR,
                        new UnexpectedException("Configuration Error"));
        mActivity = getActivity(intent);

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
    public void returnsDeveloperErrorOnAuthenticationException() throws InterruptedException {
        setupActivityWithBraintree();

        assertExceptionIsReturned(BraintreePaymentActivity.BRAINTREE_RESULT_DEVELOPER_ERROR,
                new AuthenticationException(""));
    }

    @Test(timeout = 30000)
    public void returnsDeveloperErrorOnAuthorizationException() throws InterruptedException {
        setupActivityWithBraintree();

        assertExceptionIsReturned(BraintreePaymentActivity.BRAINTREE_RESULT_DEVELOPER_ERROR,
                new AuthorizationException(""));
    }

    @Test(timeout = 30000)
    public void returnsDeveloperErrorOnUpgradeRequiredException() throws InterruptedException {
        setupActivityWithBraintree();

        assertExceptionIsReturned(BraintreePaymentActivity.BRAINTREE_RESULT_DEVELOPER_ERROR,
                new UpgradeRequiredException(""));
    }

    @Test(timeout = 30000)
    public void returnsServerErrorOnServerException() throws InterruptedException {
        setupActivityWithBraintree();

        assertExceptionIsReturned(BraintreePaymentActivity.BRAINTREE_RESULT_SERVER_ERROR,
                new ServerException(""));
    }

    @Test(timeout = 30000)
    public void returnsServerUnavailableOnDownForMaintenanceException()
            throws InterruptedException {
        setupActivityWithBraintree();

        assertExceptionIsReturned(BraintreePaymentActivity.BRAINTREE_RESULT_SERVER_UNAVAILABLE,
                new DownForMaintenanceException(""));
    }

    @Test(timeout = 30000)
    public void returnsServerErrorOnUnexpectedException() throws InterruptedException {
        setupActivityWithBraintree();

        assertExceptionIsReturned(BraintreePaymentActivity.BRAINTREE_RESULT_SERVER_ERROR,
                new UnexpectedException(""));
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

        String clientToken = new TestClientTokenBuilder().withAnalytics().build();
        Configuration configuration = null;
        try {
            configuration = Configuration.fromJson(clientToken);
        } catch (JSONException e) {
            e.printStackTrace();
            fail("Unable to generate client token");
        }
        mFragment = spy(mFragment);
        when(mFragment.getConfiguration()).thenReturn(configuration);
    }

    private void assertExceptionIsReturned(final int resultCode, final Exception exception)
            throws InterruptedException {
        waitForAddPaymentFormHeader().check(matches(isDisplayed()));

        final CountDownLatch latch = new CountDownLatch(1);
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mFragment.postCallback(exception);

                waitForActivityToFinish(mActivity);
                Map<String, Object> result = getActivityResult(mActivity);

                assertEquals(resultCode, result.get("resultCode"));
                assertEquals(exception, ((Intent) result.get("resultData"))
                        .getSerializableExtra(BraintreePaymentActivity.EXTRA_ERROR_MESSAGE));
                latch.countDown();
            }
        });

        latch.await();
    }
}
