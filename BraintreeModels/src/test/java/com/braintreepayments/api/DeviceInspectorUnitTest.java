package com.braintreepayments.api;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DeviceInspectorUnitTest {

    private Context context;
    private AppHelper appHelper;
    private ManifestValidator manifestValidator;

    @Before
    public void beforeEach() {
        context = mock(Context.class);
        appHelper = mock(AppHelper.class);
        manifestValidator = mock(ManifestValidator.class);
    }

    @Test
    public void isPayPalInstalled_forwardsResultFromAppHelper() {
        when(appHelper.isAppInstalled(context, "com.paypal.android.p2pmobile")).thenReturn(true);

        DeviceInspector sut = new DeviceInspector(appHelper, manifestValidator);
        assertTrue(sut.isPayPalInstalled(context));
    }

    @Test
    public void isVenmoInstalled_forwardsResultFromAppHelper() {
        when(appHelper.isAppInstalled(context, "com.venmo")).thenReturn(true);

        DeviceInspector sut = new DeviceInspector(appHelper, manifestValidator);
        assertTrue(sut.isVenmoInstalled(context));
    }

    @Test
    public void detectEmulator_defaultsToFalse() {
        DeviceInspector sut = new DeviceInspector(appHelper, manifestValidator);
        assertFalse(sut.isDeviceEmulator("randomBuildProduct", "randomBuildManufacturer", "randomBuildFingerprint"));
    }

    @Test
    public void detectEmulator_whenBuildProductIsGoogleSdk_returnsTrue() {
        DeviceInspector sut = new DeviceInspector(appHelper, manifestValidator);
        assertTrue(sut.isDeviceEmulator("google_sdk", "randomBuildManufacturer", "randomBuildFingerprint"));
    }

    @Test
    public void detectEmulator_whenBuildProductIsSdk_returnsTrue() {
        DeviceInspector sut = new DeviceInspector(appHelper, manifestValidator);
        assertTrue(sut.isDeviceEmulator("sdk", "randomBuildManufacturer", "randomBuildFingerprint"));
    }

    @Test
    public void detectEmulator_whenBuildManufacturerIsGenymotion_returnsTrue() {
        DeviceInspector sut = new DeviceInspector(appHelper, manifestValidator);
        assertTrue(sut.isDeviceEmulator("randomBuildProduct", "Genymotion", "randomBuildFingerprint"));
    }

    @Test
    public void detectEmulator_whenBuildFingerprintIsGeneric_returnsTrue() {
        DeviceInspector sut = new DeviceInspector(appHelper, manifestValidator);
        assertTrue(sut.isDeviceEmulator("randomBuildProduct", "randomBuildManufacturer", "generic"));
    }

    @Test
    public void getAppName_returnsAppNameFromPackageManager() throws PackageManager.NameNotFoundException {
        when(context.getPackageName()).thenReturn("sample-package-name");

        PackageManager packageManager = mock(PackageManager.class);
        when(context.getPackageManager()).thenReturn(packageManager);

        ApplicationInfo applicationInfo = new ApplicationInfo();
        when(packageManager.getApplicationInfo("sample-package-name", 0)).thenReturn(applicationInfo);

        when(packageManager.getApplicationLabel(applicationInfo)).thenReturn("SampleAppName");

        DeviceInspector sut = new DeviceInspector(appHelper, manifestValidator);
        assertEquals("SampleAppName", sut.getAppName(context));
    }

    @Test
    public void getAppName_whenApplicationInfoNotFound_returnsAppNameFromPackageManager() throws PackageManager.NameNotFoundException {
        when(context.getPackageName()).thenReturn("sample-package-name");

        PackageManager packageManager = mock(PackageManager.class);
        when(context.getPackageManager()).thenReturn(packageManager);

        when(packageManager.getApplicationInfo("sample-package-name", 0)).thenThrow(new PackageManager.NameNotFoundException());

        DeviceInspector sut = new DeviceInspector(appHelper, manifestValidator);
        assertEquals("ApplicationNameUnknown", sut.getAppName(context));
    }

    @Test
    public void isDeviceRooted_defaultsToFalse() throws IOException {
        File superUserApkFile = mock(File.class);
        when(superUserApkFile.exists()).thenReturn(false);

        Runtime runtime = mock(Runtime.class);
        Process process = mock(Process.class);

        when(runtime.exec(new String[]{"/system/xbin/which", "su"})).thenReturn(process);
        when(process.getInputStream()).thenReturn(new ByteArrayInputStream("".getBytes()));

        DeviceInspector sut = new DeviceInspector(appHelper, manifestValidator);
        assertFalse(sut.isDeviceRooted("", superUserApkFile, runtime));
    }

    @Test
    public void isDeviceRooted_whenBuildTagsIncludeTestKeys_returnsTrue() {
        DeviceInspector sut = new DeviceInspector(appHelper, manifestValidator);
        assertTrue(sut.isDeviceRooted("test-keys", mock(File.class), mock(Runtime.class)));
    }

    @Test
    public void isDeviceRooted_whenSuperUserApkFileExists_returnsTrue() {
        File superUserApkFile = mock(File.class);
        when(superUserApkFile.exists()).thenReturn(true);

        DeviceInspector sut = new DeviceInspector(appHelper, manifestValidator);
        assertTrue(sut.isDeviceRooted("", superUserApkFile, mock(Runtime.class)));
    }

    @Test
    public void isDeviceRooted_whenSuCommandPresentOnSystemPath_returnsTrue() throws IOException {
        Runtime runtime = mock(Runtime.class);
        Process process = mock(Process.class);

        when(runtime.exec(new String[]{"/system/xbin/which", "su"})).thenReturn(process);
        when(process.getInputStream()).thenReturn(new ByteArrayInputStream("/path/to/su/command".getBytes()));

        DeviceInspector sut = new DeviceInspector(appHelper, manifestValidator);
        assertTrue(sut.isDeviceRooted("", mock(File.class), runtime));
    }

    @Test
    public void isDeviceRooted_whenFileExistsThrows_returnsFalse() {
        File superUserApkFile = mock(File.class);
        when(superUserApkFile.exists()).thenThrow(new SecurityException());

        DeviceInspector sut = new DeviceInspector(appHelper, manifestValidator);
        assertFalse(sut.isDeviceRooted("", superUserApkFile, mock(Runtime.class)));
    }

    @Test
    public void isDeviceRooted_whenRuntimeExecThrows_returnsFalse() throws IOException {
        Runtime runtime = mock(Runtime.class);
        when(runtime.exec(new String[]{"/system/xbin/which", "su"})).thenThrow(new IOException());

        DeviceInspector sut = new DeviceInspector(appHelper, manifestValidator);
        assertFalse(sut.isDeviceRooted("", mock(File.class), runtime));
    }
}
