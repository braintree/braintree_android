package com.braintreepayments.api.core.usecase

import android.content.Context
import androidx.core.net.toUri
import com.braintreepayments.api.core.MerchantRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class GetReturnLinkTypeUseCaseUnitTest {

    lateinit var sut: GetReturnLinkTypeUseCase
    private val getAppLinksCompatibleBrowserUseCase = mockk<GetAppLinksCompatibleBrowserUseCase>()
    private val merchantRepository = mockk<MerchantRepository>()
    private val getDefaultAppUseCase = mockk<GetDefaultAppUseCase>()
    private val packageName = "some.package"
    private val appLinkReturnUri = "merchant.app".toUri()
    private val someUri = "example.com".toUri()

    @Before
    fun setUp() {
        val applicationContext = mockk<Context>()
        every { merchantRepository.applicationContext } returns applicationContext
        every { merchantRepository.appLinkReturnUri } returns appLinkReturnUri
        every { applicationContext.packageName } returns packageName
        sut = GetReturnLinkTypeUseCase(merchantRepository, getDefaultAppUseCase, getAppLinksCompatibleBrowserUseCase)
    }

    @Test
    fun `when invoke is called and merchant app is able to handle return uri by default and we have an app link compatible browser, APP_LINK is returned`() {
        every { getDefaultAppUseCase(appLinkReturnUri) } returns packageName
        every { getAppLinksCompatibleBrowserUseCase.invoke(someUri) } returns true

        val result = sut.invoke(someUri)

        assertEquals(GetReturnLinkTypeUseCase.ReturnLinkTypeResult.APP_LINK, result)
    }

    @Test
    fun `when invoke is called and merchant app is able to handle return uri by default and app link compatible browser is not available, DEEP_LINK is returned`() {
        every { getDefaultAppUseCase(appLinkReturnUri) } returns packageName
        every { getAppLinksCompatibleBrowserUseCase(someUri) } returns false

        val result = sut(someUri)

        assertEquals(GetReturnLinkTypeUseCase.ReturnLinkTypeResult.DEEP_LINK, result)
    }

    @Test
    fun `when invoke is called and merchant app is unable to handle return uri by default, DEEP_LINK is returned`() {

        every { getDefaultAppUseCase(appLinkReturnUri) } returns "some.other.package"
        every { getAppLinksCompatibleBrowserUseCase(someUri) } returns true

        val result = sut(someUri)

        assertEquals(GetReturnLinkTypeUseCase.ReturnLinkTypeResult.DEEP_LINK, result)
    }

    @Test
    fun `when invoke is called and merchant app is unable to handle return uri by default and app link compatible browser is not available, DEEP_LINK is returned`() {
        val someUri = "example.com".toUri()
        every { getDefaultAppUseCase(appLinkReturnUri) } returns "some.other.package"
        every { getAppLinksCompatibleBrowserUseCase(someUri) } returns false

        val result = sut(someUri)

        assertEquals(GetReturnLinkTypeUseCase.ReturnLinkTypeResult.DEEP_LINK, result)
    }
}
