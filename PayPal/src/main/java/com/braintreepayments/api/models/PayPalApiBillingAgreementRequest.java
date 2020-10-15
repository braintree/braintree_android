package com.braintreepayments.api.models;

import android.content.Context;
import android.os.Parcel;

public class PayPalApiBillingAgreementRequest extends PayPalApiCheckoutRequest {

    private static final String TOKEN_QUERY_PARAM_KEY_BA_TOKEN = "ba_token";

    public PayPalApiBillingAgreementRequest() {}

    public PayPalApiBillingAgreementRequest pairingId(Context context, String pairingId) {
        super.pairingId(context, pairingId);
        return this;
    }

    @Override
    public PayPalApiBillingAgreementRequest approvalURL(String approvalURL) {
        super.approvalURL(approvalURL);
        mTokenQueryParamKey = TOKEN_QUERY_PARAM_KEY_BA_TOKEN;
        return this;
    }

    protected PayPalApiBillingAgreementRequest(Parcel source) {
        super(source);
    }

    public static final Creator<PayPalApiBillingAgreementRequest> CREATOR = new Creator<PayPalApiBillingAgreementRequest>() {
        @Override
        public PayPalApiBillingAgreementRequest[] newArray(int size) {
            return new PayPalApiBillingAgreementRequest[size];
        }

        @Override
        public PayPalApiBillingAgreementRequest createFromParcel(Parcel source) {
            return new PayPalApiBillingAgreementRequest(source);
        }
    };
}
