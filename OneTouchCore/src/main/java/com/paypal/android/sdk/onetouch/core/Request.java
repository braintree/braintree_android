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
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public abstract class Request<T extends Request<T>> implements Parcelable {
    private String mEnvironment;
    private String mClientId;
    private String mClientMetadataId;
    private String mCancelUrl;
    private String mSuccessUrl;

    public T environment(String environment) {
        this.mEnvironment = environment;
        return getThis();
    }

    public String getEnvironment() {
        return mEnvironment;
    }

    public T clientMetadataId(String clientMetadataId) {
        this.mClientMetadataId = clientMetadataId;
        return getThis();
    }

    public String getClientMetadataId() {
        return mClientMetadataId;
    }

    public T clientId(String clientId) {
        this.mClientId = clientId;
        return getThis();
    }

    public String getClientId() {
        return mClientId;
    }

    protected String getBaseRequestToString() {
        return String.format(
                "mClientId:%s, mEnvironment:%s",
                getClientId(),
                getEnvironment());
    }

    /**
     * Defines the host to be used in the cancelation url for browser switch (the package name will
     * be used as the scheme)
     */
    public T cancelUrl(String scheme, String host) {
        this.mCancelUrl = scheme + "://" + redirectURLHostAndPath() + host;
        return getThis();
    }

    public String getCancelUrl() {
        return mCancelUrl;
    }

    /**
     * Defines the host to be used in the success url for browser switch (the package name will be
     * used as the scheme)
     */
    public T successUrl(String scheme, String host) {
        this.mSuccessUrl = scheme + "://" + redirectURLHostAndPath() + host;
        return getThis();
    }

    public String getSuccessUrl() {
        return mSuccessUrl;
    }

    private static String redirectURLHostAndPath() {
        // Return either an empty string;
        // or else a non-empty `host` or `host/path`, ending with `/`

        return "onetouch/v1/";
    }

    /**
     * Supplies the implementing class type. Useful only for using the Abstract Builder pattern.
     *
     * @return
     */
    protected abstract T getThis();

    /**
     * Returns the browser switch URL for this request.
     *
     * @param context
     * @return
     */
    public abstract String getBrowserSwitchUrl(Context context, OtcConfiguration config)
            throws CertificateException, UnsupportedEncodingException, NoSuchPaddingException,
            NoSuchAlgorithmException, IllegalBlockSizeException, JSONException, BadPaddingException,
            InvalidEncryptionDataException, InvalidKeyException, InvalidKeySpecException;

    public abstract Recipe getBrowserSwitchRecipe(OtcConfiguration config);

    public abstract void persistRequiredFields(ContextInspector contextInspector);

    public abstract Result parseBrowserResponse(ContextInspector contextInspector, Uri uri);

    /**
     * Validates that the response from wallet is something we recognize as good.
     *
     * @param contextInspector
     * @param extras
     * @return
     */
    public abstract boolean validateV1V2Response(ContextInspector contextInspector, Bundle extras);

    public abstract Recipe getRecipeToExecute(Context context, OtcConfiguration config,
            boolean isSecurityEnabled);

    public abstract void trackFpti(Context context, TrackingPoint trackingPoint, Protocol protocol);
}
