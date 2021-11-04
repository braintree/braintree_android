package com.braintreepayments.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;

import androidx.test.core.app.ApplicationProvider;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.util.ReflectionHelpers;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

@RunWith(RobolectricTestRunner.class)
public class DeviceInspectorUnitTest {

    private Context applicationContext;
    private AppHelper appHelper;
    private ClassHelper classHelper;
    private UUIDHelper uuidHelper;

    @Before
    public void beforeEach() {
        applicationContext = ApplicationProvider.getApplicationContext();
        appHelper = mock(AppHelper.class);
        uuidHelper = mock(UUIDHelper.class);
        classHelper = mock(ClassHelper.class);
    }

    @Test
    public void detectEmulator_defaultsToFalse() {
        DeviceInspector sut = new DeviceInspector(appHelper, classHelper, uuidHelper);
        assertFalse(sut.isDeviceEmulator("randomBuildProduct", "randomBuildManufacturer", "randomBuildFingerprint"));
    }

    @Test
    public void detectEmulator_whenBuildProductIsGoogleSdk_returnsTrue() {
        DeviceInspector sut = new DeviceInspector(appHelper, classHelper, uuidHelper);
        assertTrue(sut.isDeviceEmulator("google_sdk", "randomBuildManufacturer", "randomBuildFingerprint"));
    }

    @Test
    public void detectEmulator_whenBuildProductIsSdk_returnsTrue() {
        DeviceInspector sut = new DeviceInspector(appHelper, classHelper, uuidHelper);
        assertTrue(sut.isDeviceEmulator("sdk", "randomBuildManufacturer", "randomBuildFingerprint"));
    }

    @Test
    public void detectEmulator_whenBuildManufacturerIsGenymotion_returnsTrue() {
        DeviceInspector sut = new DeviceInspector(appHelper, classHelper, uuidHelper);
        assertTrue(sut.isDeviceEmulator("randomBuildProduct", "Genymotion", "randomBuildFingerprint"));
    }

    @Test
    public void detectEmulator_whenBuildFingerprintIsGeneric_returnsTrue() {
        DeviceInspector sut = new DeviceInspector(appHelper, classHelper, uuidHelper);
        assertTrue(sut.isDeviceEmulator("randomBuildProduct", "randomBuildManufacturer", "generic"));
    }

    @Test
    public void getAppName_returnsAppNameFromPackageManager() throws PackageManager.NameNotFoundException {
        Context context = mock(Context.class);
        when(context.getPackageName()).thenReturn("sample-package-name");

        PackageManager packageManager = mock(PackageManager.class);
        when(context.getPackageManager()).thenReturn(packageManager);

        ApplicationInfo applicationInfo = new ApplicationInfo();
        when(packageManager.getApplicationInfo("sample-package-name", 0)).thenReturn(applicationInfo);

        when(packageManager.getApplicationLabel(applicationInfo)).thenReturn("SampleAppName");

        DeviceInspector sut = new DeviceInspector(appHelper, classHelper, uuidHelper);
        assertEquals("SampleAppName", sut.getAppName(context));
    }

    @Test
    public void getAppName_whenApplicationInfoNotFound_returnsAppNameFromPackageManager() throws PackageManager.NameNotFoundException {
        Context context = mock(Context.class);
        when(context.getPackageName()).thenReturn("sample-package-name");

        PackageManager packageManager = mock(PackageManager.class);
        when(context.getPackageManager()).thenReturn(packageManager);

        when(packageManager.getApplicationInfo("sample-package-name", 0)).thenThrow(new PackageManager.NameNotFoundException());

        DeviceInspector sut = new DeviceInspector(appHelper, classHelper, uuidHelper);
        assertEquals("ApplicationNameUnknown", sut.getAppName(context));
    }

    @Test
    public void getDeviceMetadata_returnsAndroidAsPlatform() throws JSONException {
        DeviceInspector sut = new DeviceInspector(appHelper, classHelper, uuidHelper);
        DeviceMetadata metadata = sut.getDeviceMetadata(applicationContext, "session-id", "integration-type");
        JSONObject metadataJSON = metadata.toJSON();
        assertEquals("Android", metadataJSON.getString("platform"));
    }

