package com.braintreepayments.api.sharedutils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppHelperUnitTest {

    private lateinit var context: Context
    private lateinit var packageManager: PackageManager

    @Before
    fun beforeEach() {
        context = mockk(relaxed = true)
        packageManager = mockk(relaxed = true)
    }

    @Test
    fun isAppInstalled_whenAppInfoExistsForPackageName_returnsTrue() {
        every { context.packageManager } returns packageManager
        every { packageManager.getApplicationInfo("package.name", 0) } returns mockk<ApplicationInfo>()

        val sut = AppHelper()
        assertTrue(sut.isAppInstalled(context, "package.name"))
    }

    @Test
    fun isAppInstalled_whenAppInfoNotFoundForPackageName_returnsFalse() {
        every { context.packageManager } returns packageManager
        every { packageManager.getApplicationInfo("package.name", 0) } throws NameNotFoundException()

        val sut = AppHelper()
        assertFalse(sut.isAppInstalled(context, "package.name"))
    }
}
