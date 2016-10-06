package com.braintreepayments.api;

import android.net.Uri;
import android.os.SystemClock;
import android.util.Base64;

import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.exceptions.UnexpectedException;
import com.braintreepayments.api.interfaces.BraintreeResponseListener;
import com.braintreepayments.api.interfaces.ConfigurationListener;
import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.internal.BraintreeHttpClient;
import com.braintreepayments.api.models.Authorization;
import com.braintreepayments.api.models.ClientToken;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.testutils.TestTokenizationKey;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.concurrent.CountDownLatch;

import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static com.braintreepayments.testutils.SharedPreferencesHelper.clearSharedPreferences;
import static com.braintreepayments.testutils.SharedPreferencesHelper.getSharedPreferences;
import static com.braintreepayments.testutils.SharedPreferencesHelper.writeMockConfiguration;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricGradleTestRunner.class)
public class ConfigurationManagerUnitTest {

    private Authorization mTokenizationKey;
    private CountDownLatch mCountDownLatch;
    private BraintreeFragment mBraintreeFragment;

    @Before
    public void setup() throws InvalidArgumentException {
        clearSharedPreferences(RuntimeEnvironment.application);
        ConfigurationManager.sFetchingConfiguration = false;
        mTokenizationKey = Authorization.fromString(TestTokenizationKey.TOKENIZATION_KEY);

        mCountDownLatch = new CountDownLatch(1);

        mBraintreeFragment = mock(BraintreeFragment.class);
        when(mBraintreeFragment.getAuthorization()).thenReturn(mTokenizationKey);
        when(mBraintreeFragment.getApplicationContext()).thenReturn(RuntimeEnvironment.application);
        when(mBraintreeFragment.getHttpClient()).thenReturn(new BraintreeHttpClient(mTokenizationKey));
    }

    @Test
    public void isFetchingConfiguration_isFalseWhenNotFetchingConfiguration() {
        assertFalse(ConfigurationManager.isFetchingConfiguration());
    }

    @Test
    public void isFetchingConfiguration_isTrueWhenFetchingConfiguration() throws InterruptedException {
        when(mBraintreeFragment.getHttpClient()).thenReturn(new BraintreeHttpClient(mTokenizationKey) {
            @Override
            public void get(String path, HttpResponseCallback callback) {
                mThreadPool.submit(new Runnable() {
                    @Override
                    public void run() {
                        SystemClock.sleep(1000);
                    }
                });
            }
        });

        ConfigurationManager.getConfiguration(mBraintreeFragment, new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(Configuration configuration) {}
        }, new BraintreeResponseListener<Exception>() {
            @Override
            public void onResponse(Exception e) {}
        });