    @Test
    public void getDeviceMetadata_returnsAndroidAPIAsPlatformVersion() throws JSONException {
        ReflectionHelpers.setStaticField(Build.VERSION.class, "SDK_INT", 123);

        DeviceInspector sut = new DeviceInspector(appHelper, classHelper, uuidHelper);
        DeviceMetadata metadata = sut.getDeviceMetadata(applicationContext, "session-id", "integration-type");
        JSONObject metadataJSON = metadata.toJSON();
        assertEquals("123", metadataJSON.getString("platformVersion"));
    }

    @Test
    public void getDeviceMetadata_returnsSDKVersion() throws JSONException {
        DeviceInspector sut = new DeviceInspector(appHelper, classHelper, uuidHelper);
        DeviceMetadata metadata = sut.getDeviceMetadata(applicationContext, "session-id", "integration-type");
        JSONObject metadataJSON = metadata.toJSON();
        assertEquals(BuildConfig.VERSION_NAME, metadataJSON.getString("sdkVersion"));
    }

    @Test
    public void getDeviceMetadata_returnsMerchantAppId() throws JSONException {
        DeviceInspector sut = new DeviceInspector(appHelper, classHelper, uuidHelper);
        DeviceMetadata metadata = sut.getDeviceMetadata(applicationContext, "session-id", "integration-type");
        JSONObject metadataJSON = metadata.toJSON();
        assertEquals("com.braintreepayments.api.test", metadataJSON.getString("merchantAppId"));
    }

    @Test
    public void getDeviceMetadata_returnsMerchantAppName() throws JSONException {
        DeviceInspector sut = new DeviceInspector(appHelper, classHelper, uuidHelper);
        DeviceMetadata metadata = sut.getDeviceMetadata(applicationContext, "session-id", "integration-type");
        JSONObject metadataJSON = metadata.toJSON();
        assertEquals("Test Application", metadataJSON.getString("merchantAppName"));
    }

    @Test
    public void getDeviceMetadata_whenSwitchUserCommandLineToolNotFound_returnsDeviceRootedAsFalse() throws IOException, JSONException {
        File superUserApkFile = mock(File.class);
        when(superUserApkFile.exists()).thenReturn(false);

        Runtime runtime = mock(Runtime.class);
        Process process = mock(Process.class);

        when(runtime.exec(new String[]{"/system/xbin/which", "su"})).thenReturn(process);
        when(process.getInputStream()).thenReturn(new ByteArrayInputStream("".getBytes()));

        DeviceInspector sut = new DeviceInspector(appHelper, classHelper, uuidHelper);
        DeviceMetadata metadata = sut.getDeviceMetadata(applicationContext, "session-id", "integration-type", "", superUserApkFile, runtime);
        JSONObject metadataJSON = metadata.toJSON();
        assertEquals("false", metadataJSON.getString("deviceRooted"));
    }

    @Test
    public void getDeviceMetadata_whenBuildTagsIncludeTestKeys_returnsDeviceRootedAsTrue() throws JSONException {
        DeviceInspector sut = new DeviceInspector(appHelper, classHelper, uuidHelper);
        DeviceMetadata metadata = sut.getDeviceMetadata(
                applicationContext, "session-id", "integration-type", "test-keys", mock(File.class), mock(Runtime.class));
        JSONObject metadataJSON = metadata.toJSON();
        assertEquals("true", metadataJSON.getString("deviceRooted"));
    }

    @Test
    public void getDeviceMetadata_whenSuperUserApkFileExists_returnsDeviceRootedAsTrue() throws JSONException {
        File superUserApkFile = mock(File.class);
        when(superUserApkFile.exists()).thenReturn(true);

        DeviceInspector sut = new DeviceInspector(appHelper, classHelper, uuidHelper);
        DeviceMetadata metadata = sut.getDeviceMetadata(applicationContext, "session-id", "integration-type", "", superUserApkFile, mock(Runtime.class));

        JSONObject metadataJSON = metadata.toJSON();
        assertEquals("true", metadataJSON.getString("deviceRooted"));
    }

