package com.braintreepayments.api;

import android.app.Activity;
import android.test.ActivityInstrumentationTestCase2;

import com.braintreepayments.api.data.BraintreeEnvironment;
import com.braintreepayments.api.test.TestActivity;
import com.braintreepayments.testutils.TestClientTokenBuilder;

import org.json.JSONException;
import org.json.JSONObject;

import static com.braintreepayments.api.BraintreeTestUtils.getBraintree;

public class BraintreeWithActivityTest extends ActivityInstrumentationTestCase2<TestActivity> {

    public BraintreeWithActivityTest() {
        super(TestActivity.class);
    }

    public void testCollectDeviceDataIncludesPayPalCorrelationId()
            throws JSONException, InterruptedException {
        Braintree braintree = getBraintree(getInstrumentation().getContext(),
                new TestClientTokenBuilder().build());
        Activity activity = getActivity();

        String deviceData = braintree.collectDeviceData(activity, BraintreeEnvironment.QA);

        JSONObject json = new JSONObject(deviceData);
        assertNotNull(json.getString("correlation_id"));
    }
}
