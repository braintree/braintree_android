package com.braintreepayments.api.core

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class GetReturnLinkTypeUseCaseUnitTest {

    private val merchantRepository: MerchantRepository = mockk(relaxed = true)
    private val context: Context = mockk(relaxed = true)
    private val resolveInfo = ResolveInfo()
    private val activityInfo = ActivityInfo()
    private val contextPackageName = "context.package.name"

    lateinit var subject: GetReturnLinkTypeUseCase

    @Before
    fun setUp() {
        every { merchantRepository.applicationContext } returns context
        every { context.packageName } returns contextPackageName
        resolveInfo.activityInfo = activityInfo
        every { context.packageManager.resolveActivity(any<Intent>(), any<Int>()) } returns resolveInfo

        subject = GetReturnLinkTypeUseCase(merchantRepository)
    }

    @Test
    fun `when invoke is called and app link is available, APP_LINK is returned`() {
        activityInfo.packageName = "context.package.name"

        val result = subject()

        assertEquals(GetReturnLinkTypeUseCase.ReturnLinkTypeResult.APP_LINK, result)
    }

    @Test
    fun `when invoke is called and app link is not available, DEEP_LINK is returned`() {
        activityInfo.packageName = "different.package.name"

        val result = subject()

        assertEquals(GetReturnLinkTypeUseCase.ReturnLinkTypeResult.DEEP_LINK, result)
    }
}
