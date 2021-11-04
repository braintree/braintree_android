package com.braintreepayments.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.os.Build;

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

    private Context context;
    private Resources resources;
    private Configuration configuration;
    private ConnectivityManager connectivityManager;

    private PackageInfo packageInfo;
    private PackageManager packageManager;

    private AppHelper appHelper;
    private ClassHelper classHelper;
    private UUIDHelper uuidHelper;

    @Before
    public void beforeEach() throws PackageManager.NameNotFoundException {
        context = mock(Context.class);
        resources = mock(Resources.class);
        connectivityManager = mock(ConnectivityManager.class);
        configuration = new Configuration();

        packageInfo = new PackageInfo();
        packageManager = mock(PackageManager.class);

        appHelper = mock(AppHelper.class);
        uuidHelper = mock(UUIDHelper.class);
        classHelper = mock(ClassHelper.class);

        when(context.getPackageManager()).thenReturn(packageManager);
        when(context.getPackageName()).thenReturn("com.sample.app");
        when(packageManager.getPackageInfo("com.sample.app", 0)).thenReturn(packageInfo);

        when(context.getResources()).thenReturn(resources);
        when(resources.getConfiguration()).thenReturn(configuration);

        when(context.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(connectivityManager);
    }

    @Test
    public void getDeviceMetadata_returnsPlatform() throws JSONException {
        DeviceInspector sut = new DeviceInspector(appHelper, classHelper, uuidHelper);
        DeviceMetadata metadata = sut.getDeviceMetadata(context, "session-id", "integration-type");
        JSONObject metadataJSON = metadata.toJSON();
        assertEquals("Android", metadataJSON.getString("platform"));
    }

    @Test
    public void getDeviceMetadata_returnsPlatformVersion() throws JSONException {
        ReflectionHelpers.setStaticField(Build.VERSION.class, "SDK_INT", 123);

        DeviceInspector sut = new DeviceInspector(appHelper, classHelper, uuidHelper);
        DeviceMetadata metadata = sut.getDeviceMetadata(context, "session-id", "integration-type");
        JSONObject metadataJSON = metadata.toJSON();
        assertEquals("123", metadataJSON.getString("platformVersion"));
    }

    @Test
    public void getDeviceMetadata_returnsSDKVersion() throws JSONException {
        ReflectionHelpers.setStaticField(BuildConfig.class, "VERSION_NAME", "456");

        DeviceInspector sut = new DeviceInspector(appHelper, classHelper, uuidHelper);
        DeviceMetadata metadata = sut.getDeviceMetadata(context, "session-id", "integration-type");
        JSONObject metadataJSON = metadata.toJSON();
        assertEquals("456", metadataJSON.getString("sdkVersion"));
    }

    @Test
    public void getDeviceMetadata_returnsMerchantAppId() throws JSONException {
        DeviceInspector sut = new DeviceInspector(appHelper, classHelper, uuidHelper);
        DeviceMetadata metadata = sut.getDeviceMetadata(context, "session-id", "integration-type");
        JSONObject metadataJSON = metadata.toJSON();
        assertEquals("com.sample.app", metadataJSON.getString("merchantAppId"));
    }

    @Test
    public void getDeviceMetadata_returnsApplicationNameUnknownByDefault() throws JSONException {
        DeviceInspector sut = new DeviceInspector(appHelper, classHelper, uuidHelper);
        DeviceMetadata metadata = sut.getDeviceMetadata(context, "session-id", "integration-type");
        JSONObject metadataJSON = metadata.toJSON();
        assertEquals("ApplicationNameUnknown", metadataJSON.getString("merchantAppName"));
    }

    @Test
    public void getDeviceMetadata_returnsAppNameFromPackageManager() throws PackageManager.NameNotFoundException, JSONException {
        ApplicationInfo applicationInfo = new ApplicationInfo();
        when(packageManager.getApplicationInfo("com.sample.app", 0)).thenReturn(applicationInfo);
        when(packageManager.getApplicationLabel(applicationInfo)).thenReturn("SampleAppName");

        DeviceInspector sut = new DeviceInspector(appHelper, classHelper, uuidHelper);
        DeviceMetadata metadata = sut.getDeviceMetadata(context, "session-id", "integration-type");
        JSONObject metadataJSON = metadata.toJSON();
        assertEquals("SampleAppName", metadataJSON.getString("merchantAppName"));
    }

    @Test
    public void getDeviceMetadata_whenApplicationInfoNotFound_returnsAppNameFromPackageManager() throws PackageManager.NameNotFoundException, JSONException {
        when(packageManager.getApplicationInfo("sample-package-name", 0)).thenThrow(new PackageManager.NameNotFoundException());

        DeviceInspector sut = new DeviceInspector(appHelper, classHelper, uuidHelper);
        DeviceMetadata metadata = sut.getDeviceMetadata(context, "session-id", "integration-type");
        JSONObject metadataJSON = metadata.toJSON();
        assertEquals("ApplicationNameUnknown", metadataJSON.getString("merchantAppName"));
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
        DeviceMetadata metadata = sut.getDeviceMetadata(context, "session-id", "integration-type", "", superUserApkFile, runtime);
        JSONObject metadataJSON = metadata.toJSON();
        assertFalse(metadataJSON.getBoolean("deviceRooted"));
    }

    @Test
    public void getDeviceMetadata_whenBuildTagsIncludeTestKeys_returnsDeviceRootedAsTrue() throws JSONException {
        DeviceInspector sut = new DeviceInspector(appHelper, classHelper, uuidHelper);
        DeviceMetadata metadata = sut.getDeviceMetadata(
                context, "session-id", "integration-type", "test-keys", mock(File.class), mock(Runtime.class));
        JSONObject metadataJSON = metadata.toJSON();
        assertTrue(metadataJSON.getBoolean("deviceRooted"));
    }

    @Test
    public void getDeviceMetadata_whenSuperUserApkFileExists_returnsDeviceRootedAsTrue() throws JSONException {
        File superUserApkFile = mock(File.class);
        when(superUserApkFile.exists()).thenReturn(true);

        DeviceInspector sut = new DeviceInspector(appHelper, classHelper, uuidHelper);
        DeviceMetadata metadata = sut.getDeviceMetadata(context, "session-id", "integration-type", "", superUserApkFile, mock(Runtime.class));

        JSONObject metadataJSON = metadata.toJSON();
        assertTrue(metadataJSON.getBoolean("deviceRooted"));
    }

    @Test
    public void getDeviceMetadata_whenSuCommandPresentOnSystemPath_returnsDeviceRootedAsTrue() throws IOException, JSONException {
        Runtime runtime = mock(Runtime.class);
        Process process = mock(Process.class);

        when(runtime.exec(new String[]{"/system/xbin/which", "su"})).thenReturn(process);
        when(process.getInputStream()).thenReturn(new ByteArrayInputStream("/path/to/su/command".getBytes()));

        DeviceInspector sut = new DeviceInspector(appHelper, classHelper, uuidHelper);
        DeviceMetadata metadata = sut.getDeviceMetadata(context, "session-id", "integration-type", "", mock(File.class), runtime);

        JSONObject metadataJSON = metadata.toJSON();
        assertTrue(metadataJSON.getBoolean("deviceRooted"));
    }

    @Test
    public void getDeviceMetadata_whenFileExistsThrows_returnsDeviceRootedAsFalse() throws JSONException {
        File superUserApkFile = mock(File.class);
        when(superUserApkFile.exists()).thenThrow(new SecurityException());

        DeviceInspector sut = new DeviceInspector(appHelper, classHelper, uuidHelper);
        DeviceMetadata metadata = sut.getDeviceMetadata(context, "session-id", "integration-type", "", superUserApkFile, mock(Runtime.class));
        JSONObject metadataJSON = metadata.toJSON();
        assertFalse(metadataJSON.getBoolean("deviceRooted"));
    }

    @Test
    public void getDeviceMetadata_whenRuntimeExecThrows_returnsFalse() throws IOException, JSONException {
        Runtime runtime = mock(Runtime.class);
        when(runtime.exec(new String[]{"/system/xbin/which", "su"})).thenThrow(new IOException());

        DeviceInspector sut = new DeviceInspector(appHelper, classHelper, uuidHelper);
        DeviceMetadata metadata = sut.getDeviceMetadata(context, "session-id", "integration-type", "", mock(File.class), runtime);
        JSONObject metadataJSON = metadata.toJSON();
        assertFalse(metadataJSON.getBoolean("deviceRooted"));
    }

    @Test
    public void getDeviceMetadata_returnsDeviceManufacturer() throws JSONException {
        ReflectionHelpers.setStaticField(Build.class, "MANUFACTURER", "device-manufacturer");

        DeviceInspector sut = new DeviceInspector(appHelper, classHelper, uuidHelper);
        DeviceMetadata metadata = sut.getDeviceMetadata(context, "session-id", "integration-type");
        JSONObject metadataJSON = metadata.toJSON();
        assertEquals("device-manufacturer", metadataJSON.getString("deviceManufacturer"));
    }

    @Test
    public void getDeviceMetadata_returnsDeviceModel() throws JSONException {
        ReflectionHelpers.setStaticField(Build.class, "MODEL", "device-model");

        DeviceInspector sut = new DeviceInspector(appHelper, classHelper, uuidHelper);
        DeviceMetadata metadata = sut.getDeviceMetadata(context, "session-id", "integration-type");
        JSONObject metadataJSON = metadata.toJSON();
        assertEquals("device-model", metadataJSON.getString("deviceModel"));
    }

    @Test
    public void getDeviceMetadata_returnsDeviceAppGeneratedPersistentUuid() throws JSONException {
        when(uuidHelper.getPersistentUUID(context)).thenReturn("persistent-uuid");

        DeviceInspector sut = new DeviceInspector(appHelper, classHelper, uuidHelper);
        DeviceMetadata metadata = sut.getDeviceMetadata(context, "session-id", "integration-type");
        JSONObject metadataJSON = metadata.toJSON();
        assertEquals("persistent-uuid",
                metadataJSON.getString("deviceAppGeneratedPersistentUuid"));
    }

    @Test
    public void getDeviceMetadata_whenBuildProductManufacturerAndFingerprintAreValid_returnsFalseForIsSimulator() throws JSONException {
        ReflectionHelpers.setStaticField(Build.class, "PRODUCT", "randomBuildProduct");
        ReflectionHelpers.setStaticField(Build.class, "MANUFACTURER", "randomBuildManufacturer");
        ReflectionHelpers.setStaticField(Build.class, "FINGERPRINT", "randomBuildFingerprint");

        DeviceInspector sut = new DeviceInspector(appHelper, classHelper, uuidHelper);
        DeviceMetadata metadata = sut.getDeviceMetadata(context, "session-id", "integration-type");
        JSONObject metadataJSON = metadata.toJSON();
        assertEquals("false", metadataJSON.getString("isSimulator"));
    }

    @Test
    public void getDeviceMetadata_whenBuildProductIsGoogleSdk_returnsTrueForIsSimulator() throws JSONException {
        ReflectionHelpers.setStaticField(Build.class, "PRODUCT", "google_sdk");

        DeviceInspector sut = new DeviceInspector(appHelper, classHelper, uuidHelper);
        DeviceMetadata metadata = sut.getDeviceMetadata(context, "session-id", "integration-type");
        JSONObject metadataJSON = metadata.toJSON();
        assertTrue(metadataJSON.getBoolean("isSimulator"));
    }

    @Test
    public void getDeviceMetadata_whenBuildProductIsSdk_returnsTrueForIsSimulator() throws JSONException {
        ReflectionHelpers.setStaticField(Build.class, "PRODUCT", "sdk");

        DeviceInspector sut = new DeviceInspector(appHelper, classHelper, uuidHelper);
        DeviceMetadata metadata = sut.getDeviceMetadata(context, "session-id", "integration-type");
        JSONObject metadataJSON = metadata.toJSON();
        assertTrue(metadataJSON.getBoolean("isSimulator"));
    }

    @Test
    public void getDeviceMetadata_whenBuildManufacturerIsGenymotion_returnsTrueForIsSimulator() throws JSONException {
        ReflectionHelpers.setStaticField(Build.class, "MANUFACTURER", "Genymotion");

        DeviceInspector sut = new DeviceInspector(appHelper, classHelper, uuidHelper);
        DeviceMetadata metadata = sut.getDeviceMetadata(context, "session-id", "integration-type");
        JSONObject metadataJSON = metadata.toJSON();
        assertTrue(metadataJSON.getBoolean("isSimulator"));
    }

    @Test
    public void getDeviceMetadata_whenBuildFingerpintContainsGeneric_returnsTrueForIsSimulator() throws JSONException {
        ReflectionHelpers.setStaticField(Build.class, "FINGERPRINT", "generic");

        DeviceInspector sut = new DeviceInspector(appHelper, classHelper, uuidHelper);
        DeviceMetadata metadata = sut.getDeviceMetadata(context, "session-id", "integration-type");
        JSONObject metadataJSON = metadata.toJSON();
        assertTrue(metadataJSON.getBoolean("isSimulator"));
    }

    @Test
    public void getDeviceMetadata_forwardsSessionId() throws JSONException {
        DeviceInspector sut = new DeviceInspector(appHelper, classHelper, uuidHelper);
        DeviceMetadata metadata = sut.getDeviceMetadata(context, "session-id", "integration-type");
        JSONObject metadataJSON = metadata.toJSON();
        assertEquals("session-id", metadataJSON.getString("sessionId"));
    }

    @Test
    public void getDeviceMetadata_forwardsIntegrationType() throws JSONException {
        DeviceInspector sut = new DeviceInspector(appHelper, classHelper, uuidHelper);
        DeviceMetadata metadata = sut.getDeviceMetadata(context, "session-id", "integration-type");
        JSONObject metadataJSON = metadata.toJSON();
        assertEquals("integration-type", metadataJSON.getString("integrationType"));
    }

    // TODO: unit test network type

    @Test
    public void getDeviceMetadata_whenDeviceOrientationIsPortrait_returnsPortraitForUserInterfaceOrientation() throws JSONException {
        configuration.orientation = Configuration.ORIENTATION_PORTRAIT;

        DeviceInspector sut = new DeviceInspector(appHelper, classHelper, uuidHelper);
        DeviceMetadata metadata = sut.getDeviceMetadata(context, "session-id", "integration-type");
        JSONObject metadataJSON = metadata.toJSON();
        assertEquals("Portrait", metadataJSON.getString("userInterfaceOrientation"));
    }

    @Test
    public void getDeviceMetadata_whenDeviceOrientationIsLandscape_returnsLandscapeForUserInterfaceOrientation() throws JSONException {
        configuration.orientation = Configuration.ORIENTATION_LANDSCAPE;

        DeviceInspector sut = new DeviceInspector(appHelper, classHelper, uuidHelper);
        DeviceMetadata metadata = sut.getDeviceMetadata(context, "session-id", "integration-type");
        JSONObject metadataJSON = metadata.toJSON();
        assertEquals("Landscape", metadataJSON.getString("userInterfaceOrientation"));
    }

    @Test
    public void getDeviceMetadata_whenDeviceOrientationIsUndefined_returnsUnknownForUserInterfaceOrientation() throws JSONException {
        configuration.orientation = Configuration.ORIENTATION_UNDEFINED;

        DeviceInspector sut = new DeviceInspector(appHelper, classHelper, uuidHelper);
        DeviceMetadata metadata = sut.getDeviceMetadata(context, "session-id", "integration-type");
        JSONObject metadataJSON = metadata.toJSON();
        assertEquals("Unknown", metadataJSON.getString("userInterfaceOrientation"));
    }

    // TODO: unit test app version
    // TODO: unit test drop in version

    @Test
    public void getDeviceMetadata_forwardsIsPayPalInstalledResultFromAppHelper() throws JSONException {
        when(appHelper.isAppInstalled(context, "com.paypal.android.p2pmobile")).thenReturn(true);

        DeviceInspector sut = new DeviceInspector(appHelper, classHelper, uuidHelper);
        DeviceMetadata metadata = sut.getDeviceMetadata(context, "session-id", "integration-type");
        JSONObject metadataJSON = metadata.toJSON();
        assertTrue(metadataJSON.getBoolean("paypalInstalled"));
    }

    @Test
    public void getDeviceMetadata_forwardsIsVenmoInstalledResultFromAppHelper() throws JSONException {
        when(appHelper.isAppInstalled(context, "com.venmo")).thenReturn(true);

        DeviceInspector sut = new DeviceInspector(appHelper, classHelper, uuidHelper);
        DeviceMetadata metadata = sut.getDeviceMetadata(context, "session-id", "integration-type");
        JSONObject metadataJSON = metadata.toJSON();
        assertTrue(metadataJSON.getBoolean("venmoInstalled"));
    }
}
