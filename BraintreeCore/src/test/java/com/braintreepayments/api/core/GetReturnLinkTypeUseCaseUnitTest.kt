package com.braintreepayments.api.core

import com.braintreepayments.api.core.usecase.CheckReturnUriDefaultAppHandlerUseCase
import com.braintreepayments.api.core.usecase.GetAppLinksCompatibleBrowserUseCase
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
    private val checkReturnUriDefaultAppHandlerUseCase = mockk<CheckReturnUriDefaultAppHandlerUseCase>()

    @Before
    fun setUp() {
        sut = GetReturnLinkTypeUseCase(checkReturnUriDefaultAppHandlerUseCase, getAppLinksCompatibleBrowserUseCase)
    }

    @Test
    fun `when invoke is called and we are able to handle returnUri in app and we have a app link compatible browser,APP_LINK is returned`() {
        every { checkReturnUriDefaultAppHandlerUseCase() } returns true
        every { getAppLinksCompatibleBrowserUseCase(any()) } returns true

        val result = sut()

        assertEquals(GetReturnLinkTypeUseCase.ReturnLinkTypeResult.APP_LINK, result)
    }

    @Test
    fun `when invoke is called and app link compatible browser is not available, DEEP_LINK is returned`() {
        every { checkReturnUriDefaultAppHandlerUseCase() } returns true
        every { getAppLinksCompatibleBrowserUseCase(any()) } returns false

        val result = sut()

        assertEquals(GetReturnLinkTypeUseCase.ReturnLinkTypeResult.DEEP_LINK, result)
    }

    @Test
    fun `when invoke is called and we are unable to handle return uri, DEEP_LINK is returned`() {
        every { checkReturnUriDefaultAppHandlerUseCase() } returns false
        every { getAppLinksCompatibleBrowserUseCase(any()) } returns true

        val result = sut()

        assertEquals(GetReturnLinkTypeUseCase.ReturnLinkTypeResult.DEEP_LINK, result)
    }

    @Test
    fun `when invoke is called and we have a app link compatible browser, APP_LINK is returned`() {
        every { checkReturnUriDefaultAppHandlerUseCase() } returns false
        every { getAppLinksCompatibleBrowserUseCase(any()) } returns true

        val result = sut()

        assertEquals(GetReturnLinkTypeUseCase.ReturnLinkTypeResult.DEEP_LINK, result)
    }
}
