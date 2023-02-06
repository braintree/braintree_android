package com.braintreepayments.api;

import android.os.Parcel;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * {@link PaymentMethodNonce} representing a Samsung Pay card.
 *
 * @see PaymentMethodNonce
 */
public class SamsungPayNonce extends PaymentMethodNonce {

    private final String lastFour;
    private final String cardType;
    private final BinData binData;

    @NonNull
    static SamsungPayNonce fromJSON(JSONObject inputJson) throws JSONException {
        JSONObject data = new JSONObject(inputJson.getString("data"));
        JSONObject braintreeData = new JSONObject(data.getString("data"));

        JSONObject tokenizeSamsungPayResponse =
            braintreeData.getJSONObject("tokenizeSamsungPayCard");

        JSONObject paymentMethod = tokenizeSamsungPayResponse.optJSONObject("paymentMethod");
        if (paymentMethod == null) {
            // fallback to single use token key; throws when fallback not present
            paymentMethod = tokenizeSamsungPayResponse.getJSONObject("singleUseToken");
        }

        String nonce = paymentMethod.getString("id");

        JSONObject details = paymentMethod.getJSONObject("details");
        String cardType = details.getString("brand");
        String last4 = details.getString("last4");

        // This is a hack to get around the mismatch between the GraphQL API version used in
        // the Braintree Android SDK and the API version used by Samsung to tokenize. Samsung's
        // response has `UNKNOWN` whereas the Braintree SDK expects `Unknown`.
        String formattedBinData = details.getJSONObject("binData")
                .toString()
                .replace("UNKNOWN", "Unknown")
                .replace("YES", "Yes")
                .replace("NO", "No");
        BinData binData = BinData.fromJson(new JSONObject(formattedBinData));

        return new SamsungPayNonce(nonce, binData, cardType, last4, false);
    }

    /**
     * @return Samsung Pay card last four digits.
     */
    public String getLastFour() {
        return lastFour;
    }

    /**
     * @return Samsung Pay card type.
     */
    public String getCardType() {
        return cardType;
    }

    /**
     * @return Samsung Pay card {@link BinData}
     */
    public BinData getBinData() {
        return binData;
    }

    SamsungPayNonce(@NonNull String nonce, BinData binData, String cardType, String last4, boolean isDefault) {
        super(nonce, isDefault);
        this.binData = binData;
        this.cardType = cardType;
        this.lastFour = last4;
    }

    SamsungPayNonce(Parcel in) {
        super(in);
        cardType = in.readString();
        lastFour = in.readString();
        binData = in.readParcelable(BinData.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(cardType);
        dest.writeString(lastFour);
        dest.writeParcelable(binData, flags);
    }

    public static final Creator<SamsungPayNonce> CREATOR = new Creator<SamsungPayNonce>() {
        public SamsungPayNonce createFromParcel(Parcel source) {
            return new SamsungPayNonce(source);
        }

        public SamsungPayNonce[] newArray(int size) {
            return new SamsungPayNonce[size];
        }
    };
}
