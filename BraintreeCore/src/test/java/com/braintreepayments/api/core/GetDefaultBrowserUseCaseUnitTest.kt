package com.braintreepayments.api.core

import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
class GetDefaultBrowserUseCaseUnitTest {

    private val merchantRepository: MerchantRepository = mockk(relaxed = true)
    private val packageManager: PackageManager = mockk(relaxed = true)
    private val appLinkReturnUri = Uri.parse("https://example.com")

    lateinit var subject: GetDefaultBrowserUseCase

    @Before
    fun setUp() {
        every { merchantRepository.appLinkReturnUri } returns appLinkReturnUri
        subject = GetDefaultBrowserUseCase(merchantRepository)
    }

    @Test
    fun `when invoke is called and default browser is found, returns browser package name`() {
        val resolveInfo = ResolveInfo()
        val activityInfo = ActivityInfo()
        activityInfo.packageName = "com.android.chrome"
        resolveInfo.activityInfo = activityInfo

        val intentSlot = slot<Intent>()
        every { packageManager.resolveActivity(capture(intentSlot), PackageManager.MATCH_DEFAULT_ONLY) } returns resolveInfo

        val result = subject(packageManager)

        assertEquals("com.android.chrome", result)
        assertEquals(Intent.ACTION_VIEW, intentSlot.captured.action)
        assertEquals(appLinkReturnUri, intentSlot.captured.data)
        assertEquals(true, intentSlot.captured.categories?.contains(Intent.CATEGORY_BROWSABLE))
    }

    @Test
    fun `when invoke is called and no default browser is found, returns null`() {
        every { packageManager.resolveActivity(any(), PackageManager.MATCH_DEFAULT_ONLY) } returns null

        val result = subject(packageManager)

        assertNull(result)
    }

    @Test
    fun `when invoke is called and resolveInfo activityInfo is null, returns null`() {
        val resolveInfo = mockk<ResolveInfo>(relaxed = true)
        resolveInfo.activityInfo = null
        every { packageManager.resolveActivity(any(), PackageManager.MATCH_DEFAULT_ONLY) } returns resolveInfo

        val result = subject(packageManager)

        assertNull(result)
    }

    @Test
    fun `when invoke is called with different browser packages, returns correct package name`() {
        val browserPackages = listOf(
            "com.brave.browser",
            "org.mozilla.firefox",
            "com.microsoft.emmx",
            "com.opera.browser"
        )

        browserPackages.forEach { packageName ->
            val resolveInfo = ResolveInfo()
            val activityInfo = ActivityInfo()
            activityInfo.packageName = packageName
            resolveInfo.activityInfo = activityInfo
            every { packageManager.resolveActivity(any(), PackageManager.MATCH_DEFAULT_ONLY) } returns resolveInfo

            val result = subject(packageManager)

            assertEquals(packageName, result)
        }
    }
}
