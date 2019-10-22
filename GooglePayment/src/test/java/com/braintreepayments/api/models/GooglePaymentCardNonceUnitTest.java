package com.braintreepayments.api.models;

import android.os.Parcel;

import com.braintreepayments.api.Json;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.braintreepayments.api.test.Assertions.assertBinDataEqual;
import static com.braintreepayments.api.test.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class GooglePaymentCardNonceUnitTest {

    @Test
    public void fromJson_createsGooglePaymentCardNonce() throws Exception {
        String response = stringFromFixture("payment_methods/google_pay_card_response.json");
        JSONObject billing = new JSONObject(response).getJSONObject("paymentMethodData")
            .getJSONObject("info")
            .getJSONObject("billingAddress");
        JSONObject shipping = new JSONObject(response).getJSONObject("shippingAddress");

        PostalAddress billingPostalAddress = getPostalAddressObject(billing);
        PostalAddress shippingPostalAddress = getPostalAddressObject(shipping);

        GooglePaymentCardNonce googlePaymentCardNonce = GooglePaymentCardNonce.fromJson(response);

        assertEquals("Google Pay", googlePaymentCardNonce.getTypeLabel());
        assertEquals("fake-android-pay-nonce", googlePaymentCardNonce.getNonce());
        assertEquals("MasterCard 0276", googlePaymentCardNonce.getDescription());
        assertEquals("Visa", googlePaymentCardNonce.getCardType());
        assertEquals("11", googlePaymentCardNonce.getLastTwo());
        assertEquals("1234", googlePaymentCardNonce.getLastFour());
        assertEquals("android-user@example.com", googlePaymentCardNonce.getEmail());
        assertPostalAddress(billingPostalAddress, googlePaymentCardNonce.getBillingAddress());
        assertPostalAddress(shippingPostalAddress, googlePaymentCardNonce.getShippingAddress());
        assertTrue(googlePaymentCardNonce.isNetworkTokenized());
    }

    @Test
    public void fromJson_withoutBillingAddress_createsGooglePayCardNonce() throws Exception {
        String response = stringFromFixture("payment_methods/google_pay_card_response.json");
        JSONObject json = new JSONObject(response);
        json.getJSONObject("paymentMethodData").getJSONObject("info").remove("billingAddress");
        response = json.toString();
        JSONObject billing = new JSONObject();

        PostalAddress billingPostalAddress = getPostalAddressObject(billing);

        GooglePaymentCardNonce googlePaymentCardNonce = GooglePaymentCardNonce.fromJson(response);

        assertPostalAddress(billingPostalAddress, googlePaymentCardNonce.getBillingAddress());
    }

    @Test
    public void fromJson_withoutShippingAddress_createsGooglePayCardNonce() throws Exception {

        String response = stringFromFixture("payment_methods/google_pay_card_response.json");
        JSONObject json = new JSONObject(response);
        json.remove("shippingAddress");
        response = json.toString();
        JSONObject shipping = new JSONObject();

        PostalAddress shippingPostalAddress = getPostalAddressObject(shipping);

        GooglePaymentCardNonce googlePaymentCardNonce = GooglePaymentCardNonce.fromJson(response);

        assertPostalAddress(shippingPostalAddress, googlePaymentCardNonce.getShippingAddress());
    }

    @Test
    public void fromJson_withoutEmail_createsGooglePayCardNonce() throws JSONException {
        String response = stringFromFixture("payment_methods/google_pay_card_response.json");

        JSONObject json = new JSONObject(response);
        json.remove("email");
        response = json.toString();

        GooglePaymentCardNonce googlePaymentCardNonce = GooglePaymentCardNonce.fromJson(response);

        assertEquals("", googlePaymentCardNonce.getEmail());
    }

    @Test
    public void parcelsCorrectly() throws Exception {
        String response = stringFromFixture("payment_methods/google_pay_card_response.json");
        JSONObject billing = new JSONObject(response).getJSONObject("paymentMethodData")
                .getJSONObject("info")
                .getJSONObject("billingAddress");
        JSONObject shipping = new JSONObject(response).getJSONObject("shippingAddress");

        PostalAddress billingPostalAddress = getPostalAddressObject(billing);
        PostalAddress shippingPostalAddress = getPostalAddressObject(shipping);

        GooglePaymentCardNonce googlePaymentCardNonce = GooglePaymentCardNonce.fromJson(response);

        Parcel parcel = Parcel.obtain();
        googlePaymentCardNonce.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        GooglePaymentCardNonce parceled = GooglePaymentCardNonce.CREATOR.createFromParcel(parcel);

        assertEquals("Google Pay", parceled.getTypeLabel());
        assertEquals("fake-android-pay-nonce", parceled.getNonce());
        assertEquals("MasterCard 0276", parceled.getDescription());
        assertEquals("Visa", parceled.getCardType());
        assertEquals("11", parceled.getLastTwo());
        assertEquals("1234", parceled.getLastFour());
        assertEquals("android-user@example.com", parceled.getEmail());
        assertPostalAddress(billingPostalAddress, parceled.getBillingAddress());
        assertPostalAddress(shippingPostalAddress, parceled.getShippingAddress());

        assertBinDataEqual(googlePaymentCardNonce.getBinData(), parceled.getBinData());
    }

    private PostalAddress getPostalAddressObject(JSONObject address) throws JSONException {
        return new PostalAddress()
                .recipientName(Json.optString(address, "name", ""))
                .streetAddress(Json.optString(address, "address1", ""))
                .extendedAddress(
                        String.join("\n",
                                Json.optString(address, "address2", ""),
                                Json.optString(address, "address3", "")
                        ).trim())
                .locality(Json.optString(address, "locality", ""))
                .region(Json.optString(address, "administrativeArea", ""))
                .countryCodeAlpha2(Json.optString(address, "countryCode", ""))
                .postalCode(Json.optString(address, "postalCode", ""));
    }

    private void assertPostalAddress(PostalAddress expected, PostalAddress actual) {
        assertEquals(expected.toString(), actual.toString());
    }
}
