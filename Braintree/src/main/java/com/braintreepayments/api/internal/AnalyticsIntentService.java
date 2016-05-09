package com.braintreepayments.api.internal;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Build.VERSION;
import android.provider.Settings.Secure;

import com.braintreepayments.api.BuildConfig;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.models.Authorization;
import com.braintreepayments.api.models.ClientToken;
import com.braintreepayments.api.models.Configuration;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.List;

public class AnalyticsIntentService extends IntentService {

    public static final String EXTRA_AUTHORIZATION =
            "com.braintreepayments.api.internal.AnalyticsIntentService.EXTRA_AUTHORIZATION";
    public static final String EXTRA_CONFIGURATION =
            "com.braintreepayments.api.internal.AnalyticsIntentService.EXTRA_CONFIGURATION";

    public static final String SESSION_ID_KEY = "sessionId";
    public static final String DEVICE_NETWORK_TYPE_KEY = "deviceNetworkType";
    public static final String USER_INTERFACE_ORIENTATION_KEY = "userInterfaceOrientation";
    public static final String MERCHANT_APP_VERSION_KEY = "merchantAppVersion";
    public static final String PAYPAL_INSTALLED_KEY = "paypalInstalled";
    public static final String VENMO_INSTALLED_KEY = "venmoInstalled";

    private static final String ANALYTICS_KEY = "analytics";
    private static final String KIND_KEY = "kind";
    private static final String TIMESTAMP_KEY = "timestamp";
    private static final String META_KEY = "_meta";
    private static final String TOKENIZATION_KEY = "tokenization_key";
    private static final String AUTHORIZATION_FINGERPRINT_KEY = "authorization_fingerprint";
    private static final String PLATFORM_KEY = "platform";
    private static final String PLATFORM_VERSION_KEY = "platformVersion";
    private static final String SDK_VERSION_KEY = "sdkVersion";
    private static final String MERCHANT_APP_ID_KEY = "merchantAppId";
    private static final String MERCHANT_APP_NAME_KEY = "merchantAppName";
    private static final String DEVICE_ROOTED_KEY = "deviceRooted";
    private static final String DEVICE_MANUFACTURER_KEY = "deviceManufacturer";
    private static final String DEVICE_MODEL_KEY = "deviceModel";
    private static final String ANDROID_ID_KEY = "androidId";
    private static final String DEVICE_APP_GENERATED_PERSISTENT_UUID_KEY = "deviceAppGeneratedPersistentUuid";
    private static final String IS_SIMULATOR_KEY = "isSimulator";
    private static final String INTEGRATION_TYPE_KEY = "integrationType";

    protected HttpClient mHttpClient;
    protected Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();
        if (mContext == null) {
            mContext = getApplicationContext();
        }
    }

    public AnalyticsIntentService() {
        super(AnalyticsIntentService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(final Intent intent) {
        try {
            final AnalyticsDatabase db = AnalyticsDatabase.getInstance(mContext);
            Authorization authorization = Authorization.fromString(intent.getStringExtra(EXTRA_AUTHORIZATION));
            Configuration configuration = Configuration.fromJson(intent.getStringExtra(EXTRA_CONFIGURATION));

            if (mHttpClient == null) {
                mHttpClient = new BraintreeHttpClient(authorization);
            }

            List<List<AnalyticsEvent>> events = db.getPendingRequests();

            JSONObject analyticsRequest;
            for (final List<AnalyticsEvent> innerEvents : events) {
                analyticsRequest = serializeEvents(innerEvents, authorization);
                try {
                    mHttpClient.post(configuration.getAnalytics().getUrl(), analyticsRequest.toString());
                    db.removeEvents(innerEvents);
                } catch (Exception ignored) {}
            }
        } catch (InvalidArgumentException | JSONException ignored) {}
    }

    protected JSONObject serializeEvents(List<AnalyticsEvent> events, Authorization authorization) throws JSONException {
        AnalyticsEvent primeEvent = events.get(0);

        JSONObject requestObject = new JSONObject();
        if (authorization instanceof ClientToken) {
            requestObject.put(AUTHORIZATION_FINGERPRINT_KEY, ((ClientToken) authorization).getAuthorizationFingerprint());
        } else {
            requestObject.put(TOKENIZATION_KEY, authorization.toString());
        }

        JSONObject meta = primeEvent.metadata
                .put(PLATFORM_KEY, "Android")
                .put(INTEGRATION_TYPE_KEY, primeEvent.getIntegrationType())
                .put(PLATFORM_VERSION_KEY, Integer.toString(VERSION.SDK_INT))
                .put(SDK_VERSION_KEY, BuildConfig.VERSION_NAME)
                .put(MERCHANT_APP_ID_KEY, mContext.getPackageName())
                .put(MERCHANT_APP_NAME_KEY, getAppName())
                .put(DEVICE_ROOTED_KEY, isDeviceRooted())
                .put(DEVICE_MANUFACTURER_KEY, Build.MANUFACTURER)
                .put(DEVICE_MODEL_KEY, Build.MODEL)
                .put(ANDROID_ID_KEY, getAndroidId())
                .put(DEVICE_APP_GENERATED_PERSISTENT_UUID_KEY,
                        UUIDHelper.getPersistentUUID(mContext))
                .put(IS_SIMULATOR_KEY, detectEmulator());
        requestObject.put(META_KEY, meta);

        JSONArray eventObjects = new JSONArray();
        JSONObject eventObject;
        for (AnalyticsEvent analyticsEvent : events) {
            eventObject = new JSONObject()
                    .put(KIND_KEY, analyticsEvent.event)
                    .put(TIMESTAMP_KEY, analyticsEvent.timestamp);

            eventObjects.put(eventObject);
        }
        requestObject.put(ANALYTICS_KEY, eventObjects);

        return requestObject;
    }

    private String detectEmulator() {
        if ("google_sdk".equalsIgnoreCase(Build.PRODUCT) ||
                "sdk".equalsIgnoreCase(Build.PRODUCT) ||
                "Genymotion".equalsIgnoreCase(Build.MANUFACTURER) ||
                Build.FINGERPRINT.contains("generic")) {
            return "true";
        } else {
            return "false";
        }
    }

    private String getAppName() {
        ApplicationInfo applicationInfo;
        String packageName = mContext.getPackageName();
        PackageManager packageManager = mContext.getPackageManager();
        try {
            applicationInfo = packageManager.getApplicationInfo(packageName, 0);
        } catch (NameNotFoundException e) {
            applicationInfo = null;
        }

        String appName = null;
        if (applicationInfo != null) {
            appName = (String) packageManager.getApplicationLabel(applicationInfo);
        }

        if (appName == null) {
            return "ApplicationNameUnknown";
        }
        return appName;
    }

    private String isDeviceRooted() {
        String buildTags = android.os.Build.TAGS;
        boolean check1 = buildTags != null && buildTags.contains("test-keys");

        boolean check2;
        try {
            check2 = new File("/system/app/Superuser.apk").exists();
        } catch (Exception e) {
            check2 = false;
        }

        boolean check3;
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"/system/xbin/which", "su"});
            BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
            check3 = in.readLine() != null;
        } catch (Exception e) {
            check3 = false;
        }

        return Boolean.toString(check1 || check2 || check3);
    }

    private String getAndroidId() {
        String id = Secure.getString(mContext.getContentResolver(), Secure.ANDROID_ID);
        if (id == null) {
            return "AndroidIdUnknown";
        }
        return id;
    }
}
