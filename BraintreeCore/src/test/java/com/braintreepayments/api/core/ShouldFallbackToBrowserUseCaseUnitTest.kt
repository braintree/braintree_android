package com.braintreepayments.api.core

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class ShouldFallbackToBrowserUseCaseUnitTest {

    private val merchantRepository: MerchantRepository = mockk(relaxed = true)
    private val context: Context = mockk(relaxed = true)
    private val packageManager: PackageManager = mockk(relaxed = true)
    private val resolveInfo = ResolveInfo()
    private val activityInfo = ActivityInfo()
    
    private val paypalUri = Uri.parse("https://paypal.com/checkout")
    private val paypalPackageName = "com.paypal.android.p2pmobile"
    
    lateinit var subject: ShouldFallbackToBrowserUseCase

    @Before
    fun setUp() {
        every { context.packageManager } returns packageManager
        every { merchantRepository.applicationContext } returns context
        resolveInfo.activityInfo = activityInfo
        
        subject = ShouldFallbackToBrowserUseCase(merchantRepository)
    }

    @Test
    fun `when invoke is called and intent would open in target app, APP_SWITCH is returned`() {
        activityInfo.packageName = paypalPackageName
        every { 
            packageManager.resolveActivity(any(), PackageManager.MATCH_DEFAULT_ONLY) 
        } returns resolveInfo

        val result = subject(paypalUri)

        assertEquals(ShouldFallbackToBrowserUseCase.Result.APP_SWITCH, result)
    }

    @Test
    fun `when invoke is called and intent would not open in target app, FALLBACK is returned`() {
        activityInfo.packageName = "com.android.chrome"

        every { 
            packageManager.resolveActivity(any(), PackageManager.MATCH_DEFAULT_ONLY) 
        } returns resolveInfo

        val result = subject(paypalUri)

        assertEquals(ShouldFallbackToBrowserUseCase.Result.FALLBACK, result)
    }

    @Test
    fun `when invoke is called and no activity resolves the intent, FALLBACK is returned`() {
        every { 
            packageManager.resolveActivity(any(), PackageManager.MATCH_DEFAULT_ONLY) 
        } returns null

        val result = subject(paypalUri)

        assertEquals(ShouldFallbackToBrowserUseCase.Result.FALLBACK, result)
    }

    @Test
    fun `when invoke is called and activity info is null, FALLBACK is returned`() {
        resolveInfo.activityInfo = null

        every { 
            packageManager.resolveActivity(any(), PackageManager.MATCH_DEFAULT_ONLY) 
        } returns resolveInfo

        val result = subject(paypalUri)

        assertEquals(ShouldFallbackToBrowserUseCase.Result.FALLBACK, result)
    }

}