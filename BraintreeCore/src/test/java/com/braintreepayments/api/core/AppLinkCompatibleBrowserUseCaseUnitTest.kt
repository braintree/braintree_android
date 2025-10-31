package com.braintreepayments.api.core

import android.content.pm.PackageManager
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class AppLinkCompatibleBrowserUseCaseUnitTest {

    private val getDefaultBrowserUseCase: GetDefaultBrowserUseCase = mockk(relaxed = true)
    private val packageManager: PackageManager = mockk(relaxed = true)

    lateinit var subject: AppLinkCompatibleBrowserUseCase

    @Before
    fun setUp() {
        subject = AppLinkCompatibleBrowserUseCase(getDefaultBrowserUseCase, packageManager)
    }

    @Test
    fun `when invoke is called with Chrome as default browser, returns true`() {
        every { getDefaultBrowserUseCase(packageManager) } returns "com.android.chrome"

        val result = subject()

        assertEquals(true, result)
    }

    @Test
    fun `when invoke is called with Brave as default browser, returns true`() {
        every { getDefaultBrowserUseCase(packageManager) } returns "com.brave.browser"

        val result = subject()

        assertEquals(true, result)
    }

    @Test
    fun `when invoke is called with Samsung Browser as default browser, returns true`() {
        every { getDefaultBrowserUseCase(packageManager) } returns "com.sec.android.app.sbrowser"

        val result = subject()

        assertEquals(true, result)
    }

    @Test
    fun `when invoke is called with Firefox as default browser, returns true`() {
        every { getDefaultBrowserUseCase(packageManager) } returns "org.mozilla.firefox"

        val result = subject()

        assertEquals(true, result)
    }

    @Test
    fun `when invoke is called with Microsoft Edge as default browser, returns true`() {
        every { getDefaultBrowserUseCase(packageManager) } returns "com.microsoft.emmx"

        val result = subject()

        assertEquals(true, result)
    }

    @Test
    fun `when invoke is called with Mi Browser as default browser, returns false`() {
        every { getDefaultBrowserUseCase(packageManager) } returns "com.mi.globalbrowser"

        val result = subject()

        assertEquals(false, result)
    }

    @Test
    fun `when invoke is called with UC Browser as default browser, returns false`() {
        every { getDefaultBrowserUseCase(packageManager) } returns "com.UCMobile.intl"

        val result = subject()

        assertEquals(false, result)
    }

    @Test
    fun `when invoke is called with DuckDuckGo as default browser, returns false`() {
        every { getDefaultBrowserUseCase(packageManager) } returns "com.duckduckgo.mobile.android"

        val result = subject()

        assertEquals(false, result)
    }

    @Test
    fun `when invoke is called with Opera as default browser, returns false`() {
        every { getDefaultBrowserUseCase(packageManager) } returns "com.opera.browser"

        val result = subject()

        assertEquals(false, result)
    }

    @Test
    fun `when invoke is called with Opera GX as default browser, returns false`() {
        every { getDefaultBrowserUseCase(packageManager) } returns "com.opera.gx"

        val result = subject()

        assertEquals(false, result)
    }

    @Test
    fun `when invoke is called with Opera Mini as default browser, returns false`() {
        every { getDefaultBrowserUseCase(packageManager) } returns "com.opera.mini.native"

        val result = subject()

        assertEquals(false, result)
    }

    @Test
    fun `when invoke is called with Yandex Browser as default browser, returns false`() {
        every { getDefaultBrowserUseCase(packageManager) } returns "com.yandex.browser"

        val result = subject()

        assertEquals(false, result)
    }

    @Test
    fun `when invoke is called with unknown browser as default browser, returns false`() {
        every { getDefaultBrowserUseCase(packageManager) } returns "com.unknown.browser"

        val result = subject()

        assertEquals(false, result)
    }

    @Test
    fun `when invoke is called and default browser is null, returns false`() {
        every { getDefaultBrowserUseCase(packageManager) } returns null

        val result = subject()

        assertEquals(false, result)
    }
}
