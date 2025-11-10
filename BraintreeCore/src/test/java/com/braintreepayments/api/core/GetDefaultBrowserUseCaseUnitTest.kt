package com.braintreepayments.api.core

import android.content.pm.PackageManager
import android.net.Uri
import com.braintreepayments.api.core.usecase.GetDefaultAppUseCase
import com.braintreepayments.api.core.usecase.GetDefaultBrowserUseCase
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class GetDefaultBrowserUseCaseUnitTest {

    private val packageManager: PackageManager = mockk(relaxed = true)
    private val appLinkReturnUri = Uri.parse("https://example.com")

    internal lateinit var sut: GetDefaultBrowserUseCase
    private val getDefaultAppUseCase = mockk<GetDefaultAppUseCase>()

    @Before
    fun setUp() {
        sut = GetDefaultBrowserUseCase(packageManager, getDefaultAppUseCase)
    }

    @Test
    fun `when invoke is called and default browser is found, returns browser package name`() {
        every { getDefaultAppUseCase(appLinkReturnUri) } returns "resultString"
        val result = sut(appLinkReturnUri)

        assertEquals("resultString", result)
    }
}
