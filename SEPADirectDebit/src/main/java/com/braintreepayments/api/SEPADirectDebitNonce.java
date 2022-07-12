package com.braintreepayments.api;

import android.os.Parcel;

import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * {@link PaymentMethodNonce} representing a SEPA Direct Debit payment.
 *
 * @see PaymentMethodNonce
 */
public class SEPADirectDebitNonce extends PaymentMethodNonce {

    private static final String PAYMENT_METHOD_NONCE_KEY = "nonce";
    private static final String DETAILS_KEY = "details";
    private static final String IBAN_LAST_FOUR_KEY = "last4";
    private static final String CUSTOMER_ID_KEY = "merchantOrPartnerCustomerId";
    private static final String MANDATE_TYPE_KEY = "mandateType";

    private final String ibanLastFour;
    private final String customerId;
    private final SEPADirectDebitMandateType mandateType;

    static SEPADirectDebitNonce fromJSON(JSONObject inputJson) throws JSONException {
        String nonce = inputJson.getString(PAYMENT_METHOD_NONCE_KEY);
        JSONObject details = inputJson.optJSONObject(DETAILS_KEY);
        String ibanLastFour = null;
        String customerId = null;
        SEPADirectDebitMandateType mandateType = null;
        if (details != null) {
            ibanLastFour = details.optString(IBAN_LAST_FOUR_KEY);
            customerId = details.optString(CUSTOMER_ID_KEY);
            mandateType = SEPADirectDebitMandateType.fromString(details.optString(MANDATE_TYPE_KEY));
        }

        return new SEPADirectDebitNonce(nonce, false, ibanLastFour, customerId, mandateType);
    }

    private SEPADirectDebitNonce(String nonce, boolean isDefault, String ibanLastFour, String customerId, SEPADirectDebitMandateType mandateType) {
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
     * @return The {@link SEPADirectDebitMandateType}.
     */
    @Nullable
    public SEPADirectDebitMandateType getMandateType() {
        return mandateType;
    }

    private SEPADirectDebitNonce(Parcel in) {
        super(in);
        ibanLastFour = in.readString();
        customerId = in.readString();
        mandateType = SEPADirectDebitMandateType.fromString(in.readString());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(ibanLastFour);
        dest.writeString(customerId);
        dest.writeString(mandateType.toString());
    }

    public static final Creator<SEPADirectDebitNonce> CREATOR = new Creator<SEPADirectDebitNonce>() {
        @Override
        public SEPADirectDebitNonce createFromParcel(Parcel in) {
            return new SEPADirectDebitNonce(in);
        }

        @Override
        public SEPADirectDebitNonce[] newArray(int size) {
            return new SEPADirectDebitNonce[size];
        }
    };
}
