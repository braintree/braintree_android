package com.braintreepayments.api.core

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

    lateinit var subject: GetReturnLinkTypeUseCase
    private val getAppLinksCompatibleBrowserUseCase = mockk<GetAppLinksCompatibleBrowserUseCase>()

    @Before
    fun setUp() {
        subject = GetReturnLinkTypeUseCase(getAppLinksCompatibleBrowserUseCase)
    }

    @Test
    fun `when invoke is called and we have a app link compatible browser, APP_LINK is returned`() {
        every { getAppLinksCompatibleBrowserUseCase() } returns true

        val result = subject()

        assertEquals(GetReturnLinkTypeUseCase.ReturnLinkTypeResult.APP_LINK, result)
    }

    @Test
    fun `when invoke is called and app link is not available, DEEP_LINK is returned`() {
        every { getAppLinksCompatibleBrowserUseCase() } returns false

        val result = subject()

        assertEquals(GetReturnLinkTypeUseCase.ReturnLinkTypeResult.DEEP_LINK, result)
    }
}
