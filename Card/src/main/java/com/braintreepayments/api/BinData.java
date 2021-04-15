package com.braintreepayments.api;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.StringDef;

import org.json.JSONObject;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A class to contain BIN data for the card number
 */
public class BinData implements Parcelable {

    public static final String BIN_DATA_KEY = "binData";

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({BinData.YES, BinData.NO, BinData.UNKNOWN})
    @interface BinType {}
    public static final String YES = "Yes";
    public static final String NO = "No";
    public static final String UNKNOWN = "Unknown";

    private static final String PREPAID_KEY = "prepaid";
    private static final String HEALTHCARE_KEY = "healthcare";
    private static final String DEBIT_KEY = "debit";
    private static final String DURBIN_REGULATED_KEY = "durbinRegulated";
    private static final String COMMERCIAL_KEY = "commercial";
    private static final String PAYROLL_KEY = "payroll";
    private static final String ISSUING_BANK_KEY = "issuingBank";
    private static final String COUNTRY_OF_ISSUANCE_KEY = "countryOfIssuance";
    private static final String PRODUCT_ID_KEY = "productId";

    private String mPrepaid;
    private String mHealthcare;
    private String mDebit;
    private String mDurbinRegulated;
    private String mCommercial;
    private String mPayroll;
    private String mIssuingBank;
    private String mCountryOfIssuance;
    private String mProductId;

    protected static BinData fromJson(JSONObject json) {
        if (json == null) {
            json = new JSONObject();
        }

        BinData binData = new BinData();
        binData.mPrepaid = Json.optString(json, PREPAID_KEY, UNKNOWN);
        binData.mHealthcare = Json.optString(json, HEALTHCARE_KEY, UNKNOWN);
        binData.mDebit = Json.optString(json, DEBIT_KEY, UNKNOWN);
        binData.mDurbinRegulated = Json.optString(json, DURBIN_REGULATED_KEY, UNKNOWN);
        binData.mCommercial = Json.optString(json, COMMERCIAL_KEY, UNKNOWN);
        binData.mPayroll = Json.optString(json, PAYROLL_KEY, UNKNOWN);

        binData.mIssuingBank = convertNullToUnknown(json, ISSUING_BANK_KEY);
        binData.mCountryOfIssuance = convertNullToUnknown(json, COUNTRY_OF_ISSUANCE_KEY);
        binData.mProductId = convertNullToUnknown(json, PRODUCT_ID_KEY);

        return binData;
    }

    private static String convertNullToUnknown(JSONObject json, String key) {
        if (json.has(key) && json.isNull(key)) {
            return UNKNOWN;
        } else {
            return Json.optString(json, key, "");
        }
    }

    /**
     * @return Whether the card is a prepaid card. Possible {@link BinType} values are {@link BinData#YES}, {@link BinData#NO} or {@link BinData#UNKNOWN}
     */
    @BinType
    public String getPrepaid() {
        return mPrepaid;
    }

    /**
     * @return Whether the card is a healthcare card. Possible {@link BinType} values are {@link BinData#YES}, {@link BinData#NO} or {@link BinData#UNKNOWN}
     */
    @BinType
    public String getHealthcare() {
        return mHealthcare;
    }

    /**
     * @return Whether the card is a debit card. Possible {@link BinType} values are {@link BinData#YES}, {@link BinData#NO} or {@link BinData#UNKNOWN}
     */
    @BinType
    public String getDebit() {
        return mDebit;
    }

    /**
     * @return A value indicating whether the issuing bank's card range is regulated by the Durbin Amendment due to the bank's assets. Possible {@link BinType} values are {@link BinData#YES}, {@link BinData#NO} or {@link BinData#UNKNOWN}
     */
    @BinType
    public String getDurbinRegulated() {
        return mDurbinRegulated;
    }

    /**
     * @return Whether the card type is a commercial card and is capable of processing Level 2 transactions. Possible {@link BinType} values are {@link BinData#YES}, {@link BinData#NO} or {@link BinData#UNKNOWN}
     */
    @BinType
    public String getCommercial() {
        return mCommercial;
    }

    /**
     * @return Whether the card is a payroll card. Possible {@link BinType} values are {@link BinData#YES}, {@link BinData#NO} or {@link BinData#UNKNOWN}
     */
    @BinType
    public String getPayroll() {
        return mPayroll;
    }

    /**
     * @return The bank that issued the credit card.
     */
    public String getIssuingBank() {
        return mIssuingBank;
    }

    /**
     * @return The country that issued the credit card.
     */
    public String getCountryOfIssuance() {
        return mCountryOfIssuance;
    }

    /**
     * @return The code for the product type of the card (e.g. `D` (Visa Signature Preferred), `G` (Visa Business)).
     */
    public String getProductId() {
        return mProductId;
    }

    public BinData() {}

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int i) {
        dest.writeString(mPrepaid);
        dest.writeString(mHealthcare);
        dest.writeString(mDebit);
        dest.writeString(mDurbinRegulated);
        dest.writeString(mCommercial);
        dest.writeString(mPayroll);
        dest.writeString(mIssuingBank);
        dest.writeString(mCountryOfIssuance);
        dest.writeString(mProductId);
    }

    private BinData(Parcel in) {
        mPrepaid = in.readString();
        mHealthcare = in.readString();
        mDebit = in.readString();
        mDurbinRegulated = in.readString();
        mCommercial = in.readString();
        mPayroll = in.readString();
        mIssuingBank = in.readString();
        mCountryOfIssuance = in.readString();
        mProductId = in.readString();
    }

    public static final Creator<BinData> CREATOR = new Creator<BinData>() {
        public BinData createFromParcel(Parcel source) {
            return new BinData(source);
        }

        public BinData[] newArray(int size) {
            return new BinData[size];
        }
    };
}
