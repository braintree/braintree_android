package com.braintreepayments.api;

import android.os.Parcel;
import android.os.Parcelable;

public class PayPalNativeCheckoutConfig implements Parcelable {

    private String returnUrl;
    private String correlationId;

    public PayPalNativeCheckoutConfig() {
    }

    /**
     * Optional: The correlation id or ec-token associated with the transaction
     *
     * @param correlationId a string containing the id
     */
    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    /**
     * Optional: The return url used to return from the native client
     *
     * @param returnUrl a string containing the url
     */
    public void setReturnUrl(String returnUrl) {
        this.returnUrl = returnUrl;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public String getReturnUrl() {
        return returnUrl;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(returnUrl);
        parcel.writeString(correlationId);
    }

    private PayPalNativeCheckoutConfig(Parcel in) {
        returnUrl = in.readString();
        correlationId = in.readString();
    }

    public static final Creator<PayPalNativeCheckoutConfig> CREATOR = new Creator<PayPalNativeCheckoutConfig>() {
        public PayPalNativeCheckoutConfig createFromParcel(Parcel source) {
            return new PayPalNativeCheckoutConfig(source);
        }

        public PayPalNativeCheckoutConfig[] newArray(int size) {
            return new PayPalNativeCheckoutConfig[size];
        }
    };
}
