package com.braintreepayments.api.core.usecase

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.braintreepayments.api.core.MerchantRepository
import com.braintreepayments.api.core.usecase.GetReturnLinkTypeUseCase.ReturnLinkTypeResult
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4ClassRunner::class)
class GetReturnLinkTypeUseCaseTest {

    private lateinit var context: Context
    private lateinit var merchantRepository: MerchantRepository
    private lateinit var sut: GetReturnLinkTypeUseCase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        merchantRepository = MerchantRepository()
        merchantRepository.applicationContext = context
        merchantRepository.appLinkReturnUri = Uri.parse("https://example.com/braintree-return")

        val getDefaultAppUseCase = GetDefaultAppUseCase(context.packageManager)
        val getAppLinksCompatibleBrowserUseCase = GetAppLinksCompatibleBrowserUseCase(getDefaultAppUseCase)
        sut = GetReturnLinkTypeUseCase(merchantRepository, getDefaultAppUseCase, getAppLinksCompatibleBrowserUseCase)
    }

    @Test(timeout = 1000)
    fun invoke_returnsDeepLink_whenTestAppIsNotDefaultHandlerForReturnUri() {
        val result = sut()
        assertEquals(ReturnLinkTypeResult.DEEP_LINK, result)
    }

    @Test(timeout = 1000)
    fun invoke_returnsDeepLink_whenAppLinkReturnUriIsNull() {
        merchantRepository.appLinkReturnUri = null
        val result = sut()
        assertEquals(ReturnLinkTypeResult.DEEP_LINK, result)
    }

    @Test(timeout = 1000)
    fun invoke_returnsConsistentResultForSameInput() {
        val result1 = sut()
        val result2 = sut()
        assertEquals(result1, result2)
    }
}