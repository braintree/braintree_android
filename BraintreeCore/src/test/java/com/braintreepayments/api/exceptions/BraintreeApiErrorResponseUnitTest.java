package com.braintreepayments.api.exceptions;

import android.os.Parcel;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

@RunWith(RobolectricTestRunner.class)
public class BraintreeApiErrorResponseUnitTest {

    @Test
    public void parsesErrorsCorrectly() {
        String response = stringFromFixture("errors/braintree_api_error_response.json");

        BraintreeApiErrorResponse errorResponse = new BraintreeApiErrorResponse(response);

        assertEquals("The provided parameters are invalid; see details for field-specific error messages.",
                errorResponse.getMessage());
        assertEquals(response, errorResponse.getErrorResponse());
        assertEquals(1, errorResponse.getErrors().size());
        assertEquals("not_an_integer", errorResponse.getErrors().get(0).getCode());
        assertEquals("The provided value must be a string encoding of a base-10 integer between 1 and 12.",
                errorResponse.getErrors().get(0).getMessage());
        assertEquals("body", errorResponse.getErrors().get(0).getIn());
        assertEquals("/expiration_month", errorResponse.getErrors().get(0).getAt());
    }

    @Test
    public void doesNotBlowUpParsingBadJson() {
        String badJson = stringFromFixture("random_json.json");

        BraintreeApiErrorResponse errorResponse = new BraintreeApiErrorResponse(badJson);

        assertEquals("Parsing error response failed", errorResponse.getMessage());
        assertNull(errorResponse.getErrors());
    }

    @Test
    public void parcelsCorrectly() {
        String response = stringFromFixture("errors/braintree_api_error_response.json");
        BraintreeApiErrorResponse errorResponse = new BraintreeApiErrorResponse(response);
        Parcel parcel = Parcel.obtain();
        errorResponse.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        BraintreeApiErrorResponse parceled = BraintreeApiErrorResponse.CREATOR.createFromParcel(parcel);

        assertEquals("The provided parameters are invalid; see details for field-specific error messages.",
                parceled.getMessage());
        assertEquals(response, parceled.getErrorResponse());
        assertEquals(1, parceled.getErrors().size());
        assertEquals("not_an_integer", parceled.getErrors().get(0).getCode());
        assertEquals("The provided value must be a string encoding of a base-10 integer between 1 and 12.",
                parceled.getErrors().get(0).getMessage());
        assertEquals("body", parceled.getErrors().get(0).getIn());
        assertEquals("/expiration_month", parceled.getErrors().get(0).getAt());
    }
}
