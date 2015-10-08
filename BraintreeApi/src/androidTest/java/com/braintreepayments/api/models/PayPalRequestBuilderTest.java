package com.braintreepayments.api.models;

import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.SmallTest;

import com.braintreepayments.api.exceptions.ConfigurationException;
import com.braintreepayments.api.test.TestActivity;
import com.paypal.android.sdk.onetouch.core.AuthorizationRequest;
import com.paypal.android.sdk.onetouch.core.Request;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class PayPalRequestBuilderTest {

    @Rule
    public final ActivityTestRule<TestActivity> mActivityTestRule =
            new ActivityTestRule<>(TestActivity.class);

    private TestActivity mActivity;

    @Before
    public void setUp() {
        mActivity = mActivityTestRule.getActivity();
    }

    @Test(timeout = 1000)
    @SmallTest
    public void buildPayPalRequest_buildsWithLiveStageUrl()
            throws JSONException, ConfigurationException {
        PayPalRequestBuilder payPalRequestBuilder = new PayPalRequestBuilder();
        Configuration configuration = Configuration.fromJson(
                stringFromFixture("configuration_with_live_paypal.json"));


        Request request = payPalRequestBuilder.createAuthorizationRequest(mActivity, configuration);
        assertEquals(AuthorizationRequest.ENVIRONMENT_LIVE, request.getEnvironment());
        assertBaseRequestProperties(request);
    }

    @Test(timeout = 1000)
    @SmallTest
    public void buildPayPalRequest_buildsWithOfflineStageUrl()
            throws JSONException, ConfigurationException {
        PayPalRequestBuilder payPalRequestBuilder = new PayPalRequestBuilder();
        Configuration configuration = Configuration.fromJson(
                stringFromFixture("configuration_with_offline_paypal.json"));


        Request request = payPalRequestBuilder.createAuthorizationRequest(mActivity, configuration);
        assertEquals(AuthorizationRequest.ENVIRONMENT_MOCK, request.getEnvironment());
        assertBaseRequestProperties(request);
    }

    @Test(timeout = 1000)
    @SmallTest
    public void buildPayPalRequest_buildsWithCustomStageUrl()
            throws JSONException, ConfigurationException {
        PayPalRequestBuilder payPalRequestBuilder = new PayPalRequestBuilder();
        Configuration configuration = Configuration.fromJson(
                stringFromFixture("configuration_with_custom_paypal.json"));


        Request request = payPalRequestBuilder.createAuthorizationRequest(mActivity, configuration);
        assertEquals("custom", request.getEnvironment());
        assertBaseRequestProperties(request);
    }

    private void assertBaseRequestProperties(Request request){
        String packageName = mActivity.getPackageName();
        String expectedCancelUrl = String.format("%s.braintree://onetouch/v1/cancel", packageName);
        String expectedSuccessUrl = String.format("%s.braintree://onetouch/v1/success", packageName);
        assertEquals(expectedCancelUrl, request.getCancelUrl());
        assertEquals(expectedSuccessUrl, request.getSuccessUrl());
        assertEquals("paypal_client_id", request.getClientId());
    }

    @Test(expected = ConfigurationException.class)
    @SmallTest
    public void buildPayPalRequest_failOnBadConfiguration() throws JSONException, ConfigurationException {
        PayPalRequestBuilder payPalRequestBuilder = new PayPalRequestBuilder();
        Configuration configuration = Configuration.fromJson(stringFromFixture("configuration_with_disabled_paypal.json"));
        Request request = payPalRequestBuilder.createAuthorizationRequest(mActivity, configuration);
    }
}

