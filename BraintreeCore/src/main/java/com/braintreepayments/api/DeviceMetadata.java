package com.braintreepayments.api;

import org.json.JSONException;
import org.json.JSONObject;

class DeviceMetadata {

    private static final String SESSION_ID_KEY = "sessionId";
    private static final String DEVICE_NETWORK_TYPE_KEY = "deviceNetworkType";
    private static final String USER_INTERFACE_ORIENTATION_KEY = "userInterfaceOrientation";
    private static final String MERCHANT_APP_VERSION_KEY = "merchantAppVersion";
    private static final String PAYPAL_INSTALLED_KEY = "paypalInstalled";
    private static final String VENMO_INSTALLED_KEY = "venmoInstalled";
    private static final String INTEGRATION_TYPE_KEY = "integrationType";
    private static final String DROP_IN_VERSION_KEY = "dropinVersion";

    private static final String PLATFORM_KEY = "platform";
    private static final String PLATFORM_VERSION_KEY = "platformVersion";
    private static final String SDK_VERSION_KEY = "sdkVersion";
    private static final String MERCHANT_APP_ID_KEY = "merchantAppId";
    private static final String MERCHANT_APP_NAME_KEY = "merchantAppName";
    private static final String DEVICE_ROOTED_KEY = "deviceRooted";
    private static final String DEVICE_MANUFACTURER_KEY = "deviceManufacturer";
    private static final String DEVICE_MODEL_KEY = "deviceModel";
    private static final String DEVICE_APP_GENERATED_PERSISTENT_UUID_KEY = "deviceAppGeneratedPersistentUuid";
    private static final String IS_SIMULATOR_KEY = "isSimulator";

    static class Builder {

        private final DeviceMetadata instance;

        Builder() {
            instance = new DeviceMetadata();
        }

        Builder appVersion(String appVersion) {
            instance.appVersion = appVersion;
            return this;
        }

        Builder deviceManufacturer(String deviceManufacturer) {
            instance.deviceManufacturer = deviceManufacturer;
            return this;
        }

        Builder deviceModel(String deviceModel) {
            instance.deviceModel = deviceModel;
            return this;
        }

        Builder devicePersistentUUID(String devicePersistentUUID) {
            instance.devicePersistentUUID = devicePersistentUUID;
            return this;
        }

        Builder dropInVersion(String dropInVersion) {
            instance.dropInVersion = dropInVersion;
            return this;
        }

        Builder integration(String integration) {
            instance.integration = integration;
            return this;
        }

        Builder isDeviceRooted(boolean isDeviceRooted) {
            instance.isDeviceRooted = isDeviceRooted;
            return this;
        }

        Builder isPayPalInstalled(boolean isPayPalInstalled) {
            instance.isPayPalInstalled = isPayPalInstalled;
            return this;
        }

        Builder isVenmoInstalled(boolean isVenmoInstalled) {
            instance.isVenmoInstalled = isVenmoInstalled;
            return this;
        }

        Builder isSimulator(boolean isSimulator) {
            instance.isSimulator = isSimulator;
            return this;
        }

        Builder merchantAppId(String merchantAppId) {
            instance.merchantAppId = merchantAppId;
            return this;
        }

        Builder merchantAppName(String merchantAppName) {
            instance.merchantAppName = merchantAppName;
            return this;
        }

        Builder networkType(String networkType) {
            instance.networkType = networkType;
            return this;
        }

        Builder platform(String platform) {
            instance.platform = platform;
            return this;
        }

        Builder platformVersion(String platformVersion) {
            instance.platformVersion = platformVersion;
            return this;
        }

        Builder sdkVersion(String sdkVersion) {
            instance.sdkVersion = sdkVersion;
            return this;
        }

        Builder sessionId(String sessionId) {
            instance.sessionId = sessionId;
            return this;
        }

        Builder userOrientation(String userOrientation) {
            instance.userOrientation = userOrientation;
            return this;
        }

        DeviceMetadata build() {
            return instance;
        }
    }

    private String appVersion;
    private String deviceManufacturer;
    private String deviceModel;
    private String devicePersistentUUID;
    private String dropInVersion;
    private String integration;
    private boolean isDeviceRooted;
    private boolean isPayPalInstalled;
    private boolean isSimulator;
    private boolean isVenmoInstalled;
    private String merchantAppId;
    private String merchantAppName;
    private String networkType;
    private String platform;
    private String platformVersion;
    private String sdkVersion;
    private String sessionId;
    private String userOrientation;

    private DeviceMetadata() {
    }

    JSONObject toJSON() throws JSONException {
        return new JSONObject()
                .put(SESSION_ID_KEY, sessionId)
                .put(INTEGRATION_TYPE_KEY, integration)
                .put(DEVICE_NETWORK_TYPE_KEY, networkType)
                .put(USER_INTERFACE_ORIENTATION_KEY, userOrientation)
                .put(MERCHANT_APP_VERSION_KEY, appVersion)
                .put(PAYPAL_INSTALLED_KEY, isPayPalInstalled)
                .put(VENMO_INSTALLED_KEY, isVenmoInstalled)
                .put(DROP_IN_VERSION_KEY, dropInVersion)
                .put(PLATFORM_KEY, platform)
                .put(PLATFORM_VERSION_KEY, platformVersion)
                .put(SDK_VERSION_KEY, sdkVersion)
                .put(MERCHANT_APP_ID_KEY, merchantAppId)
                .put(MERCHANT_APP_NAME_KEY, merchantAppName)
                .put(DEVICE_ROOTED_KEY, isDeviceRooted)
                .put(DEVICE_MANUFACTURER_KEY, deviceManufacturer)
                .put(DEVICE_MODEL_KEY, deviceModel)
                .put(DEVICE_APP_GENERATED_PERSISTENT_UUID_KEY, devicePersistentUUID)
                .put(IS_SIMULATOR_KEY, isSimulator);
    }
}
