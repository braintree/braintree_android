package com.braintreepayments.api.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.identity.intents.model.UserAddress;
import com.google.android.gms.wallet.PaymentData;

import org.json.JSONException;
import org.json.JSONObject;

import static com.braintreepayments.api.models.BinData.BIN_DATA_KEY;

/**
 * {@link PaymentMethodNonce} representing a Google Payments card.
 * @see PaymentMethodNonce
 */
public class GooglePaymentsCardNonce extends PaymentMethodNonce implements Parcelable {

    private static final String API_RESOURCE_KEY = "androidPayCards";

    private static final String CARD_DETAILS_KEY = "details";
    private static final String CARD_TYPE_KEY = "cardType";
    private static final String LAST_TWO_KEY = "lastTwo";

    private String mCardType;
    private String mLastTwo;
    private String mEmail;
    private UserAddress mBillingAddress;
    private UserAddress mShippingAddress;
    private BinData mBinData;

    /**
     * Convert {@link PaymentData} to a {@link GooglePaymentsCardNonce}.
     *
     * @param paymentData the {@link PaymentData} from a Google Payments response.
     * @return {@link GooglePaymentsCardNonce}.
     * @throws JSONException when parsing the response fails.
     */
    public static GooglePaymentsCardNonce fromPaymentData(PaymentData paymentData) throws JSONException {
        GooglePaymentsCardNonce googlePaymentsCardNonce = GooglePaymentsCardNonce
                .fromJson(paymentData.getPaymentMethodToken().getToken());
        googlePaymentsCardNonce.mDescription = paymentData.getCardInfo().getCardDescription();
        googlePaymentsCardNonce.mEmail = paymentData.getEmail();
        googlePaymentsCardNonce.mBillingAddress = paymentData.getCardInfo().getBillingAddress();
        googlePaymentsCardNonce.mShippingAddress = paymentData.getShippingAddress();

        return googlePaymentsCardNonce;
    }

    /**
     * Convert an API response to a {@link GooglePaymentsCardNonce}.
     *
     * @param json Raw JSON response from Braintree of a {@link GooglePaymentsCardNonce}.
     * @return {@link GooglePaymentsCardNonce}.
     * @throws JSONException when parsing the response fails.
     */
    public static GooglePaymentsCardNonce fromJson(String json) throws JSONException {
        GooglePaymentsCardNonce googlePaymentsCardNonce = new GooglePaymentsCardNonce();
        googlePaymentsCardNonce.fromJson(GooglePaymentsCardNonce.getJsonObjectForType(API_RESOURCE_KEY, json));

        return googlePaymentsCardNonce;
    }

    protected void fromJson(JSONObject json) throws JSONException {
        super.fromJson(json);

        mDescription = getTypeLabel();
        mBinData = BinData.fromJson(json.optJSONObject(BIN_DATA_KEY));
        JSONObject details = json.getJSONObject(CARD_DETAILS_KEY);
        mLastTwo = details.getString(LAST_TWO_KEY);
        mCardType = details.getString(CARD_TYPE_KEY);
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
     * @return The user's email address associated the Google Payments account.
     */
    public String getEmail() {
        return mEmail;
    }

    /**
     * @return The user's billing address.
     */
    public UserAddress getBillingAddress() {
        return mBillingAddress;
    }

    /**
     * @return The user's shipping address.
     */
    public UserAddress getShippingAddress() {
        return mShippingAddress;
    }

    /**
     * @return The BIN data for the card number associated with {@link GooglePaymentsCardNonce}
     */
    public BinData getBinData() {
        return mBinData;
    }

    @Override
    public String getTypeLabel() {
        return "Google Payments";
    }

    public GooglePaymentsCardNonce() {}

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(mCardType);
        dest.writeString(mLastTwo);
        dest.writeString(mEmail);
        dest.writeParcelable(mBillingAddress, flags);
        dest.writeParcelable(mShippingAddress, flags);
        dest.writeParcelable(mBinData, flags);
    }

    private GooglePaymentsCardNonce(Parcel in) {
        super(in);
        mCardType = in.readString();
        mLastTwo = in.readString();
        mEmail = in.readString();
        mBillingAddress = in.readParcelable(UserAddress.class.getClassLoader());
        mShippingAddress = in.readParcelable(UserAddress.class.getClassLoader());
        mBinData = in.readParcelable(BinData.class.getClassLoader());
    }

    public static final Creator<GooglePaymentsCardNonce> CREATOR = new Creator<GooglePaymentsCardNonce>() {
        public GooglePaymentsCardNonce createFromParcel(Parcel source) {
            return new GooglePaymentsCardNonce(source);
        }

        public GooglePaymentsCardNonce[] newArray(int size) {
            return new GooglePaymentsCardNonce[size];
        }
    };
}
