package com.paypal.android.sdk.onetouch.core;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Base64;

import com.braintreepayments.api.Json;
import com.paypal.android.sdk.onetouch.core.base.ContextInspector;
import com.paypal.android.sdk.onetouch.core.base.DeviceInspector;
import com.paypal.android.sdk.onetouch.core.config.ConfigEndpoint;
import com.paypal.android.sdk.onetouch.core.config.OAuth2Recipe;
import com.paypal.android.sdk.onetouch.core.config.OtcConfiguration;
import com.paypal.android.sdk.onetouch.core.config.Recipe;
import com.paypal.android.sdk.onetouch.core.encryption.EncryptionUtils;
import com.paypal.android.sdk.onetouch.core.encryption.OtcCrypto;
import com.paypal.android.sdk.onetouch.core.enums.Protocol;
import com.paypal.android.sdk.onetouch.core.enums.RequestTarget;
import com.paypal.android.sdk.onetouch.core.enums.ResponseType;
import com.paypal.android.sdk.onetouch.core.exception.BrowserSwitchException;
import com.paypal.android.sdk.onetouch.core.exception.InvalidEncryptionDataException;
import com.paypal.android.sdk.onetouch.core.exception.ResponseParsingException;
import com.paypal.android.sdk.onetouch.core.fpti.TrackingPoint;
import com.paypal.android.sdk.onetouch.core.network.EnvironmentManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class AuthorizationRequest extends Request<AuthorizationRequest> implements Parcelable {

    private final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s");
    private final OtcCrypto mOtcCrypto = new OtcCrypto();
    private final HashSet<String> mScopes;
    private final HashMap<String, String> mAdditionalPayloadAttributes;
    private final String mMsgGuid;
    private final byte[] mEncryptionKey;

    private String mPrivacyUrl;
    private String mUserAgreementUrl;

    public AuthorizationRequest(Context context) {
        clientMetadataId(PayPalOneTouchCore.getClientMetadataId(context));

        mMsgGuid = UUID.randomUUID().toString();
        mEncryptionKey = mOtcCrypto.generateRandom256BitKey();
        mAdditionalPayloadAttributes = new HashMap<>();
        mScopes = new HashSet<>();
    }

    public AuthorizationRequest withAdditionalPayloadAttribute(String key, String value) {
        mAdditionalPayloadAttributes.put(key, value);
        return this;
    }

    protected Map<String, String> getAdditionalPayloadAttributes() {
        return new HashMap<>(mAdditionalPayloadAttributes);
    }

    public AuthorizationRequest withScopeValue(String scopeValue) {
        Matcher matcher = WHITESPACE_PATTERN.matcher(scopeValue);
        boolean found = matcher.find();
        if (found) {
            throw new IllegalArgumentException("scopes must be provided individually, with no whitespace");
        }

        mScopes.add(scopeValue);
        return this;
    }

    private Set<String> getScopes() {
        return new HashSet<>(mScopes);
    }

    public String getScopeString() {
        return TextUtils.join(" ", getScopes());
    }

    public AuthorizationRequest privacyUrl(String privacyUrl) {
        mPrivacyUrl = privacyUrl;
        return this;
    }

    public String getPrivacyUrl() {
        return mPrivacyUrl;
    }

    public AuthorizationRequest userAgreementUrl(String userAgreementUrl) {
        mUserAgreementUrl = userAgreementUrl;
        return this;
    }

    public String getUserAgreementUrl() {
        return mUserAgreementUrl;
    }

    @Override
    public String getBrowserSwitchUrl(Context context, OtcConfiguration config) throws CertificateException,
            UnsupportedEncodingException, NoSuchPaddingException, NoSuchAlgorithmException, IllegalBlockSizeException,
            JSONException, BadPaddingException, InvalidEncryptionDataException, InvalidKeyException {
        OAuth2Recipe recipe = config.getBrowserOauth2Config(getScopes());
        ConfigEndpoint configEndpoint = recipe.getEndpoint(getEnvironment());
        X509Certificate cert = EncryptionUtils.getX509CertificateFromBase64String(configEndpoint.certificate);

        return configEndpoint.url
                + "?payload=" + URLEncoder.encode(buildPayload(context, cert), "utf-8")
                + "&payloadEnc=" + URLEncoder.encode(buildPayloadEnc(cert), "utf-8")
                + "&x-source=" + context.getPackageName()
                + "&x-success=" + getSuccessUrl()
                + "&x-cancel=" + getCancelUrl();
    }

    @Override
    public Recipe getBrowserSwitchRecipe(OtcConfiguration config) {
        return config.getBrowserOauth2Config(getScopes());
    }

    private boolean isValidResponse(String msgGUID) {
        return (mMsgGuid.equals(msgGUID));
    }

    private class RFC3339DateFormat extends SimpleDateFormat {
        RFC3339DateFormat() {
            super("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US);
        }
    }

    private String buildPayloadEnc(Certificate cert) throws NoSuchPaddingException, NoSuchAlgorithmException,
            IllegalBlockSizeException, BadPaddingException, InvalidEncryptionDataException, InvalidKeyException,
            JSONException {
        JSONObject payloadEnc = getJsonObjectToEncrypt();
        byte[] output = mOtcCrypto.encryptRSAData(payloadEnc.toString().getBytes(), cert);
        return Base64.encodeToString(output, Base64.NO_WRAP);
    }

    private JSONObject getJsonObjectToEncrypt() throws JSONException {
        JSONObject payloadEnc = new JSONObject();
        payloadEnc.put("timestamp", new RFC3339DateFormat().format(new Date()));
        payloadEnc.put("msg_GUID", mMsgGuid);
        payloadEnc.put("sym_key", EncryptionUtils.byteArrayToHexString(mEncryptionKey));
        String deviceName = DeviceInspector.getDeviceName();
        payloadEnc.put("device_name", deviceName.substring(0, Math.min(deviceName.length(), 30)));
        return payloadEnc;
    }

    private String buildPayload(Context context, X509Certificate cert) {
        JSONObject payload = new JSONObject();
        try {
            payload.put("version", 3);
            payload.put("client_id", getClientId());
            payload.put("app_name", DeviceInspector.getApplicationInfoName(context));
            payload.put("environment", getEnvironment());
            payload.put("environment_url", EnvironmentManager.getEnvironmentUrl(getEnvironment()));
            payload.put("scope", getScopeString());
            payload.put("response_type", "code");
            payload.put("privacy_url", getPrivacyUrl());
            payload.put("agreement_url", getUserAgreementUrl());
            payload.put("client_metadata_id", getClientMetadataId());
            payload.put("key_id", cert.getSerialNumber());

            // If this is false, keep me logged in will not be offered (won't save cookies)
            payload.put("android_chrome_available", isChromeAvailable(context));

            for (Entry<String, String> entry : mAdditionalPayloadAttributes.entrySet()) {
                payload.put(entry.getKey(), entry.getValue());
            }

            return Base64.encodeToString(payload.toString().getBytes(), Base64.NO_WRAP);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isChromeAvailable(Context context) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.paypal.com"));
        intent.setPackage("com.android.chrome");
        return intent.resolveActivity(context.getPackageManager()) != null;
    }

    @Override
    public Result parseBrowserResponse(ContextInspector contextInspector, Uri uri) {
        String status = uri.getLastPathSegment();
        String payloadEnc = uri.getQueryParameter("payloadEnc");
        JSONObject payload;
        try {
            payload = new JSONObject(new String(Base64.decode(uri.getQueryParameter("payload"), Base64.DEFAULT)));
        } catch (NullPointerException | IllegalArgumentException | JSONException e) {
            payload = new JSONObject();
        }

        if (Uri.parse(getSuccessUrl()).getLastPathSegment().equals(status)) {
            if (!payload.has("msg_GUID")) {
                return new Result(new ResponseParsingException("Response incomplete"));
            }

            if (TextUtils.isEmpty(payloadEnc) || !isValidResponse(Json.optString(payload, "msg_GUID", ""))) {
                return new Result(new ResponseParsingException("Response invalid"));
            }

            try {
                JSONObject decryptedPayloadEnc = getDecryptedPayload(payloadEnc);

                String error = Json.optString(payload, "error", "");
                // the string 'null' is coming back in production
                if (!TextUtils.isEmpty(error) && !"null".equals(error)) {
                    return new Result(new BrowserSwitchException(error));
                }

                return new Result(Json.optString(payload, "environment", ""), ResponseType.authorization_code,
                        new JSONObject().put("code", decryptedPayloadEnc.getString("payment_code")),
                        decryptedPayloadEnc.getString("email"));
            } catch (JSONException | InvalidAlgorithmParameterException | NoSuchAlgorithmException
                    | IllegalBlockSizeException | BadPaddingException | NoSuchPaddingException | InvalidKeyException
                    | InvalidEncryptionDataException | IllegalArgumentException e) {
                return new Result(new ResponseParsingException(e));
            }
        } else if (Uri.parse(getCancelUrl()).getLastPathSegment().equals(status)) {
            String error = Json.optString(payload, "error", "");

            // the string 'null' is coming back in production
            if (!TextUtils.isEmpty(error) && !"null".equals(error)) {
                return new Result(new BrowserSwitchException(error));
            } else {
                return new Result();
            }
        } else {
            return new Result(new ResponseParsingException("Response uri invalid"));
        }
    }

    @Override
    public boolean validateV1V2Response(ContextInspector contextInspector, Bundle extras) {
        return true;
    }

    @Override
    public Recipe getRecipeToExecute(Context context, OtcConfiguration config) {
        for (OAuth2Recipe recipe : config.getOauth2Recipes()) {
            if (recipe.isValidForScopes(getScopes())) {
                if (RequestTarget.wallet == recipe.getTarget()) {
                    if (recipe.isValidAppTarget(context)) {
                        return recipe;
                    }
                } else if (RequestTarget.browser == recipe.getTarget()) {
                    try {
                        String browserSwitchUrl = getBrowserSwitchUrl(context, config);
                        if (recipe.isValidBrowserTarget(context, browserSwitchUrl)) {
                            return recipe;
                        }
                    } catch (CertificateException | UnsupportedEncodingException | NoSuchPaddingException
                            | NoSuchAlgorithmException | IllegalBlockSizeException | JSONException | BadPaddingException
                            | InvalidEncryptionDataException | InvalidKeyException ignored) {}
                }
            }
        }

        return null;
    }

    @Override
    public void trackFpti(Context context, TrackingPoint trackingPoint, Protocol protocol) {
        Map<String, String> fptiDataBundle = new HashMap<>();
        fptiDataBundle.put("clid", getClientId());
        PayPalOneTouchCore.getFptiManager(context).trackFpti(trackingPoint, getEnvironment(), fptiDataBundle, protocol);
    }

    private JSONObject getDecryptedPayload(String payloadEnc) throws IllegalBlockSizeException, InvalidKeyException,
            NoSuchAlgorithmException, InvalidAlgorithmParameterException, NoSuchPaddingException, BadPaddingException,
            InvalidEncryptionDataException, JSONException, IllegalArgumentException {
        byte[] base64PayloadEnc = Base64.decode(payloadEnc, Base64.DEFAULT);
        byte[] output = new OtcCrypto().decryptAESCTRData(base64PayloadEnc, mEncryptionKey);

        return new JSONObject(new String(output));
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);

        dest.writeString(mPrivacyUrl);
        dest.writeString(mUserAgreementUrl);
        dest.writeSerializable(mScopes);
        dest.writeSerializable(mAdditionalPayloadAttributes);
        dest.writeString(mMsgGuid);
        dest.writeInt(mEncryptionKey.length);
        dest.writeByteArray(mEncryptionKey);
    }

    private AuthorizationRequest(Parcel source) {
        super(source);

        mPrivacyUrl = source.readString();
        mUserAgreementUrl = source.readString();
        mScopes = (HashSet) source.readSerializable();
        mAdditionalPayloadAttributes = (HashMap) source.readSerializable();
        mMsgGuid = source.readString();
        mEncryptionKey = new byte[source.readInt()];
        source.readByteArray(mEncryptionKey);
    }

    public static final Parcelable.Creator<AuthorizationRequest> CREATOR = new Creator<AuthorizationRequest>() {
        @Override
        public AuthorizationRequest[] newArray(int size) {
            return new AuthorizationRequest[size];
        }

        @Override
        public AuthorizationRequest createFromParcel(Parcel source) {
            return new AuthorizationRequest(source);
        }
    };
}