        assertTrue(ConfigurationManager.isFetchingConfiguration());
    }

    @Test(timeout = 1000)
    public void isFetchingConfiguration_isFalseInSuccessCallback() throws InterruptedException {
        stubConfigurationFromGateway(stringFromFixture("configuration_with_analytics.json"));

        ConfigurationManager.getConfiguration(mBraintreeFragment, new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(Configuration configuration) {
                assertFalse(ConfigurationManager.isFetchingConfiguration());
                mCountDownLatch.countDown();
            }
        }, new BraintreeResponseListener<Exception>() {
            @Override
            public void onResponse(Exception e) {
                fail(e.getMessage());
            }
        });

        mCountDownLatch.await();
    }

    @Test(timeout = 1000)
    public void isFetchingConfiguration_isFalseInErrorCallback() throws InterruptedException {
        when(mBraintreeFragment.getHttpClient()).thenReturn(new BraintreeHttpClient(mTokenizationKey) {
            @Override
            public void get(String path, HttpResponseCallback callback) {
                if (path.contains(mTokenizationKey.getConfigUrl())) {
                    callback.failure(new UnexpectedException("Something bad happened"));
                }
            }
        });

        ConfigurationManager.getConfiguration(mBraintreeFragment, new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(Configuration configuration) {
                fail("Success listener should not have been called for bad request");
            }
        }, new BraintreeResponseListener<Exception>() {
            @Override
            public void onResponse(Exception e) {
                assertFalse(ConfigurationManager.isFetchingConfiguration());
                mCountDownLatch.countDown();
            }
        });

        mCountDownLatch.await();
    }

    @Test(timeout = 1000)
    public void getConfiguration_getsConfigFromCacheWhenTimeoutHasNotExpired() throws InterruptedException {
        writeMockConfiguration(RuntimeEnvironment.application, mTokenizationKey.getConfigUrl(),
                mTokenizationKey.toString(), stringFromFixture("configuration.json"), System.currentTimeMillis());

        ConfigurationManager.getConfiguration(mBraintreeFragment, new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(Configuration configuration) {
                assertEquals(stringFromFixture("configuration.json"), configuration.toJson());
                mCountDownLatch.countDown();
            }
        }, new BraintreeResponseListener<Exception>() {
            @Override
            public void onResponse(Exception e) {
                fail(e.getMessage());
            }
        });

        mCountDownLatch.await();
    }

    @Test(timeout = 1000)
    public void getConfiguration_getsConfigFromGatewayWhenTimeoutExpired() throws InterruptedException {
        writeMockConfiguration(RuntimeEnvironment.application, mTokenizationKey.getConfigUrl(),
                mTokenizationKey.toString(), stringFromFixture("configuration.json"),
                System.currentTimeMillis() - (ConfigurationManager.TTL + 1));
        stubConfigurationFromGateway(stringFromFixture("configuration_with_analytics.json"));

        ConfigurationManager.getConfiguration(mBraintreeFragment, new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(Configuration configuration) {
                assertEquals(stringFromFixture("configuration_with_analytics.json"),
                        configuration.toJson());
                mCountDownLatch.countDown();
            }
        }, new BraintreeResponseListener<Exception>() {
            @Override
            public void onResponse(Exception e) {
                fail(e.getMessage());
            }
        });

        mCountDownLatch.await();
    }

    @Test(timeout = 1000)
    public void getConfiguration_fetchesConfigFromGatewayWhenCacheIsEmpty() throws InterruptedException {
        stubConfigurationFromGateway(stringFromFixture("configuration_with_analytics.json"));

        ConfigurationManager.getConfiguration(mBraintreeFragment, new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(Configuration configuration) {
                assertEquals(stringFromFixture("configuration_with_analytics.json"),
                        configuration.toJson());
                mCountDownLatch.countDown();
            }
        }, new BraintreeResponseListener<Exception>() {
            @Override
            public void onResponse(Exception e) {
                fail(e.getMessage());
            }
        });

        mCountDownLatch.await();
    }

    @Test(timeout = 1000)
    public void getConfiguration_fetchesConfigurationFromGatewayWhenCachedConfigIsInvalid()
            throws InterruptedException {
        writeMockConfiguration(RuntimeEnvironment.application, mTokenizationKey.getConfigUrl(),
                mTokenizationKey.toString(), "not a config");
        stubConfigurationFromGateway(stringFromFixture("configuration.json"));

        ConfigurationManager.getConfiguration(mBraintreeFragment, new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(Configuration configuration) {
                assertEquals(stringFromFixture("configuration.json"), configuration.toJson());
                mCountDownLatch.countDown();
            }
        }, new BraintreeResponseListener<Exception>() {
            @Override
            public void onResponse(Exception e) {
                fail(e.getMessage());
            }
        });

        mCountDownLatch.await();
    }

    @Test(timeout = 1000)
    public void getConfiguration_takesClientTokenIntoAccountForCache()
            throws InvalidArgumentException, InterruptedException {
        ClientToken clientToken = (ClientToken) Authorization.fromString(
                stringFromFixture("client_token_with_authorization_fingerprint_options.json"));
        when(mBraintreeFragment.getAuthorization()).thenReturn(clientToken);
        writeMockConfiguration(RuntimeEnvironment.application, clientToken.getConfigUrl(),
                clientToken.getAuthorizationFingerprint(), stringFromFixture("configuration.json"),
                System.currentTimeMillis());

        ConfigurationManager.getConfiguration(mBraintreeFragment, new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(Configuration configuration) {
                assertEquals(stringFromFixture("configuration.json"), configuration.toJson());
                mCountDownLatch.countDown();
            }
        }, new BraintreeResponseListener<Exception>() {
            @Override
            public void onResponse(Exception e) {
                fail(e.getMessage());
            }
        });

        mCountDownLatch.await();
    }

    @Test(timeout = 1000)
    public void getConfiguration_writesConfigToDiskWithValidTimestampAfterFetch() throws InterruptedException {
        stubConfigurationFromGateway(stringFromFixture("configuration.json"));

        ConfigurationManager.getConfiguration(mBraintreeFragment, new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(Configuration configuration) {
                String key = Base64.encodeToString(
                        Uri.parse(mTokenizationKey.getConfigUrl())
                                .buildUpon()
                                .appendQueryParameter("configVersion", "3")
                                .build()
                                .toString()
                                .concat(mTokenizationKey.toString())
                                .getBytes(),
                        0);

                assertEquals(stringFromFixture("configuration.json"),
                        getSharedPreferences(RuntimeEnvironment.application).getString(key, ""));
                assertTrue(System.currentTimeMillis() -
                        getSharedPreferences(RuntimeEnvironment.application).getLong(key + "_timestamp", 0) < 1000);
                mCountDownLatch.countDown();
            }
        }, new BraintreeResponseListener<Exception>() {
            @Override
            public void onResponse(Exception e) {
                fail(e.getMessage());
            }
        });

        mCountDownLatch.await();
    }

    @Test(timeout = 1000)
    public void getConfiguration_callsErrorListenerWhenHttpFails() throws InterruptedException {
        BraintreeHttpClient fakeClient = new BraintreeHttpClient(mTokenizationKey) {
            @Override
            public void get(String path, HttpResponseCallback callback) {
                if (path.contains(mTokenizationKey.getConfigUrl())) {
                    callback.failure(new UnexpectedException("Something bad happened"));
                }
            }
        };
        when(mBraintreeFragment.getHttpClient()).thenReturn(fakeClient);

        ConfigurationManager.getConfiguration(mBraintreeFragment, new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(Configuration configuration) {
                fail("Success listener should not have been called for bad request");
            }
        }, new BraintreeResponseListener<Exception>() {
            @Override
            public void onResponse(Exception e) {
                assertTrue(e instanceof UnexpectedException);
                assertEquals("Something bad happened", e.getMessage());
                mCountDownLatch.countDown();
            }
        });

        mCountDownLatch.await();
    }

    private void stubConfigurationFromGateway(final String responseString) {
        BraintreeHttpClient fakeClient = new BraintreeHttpClient(mBraintreeFragment.getAuthorization()) {
            @Override
            public void get(String path, HttpResponseCallback callback) {
                if (path.contains(mBraintreeFragment.getAuthorization().getConfigUrl())) {
                    callback.success(responseString);
                }
            }
        };
        when(mBraintreeFragment.getHttpClient()).thenReturn(fakeClient);
    }
}
