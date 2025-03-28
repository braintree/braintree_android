package com.braintreepayments.api.core

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
    private val getReturnLinkTypeUseCase: GetReturnLinkTypeUseCase = mockk(relaxed = true)
    private val appLinkReturnUri = Uri.parse("https://example.com")
    private val deepLinkFallbackUrlScheme = "com.braintreepayments.demo"

    lateinit var subject: GetReturnLinkUseCase

    @Before
    fun setUp() {
        every { merchantRepository.appLinkReturnUri } returns appLinkReturnUri
        every { merchantRepository.deepLinkFallbackUrlScheme } returns deepLinkFallbackUrlScheme

        subject = GetReturnLinkUseCase(merchantRepository, getReturnLinkTypeUseCase)
    }

    @Test
    fun `when invoke is called and app link is available, AppLink is returned`() {
        every { getReturnLinkTypeUseCase() } returns GetReturnLinkTypeUseCase.ReturnLinkTypeResult.APP_LINK

        val result = subject()

        assertEquals(GetReturnLinkUseCase.ReturnLinkResult.AppLink(appLinkReturnUri), result)
    }

    @Test
    fun `when invoke is called and app link is not available, DeepLink is returned`() {
        every { getReturnLinkTypeUseCase() } returns GetReturnLinkTypeUseCase.ReturnLinkTypeResult.DEEP_LINK

        val result = subject()

        assertEquals(GetReturnLinkUseCase.ReturnLinkResult.DeepLink(deepLinkFallbackUrlScheme), result)
    }

    @Test
    fun `when invoke is called and deep link is available but null, Failure is returned`() {
        every { getReturnLinkTypeUseCase() } returns GetReturnLinkTypeUseCase.ReturnLinkTypeResult.DEEP_LINK
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
        every { getReturnLinkTypeUseCase() } returns GetReturnLinkTypeUseCase.ReturnLinkTypeResult.APP_LINK
        every { merchantRepository.appLinkReturnUri } returns null

        val result = subject()

        assertTrue { result is GetReturnLinkUseCase.ReturnLinkResult.Failure }
        assertEquals(
            "App Link Return Uri is null",
            (result as GetReturnLinkUseCase.ReturnLinkResult.Failure).exception.message
        )
    }
}
