package com.braintreepayments.api;

import android.os.Parcel;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.braintreepayments.api.Assertions.assertBinDataEqual;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class GooglePayCardNonceUnitTest {

    @Test
    public void fromJson_createsGooglePayCardNonce() throws Exception {
        String response = Fixtures.PAYMENT_METHODS_GOOGLE_PAY_CARD_RESPONSE;
        JSONObject billing = new JSONObject(response).getJSONObject("paymentMethodData")
                .getJSONObject("info")
                .getJSONObject("billingAddress");
        JSONObject shipping = new JSONObject(response).getJSONObject("shippingAddress");

        PostalAddress billingPostalAddress = getPostalAddressObject(billing);
        PostalAddress shippingPostalAddress = getPostalAddressObject(shipping);

        GooglePayCardNonce googlePayCardNonce = (GooglePayCardNonce) GooglePayCardNonce.fromJSON(new JSONObject(response));

        assertEquals("fake-google-pay-nonce", googlePayCardNonce.getString());
        assertEquals("Visa", googlePayCardNonce.getCardType());
        assertEquals("11", googlePayCardNonce.getLastTwo());
        assertEquals("1234", googlePayCardNonce.getLastFour());
        assertEquals("android-user@example.com", googlePayCardNonce.getEmail());
        assertPostalAddress(billingPostalAddress, googlePayCardNonce.getBillingAddress());
        assertPostalAddress(shippingPostalAddress, googlePayCardNonce.getShippingAddress());
        assertTrue(googlePayCardNonce.isNetworkTokenized());
    }

    @Test
    public void fromJson_withoutBillingAddress_createsGooglePayCardNonce() throws Exception {
        String response = Fixtures.PAYMENT_METHODS_GOOGLE_PAY_CARD_RESPONSE;
        JSONObject json = new JSONObject(response);
        json.getJSONObject("paymentMethodData").getJSONObject("info").remove("billingAddress");
        response = json.toString();
        JSONObject billing = new JSONObject();

        PostalAddress billingPostalAddress = getPostalAddressObject(billing);

        GooglePayCardNonce googlePayCardNonce = (GooglePayCardNonce) GooglePayCardNonce.fromJSON(new JSONObject(response));

        assertPostalAddress(billingPostalAddress, googlePayCardNonce.getBillingAddress());
    }

    @Test
    public void fromJson_withoutShippingAddress_createsGooglePayCardNonce() throws Exception {
        String response = Fixtures.PAYMENT_METHODS_GOOGLE_PAY_CARD_RESPONSE;
        JSONObject json = new JSONObject(response);
        json.remove("shippingAddress");
        response = json.toString();
        JSONObject shipping = new JSONObject();

        PostalAddress shippingPostalAddress = getPostalAddressObject(shipping);

        GooglePayCardNonce googlePayCardNonce = (GooglePayCardNonce) GooglePayCardNonce.fromJSON(new JSONObject(response));

        assertPostalAddress(shippingPostalAddress, googlePayCardNonce.getShippingAddress());
    }

    @Test
    public void fromJson_withoutEmail_createsGooglePayCardNonce() throws JSONException {
        String response = Fixtures.PAYMENT_METHODS_GOOGLE_PAY_CARD_RESPONSE;

        JSONObject json = new JSONObject(response);
        json.remove("email");
        response = json.toString();

        GooglePayCardNonce googlePayCardNonce = (GooglePayCardNonce) GooglePayCardNonce.fromJSON(new JSONObject(response));

        assertEquals("", googlePayCardNonce.getEmail());
    }

    @Test
    public void parcelsCorrectly() throws Exception {
        String response = Fixtures.PAYMENT_METHODS_GOOGLE_PAY_CARD_RESPONSE;
        JSONObject billing = new JSONObject(response).getJSONObject("paymentMethodData")
                .getJSONObject("info")
                .getJSONObject("billingAddress");
        JSONObject shipping = new JSONObject(response).getJSONObject("shippingAddress");

        PostalAddress billingPostalAddress = getPostalAddressObject(billing);
        PostalAddress shippingPostalAddress = getPostalAddressObject(shipping);

        GooglePayCardNonce googlePayCardNonce = (GooglePayCardNonce) GooglePayCardNonce.fromJSON(new JSONObject(response));

        Parcel parcel = Parcel.obtain();
        googlePayCardNonce.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        GooglePayCardNonce parceled = GooglePayCardNonce.CREATOR.createFromParcel(parcel);

        assertEquals("fake-google-pay-nonce", parceled.getString());
        assertEquals("Visa", parceled.getCardType());
        assertEquals("11", parceled.getLastTwo());
        assertEquals("1234", parceled.getLastFour());
        assertEquals("android-user@example.com", parceled.getEmail());
        assertPostalAddress(billingPostalAddress, parceled.getBillingAddress());
        assertPostalAddress(shippingPostalAddress, parceled.getShippingAddress());

        assertBinDataEqual(googlePayCardNonce.getBinData(), parceled.getBinData());
    }

    private PostalAddress getPostalAddressObject(JSONObject address) {
        PostalAddress result = new PostalAddress();
        result.setRecipientName(Json.optString(address, "name", ""));
        result.setStreetAddress(Json.optString(address, "address1", ""));
        result.setExtendedAddress(
                String.join("\n",
                        Json.optString(address, "address2", ""),
                        Json.optString(address, "address3", "")
                ).trim());
        result.setLocality(Json.optString(address, "locality", ""));
        result.setRegion(Json.optString(address, "administrativeArea", ""));
        result.setCountryCodeAlpha2(Json.optString(address, "countryCode", ""));
        result.setPostalCode(Json.optString(address, "postalCode", ""));

        return result;
    }

    private void assertPostalAddress(PostalAddress expected, PostalAddress actual) {
        assertEquals(expected.toString(), actual.toString());
    }
}
