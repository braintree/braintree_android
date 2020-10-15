package com.braintreepayments.api.models;

import android.os.Parcel;

import com.braintreepayments.api.enums.PayPalApiResponseType;
import com.braintreepayments.api.enums.PayPalApiResultType;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

@RunWith(RobolectricTestRunner.class)
public class PayPalApiResultUnitTest {

    @Test
    public void constructsASuccessResult() throws JSONException {
        JSONObject response = new JSONObject()
                .put("access_token", "access_token");
        PayPalApiResult result = new PayPalApiResult("test", PayPalApiResponseType.web, response, "test@paypal.com");

        assertEquals(PayPalApiResultType.Success, result.getResultType());
        assertNull(result.getError());
        JSONObject responseJson = result.getResponse();
        assertEquals("access_token", responseJson.getJSONObject("response").getString("access_token"));
        assertEquals("test", responseJson.getJSONObject("client").getString("environment"));
        assertEquals("web", responseJson.getString("response_type"));
        assertEquals("test@paypal.com", responseJson.getJSONObject("user").getString("display_string"));
    }

    @Test
    public void constructsAnErrorResult() {
        Exception exception = new Exception("error");
        PayPalApiResult result = new PayPalApiResult(exception);

        assertEquals(PayPalApiResultType.Error, result.getResultType());
        assertEquals(exception, result.getError());
    }

    @Test
    public void constructsACancelResult() {
        PayPalApiResult result = new PayPalApiResult();

        assertEquals(PayPalApiResultType.Cancel, result.getResultType());
        assertNull(result.getError());
    }

    @Test
    public void success_parcelsCorrectly() throws JSONException {
        JSONObject response = new JSONObject()
                .put("access_token", "access_token");
        PayPalApiResult result = new PayPalApiResult("test", PayPalApiResponseType.web, response, "test@paypal.com");
        Parcel parcel = Parcel.obtain();
        result.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        PayPalApiResult parceledResult = PayPalApiResult.CREATOR.createFromParcel(parcel);

        assertEquals(PayPalApiResultType.Success, parceledResult.getResultType());
        assertNull(parceledResult.getError());
        JSONObject responseJson = result.getResponse();
        assertEquals("access_token", responseJson.getJSONObject("response").getString("access_token"));
        assertEquals("test", responseJson.getJSONObject("client").getString("environment"));
        assertEquals("web", responseJson.getString("response_type"));
        assertEquals("test@paypal.com", responseJson.getJSONObject("user").getString("display_string"));
    }

    @Test
    public void error_parcelsCorrectly() {
        Exception exception = new Exception("error");
        PayPalApiResult result = new PayPalApiResult(exception);
        Parcel parcel = Parcel.obtain();
        result.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        PayPalApiResult parceledResult = PayPalApiResult.CREATOR.createFromParcel(parcel);

        assertEquals(PayPalApiResultType.Error, parceledResult.getResultType());
        assertEquals(exception.getMessage(), parceledResult.getError().getMessage());
    }

    @Test
    public void cancel_parcelsCorrectly() {
        PayPalApiResult result = new PayPalApiResult();
        Parcel parcel = Parcel.obtain();
        result.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        PayPalApiResult parceledResult = PayPalApiResult.CREATOR.createFromParcel(parcel);

        assertEquals(PayPalApiResultType.Cancel, parceledResult.getResultType());
        assertNull(parceledResult.getError());
    }
}
