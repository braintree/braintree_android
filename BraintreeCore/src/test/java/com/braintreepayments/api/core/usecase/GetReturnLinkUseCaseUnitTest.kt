package com.braintreepayments.api.core.usecase

import android.net.Uri
import com.braintreepayments.api.core.MerchantRepository
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
    private val getReturnLinkTypeUseCase: GetReturnLinkTypeUseCase = mockk(relaxed = true)
    private val getDefaultAppUseCase: GetDefaultAppUseCase = mockk(relaxed = true)
    private val getAppLinksCompatibleBrowserUseCase: GetAppLinksCompatibleBrowserUseCase = mockk(relaxed = true)
    private val appLinkReturnUri = Uri.parse("https://example.com")
    private val deepLinkFallbackUrlScheme = "com.braintreepayments.demo"

    lateinit var sut: GetReturnLinkUseCase

    @Before
    fun setUp() {
        every { merchantRepository.appLinkReturnUri } returns appLinkReturnUri
        every { merchantRepository.deepLinkFallbackUrlScheme } returns deepLinkFallbackUrlScheme

        sut = GetReturnLinkUseCase(
            merchantRepository,
            getDefaultAppUseCase,
            getAppLinksCompatibleBrowserUseCase,
            getReturnLinkTypeUseCase
        )
    }

    @Test
    fun `when invoke is called and app link is available, AppLink is returned`() {
        every { getReturnLinkTypeUseCase() } returns GetReturnLinkTypeUseCase.ReturnLinkTypeResult.APP_LINK

        val result = sut()

        assertEquals(GetReturnLinkUseCase.ReturnLinkResult.AppLink(appLinkReturnUri), result)
    }

    @Test
    fun `when invoke is called and app link is not available, DeepLink is returned`() {
        every { getReturnLinkTypeUseCase() } returns GetReturnLinkTypeUseCase.ReturnLinkTypeResult.DEEP_LINK

        val result = sut()

        assertEquals(GetReturnLinkUseCase.ReturnLinkResult.DeepLink(deepLinkFallbackUrlScheme), result)
    }

    @Test
    fun `when invoke is called and deep link is available but null, Failure is returned`() {
        every { getReturnLinkTypeUseCase() } returns GetReturnLinkTypeUseCase.ReturnLinkTypeResult.DEEP_LINK
        every { merchantRepository.deepLinkFallbackUrlScheme } returns null

        val result = sut()

        assertTrue(result is GetReturnLinkUseCase.ReturnLinkResult.Failure)
        assertEquals(
            "Deep Link fallback return url is null",
            result.exception.message
        )
    }

    @Test
    fun `when invoke is called and app link is available but null, Failure is returned`() {
        every { getReturnLinkTypeUseCase() } returns GetReturnLinkTypeUseCase.ReturnLinkTypeResult.APP_LINK
        every { merchantRepository.appLinkReturnUri } returns null

        val result = sut()

        assertTrue(result is GetReturnLinkUseCase.ReturnLinkResult.Failure)
        assertEquals(
            "App Link Return Uri is null",
            result.exception.message
        )
    }
}