    @Test
    public void getDeviceMetadata_whenSuCommandPresentOnSystemPath_returnsDeviceRootedAsTrue() throws IOException, JSONException {
        Runtime runtime = mock(Runtime.class);
        Process process = mock(Process.class);

        when(runtime.exec(new String[]{"/system/xbin/which", "su"})).thenReturn(process);
        when(process.getInputStream()).thenReturn(new ByteArrayInputStream("/path/to/su/command".getBytes()));

        DeviceInspector sut = new DeviceInspector(appHelper, classHelper, uuidHelper);
        DeviceMetadata metadata = sut.getDeviceMetadata(applicationContext, "session-id", "integration-type", "", mock(File.class), runtime);

        JSONObject metadataJSON = metadata.toJSON();
        assertEquals("true", metadataJSON.getString("deviceRooted"));
    }

    @Test
    public void getDeviceMetadata_whenFileExistsThrows_returnsDeviceRootedAsFalse() throws JSONException {
        File superUserApkFile = mock(File.class);
        when(superUserApkFile.exists()).thenThrow(new SecurityException());

        DeviceInspector sut = new DeviceInspector(appHelper, classHelper, uuidHelper);
        DeviceMetadata metadata = sut.getDeviceMetadata(applicationContext, "session-id", "integration-type", "", superUserApkFile, mock(Runtime.class));
        JSONObject metadataJSON = metadata.toJSON();
        assertEquals("false", metadataJSON.getString("deviceRooted"));
    }

    @Test
    public void getDeviceMetadata_whenRuntimeExecThrows_returnsFalse() throws IOException, JSONException {
        Runtime runtime = mock(Runtime.class);
        when(runtime.exec(new String[]{"/system/xbin/which", "su"})).thenThrow(new IOException());

        DeviceInspector sut = new DeviceInspector(appHelper, classHelper, uuidHelper);
        DeviceMetadata metadata = sut.getDeviceMetadata(applicationContext, "session-id", "integration-type", "", mock(File.class), runtime);
        JSONObject metadataJSON = metadata.toJSON();
        assertEquals("false", metadataJSON.getString("deviceRooted"));
    }

    @Test
    public void getDeviceMetadata_returnsDeviceManufacturer() throws JSONException {
        ReflectionHelpers.setStaticField(Build.class, "MANUFACTURER", "device-manufacturer");

        DeviceInspector sut = new DeviceInspector(appHelper, classHelper, uuidHelper);
        DeviceMetadata metadata = sut.getDeviceMetadata(applicationContext, "session-id", "integration-type");
        JSONObject metadataJSON = metadata.toJSON();
        assertEquals("device-manufacturer", metadataJSON.getString("deviceManufacturer"));
    }

    @Test
    public void getDeviceMetadata_returnsDeviceModel() throws JSONException {
        ReflectionHelpers.setStaticField(Build.class, "MODEL", "device-model");

        DeviceInspector sut = new DeviceInspector(appHelper, classHelper, uuidHelper);
        DeviceMetadata metadata = sut.getDeviceMetadata(applicationContext, "session-id", "integration-type");
        JSONObject metadataJSON = metadata.toJSON();
        assertEquals("device-model", metadataJSON.getString("deviceModel"));
    }

    @Test
    public void getDeviceMetadata_returnsDeviceAppGeneratedPersistentUuid() throws JSONException {
        when(uuidHelper.getPersistentUUID(applicationContext)).thenReturn("persistent-uuid");

        DeviceInspector sut = new DeviceInspector(appHelper, classHelper, uuidHelper);
        DeviceMetadata metadata = sut.getDeviceMetadata(applicationContext, "session-id", "integration-type");
        JSONObject metadataJSON = metadata.toJSON();
        assertEquals("persistent-uuid",
                metadataJSON.getString("deviceAppGeneratedPersistentUuid"));
    }

    @Test
    public void getDeviceMetadata_whenBuildProductManufacturerAndFingerprintAreValid_returnsFalse() throws JSONException {
        ReflectionHelpers.setStaticField(Build.class, "PRODUCT", "build-product");
        ReflectionHelpers.setStaticField(Build.class, "MANUFACTURER", "build-manufacturer");
        ReflectionHelpers.setStaticField(Build.class, "FINGERPRINT", "build-fingerprint");

        DeviceInspector sut = new DeviceInspector(appHelper, classHelper, uuidHelper);
        DeviceMetadata metadata = sut.getDeviceMetadata(applicationContext, "session-id", "integration-type");
        JSONObject metadataJSON = metadata.toJSON();
        assertEquals("false", metadataJSON.getString("isSimulator"));
    }

