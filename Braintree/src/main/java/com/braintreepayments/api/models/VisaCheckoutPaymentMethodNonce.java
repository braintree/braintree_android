package com.braintreepayments.api.models;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;

public class VisaCheckoutPaymentMethodNonce extends PaymentMethodNonce implements Parcelable {
    public static final String TYPE = "VisaCheckoutCard";
    protected static final String API_RESOURCE_KEY = "visaCheckoutCards";

    protected VisaCheckoutPaymentMethodNonce(Parcel in) {
        super(in);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<VisaCheckoutPaymentMethodNonce> CREATOR =
            new Creator<VisaCheckoutPaymentMethodNonce>() {
                @Override
                public VisaCheckoutPaymentMethodNonce createFromParcel(Parcel in) {
                    return new VisaCheckoutPaymentMethodNonce(in);
                }

                @Override
                public VisaCheckoutPaymentMethodNonce[] newArray(int size) {
                    return new VisaCheckoutPaymentMethodNonce[size];
                }
            };

    @Override
    public String getTypeLabel() {
        return "Visa Checkout";
    }

    public static PaymentMethodNonce fromJson(String json) throws JSONException {
        VisaCheckoutPaymentMethodNonce visaCheckoutPaymentMethodNonce = new VisaCheckoutPaymentMethodNonce();
        visaCheckoutPaymentMethodNonce.fromJson(PaymentMethodNonce.getJsonObjectForType(API_RESOURCE_KEY, json));
        return visaCheckoutPaymentMethodNonce;
    }

    private VisaCheckoutPaymentMethodNonce() {
    }
}
