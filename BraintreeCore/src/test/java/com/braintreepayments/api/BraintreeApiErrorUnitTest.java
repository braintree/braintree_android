package com.braintreepayments.api;

import android.os.Parcel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

@RunWith(RobolectricTestRunner.class)
public class BraintreeApiErrorUnitTest {

    @Test
    public void fromJsonArray_parsesErrors() throws JSONException {
        JSONObject response = new JSONObject(Fixtures.ERRORS_BRAINTREE_API_ERROR_RESPONSE);
        JSONArray details = response.getJSONObject("error").getJSONArray("details");

        List<BraintreeApiError> errors = BraintreeApiError.fromJsonArray(details);

        assertEquals(1, errors.size());
        assertEquals("not_an_integer", errors.get(0).getCode());
        assertEquals("The provided value must be a string encoding of a base-10 integer between 1 and 12.",
                errors.get(0).getMessage());
        assertEquals("body", errors.get(0).getIn());
        assertEquals("/expiration_month", errors.get(0).getAt());
    }

    @Test
    public void fromJsonArray_doesNotBlowUpParsingBadJson() {
        List<BraintreeApiError> errors = BraintreeApiError.fromJsonArray(new JSONArray());

        assertEquals(0, errors.size());
    }

    @Test
    public void fromJson_parsesDetails() throws JSONException {
        JSONObject response = new JSONObject(Fixtures.ERRORS_BRAINTREE_API_ERROR_RESPONSE);
        JSONObject detail = response.getJSONObject("error").getJSONArray("details").getJSONObject(0);

        BraintreeApiError error = BraintreeApiError.fromJson(detail);

        assertEquals("not_an_integer", error.getCode());
        assertEquals("The provided value must be a string encoding of a base-10 integer between 1 and 12.",
                error.getMessage());
        assertEquals("body", error.getIn());
        assertEquals("/expiration_month", error.getAt());
    }

    @Test
    public void fromJson_doesNotBlowUpParsingBadJson() throws JSONException {
        String badJson = Fixtures.RANDOM_JSON;

        BraintreeApiError error = BraintreeApiError.fromJson(new JSONObject(badJson));

        assertNull(error.getCode());
        assertNull(error.getMessage());
        assertNull(error.getIn());
        assertNull(error.getAt());
    }

    @Test
    public void parcelsCorrectly() throws JSONException {
        JSONObject response = new JSONObject(Fixtures.ERRORS_BRAINTREE_API_ERROR_RESPONSE);
        JSONObject detail = response.getJSONObject("error").getJSONArray("details").getJSONObject(0);
        BraintreeApiError error = BraintreeApiError.fromJson(detail);
        Parcel parcel = Parcel.obtain();
        error.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        BraintreeApiError parceled = BraintreeApiError.CREATOR.createFromParcel(parcel);

        assertEquals("not_an_integer", parceled.getCode());
        assertEquals("The provided value must be a string encoding of a base-10 integer between 1 and 12.",
                parceled.getMessage());
        assertEquals("body", parceled.getIn());
        assertEquals("/expiration_month", parceled.getAt());
    }
}
