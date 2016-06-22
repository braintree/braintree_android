package com.braintreepayments.api;

import android.net.Uri;
import android.os.SystemClock;
import android.support.test.runner.AndroidJUnit4;
import android.util.Base64;

import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.exceptions.UnexpectedException;
import com.braintreepayments.api.interfaces.BraintreeResponseListener;
import com.braintreepayments.api.interfaces.ConfigurationListener;
import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.internal.BraintreeHttpClient;
import com.braintreepayments.api.internal.BraintreeSharedPreferences;
import com.braintreepayments.api.models.Authorization;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.testutils.TestTokenizationKey;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static com.braintreepayments.testutils.SharedPreferencesHelper.writeMockConfiguration;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
public class ConfigurationManagerTest {

    private Authorization mTokenizationKey;

    @Before
    public void setup() throws InvalidArgumentException {
        BraintreeSharedPreferences.getSharedPreferences(getTargetContext()).edit().clear().commit();
        ConfigurationManager.sFetchingConfiguration = false;
        mTokenizationKey = Authorization.fromString(TestTokenizationKey.TOKENIZATION_KEY);
    }

    @Test(timeout = 1000)
    public void isFetchingConfiguration_isFalseWhenNotFetchingConfiguration() {
        assertFalse(ConfigurationManager.isFetchingConfiguration());
    }

    @Test(timeout = 1000)
    public void isFetchingConfiguration_isTrueWhenFetchingConfiguration() throws InterruptedException {
        BraintreeFragment fragment = getMockFragment();
        when(fragment.getHttpClient()).thenReturn(new BraintreeHttpClient(mTokenizationKey) {
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

        ConfigurationManager.getConfiguration(fragment, new ConfigurationListener() {
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
        final BraintreeFragment fragment = getMockFragment();
        stubConfigurationFromGateway(fragment, stringFromFixture("configuration_with_analytics.json"));
        final CountDownLatch latch = new CountDownLatch(1);
        ConfigurationManager.getConfiguration(fragment, new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(Configuration configuration) {
                assertFalse(ConfigurationManager.isFetchingConfiguration());
                latch.countDown();
            }
        }, new BraintreeResponseListener<Exception>() {
            @Override
            public void onResponse(Exception e) {
                fail(e.getMessage());
            }
        });

        latch.await();
    }

    @Test(timeout = 1000)
    public void isFetchingConfiguration_isFalseInErrorCallback() throws InterruptedException {
        BraintreeFragment fragment = getMockFragment();
        when(fragment.getHttpClient()).thenReturn(new BraintreeHttpClient(mTokenizationKey) {
            @Override
            public void get(String path, HttpResponseCallback callback) {
                if (path.contains(mTokenizationKey.getConfigUrl())) {
                    callback.failure(new UnexpectedException("Something bad happened"));
                }
            }
        });
        final CountDownLatch latch = new CountDownLatch(1);
        ConfigurationManager.getConfiguration(fragment, new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(Configuration configuration) {
                fail("Success listener should not have been called for bad request");
            }
        }, new BraintreeResponseListener<Exception>() {
            @Override
            public void onResponse(Exception e) {
                assertFalse(ConfigurationManager.isFetchingConfiguration());
                latch.countDown();
            }
        });

        latch.await();
    }

    @Test(timeout = 1000)
    public void getConfiguration_getsConfigFromCacheWhenTimeoutHasNotExpired()
            throws InterruptedException {
        BraintreeFragment fragment = getMockFragment();

        writeMockConfiguration(mTokenizationKey.getConfigUrl(),
                stringFromFixture("configuration.json"));

        final CountDownLatch latch = new CountDownLatch(1);
        ConfigurationManager.getConfiguration(fragment, new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(Configuration configuration) {
                assertEquals(stringFromFixture("configuration.json"), configuration.toJson());
                latch.countDown();
            }
        }, new BraintreeResponseListener<Exception>() {
            @Override
            public void onResponse(Exception e) {
                fail(e.getMessage());
            }
        });

        latch.await();
    }

    @Test(timeout = 1000)
    public void getConfiguration_getsConfigFromGatewayWhenTimeoutExpired()
            throws InterruptedException {
        final BraintreeFragment fragment = getMockFragment();

        writeMockConfiguration(mTokenizationKey.getConfigUrl(),
                stringFromFixture("configuration.json"),
                System.currentTimeMillis() - (ConfigurationManager.TTL + 1));

        stubConfigurationFromGateway(fragment,
                stringFromFixture("configuration_with_analytics.json"));

        final CountDownLatch latch = new CountDownLatch(1);
        ConfigurationManager.getConfiguration(fragment, new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(Configuration configuration) {
                assertEquals(stringFromFixture("configuration_with_analytics.json"),
                        configuration.toJson());
                latch.countDown();
            }
        }, new BraintreeResponseListener<Exception>() {
            @Override
            public void onResponse(Exception e) {
                fail(e.getMessage());
            }
        });

        latch.await();
    }

