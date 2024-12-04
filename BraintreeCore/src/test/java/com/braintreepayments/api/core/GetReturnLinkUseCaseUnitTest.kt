package com.braintreepayments.api.core

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import android.net.Uri
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class GetReturnLinkUseCaseUnitTest {

    private val merchantRepository: MerchantRepository = mockk(relaxed = true)
    private val context: Context = mockk(relaxed = true)
    private val resolveInfo = ResolveInfo()
    private val activityInfo = ActivityInfo()
    private val contextPackageName = "context.package.name"
    private val appLinkReturnUri = Uri.parse("https://example.com")
    private val deepLinkFallbackUrlScheme = "com.braintreepayments.demo"

    lateinit var subject: GetReturnLinkUseCase

    @Before
    fun setUp() {
        every { merchantRepository.applicationContext } returns context
        every { merchantRepository.appLinkReturnUri } returns appLinkReturnUri
        every { merchantRepository.deepLinkFallbackUrlScheme } returns deepLinkFallbackUrlScheme
        every { context.packageName } returns contextPackageName
        resolveInfo.activityInfo = activityInfo
        every { context.packageManager.resolveActivity(any<Intent>(), any<Int>()) } returns resolveInfo

        subject = GetReturnLinkUseCase(merchantRepository)
    }

    @Test
    fun `when invoke is called and app link is available, APP_LINK is returned`() {
        activityInfo.packageName = "context.package.name"

        val result = subject()

        assertEquals(GetReturnLinkUseCase.ReturnLinkResult.AppLink(appLinkReturnUri), result)
    }

    @Test
    fun `when invoke is called and app link is not available, DEEP_LINK is returned`() {
        activityInfo.packageName = "different.package.name"

        val result = subject()

        assertEquals(GetReturnLinkUseCase.ReturnLinkResult.DeepLink(deepLinkFallbackUrlScheme), result)
    }

    @Test
    fun `when invoke is called and deep link is available but null, Failure is returned`() {
        activityInfo.packageName = "different.package.name"
        every { merchantRepository.deepLinkFallbackUrlScheme } returns null

        val result = subject()

        assertTrue { result is GetReturnLinkUseCase.ReturnLinkResult.Failure }
        assertEquals(
            "Deep Link fallback return url is null",
            (result as GetReturnLinkUseCase.ReturnLinkResult.Failure).exception.message
        )
    }

    @Test
    fun `when invoke is called and app link is available but null, Failure is returned`() {
        activityInfo.packageName = "context.package.name"
        every { merchantRepository.appLinkReturnUri } returns null

        val result = subject()

        assertTrue { result is GetReturnLinkUseCase.ReturnLinkResult.Failure }
        assertEquals(
            "App Link Return Uri is null",
            (result as GetReturnLinkUseCase.ReturnLinkResult.Failure).exception.message
        )
    }
}
