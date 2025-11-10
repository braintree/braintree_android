package com.braintreepayments.api.core

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.net.toUri
import com.braintreepayments.api.core.usecase.GetAppLinksCompatibleBrowserUseCase
import com.braintreepayments.api.core.usecase.GetDefaultAppUseCase
import com.braintreepayments.api.core.usecase.GetReturnLinkTypeUseCase
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

    @Before
    fun setUp() {
        val applicationContext = mockk<Context>()
        every { merchantRepository.applicationContext } returns applicationContext
        every { applicationContext.packageName } returns packageName
        sut = GetReturnLinkTypeUseCase(merchantRepository, getDefaultAppUseCase, getAppLinksCompatibleBrowserUseCase)
    }

    @Test
    fun `when invoke is called and we are able to handle returnUri in app and we have a app link compatible browser,APP_LINK is returned`() {
        val someUri = "example.com".toUri()
        every { getDefaultAppUseCase(any()) } returns packageName
        every { getAppLinksCompatibleBrowserUseCase.invoke(someUri) } returns true

        val result = sut.invoke(someUri)

        assertEquals(GetReturnLinkTypeUseCase.ReturnLinkTypeResult.APP_LINK, result)
    }

    @Test
    fun `when invoke is called and app link compatible browser is not available, DEEP_LINK is returned`() {
        every { getAppLinksCompatibleBrowserUseCase(any()) } returns false

        val result = sut()

        assertEquals(GetReturnLinkTypeUseCase.ReturnLinkTypeResult.DEEP_LINK, result)
    }

    @Test
    fun `when invoke is called and we are unable to handle return uri, DEEP_LINK is returned`() {
        every { getAppLinksCompatibleBrowserUseCase(any()) } returns true

        val result = sut()

        assertEquals(GetReturnLinkTypeUseCase.ReturnLinkTypeResult.DEEP_LINK, result)
    }

    @Test
    fun `when invoke is called and we have a app link compatible browser, APP_LINK is returned`() {
        every { getAppLinksCompatibleBrowserUseCase(any()) } returns true

        val result = sut()

        assertEquals(GetReturnLinkTypeUseCase.ReturnLinkTypeResult.DEEP_LINK, result)
    }
}
