package com.braintreepayments.api;

import android.os.Parcel;

import org.json.JSONException;
import org.json.JSONObject;

import static com.braintreepayments.api.BinData.BIN_DATA_KEY;


/**
 * {@link PaymentMethodNonce} representing a Visa Checkout card.
 * @see PaymentMethodNonce
 */
public class VisaCheckoutNonce extends PaymentMethodNonce {

    private static final String API_RESOURCE_KEY = "visaCheckoutCards";

    private static final String PAYMENT_METHOD_NONCE_KEY = "nonce";
    private static final String PAYMENT_METHOD_DEFAULT_KEY = "default";

    private static final String CARD_DETAILS_KEY = "details";
    private static final String CARD_TYPE_KEY = "cardType";
    private static final String LAST_TWO_KEY = "lastTwo";
    private static final String BILLING_ADDRESS_KEY = "billingAddress";
    private static final String SHIPPING_ADDRESS_KEY = "shippingAddress";
    private static final String USER_DATA_KEY = "userData";
    private static final String CALL_ID_KEY = "callId";

    private final String lastTwo;
    private final String cardType;
    private final VisaCheckoutAddress billingAddress;
    private final VisaCheckoutAddress shippingAddress;
    private final VisaCheckoutUserData userData;
    private final String callId;
    private final BinData binData;

    static VisaCheckoutNonce fromJSON(JSONObject inputJson) throws JSONException {
        JSONObject json;
        if (inputJson.has(API_RESOURCE_KEY)) {
            json = inputJson.getJSONArray(API_RESOURCE_KEY).getJSONObject(0);
        } else {
            json = inputJson;
        }

        JSONObject details = json.getJSONObject(CARD_DETAILS_KEY);
        String lastTwo = details.getString(LAST_TWO_KEY);
        String cardType = details.getString(CARD_TYPE_KEY);
        VisaCheckoutAddress billingAddress = VisaCheckoutAddress.fromJson(json.optJSONObject(BILLING_ADDRESS_KEY));
        VisaCheckoutAddress shippingAddress = VisaCheckoutAddress.fromJson(json.optJSONObject(SHIPPING_ADDRESS_KEY));
        VisaCheckoutUserData userData = VisaCheckoutUserData.fromJson(json.optJSONObject(USER_DATA_KEY));
        String callId = Json.optString(json, CALL_ID_KEY, "");
        BinData binData = BinData.fromJson(json.optJSONObject(BIN_DATA_KEY));

        String nonce = json.getString(PAYMENT_METHOD_NONCE_KEY);
        boolean isDefault = json.optBoolean(PAYMENT_METHOD_DEFAULT_KEY, false);

        return new VisaCheckoutNonce(lastTwo, cardType, billingAddress, shippingAddress, userData, callId, binData, nonce, isDefault);
    }

    private VisaCheckoutNonce(String lastTwo, String cardType, VisaCheckoutAddress billingAddress, VisaCheckoutAddress shippingAddress, VisaCheckoutUserData userData, String callId, BinData binData, String nonce, boolean isDefault) {
        super(nonce, isDefault, PaymentMethodType.VISA_CHECKOUT, "Visa Checkout", "Visa Checkout");
        this.lastTwo = lastTwo;
        this.cardType = cardType;
        this.billingAddress = billingAddress;
        this.shippingAddress = shippingAddress;
        this.userData = userData;
        this.callId = callId;
        this.binData = binData;
    }

    /**
     * @return Last two digits of the user's underlying card, intended for display purposes.
     */
    public String getLastTwo() {
        return lastTwo;
    }

    /**
     * @return Type of this card (e.g. Visa, MasterCard, American Express)
     */
    public String getCardType() {
        return cardType;
    }

    /**
     * @return The user's billing address.
     */
    public VisaCheckoutAddress getBillingAddress() {
        return billingAddress;
    }

    /**
     * @return The user's shipping address.
     */
    public VisaCheckoutAddress getShippingAddress() {
        return shippingAddress;
    }

    /**
     * @return The user's data.
     */
    public VisaCheckoutUserData getUserData() {
        return userData;
    }

    /**
     * @return The Call ID from the VisaPaymentSummary.
     */
    public String getCallId() {
        return callId;
    }

    /**
     * @return The BIN data for the card number associated with {@link VisaCheckoutNonce}
     */
    public BinData getBinData() {
        return binData;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(lastTwo);
        dest.writeString(cardType);
        dest.writeParcelable(billingAddress, flags);
        dest.writeParcelable(shippingAddress, flags);
        dest.writeParcelable(userData, flags);
        dest.writeString(callId);
        dest.writeParcelable(binData, flags);
    }

    protected VisaCheckoutNonce(Parcel in) {
        super(in);
        lastTwo = in.readString();
        cardType = in.readString();
        billingAddress = in.readParcelable(VisaCheckoutAddress.class.getClassLoader());
        shippingAddress = in.readParcelable(VisaCheckoutAddress.class.getClassLoader());
        userData = in.readParcelable(VisaCheckoutUserData.class.getClassLoader());
        callId = in.readString();
        binData = in.readParcelable(BinData.class.getClassLoader());
    }

    public static final Creator<VisaCheckoutNonce> CREATOR =
            new Creator<VisaCheckoutNonce>() {
                @Override
                public VisaCheckoutNonce createFromParcel(Parcel in) {
                    return new VisaCheckoutNonce(in);
                }

                @Override
                public VisaCheckoutNonce[] newArray(int size) {
                    return new VisaCheckoutNonce[size];
                }
            };
}
