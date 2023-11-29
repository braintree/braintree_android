package com.braintreepayments.api

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.net.ConnectivityManager
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
    private var signatureVerifier: SignatureVerifier = mockk(relaxed = true)
    private lateinit var sut: DeviceInspector

    private val btConfiguration =
        com.braintreepayments.api.Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ENVIRONMENT)

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
            signatureVerifier,
        )
    }

    @Test
    @Throws(JSONException::class)
    fun getDeviceMetadata_returnsPlatform() {
        val metadata = sut.getDeviceMetadata(context, btConfiguration, "session-id", "integration-type")
        assertEquals("Android", metadata.toJSON().getString("platform"))
    }

    @Test
    @Throws(JSONException::class)
    fun getDeviceMetadata_returnsEventSource() {
        val metadata = sut.getDeviceMetadata(context, btConfiguration, "session-id", "integration-type")
        assertEquals("mobile-native", metadata.toJSON().getString("event_source"))
    }

    @Test
    @Throws(JSONException::class)
    fun getDeviceMetadata_returnsComponent() {
        val metadata = sut.getDeviceMetadata(context, btConfiguration, "session-id", "integration-type")
        assertEquals("braintreeclientsdk", metadata.toJSON().getString("comp"))
    }

    @Test
    @Throws(JSONException::class)
    fun getDeviceMetadata_returnsPlatformVersion() {
        ReflectionHelpers.setStaticField(VERSION::class.java, "SDK_INT", 123)
        val metadata = sut.getDeviceMetadata(context, btConfiguration, "session-id", "integration-type")
        assertEquals("Android API 123", metadata.toJSON().getString("client_os"))
    }

    @Test
    @Throws(JSONException::class)
    fun getDeviceMetadata_returnsSDKVersion() {
        val metadata = sut.getDeviceMetadata(context, btConfiguration, "session-id", "integration-type")
        assertEquals(BuildConfig.VERSION_NAME, metadata.toJSON().getString("c_sdk_ver"))
    }

    @Test
    @Throws(JSONException::class)
    fun getDeviceMetadata_returnsAppId() {
        val metadata = sut.getDeviceMetadata(context, btConfiguration, "session-id", "integration-type")
        assertEquals("com.sample.app", metadata.toJSON().getString("app_id"))
    }

    @Test
    @Throws(PackageManager.NameNotFoundException::class, JSONException::class)
    fun getDeviceMetadata_whenApplicationInfoUnavailable_returnsApplicationNameUnknown() {
        every { packageManager.getApplicationInfo("com.sample.app", 0) } throws PackageManager.NameNotFoundException()
        val metadata = sut.getDeviceMetadata(context, btConfiguration, "session-id", "integration-type")
        assertEquals(
            "ApplicationNameUnknown",
            metadata.toJSON().getString("app_name")
        )
    }

    @Test
    @Throws(PackageManager.NameNotFoundException::class, JSONException::class)
    fun getDeviceMetadata_returnsAppNameFromPackageManager() {
        val applicationInfo = ApplicationInfo()
        every { packageManager.getApplicationInfo("com.sample.app", 0) } returns applicationInfo
        every { packageManager.getApplicationLabel(applicationInfo) } returns "SampleAppName"
        val metadata = sut.getDeviceMetadata(context, btConfiguration, "session-id", "integration-type")
        assertEquals("SampleAppName", metadata.toJSON().getString("app_name"))
    }

    @Test
    @Throws(JSONException::class)
    fun getDeviceMetadata_returnsDeviceManufacturer() {
        ReflectionHelpers.setStaticField(Build::class.java, "MANUFACTURER", "device-manufacturer")
        val metadata = sut.getDeviceMetadata(context, btConfiguration, "session-id", "integration-type")
        assertEquals(
            "device-manufacturer",
            metadata.toJSON().getString("device_manufacturer")
        )
    }

    @Test
    @Throws(JSONException::class)
    fun getDeviceMetadata_returnsDeviceModel() {
        ReflectionHelpers.setStaticField(Build::class.java, "MODEL", "device-model")
        val metadata = sut.getDeviceMetadata(context, btConfiguration, "session-id", "integration-type")
        assertEquals("device-model", metadata.toJSON().getString("mobile_device_model"))
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
        val metadata = sut.getDeviceMetadata(context, btConfiguration, "session-id", "integration-type")
        assertEquals("false", metadata.toJSON().getString("is_simulator"))
    }

    @Test
    @Throws(JSONException::class)
    fun getDeviceMetadata_whenBuildProductIsGoogleSdk_returnsTrueForIsSimulator() {
        ReflectionHelpers.setStaticField(Build::class.java, "PRODUCT", "google_sdk")
        val metadata = sut.getDeviceMetadata(context, btConfiguration, "session-id", "integration-type")
        assertTrue(metadata.toJSON().getBoolean("is_simulator"))
    }

    @Test
    @Throws(JSONException::class)
    fun getDeviceMetadata_whenBuildProductIsSdk_returnsTrueForIsSimulator() {
        ReflectionHelpers.setStaticField(Build::class.java, "PRODUCT", "sdk")
        val metadata = sut.getDeviceMetadata(context, btConfiguration, "session-id", "integration-type")
        assertTrue(metadata.toJSON().getBoolean("is_simulator"))
    }

    @Test
    @Throws(JSONException::class)
    fun getDeviceMetadata_whenBuildManufacturerIsGenymotion_returnsTrueForIsSimulator() {
        ReflectionHelpers.setStaticField(Build::class.java, "MANUFACTURER", "Genymotion")
        val metadata = sut.getDeviceMetadata(context, btConfiguration, "session-id", "integration-type")
        assertTrue(metadata.toJSON().getBoolean("is_simulator"))
    }

    @Test
    @Throws(JSONException::class)
    fun getDeviceMetadata_whenBuildFingerpintContainsGeneric_returnsTrueForIsSimulator() {
        ReflectionHelpers.setStaticField(Build::class.java, "FINGERPRINT", "generic")
        val metadata = sut.getDeviceMetadata(context, btConfiguration, "session-id", "integration-type")
        assertTrue(metadata.toJSON().getBoolean("is_simulator"))
    }

    @Test
    @Throws(JSONException::class)
    fun getDeviceMetadata_forwardsSessionId() {
        val metadata = sut.getDeviceMetadata(context, btConfiguration, "session-id", "integration-type")
        assertEquals("session-id", metadata.toJSON().getString("session_id"))
    }

    @Test
    @Throws(JSONException::class)
    fun getDeviceMetadata_forwardsIntegrationType() {
        val metadata = sut.getDeviceMetadata(context, btConfiguration, "session-id", "integration-type")
        assertEquals("integration-type", metadata.toJSON().getString("api_integration_type"))
    }

    @Test
    @Throws(JSONException::class, PackageManager.NameNotFoundException::class)
    fun getDeviceMetadata_returnsAppVersion() {
        val packageInfo = PackageInfo()
        packageInfo.versionName = "AppVersion"
        every { packageManager.getPackageInfo("com.sample.app", 0) } returns packageInfo
        val metadata = sut.getDeviceMetadata(context, btConfiguration, "session-id", "integration-type")
        assertEquals("AppVersion", metadata.toJSON().getString("mapv"))
    }

    @Test
    @Throws(JSONException::class, PackageManager.NameNotFoundException::class)
    fun getDeviceMetadata_whenAppVersionUnavailable_returnsVersionUnknownByDefault() {
        every { packageManager.getPackageInfo("com.sample.app", 0) } throws PackageManager.NameNotFoundException()
        val metadata = sut.getDeviceMetadata(context, btConfiguration, "session-id", "integration-type")
        assertEquals("VersionUnknown", metadata.toJSON().getString("mapv"))
    }

    @Test
    @Throws(JSONException::class)
    fun getDeviceMetadata_forwardsDropInVersionFromClassHelper() {
        mockkObject(DeviceInspector) {
            every { DeviceInspector.getDropInVersion() } returns "fake-drop-in-version"
            val metadata = sut.getDeviceMetadata(context, btConfiguration, "session-id", "integration-type")
            assertEquals("fake-drop-in-version", metadata.toJSON().getString("drop_in_sdk_ver"))
        }
    }

    @Test
    @Throws(JSONException::class)
    fun getSDKEnvironment_forwardsEnvironmentFromConfig() {
        val metadata = sut.getDeviceMetadata(context, btConfiguration, "session-id", "integration-type")
        assertEquals("test", metadata.toJSON().getString("merchant_sdk_env"))
    }

    @Test
    @Throws(JSONException::class)
    fun getMerchantID_forwardsMerchantIdFromConfig() {
        val metadata = sut.getDeviceMetadata(context, btConfiguration, "session-id", "integration-type")
        assertEquals("integration_merchant_id", metadata.toJSON().getString("merchant_id"))
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
