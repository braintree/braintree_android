package com.braintreepayments.api;

import android.app.Activity;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;

import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.BraintreeErrorListener;
import com.braintreepayments.api.interfaces.BraintreeResponseListener;
import com.braintreepayments.api.interfaces.ConfigurationListener;
import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCreatedListener;
import com.braintreepayments.api.interfaces.PaymentMethodNoncesUpdatedListener;
import com.braintreepayments.api.interfaces.QueuedCallback;
import com.braintreepayments.api.internal.BraintreeHttpClient;
import com.braintreepayments.api.models.AnalyticsConfiguration;
import com.braintreepayments.api.models.Authorization;
import com.braintreepayments.api.models.CardNonce;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.PayPalAccountNonce;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.testutils.BraintreeActivityTestRule;
import com.braintreepayments.api.test.TestActivity;
import com.braintreepayments.testutils.TestClientTokenBuilder;
import com.google.android.gms.common.api.GoogleApiClient;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static com.braintreepayments.api.BraintreeFragmentTestUtils.getFragment;
import static com.braintreepayments.api.BraintreeFragmentTestUtils.getMockFragment;
import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static com.braintreepayments.testutils.TestTokenizationKey.TOKENIZATION_KEY;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
public class BraintreeFragmentTest {

    @Rule
    public final BraintreeActivityTestRule<TestActivity> mActivityTestRule =
            new BraintreeActivityTestRule<>(TestActivity.class);

    private Activity mActivity;
    private String mClientToken;
    private CountDownLatch mCountDownLatch;

    @Before
    public void setUp() {
        mActivity = mActivityTestRule.getActivity();
        mClientToken = new TestClientTokenBuilder().build();
        mCountDownLatch = new CountDownLatch(1);
    }

    @Test(timeout = 1000)
    @SmallTest
    public void newInstance_returnsABraintreeFragmentFromATokenizationKey()
            throws InvalidArgumentException {
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity, TOKENIZATION_KEY);

