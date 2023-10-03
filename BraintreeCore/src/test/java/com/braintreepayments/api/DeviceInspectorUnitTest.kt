package com.braintreepayments.api

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Build
import android.os.Build.VERSION
import io.mockk.*
import org.json.JSONException
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
class DeviceInspectorUnitTest {

    private var context: Context = mockk(relaxed = true)
    private var configuration: Configuration = mockk(relaxed = true)
    private var connectivityManager: ConnectivityManager = mockk(relaxed = true)
    private var packageManager: PackageManager = mockk(relaxed = true)
    private var appHelper: AppHelper = mockk(relaxed = true)
    private var uuidHelper: UUIDHelper = mockk(relaxed = true)
    private var signatureVerifier: SignatureVerifier = mockk(relaxed = true)
    private lateinit var sut: DeviceInspector

    @Before
    @Throws(PackageManager.NameNotFoundException::class)
    fun beforeEach() {
        val resources = mockk<Resources>(relaxed = true)
        every { context.packageManager } returns packageManager
        every { context.packageName } returns "com.sample.app"
        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager
        every { context.resources } returns resources
        every { resources.configuration } returns configuration

        sut = DeviceInspector(
            appHelper,
            uuidHelper,
            signatureVerifier,
        )
    }

    @Test
    @Throws(JSONException::class)
    fun getDeviceMetadata_returnsPlatform() {
        val metadata = sut.getDeviceMetadata(context, "session-id", "integration-type")
        assertEquals("Android", metadata.toJSON().getString("platform"))
    }

    @Test
    @Throws(JSONException::class)
    fun getDeviceMetadata_returnsPlatformVersion() {
        ReflectionHelpers.setStaticField(VERSION::class.java, "SDK_INT", 123)
        val metadata = sut.getDeviceMetadata(context, "session-id", "integration-type")
        assertEquals("123", metadata.toJSON().getString("platformVersion"))
    }

    @Test
    @Throws(JSONException::class)
    fun getDeviceMetadata_returnsSDKVersion() {
        val metadata = sut.getDeviceMetadata(context, "session-id", "integration-type")
        assertEquals(BuildConfig.VERSION_NAME, metadata.toJSON().getString("sdkVersion"))
    }

    @Test
    @Throws(JSONException::class)
    fun getDeviceMetadata_returnsMerchantAppId() {
        val metadata = sut.getDeviceMetadata(context, "session-id", "integration-type")
        assertEquals("com.sample.app", metadata.toJSON().getString("merchantAppId"))
    }

    @Test
    @Throws(PackageManager.NameNotFoundException::class, JSONException::class)
    fun getDeviceMetadata_whenApplicationInfoUnavailable_returnsApplicationNameUnknown() {
        every { packageManager.getApplicationInfo("com.sample.app", 0) } throws PackageManager.NameNotFoundException()
        val metadata = sut.getDeviceMetadata(context, "session-id", "integration-type")
        assertEquals(
            "ApplicationNameUnknown",
            metadata.toJSON().getString("merchantAppName")
        )
    }

    @Test
    @Throws(PackageManager.NameNotFoundException::class, JSONException::class)
    fun getDeviceMetadata_returnsAppNameFromPackageManager() {
        val applicationInfo = ApplicationInfo()
        every { packageManager.getApplicationInfo("com.sample.app", 0) } returns applicationInfo
        every { packageManager.getApplicationLabel(applicationInfo) } returns "SampleAppName"
        val metadata = sut.getDeviceMetadata(context, "session-id", "integration-type")
        assertEquals("SampleAppName", metadata.toJSON().getString("merchantAppName"))
    }

    @Test
    @Throws(JSONException::class)
    fun getDeviceMetadata_returnsDeviceManufacturer() {
        ReflectionHelpers.setStaticField(Build::class.java, "MANUFACTURER", "device-manufacturer")
        val metadata = sut.getDeviceMetadata(context, "session-id", "integration-type")
        assertEquals(
            "device-manufacturer",
            metadata.toJSON().getString("deviceManufacturer")
        )
    }

    @Test
    @Throws(JSONException::class)
    fun getDeviceMetadata_returnsDeviceModel() {
        ReflectionHelpers.setStaticField(Build::class.java, "MODEL", "device-model")
        val metadata = sut.getDeviceMetadata(context, "session-id", "integration-type")
        assertEquals("device-model", metadata.toJSON().getString("deviceModel"))
    }

    @Test
    @Throws(JSONException::class)
    fun getDeviceMetadata_returnsDeviceAppGeneratedPersistentUuid() {
        every { uuidHelper.getPersistentUUID(context) } returns "persistent-uuid"
        val metadata = sut.getDeviceMetadata(context, "session-id", "integration-type")
        assertEquals(
            "persistent-uuid",
            metadata.toJSON().getString("deviceAppGeneratedPersistentUuid")
        )
    }

