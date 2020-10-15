package com.braintreepayments.api.models;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

public abstract class PayPalApiRequest<T extends PayPalApiRequest<T>> implements Parcelable {

    private String mEnvironment;
    private String mClientId;
    private String mClientMetadataId;
    private String mCancelUrl;
    private String mSuccessUrl;

    @SuppressWarnings("unchecked")
    public T environment(String environment) {
        mEnvironment = environment;
        return (T) this;
    }

    public String getEnvironment() {
        return mEnvironment;
    }

    @SuppressWarnings("unchecked")
    public T clientMetadataId(String clientMetadataId) {
        mClientMetadataId = clientMetadataId;
        return (T) this;
    }

    public String getClientMetadataId() {
        return mClientMetadataId;
    }

    @SuppressWarnings("unchecked")
    public T clientId(String clientId) {
        mClientId = clientId;
        return (T) this;
    }

    public String getClientId() {
        return mClientId;
    }

    /**
     * Defines the host to be used in the cancellation url for browser switch (the package name will
     * be used as the scheme)
     */
    @SuppressWarnings("unchecked")
    public T cancelUrl(String scheme, String host) {
        mCancelUrl = scheme + "://" + redirectURLHostAndPath() + host;
        return (T) this;
    }

    public String getCancelUrl() {
        return mCancelUrl;
    }

    /**
     * Defines the host to be used in the success url for browser switch (the package name will be
     * used as the scheme)
     */
    @SuppressWarnings("unchecked")
    public T successUrl(String scheme, String host) {
        mSuccessUrl = scheme + "://" + redirectURLHostAndPath() + host;
        return (T) this;
    }

    public String getSuccessUrl() {
        return mSuccessUrl;
    }

    private static String redirectURLHostAndPath() {
        return "onetouch/v1/";
    }

    public abstract String getBrowserSwitchUrl();

    public abstract PayPalApiResult parseBrowserResponse(Uri uri);

    protected PayPalApiRequest() {}

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mEnvironment);
        dest.writeString(mClientId);
        dest.writeString(mClientMetadataId);
        dest.writeString(mCancelUrl);
        dest.writeString(mSuccessUrl);
    }

    protected PayPalApiRequest(Parcel source) {
        mEnvironment = source.readString();
        mClientId = source.readString();
        mClientMetadataId = source.readString();
        mCancelUrl = source.readString();
        mSuccessUrl = source.readString();
    }
}
