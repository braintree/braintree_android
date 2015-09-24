package com.braintreepayments.api;

import android.app.Activity;
import android.app.Instrumentation.ActivityResult;
import android.content.Intent;
import android.net.Uri;
import android.os.Looper;
import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.support.test.filters.FlakyTest;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;

import com.braintreepayments.api.exceptions.ConfigurationException;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.BraintreeCancelListener;
import com.braintreepayments.api.interfaces.BraintreeErrorListener;
import com.braintreepayments.api.interfaces.ConfigurationListener;
import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.internal.BraintreeHttpClient;
import com.braintreepayments.api.models.Authorization;
import com.braintreepayments.api.models.ClientToken;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.PayPalCheckout;
import com.braintreepayments.api.test.TestActivity;
import com.braintreepayments.testutils.TestClientTokenBuilder;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;

import static android.support.test.espresso.intent.Intents.intending;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static com.braintreepayments.api.BraintreeFragmentTestUtils.getMockFragment;
import static com.braintreepayments.api.BraintreeFragmentTestUtils.verifyAnalyticsEvent;
import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;
import static org.hamcrest.Matchers.allOf;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
public class PayPalTest {

    @Rule
    public final IntentsTestRule<TestActivity> mActivityTestRule =
            new IntentsTestRule<>(TestActivity.class);

    private Activity mActivity;
    private CountDownLatch mLatch;

    @Before
    public void setUp() {
        mActivity = mActivityTestRule.getActivity();
        mLatch = new CountDownLatch(1);
    }

    @Test(timeout = 1000)
    @SmallTest
    @FlakyTest
    public void authorizeAccount_startsPayPal() throws JSONException, InterruptedException {
        Configuration configuration = Configuration.fromJson(
                stringFromFixture("configuration_with_offline_paypal.json"));
        final ArgumentCaptor<Intent> launchIntentCaptor = ArgumentCaptor.forClass(Intent.class);
        final BraintreeFragment fragment = getMockFragment(mActivity, configuration);

        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                doAnswer(new Answer() {
                    @Override
                    public Object answer(InvocationOnMock invocation) throws Throwable {
                        verify(fragment).startActivityForResult(launchIntentCaptor.capture(),
                                eq(PayPal.PAYPAL_AUTHORIZATION_REQUEST_CODE));
                        mLatch.countDown();
                        return null;
                    }
                }).when(fragment).startActivityForResult(any(Intent.class),
                        eq(PayPal.PAYPAL_AUTHORIZATION_REQUEST_CODE));

                // TODO: sometimes getActivity() returns null, and I don't know why
                if (fragment.getActivity() != null) {
                    PayPal.authorizeAccount(fragment);
                }
            }
        });
        mLatch.await();
    }

    @Test(timeout = 10000)
    @MediumTest
    public void checkout_cancelUrlTriggersCancelListener()
            throws JSONException, InterruptedException, InvalidArgumentException {
        Looper.prepare();
        Configuration configuration = Configuration.fromJson(
                stringFromFixture("configuration_with_offline_paypal.json"));
        final BraintreeFragment fragment = getMockFragment(mActivity, configuration);
        fragment.mHttpClient = new BraintreeHttpClient(
                ClientToken.fromString(new TestClientTokenBuilder().build())) {
            @Override
            public void post(String path, String data, HttpResponseCallback callback) {
                callback.success(stringFromFixture("paypal_hermes_response.json"));
            }
        };

        Intent returnIntent = new Intent();
        returnIntent.setData(Uri.parse("http://paypal.com/do/the/thing/canceled?token=canceled-token"));
        ActivityResult result = new ActivityResult(Activity.RESULT_OK, returnIntent);

        intending(allOf(hasAction(Intent.ACTION_VIEW))).respondWith(result);

        fragment.addListener(new BraintreeCancelListener() {
            @Override
            public void onCancel(int requestCode) {
                if (requestCode == PayPal.PAYPAL_AUTHORIZATION_REQUEST_CODE) {
                    mLatch.countDown();
                }
            }
        });
        PayPal.checkout(fragment, new PayPalCheckout(BigDecimal.ONE));

        mLatch.await();
    }

    @Test(timeout = 10000)
    @MediumTest
    public void checkout_resultCanceledTriggersCancelListener()
            throws JSONException, InterruptedException, InvalidArgumentException {
        Looper.prepare();
        Configuration configuration = Configuration.fromJson(
                stringFromFixture("configuration_with_offline_paypal.json"));
        final BraintreeFragment fragment = getMockFragment(mActivity, configuration);
        fragment.mHttpClient = new BraintreeHttpClient(
                ClientToken.fromString(new TestClientTokenBuilder().build())) {
            @Override
            public void post(String path, String data, HttpResponseCallback callback) {
                callback.success(stringFromFixture("paypal_hermes_response.json"));
            }
        };

        ActivityResult result = new ActivityResult(Activity.RESULT_CANCELED, new Intent());
        intending(allOf(hasAction(Intent.ACTION_VIEW))).respondWith(result);

        fragment.addListener(new BraintreeCancelListener() {
            @Override
            public void onCancel(int requestCode) {
                if (requestCode == PayPal.PAYPAL_AUTHORIZATION_REQUEST_CODE) {
                    mLatch.countDown();
                }
            }
        });
        PayPal.checkout(fragment, new PayPalCheckout(BigDecimal.ONE));
        mLatch.await();
    }

    @Test(timeout = 1000)
    @SmallTest
    public void authorizeAccount_sendsAnalyticsEvent()
            throws JSONException, InvalidArgumentException {
        Configuration configuration = Configuration.fromJson(
                stringFromFixture("configuration_with_offline_paypal.json"));
        Authorization clientToken = Authorization.fromString(stringFromFixture("client_token.json"));
        final BraintreeFragment fragment = getMockFragment(mActivity, configuration);
        doNothing().when(fragment).startActivityForResult(any(Intent.class), anyInt());
        doNothing().when(fragment).waitForConfiguration(any(ConfigurationListener.class));
        when(fragment.getAuthorization()).thenReturn(clientToken);

        // TODO: sometimes getActivity() returns null, and I don't know why
        if (fragment.getActivity() != null) {
            PayPal.authorizeAccount(fragment);
        }
        verifyAnalyticsEvent(fragment, "paypal.selected");
    }

    @Test(timeout = 1000)
    @SmallTest
    public void onActivityResult_postConfigurationExceptionWhenInvalid()
            throws JSONException, InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        Configuration configuration =
                Configuration.fromJson(stringFromFixture("configuration_with_analytics.json"));
        final BraintreeFragment fragment = getMockFragment(mActivity, configuration);

        fragment.addListener(new BraintreeErrorListener() {
            @Override
            public void onUnrecoverableError(Throwable throwable) {
                assertEquals(ConfigurationException.class, throwable.getClass());
                assertEquals("PayPal is disabled or configuration is invalid",
                        throwable.getMessage());
                latch.countDown();
            }

            @Override
            public void onRecoverableError(ErrorWithResponse error) {
                fail(error.getMessage());
            }
        });

        PayPal.authorizeAccount(fragment);
        latch.await();
    }
}
