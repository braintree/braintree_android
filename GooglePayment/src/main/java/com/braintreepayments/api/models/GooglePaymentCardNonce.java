package com.braintreepayments.api.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.braintreepayments.api.Json;

import org.json.JSONException;
import org.json.JSONObject;

import androidx.annotation.Nullable;

import static com.braintreepayments.api.models.BinData.BIN_DATA_KEY;

/**
 * {@link PaymentMethodNonce} representing a Google Payments card.
 *
 * @see PaymentMethodNonce
 */
public class GooglePaymentCardNonce extends PaymentMethodNonce implements Parcelable {

    protected static final String API_RESOURCE_KEY = "androidPayCards";
    private static final String CARD_DETAILS_KEY = "details";
    private static final String CARD_TYPE_KEY = "cardType";
    private static final String LAST_TWO_KEY = "lastTwo";
    private static final String LAST_FOUR_KEY = "lastFour";

    private String mCardType;
    private String mLastTwo;
    private String mLastFour;
    private String mEmail;
    private PostalAddress mBillingAddress;
    private PostalAddress mShippingAddress;
    private BinData mBinData;

    /**
     * Convert an API response to a {@link GooglePaymentCardNonce}.
     *
     * @param json Raw JSON response from Braintree of a {@link GooglePaymentCardNonce}.
     * @return {@link GooglePaymentCardNonce}.
     * @throws JSONException when parsing the response fails.
     */
    public static GooglePaymentCardNonce fromJson(String json) throws JSONException {
        GooglePaymentCardNonce googlePaymentCardNonce = new GooglePaymentCardNonce();
        googlePaymentCardNonce.fromJson(new JSONObject(json));

        return googlePaymentCardNonce;
    }

    protected void fromJson(JSONObject json) throws JSONException {

        JSONObject billingAddressJson = new JSONObject();
        JSONObject shippingAddressJson = new JSONObject();

        JSONObject token = PaymentMethodNonceFactory.extractPaymentMethodToken(json.toString());

        JSONObject androidPayCardObject = new JSONObject(token.getJSONArray(API_RESOURCE_KEY).get(0).toString());
        super.fromJson(androidPayCardObject);
        JSONObject details = androidPayCardObject.getJSONObject(CARD_DETAILS_KEY);

        JSONObject info = json
                .getJSONObject("paymentMethodData")
                .getJSONObject("info");

        if (info.has("billingAddress")) {
            billingAddressJson = info.getJSONObject("billingAddress");
        }

        if (json.has("shippingAddress")) {
            shippingAddressJson = json.getJSONObject("shippingAddress");
        }

        mDescription = json
                .getJSONObject("paymentMethodData")
                .get("description").toString();
        mEmail = Json.optString(json, "email", "");
        mBillingAddress = postalAddressFromJson(billingAddressJson);
        mShippingAddress = postalAddressFromJson(shippingAddressJson);

        mBinData = BinData.fromJson(json.optJSONObject(BIN_DATA_KEY));
        mLastTwo = details.getString(LAST_TWO_KEY);
        mLastFour = details.getString(LAST_FOUR_KEY);
        mCardType = details.getString(CARD_TYPE_KEY);
    }

    public static PostalAddress postalAddressFromJson(JSONObject json) {
        PostalAddress address = new PostalAddress();

        address
                .recipientName(Json.optString(json, "name", ""))
                .phoneNumber(Json.optString(json, "phoneNumber", ""))
                .streetAddress(Json.optString(json, "address1", ""))
                .extendedAddress(formatExtendedAddress(json))
                .locality(Json.optString(json, "locality", ""))
                .region(Json.optString(json, "administrativeArea", ""))
                .countryCodeAlpha2(Json.optString(json, "countryCode", ""))
                .postalCode(Json.optString(json, "postalCode", ""))
                .sortingCode(Json.optString(json, "sortingCode", ""));

        return address;
    }

    private static String formatExtendedAddress(JSONObject address) {
        String extendedAddress = "" +
                Json.optString(address, "address2", "") + "\n" +
                Json.optString(address, "address3", "") + "\n" +
                Json.optString(address, "address4", "") + "\n" +
                Json.optString(address, "address5", "");

        return extendedAddress.trim();
    }

    /**
     * @return Type of this card (e.g. Visa, MasterCard, American Express)
     */
    public String getCardType() {
        return mCardType;
    }

    /**
     * @return Last two digits of the user's underlying card, intended for display purposes.
     */
    public String getLastTwo() {
        return mLastTwo;
    }

    /**
     * @return Last four digits of the user's underlying card, intended for display purposes.
     */
    public String getLastFour() {
        return mLastFour;
    }

    /**
     * @return The user's email address associated the Google Payments account.
     */
    @Nullable
    public String getEmail() {
        return mEmail;
    }

    /**
     * @return The user's billing address.
     */
    @Nullable
    public PostalAddress getBillingAddress() {
        return mBillingAddress;
    }

    /**
     * @return The user's shipping address.
     */
    @Nullable
    public PostalAddress getShippingAddress() {
        return mShippingAddress;
    }

    /**
     * @return The BIN data for the card number associated with {@link GooglePaymentCardNonce}
     */
    public BinData getBinData() {
        return mBinData;
    }

    @Override
    public String getTypeLabel() {
        return "Google Pay";
    }

    public GooglePaymentCardNonce() {
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(mCardType);
        dest.writeString(mLastTwo);
        dest.writeString(mLastFour);
        dest.writeString(mEmail);
        dest.writeParcelable(mBillingAddress, flags);
        dest.writeParcelable(mShippingAddress, flags);
        dest.writeParcelable(mBinData, flags);
    }

    private GooglePaymentCardNonce(Parcel in) {
        super(in);
        mCardType = in.readString();
        mLastTwo = in.readString();
        mLastFour = in.readString();
        mEmail = in.readString();
        mBillingAddress = in.readParcelable(PostalAddress.class.getClassLoader());
        mShippingAddress = in.readParcelable(PostalAddress.class.getClassLoader());
        mBinData = in.readParcelable(BinData.class.getClassLoader());
    }

    public static final Creator<GooglePaymentCardNonce> CREATOR = new Creator<GooglePaymentCardNonce>() {
        public GooglePaymentCardNonce createFromParcel(Parcel source) {
            return new GooglePaymentCardNonce(source);
        }

        public GooglePaymentCardNonce[] newArray(int size) {
            return new GooglePaymentCardNonce[size];
        }
    };
}
