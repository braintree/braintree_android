package com.braintreepayments.api;

import android.os.Parcel;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

public class SamsungPayNonce extends PaymentMethodNonce {

    private final String lastFour;
    private final String cardType;
    private final BinData binData;

    @NonNull
    static SamsungPayNonce fromJSON(JSONObject inputJson) throws JSONException {
        JSONObject data = new JSONObject(inputJson.getString("data"));
        JSONObject braintreeData = new JSONObject(data.getString("data"));

        JSONObject paymentMethod = braintreeData
                .getJSONObject("tokenizeSamsungPayCard")
                .getJSONObject("paymentMethod");

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

    public String getLastFour() {
        return lastFour;
    }

    public String getCardType() {
        return cardType;
    }

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
