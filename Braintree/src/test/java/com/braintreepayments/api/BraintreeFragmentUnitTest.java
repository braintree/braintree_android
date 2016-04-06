package com.braintreepayments.api;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.BraintreeCancelListener;
import com.braintreepayments.api.interfaces.BraintreeErrorListener;
import com.braintreepayments.api.interfaces.BraintreeResponseListener;
import com.braintreepayments.api.interfaces.ConfigurationListener;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCreatedListener;
import com.braintreepayments.api.interfaces.PaymentMethodNoncesUpdatedListener;
import com.braintreepayments.api.interfaces.QueuedCallback;
import com.braintreepayments.api.models.CardNonce;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.PayPalAccountNonce;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.testutils.FragmentTestActivity;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static com.braintreepayments.testutils.TestTokenizationKey.TOKENIZATION_KEY;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.powermock.api.mockito.PowerMockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(RobolectricGradleTestRunner.class)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*", "org.json.*" })
@PrepareForTest({ ConfigurationManager.class, AnalyticsManager.class, PayPal.class, ThreeDSecure.class, Venmo.class })
public class BraintreeFragmentUnitTest {

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    private Activity mActivity;
    private CountDownLatch mCountDownLatch;

    @Before
    public void setup() {
        mActivity = spy(Robolectric.setupActivity(FragmentTestActivity.class));
        doNothing().when(mActivity).startActivity(any(Intent.class));
        mCountDownLatch = new CountDownLatch(1);
    }

    @Test
    public void newInstance_returnsABraintreeFragmentFromATokenizationKey() throws InvalidArgumentException {
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity, TOKENIZATION_KEY);

