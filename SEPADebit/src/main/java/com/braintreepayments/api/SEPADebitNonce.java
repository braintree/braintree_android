package com.braintreepayments.api;

import android.os.Parcel;

import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * {@link PaymentMethodNonce} representing a SEPA Debit payment.
 *
 * @see PaymentMethodNonce
 */
public class SEPADebitNonce extends PaymentMethodNonce {

    private static final String PAYMENT_METHOD_NONCE_KEY = "nonce";
    private static final String DETAILS_KEY = "details";
    private static final String IBAN_LAST_FOUR_KEY = "ibanLastChars";
    private static final String CUSTOMER_ID_KEY = "customerId";
    private static final String MANDATE_TYPE_KEY = "mandateType";

    private final String ibanLastFour;
    private final String customerId;
    private final String mandateType;

    static SEPADebitNonce fromJSON(JSONObject inputJson) throws JSONException {
        String nonce = inputJson.getString(PAYMENT_METHOD_NONCE_KEY);
        JSONObject details = inputJson.optJSONObject(DETAILS_KEY);
        String ibanLastFour = null;
        String customerId = null;
        String mandateType = null;
        if (details != null) {
            ibanLastFour = details.optString(IBAN_LAST_FOUR_KEY);
            customerId = details.optString(CUSTOMER_ID_KEY);
            mandateType = details.optString(MANDATE_TYPE_KEY);
        }

        return new SEPADebitNonce(nonce, false, ibanLastFour, customerId, mandateType);
    }

    private SEPADebitNonce(String nonce, boolean isDefault, String ibanLastFour, String customerId, String mandateType) {
        super(nonce, isDefault);
        this.ibanLastFour = ibanLastFour;
        this.customerId = customerId;
        this.mandateType = mandateType;
    }

    /**
     * @return The IBAN last four characters.
     */
    @Nullable
    public String getIbanLastFour() {
        return ibanLastFour;
    }

    /**
     * @return The customer ID.
     */
    @Nullable
    public String getCustomerId() {
        return customerId;
    }

    /**
     * @return The mandate type - either recurring or one off.
     */
    @SEPADebitMandateType
    @Nullable
    public String getMandateType() {
        return mandateType;
    }

    private SEPADebitNonce(Parcel in) {
        super(in);
        ibanLastFour = in.readString();
        customerId = in.readString();
        mandateType = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(ibanLastFour);
        dest.writeString(customerId);
        dest.writeString(mandateType);
    }

    public static final Creator<SEPADebitNonce> CREATOR = new Creator<SEPADebitNonce>() {
        @Override
        public SEPADebitNonce createFromParcel(Parcel in) {
            return new SEPADebitNonce(in);
        }

        @Override
        public SEPADebitNonce[] newArray(int size) {
            return new SEPADebitNonce[size];
        }
    };
}
