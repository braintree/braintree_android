package com.braintreepayments.api.core.usecase

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.braintreepayments.api.core.MerchantRepository
import com.braintreepayments.api.core.usecase.GetReturnLinkUseCase.ReturnLinkResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4ClassRunner::class)
class GetReturnLinkUseCaseTest {

    private lateinit var context: Context
    private lateinit var merchantRepository: MerchantRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        merchantRepository = MerchantRepository()
        merchantRepository.applicationContext = context
    }

    @Test(timeout = 1000)
    fun invoke_returnsDeepLink_whenMerchantHasDeepLinkFallback() {
        merchantRepository.deepLinkFallbackUrlScheme = "com.example.app.braintree"
        val sut = GetReturnLinkUseCase(merchantRepository)

        val result = sut()

        assertTrue(result is ReturnLinkResult.DeepLink)
        assertEquals(
            "com.example.app.braintree",
            (result as ReturnLinkResult.DeepLink).deepLinkFallbackUrlScheme
        )
    }

    @Test(timeout = 1000)
    fun invoke_returnsFailure_whenBothLinksAreNull() {
        merchantRepository.appLinkReturnUri = null
        merchantRepository.deepLinkFallbackUrlScheme = null
        val sut = GetReturnLinkUseCase(merchantRepository)

        val result = sut()

        assertTrue(result is ReturnLinkResult.Failure)
    }
}
