package com.braintreepayments.api.core

import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class GetAppLinksCompatibleBrowserUseCaseUnitTest {

    private val getDefaultBrowserUseCase: GetDefaultBrowserUseCase = mockk(relaxed = true)

    internal lateinit var sut: GetAppLinksCompatibleBrowserUseCase

    @Before
    fun setUp() {
        sut = GetAppLinksCompatibleBrowserUseCase(getDefaultBrowserUseCase)
    }

    @Test
    fun `when invoke is called with Chrome as default browser, returns true`() {
        every { getDefaultBrowserUseCase() } returns "com.android.chrome"

        val result = sut()

        assertEquals(true, result)
    }

    @Test
    fun `when invoke is called with Brave as default browser, returns true`() {
        every { getDefaultBrowserUseCase() } returns "com.brave.browser"

        val result = sut()

        assertEquals(true, result)
    }

    @Test
    fun `when invoke is called with Samsung Browser as default browser, returns true`() {
        every { getDefaultBrowserUseCase() } returns "com.sec.android.app.sbrowser"

        val result = sut()

        assertEquals(true, result)
    }

    @Test
    fun `when invoke is called with Firefox as default browser, returns true`() {
        every { getDefaultBrowserUseCase() } returns "org.mozilla.firefox"

        val result = sut()

        assertEquals(true, result)
    }

    @Test
    fun `when invoke is called with Microsoft Edge as default browser, returns true`() {
        every { getDefaultBrowserUseCase() } returns "com.microsoft.emmx"

        val result = sut()

        assertEquals(true, result)
    }

    @Test
    fun `when invoke is called with Mi Browser as default browser, returns false`() {
        every { getDefaultBrowserUseCase() } returns "com.mi.globalbrowser"

        val result = sut()

        assertEquals(false, result)
    }

    @Test
    fun `when invoke is called with UC Browser as default browser, returns false`() {
        every { getDefaultBrowserUseCase() } returns "com.UCMobile.intl"

        val result = sut()

        assertEquals(false, result)
    }

    @Test
    fun `when invoke is called with DuckDuckGo as default browser, returns false`() {
        every { getDefaultBrowserUseCase() } returns "com.duckduckgo.mobile.android"

        val result = sut()

        assertEquals(false, result)
    }

    @Test
    fun `when invoke is called with Opera as default browser, returns false`() {
        every { getDefaultBrowserUseCase() } returns "com.opera.browser"

        val result = sut()

        assertEquals(false, result)
    }

    @Test
    fun `when invoke is called with Opera GX as default browser, returns false`() {
        every { getDefaultBrowserUseCase() } returns "com.opera.gx"

        val result = sut()

        assertEquals(false, result)
    }

    @Test
    fun `when invoke is called with Opera Mini as default browser, returns false`() {
        every { getDefaultBrowserUseCase() } returns "com.opera.mini.native"

        val result = sut()

        assertEquals(false, result)
    }

    @Test
    fun `when invoke is called with Yandex Browser as default browser, returns false`() {
        every { getDefaultBrowserUseCase() } returns "com.yandex.browser"

        val result = sut()

        assertEquals(false, result)
    }

    @Test
    fun `when invoke is called with unknown browser as default browser, returns false`() {
        every { getDefaultBrowserUseCase() } returns "com.unknown.browser"

        val result = sut()

        assertEquals(false, result)
    }

    @Test
    fun `when invoke is called and default browser is null, returns false`() {
        every { getDefaultBrowserUseCase() } returns null

        val result = sut()

        assertEquals(false, result)
    }
}