    @Test
    public void getDeviceMetadata_whenBuildProductIsGoogleSdk_returnsTrue() throws JSONException {
        ReflectionHelpers.setStaticField(Build.class, "PRODUCT", "google_sdk");

        DeviceInspector sut = new DeviceInspector(appHelper, classHelper, uuidHelper);
        DeviceMetadata metadata = sut.getDeviceMetadata(applicationContext, "session-id", "integration-type");
        JSONObject metadataJSON = metadata.toJSON();
        assertEquals("true", metadataJSON.getString("isSimulator"));
    }

    @Test
    public void getDeviceMetadata_whenBuildProductIsSdk_returnsTrue() throws JSONException {
        ReflectionHelpers.setStaticField(Build.class, "PRODUCT", "sdk");

        DeviceInspector sut = new DeviceInspector(appHelper, classHelper, uuidHelper);
        DeviceMetadata metadata = sut.getDeviceMetadata(applicationContext, "session-id", "integration-type");
        JSONObject metadataJSON = metadata.toJSON();
        assertEquals("true", metadataJSON.getString("isSimulator"));
    }

    @Test
    public void getDeviceMetadata_whenBuildManufacturerIsGenymotion_returnsTrue() throws JSONException {
        ReflectionHelpers.setStaticField(Build.class, "MANUFACTURER", "genymotion");

        DeviceInspector sut = new DeviceInspector(appHelper, classHelper, uuidHelper);
        DeviceMetadata metadata = sut.getDeviceMetadata(applicationContext, "session-id", "integration-type");
        JSONObject metadataJSON = metadata.toJSON();
        assertEquals("true", metadataJSON.getString("isSimulator"));
    }

    @Test
    public void getDeviceMetadata_whenBuildFingerpintContainsGeneric_returnsTrue() throws JSONException {
        ReflectionHelpers.setStaticField(Build.class, "FINGERPRINT", "generic-fingerprint");

        DeviceInspector sut = new DeviceInspector(appHelper, classHelper, uuidHelper);
        DeviceMetadata metadata = sut.getDeviceMetadata(applicationContext, "session-id", "integration-type");
        JSONObject metadataJSON = metadata.toJSON();
        assertEquals("true", metadataJSON.getString("isSimulator"));
    }

    @Test
    public void getDeviceMetadata_whenDeviceOrientationIsPortrait_returnsPortrait() throws JSONException {
        Context context = mock(Context.class);
        Resources resources = mock(Resources.class);
        when(context.getResources()).thenReturn(resources);

        Configuration configuration = new Configuration();
        configuration.orientation = Configuration.ORIENTATION_PORTRAIT;
        when(resources.getConfiguration()).thenReturn(configuration);

        DeviceInspector sut = new DeviceInspector(appHelper, classHelper, uuidHelper);
        DeviceMetadata metadata = sut.getDeviceMetadata(context, "session-id", "integration-type");
        JSONObject metadataJSON = metadata.toJSON();
        assertEquals("Portrait", metadataJSON.getString("userInterfaceOrientation"));
    }

    @Test
    public void getDeviceMetadata_whenDeviceOrientationIsLandscape_returnsLandscape() throws JSONException {
        Context context = mock(Context.class);
        Resources resources = mock(Resources.class);
        when(context.getResources()).thenReturn(resources);

        Configuration configuration = new Configuration();
        configuration.orientation = Configuration.ORIENTATION_LANDSCAPE;
        when(resources.getConfiguration()).thenReturn(configuration);

        DeviceInspector sut = new DeviceInspector(appHelper, classHelper, uuidHelper);
        DeviceMetadata metadata = sut.getDeviceMetadata(context, "session-id", "integration-type");
        JSONObject metadataJSON = metadata.toJSON();
        assertEquals("Landscape", metadataJSON.getString("userInterfaceOrientation"));
    }

