package com.braintreepayments.api;

import android.app.Activity;
import android.test.ActivityInstrumentationTestCase2;

import com.braintreepayments.api.data.BraintreeEnvironment;
import com.braintreepayments.api.test.TestActivity;

import org.json.JSONException;
import org.json.JSONObject;

public class BraintreeApiWithActivityTest extends ActivityInstrumentationTestCase2<TestActivity> {

    public BraintreeApiWithActivityTest() {
        super(TestActivity.class);
    }

    public void testCollectDeviceDataIncludesPayPalCorrelationId() throws JSONException {
        BraintreeApi braintreeApi = new BraintreeApi(getInstrumentation().getContext(), new TestClientTokenBuilder().build());
        Activity activity = getActivity();

        String deviceData = braintreeApi.collectDeviceData(activity, BraintreeEnvironment.QA);

        JSONObject json = new JSONObject(deviceData);
        assertNotNull(json.getString("correlation_id"));
    }

}
