package com.braintreepayments.api

import androidx.fragment.app.FragmentActivity
import com.braintreepayments.api.TestConfigurationBuilder.TestGooglePayConfigurationBuilder
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GooglePayCapabilitiesUnitTest {

    private val configWithGooglePayEnabled: Configuration = TestConfigurationBuilder()
            .googlePay(TestGooglePayConfigurationBuilder()
                    .enabled(true))
            .buildConfiguration()

    private val configWithGooglePayDisabled: Configuration = TestConfigurationBuilder()
            .googlePay(TestGooglePayConfigurationBuilder()
                    .enabled(false))
            .buildConfiguration()

    lateinit var activity: FragmentActivity
    lateinit var googleApiAvailability: GoogleApiAvailability

    @Before
    fun beforeEach() {
        activity = mockk(relaxed = true)
        googleApiAvailability = mockk(relaxed = true)

        mockkStatic(GoogleApiAvailability::class)
        every { GoogleApiAvailability.getInstance() } returns googleApiAvailability
    }

    @Test
    fun isGooglePayEnabled_whenConfigurationEnabledAndApiAvailable_returnsTrue() {
        every {
            googleApiAvailability.isGooglePlayServicesAvailable(any())
        } returns ConnectionResult.SUCCESS

        assertTrue(GooglePayCapabilities.isGooglePayEnabled(activity, configWithGooglePayEnabled))
    }

    @Test
    fun isGooglePayEnabled_whenConfigurationNotEnabled_returnsFalse() {
        every {
            googleApiAvailability.isGooglePlayServicesAvailable(any())
        } returns ConnectionResult.SUCCESS

        assertFalse(GooglePayCapabilities.isGooglePayEnabled(activity, configWithGooglePayDisabled))
    }

    @Test
    fun isGooglePayEnabled_whenApiNotAvailable_returnsFalse() {
        every {
            googleApiAvailability.isGooglePlayServicesAvailable(any())
        } returns ConnectionResult.NETWORK_ERROR

        assertFalse(GooglePayCapabilities.isGooglePayEnabled(activity, configWithGooglePayEnabled))
    }
}
