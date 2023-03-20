package com.braintreepayments.api;

import android.os.Parcel;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import static com.braintreepayments.api.BinData.BIN_DATA_KEY;

/**
 * {@link PaymentMethodNonce} representing a Google Pay card.
 *
 * @see PaymentMethodNonce
 */
public class GooglePayCardNonce extends PaymentMethodNonce {

    static final String API_RESOURCE_KEY = "androidPayCards";

    private static final String CARD_DETAILS_KEY = "details";
    private static final String CARD_TYPE_KEY = "cardType";
    private static final String LAST_TWO_KEY = "lastTwo";
    private static final String LAST_FOUR_KEY = "lastFour";
    private static final String IS_NETWORK_TOKENIZED_KEY = "isNetworkTokenized";

    private static final String PAYMENT_METHOD_NONCE_KEY = "nonce";
    private static final String PAYMENT_METHOD_DEFAULT_KEY = "default";

    private final String cardType;
    private final String lastTwo;
    private final String lastFour;
    private final String email;
    private boolean isNetworkTokenized;
    private final PostalAddress billingAddress;
    private final PostalAddress shippingAddress;
    private final BinData binData;

    static PaymentMethodNonce fromJSON(JSONObject inputJson) throws JSONException {
        JSONObject tokenPayload = new JSONObject(inputJson
                .getJSONObject("paymentMethodData")
                .getJSONObject("tokenizationData")
                .getString("token"));

        if (tokenPayload.has(GooglePayCardNonce.API_RESOURCE_KEY)) {
            return GooglePayCardNonce.fromGooglePayJSON(inputJson);
        } else if (tokenPayload.has(PayPalAccountNonce.API_RESOURCE_KEY)) {
            return PayPalAccountNonce.fromJSON(inputJson);
        } else {
            throw new JSONException("Could not parse JSON for a payment method nonce");
        }
    }

    private static GooglePayCardNonce fromGooglePayJSON(JSONObject inputJson) throws JSONException {
        JSONObject tokenPayload = new JSONObject(inputJson
                .getJSONObject("paymentMethodData")
                .getJSONObject("tokenizationData")
                .getString("token"));

        JSONObject androidPayCardObject = tokenPayload.getJSONArray(API_RESOURCE_KEY).getJSONObject(0);
        String nonce = androidPayCardObject.getString(PAYMENT_METHOD_NONCE_KEY);
        boolean isDefault = androidPayCardObject.optBoolean(PAYMENT_METHOD_DEFAULT_KEY, false);

        JSONObject details = androidPayCardObject.getJSONObject(CARD_DETAILS_KEY);
        JSONObject info = inputJson
                .getJSONObject("paymentMethodData")
                .getJSONObject("info");

        JSONObject billingAddressJson = new JSONObject();
        if (info.has("billingAddress")) {
            billingAddressJson = info.getJSONObject("billingAddress");
        }

        JSONObject shippingAddressJson = new JSONObject();
        if (inputJson.has("shippingAddress")) {
            shippingAddressJson = inputJson.getJSONObject("shippingAddress");
        }

        String email = Json.optString(inputJson, "email", "");
        PostalAddress billingAddress = postalAddressFromJson(billingAddressJson);
        PostalAddress shippingAddress = postalAddressFromJson(shippingAddressJson);

        BinData binData = BinData.fromJson(inputJson.optJSONObject(BIN_DATA_KEY));
        String lastTwo = details.getString(LAST_TWO_KEY);
        String lastFour = details.getString(LAST_FOUR_KEY);
        String cardType = details.getString(CARD_TYPE_KEY);
        boolean isNetworkTokenized = details.optBoolean(IS_NETWORK_TOKENIZED_KEY, false);

        return new GooglePayCardNonce(cardType, lastTwo, lastFour, email, isNetworkTokenized, billingAddress, shippingAddress, binData, nonce, isDefault);
    }

    GooglePayCardNonce(
            String cardType,
            String lastTwo,
            String lastFour,
            String email,
            boolean isNetworkTokenized,
            PostalAddress billingAddress,
            PostalAddress shippingAddress,
            BinData binData,
            String nonce,
            boolean isDefault
    ) {
        super(nonce, isDefault);
        this.cardType = cardType;
        this.lastTwo = lastTwo;
        this.lastFour = lastFour;
        this.email = email;
        this.isNetworkTokenized = isNetworkTokenized;
        this.billingAddress = billingAddress;
        this.shippingAddress = shippingAddress;
        this.binData = binData;
    }

    static PostalAddress postalAddressFromJson(JSONObject json) {
        PostalAddress address = new PostalAddress();

        address.setRecipientName(Json.optString(json, "name", ""));
        address.setPhoneNumber(Json.optString(json, "phoneNumber", ""));
        address.setStreetAddress(Json.optString(json, "address1", ""));
        address.setExtendedAddress(formatExtendedAddress(json));
        address.setLocality(Json.optString(json, "locality", ""));
        address.setRegion(Json.optString(json, "administrativeArea", ""));
        address.setCountryCodeAlpha2(Json.optString(json, "countryCode", ""));
        address.setPostalCode(Json.optString(json, "postalCode", ""));
        address.setSortingCode(Json.optString(json, "sortingCode", ""));

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
    @NonNull
    public String getCardType() {
        return cardType;
    }

    /**
     * @return Last two digits of the user's underlying card, intended for display purposes.
     */
    @NonNull
    public String getLastTwo() {
        return lastTwo;
    }

    /**
     * @return Last four digits of the user's underlying card, intended for display purposes.
     */
    @NonNull
    public String getLastFour() {
        return lastFour;
    }

    /**
     * @return The user's email address associated the Google Pay account.
     */
    @NonNull
    public String getEmail() {
        return email;
    }

    /**
     * @return true if the card is network tokenized.
     */
    public boolean isNetworkTokenized() {
        return isNetworkTokenized;
    }

    /**
     * @return The user's billing address.
     */
    @NonNull
    public PostalAddress getBillingAddress() {
        return billingAddress;
    }

    /**
     * @return The user's shipping address.
     */
    @NonNull
    public PostalAddress getShippingAddress() {
        return shippingAddress;
    }

    /**
     * @return The BIN data for the card number associated with {@link GooglePayCardNonce}
     */
    @NonNull
    public BinData getBinData() {
        return binData;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(cardType);
        dest.writeString(lastTwo);
        dest.writeString(lastFour);
        dest.writeString(email);
        dest.writeParcelable(billingAddress, flags);
        dest.writeParcelable(shippingAddress, flags);
        dest.writeParcelable(binData, flags);
        dest.writeByte(isNetworkTokenized ? (byte) 1 : (byte) 0);
    }

    private GooglePayCardNonce(Parcel in) {
        super(in);
        cardType = in.readString();
        lastTwo = in.readString();
        lastFour = in.readString();
        email = in.readString();
        billingAddress = in.readParcelable(PostalAddress.class.getClassLoader());
        shippingAddress = in.readParcelable(PostalAddress.class.getClassLoader());
        binData = in.readParcelable(BinData.class.getClassLoader());
        isNetworkTokenized = in.readByte() > 0;
    }

    public static final Creator<GooglePayCardNonce> CREATOR = new Creator<GooglePayCardNonce>() {
        public GooglePayCardNonce createFromParcel(Parcel source) {
            return new GooglePayCardNonce(source);
        }

        public GooglePayCardNonce[] newArray(int size) {
            return new GooglePayCardNonce[size];
        }
    };
}
