package com.braintreepayments.api.core

import androidx.core.net.toUri
import com.braintreepayments.api.core.usecase.GetAppLinksCompatibleBrowserUseCase
import com.braintreepayments.api.core.usecase.GetDefaultBrowserUseCase
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
    private val sampleUri = "https://example.com".toUri()

    @Before
    fun setUp() {
        sut = GetAppLinksCompatibleBrowserUseCase(getDefaultBrowserUseCase)
    }

    @Test
    fun `when invoke is called with Chrome as default browser, returns true`() {
        every { getDefaultBrowserUseCase(sampleUri) } returns "com.android.chrome"

        val result = sut(sampleUri)

        assertEquals(true, result)
    }

    @Test
    fun `when invoke is called with Chrome canary as default browser, returns true`() {
        every { getDefaultBrowserUseCase(sampleUri) } returns "com.android.chrome.canary"

        val result = sut(sampleUri)

        assertEquals(true, result)
    }

    @Test
    fun `when invoke is called with incomplete appId of Chrome as default browser, returns false`() {
        every { getDefaultBrowserUseCase(sampleUri) } returns "com.android"

        val result = sut(sampleUri)

        assertEquals(false, result)
    }

    @Test
    fun `when invoke is called with Brave as default browser, returns true`() {
        every { getDefaultBrowserUseCase(sampleUri) } returns "com.brave.browser"

        val result = sut(sampleUri)

        assertEquals(true, result)
    }

    @Test
    fun `when invoke is called with Samsung Browser as default browser, returns true`() {
        every { getDefaultBrowserUseCase(sampleUri) } returns "com.sec.android.app.sbrowser"

        val result = sut(sampleUri)

        assertEquals(true, result)
    }

    @Test
    fun `when invoke is called with Firefox as default browser, returns true`() {
        every { getDefaultBrowserUseCase(sampleUri) } returns "org.mozilla.firefox"

        val result = sut(sampleUri)

        assertEquals(true, result)
    }

    @Test
    fun `when invoke is called with Microsoft Edge as default browser, returns true`() {
        every { getDefaultBrowserUseCase(sampleUri) } returns "com.microsoft.emmx"

        val result = sut(sampleUri)

        assertEquals(true, result)
    }

    @Test
    fun `when invoke is called with Mi Browser as default browser, returns false`() {
        every { getDefaultBrowserUseCase(sampleUri) } returns "com.mi.globalbrowser"

        val result = sut(sampleUri)

        assertEquals(false, result)
    }

    @Test
    fun `when invoke is called with UC Browser as default browser, returns false`() {
        every { getDefaultBrowserUseCase(sampleUri) } returns "com.UCMobile.intl"

        val result = sut(sampleUri)

        assertEquals(false, result)
    }

    @Test
    fun `when invoke is called with DuckDuckGo as default browser, returns false`() {
        every { getDefaultBrowserUseCase(sampleUri) } returns "com.duckduckgo.mobile.android"

        val result = sut(sampleUri)

        assertEquals(false, result)
    }

    @Test
    fun `when invoke is called with Opera as default browser, returns false`() {
        every { getDefaultBrowserUseCase(sampleUri) } returns "com.opera.browser"

        val result = sut(sampleUri)

        assertEquals(false, result)
    }

    @Test
    fun `when invoke is called with Opera GX as default browser, returns false`() {
        every { getDefaultBrowserUseCase(sampleUri) } returns "com.opera.gx"

        val result = sut(sampleUri)

        assertEquals(false, result)
    }

    @Test
    fun `when invoke is called with Opera Mini as default browser, returns false`() {
        every { getDefaultBrowserUseCase(sampleUri) } returns "com.opera.mini.native"

        val result = sut(sampleUri)

        assertEquals(false, result)
    }

    @Test
    fun `when invoke is called with Yandex Browser as default browser, returns false`() {
        every { getDefaultBrowserUseCase(sampleUri) } returns "com.yandex.browser"

        val result = sut(sampleUri)

        assertEquals(false, result)
    }

    @Test
    fun `when invoke is called with unknown browser as default browser, returns false`() {
        every { getDefaultBrowserUseCase(sampleUri) } returns "com.unknown.browser"

        val result = sut(sampleUri)

        assertEquals(false, result)
    }

    @Test
    fun `when invoke is called and default browser is null, returns false`() {
        every { getDefaultBrowserUseCase(sampleUri) } returns null

        val result = sut(sampleUri)

        assertEquals(false, result)
    }
}
