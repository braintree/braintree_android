package com.paypal.android.sdk.onetouch.core.config;

import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.paypal.android.sdk.onetouch.core.BuildConfig;
import com.paypal.android.sdk.onetouch.core.base.ContextInspector;
import com.paypal.android.sdk.onetouch.core.network.PayPalHttpClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {

    private static final String CONFIGURATION_URL =
            "https://www.paypalobjects.com/webstatic/otc/otc-config.android.json";
    private static final String PREFERENCES_CONFIG_FILE = "com.paypal.otc.config.file";
    private static final String PREFERENCES_LAST_UPDATED = "com.paypal.otc.config.lastUpdated.timestamp";
    private static final String PREFERENCES_CONFIG_IS_DEFAULT = "com.paypal.otc.config.isDefault";
    private static final int MINIMUM_TIME_BETWEEN_REFRESH = -4;
    private static final int MINIMUM_TIME_BETWEEN_CONSECUTIVE_REQUESTS = -5;

    private boolean mUseHardcodedConfig = false;

    private final ContextInspector mContextInspector;
    private final PayPalHttpClient mHttpClient;
    private Date mLastInitiatedUpdate;

    public ConfigManager(ContextInspector contextInspector, PayPalHttpClient httpClient) {
        mContextInspector = contextInspector;
        mHttpClient = httpClient;
    }

    public void useHardcodedConfig(boolean useHardcodedConfig) {
        mUseHardcodedConfig = useHardcodedConfig;
        refreshConfiguration();
    }

    public void refreshConfiguration() {
        if (!mUseHardcodedConfig && requiresUpdate()) {
            mLastInitiatedUpdate = new Date();
            mHttpClient.get(CONFIGURATION_URL, new HttpResponseCallback() {
                @Override
                public void success(String responseBody) {
                    try {
                        JSONObject json = new JSONObject(responseBody);
                        setConfig(json.toString(), false);
                    } catch (JSONException ignored) {}
                }

                @Override
                public void failure(Exception exception) {}
            });
        }
    }

    private boolean requiresUpdate() {
        Calendar lastUpdatedCalendar = Calendar.getInstance();
        lastUpdatedCalendar.add(Calendar.HOUR_OF_DAY, MINIMUM_TIME_BETWEEN_REFRESH);
        long lastUpdated = mContextInspector.getLongPreference(PREFERENCES_LAST_UPDATED, 0);
        boolean isOutdated = new Date(lastUpdated).before(lastUpdatedCalendar.getTime());

        Calendar recentlyUpdatedCalendar = Calendar.getInstance();
        recentlyUpdatedCalendar.add(Calendar.SECOND, MINIMUM_TIME_BETWEEN_CONSECUTIVE_REQUESTS);
        boolean recentlyUpdated = !(mLastInitiatedUpdate == null ||
                mLastInitiatedUpdate.before(recentlyUpdatedCalendar.getTime()));

        boolean isDefaultConfig = mContextInspector.getBooleanPreference(PREFERENCES_CONFIG_IS_DEFAULT, true);
        return ((isOutdated || isDefaultConfig) && !recentlyUpdated);
    }

    public OtcConfiguration getConfig() {
        refreshConfiguration();
        boolean useDefault = false;

        String jsonConfig = mContextInspector.getStringPreference(PREFERENCES_CONFIG_FILE);
        if (null == jsonConfig || mUseHardcodedConfig) {
            jsonConfig = BuildConfig.CONFIGURATION;
            useDefault = true;
        }

        OtcConfiguration config;
        try {
            config = getOtcConfiguration(jsonConfig);
        } catch (JSONException e) {
            try {
                jsonConfig = BuildConfig.CONFIGURATION;
                useDefault = true;
                config = getOtcConfiguration(jsonConfig);

                refreshConfiguration();
            } catch (JSONException e1) {
                throw new RuntimeException("could not parse default file");
            }
        }

        if (useDefault) {
            setConfig(jsonConfig, true);
            // may need to update again if there was an error with stored prefs
            refreshConfiguration();
        }

        return config;
    }

    private OtcConfiguration getOtcConfiguration(String jsonConfig) throws JSONException {
        return new ConfigFileParser().getParsedConfig(new JSONObject(jsonConfig));
    }

    private void setConfig(String serverReply, boolean isDefault) {
        Map<String, Object> updatedPrefs = new HashMap<>();
        updatedPrefs.put(PREFERENCES_CONFIG_FILE, serverReply);
        updatedPrefs.put(PREFERENCES_LAST_UPDATED, new Date().getTime());
        updatedPrefs.put(PREFERENCES_CONFIG_IS_DEFAULT, isDefault);
        mContextInspector.setPreferences(updatedPrefs);
    }
}