    @Test
    public void getDeviceMetadata_whenDeviceOrientationIsUndefined_returnsUnknown() throws JSONException {
        Context context = mock(Context.class);
        Resources resources = mock(Resources.class);
        when(context.getResources()).thenReturn(resources);

        Configuration configuration = new Configuration();
        configuration.orientation = Configuration.ORIENTATION_UNDEFINED;
        when(resources.getConfiguration()).thenReturn(configuration);

        DeviceInspector sut = new DeviceInspector(appHelper, classHelper, uuidHelper);
        DeviceMetadata metadata = sut.getDeviceMetadata(context, "session-id", "integration-type");
        JSONObject metadataJSON = metadata.toJSON();
        assertEquals("Unknown", metadataJSON.getString("userInterfaceOrientation"));
    }

    @Test
    public void getDeviceMetadata_forwardsIntegrationType() throws JSONException {
        DeviceInspector sut = new DeviceInspector(appHelper, classHelper, uuidHelper);
        DeviceMetadata metadata = sut.getDeviceMetadata(applicationContext, "session-id", "integration-type");
        JSONObject metadataJSON = metadata.toJSON();
        assertEquals("integration-type", metadataJSON.getString("integrationType"));
    }

    @Test
    public void getDeviceMetadata_forwardsSessionId() throws JSONException {
        DeviceInspector sut = new DeviceInspector(appHelper, classHelper, uuidHelper);
        DeviceMetadata metadata = sut.getDeviceMetadata(applicationContext, "session-id", "integration-type");
        JSONObject metadataJSON = metadata.toJSON();
        assertEquals("session-id", metadataJSON.getString("sessionId"));
    }

    @Test
    public void getDeviceMetadata_forwardsIsPayPalInstalledResultFromAppHelper() throws JSONException {
        Context context = mock(Context.class);
        when(appHelper.isAppInstalled(context, "com.paypal.android.p2pmobile")).thenReturn(true);

        DeviceInspector sut = new DeviceInspector(appHelper, classHelper, uuidHelper);
        DeviceMetadata metadata = sut.getDeviceMetadata(context, "session-id", "integration-type");
        JSONObject metadataJSON = metadata.toJSON();
        assertTrue(metadataJSON.getBoolean("paypalInstalled"));
    }

    @Test
    public void getDeviceMetadata_forwardsIsVenmoInstalledResultFromAppHelper() throws JSONException {
        Context context = mock(Context.class);
        when(appHelper.isAppInstalled(context, "com.venmo")).thenReturn(true);

        DeviceInspector sut = new DeviceInspector(appHelper, classHelper, uuidHelper);
        DeviceMetadata metadata = sut.getDeviceMetadata(context, "session-id", "integration-type");
        JSONObject metadataJSON = metadata.toJSON();
        assertTrue(metadataJSON.getBoolean("venmoInstalled"));
    }

    @Test
    public void getDeviceMetadata_inspectsDeviceForMetadata() throws JSONException {
        DeviceInspector sut = new DeviceInspector(appHelper, classHelper, uuidHelper);
        DeviceMetadata metadata = sut.getDeviceMetadata(applicationContext, "session-id", "integration-type");

        JSONObject metadataJSON = metadata.toJSON();
//        assertEquals("platform", metadataJSON.getString("platform"));
//        assertEquals("platform-version", metadataJSON.getString("platformVersion"));
//        assertEquals("sdk-version", metadataJSON.getString("sdkVersion"));
//        assertEquals("merchant-app-id", metadataJSON.getString("merchantAppId"));
//        assertEquals("merchant-app-name", metadataJSON.getString("merchantAppName"));
//        assertEquals("false", metadataJSON.getString("deviceRooted"));
//        assertEquals("device-manufacturer", metadataJSON.getString("deviceManufacturer"));
//        assertEquals("device-model", metadataJSON.getString("deviceModel"));
//        assertEquals("persistent-uuid",
//                metadataJSON.getString("deviceAppGeneratedPersistentUuid"));
//        assertEquals("false", metadataJSON.getString("isSimulator"));
//        assertEquals("user-orientation", metadataJSON.getString("userInterfaceOrientation"));
//        assertEquals("sample-integration", metadataJSON.getString("integrationType"));
//        assertEquals("sample-session-id", metadataJSON.getString("sessionId"));
//        TestCase.assertTrue(metadataJSON.getBoolean("paypalInstalled"));
    }
}
