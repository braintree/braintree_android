package com.braintreepayments.api;

import android.app.Activity;
import android.support.test.runner.AndroidJUnit4;

import com.braintreepayments.api.interfaces.BraintreeResponseListener;
import com.braintreepayments.api.interfaces.ConfigurationListener;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.test.TestActivity;
import com.braintreepayments.testutils.BraintreeActivityTestRule;
import com.braintreepayments.testutils.TestClientTokenBuilder;
import com.google.android.gms.common.api.GoogleApiClient;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;

import static com.braintreepayments.api.BraintreeFragmentTestUtils.getFragment;
import static com.braintreepayments.testutils.TestTokenizationKey.TOKENIZATION_KEY;
import static junit.framework.Assert.assertNotNull;

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
}
