package com.braintreepayments.api.paypal

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import com.braintreepayments.api.core.DeviceInspector
import com.braintreepayments.api.core.MerchantRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class ResolvePayPalUseCaseUnitTest {

    private val merchantRepository: MerchantRepository = mockk(relaxed = true)
    private val context: Context = mockk(relaxed = true)
    private val packageManager: PackageManager = mockk(relaxed = true)
    private val resolveInfo = ResolveInfo()
    private val activityInfo = ActivityInfo()

    private val paypalUri = Uri.parse("https://paypal.com/checkout")
    private val paypalPackageName = DeviceInspector.PAYPAL_APP_PACKAGE
    private val chromePackageName = "com.android.chrome"

    private lateinit var subject: ResolvePayPalUseCase

    @Before
    fun setUp() {
        every { context.packageManager } returns packageManager
        every { merchantRepository.applicationContext } returns context
        resolveInfo.activityInfo = activityInfo

        subject = ResolvePayPalUseCase(merchantRepository)
    }

    @Test
    fun `when invoke is called and intent resolves with target app, returns true`() {
        activityInfo.packageName = paypalPackageName
        every {
            packageManager.resolveActivity(any(), PackageManager.MATCH_DEFAULT_ONLY)
        } returns resolveInfo

        val result = subject(paypalUri)

        assertTrue(result)
    }

    @Test
    fun `when invoke is called and intent does not resolve with target app, returns false`() {
        activityInfo.packageName = chromePackageName

        every {
            packageManager.resolveActivity(any(), PackageManager.MATCH_DEFAULT_ONLY)
        } returns resolveInfo

        val result = subject(paypalUri)

        assertFalse(result)
    }

    @Test
    fun `when invoke is called and no activity resolves the intent, returns false`() {
        every {
            packageManager.resolveActivity(any(), PackageManager.MATCH_DEFAULT_ONLY)
        } returns null

        val result = subject(paypalUri)

        assertFalse(result)
    }

    @Test
    fun `when invoke is called and activity info is null, returns false`() {
        resolveInfo.activityInfo = null

        every {
            packageManager.resolveActivity(any(), PackageManager.MATCH_DEFAULT_ONLY)
        } returns resolveInfo

        val result = subject(paypalUri)

        assertFalse(result)
    }
}
