package com.braintreepayments.api.core.usecase

import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GetDefaultAppUseCaseTest {
    private lateinit var sut: GetDefaultAppUseCase
    private val packageManager: PackageManager = mockk<PackageManager>()
    private val appLinkReturnUri = Uri.parse("https://example.com")

    @Before
    fun setUp() {
        sut = GetDefaultAppUseCase(packageManager)
    }

    @Test
    fun `when invoke is called and default browser is found, returns browser package name`() {
        val resolveInfo = ResolveInfo()
        val activityInfo = ActivityInfo()
        activityInfo.packageName = "com.android.chrome"
        resolveInfo.activityInfo = activityInfo

        val intentSlot = slot<Intent>()
        every {
            packageManager.resolveActivity(
                capture(intentSlot),
                PackageManager.MATCH_DEFAULT_ONLY
            )
        } returns resolveInfo

        val result = sut(appLinkReturnUri)

        Assert.assertEquals("com.android.chrome", result)
        Assert.assertEquals(Intent.ACTION_VIEW, intentSlot.captured.action)
        Assert.assertEquals(appLinkReturnUri, intentSlot.captured.data)
        Assert.assertEquals(true, intentSlot.captured.categories?.contains(Intent.CATEGORY_BROWSABLE))
    }

    @Test
    fun `when invoke is called and no default browser is found, returns null`() {
        every { packageManager.resolveActivity(any(), PackageManager.MATCH_DEFAULT_ONLY) } returns null

        val result = sut(ArgumentMatchers.any())

        Assert.assertNull(result)
    }

    @Test
    fun `when invoke is called and resolveInfo activityInfo is null, returns null`() {
        val resolveInfo = mockk<ResolveInfo>(relaxed = true)
        resolveInfo.activityInfo = null
        every { packageManager.resolveActivity(any(), PackageManager.MATCH_DEFAULT_ONLY) } returns resolveInfo

        val result = sut(ArgumentMatchers.any())

        Assert.assertNull(result)
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

            val result = sut(ArgumentMatchers.any())

            Assert.assertEquals(packageName, result)
        }
    }
}