        assertNotNull(fragment);
    }

    @Test(timeout = 1000)
    @SmallTest
    public void newInstance_returnsABraintreeFragmentFromAClientToken()
            throws InvalidArgumentException {
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity, mClientToken);

        assertNotNull(fragment);
    }

    @Test(timeout = 1000, expected = InvalidArgumentException.class)
    @SmallTest
    public void newInstance_throwsAnExceptionForABadTokenizationKey()
            throws InvalidArgumentException {
        BraintreeFragment.newInstance(mActivity, "test_key_merchant");
    }

    @Test(timeout = 1000, expected = InvalidArgumentException.class)
    @SmallTest
    public void newInstance_throwsAnExceptionForABadClientToken() throws InvalidArgumentException {
        BraintreeFragment.newInstance(mActivity, "{}");
    }

    @Test(timeout = 1000)
    @SmallTest
    public void newInstance_returnsAnExistingInstance()
            throws InterruptedException {
        BraintreeFragment fragment1 = getFragment(mActivity, mClientToken);
        BraintreeFragment fragment2 = getFragment(mActivity, mClientToken);

        assertEquals(fragment1, fragment2);
    }

    @Test(timeout = 1000)
    @SmallTest
    public void newInstance_setsIntegrationTypeToCustomForAllActivities()
            throws InterruptedException {
        BraintreeFragment fragment = getFragment(mActivity, mClientToken);

        assertEquals("custom", fragment.mIntegrationType);
    }

    @Test(timeout = 1000)
    @SmallTest
    public void sendsAnalyticsEventForTokenizationKey() throws InterruptedException {
        BraintreeFragment fragment = getFragment(mActivity, TOKENIZATION_KEY);
        fragment.waitForConfiguration(new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(Configuration configuration) {
                assertEquals(1, AnalyticsManager.sRequestQueue.size());
                assertEquals("android.custom.started.client-key",
                        AnalyticsManager.sRequestQueue.get(0).getEvent());
                mCountDownLatch.countDown();
            }
        });
        mCountDownLatch.await();
    }

    @Test(timeout = 1000)
    @SmallTest
    public void sendsAnalyticsEventForClientToken() throws InterruptedException {
        BraintreeFragment fragment = getFragment(mActivity, mClientToken);
        fragment.waitForConfiguration(new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(Configuration configuration) {
                assertEquals(1, AnalyticsManager.sRequestQueue.size());
                assertEquals("android.custom.started.client-token",
                        AnalyticsManager.sRequestQueue.get(0).getEvent());
                mCountDownLatch.countDown();
            }
        });
        mCountDownLatch.await();
    }

    @Test(timeout = 10000)
    @MediumTest
    public void fetchConfiguration_worksWithATokenizationKey() throws InterruptedException {
        final BraintreeFragment fragment = getFragment(mActivity, TOKENIZATION_KEY);
        fragment.waitForConfiguration(new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(Configuration configuration) {
                assertNotNull(configuration);
                mCountDownLatch.countDown();
            }
        });

        mCountDownLatch.await();
    }

    @Test(timeout = 10000)
    @MediumTest
    public void fetchConfiguration_worksWithAClientToken() throws InterruptedException {
        final BraintreeFragment fragment = getFragment(mActivity, mClientToken);
        fragment.waitForConfiguration(new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(Configuration configuration) {
                assertNotNull(configuration);
                mCountDownLatch.countDown();
            }
        });

        mCountDownLatch.await();
    }

    @Test(timeout = 1000)
    @SmallTest
    public void postsAnErrorWhenFetchingConfigurationFails()
            throws InvalidArgumentException, InterruptedException {
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity,
                stringFromFixture("client_token_with_bad_config_url.json"));
        fragment.addListener(new BraintreeErrorListener() {
            @Override
            public void onError(Exception error) {
                assertEquals(
                        "Protocol not found: nullnullincorrect_url?configVersion=3&authorizationFingerprint=authorization_fingerprint",
                        error.getMessage());
                mCountDownLatch.countDown();
            }
        });

        mCountDownLatch.await();
    }

    @Test(timeout = 1000)
    @SmallTest
    public void callsConfigurationErrorListenerWhenFetchingConfigurationFails()
            throws InvalidArgumentException, InterruptedException {
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity,
                stringFromFixture("client_token_with_bad_config_url.json"));
        fragment.setConfigurationErrorListener(new BraintreeResponseListener<Exception>() {
            @Override
            public void onResponse(Exception error) {
                assertEquals(
                        "Protocol not found: nullnullincorrect_url?configVersion=3&authorizationFingerprint=authorization_fingerprint",
                        error.getMessage());
                mCountDownLatch.countDown();
            }
        });

        mCountDownLatch.await();
    }

    @Test(timeout = 1000)
    @SmallTest
    public void getContext_returnsContext() throws InvalidArgumentException {
        BraintreeFragment fragment = getFragment(mActivity, mClientToken);

        assertEquals(mActivity.getApplicationContext(), fragment.getApplicationContext());
    }

    @Test(timeout = 1000)
    @SmallTest
    public void getTokenizationKey_returnsTokenizationKey() throws InvalidArgumentException {
        BraintreeFragment fragment = getFragment(mActivity, TOKENIZATION_KEY);

        assertEquals(TOKENIZATION_KEY, fragment.getAuthorization().toString());
    }

    @Test(timeout = 1000)
    @SmallTest
    public void getConfiguration_returnsConfiguration()
            throws InterruptedException, InvalidArgumentException {
        final BraintreeFragment fragment = getFragment(mActivity, mClientToken);
        fragment.waitForConfiguration(new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(Configuration configuration) {
                assertEquals("integration2_merchant_id",
                        configuration.getMerchantId());
                mCountDownLatch.countDown();
            }
        });

        mCountDownLatch.await();
    }

    @Test(timeout = 1000)
    @SmallTest
    public void getHttpClient_returnsHttpClient() {
        BraintreeFragment fragment = getFragment(mActivity, mClientToken);

        assertNotNull(fragment.getHttpClient());
    }

    @Test(timeout = 1000)
    @SmallTest
    public void getGoogleApiClient_returnsGoogleApiClient() throws InterruptedException {
        BraintreeFragment fragment = getFragment(mActivity, mClientToken);

        fragment.getGoogleApiClient(new BraintreeResponseListener<GoogleApiClient>() {
            @Override
            public void onResponse(GoogleApiClient googleApiClient) {
                assertNotNull(googleApiClient);
                mCountDownLatch.countDown();
            }
        });

        mCountDownLatch.await();
    }

    @Test(timeout = 1000)
    @SmallTest
    public void addListener_flushesExceptionCallbacks() throws InterruptedException {
        BraintreeFragment fragment = getFragment(mActivity, mClientToken);
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

    @Test(timeout = 1000)
    @SmallTest
    public void addListener_flushesErrorWithResponseCallback() throws InterruptedException {
        BraintreeFragment fragment = getFragment(mActivity, mClientToken);
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

    @Test(timeout = 1000)
    @SmallTest
    public void addListener_flushesPaymentMethodNonceCreatedCallback() throws InterruptedException {
        BraintreeFragment fragment = getFragment(mActivity, mClientToken);
        fragment.postCallback(new CardNonce());

        fragment.addListener(new PaymentMethodNonceCreatedListener() {
            @Override
            public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
                mCountDownLatch.countDown();
            }
        });

        mCountDownLatch.await();
    }

    @Test(timeout = 1000)
    @SmallTest
    public void addListener_flushesPaymentMethodNoncesUpdatedCallback()
            throws InterruptedException {
        BraintreeFragment fragment = getFragment(mActivity, mClientToken);
        fragment.postCallback(new ArrayList<PaymentMethodNonce>());

        fragment.addListener(new PaymentMethodNoncesUpdatedListener() {
            @Override
            public void onPaymentMethodNoncesUpdated(List<PaymentMethodNonce> paymentMethodNonces) {
                mCountDownLatch.countDown();
            }
        });

        mCountDownLatch.await();
    }

    @Test(timeout = 1000)
    @SmallTest
    public void removeListener_noPaymentMethodNonceCreatedReceived() {
        BraintreeFragment fragment = getFragment(mActivity, mClientToken);
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

    @Test(timeout = 1000)
    @SmallTest
    public void removeListener_noPaymentMethodNoncesUpdatedCallbacksReceived() {
        BraintreeFragment fragment = getFragment(mActivity, mClientToken);
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

    @Test(timeout = 1000)
    @SmallTest
    public void removeListener_noErrorCallbacksReceived() {
        BraintreeFragment fragment = getFragment(mActivity, mClientToken);
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

    @Test(timeout = 10000)
    @MediumTest
    public void waitForConfiguration_postsCallbackAfterConfigurationIsReceived()
            throws InterruptedException {
        final BraintreeFragment fragment = getFragment(mActivity, mClientToken);
        fragment.waitForConfiguration(new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(Configuration configuration) {
                assertNotNull(configuration);
                mCountDownLatch.countDown();
            }
        });

        mCountDownLatch.await();
    }

    @Test(timeout = 1000)
    @SmallTest
    public void postCallback_addsPaymentMethodNonceToCache() {
        BraintreeFragment fragment = getMockFragment(mActivity, mock(Configuration.class));
        assertEquals(0, fragment.getCachedPaymentMethodNonces().size());

        fragment.postCallback(new CardNonce());

        assertEquals(1, fragment.getCachedPaymentMethodNonces().size());
        assertTrue(fragment.getCachedPaymentMethodNonces().get(0) instanceof CardNonce);
    }

    @Test(timeout = 1000)
    @SmallTest
    public void postCallback_addsPaymentMethodNonceToBeginningOfCache() {
        BraintreeFragment fragment = getMockFragment(mActivity, mock(Configuration.class));
        assertEquals(0, fragment.getCachedPaymentMethodNonces().size());

        fragment.postCallback(new CardNonce());
        fragment.postCallback(new PayPalAccountNonce());

        assertEquals(2, fragment.getCachedPaymentMethodNonces().size());
        assertTrue(fragment.getCachedPaymentMethodNonces().get(0) instanceof PayPalAccountNonce);
    }

    @Test(timeout = 1000)
    @SmallTest
    public void postCallback_setsBooleanForFetchedPaymentMethodNonces() {
        BraintreeFragment fragment = getMockFragment(mActivity, mock(Configuration.class));
        assertFalse(fragment.hasFetchedPaymentMethodNonces());

        fragment.postCallback(new ArrayList<PaymentMethodNonce>());

        assertTrue(fragment.hasFetchedPaymentMethodNonces());
    }

    @Test(timeout = 1000)
    @SmallTest
    public void postCallback_addsAllPaymentMethodNoncesToCache() {
        BraintreeFragment fragment = getMockFragment(mActivity, mock(Configuration.class));
        assertEquals(0, fragment.getCachedPaymentMethodNonces().size());
        List<PaymentMethodNonce> paymentMethodNonceList = new ArrayList<>();
        paymentMethodNonceList.add(new CardNonce());
        paymentMethodNonceList.add(new PayPalAccountNonce());

        fragment.postCallback(paymentMethodNonceList);

        assertEquals(2, fragment.getCachedPaymentMethodNonces().size());
        assertTrue(fragment.getCachedPaymentMethodNonces().get(0) instanceof CardNonce);
        assertTrue(fragment.getCachedPaymentMethodNonces().get(1) instanceof PayPalAccountNonce);
    }

    @Test(timeout = 1000)
    @SmallTest
    public void postCallback_exceptionIsPostedToListeners() throws InterruptedException {
        BraintreeFragment fragment = getFragment(mActivity, mClientToken);
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

    @Test(timeout = 1000)
    @SmallTest
    public void postCallback_ErrorWithResponseIsPostedToListeners() throws InterruptedException {
        BraintreeFragment fragment = getFragment(mActivity, mClientToken);
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

    @Test(timeout = 1000)
    @SmallTest
    public void doesNotExecuteCallbackWithNoListeners() throws InterruptedException {
        BraintreeFragment fragment = getMockFragment(mActivity, mock(Configuration.class));
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
        getInstrumentation().waitForIdleSync();
    }

    @Test(timeout = 1000)
    @SmallTest
    public void executesCallbacksOnlyWhenShouldRunIsTrue() throws InterruptedException {
        final AtomicBoolean shouldRun = new AtomicBoolean(false);
        final AtomicBoolean run = new AtomicBoolean(false);
        BraintreeFragment fragment = getMockFragment(mActivity, mock(Configuration.class));
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
        getInstrumentation().waitForIdleSync();
        assertFalse(run.get());

        shouldRun.set(true);
        fragment.flushCallbacks();
        getInstrumentation().waitForIdleSync();
        assertTrue(run.get());
    }

    @Test(timeout = 10000)
    @MediumTest
    public void sendAnalyticsEvent_sendsEventsToServer() throws InterruptedException,
            JSONException, InvalidArgumentException {
        String clientToken = new TestClientTokenBuilder().withAnalytics().build();
        Configuration configuration = Configuration.fromJson(clientToken);
        BraintreeFragment fragment = spy(getFragment(mActivity, clientToken));
        when(fragment.getConfiguration()).thenReturn(configuration);
        Authorization authorization = Authorization.fromString(clientToken);

        BraintreeHttpClient httpClient =
                new BraintreeHttpClient(authorization) {
                    @Override
                    public void post(String url, final String params,
                            final HttpResponseCallback callback) {
                        super.post(url, params, new HttpResponseCallback() {
                            @Override
                            public void success(String responseBody) {
                                if (!params.contains("started")) {
                                    assertTrue(params.contains("analytics.event"));
                                    mCountDownLatch.countDown();
                                }
                            }

                            @Override
                            public void failure(Exception exception) {
                                fail("Request failure: " + exception.getMessage());
                            }
                        });
                    }
                };
        when(fragment.getHttpClient()).thenReturn(httpClient);

        fragment.sendAnalyticsEvent("analytics.event");
        AnalyticsManager.flushEvents(fragment);

        mCountDownLatch.await();
    }

    @Test(timeout = 10000)
    @MediumTest
    public void onPause_flushesAnalyticsEvents() throws JSONException {
        AnalyticsConfiguration analyticsConfiguration = mock(AnalyticsConfiguration.class);
        when(analyticsConfiguration.isEnabled()).thenReturn(true);
        when(analyticsConfiguration.getUrl()).thenReturn("analytics_url");
        Configuration configuration = mock(Configuration.class);
        when(configuration.getAnalytics()).thenReturn(analyticsConfiguration);
        BraintreeFragment fragment = getMockFragment(mActivity, configuration);
        BraintreeHttpClient httpClient = mock(BraintreeHttpClient.class);
        when(fragment.getHttpClient()).thenReturn(httpClient);

        fragment.sendAnalyticsEvent("event");
        fragment.onPause();

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(httpClient).post(eq("analytics_url"), captor.capture(),
                isNull(HttpResponseCallback.class));
        assertTrue(new JSONObject(captor.getValue()).getJSONArray("analytics").length() < 5);
    }
}
