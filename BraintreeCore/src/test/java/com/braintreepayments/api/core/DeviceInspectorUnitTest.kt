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
    fun `getDeviceMetadata returns metadata with platform set to Android`() {
        val metadata =
            sut.getDeviceMetadata(context, btConfiguration, "session-id", IntegrationType.CUSTOM)
        assertEquals("Android", metadata.platform)
    }

    @Test
    @Throws(JSONException::class)
    fun `getDeviceMetadata returns metadata with eventSource set to mobile-native`() {
        val metadata =
            sut.getDeviceMetadata(context, btConfiguration, "session-id", IntegrationType.CUSTOM)
        assertEquals("mobile-native", metadata.eventSource)
    }

    @Test
    @Throws(JSONException::class)
    fun `getDeviceMetadata returns metadata with component set to braintreeclientsdk`() {
        val metadata =
            sut.getDeviceMetadata(context, btConfiguration, "session-id", IntegrationType.CUSTOM)
        assertEquals("braintreeclientsdk", metadata.component)
    }

    @Test
    @Throws(JSONException::class)
    fun `getDeviceMetadata returns metadata with clientOs based on Build VERSION SDK_INT`() {
        ReflectionHelpers.setStaticField(VERSION::class.java, "SDK_INT", 123)
        val metadata =
            sut.getDeviceMetadata(context, btConfiguration, "session-id", IntegrationType.CUSTOM)
        assertEquals("Android API 123", metadata.clientOs)
    }

    @Test
    @Throws(JSONException::class)
    fun `getDeviceMetadata returns metadata with clientSDKVersion set to BuildConfig VERSION_NAME`() {
        val metadata =
            sut.getDeviceMetadata(context, btConfiguration, "session-id", IntegrationType.CUSTOM)
        assertEquals(BuildConfig.VERSION_NAME, metadata.clientSDKVersion)
    }

    @Test
    @Throws(JSONException::class)
    fun `getDeviceMetadata returns metadata with appId set to context package name`() {
        val metadata =
            sut.getDeviceMetadata(context, btConfiguration, "session-id", IntegrationType.CUSTOM)
        assertEquals("com.sample.app", metadata.appId)
    }

    @Test
    @Throws(PackageManager.NameNotFoundException::class, JSONException::class)
    fun `when packageManager throws NameNotFoundException for application info, getDeviceMetadata returns metadata with appName set to ApplicationNameUnknown`() {
        every {
            packageManager.getApplicationInfo("com.sample.app", 0)
        } throws PackageManager.NameNotFoundException()
        val metadata =
            sut.getDeviceMetadata(context, btConfiguration, "session-id", IntegrationType.CUSTOM)
        assertEquals("ApplicationNameUnknown", metadata.appName)
    }

    @Test
    @Throws(PackageManager.NameNotFoundException::class, JSONException::class)
    fun `getDeviceMetadata returns metadata with appName from packageManager application label`() {
        val applicationInfo = ApplicationInfo()
        every { packageManager.getApplicationInfo("com.sample.app", 0) } returns applicationInfo
        every { packageManager.getApplicationLabel(applicationInfo) } returns "SampleAppName"
        val metadata =
            sut.getDeviceMetadata(context, btConfiguration, "session-id", IntegrationType.CUSTOM)
        assertEquals("SampleAppName", metadata.appName)
    }

    @Test
    @Throws(JSONException::class)
    fun `getDeviceMetadata returns metadata with deviceManufacturer from Build MANUFACTURER`() {
        ReflectionHelpers.setStaticField(Build::class.java, "MANUFACTURER", "device-manufacturer")
        val metadata =
            sut.getDeviceMetadata(context, btConfiguration, "session-id", IntegrationType.CUSTOM)
        assertEquals("device-manufacturer", metadata.deviceManufacturer)
    }

    @Test
    @Throws(JSONException::class)
    fun `getDeviceMetadata returns metadata with deviceModel from Build MODEL`() {
        ReflectionHelpers.setStaticField(Build::class.java, "MODEL", "device-model")
        val metadata =
            sut.getDeviceMetadata(context, btConfiguration, "session-id", IntegrationType.CUSTOM)
        assertEquals("device-model", metadata.deviceModel)
    }

    @Test
    @Throws(JSONException::class)
    fun `when Build PRODUCT, MANUFACTURER and FINGERPRINT are all valid device values, getDeviceMetadata returns metadata with isSimulator set to false`() {
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
    fun `when Build FINGERPRINT contains generic, getDeviceMetadata returns metadata with isSimulator set to true`() {
        ReflectionHelpers.setStaticField(Build::class.java, "FINGERPRINT", "generic")
        val metadata =
            sut.getDeviceMetadata(context, btConfiguration, "session-id", IntegrationType.CUSTOM)
        assertTrue(metadata.isSimulator)
    }

    @Test
    @Throws(JSONException::class)
    fun `when Build FINGERPRINT contains unknown, getDeviceMetadata returns metadata with isSimulator set to true`() {
        ReflectionHelpers.setStaticField(Build::class.java, "FINGERPRINT", "unknown")
        val metadata =
            sut.getDeviceMetadata(context, btConfiguration, "session-id", IntegrationType.CUSTOM)
        assertTrue(metadata.isSimulator)
    }

    @Test
    @Throws(JSONException::class)
    fun `when Build HARDWARE is goldfish, getDeviceMetadata returns metadata with isSimulator set to true`() {
        ReflectionHelpers.setStaticField(Build::class.java, "HARDWARE", "goldfish")
        val metadata =
            sut.getDeviceMetadata(context, btConfiguration, "session-id", IntegrationType.CUSTOM)
        assertTrue(metadata.isSimulator)
    }

    @Test
    @Throws(JSONException::class)
    fun `when Build HARDWARE is ranchu, getDeviceMetadata returns metadata with isSimulator set to true`() {
        ReflectionHelpers.setStaticField(Build::class.java, "HARDWARE", "ranchu")
        val metadata =
            sut.getDeviceMetadata(context, btConfiguration, "session-id", IntegrationType.CUSTOM)
        assertTrue(metadata.isSimulator)
    }

    @Test
    @Throws(JSONException::class)
    fun `when Build MODEL is google_sdk, getDeviceMetadata returns metadata with isSimulator set to true`() {
        ReflectionHelpers.setStaticField(Build::class.java, "MODEL", "google_sdk")
        val metadata =
            sut.getDeviceMetadata(context, btConfiguration, "session-id", IntegrationType.CUSTOM)
        assertTrue(metadata.isSimulator)
    }

    @Test
    @Throws(JSONException::class)
    fun `when Build MODEL is Emulator, getDeviceMetadata returns metadata with isSimulator set to true`() {
        ReflectionHelpers.setStaticField(Build::class.java, "MODEL", "Emulator")
        val metadata =
            sut.getDeviceMetadata(context, btConfiguration, "session-id", IntegrationType.CUSTOM)
        assertTrue(metadata.isSimulator)
    }

    @Test
    @Throws(JSONException::class)
    fun `when Build MODEL is Android SDK built for x86, getDeviceMetadata returns metadata with isSimulator set to true`() {
        ReflectionHelpers.setStaticField(Build::class.java, "MODEL", "Android SDK built for x86")
        val metadata =
            sut.getDeviceMetadata(context, btConfiguration, "session-id", IntegrationType.CUSTOM)
        assertTrue(metadata.isSimulator)
    }

    @Test
    @Throws(JSONException::class)
    fun `when Build MANUFACTURER is Genymotion, getDeviceMetadata returns metadata with isSimulator set to true`() {
        ReflectionHelpers.setStaticField(Build::class.java, "MANUFACTURER", "Genymotion")
        val metadata =
            sut.getDeviceMetadata(context, btConfiguration, "session-id", IntegrationType.CUSTOM)
        assertTrue(metadata.isSimulator)
    }

    @Test
    @Throws(JSONException::class)
    fun `when Build PRODUCT is sdk_google, getDeviceMetadata returns metadata with isSimulator set to true`() {
        ReflectionHelpers.setStaticField(Build::class.java, "PRODUCT", "sdk_google")
        val metadata =
            sut.getDeviceMetadata(context, btConfiguration, "session-id", IntegrationType.CUSTOM)
        assertTrue(metadata.isSimulator)
    }

    @Test
    @Throws(JSONException::class)
    fun `when Build PRODUCT is google_sdk, getDeviceMetadata returns metadata with isSimulator set to true`() {
        ReflectionHelpers.setStaticField(Build::class.java, "PRODUCT", "google_sdk")
        val metadata =
            sut.getDeviceMetadata(context, btConfiguration, "session-id", IntegrationType.CUSTOM)
        assertTrue(metadata.isSimulator)
    }

    @Test
    @Throws(JSONException::class)
    fun `when Build PRODUCT is sdk, getDeviceMetadata returns metadata with isSimulator set to true`() {
        ReflectionHelpers.setStaticField(Build::class.java, "PRODUCT", "sdk")
        val metadata =
            sut.getDeviceMetadata(context, btConfiguration, "session-id", IntegrationType.CUSTOM)
        assertTrue(metadata.isSimulator)
    }

    @Test
    @Throws(JSONException::class)
    fun `when Build PRODUCT is sdk_x86, getDeviceMetadata returns metadata with isSimulator set to true`() {
        ReflectionHelpers.setStaticField(Build::class.java, "PRODUCT", "sdk_x86")
        val metadata =
            sut.getDeviceMetadata(context, btConfiguration, "session-id", IntegrationType.CUSTOM)
        assertTrue(metadata.isSimulator)
    }

    @Test
    @Throws(JSONException::class)
    fun `when Build PRODUCT is vbox86p, getDeviceMetadata returns metadata with isSimulator set to true`() {
        ReflectionHelpers.setStaticField(Build::class.java, "PRODUCT", "vbox86p")
        val metadata =
            sut.getDeviceMetadata(context, btConfiguration, "session-id", IntegrationType.CUSTOM)
        assertTrue(metadata.isSimulator)
    }

    @Test
    @Throws(JSONException::class)
    fun `when Build PRODUCT is emulator, getDeviceMetadata returns metadata with isSimulator set to true`() {
        ReflectionHelpers.setStaticField(Build::class.java, "PRODUCT", "emulator")
        val metadata =
            sut.getDeviceMetadata(context, btConfiguration, "session-id", IntegrationType.CUSTOM)
        assertTrue(metadata.isSimulator)
    }

    @Test
    @Throws(JSONException::class)
    fun `when Build PRODUCT is simulator, getDeviceMetadata returns metadata with isSimulator set to true`() {
        ReflectionHelpers.setStaticField(Build::class.java, "PRODUCT", "simulator")
        val metadata =
            sut.getDeviceMetadata(context, btConfiguration, "session-id", IntegrationType.CUSTOM)
        assertTrue(metadata.isSimulator)
    }

    @Test
    @Throws(JSONException::class)
    fun `getDeviceMetadata forwards sessionId argument to result`() {
        val metadata =
            sut.getDeviceMetadata(context, btConfiguration, "session-id", IntegrationType.CUSTOM)
        assertEquals("session-id", metadata.sessionId)
    }

    @Test
    @Throws(JSONException::class)
    fun `getDeviceMetadata forwards integrationType argument to result`() {
        val metadata =
            sut.getDeviceMetadata(context, btConfiguration, "session-id", IntegrationType.CUSTOM)
        assertEquals(IntegrationType.CUSTOM, metadata.integrationType)
    }

    @Test
    @Throws(JSONException::class, PackageManager.NameNotFoundException::class)
    fun `getDeviceMetadata returns metadata with merchantAppVersion from packageManager package info`() {
        val packageInfo = PackageInfo()
        packageInfo.versionName = "AppVersion"
        every { packageManager.getPackageInfo("com.sample.app", 0) } returns packageInfo
        val metadata =
            sut.getDeviceMetadata(context, btConfiguration, "session-id", IntegrationType.CUSTOM)
        assertEquals("AppVersion", metadata.merchantAppVersion)
    }

    @Test
    @Throws(JSONException::class, PackageManager.NameNotFoundException::class)
    fun `when packageManager throws NameNotFoundException for package info, getDeviceMetadata returns metadata with merchantAppVersion set to VersionUnknown`() {
        every {
            packageManager.getPackageInfo("com.sample.app", 0)
        } throws PackageManager.NameNotFoundException()
        val metadata =
            sut.getDeviceMetadata(context, btConfiguration, "session-id", IntegrationType.CUSTOM)
        assertEquals("VersionUnknown", metadata.merchantAppVersion)
    }

    @Test
    @Throws(JSONException::class)
    fun `getDeviceMetadata returns metadata with dropInSDKVersion from DeviceInspector getDropInVersion`() {
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
    fun `getDeviceMetadata returns metadata with environment from configuration`() {
        val metadata =
            sut.getDeviceMetadata(context, btConfiguration, "session-id", IntegrationType.CUSTOM)
        assertEquals("test", metadata.environment)
    }

    @Test
    @Throws(JSONException::class)
    fun `getDeviceMetadata returns metadata with merchantId from configuration`() {
        val metadata =
            sut.getDeviceMetadata(context, btConfiguration, "session-id", IntegrationType.CUSTOM)
        assertEquals("integration_merchant_id", metadata.merchantId)
    }

    @Test
    fun `isPayPalInstalled returns true when appHelper reports PayPal app installed`() {
        every { appHelper.isAppInstalled(context, "com.paypal.android.p2pmobile") } returns true
        assertTrue(sut.isPayPalInstalled())
    }

    @Test
    fun `isVenmoInstalled returns true when appHelper reports Venmo app installed`() {
        every { appHelper.isAppInstalled(context, "com.venmo") } returns true
        assertTrue(sut.isVenmoInstalled(context))
    }

    @Test
    fun `isVenmoAppSwitchAvailable checks appHelper with intent targeting Venmo SetupMerchantActivity`() {
        sut.isVenmoAppSwitchAvailable(context)
        val intentSlot = slot<Intent>()
        verify { appHelper.isIntentAvailable(context, capture(intentSlot)) }

        val intent = intentSlot.captured
        val component = intent.component
        assertEquals("com.venmo", component!!.packageName)
        assertEquals("com.venmo.controller.SetupMerchantActivity", component.className)
    }

    @Test
    fun `when appHelper reports Venmo intent unavailable, isVenmoAppSwitchAvailable returns false`() {
        every { appHelper.isIntentAvailable(context, ofType(Intent::class)) } returns false
        assertFalse(sut.isVenmoAppSwitchAvailable(context))
    }

    @Test
    fun `when Venmo intent is available but signature is not valid, isVenmoAppSwitchAvailable returns false`() {
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
    fun `when Venmo intent is available and signature is valid, isVenmoAppSwitchAvailable returns true`() {
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
