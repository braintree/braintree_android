package com.paypal.android.sdk.onetouch.core;

import android.os.Parcel;

import com.paypal.android.sdk.onetouch.core.enums.ResponseType;
import com.paypal.android.sdk.onetouch.core.enums.ResultType;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

@RunWith(RobolectricGradleTestRunner.class)
public class ResultTest {

    @Test
    public void constructsASuccessResult() throws JSONException {
        JSONObject response = new JSONObject()
                .put("access_token", "access_token");
        Result result = new Result("test", ResponseType.web, response, "test@paypal.com");

        assertEquals(ResultType.Success, result.getResultType());
        assertNull(result.getError());
        JSONObject responseJson = result.getResponse();
        assertEquals("access_token", responseJson.getJSONObject("response").getString("access_token"));
        assertEquals("test", responseJson.getJSONObject("client").getString("environment"));
        assertEquals("PayPalOneTouch-Android", responseJson.getJSONObject("client").getString("product_name"));
        assertEquals("Android", responseJson.getJSONObject("client").getString("platform"));
        assertEquals("web", responseJson.getString("response_type"));
        assertEquals("test@paypal.com", responseJson.getJSONObject("user").getString("display_string"));
    }

    @Test
    public void constructsAnErrorResult() {
        Exception exception = new Exception("error");
        Result result = new Result(exception);

        assertEquals(ResultType.Error, result.getResultType());
        assertEquals(exception, result.getError());
    }

    @Test
    public void constructsACancelResult() {
        Result result = new Result();

        assertEquals(ResultType.Cancel, result.getResultType());
        assertNull(result.getError());
    }

    @Test
    public void success_parcelsCorrectly() throws JSONException {
        JSONObject response = new JSONObject()
                .put("access_token", "access_token");
        Result result = new Result("test", ResponseType.web, response, "test@paypal.com");
        Parcel parcel = Parcel.obtain();
        result.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        Result parceledResult = Result.CREATOR.createFromParcel(parcel);

        assertEquals(ResultType.Success, parceledResult.getResultType());
        assertNull(parceledResult.getError());
        JSONObject responseJson = result.getResponse();
        assertEquals("access_token", responseJson.getJSONObject("response").getString("access_token"));
        assertEquals("test", responseJson.getJSONObject("client").getString("environment"));
        assertEquals("PayPalOneTouch-Android", responseJson.getJSONObject("client").getString("product_name"));
        assertEquals("Android", responseJson.getJSONObject("client").getString("platform"));
        assertEquals("web", responseJson.getString("response_type"));
        assertEquals("test@paypal.com", responseJson.getJSONObject("user").getString("display_string"));
    }

    @Test
    public void error_parcelsCorrectly() {
        Exception exception = new Exception("error");
        Result result = new Result(exception);
        Parcel parcel = Parcel.obtain();
        result.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        Result parceledResult = Result.CREATOR.createFromParcel(parcel);

        assertEquals(ResultType.Error, parceledResult.getResultType());
        assertEquals(exception.getMessage(), parceledResult.getError().getMessage());
    }

    @Test
    public void cancel_parcelsCorrectly() {
        Result result = new Result();
        Parcel parcel = Parcel.obtain();
        result.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        Result parceledResult = Result.CREATOR.createFromParcel(parcel);

        assertEquals(ResultType.Cancel, parceledResult.getResultType());
        assertNull(parceledResult.getError());
    }
}
