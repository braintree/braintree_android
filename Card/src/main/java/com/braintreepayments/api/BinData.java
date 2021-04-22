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

    private String prepaid;
    private String healthcare;
    private String debit;
    private String durbinRegulated;
    private String commercial;
    private String payroll;
    private String issuingBank;
    private String countryOfIssuance;
    private String productId;

    protected static BinData fromJson(JSONObject json) {
        if (json == null) {
            json = new JSONObject();
        }

        BinData binData = new BinData();
        binData.prepaid = Json.optString(json, PREPAID_KEY, UNKNOWN);
        binData.healthcare = Json.optString(json, HEALTHCARE_KEY, UNKNOWN);
        binData.debit = Json.optString(json, DEBIT_KEY, UNKNOWN);
        binData.durbinRegulated = Json.optString(json, DURBIN_REGULATED_KEY, UNKNOWN);
        binData.commercial = Json.optString(json, COMMERCIAL_KEY, UNKNOWN);
        binData.payroll = Json.optString(json, PAYROLL_KEY, UNKNOWN);

        binData.issuingBank = convertNullToUnknown(json, ISSUING_BANK_KEY);
        binData.countryOfIssuance = convertNullToUnknown(json, COUNTRY_OF_ISSUANCE_KEY);
        binData.productId = convertNullToUnknown(json, PRODUCT_ID_KEY);

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
        return prepaid;
    }

    /**
     * @return Whether the card is a healthcare card. Possible {@link BinType} values are {@link BinData#YES}, {@link BinData#NO} or {@link BinData#UNKNOWN}
     */
    @BinType
    public String getHealthcare() {
        return healthcare;
    }

    /**
     * @return Whether the card is a debit card. Possible {@link BinType} values are {@link BinData#YES}, {@link BinData#NO} or {@link BinData#UNKNOWN}
     */
    @BinType
    public String getDebit() {
        return debit;
    }

    /**
     * @return A value indicating whether the issuing bank's card range is regulated by the Durbin Amendment due to the bank's assets. Possible {@link BinType} values are {@link BinData#YES}, {@link BinData#NO} or {@link BinData#UNKNOWN}
     */
    @BinType
    public String getDurbinRegulated() {
        return durbinRegulated;
    }

    /**
     * @return Whether the card type is a commercial card and is capable of processing Level 2 transactions. Possible {@link BinType} values are {@link BinData#YES}, {@link BinData#NO} or {@link BinData#UNKNOWN}
     */
    @BinType
    public String getCommercial() {
        return commercial;
    }

    /**
     * @return Whether the card is a payroll card. Possible {@link BinType} values are {@link BinData#YES}, {@link BinData#NO} or {@link BinData#UNKNOWN}
     */
    @BinType
    public String getPayroll() {
        return payroll;
    }

    /**
     * @return The bank that issued the credit card.
     */
    public String getIssuingBank() {
        return issuingBank;
    }

    /**
     * @return The country that issued the credit card.
     */
    public String getCountryOfIssuance() {
        return countryOfIssuance;
    }

    /**
     * @return The code for the product type of the card (e.g. `D` (Visa Signature Preferred), `G` (Visa Business)).
     */
    public String getProductId() {
        return productId;
    }

    public BinData() {}

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int i) {
        dest.writeString(prepaid);
        dest.writeString(healthcare);
        dest.writeString(debit);
        dest.writeString(durbinRegulated);
        dest.writeString(commercial);
        dest.writeString(payroll);
        dest.writeString(issuingBank);
        dest.writeString(countryOfIssuance);
        dest.writeString(productId);
    }

    private BinData(Parcel in) {
        prepaid = in.readString();
        healthcare = in.readString();
        debit = in.readString();
        durbinRegulated = in.readString();
        commercial = in.readString();
        payroll = in.readString();
        issuingBank = in.readString();
        countryOfIssuance = in.readString();
        productId = in.readString();
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
