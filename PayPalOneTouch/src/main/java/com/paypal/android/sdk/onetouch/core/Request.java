package com.paypal.android.sdk.onetouch.core;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;

import com.paypal.android.sdk.onetouch.core.base.ContextInspector;
import com.paypal.android.sdk.onetouch.core.config.OtcConfiguration;
import com.paypal.android.sdk.onetouch.core.config.Recipe;
import com.paypal.android.sdk.onetouch.core.enums.Protocol;
import com.paypal.android.sdk.onetouch.core.exception.InvalidEncryptionDataException;
import com.paypal.android.sdk.onetouch.core.fpti.TrackingPoint;

import org.json.JSONException;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public abstract class Request<T extends Request<T>> implements Parcelable {

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

    public abstract String getBrowserSwitchUrl(Context context, OtcConfiguration config) throws CertificateException,
            UnsupportedEncodingException, NoSuchPaddingException, NoSuchAlgorithmException, IllegalBlockSizeException,
            JSONException, BadPaddingException, InvalidEncryptionDataException, InvalidKeyException;

    public abstract Recipe getBrowserSwitchRecipe(OtcConfiguration config);

    public abstract void persistRequiredFields(ContextInspector contextInspector);

    public abstract Result parseBrowserResponse(ContextInspector contextInspector, Uri uri);

    public abstract boolean validateV1V2Response(ContextInspector contextInspector, Bundle extras);

    public abstract Recipe getRecipeToExecute(Context context, OtcConfiguration config, boolean isSecurityEnabled);

    public abstract void trackFpti(Context context, TrackingPoint trackingPoint, Protocol protocol);
}
