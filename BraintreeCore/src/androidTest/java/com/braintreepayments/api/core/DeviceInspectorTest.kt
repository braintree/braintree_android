package com.braintreepayments.api.core

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.braintreepayments.api.testutils.Fixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4ClassRunner::class)
class DeviceInspectorTest {

    private lateinit var context: Context
    private lateinit var sut: DeviceInspector
    private lateinit var configuration: Configuration

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        sut = DeviceInspector(context)
        configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ENVIRONMENT)
    }

    @Test(timeout = 1000)
    fun getDeviceMetadata_returnsNonNullMetadata() {
        val metadata = sut.getDeviceMetadata(context, configuration, "test-session", IntegrationType.CUSTOM)
        assertNotNull(metadata)
    }

    @Test(timeout = 1000)
    fun getDeviceMetadata_returnsCorrectPlatform() {
        val metadata = sut.getDeviceMetadata(context, configuration, "test-session", IntegrationType.CUSTOM)
        assertEquals("Android", metadata.platform)
    }

    @Test(timeout = 1000)
    fun getDeviceMetadata_returnsCorrectEventSource() {
        val metadata = sut.getDeviceMetadata(context, configuration, "test-session", IntegrationType.CUSTOM)
        assertEquals("mobile-native", metadata.eventSource)
    }

    @Test(timeout = 1000)
    fun getDeviceMetadata_returnsCorrectComponent() {
        val metadata = sut.getDeviceMetadata(context, configuration, "test-session", IntegrationType.CUSTOM)
        assertEquals("braintreeclientsdk", metadata.component)
    }

    @Test(timeout = 1000)
    fun getDeviceMetadata_returnsValidClientOs() {
        val metadata = sut.getDeviceMetadata(context, configuration, "test-session", IntegrationType.CUSTOM)
        val expected = "Android API ${Build.VERSION.SDK_INT}"
        assertEquals(expected, metadata.clientOs)
    }

    @Test(timeout = 1000)
    fun getDeviceMetadata_returnsNonEmptyClientSDKVersion() {
        val metadata = sut.getDeviceMetadata(context, configuration, "test-session", IntegrationType.CUSTOM)
        requireNotNull(metadata.clientSDKVersion)
        assertTrue(metadata.clientSDKVersion.isNotEmpty())
    }

    @Test(timeout = 1000)
    fun getDeviceMetadata_returnsCorrectAppId() {
        val metadata = sut.getDeviceMetadata(context, configuration, "test-session", IntegrationType.CUSTOM)
        assertEquals(context.packageName, metadata.appId)
    }

    @Test(timeout = 1000)
    fun getDeviceMetadata_returnsNonNullAppName() {
        val metadata = sut.getDeviceMetadata(context, configuration, "test-session", IntegrationType.CUSTOM)
        assertNotNull(metadata.appName)
        assertTrue(metadata.appName != "ApplicationNameUnknown")
    }

    @Test(timeout = 1000)
    fun getDeviceMetadata_returnsNonEmptyDeviceManufacturer() {
        val metadata = sut.getDeviceMetadata(context, configuration, "test-session", IntegrationType.CUSTOM)
        requireNotNull(metadata.deviceManufacturer)
        assertTrue(metadata.deviceManufacturer.isNotEmpty())
    }

    @Test(timeout = 1000)
    fun getDeviceMetadata_returnsNonEmptyDeviceModel() {
        val metadata = sut.getDeviceMetadata(context, configuration, "test-session", IntegrationType.CUSTOM)
        requireNotNull(metadata.deviceModel)
        assertTrue(metadata.deviceModel.isNotEmpty())
    }

    @Test(timeout = 1000)
    fun getDeviceMetadata_detectsEmulatorOnCIEmulator() {
        val metadata = sut.getDeviceMetadata(context, configuration, "test-session", IntegrationType.CUSTOM)
        assertTrue("Expected emulator detection to return true on CI emulator", metadata.isSimulator)
    }

    @Test(timeout = 1000)
    fun getDeviceMetadata_returnsCorrectEnvironmentFromConfiguration() {
        val metadata = sut.getDeviceMetadata(context, configuration, "test-session", IntegrationType.CUSTOM)
        assertEquals("test", metadata.environment)
    }

    @Test(timeout = 1000)
    fun getDeviceMetadata_returnsCorrectMerchantId() {
        val metadata = sut.getDeviceMetadata(context, configuration, "test-session", IntegrationType.CUSTOM)
        assertEquals("integration_merchant_id", metadata.merchantId)
    }

    @Test(timeout = 1000)
    fun getDeviceMetadata_forwardsSessionId() {
        val sessionId = "unique-session-id-12345"
        val metadata = sut.getDeviceMetadata(context, configuration, sessionId, IntegrationType.CUSTOM)
        assertEquals(sessionId, metadata.sessionId)
    }

    @Test(timeout = 1000)
    fun getDeviceMetadata_forwardsIntegrationType() {
        val metadata = sut.getDeviceMetadata(context, configuration, "test-session", IntegrationType.CUSTOM)
        assertEquals(IntegrationType.CUSTOM, metadata.integrationType)
    }

    @Test(timeout = 1000)
    fun isPayPalInstalled_returnsFalseOnEmulator() {
        assertFalse(sut.isPayPalInstalled())
    }

    @Test(timeout = 1000)
    fun isVenmoInstalled_returnsFalseOnEmulator() {
        assertFalse(sut.isVenmoInstalled(context))
    }

    @Test(timeout = 1000)
    fun isVenmoAppSwitchAvailable_returnsFalseOnEmulator() {
        assertFalse(sut.isVenmoAppSwitchAvailable(context))
    }
}