    @Test(timeout = 1000)
    public void getConfiguration_fetchesConfigFromGatewayWhenCacheIsEmpty()
            throws InterruptedException {
        final BraintreeFragment fragment = getMockFragment();

        stubConfigurationFromGateway(fragment,
                stringFromFixture("configuration_with_analytics.json"));

        final CountDownLatch latch = new CountDownLatch(1);
        ConfigurationManager.getConfiguration(fragment, new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(Configuration configuration) {
                assertEquals(stringFromFixture("configuration_with_analytics.json"),
                        configuration.toJson());
                latch.countDown();
            }
        }, new BraintreeResponseListener<Exception>() {
            @Override
            public void onResponse(Exception e) {
                fail(e.getMessage());
            }
        });

        latch.await();
    }

    @Test(timeout = 1000)
    public void getConfiguration_fetchesConfigurationFromGatewayWhenCachedConfigIsInvalid()
            throws InterruptedException {
        BraintreeFragment fragment = getMockFragment();
        writeMockConfiguration(mTokenizationKey.getConfigUrl(), "not a config");

        stubConfigurationFromGateway(fragment, stringFromFixture("configuration.json"));

        final CountDownLatch latch = new CountDownLatch(1);
        ConfigurationManager.getConfiguration(fragment, new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(Configuration configuration) {
                assertEquals(stringFromFixture("configuration.json"), configuration.toJson());
                latch.countDown();
            }
        }, new BraintreeResponseListener<Exception>() {
            @Override
            public void onResponse(Exception e) {
                fail(e.getMessage());
            }
        });

        latch.await();
    }

    @Test(timeout = 1000)
    public void getConfiguration_writesConfigToDiskWithValidTimestampAfterFetch()
            throws InterruptedException {
        BraintreeFragment fragment = getMockFragment();
        stubConfigurationFromGateway(fragment, stringFromFixture("configuration.json"));

        final CountDownLatch latch = new CountDownLatch(1);
        ConfigurationManager.getConfiguration(fragment, new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(Configuration configuration) {
                String key = Uri.parse(mTokenizationKey.getConfigUrl()).buildUpon()
                        .appendQueryParameter("configVersion", "3").build().toString();
                key = Base64.encodeToString(key.getBytes(), 0);

                assertEquals(stringFromFixture("configuration.json"),
                        BraintreeSharedPreferences.getSharedPreferences(getTargetContext()).getString(key, ""));
                assertTrue(System.currentTimeMillis() -
                        BraintreeSharedPreferences.getSharedPreferences(getTargetContext())
                                .getLong(key + "_timestamp", 0) < 1000);
                latch.countDown();
            }
        }, new BraintreeResponseListener<Exception>() {
            @Override
            public void onResponse(Exception e) {
                fail(e.getMessage());
            }
        });

        latch.await();
    }

    @Test(timeout = 1000)
    public void getConfiguration_callsErrorListenerWhenHttpFails() throws InterruptedException {
        BraintreeFragment fragment = getMockFragment();
        BraintreeHttpClient fakeClient = new BraintreeHttpClient(mTokenizationKey) {
            @Override
            public void get(String path, HttpResponseCallback callback) {
                if (path.contains(mTokenizationKey.getConfigUrl())) {
                    callback.failure(new UnexpectedException("Something bad happened"));
                }
            }
        };
        when(fragment.getHttpClient()).thenReturn(fakeClient);

        final CountDownLatch latch = new CountDownLatch(1);
        ConfigurationManager.getConfiguration(fragment, new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(Configuration configuration) {
                fail("Success listener should not have been called for bad request");
            }
        }, new BraintreeResponseListener<Exception>() {
            @Override
            public void onResponse(Exception e) {
                assertTrue(e instanceof UnexpectedException);
                assertEquals("Something bad happened", e.getMessage());
                latch.countDown();
            }
        });

        latch.await();
    }

    private BraintreeFragment getMockFragment() {
        BraintreeFragment fragment = mock(BraintreeFragment.class);
        when(fragment.getAuthorization()).thenReturn(mTokenizationKey);
        when(fragment.getApplicationContext()).thenReturn(getTargetContext());
        when(fragment.getHttpClient()).thenReturn(new BraintreeHttpClient(mTokenizationKey));

        return fragment;
    }

    private void stubConfigurationFromGateway(final BraintreeFragment fragment, final String responseString) {
        BraintreeHttpClient fakeClient = new BraintreeHttpClient(fragment.getAuthorization()) {
            @Override
            public void get(String path, HttpResponseCallback callback) {
                if (path.contains(fragment.getAuthorization().getConfigUrl())) {
                    callback.success(responseString);
                }
            }
        };
        when(fragment.getHttpClient()).thenReturn(fakeClient);
    }
}
