package com.braintreepayments.api.models;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.StringDef;

import com.braintreepayments.api.models.BinData.DurbinRequired;

import org.json.JSONObject;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A class to contain BIN data for the card number associated with this
 * {@link CardNonce}
 */
public class BinData implements Parcelable {

    public static final String BIN_DATA_KEY = "binData";
    public static final String YES = "Yes";
    public static final String NO = "No";
    public static final String UNKNOWN = "Unknown";

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({BinData.YES, BinData.NO, BinData.UNKNOWN})
    @interface Prepaid {}

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({BinData.YES, BinData.NO, BinData.UNKNOWN})
    @interface Healthcare {}

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({BinData.YES, BinData.NO, BinData.UNKNOWN})
    @interface Debit {}

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({BinData.YES, BinData.NO, BinData.UNKNOWN})
    @interface DurbinRequired {}

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({BinData.YES, BinData.NO, BinData.UNKNOWN})
    @interface Commercial {}

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({BinData.YES, BinData.NO, BinData.UNKNOWN})
    @interface Payroll {}

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
        binData.mPrepaid = json.optString(PREPAID_KEY);
        binData.mHealthcare = json.optString(HEALTHCARE_KEY);
        binData.mDebit = json.optString(DEBIT_KEY);
        binData.mDurbinRegulated = json.optString(DURBIN_REGULATED_KEY);
        binData.mCommercial = json.optString(COMMERCIAL_KEY);
        binData.mPayroll = json.optString(PAYROLL_KEY);
        binData.mIssuingBank = json.optString(ISSUING_BANK_KEY);
        binData.mCountryOfIssuance = json.optString(COUNTRY_OF_ISSUANCE_KEY);
        binData.mProductId = json.optString(PRODUCT_ID_KEY);

        return binData;
    }

    @Prepaid
    public String getPrepaid() {
        return mPrepaid;
    }

    @Healthcare
    public String getHealthcare() {
        return mHealthcare;
    }

    @Debit
    public String getDebit() {
        return mDebit;
    }

    @DurbinRequired
    public String getDurbinRegulated() {
        return mDurbinRegulated;
    }

    @Commercial
    public String getCommercial() {
        return mCommercial;
    }

    @Payroll
    public String getPayroll() {
        return mPayroll;
    }

    public String getIssuingBank() {
        return mIssuingBank;
    }

    public String getCountryOfIssuance() {
        return mCountryOfIssuance;
    }

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