    @Test
    @Throws(JSONException::class)
    fun getDeviceMetadata_whenBuildProductManufacturerAndFingerprintAreValid_returnsFalseForIsSimulator() {
        ReflectionHelpers.setStaticField(Build::class.java, "PRODUCT", "randomBuildProduct")
        ReflectionHelpers.setStaticField(
            Build::class.java,
            "MANUFACTURER",
            "randomBuildManufacturer"
        )
        ReflectionHelpers.setStaticField(Build::class.java, "FINGERPRINT", "randomBuildFingerprint")
        val metadata = sut.getDeviceMetadata(context, "session-id", "integration-type")
        assertEquals("false", metadata.toJSON().getString("isSimulator"))
    }

    @Test
    @Throws(JSONException::class)
    fun getDeviceMetadata_whenBuildProductIsGoogleSdk_returnsTrueForIsSimulator() {
        ReflectionHelpers.setStaticField(Build::class.java, "PRODUCT", "google_sdk")
        val metadata = sut.getDeviceMetadata(context, "session-id", "integration-type")
        assertTrue(metadata.toJSON().getBoolean("isSimulator"))
    }

    @Test
    @Throws(JSONException::class)
    fun getDeviceMetadata_whenBuildProductIsSdk_returnsTrueForIsSimulator() {
        ReflectionHelpers.setStaticField(Build::class.java, "PRODUCT", "sdk")
        val metadata = sut.getDeviceMetadata(context, "session-id", "integration-type")
        assertTrue(metadata.toJSON().getBoolean("isSimulator"))
    }

    @Test
    @Throws(JSONException::class)
    fun getDeviceMetadata_whenBuildManufacturerIsGenymotion_returnsTrueForIsSimulator() {
        ReflectionHelpers.setStaticField(Build::class.java, "MANUFACTURER", "Genymotion")
        val metadata = sut.getDeviceMetadata(context, "session-id", "integration-type")
        assertTrue(metadata.toJSON().getBoolean("isSimulator"))
    }

    @Test
    @Throws(JSONException::class)
    fun getDeviceMetadata_whenBuildFingerpintContainsGeneric_returnsTrueForIsSimulator() {
        ReflectionHelpers.setStaticField(Build::class.java, "FINGERPRINT", "generic")
        val metadata = sut.getDeviceMetadata(context, "session-id", "integration-type")
        assertTrue(metadata.toJSON().getBoolean("isSimulator"))
    }

    @Test
    @Throws(JSONException::class)
    fun getDeviceMetadata_forwardsSessionId() {
        val metadata = sut.getDeviceMetadata(context, "session-id", "integration-type")
        assertEquals("session-id", metadata.toJSON().getString("sessionId"))
    }

    @Test
    @Throws(JSONException::class)
    fun getDeviceMetadata_forwardsIntegrationType() {
        val metadata = sut.getDeviceMetadata(context, "session-id", "integration-type")
        assertEquals("integration-type", metadata.toJSON().getString("integrationType"))
    }

    @Test
    @Throws(JSONException::class)
    fun getDeviceMetadata_returnsNetworkType() {
        val networkInfo = mockk<NetworkInfo>(relaxed = true)
        every { networkInfo.typeName } returns "network-type-name"
        every { connectivityManager.activeNetworkInfo } returns networkInfo
        val metadata = sut.getDeviceMetadata(context, "session-id", "integration-type")
        assertEquals("network-type-name", metadata.toJSON().getString("deviceNetworkType"))
    }

    @Test
    @Throws(JSONException::class)
    fun getDeviceMetadata_whenNetworkInfoUnavailable_returnsNoneForNetworkType() {
        every { connectivityManager.activeNetworkInfo } returns null
        val metadata = sut.getDeviceMetadata(context, "session-id", "integration-type")
        assertEquals("none", metadata.toJSON().getString("deviceNetworkType"))
    }

    @Test
    @Throws(JSONException::class)
    fun getDeviceMetadata_whenDeviceOrientationIsPortrait_returnsPortraitForUserInterfaceOrientation() {
        configuration.orientation = Configuration.ORIENTATION_PORTRAIT
        val metadata = sut.getDeviceMetadata(context, "session-id", "integration-type")
        assertEquals("Portrait", metadata.toJSON().getString("userInterfaceOrientation"))
    }

    @Test
    @Throws(JSONException::class)
    fun getDeviceMetadata_whenDeviceOrientationIsLandscape_returnsLandscapeForUserInterfaceOrientation() {
        configuration.orientation = Configuration.ORIENTATION_LANDSCAPE
        val metadata = sut.getDeviceMetadata(context, "session-id", "integration-type")
        assertEquals("Landscape", metadata.toJSON().getString("userInterfaceOrientation"))
    }

