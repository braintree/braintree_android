package com.paypal.android.sdk.onetouch.core;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

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

    private static final String TAG = AuthorizationRequest.class.getSimpleName();

    private static final String PREFS_ENCRYPTION_KEY = "com.paypal.otc.key";
    private static final String PREFS_MSG_GUID = "com.paypal.otc.msg_guid";

    // Environments
    public static final String ENVIRONMENT_LIVE = EnvironmentManager.LIVE;
    public static final String ENVIRONMENT_MOCK = EnvironmentManager.MOCK;
    public static final String ENVIRONMENT_SANDBOX = EnvironmentManager.SANDBOX;

    private final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s");
    private final OtcCrypto mOtcCrypto = new OtcCrypto();
    private final HashSet<String> mScopes;
    private final HashMap<String, String> mAdditionalPayloadAttributes;
    private final String mMsgGuid;
    private final byte[] mEncryptionKey;

    private String mPrivacyUrl;
    private String mUserAgreementUrl;

    /**
     * Constructs a new `PayPalAuthorizationRequest` with ids initialized.
     */
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

    public AuthorizationRequest withScopeValue(String scopeValue) {
        Matcher matcher = WHITESPACE_PATTERN.matcher(scopeValue);
        boolean found = matcher.find();
        if (found) {
            throw new IllegalArgumentException(
                    "scopes must be provided individually, with no whitespace");
        }

        mScopes.add(scopeValue);
        return this;
    }

    private Set<String> getScopes() {
        // return copy so it doesn't modify original
        return new HashSet<>(mScopes);
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
    public String toString() {
        return String.format(
                AuthorizationRequest.class.getSimpleName() + ": {" + getBaseRequestToString() +
                        ", " +
                        "privacyUrl:%s, userAgreementUrl:%s, scopeValues:%s}",
                getClientId(),
                mPrivacyUrl,
                mUserAgreementUrl,
                mScopes);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(getClientMetadataId());
        dest.writeString(getClientId());
        dest.writeString(getEnvironment());
        dest.writeString(mPrivacyUrl);
        dest.writeString(mUserAgreementUrl);
        dest.writeSerializable(mScopes);
        dest.writeSerializable(mAdditionalPayloadAttributes);
        dest.writeString(mMsgGuid);
        dest.writeInt(mEncryptionKey.length);
        dest.writeByteArray(mEncryptionKey);
    }

    /**
     * Constructs a new `PayPalAuthorizationRequest` with data from source parcel
     */
    private AuthorizationRequest(Parcel source) {
        clientMetadataId(source.readString());
        clientId(source.readString());
        environment(source.readString());
        mPrivacyUrl = source.readString();
        mUserAgreementUrl = source.readString();
        mScopes = (HashSet) source.readSerializable();
        mAdditionalPayloadAttributes = (HashMap) source.readSerializable();
        mMsgGuid = source.readString();
        mEncryptionKey = new byte[source.readInt()];
        source.readByteArray(mEncryptionKey);
    }

    /**
     * required by {@link Parcelable}
     */
    public static final Parcelable.Creator<AuthorizationRequest> CREATOR =
            new Creator<AuthorizationRequest>() {

                @Override
                public AuthorizationRequest[] newArray(int size) {
                    return new AuthorizationRequest[size];
                }

                @Override
                public AuthorizationRequest createFromParcel(Parcel source) {
                    return new AuthorizationRequest(source);
                }
            };

    @Override
    protected AuthorizationRequest getThis() {
        return this;
    }

    public String getScopeString() {
        return TextUtils.join(" ", getScopes());
    }

    @Override
    public String getBrowserSwitchUrl(Context context, OtcConfiguration config)
            throws CertificateException, UnsupportedEncodingException, NoSuchPaddingException,
            NoSuchAlgorithmException, IllegalBlockSizeException, JSONException, BadPaddingException,
            InvalidEncryptionDataException, InvalidKeyException {

        String xSource = context.getPackageName();

        OAuth2Recipe recipe = config.getBrowserOauth2Config(getScopes());

        ConfigEndpoint configEndpoint = recipe.getEndpoint(getEnvironment());

        String certificateBase64 = configEndpoint.getCertificate();
        X509Certificate cert =
                EncryptionUtils.getX509CertificateFromBase64String(certificateBase64);

        return configEndpoint.getUrl()
                + "?payload=" + URLEncoder.encode(buildPayload(context, cert), "utf-8")
                + "&payloadEnc=" + URLEncoder.encode(buildPayloadEnc(cert), "utf-8")
                + "&x-source=" + xSource
                + "&x-success=" + getSuccessUrl()
                + "&x-cancel=" + getCancelUrl();
    }

    @Override
    public Recipe getBrowserSwitchRecipe(OtcConfiguration config) {
        return config.getBrowserOauth2Config(getScopes());
    }

    @Override
    public void persistRequiredFields(ContextInspector contextInspector) {
        Map<String, String> prefs = new HashMap<>();
        prefs.put(PREFS_MSG_GUID, mMsgGuid);
        prefs.put(PREFS_ENCRYPTION_KEY, EncryptionUtils.byteArrayToHexString(mEncryptionKey));
        contextInspector.setPreferences(prefs);
    }

    private boolean validResponse(ContextInspector contextInspector, String msgGUID) {
        String prefsMsgGUID =
                contextInspector.getStringPreference(AuthorizationRequest.PREFS_MSG_GUID);
        String prefsSymmetricKey = getStoredSymmetricKey(contextInspector);

        if (TextUtils.isEmpty(prefsMsgGUID)) {
            Log.e(TAG, "stored msg_GUID is empty");
        } else if (!msgGUID.equals(prefsMsgGUID)) {
            Log.e(TAG, "msgGUIDs do not match");
        } else if (TextUtils.isEmpty(prefsSymmetricKey)) {
            Log.e(TAG, "empty symmetric key");
        } else {
            return true;
        }
        return false;
    }

    private class RFC3339DateFormat extends SimpleDateFormat {
        RFC3339DateFormat() {
            super("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US);
        }
    }

    private String buildPayloadEnc(Certificate cert)
            throws NoSuchPaddingException, NoSuchAlgorithmException,
            IllegalBlockSizeException, BadPaddingException,
            InvalidEncryptionDataException,
            InvalidKeyException, JSONException {

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
            // roman says this should be a number
            payload.put("version", 3);
            // Roman says no app_guid in secure browser switch
            //payload.put("app_guid", contextInspector.getInstallationGUID());
            payload.put("client_id", getClientId());
            // Roman confirmed this is correct, but is not ever read from the app.
            payload.put("app_name", DeviceInspector.getApplicationInfoName(context));
            payload.put("environment", getEnvironment());
            payload.put("environment_url", EnvironmentManager.getEnvironmentUrl(getEnvironment()));
            payload.put("scope", getScopeString());
            payload.put("response_type", "code");
            payload.put("privacy_url", getPrivacyUrl());
            payload.put("agreement_url",
                    getUserAgreementUrl());
            payload.put("client_metadata_id", getClientMetadataId());
            payload.put("key_id", cert.getSerialNumber());

            // If this is false, Braintree will not offer KMLI (won't save cookies)
            payload.put("android_chrome_available", isChromeAvailable(context));

            for (Entry<String, String> entry : mAdditionalPayloadAttributes.entrySet()) {
                payload.put(entry.getKey(), entry.getValue());
            }

            return Base64
                    .encodeToString(payload.toString().getBytes(), Base64.NO_WRAP);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isChromeAvailable(Context context) {
        Intent intent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://www.paypal.com"/*dummy url here*/));
        intent.setPackage("com.android.chrome");
        return intent.resolveActivity(context.getPackageManager()) != null;
    }

    private String getStoredSymmetricKey(ContextInspector contextInspector) {
        return contextInspector.getStringPreference(AuthorizationRequest.PREFS_ENCRYPTION_KEY);
    }

    @Override
    public Result parseBrowserResponse(ContextInspector contextInspector, Uri uri) {
        String status = uri.getLastPathSegment();

        if (!Uri.parse(getSuccessUrl()).getLastPathSegment().equals(status)) {
            return new Result();
        }

        String payloadEnc = uri.getQueryParameter("payloadEnc");
        String payload = uri.getQueryParameter("payload");

        // decode
        byte[] base64Payload = Base64.decode(payload, Base64.DEFAULT);

        try {
            JSONObject payloadJson = new JSONObject(new String(base64Payload));
            if (payloadJson.has("msg_GUID")) {
                String msgGUID = payloadJson.optString("msg_GUID");

                if (isSuccessResponse(payloadEnc, msgGUID) &&
                        validResponse(contextInspector, msgGUID)) {
                    // we can decrypt
                    JSONObject decryptedPayloadEnc = getDecryptedPayload(payloadEnc,
                            getStoredSymmetricKey(contextInspector));

                    String error = payloadJson.optString("error");

                    // the string 'null' is coming back in production for some reason
                    if (!TextUtils.isEmpty(error) && !"null".equals(error)) {
                        return new Result(new BrowserSwitchException(error));
                    } else {
                        // Based on this interpretation, construct the PayPalOneTouchResult to
                        // reflect the response we've received.
                        return new Result(payloadJson.optString("environment"),
                                ResponseType.authorization_code,
                                new JSONObject()
                                        .put("code", decryptedPayloadEnc.getString("payment_code")),
                                decryptedPayloadEnc.getString("email")
                        );
                    }
                } else {
                    // invalid guid, no payloadEnc, status != success
                    Log.e(TAG, "response not understood");

                    return new Result(new ResponseParsingException("Response was not understood"));
                }
            } else {
                // missing msg_GUID in response
                return new Result(
                        new ResponseParsingException("Response was missing some information"));
            }
        } catch (JSONException
                | InvalidAlgorithmParameterException
                | NoSuchAlgorithmException
                | IllegalBlockSizeException
                | BadPaddingException
                | NoSuchPaddingException
                | InvalidKeyException
                | InvalidEncryptionDataException
                e) {
            Log.e(TAG, "failed", e);
            return new Result(new ResponseParsingException(e));
        }
    }

    @Override
    public boolean validateV1V2Response(ContextInspector contextInspector, Bundle extras) {
        // roman says we shouldn't need to validate anything here
        return true;
    }

    @Override
    public Recipe getRecipeToExecute(Context context, OtcConfiguration config,
            boolean isSecurityEnabled) {
        for (OAuth2Recipe recipe : config.getOauth2Recipes()) {
            // don't even look at them if they can't handle the scopes.  You CAN'T HANDLE THE SCOPE!
            if (recipe.isValidForScopes(getScopes())) {
                if (RequestTarget.wallet == recipe.getTarget()) {
                    if (recipe.isValidAppTarget(context, isSecurityEnabled)) {
                        return recipe;
                    }
                } else if (RequestTarget.browser == recipe.getTarget()) {
                    try {
                        String browserSwitchUrl = getBrowserSwitchUrl(context, config);
                        if (recipe.isValidBrowserTarget(context, browserSwitchUrl)) {
                            return recipe;
                        }
                    } catch (CertificateException | UnsupportedEncodingException
                            | NoSuchPaddingException | NoSuchAlgorithmException
                            | IllegalBlockSizeException | JSONException | BadPaddingException
                            | InvalidEncryptionDataException | InvalidKeyException e) {
                        Log.e(TAG, "cannot create browser switch URL", e);
                    }
                }
            }
        }
        return null;
    }

    @Override
    public void trackFpti(Context context, TrackingPoint trackingPoint, Protocol protocol) {
        Map<String, String> fptiDataBundle = new HashMap<>();

        fptiDataBundle.put("clid", getClientId());

        PayPalOneTouchCore.getFptiManager(context)
                .trackFpti(trackingPoint, getEnvironment(), fptiDataBundle, protocol);
    }

    private boolean isSuccessResponse(String payloadEnc, String msgGUID) {
        if (TextUtils.isEmpty(msgGUID)) {
            Log.e(TAG, "response msgGUID is empty");
        } else if (TextUtils.isEmpty(payloadEnc)) {
            Log.e(TAG, "empty payloadEnc");
        } else {
            return true;
        }
        return false;
    }

    private JSONObject getDecryptedPayload(String payloadEnc, String symmetricKey)
            throws IllegalBlockSizeException, InvalidKeyException, NoSuchAlgorithmException,
            InvalidAlgorithmParameterException, NoSuchPaddingException, BadPaddingException,
            InvalidEncryptionDataException, JSONException {
        byte[] base64PayloadEnc = Base64.decode(payloadEnc, Base64.DEFAULT);
        // convert key to bytes
        byte[] key = EncryptionUtils.hexStringToByteArray(symmetricKey);
        byte[] output = new OtcCrypto().decryptAESCTRData(base64PayloadEnc, key);

        return new JSONObject(new String(output));
    }
}