        assertNotNull(fragment);
    }

    @Test
    public void newInstance_returnsABraintreeFragmentFromAClientToken() throws InvalidArgumentException {
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity, stringFromFixture("client_token.json"));

        assertNotNull(fragment);
    }

    @Test(expected = InvalidArgumentException.class)
    public void newInstance_throwsAnExceptionForABadTokenizationKey() throws InvalidArgumentException {
        BraintreeFragment.newInstance(mActivity, "test_key_merchant");
    }

    @Test(expected = InvalidArgumentException.class)
    public void newInstance_throwsAnExceptionForABadClientToken() throws InvalidArgumentException {
        BraintreeFragment.newInstance(mActivity, "{}");
    }

    @Test
    public void newInstance_returnsAnExistingInstance() throws InvalidArgumentException {
        BraintreeFragment fragment1 = BraintreeFragment.newInstance(mActivity, TOKENIZATION_KEY);
        BraintreeFragment fragment2 = BraintreeFragment.newInstance(mActivity, TOKENIZATION_KEY);

        assertEquals(fragment1, fragment2);
    }

    @Test
    public void newInstance_setsIntegrationTypeToCustomForAllActivities() throws InvalidArgumentException {
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity, TOKENIZATION_KEY);

        assertEquals("custom", fragment.mIntegrationType);
    }

    @Test
    public void sendsAnalyticsEventForTokenizationKey() throws InvalidArgumentException, JSONException {
        mockConfigurationManager(Configuration.fromJson(stringFromFixture("configuration_with_analytics.json")));
        mockStatic(AnalyticsManager.class);
        doNothing().when(AnalyticsManager.class);
        AnalyticsManager.sendRequest(any(BraintreeFragment.class), anyString(), anyString());

        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity, TOKENIZATION_KEY);

        verifyStatic();
        AnalyticsManager.sendRequest(fragment, "custom", "started.client-key");
    }

    @Test
    public void sendsAnalyticsEventForClientToken() throws InvalidArgumentException, JSONException {
        mockConfigurationManager(Configuration.fromJson(stringFromFixture("configuration_with_analytics.json")));
        mockStatic(AnalyticsManager.class);
        doNothing().when(AnalyticsManager.class);
        AnalyticsManager.sendRequest(any(BraintreeFragment.class), anyString(), anyString());

        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity, stringFromFixture("client_token.json"));

        verifyStatic();
        AnalyticsManager.sendRequest(fragment, "custom", "started.client-token");
    }

    @Test
    public void postsAnErrorWhenFetchingConfigurationFails() throws InvalidArgumentException, InterruptedException {
        mockConfigurationManager(new Exception("Configuration error"));
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity, TOKENIZATION_KEY);
        final CountDownLatch latch = new CountDownLatch(2);
        fragment.addListener(new BraintreeErrorListener() {
            @Override
            public void onError(Exception error) {
                assertEquals("Request for configuration has failed: Configuration error. Future requests will retry up to 3 times",
                        error.getMessage());
                latch.countDown();
            }
        });
        fragment.setConfigurationErrorListener(new BraintreeResponseListener<Exception>() {
            @Override
            public void onResponse(Exception error) {
                assertEquals("Request for configuration has failed: Configuration error. Future requests will retry up to 3 times",
                        error.getMessage());
                latch.countDown();
            }
        });

        latch.await();
    }

    @Test
    public void onSaveInstanceState_savesState() throws InvalidArgumentException {
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity, TOKENIZATION_KEY);
        Bundle bundle = new Bundle();

        fragment.onSaveInstanceState(bundle);

        assertTrue(bundle.getParcelableArrayList(BraintreeFragment.EXTRA_CACHED_PAYMENT_METHOD_NONCES).isEmpty());
        assertFalse(bundle.getBoolean(BraintreeFragment.EXTRA_FETCHED_PAYMENT_METHOD_NONCES));
        assertFalse(bundle.getBoolean(BraintreeFragment.EXTRA_BROWSER_SWITCHING));
    }

    @Test
    public void getContext_returnsContext() throws InvalidArgumentException {
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity, TOKENIZATION_KEY);

        assertEquals(mActivity.getApplicationContext(), fragment.getApplicationContext());
    }

    @Test
    public void getTokenizationKey_returnsTokenizationKey() throws InvalidArgumentException {
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity, TOKENIZATION_KEY);

        assertEquals(TOKENIZATION_KEY, fragment.getAuthorization().toString());
    }

    @Test
    public void getConfiguration_returnsConfiguration() throws InterruptedException, InvalidArgumentException,
            JSONException {
        Configuration configuration = Configuration.fromJson(stringFromFixture("configuration.json"));
        mockConfigurationManager(configuration);
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity, TOKENIZATION_KEY);

        assertEquals(configuration, fragment.getConfiguration());
    }

    @Test
    public void waitForConfiguration_postsCallbackAfterConfigurationIsReceived() throws JSONException,
            InvalidArgumentException, InterruptedException {
        final Configuration configuration = Configuration.fromJson(stringFromFixture("configuration.json"));
        mockConfigurationManager(configuration);
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity, TOKENIZATION_KEY);

        fragment.waitForConfiguration(new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(Configuration returnedConfiguration) {
                assertEquals(configuration, returnedConfiguration);
                mCountDownLatch.countDown();
            }
        });

        mCountDownLatch.await();
    }

    @Test
    public void waitForConfiguration_doesNotPostCallbackWhenNotAttached() throws JSONException,
            InvalidArgumentException, InterruptedException {
        final Configuration configuration = Configuration.fromJson(stringFromFixture("configuration.json"));
        mockConfigurationManager(configuration);
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity, TOKENIZATION_KEY);

        mActivity.getFragmentManager().beginTransaction().detach(fragment).commit();
        mActivity.getFragmentManager().executePendingTransactions();

        fragment.waitForConfiguration(new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(Configuration returnedConfiguration) {
                fail("onConfigurationFetched was called");
            }
        });
    }

    @Test
    public void waitForConfiguration_postsCallbackWhenFragmentIsAttached() throws JSONException,
            InvalidArgumentException, InterruptedException {
        final Configuration configuration = Configuration.fromJson(stringFromFixture("configuration.json"));
        mockConfigurationManager(configuration);
        final BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity, TOKENIZATION_KEY);

        fragment.waitForConfiguration(new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(Configuration returnedConfiguration) {
                assertTrue(fragment.isAdded());
                mCountDownLatch.countDown();
            }
        });

        mCountDownLatch.await();
    }

    @Test
    public void getHttpClient_returnsHttpClient() throws InvalidArgumentException {
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity, TOKENIZATION_KEY);

        assertNotNull(fragment.getHttpClient());
    }

    @Test
    public void addListener_flushesExceptionCallbacks() throws InterruptedException, InvalidArgumentException {
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity, TOKENIZATION_KEY);
        fragment.postCallback(new Exception("Error!"));

        fragment.addListener(new BraintreeErrorListener() {
            @Override
            public void onError(Exception error) {
                assertEquals("Error!", error.getMessage());
                mCountDownLatch.countDown();
            }
        });

        mCountDownLatch.await();
    }

    @Test
    public void addListener_flushesErrorWithResponseCallback() throws InterruptedException, InvalidArgumentException {
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity, TOKENIZATION_KEY);
        fragment.postCallback(new ErrorWithResponse(422, ""));

        fragment.addListener(new BraintreeErrorListener() {
            @Override
            public void onError(Exception error) {
                assertTrue(error instanceof ErrorWithResponse);
                assertEquals(422, ((ErrorWithResponse) error).getStatusCode());
                mCountDownLatch.countDown();
            }
        });

        mCountDownLatch.await();
    }

    @Test
    public void addListener_flushesPaymentMethodNonceCreatedCallback() throws InterruptedException,
            InvalidArgumentException {
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity, TOKENIZATION_KEY);
        fragment.postCallback(new CardNonce());

        fragment.addListener(new PaymentMethodNonceCreatedListener() {
            @Override
            public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
                mCountDownLatch.countDown();
            }
        });

        mCountDownLatch.await();
    }

    @Test
    public void addListener_flushesPaymentMethodNoncesUpdatedCallback() throws InterruptedException,
            InvalidArgumentException {
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity, TOKENIZATION_KEY);
        fragment.postCallback(new ArrayList<PaymentMethodNonce>());

        fragment.addListener(new PaymentMethodNoncesUpdatedListener() {
            @Override
            public void onPaymentMethodNoncesUpdated(List<PaymentMethodNonce> paymentMethodNonces) {
                mCountDownLatch.countDown();
            }
        });

        mCountDownLatch.await();
    }

    @Test
    public void removeListener_noPaymentMethodNonceCreatedReceived() throws InvalidArgumentException {
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity, TOKENIZATION_KEY);
        PaymentMethodNonceCreatedListener listener = new PaymentMethodNonceCreatedListener() {
            @Override
            public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
                fail("Listener was called");
            }
        };

        fragment.addListener(listener);
        fragment.removeListener(listener);

        fragment.postCallback(new CardNonce());
    }

    @Test
    public void removeListener_noPaymentMethodNoncesUpdatedCallbacksReceived() throws InvalidArgumentException {
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity, TOKENIZATION_KEY);
        PaymentMethodNoncesUpdatedListener listener = new PaymentMethodNoncesUpdatedListener() {
            @Override
            public void onPaymentMethodNoncesUpdated(List<PaymentMethodNonce> paymentMethodNonces) {
                fail("Listener was called");
            }
        };

        fragment.addListener(listener);
        fragment.removeListener(listener);

        fragment.postCallback(new ArrayList<PaymentMethodNonce>());
    }

    @Test
    public void removeListener_noErrorCallbacksReceived() throws InvalidArgumentException {
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity, TOKENIZATION_KEY);
        BraintreeErrorListener listener = new BraintreeErrorListener() {
            @Override
            public void onError(Exception error) {
                fail("Listener was called");
            }
        };

        fragment.addListener(listener);
        fragment.removeListener(listener);

        fragment.postCallback(new Exception());
        fragment.postCallback(new ErrorWithResponse(400, ""));
    }

    @Test
    public void waitForConfiguration_retriesIfConfigurationIsNull() throws InvalidArgumentException,
            InterruptedException {
        mockConfigurationManager(new Exception());
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity, TOKENIZATION_KEY);

        fragment.waitForConfiguration(new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(Configuration configuration) {}
        });

        // Request 1: BraintreeFragment sends a "set up" analytics event when it's instantiated
        // Request 2: BraintreeFragment calls #fetchConfiguration in BraintreeFragment#onCreate
        // Request 3: fragment.waitForConfiguration called in this test
        verifyStatic(times(3));
        ConfigurationManager.getConfiguration(eq(fragment), any(ConfigurationListener.class),
                any(BraintreeResponseListener.class));
    }

    @Test
    public void postCallback_addsPaymentMethodNonceToCache() throws InvalidArgumentException {
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity, TOKENIZATION_KEY);
        assertEquals(0, fragment.getCachedPaymentMethodNonces().size());

        fragment.postCallback(new CardNonce());

        assertEquals(1, fragment.getCachedPaymentMethodNonces().size());
        assertTrue(fragment.getCachedPaymentMethodNonces().get(0) instanceof CardNonce);
    }

    @Test
    public void postCallback_addsPaymentMethodNonceToBeginningOfCache() throws InvalidArgumentException {
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity, TOKENIZATION_KEY);
        assertEquals(0, fragment.getCachedPaymentMethodNonces().size());

        fragment.postCallback(new CardNonce());
        fragment.postCallback(new PayPalAccountNonce());

        assertEquals(2, fragment.getCachedPaymentMethodNonces().size());
        assertTrue(fragment.getCachedPaymentMethodNonces().get(0) instanceof PayPalAccountNonce);
    }

    @Test
    public void postCallback_setsBooleanForFetchedPaymentMethodNonces() throws InvalidArgumentException {
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity, TOKENIZATION_KEY);
        assertFalse(fragment.hasFetchedPaymentMethodNonces());

        fragment.postCallback(new ArrayList<PaymentMethodNonce>());

        assertTrue(fragment.hasFetchedPaymentMethodNonces());
    }

    @Test
    public void postCallback_addsAllPaymentMethodNoncesToCache() throws InvalidArgumentException {
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity, TOKENIZATION_KEY);
        assertEquals(0, fragment.getCachedPaymentMethodNonces().size());
        List<PaymentMethodNonce> paymentMethodNonceList = new ArrayList<>();
        paymentMethodNonceList.add(new CardNonce());
        paymentMethodNonceList.add(new PayPalAccountNonce());

        fragment.postCallback(paymentMethodNonceList);

        assertEquals(2, fragment.getCachedPaymentMethodNonces().size());
        assertTrue(fragment.getCachedPaymentMethodNonces().get(0) instanceof CardNonce);
        assertTrue(fragment.getCachedPaymentMethodNonces().get(1) instanceof PayPalAccountNonce);
    }

    @Test
    public void postCallback_exceptionIsPostedToListeners() throws InterruptedException, InvalidArgumentException {
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity, TOKENIZATION_KEY);
        fragment.addListener(new BraintreeErrorListener() {
            @Override
            public void onError(Exception error) {
                assertEquals("Error!", error.getMessage());
                mCountDownLatch.countDown();
            }
        });

        fragment.postCallback(new Exception("Error!"));

        mCountDownLatch.await();
    }

    @Test
    public void postCallback_ErrorWithResponseIsPostedToListeners() throws InterruptedException,
            InvalidArgumentException {
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity, TOKENIZATION_KEY);
        fragment.addListener(new BraintreeErrorListener() {
            @Override
            public void onError(Exception error) {
                assertTrue(error instanceof ErrorWithResponse);
                assertEquals(422, ((ErrorWithResponse) error).getStatusCode());
                mCountDownLatch.countDown();
            }
        });

        fragment.postCallback(new ErrorWithResponse(422, ""));

        mCountDownLatch.await();
    }

    @Test
    public void executesCallbacksOnlyWhenShouldRunIsTrue() throws InvalidArgumentException {
        final AtomicBoolean shouldRun = new AtomicBoolean(false);
        final AtomicBoolean run = new AtomicBoolean(false);
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity, TOKENIZATION_KEY);
        QueuedCallback callback = new QueuedCallback() {
            @Override
            public boolean shouldRun() {
                return shouldRun.get();
            }

            @Override
            public void run() {
                run.set(true);
            }
        };

        fragment.postOrQueueCallback(callback);
        assertFalse(run.get());

        shouldRun.set(true);
        fragment.flushCallbacks();
        assertTrue(run.get());
    }

    @Test
    public void doesNotExecuteCallbackWhenShouldRunIsFalse() throws InterruptedException, InvalidArgumentException {
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity, TOKENIZATION_KEY);
        QueuedCallback callback = new QueuedCallback() {
            @Override
            public boolean shouldRun() {
                return false;
            }

            @Override
            public void run() {
                fail("Listener was called");
            }
        };

        fragment.postOrQueueCallback(callback);
    }

    @Test
    public void onPause_flushesAnalyticsEvents() throws JSONException, InvalidArgumentException {
        mockConfigurationManager(Configuration.fromJson(stringFromFixture("configuration_with_analytics.json")));
        mockStatic(AnalyticsManager.class);
        doNothing().when(AnalyticsManager.class);
        AnalyticsManager.sendRequest(any(BraintreeFragment.class), anyString(), anyString());
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity, TOKENIZATION_KEY);
        fragment.sendAnalyticsEvent("event");

        fragment.onPause();

        verifyStatic();
        AnalyticsManager.flushEvents(fragment);
    }

    @Test
    public void onActivityResult_handlesPayPalResult() throws InvalidArgumentException {
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity, TOKENIZATION_KEY);
        mockStatic(PayPal.class);
        Intent intent = new Intent();

        fragment.onActivityResult(PayPal.PAYPAL_REQUEST_CODE, Activity.RESULT_FIRST_USER, intent);

        verifyStatic();
        PayPal.onActivityResult(fragment, intent);
    }

    @Test
    public void onActivityResult_handlesThreeDSecureResult() throws InvalidArgumentException {
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity, TOKENIZATION_KEY);
        mockStatic(ThreeDSecure.class);
        Intent intent = new Intent();

        fragment.onActivityResult(ThreeDSecure.THREE_D_SECURE_REQUEST_CODE, Activity.RESULT_OK, intent);

        verifyStatic();
        ThreeDSecure.onActivityResult(fragment, Activity.RESULT_OK, intent);
    }

    @Test
    public void onActivityResult_handlesVenmoResult() throws InvalidArgumentException {
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity, TOKENIZATION_KEY);
        mockStatic(Venmo.class);
        Intent intent = new Intent();

        fragment.onActivityResult(Venmo.VENMO_REQUEST_CODE, Activity.RESULT_OK, intent);

        verifyStatic();
        Venmo.onActivityResult(fragment, Activity.RESULT_OK, intent);
    }

    @Test
    public void onActivityResult_postsCancelCallbackWhenResultCodeIsCanceled()
            throws InvalidArgumentException, InterruptedException {
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity, TOKENIZATION_KEY);
        fragment.addListener(new BraintreeCancelListener() {
            @Override
            public void onCancel(int requestCode) {
                assertEquals(42, requestCode);
                mCountDownLatch.countDown();
            }
        });

        fragment.onActivityResult(42, Activity.RESULT_CANCELED, null);

        mCountDownLatch.await();
    }

    @Test
    public void startActivity_clearsLastBrowserSwitchResponseWhenBrowserSwitching() throws InvalidArgumentException {
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity, TOKENIZATION_KEY);
        BraintreeBrowserSwitchActivity.sLastBrowserSwitchResponse = new Intent();

        fragment.startActivity(new Intent().putExtra(BraintreeBrowserSwitchActivity.EXTRA_BROWSER_SWITCH, true));

        assertNull(BraintreeBrowserSwitchActivity.sLastBrowserSwitchResponse);
    }

    @Test
    public void onResume_doesNothingIfNotBrowserSwitching() throws InvalidArgumentException {
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity, TOKENIZATION_KEY);
        mockStatic(PayPal.class);

        fragment.onResume();

        verifyStatic(never());
        PayPal.onActivityResult(any(BraintreeFragment.class), any(Intent.class));
    }

    @Test
    public void onResume_handlesBrowserSwitch() throws InvalidArgumentException {
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity, TOKENIZATION_KEY);
        mockStatic(PayPal.class);
        fragment.startActivity(new Intent().putExtra(BraintreeBrowserSwitchActivity.EXTRA_BROWSER_SWITCH, true));
        Intent intent = new Intent();
        BraintreeBrowserSwitchActivity.sLastBrowserSwitchResponse = intent;

        fragment.onResume();

        assertNull(BraintreeBrowserSwitchActivity.sLastBrowserSwitchResponse);
        fragment.onResume();

        verifyStatic(times(1));
        PayPal.onActivityResult(fragment, intent);
    }

    /* helpers */
    private void mockConfigurationManager(final Configuration configuration) {
        mockStatic(ConfigurationManager.class);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                if (invocation.getArguments()[1] != null) {
                    ((ConfigurationListener) invocation.getArguments()[1]).onConfigurationFetched(configuration);
                }
                return null;
            }
        }).when(ConfigurationManager.class);
        ConfigurationManager.getConfiguration(any(BraintreeFragment.class), any(ConfigurationListener.class),
                any(BraintreeResponseListener.class));
    }

    private void mockConfigurationManager(final Exception exeption) {
        mockStatic(ConfigurationManager.class);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                if (invocation.getArguments()[2] != null) {
                    ((BraintreeResponseListener<Exception>) invocation.getArguments()[2]).onResponse(exeption);
                }
                return null;
            }
        }).when(ConfigurationManager.class);
        ConfigurationManager.getConfiguration(any(BraintreeFragment.class), any(ConfigurationListener.class),
                any(BraintreeResponseListener.class));
    }
}