    @Test
    @Throws(JSONException::class)
    fun getDeviceMetadata_whenDeviceOrientationIsUndefined_returnsUnknownForUserInterfaceOrientation() {
        configuration.orientation = Configuration.ORIENTATION_UNDEFINED
        val metadata = sut.getDeviceMetadata(context, "session-id", "integration-type")
        assertEquals("Unknown", metadata.toJSON().getString("userInterfaceOrientation"))
    }

    @Test
    @Throws(JSONException::class, PackageManager.NameNotFoundException::class)
    fun getDeviceMetadata_returnsAppVersion() {
        val packageInfo = PackageInfo()
        packageInfo.versionName = "AppVersion"
        every { packageManager.getPackageInfo("com.sample.app", 0) } returns packageInfo
        val metadata = sut.getDeviceMetadata(context, "session-id", "integration-type")
        assertEquals("AppVersion", metadata.toJSON().getString("merchantAppVersion"))
    }

    @Test
    @Throws(JSONException::class, PackageManager.NameNotFoundException::class)
    fun getDeviceMetadata_whenAppVersionUnavailable_returnsVersionUnknownByDefault() {
        every { packageManager.getPackageInfo("com.sample.app", 0) } throws PackageManager.NameNotFoundException()
        val metadata = sut.getDeviceMetadata(context, "session-id", "integration-type")
        assertEquals("VersionUnknown", metadata.toJSON().getString("merchantAppVersion"))
    }

    @Test
    @Throws(JSONException::class)
    fun getDeviceMetadata_forwardsDropInVersionFromClassHelper() {
        mockkObject(DeviceInspector) {
            every { DeviceInspector.getDropInVersion() } returns "drop-in-version"
            val metadata = sut.getDeviceMetadata(context, "session-id", "integration-type")
            assertEquals("drop-in-version", metadata.toJSON().getString("dropinVersion"))
        }
    }

    @Test
    @Throws(JSONException::class)
    fun getDeviceMetadata_forwardsIsPayPalInstalledResultFromAppHelper() {
        every { appHelper.isAppInstalled(context, "com.paypal.android.p2pmobile") } returns true
        val metadata = sut.getDeviceMetadata(context, "session-id", "integration-type")
        assertTrue(metadata.toJSON().getBoolean("paypalInstalled"))
    }

    @Test
    @Throws(JSONException::class)
    fun getDeviceMetadata_forwardsIsVenmoInstalledResultFromAppHelper() {
        every { appHelper.isAppInstalled(context, "com.venmo") } returns true
        val metadata = sut.getDeviceMetadata(context, "session-id", "integration-type")
        assertTrue(metadata.toJSON().getBoolean("venmoInstalled"))
    }

    @Test
    fun isPayPalInstalled_forwardsIsPayPalInstalledResultFromAppHelper() {
        every { appHelper.isAppInstalled(context, "com.paypal.android.p2pmobile") } returns true
        assertTrue(sut.isPayPalInstalled(context))
    }

    @Test
    fun isVenmoInstalled_forwardsIsVenmoInstalledResultFromAppHelper() {
        every { appHelper.isAppInstalled(context, "com.venmo") } returns true
        assertTrue(sut.isVenmoInstalled(context))
    }

    @Test
    fun isVenmoAppSwitchAvailable_checksForVenmoIntentAvailability() {
        sut.isVenmoAppSwitchAvailable(context)
        val intentSlot = slot<Intent>()
        verify { appHelper.isIntentAvailable(context, capture(intentSlot)) }

        val intent = intentSlot.captured
        val component = intent.component
        assertEquals("com.venmo", component!!.packageName)
        assertEquals("com.venmo.controller.SetupMerchantActivity", component.className)
    }

    @Test
    fun isVenmoAppSwitchAvailable_whenVenmoIsNotInstalled_returnsFalse() {
        every { appHelper.isIntentAvailable(context, ofType(Intent::class)) } returns false
        assertFalse(sut.isVenmoAppSwitchAvailable(context))
    }

    @Test
    fun isVenmoAppSwitchAvailable_whenVenmoIsInstalledSignatureIsNotValid_returnsFalse() {
        every { appHelper.isIntentAvailable(context, ofType(Intent::class)) } returns true

        every {
            signatureVerifier.isSignatureValid(
                context,
                "com.venmo",
                DeviceInspector.VENMO_BASE_64_ENCODED_SIGNATURE
            )
        } returns false

        assertFalse(sut.isVenmoAppSwitchAvailable(context))
    }

    @Test
    fun isVenmoAppSwitchAvailable_whenVenmoIsInstalledSignatureIsValid_returnsTrue() {
        every { appHelper.isIntentAvailable(context, ofType(Intent::class)) } returns true

        every {
            signatureVerifier.isSignatureValid(
                context,
                "com.venmo",
                DeviceInspector.VENMO_BASE_64_ENCODED_SIGNATURE
            )
        } returns true

        assertTrue(sut.isVenmoAppSwitchAvailable(context))
    }
}
