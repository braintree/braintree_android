package com.braintreepayments.api.core

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
import com.braintreepayments.api.sharedutils.AppHelper
import com.braintreepayments.api.sharedutils.SignatureVerifier
import com.braintreepayments.api.testutils.Fixtures
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.verify
import org.json.JSONException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
        com.braintreepayments.api.core.Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ENVIRONMENT)

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
            context = context,
            appHelper = appHelper,
            signatureVerifier = signatureVerifier,
        )
    }

    @Test
    @Throws(JSONException::class)
    fun getDeviceMetadata_returnsPlatform() {
        val metadata =
            sut.getDeviceMetadata(context, btConfiguration, "session-id", IntegrationType.CUSTOM)
        assertEquals("Android", metadata.platform)
    }

    @Test
    @Throws(JSONException::class)
    fun getDeviceMetadata_returnsEventSource() {
        val metadata =
            sut.getDeviceMetadata(context, btConfiguration, "session-id", IntegrationType.CUSTOM)
        assertEquals("mobile-native", metadata.eventSource)
    }

    @Test
    @Throws(JSONException::class)
    fun getDeviceMetadata_returnsComponent() {
        val metadata =
            sut.getDeviceMetadata(context, btConfiguration, "session-id", IntegrationType.CUSTOM)
        assertEquals("braintreeclientsdk", metadata.component)
    }

    @Test
    @Throws(JSONException::class)
    fun getDeviceMetadata_returnsPlatformVersion() {
        ReflectionHelpers.setStaticField(VERSION::class.java, "SDK_INT", 123)
        val metadata =
            sut.getDeviceMetadata(context, btConfiguration, "session-id", IntegrationType.CUSTOM)
        assertEquals("Android API 123", metadata.clientOs)
    }

    @Test
    @Throws(JSONException::class)
    fun getDeviceMetadata_returnsSDKVersion() {
        val metadata =
            sut.getDeviceMetadata(context, btConfiguration, "session-id", IntegrationType.CUSTOM)
        assertEquals(BuildConfig.VERSION_NAME, metadata.clientSDKVersion)
    }

    @Test
    @Throws(JSONException::class)
    fun getDeviceMetadata_returnsAppId() {
        val metadata =
            sut.getDeviceMetadata(context, btConfiguration, "session-id", IntegrationType.CUSTOM)
        assertEquals("com.sample.app", metadata.appId)
    }

    @Test
    @Throws(PackageManager.NameNotFoundException::class, JSONException::class)
    fun getDeviceMetadata_whenApplicationInfoUnavailable_returnsApplicationNameUnknown() {
        every {
            packageManager.getApplicationInfo("com.sample.app", 0)
        } throws PackageManager.NameNotFoundException()
        val metadata =
            sut.getDeviceMetadata(context, btConfiguration, "session-id", IntegrationType.CUSTOM)
        assertEquals("ApplicationNameUnknown", metadata.appName)
    }

    @Test
    @Throws(PackageManager.NameNotFoundException::class, JSONException::class)
    fun getDeviceMetadata_returnsAppNameFromPackageManager() {
        val applicationInfo = ApplicationInfo()
        every { packageManager.getApplicationInfo("com.sample.app", 0) } returns applicationInfo
        every { packageManager.getApplicationLabel(applicationInfo) } returns "SampleAppName"
        val metadata =
            sut.getDeviceMetadata(context, btConfiguration, "session-id", IntegrationType.CUSTOM)
        assertEquals("SampleAppName", metadata.appName)
    }

    @Test
    @Throws(JSONException::class)
    fun getDeviceMetadata_returnsDeviceManufacturer() {
        ReflectionHelpers.setStaticField(Build::class.java, "MANUFACTURER", "device-manufacturer")
        val metadata =
            sut.getDeviceMetadata(context, btConfiguration, "session-id", IntegrationType.CUSTOM)
        assertEquals("device-manufacturer", metadata.deviceManufacturer)
    }

    @Test
    @Throws(JSONException::class)
    fun getDeviceMetadata_returnsDeviceModel() {
        ReflectionHelpers.setStaticField(Build::class.java, "MODEL", "device-model")
        val metadata =
            sut.getDeviceMetadata(context, btConfiguration, "session-id", IntegrationType.CUSTOM)
        assertEquals("device-model", metadata.deviceModel)
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
        val metadata =
            sut.getDeviceMetadata(context, btConfiguration, "session-id", IntegrationType.CUSTOM)
        assertFalse(metadata.isSimulator)
    }

    @Test
    @Throws(JSONException::class)
    fun getDeviceMetadata_whenBuildFingerpintContainsGeneric_returnsTrueForIsSimulator() {
        ReflectionHelpers.setStaticField(Build::class.java, "FINGERPRINT", "generic")
        val metadata =
            sut.getDeviceMetadata(context, btConfiguration, "session-id", IntegrationType.CUSTOM)
        assertTrue(metadata.isSimulator)
    }

    @Test
    @Throws(JSONException::class)
    fun getDeviceMetadata_whenBuildFingerpintContainsUnknown_returnsTrueForIsSimulator() {
        ReflectionHelpers.setStaticField(Build::class.java, "FINGERPRINT", "unknown")
        val metadata =
            sut.getDeviceMetadata(context, btConfiguration, "session-id", IntegrationType.CUSTOM)
        assertTrue(metadata.isSimulator)
    }

    @Test
    @Throws(JSONException::class)
    fun getDeviceMetadata_whenHardwareContainsGoldfish_returnsTrueForIsSimulator() {
        ReflectionHelpers.setStaticField(Build::class.java, "HARDWARE", "goldfish")
        val metadata =
            sut.getDeviceMetadata(context, btConfiguration, "session-id", IntegrationType.CUSTOM)
        assertTrue(metadata.isSimulator)
    }

    @Test
    @Throws(JSONException::class)
    fun getDeviceMetadata_whenHardwareContainsRanchu_returnsTrueForIsSimulator() {
        ReflectionHelpers.setStaticField(Build::class.java, "HARDWARE", "ranchu")
        val metadata =
            sut.getDeviceMetadata(context, btConfiguration, "session-id", IntegrationType.CUSTOM)
        assertTrue(metadata.isSimulator)
    }

    @Test
    @Throws(JSONException::class)
    fun getDeviceMetadata_whenHardwareModelGoogleSDK_returnsTrueForIsSimulator() {
        ReflectionHelpers.setStaticField(Build::class.java, "MODEL", "google_sdk")
        val metadata =
            sut.getDeviceMetadata(context, btConfiguration, "session-id", IntegrationType.CUSTOM)
        assertTrue(metadata.isSimulator)
    }

    @Test
    @Throws(JSONException::class)
    fun getDeviceMetadata_whenHardwareModelEmulator_returnsTrueForIsSimulator() {
        ReflectionHelpers.setStaticField(Build::class.java, "MODEL", "Emulator")
        val metadata =
            sut.getDeviceMetadata(context, btConfiguration, "session-id", IntegrationType.CUSTOM)
        assertTrue(metadata.isSimulator)
    }

    @Test
    @Throws(JSONException::class)
    fun getDeviceMetadata_whenHardwareModelAndroidSDKBuiltForX86_returnsTrueForIsSimulator() {
        ReflectionHelpers.setStaticField(Build::class.java, "MODEL", "Android SDK built for x86")
        val metadata =
            sut.getDeviceMetadata(context, btConfiguration, "session-id", IntegrationType.CUSTOM)
        assertTrue(metadata.isSimulator)
    }

    @Test
    @Throws(JSONException::class)
    fun getDeviceMetadata_whenBuildManufacturerIsGenymotion_returnsTrueForIsSimulator() {
        ReflectionHelpers.setStaticField(Build::class.java, "MANUFACTURER", "Genymotion")
        val metadata =
            sut.getDeviceMetadata(context, btConfiguration, "session-id", IntegrationType.CUSTOM)
        assertTrue(metadata.isSimulator)
    }

    @Test
    @Throws(JSONException::class)
    fun getDeviceMetadata_whenBuildProductIsSdkGoogle_returnsTrueForIsSimulator() {
        ReflectionHelpers.setStaticField(Build::class.java, "PRODUCT", "sdk_google")
        val metadata =
            sut.getDeviceMetadata(context, btConfiguration, "session-id", IntegrationType.CUSTOM)
        assertTrue(metadata.isSimulator)
    }

    @Test
    @Throws(JSONException::class)
    fun getDeviceMetadata_whenBuildProductIsGoogleSdk_returnsTrueForIsSimulator() {
        ReflectionHelpers.setStaticField(Build::class.java, "PRODUCT", "google_sdk")
        val metadata =
            sut.getDeviceMetadata(context, btConfiguration, "session-id", IntegrationType.CUSTOM)
        assertTrue(metadata.isSimulator)
    }

    @Test
    @Throws(JSONException::class)
    fun getDeviceMetadata_whenBuildProductIsSdk_returnsTrueForIsSimulator() {
        ReflectionHelpers.setStaticField(Build::class.java, "PRODUCT", "sdk")
        val metadata =
            sut.getDeviceMetadata(context, btConfiguration, "session-id", IntegrationType.CUSTOM)
        assertTrue(metadata.isSimulator)
    }

    @Test
    @Throws(JSONException::class)
    fun getDeviceMetadata_whenBuildProductIssdkx86_returnsTrueForIsSimulator() {
        ReflectionHelpers.setStaticField(Build::class.java, "PRODUCT", "sdk_x86")
        val metadata =
            sut.getDeviceMetadata(context, btConfiguration, "session-id", IntegrationType.CUSTOM)
        assertTrue(metadata.isSimulator)
    }

    @Test
    @Throws(JSONException::class)
    fun getDeviceMetadata_whenBuildProductIsVbox86p_returnsTrueForIsSimulator() {
        ReflectionHelpers.setStaticField(Build::class.java, "PRODUCT", "vbox86p")
        val metadata =
            sut.getDeviceMetadata(context, btConfiguration, "session-id", IntegrationType.CUSTOM)
        assertTrue(metadata.isSimulator)
    }

    @Test
    @Throws(JSONException::class)
    fun getDeviceMetadata_whenBuildProductIsEmulator_returnsTrueForIsSimulator() {
        ReflectionHelpers.setStaticField(Build::class.java, "PRODUCT", "emulator")
        val metadata =
            sut.getDeviceMetadata(context, btConfiguration, "session-id", IntegrationType.CUSTOM)
        assertTrue(metadata.isSimulator)
    }

    @Test
    @Throws(JSONException::class)
    fun getDeviceMetadata_whenBuildProductIsSimulator_returnsTrueForIsSimulator() {
        ReflectionHelpers.setStaticField(Build::class.java, "PRODUCT", "simulator")
        val metadata =
            sut.getDeviceMetadata(context, btConfiguration, "session-id", IntegrationType.CUSTOM)
        assertTrue(metadata.isSimulator)
    }

    @Test
    @Throws(JSONException::class)
    fun getDeviceMetadata_forwardsSessionId() {
        val metadata =
            sut.getDeviceMetadata(context, btConfiguration, "session-id", IntegrationType.CUSTOM)
        assertEquals("session-id", metadata.sessionId)
    }

    @Test
    @Throws(JSONException::class)
    fun getDeviceMetadata_forwardsIntegrationType() {
        val metadata =
            sut.getDeviceMetadata(context, btConfiguration, "session-id", IntegrationType.CUSTOM)
        assertEquals(IntegrationType.CUSTOM, metadata.integrationType)
    }

    @Test
    @Throws(JSONException::class, PackageManager.NameNotFoundException::class)
    fun getDeviceMetadata_returnsAppVersion() {
        val packageInfo = PackageInfo()
        packageInfo.versionName = "AppVersion"
        every { packageManager.getPackageInfo("com.sample.app", 0) } returns packageInfo
        val metadata =
            sut.getDeviceMetadata(context, btConfiguration, "session-id", IntegrationType.CUSTOM)
        assertEquals("AppVersion", metadata.merchantAppVersion)
    }

    @Test
    @Throws(JSONException::class, PackageManager.NameNotFoundException::class)
    fun getDeviceMetadata_whenAppVersionUnavailable_returnsVersionUnknownByDefault() {
        every {
            packageManager.getPackageInfo("com.sample.app", 0)
        } throws PackageManager.NameNotFoundException()
        val metadata =
            sut.getDeviceMetadata(context, btConfiguration, "session-id", IntegrationType.CUSTOM)
        assertEquals("VersionUnknown", metadata.merchantAppVersion)
    }

    @Test
    @Throws(JSONException::class)
    fun getDeviceMetadata_forwardsDropInVersionFromClassHelper() {
        mockkObject(DeviceInspector) {
            every { DeviceInspector.getDropInVersion() } returns "fake-drop-in-version"
            val metadata = sut.getDeviceMetadata(
                context,
                btConfiguration,
                "session-id",
                IntegrationType.CUSTOM
            )
            assertEquals("fake-drop-in-version", metadata.dropInSDKVersion)
        }
    }

    @Test
    @Throws(JSONException::class)
    fun getSDKEnvironment_forwardsEnvironmentFromConfig() {
        val metadata =
            sut.getDeviceMetadata(context, btConfiguration, "session-id", IntegrationType.CUSTOM)
        assertEquals("test", metadata.environment)
    }

    @Test
    @Throws(JSONException::class)
    fun getMerchantID_forwardsMerchantIdFromConfig() {
        val metadata =
            sut.getDeviceMetadata(context, btConfiguration, "session-id", IntegrationType.CUSTOM)
        assertEquals("integration_merchant_id", metadata.merchantId)
    }

    @Test
    fun isPayPalInstalled_forwardsIsPayPalInstalledResultFromAppHelper() {
        every { appHelper.isAppInstalled(context, "com.paypal.android.p2pmobile") } returns true
        assertTrue(sut.isPayPalInstalled())
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
