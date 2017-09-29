package com.braintreepayments.api.models;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import com.google.android.gms.identity.intents.model.UserAddress;
import com.google.android.gms.wallet.PaymentData;

import org.json.JSONException;
import org.json.JSONObject;

import static com.braintreepayments.api.models.BinData.BIN_DATA_KEY;

/**
 * {@link PaymentMethodNonce} representing a Google Payments card.
 * @see PaymentMethodNonce
 */
public class GooglePaymentCardNonce extends PaymentMethodNonce implements Parcelable {

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
     * Convert {@link PaymentData} to a {@link GooglePaymentCardNonce}.
     *
     * @param paymentData the {@link PaymentData} from a Google Payments response.
     * @return {@link GooglePaymentCardNonce}.
     * @throws JSONException when parsing the response fails.
     */
    public static GooglePaymentCardNonce fromPaymentData(PaymentData paymentData) throws JSONException {
        GooglePaymentCardNonce googlePaymentCardNonce = GooglePaymentCardNonce
                .fromJson(paymentData.getPaymentMethodToken().getToken());
        googlePaymentCardNonce.mDescription = paymentData.getCardInfo().getCardDescription();
        googlePaymentCardNonce.mEmail = paymentData.getEmail();
        googlePaymentCardNonce.mBillingAddress = paymentData.getCardInfo().getBillingAddress();
        googlePaymentCardNonce.mShippingAddress = paymentData.getShippingAddress();

        return googlePaymentCardNonce;
    }

    /**
     * Convert an API response to a {@link GooglePaymentCardNonce}.
     *
     * @param json Raw JSON response from Braintree of a {@link GooglePaymentCardNonce}.
     * @return {@link GooglePaymentCardNonce}.
     * @throws JSONException when parsing the response fails.
     */
    public static GooglePaymentCardNonce fromJson(String json) throws JSONException {
        GooglePaymentCardNonce googlePaymentCardNonce = new GooglePaymentCardNonce();
        googlePaymentCardNonce.fromJson(GooglePaymentCardNonce.getJsonObjectForType(API_RESOURCE_KEY, json));

        return googlePaymentCardNonce;
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
    @Nullable
    public String getEmail() {
        return mEmail;
    }

    /**
     * @return The user's billing address.
     */
    @Nullable
    public UserAddress getBillingAddress() {
        return mBillingAddress;
    }

    /**
     * @return The user's shipping address.
     */
    @Nullable
    public UserAddress getShippingAddress() {
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
        return "Google Payments";
    }

    public GooglePaymentCardNonce() {}

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

    private GooglePaymentCardNonce(Parcel in) {
        super(in);
        mCardType = in.readString();
        mLastTwo = in.readString();
        mEmail = in.readString();
        mBillingAddress = in.readParcelable(UserAddress.class.getClassLoader());
        mShippingAddress = in.readParcelable(UserAddress.class.getClassLoader());
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
