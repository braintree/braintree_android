package com.braintreepayments.api;

import android.app.Activity;
import android.content.Context;
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
import com.braintreepayments.api.interfaces.UnionPayListener;
import com.braintreepayments.api.internal.AnalyticsDatabase;
import com.braintreepayments.api.internal.AnalyticsDatabaseTestUtils;
import com.braintreepayments.api.internal.AnalyticsIntentService;
import com.braintreepayments.api.internal.HttpClient;
import com.braintreepayments.api.models.AndroidPayCardNonce;
import com.braintreepayments.api.models.CardNonce;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.PayPalAccountNonce;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.api.models.UnionPayCapabilities;
import com.braintreepayments.api.test.FragmentTestActivity;
import com.braintreepayments.api.test.UnitTestListenerActivity;
import com.braintreepayments.testutils.TestConfigurationBuilder;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.braintreepayments.api.internal.AnalyticsDatabaseTestUtils.verifyAnalyticsEvent;
import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static com.braintreepayments.testutils.ReflectionHelper.getField;
import static com.braintreepayments.testutils.ReflectionHelper.setField;
import static com.braintreepayments.testutils.TestConfigurationBuilder.basicConfig;
import static com.braintreepayments.testutils.TestTokenizationKey.TOKENIZATION_KEY;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(RobolectricGradleTestRunner.class)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*", "org.json.*" })
@PrepareForTest({ ConfigurationManager.class, PayPal.class, ThreeDSecure.class, Venmo.class, AndroidPay.class })
public class BraintreeFragmentUnitTest {

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    private Activity mActivity;
    private AtomicBoolean mCalled;

    @Before
    public void setup() {
        mActivity = spy(Robolectric.setupActivity(FragmentTestActivity.class));
        doNothing().when(mActivity).startActivity(any(Intent.class));
        AnalyticsDatabaseTestUtils.clearAllEvents(mActivity);
        mCalled = new AtomicBoolean(false);
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

    @Test(expected = InvalidArgumentException.class)
    public void newInstance_throwsAnExceptionWhenActivityIsNull() throws InvalidArgumentException {
        BraintreeFragment.newInstance(null, TOKENIZATION_KEY);
    }

    @Test(expected = InvalidArgumentException.class)
    public void newInstance_throwsAnExceptionWhenActivityIsDestroyed() throws InvalidArgumentException {
        Activity activity = Robolectric.buildActivity(FragmentTestActivity.class)
                .create()
                .start()
                .resume()
                .pause()
                .stop()
                .destroy()
                .get();

        BraintreeFragment.newInstance(activity, TOKENIZATION_KEY);
    }

    @Test
    public void onCreate_callsFetchConfiguration() throws InvalidArgumentException {
        mockStatic(ConfigurationManager.class);

        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity, TOKENIZATION_KEY);

        verifyStatic(times(2));
        ConfigurationManager.getConfiguration(eq(fragment), any(ConfigurationListener.class),
                any(BraintreeResponseListener.class));
    }

    @Test
    public void onCreate_restoresConfigurationAndHttpClient() throws InvalidArgumentException, NoSuchFieldException,
            IllegalAccessException {
        Configuration configuration = new TestConfigurationBuilder().buildConfiguration();
        mockConfigurationManager(configuration);
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity, TOKENIZATION_KEY);
        Bundle bundle = new Bundle();
        fragment.onSaveInstanceState(bundle);
        setField(BraintreeFragment.class, "mConfiguration", fragment, null);
        fragment.mHttpClient = null;

        fragment.onCreate(bundle);

