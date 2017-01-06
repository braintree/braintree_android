package com.braintreepayments.api;

import android.app.Activity;
import android.support.test.runner.AndroidJUnit4;

import com.braintreepayments.api.exceptions.ConfigurationException;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.BraintreeErrorListener;
import com.braintreepayments.api.interfaces.BraintreeResponseListener;
import com.braintreepayments.api.interfaces.ConfigurationListener;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.test.BraintreeActivityTestRule;
import com.braintreepayments.api.test.TestActivity;
import com.braintreepayments.api.test.TestClientTokenBuilder;
import com.braintreepayments.testutils.TestConfigurationBuilder;
import com.braintreepayments.testutils.TestConfigurationBuilder.TestVisaCheckoutConfigurationBuilder;
import com.google.android.gms.common.api.GoogleApiClient;
import com.visa.checkout.VisaMcomLibrary;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;

import static com.braintreepayments.api.BraintreeFragmentTestUtils.getFragmentWithAuthorization;
import static com.braintreepayments.api.BraintreeFragmentTestUtils.getFragmentWithConfiguration;
import static com.braintreepayments.testutils.TestTokenizationKey.TOKENIZATION_KEY;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

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

    @Test(timeout = 10000)
    public void fetchConfiguration_worksWithATokenizationKey() throws InterruptedException {
        final BraintreeFragment fragment = getFragmentWithAuthorization(mActivity, TOKENIZATION_KEY);
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
    public void fetchConfiguration_worksWithAClientToken() throws InterruptedException {
        final BraintreeFragment fragment = getFragmentWithAuthorization(mActivity, mClientToken);
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
    public void newInstance_returnsSingleton() throws InvalidArgumentException, InterruptedException {
        final BraintreeFragment[] fragments = new BraintreeFragment[2];
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    fragments[0] = BraintreeFragment.newInstance(mActivity, TOKENIZATION_KEY);
                    fragments[1] = BraintreeFragment.newInstance(mActivity, TOKENIZATION_KEY);
                } catch (InvalidArgumentException e) {
                    fail(e.getMessage());
                }

                mCountDownLatch.countDown();
            }
        });

        mCountDownLatch.await();
        assertNotNull(fragments[0]);
        assertNotNull(fragments[1]);
        assertEquals(fragments[0], fragments[1]);
    }

    @Test(timeout = 1000)
    public void getGoogleApiClient_returnsGoogleApiClient() throws InterruptedException {
        BraintreeFragment fragment = getFragmentWithAuthorization(mActivity, mClientToken);

        fragment.getGoogleApiClient(new BraintreeResponseListener<GoogleApiClient>() {
            @Override
            public void onResponse(GoogleApiClient googleApiClient) {
                assertNotNull(googleApiClient);
                mCountDownLatch.countDown();
            }
        });

        mCountDownLatch.await();
    }

    @Test(timeout = 10000)
    public void getVisaCheckoutLibrary_returnsVisaCheckoutLibrary() throws InterruptedException {
        BraintreeFragment fragment = getFragmentWithConfiguration(mActivity, new TestConfigurationBuilder()
                .visaCheckout(new TestVisaCheckoutConfigurationBuilder()
                        .apikey("apikey")
                        .externalClientId("externalClientId"))
                .build());

        fragment.getVisaCheckoutLibrary(new BraintreeResponseListener<VisaMcomLibrary>() {
            @Override
            public void onResponse(VisaMcomLibrary visaMcomLibrary) {
                assertNotNull(visaMcomLibrary);
                mCountDownLatch.countDown();
            }
        });

        mCountDownLatch.await();
    }

    @Test
    public void getVisaCheckoutLibrary_whenVisaCheckoutReturnsNull_postsExceptionAndDoesNotCallListener()
            throws InvalidArgumentException, InterruptedException, JSONException {
        final CountDownLatch latch = new CountDownLatch(1);
        BraintreeFragment fragment = getFragmentWithConfiguration(mActivity, new TestConfigurationBuilder()
                .build());

        fragment.addListener(new BraintreeErrorListener() {
            @Override
            public void onError(Exception error) {
                assertTrue(error instanceof ConfigurationException);
                assertEquals("Visa Checkout is not enabled.", error.getMessage());
                latch.countDown();
            }
        });

        fragment.getVisaCheckoutLibrary(new BraintreeResponseListener<VisaMcomLibrary>() {
            @Override
            public void onResponse(VisaMcomLibrary visaMcomLibrary) {
                fail("Not expected a response");
            }
        });

        latch.await();
    }

}
