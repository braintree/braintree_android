package com.braintreepayments.api;

import static com.braintreepayments.api.DeviceInspector.VENMO_BASE_64_ENCODED_SIGNATURE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.util.ReflectionHelpers;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

@RunWith(RobolectricTestRunner.class)
public class DeviceInspectorUnitTest {

    private Context context;
    private Configuration configuration;
    private ConnectivityManager connectivityManager;

    private PackageManager packageManager;

    private AppHelper appHelper;
    private ClassHelper classHelper;
    private UUIDHelper uuidHelper;
    private SignatureVerifier signatureVerifier;

    private Runtime runtime;
    private Process process;
    private File superUserApkFile;

    private DeviceInspector sut;

    @Before
    public void beforeEach() throws PackageManager.NameNotFoundException {
        context = mock(Context.class);
        Resources resources = mock(Resources.class);
        connectivityManager = mock(ConnectivityManager.class);
        configuration = new Configuration();

        packageManager = mock(PackageManager.class);

        appHelper = mock(AppHelper.class);
        uuidHelper = mock(UUIDHelper.class);
        classHelper = mock(ClassHelper.class);
        signatureVerifier = mock(SignatureVerifier.class);

        superUserApkFile = mock(File.class);
        runtime = mock(Runtime.class);
        process = mock(Process.class);

        when(context.getPackageManager()).thenReturn(packageManager);
        when(context.getPackageName()).thenReturn("com.sample.app");

        when(context.getResources()).thenReturn(resources);
        when(resources.getConfiguration()).thenReturn(configuration);

        when(context.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(connectivityManager);

        sut = new DeviceInspector(appHelper, classHelper, uuidHelper, signatureVerifier, runtime, superUserApkFile);
    }

    @Test
    public void getDeviceMetadata_returnsPlatform() throws JSONException {
        DeviceMetadata metadata = sut.getDeviceMetadata(context, "session-id", "integration-type");
        assertEquals("Android", metadata.toJSON().getString("platform"));
    }

    @Test
    public void getDeviceMetadata_returnsPlatformVersion() throws JSONException {
        ReflectionHelpers.setStaticField(Build.VERSION.class, "SDK_INT", 123);

        DeviceMetadata metadata = sut.getDeviceMetadata(context, "session-id", "integration-type");
        assertEquals("123", metadata.toJSON().getString("platformVersion"));
    }

    @Test
    public void getDeviceMetadata_returnsSDKVersion() throws JSONException {
        DeviceMetadata metadata = sut.getDeviceMetadata(context, "session-id", "integration-type");
        assertEquals(BuildConfig.VERSION_NAME, metadata.toJSON().getString("sdkVersion"));
    }

    @Test
    public void getDeviceMetadata_returnsMerchantAppId() throws JSONException {
        DeviceMetadata metadata = sut.getDeviceMetadata(context, "session-id", "integration-type");
        assertEquals("com.sample.app", metadata.toJSON().getString("merchantAppId"));
    }

    @Test
    public void getDeviceMetadata_returnsApplicationNameUnknownByDefault() throws JSONException {
        DeviceMetadata metadata = sut.getDeviceMetadata(context, "session-id", "integration-type");
        assertEquals("ApplicationNameUnknown", metadata.toJSON().getString("merchantAppName"));
    }

    @Test
    public void getDeviceMetadata_whenApplicationInfoUnavailable_returnsApplicationNameUnknown() throws PackageManager.NameNotFoundException, JSONException {
        when(packageManager.getApplicationInfo("com.sample.app", 0)).thenThrow(new PackageManager.NameNotFoundException());

        DeviceMetadata metadata = sut.getDeviceMetadata(context, "session-id", "integration-type");
        assertEquals("ApplicationNameUnknown", metadata.toJSON().getString("merchantAppName"));
    }

    @Test
    public void getDeviceMetadata_returnsAppNameFromPackageManager() throws PackageManager.NameNotFoundException, JSONException {
        ApplicationInfo applicationInfo = new ApplicationInfo();
        when(packageManager.getApplicationInfo("com.sample.app", 0)).thenReturn(applicationInfo);
        when(packageManager.getApplicationLabel(applicationInfo)).thenReturn("SampleAppName");

        DeviceMetadata metadata = sut.getDeviceMetadata(context, "session-id", "integration-type");
        assertEquals("SampleAppName", metadata.toJSON().getString("merchantAppName"));
    }

    @Test
    public void getDeviceMetadata_whenApplicationInfoNotFound_returnsAppNameFromPackageManager() throws PackageManager.NameNotFoundException, JSONException {
        when(packageManager.getApplicationInfo("sample-package-name", 0)).thenThrow(new PackageManager.NameNotFoundException());

        DeviceMetadata metadata = sut.getDeviceMetadata(context, "session-id", "integration-type");
        assertEquals("ApplicationNameUnknown", metadata.toJSON().getString("merchantAppName"));
    }

    @Test
    public void getDeviceMetadata_returnsDeviceManufacturer() throws JSONException {
        ReflectionHelpers.setStaticField(Build.class, "MANUFACTURER", "device-manufacturer");

        DeviceMetadata metadata = sut.getDeviceMetadata(context, "session-id", "integration-type");
        assertEquals("device-manufacturer", metadata.toJSON().getString("deviceManufacturer"));
    }

    @Test
    public void getDeviceMetadata_returnsDeviceModel() throws JSONException {
        ReflectionHelpers.setStaticField(Build.class, "MODEL", "device-model");

        DeviceMetadata metadata = sut.getDeviceMetadata(context, "session-id", "integration-type");
        assertEquals("device-model", metadata.toJSON().getString("deviceModel"));
    }

    @Test
    public void getDeviceMetadata_returnsDeviceAppGeneratedPersistentUuid() throws JSONException {
        when(uuidHelper.getPersistentUUID(context)).thenReturn("persistent-uuid");

        DeviceMetadata metadata = sut.getDeviceMetadata(context, "session-id", "integration-type");
        assertEquals("persistent-uuid",
                metadata.toJSON().getString("deviceAppGeneratedPersistentUuid"));
    }

    @Test
    public void getDeviceMetadata_whenBuildProductManufacturerAndFingerprintAreValid_returnsFalseForIsSimulator() throws JSONException {
        ReflectionHelpers.setStaticField(Build.class, "PRODUCT", "randomBuildProduct");
        ReflectionHelpers.setStaticField(Build.class, "MANUFACTURER", "randomBuildManufacturer");
        ReflectionHelpers.setStaticField(Build.class, "FINGERPRINT", "randomBuildFingerprint");

        DeviceMetadata metadata = sut.getDeviceMetadata(context, "session-id", "integration-type");
        assertEquals("false", metadata.toJSON().getString("isSimulator"));
    }

    @Test
    public void getDeviceMetadata_whenBuildProductIsGoogleSdk_returnsTrueForIsSimulator() throws JSONException {
        ReflectionHelpers.setStaticField(Build.class, "PRODUCT", "google_sdk");

        DeviceMetadata metadata = sut.getDeviceMetadata(context, "session-id", "integration-type");
        assertTrue(metadata.toJSON().getBoolean("isSimulator"));
    }

    @Test
    public void getDeviceMetadata_whenBuildProductIsSdk_returnsTrueForIsSimulator() throws JSONException {
        ReflectionHelpers.setStaticField(Build.class, "PRODUCT", "sdk");

        DeviceMetadata metadata = sut.getDeviceMetadata(context, "session-id", "integration-type");
        assertTrue(metadata.toJSON().getBoolean("isSimulator"));
    }

    @Test
    public void getDeviceMetadata_whenBuildManufacturerIsGenymotion_returnsTrueForIsSimulator() throws JSONException {
        ReflectionHelpers.setStaticField(Build.class, "MANUFACTURER", "Genymotion");

        DeviceMetadata metadata = sut.getDeviceMetadata(context, "session-id", "integration-type");
        assertTrue(metadata.toJSON().getBoolean("isSimulator"));
    }

    @Test
    public void getDeviceMetadata_whenBuildFingerpintContainsGeneric_returnsTrueForIsSimulator() throws JSONException {
        ReflectionHelpers.setStaticField(Build.class, "FINGERPRINT", "generic");

        DeviceMetadata metadata = sut.getDeviceMetadata(context, "session-id", "integration-type");
        assertTrue(metadata.toJSON().getBoolean("isSimulator"));
    }

    @Test
    public void getDeviceMetadata_forwardsSessionId() throws JSONException {
        DeviceMetadata metadata = sut.getDeviceMetadata(context, "session-id", "integration-type");
        assertEquals("session-id", metadata.toJSON().getString("sessionId"));
    }

    @Test
    public void getDeviceMetadata_forwardsIntegrationType() throws JSONException {
        DeviceMetadata metadata = sut.getDeviceMetadata(context, "session-id", "integration-type");
        assertEquals("integration-type", metadata.toJSON().getString("integrationType"));
    }

    @Test
    public void getDeviceMetadata_returnsNetworkType() throws JSONException {
        NetworkInfo networkInfo = mock(NetworkInfo.class);
        when(networkInfo.getTypeName()).thenReturn("network-type-name");
        when(connectivityManager.getActiveNetworkInfo()).thenReturn(networkInfo);

        DeviceMetadata metadata = sut.getDeviceMetadata(context, "session-id", "integration-type");
        assertEquals("network-type-name", metadata.toJSON().getString("deviceNetworkType"));
    }

    @Test
    public void getDeviceMetadata_whenNetworkInfoUnavailable_returnsNoneForNetworkType() throws JSONException {
        DeviceMetadata metadata = sut.getDeviceMetadata(context, "session-id", "integration-type");
        assertEquals("none", metadata.toJSON().getString("deviceNetworkType"));
    }

    @Test
    public void getDeviceMetadata_whenDeviceOrientationIsPortrait_returnsPortraitForUserInterfaceOrientation() throws JSONException {
        configuration.orientation = Configuration.ORIENTATION_PORTRAIT;

        DeviceMetadata metadata = sut.getDeviceMetadata(context, "session-id", "integration-type");
        assertEquals("Portrait", metadata.toJSON().getString("userInterfaceOrientation"));
    }

    @Test
    public void getDeviceMetadata_whenDeviceOrientationIsLandscape_returnsLandscapeForUserInterfaceOrientation() throws JSONException {
        configuration.orientation = Configuration.ORIENTATION_LANDSCAPE;

        DeviceMetadata metadata = sut.getDeviceMetadata(context, "session-id", "integration-type");
        assertEquals("Landscape", metadata.toJSON().getString("userInterfaceOrientation"));
    }

    @Test
    public void getDeviceMetadata_whenDeviceOrientationIsUndefined_returnsUnknownForUserInterfaceOrientation() throws JSONException {
        configuration.orientation = Configuration.ORIENTATION_UNDEFINED;

        DeviceMetadata metadata = sut.getDeviceMetadata(context, "session-id", "integration-type");
        assertEquals("Unknown", metadata.toJSON().getString("userInterfaceOrientation"));
    }

    @Test
    public void getDeviceMetadata_returnsAppVersion() throws JSONException, PackageManager.NameNotFoundException {
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.versionName = "AppVersion";
        when(packageManager.getPackageInfo("com.sample.app", 0)).thenReturn(packageInfo);

        DeviceMetadata metadata = sut.getDeviceMetadata(context, "session-id", "integration-type");
        assertEquals("AppVersion", metadata.toJSON().getString("merchantAppVersion"));
    }

    @Test
    public void getDeviceMetadata_whenAppVersionUnavailable_returnsVersionUnknownByDefault() throws JSONException, PackageManager.NameNotFoundException {
        when(packageManager.getPackageInfo("com.sample.app", 0)).thenThrow(new PackageManager.NameNotFoundException());

        DeviceMetadata metadata = sut.getDeviceMetadata(context, "session-id", "integration-type");
        assertEquals("VersionUnknown", metadata.toJSON().getString("merchantAppVersion"));
    }

    @Test
    public void getDeviceMetadata_forwardsDropInVersionFromClassHelper() throws JSONException {
        String className = "com.braintreepayments.api.dropin.BuildConfig";
        String fieldName = "VERSION_NAME";
        when(classHelper.getFieldValue(className, fieldName)).thenReturn("drop-in-version");

        DeviceMetadata metadata = sut.getDeviceMetadata(context, "session-id", "integration-type");
        assertEquals("drop-in-version", metadata.toJSON().getString("dropinVersion"));
    }

    @Test
    public void getDeviceMetadata_forwardsIsPayPalInstalledResultFromAppHelper() throws JSONException {
        when(appHelper.isAppInstalled(context, "com.paypal.android.p2pmobile")).thenReturn(true);

        DeviceMetadata metadata = sut.getDeviceMetadata(context, "session-id", "integration-type");
        assertTrue(metadata.toJSON().getBoolean("paypalInstalled"));
    }

    @Test
    public void getDeviceMetadata_forwardsIsVenmoInstalledResultFromAppHelper() throws JSONException {
        when(appHelper.isAppInstalled(context, "com.venmo")).thenReturn(true);

        DeviceMetadata metadata = sut.getDeviceMetadata(context, "session-id", "integration-type");
        assertTrue(metadata.toJSON().getBoolean("venmoInstalled"));
    }

    @Test
    public void isPayPalInstalled_forwardsIsPayPalInstalledResultFromAppHelper() {
        when(appHelper.isAppInstalled(context, "com.paypal.android.p2pmobile")).thenReturn(true);
        assertTrue(sut.isPayPalInstalled(context));
    }

    @Test
    public void isVenmoInstalled_forwardsIsVenmoInstalledResultFromAppHelper() {
        when(appHelper.isAppInstalled(context, "com.venmo")).thenReturn(true);
        assertTrue(sut.isVenmoInstalled(context));
    }

    @Test
    public void isVenmoAppSwitchAvailable_checksForVenmoIntentAvailability() {
        sut.isVenmoAppSwitchAvailable(context);

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(appHelper).isIntentAvailable(same(context), captor.capture());

        Intent intent = captor.getValue();
        ComponentName component = intent.getComponent();
        assertEquals("com.venmo", component.getPackageName());
        assertEquals("com.venmo.controller.SetupMerchantActivity", component.getClassName());
    }

    @Test
    public void isVenmoAppSwitchAvailable_whenVenmoIsNotInstalled_returnsFalse() {
        when(appHelper.isIntentAvailable(same(context), any(Intent.class))).thenReturn(false);
        assertFalse(sut.isVenmoAppSwitchAvailable(context));
    }

    @Test
    public void isVenmoAppSwitchAvailable_whenVenmoIsInstalledSignatureIsNotValid_returnsFalse() {
        when(appHelper.isIntentAvailable(same(context), any(Intent.class))).thenReturn(true);
        when(
                signatureVerifier.isSignatureValid(context, "com.venmo", VENMO_BASE_64_ENCODED_SIGNATURE)
        ).thenReturn(false);

        assertFalse(sut.isVenmoAppSwitchAvailable(context));
    }

    @Test
    public void isVenmoAppSwitchAvailable_whenVenmoIsInstalledSignatureIsValid_returnsTrue() {
        when(appHelper.isIntentAvailable(same(context), any(Intent.class))).thenReturn(true);
        when(
                signatureVerifier.isSignatureValid(context, "com.venmo", VENMO_BASE_64_ENCODED_SIGNATURE)
        ).thenReturn(true);

        assertTrue(sut.isVenmoAppSwitchAvailable(context));
    }
}