        assertNotNull(fragment.getConfiguration());
        assertNotNull(fragment.mHttpClient);
        assertEquals("client_api_url", getField(HttpClient.class, "mBaseUrl", fragment.mHttpClient));
    }

    @Test
    public void onAttach_recordsNewActivity()
            throws JSONException, InvalidArgumentException, NoSuchFieldException, IllegalAccessException {
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity, TOKENIZATION_KEY);
        assertEquals(false, getField(BraintreeFragment.class, "mNewActivityNeedsConfiguration", fragment));

        fragment.onAttach(null);

        assertEquals(true, getField(BraintreeFragment.class, "mNewActivityNeedsConfiguration", fragment));
    }

    @Test
    public void newInstance_setsIntegrationTypeToCustomForAllActivities() throws InvalidArgumentException {
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity, TOKENIZATION_KEY);

        assertEquals("custom", fragment.mIntegrationType);
    }

    @Test
    public void sendEvent_addsEventToDatabase() throws InvalidArgumentException {
        Configuration configuration = new TestConfigurationBuilder().withAnalytics().buildConfiguration();

        BraintreeFragment fragment = spy(BraintreeFragment.newInstance(mActivity, TOKENIZATION_KEY));
        when(fragment.getConfiguration()).thenReturn(configuration);
        fragment.sendAnalyticsEvent("test.event");

        verifyAnalyticsEvent(mActivity, "test.event");
    }

    @Test
    public void sendEvent_doesNothingIfAnalyticsNotEnabled() throws InvalidArgumentException {
        AnalyticsDatabase db = AnalyticsDatabase.getInstance(mActivity);

        BraintreeFragment fragment = spy(BraintreeFragment.newInstance(mActivity, TOKENIZATION_KEY));
        when(fragment.getConfiguration()).thenReturn((Configuration) basicConfig());
        fragment.sendAnalyticsEvent("test.event");

        assertEquals(0, db.getPendingRequests().size());
    }

    @Test
    public void postsAnErrorWhenFetchingConfigurationFails() throws InvalidArgumentException {
        mockConfigurationManager(new Exception("Configuration error"));
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity, TOKENIZATION_KEY);
        final AtomicInteger calls = new AtomicInteger(0);
        fragment.addListener(new BraintreeErrorListener() {
            @Override
            public void onError(Exception error) {
                assertEquals("Request for configuration has failed: Configuration error. Future requests will retry up to 3 times",
                        error.getMessage());
                calls.getAndIncrement();
            }
        });
        fragment.setConfigurationErrorListener(new BraintreeResponseListener<Exception>() {
            @Override
            public void onResponse(Exception error) {
                assertEquals("Request for configuration has failed: Configuration error. Future requests will retry up to 3 times",
                        error.getMessage());
                calls.getAndIncrement();
            }
        });

        assertEquals(2, calls.get());
    }

    @Test
    public void onSaveInstanceState_savesState() throws InvalidArgumentException, JSONException {
        Configuration configuration = new TestConfigurationBuilder().buildConfiguration();
        mockConfigurationManager(configuration);
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity, TOKENIZATION_KEY);
        Bundle bundle = new Bundle();

        fragment.onSaveInstanceState(bundle);

        assertTrue(bundle.getParcelableArrayList(BraintreeFragment.EXTRA_CACHED_PAYMENT_METHOD_NONCES).isEmpty());
        assertFalse(bundle.getBoolean(BraintreeFragment.EXTRA_FETCHED_PAYMENT_METHOD_NONCES));
        assertFalse(bundle.getBoolean(BraintreeFragment.EXTRA_BROWSER_SWITCHING));
        assertEquals(configuration.toJson(), bundle.getString(BraintreeFragment.EXTRA_CONFIGURATION));
    }

    @Test
    public void onSaveInstanceState_doesNotIncludeConfigurationWhenNull() throws InvalidArgumentException,
            JSONException {
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity, TOKENIZATION_KEY);
        Bundle bundle = new Bundle();

        fragment.onSaveInstanceState(bundle);

        assertTrue(bundle.getParcelableArrayList(BraintreeFragment.EXTRA_CACHED_PAYMENT_METHOD_NONCES).isEmpty());
        assertFalse(bundle.getBoolean(BraintreeFragment.EXTRA_FETCHED_PAYMENT_METHOD_NONCES));
        assertFalse(bundle.getBoolean(BraintreeFragment.EXTRA_BROWSER_SWITCHING));
        assertFalse(bundle.containsKey(BraintreeFragment.EXTRA_CONFIGURATION));
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
    public void getConfiguration_returnsConfiguration() throws InvalidArgumentException, JSONException {
        Configuration configuration = Configuration.fromJson(stringFromFixture("configuration.json"));
        mockConfigurationManager(configuration);
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity, TOKENIZATION_KEY);

        assertEquals(configuration, fragment.getConfiguration());
    }

    @Test
    public void waitForConfiguration_postsCallbackAfterConfigurationIsReceived() throws JSONException,
            InvalidArgumentException {
        final Configuration configuration = Configuration.fromJson(stringFromFixture("configuration.json"));
        mockConfigurationManager(configuration);
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity, TOKENIZATION_KEY);

        fragment.waitForConfiguration(new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(Configuration returnedConfiguration) {
                assertEquals(configuration, returnedConfiguration);
                mCalled.set(true);
            }
        });

        assertTrue(mCalled.get());
    }

    @Test
    public void waitForConfiguration_doesNotPostCallbackWhenNotAttached() throws JSONException,
            InvalidArgumentException {
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
            InvalidArgumentException {
        final Configuration configuration = Configuration.fromJson(stringFromFixture("configuration.json"));
        mockConfigurationManager(configuration);
        final BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity, TOKENIZATION_KEY);

        fragment.waitForConfiguration(new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(Configuration returnedConfiguration) {
                assertTrue(fragment.isAdded());
                mCalled.set(true);
            }
        });

        assertTrue(mCalled.get());
    }

    @Test
    public void getHttpClient_returnsHttpClient() throws InvalidArgumentException {
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity, TOKENIZATION_KEY);

        assertNotNull(fragment.getHttpClient());
    }

    @Test
    public void addListener_flushesExceptionCallbacks() throws InvalidArgumentException {
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity, TOKENIZATION_KEY);
        fragment.postCallback(new Exception("Error!"));

        fragment.addListener(new BraintreeErrorListener() {
            @Override
            public void onError(Exception error) {
                assertEquals("Error!", error.getMessage());
                mCalled.set(true);
            }
        });

        assertTrue(mCalled.get());
    }

    @Test
    public void addListener_flushesErrorWithResponseCallback() throws InvalidArgumentException {
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity, TOKENIZATION_KEY);
        fragment.postCallback(new ErrorWithResponse(422, ""));

        fragment.addListener(new BraintreeErrorListener() {
            @Override
            public void onError(Exception error) {
                assertTrue(error instanceof ErrorWithResponse);
                assertEquals(422, ((ErrorWithResponse) error).getStatusCode());
                mCalled.set(true);
            }
        });

        assertTrue(mCalled.get());
    }

    @Test
    public void addListener_flushesPaymentMethodNonceCreatedCallback() throws InvalidArgumentException {
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity, TOKENIZATION_KEY);
        fragment.postCallback(new CardNonce());

        fragment.addListener(new PaymentMethodNonceCreatedListener() {
            @Override
            public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
                mCalled.set(true);
            }
        });

        assertTrue(mCalled.get());
    }

    @Test
    public void addListener_flushesPaymentMethodNoncesUpdatedCallback() throws InvalidArgumentException {
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity, TOKENIZATION_KEY);
        fragment.postCallback(new ArrayList<PaymentMethodNonce>());

        fragment.addListener(new PaymentMethodNoncesUpdatedListener() {
            @Override
            public void onPaymentMethodNoncesUpdated(List<PaymentMethodNonce> paymentMethodNonces) {
                mCalled.set(true);
            }
        });

        assertTrue(mCalled.get());
    }

    @Test
    public void addListener_flushesCancelCallback() throws InvalidArgumentException {
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity, TOKENIZATION_KEY);
        fragment.postCancelCallback(42);

        fragment.addListener(new BraintreeCancelListener() {
            @Override
            public void onCancel(int requestCode) {
                assertEquals(42, requestCode);
                mCalled.set(true);
            }
        });

        assertTrue(mCalled.get());
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
    public void removeListener_noCancelReceived() throws InvalidArgumentException {
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity, TOKENIZATION_KEY);
        BraintreeCancelListener listener = new BraintreeCancelListener() {
            @Override
            public void onCancel(int requestCode) {
                fail("Listener was called");
            }
        };

        fragment.addListener(listener);
        fragment.removeListener(listener);

        fragment.postCancelCallback(42);
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
    public void addAndRemoveListenersAddAndRemoveAllListeners() throws InvalidArgumentException {
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity, TOKENIZATION_KEY);
        ConfigurationListener configurationListener = new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(Configuration configuration) {}
        };
        BraintreeErrorListener braintreeErrorListener = new BraintreeErrorListener() {
            @Override
            public void onError(Exception error) {}
        };
        PaymentMethodNoncesUpdatedListener paymentMethodNoncesUpdatedListener = new PaymentMethodNoncesUpdatedListener() {
            @Override
            public void onPaymentMethodNoncesUpdated(List<PaymentMethodNonce> paymentMethodNonces) {}
        };
        PaymentMethodNonceCreatedListener paymentMethodNonceCreatedListener = new PaymentMethodNonceCreatedListener() {
            @Override
            public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {}
        };
        BraintreeCancelListener braintreeCancelListener = new BraintreeCancelListener() {
            @Override
            public void onCancel(int requestCode) {}
        };
        UnionPayListener unionPayListener = new UnionPayListener() {
            @Override
            public void onCapabilitiesFetched(UnionPayCapabilities capabilities) {}

            @Override
            public void onSmsCodeSent(String enrollmentId, boolean smsCodeRequired) {}
        };

        fragment.addListener(configurationListener);
        fragment.addListener(braintreeErrorListener);
        fragment.addListener(paymentMethodNoncesUpdatedListener);
        fragment.addListener(paymentMethodNonceCreatedListener);
        fragment.addListener(braintreeCancelListener);
        fragment.addListener(unionPayListener);

        assertEquals(6, fragment.getListeners().size());

        fragment.removeListener(configurationListener);
        fragment.removeListener(braintreeErrorListener);
        fragment.removeListener(paymentMethodNoncesUpdatedListener);
        fragment.removeListener(paymentMethodNonceCreatedListener);
        fragment.removeListener(braintreeCancelListener);
        fragment.removeListener(unionPayListener);

        assertEquals(0, fragment.getListeners().size());
    }

    @Test
    public void getListeners_isEmptyWhenNoListenersAreSet() throws InvalidArgumentException {
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity, TOKENIZATION_KEY);

        assertEquals(0, fragment.getListeners().size());
    }

    @Test
    public void getListeners_returnsAllListeners() throws InvalidArgumentException {
        BraintreeFragment fragment = BraintreeFragment.newInstance(
                Robolectric.setupActivity(UnitTestListenerActivity.class), TOKENIZATION_KEY);

        assertEquals(6, fragment.getListeners().size());
    }

    @Test
    public void waitForConfiguration_retriesIfConfigurationIsNull() throws InvalidArgumentException {
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
    public void postCallback_postsPaymentMethodNonceToListener() throws InvalidArgumentException {
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity, TOKENIZATION_KEY);
        final AtomicBoolean wasCalled = new AtomicBoolean(false);
        fragment.addListener(new PaymentMethodNonceCreatedListener() {
            @Override
            public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
                assertTrue(paymentMethodNonce instanceof CardNonce);
                wasCalled.set(true);
            }
        });

        fragment.postCallback(new CardNonce());

        assertTrue(wasCalled.get());
    }

    @Test
    public void postCancelCallback_postsRequestCodeToListener() throws InvalidArgumentException {
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity, TOKENIZATION_KEY);
        final AtomicBoolean wasCalled = new AtomicBoolean(false);
        fragment.addListener(new BraintreeCancelListener() {
            @Override
            public void onCancel(int requestCode) {
                assertEquals(42, requestCode);
                wasCalled.set(true);
            }
        });

        fragment.postCancelCallback(42);

        assertTrue(wasCalled.get());
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
    public void postCallback_onlyAllowsOneAndroidPayCardNonceToBePresent() throws InvalidArgumentException {
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity, TOKENIZATION_KEY);
        assertEquals(0, fragment.getCachedPaymentMethodNonces().size());

        fragment.postCallback(new CardNonce());
        fragment.postCallback(new AndroidPayCardNonce());
        fragment.postCallback(new AndroidPayCardNonce());

        assertEquals(2, fragment.getCachedPaymentMethodNonces().size());
        assertTrue(fragment.getCachedPaymentMethodNonces().get(0) instanceof AndroidPayCardNonce);
    }

    @Test
    public void postCallback_doesNotRemoveAndroidPayCardNonceWhenAnotherNonceIsAdded() throws InvalidArgumentException {
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity, TOKENIZATION_KEY);
        assertEquals(0, fragment.getCachedPaymentMethodNonces().size());

        fragment.postCallback(new AndroidPayCardNonce());
        fragment.postCallback(new CardNonce());

        assertEquals(2, fragment.getCachedPaymentMethodNonces().size());
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
    public void postCallback_exceptionIsPostedToListeners() throws InvalidArgumentException {
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity, TOKENIZATION_KEY);
        fragment.addListener(new BraintreeErrorListener() {
            @Override
            public void onError(Exception error) {
                assertEquals("Error!", error.getMessage());
                mCalled.set(true);
            }
        });

        fragment.postCallback(new Exception("Error!"));

        assertTrue(mCalled.get());
    }

    @Test
    public void postCallback_ErrorWithResponseIsPostedToListeners() throws InvalidArgumentException {
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity, TOKENIZATION_KEY);
        fragment.addListener(new BraintreeErrorListener() {
            @Override
            public void onError(Exception error) {
                assertTrue(error instanceof ErrorWithResponse);
                assertEquals(422, ((ErrorWithResponse) error).getStatusCode());
                mCalled.set(true);
            }
        });

        fragment.postCallback(new ErrorWithResponse(422, ""));

        assertTrue(mCalled.get());
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
    public void doesNotExecuteCallbackWhenShouldRunIsFalse() throws InvalidArgumentException {
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
    public void onStop_flushesAnalyticsEvents() throws JSONException, InvalidArgumentException {
        String configuration = new TestConfigurationBuilder().withAnalytics().build();
        mockConfigurationManager(Configuration.fromJson(configuration));

        Robolectric.getForegroundThreadScheduler().pause();
        Context context = Mockito.spy(RuntimeEnvironment.application);
        when(mActivity.getApplicationContext()).thenReturn(context);
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity, TOKENIZATION_KEY);
        Robolectric.getForegroundThreadScheduler().unPause();
        Robolectric.getForegroundThreadScheduler().advanceToLastPostedRunnable();

        fragment.onStop();

        ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(context).startService(intentCaptor.capture());

        Intent serviceIntent = intentCaptor.getValue();
        assertEquals(TOKENIZATION_KEY, serviceIntent.getStringExtra(AnalyticsIntentService.EXTRA_AUTHORIZATION));
        assertEquals(configuration, serviceIntent.getStringExtra(AnalyticsIntentService.EXTRA_CONFIGURATION));
    }

    @Test
    public void onActivityResult_handlesPayPalResult() throws InvalidArgumentException {
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity, TOKENIZATION_KEY);
        mockStatic(PayPal.class);
        Intent intent = new Intent();

        fragment.onActivityResult(PayPal.PAYPAL_REQUEST_CODE, Activity.RESULT_FIRST_USER, intent);

        verifyStatic();
        PayPal.onActivityResult(fragment, Activity.RESULT_FIRST_USER, intent);
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
    public void onActivityResult_handlesAndroidPayResult() throws InvalidArgumentException {
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity, TOKENIZATION_KEY);
        mockStatic(AndroidPay.class);
        Intent intent = new Intent();

        fragment.onActivityResult(AndroidPay.ANDROID_PAY_REQUEST_CODE, Activity.RESULT_OK, intent);

        verifyStatic();
        AndroidPay.onActivityResult(fragment, Activity.RESULT_OK, intent);
    }

    @Test
    public void onActivityResult_postsCancelCallbackWhenResultCodeIsCanceled() throws InvalidArgumentException {
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity, TOKENIZATION_KEY);
        fragment.addListener(new BraintreeCancelListener() {
            @Override
            public void onCancel(int requestCode) {
                assertEquals(42, requestCode);
                mCalled.set(true);
            }
        });

        fragment.onActivityResult(42, Activity.RESULT_CANCELED, null);

        assertTrue(mCalled.get());
    }

    @Test
    public void startActivity_clearsLastBrowserSwitchResponseWhenBrowserSwitching() throws InvalidArgumentException {
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity, TOKENIZATION_KEY);
        BraintreeBrowserSwitchActivity.sLastBrowserSwitchResponse = new Intent();

        fragment.startActivity(new Intent().putExtra(BraintreeBrowserSwitchActivity.EXTRA_BROWSER_SWITCH, true));

        assertNull(BraintreeBrowserSwitchActivity.sLastBrowserSwitchResponse);
    }

    @Test
    public void startActivityForResult_postsExceptionWhenNotAttached() throws JSONException,
            InvalidArgumentException {
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity, TOKENIZATION_KEY);
        mActivity.getFragmentManager().beginTransaction().detach(fragment).commit();
        mActivity.getFragmentManager().executePendingTransactions();
        fragment.addListener(new BraintreeErrorListener() {
            @Override
            public void onError(Exception error) {
                assertEquals("BraintreeFragment is not attached to an Activity. Please ensure it is attached and try again.",
                        error.getMessage());
                mCalled.set(true);
            }
        });

        fragment.startActivityForResult(new Intent(), 1);

        assertTrue(mCalled.get());
    }

    @Test
    public void startActivityForResult_doesNotPostExceptionWhenAttached() throws InvalidArgumentException {
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity, TOKENIZATION_KEY);
        fragment.addListener(new BraintreeErrorListener() {
            @Override
            public void onError(Exception error) {
                fail("onError was called");
            }
        });

        fragment.startActivityForResult(new Intent(), 1);

        assertFalse(mCalled.get());
    }

    @Test
    public void onResume_doesNotPostConfigurationToCallbackForTheSameActivity() throws InvalidArgumentException {
        Configuration configuration = new TestConfigurationBuilder().buildConfiguration();
        mockConfigurationManager(configuration);
        UnitTestListenerActivity activity = Robolectric.setupActivity(UnitTestListenerActivity.class);
        BraintreeFragment fragment = BraintreeFragment.newInstance(activity, TOKENIZATION_KEY);

        fragment.onResume();

        assertEquals(1, activity.configurations.size());
        assertEquals(configuration, activity.configurations.get(0));
    }

    @Test
    public void onResume_postsConfigurationToCallbackForNewActivity() throws InvalidArgumentException {
        Configuration configuration = new TestConfigurationBuilder().buildConfiguration();
        mockConfigurationManager(configuration);
        UnitTestListenerActivity activity = Robolectric.setupActivity(UnitTestListenerActivity.class);
        BraintreeFragment fragment = BraintreeFragment.newInstance(activity, TOKENIZATION_KEY);
        fragment.onAttach(null);

        fragment.onResume();

        assertEquals(2, activity.configurations.size());
        assertEquals(configuration, activity.configurations.get(0));
        assertEquals(configuration, activity.configurations.get(1));
    }

    @Test
    public void onResume_doesNothingIfNotBrowserSwitching() throws InvalidArgumentException {
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity, TOKENIZATION_KEY);
        mockStatic(PayPal.class);

        fragment.onResume();

        verifyStatic(never());
        PayPal.onActivityResult(any(BraintreeFragment.class), anyInt(), any(Intent.class));
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
        PayPal.onActivityResult(fragment, Activity.RESULT_OK, intent);
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
