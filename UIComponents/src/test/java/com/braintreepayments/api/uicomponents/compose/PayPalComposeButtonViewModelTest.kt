package com.braintreepayments.api.uicomponents.compose

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class PayPalComposeButtonViewModelTest {
    private val mockRepository = mockk<PayPalPendingRequestRepository>()
    private val sut = PayPalComposeButtonViewModel(mockRepository)

    @Test
    fun storePendingRequest() {
        val pendingRequest = "pendingRequest"
        sut.storePendingRequest(pendingRequest)
        coVerify { mockRepository.storePendingRequest(pendingRequest) }
    }

    @Test
    fun getPendingRequest() = runTest {
        val expectedRequest = "pendingRequest"
        coEvery { mockRepository.getPendingRequest() } returns expectedRequest

        sut.getPendingRequest()
        coVerify { mockRepository.getPendingRequest() }
    }

    @Test
    fun clearPendingRequest() {
        sut.clearPendingRequest()
        coVerify { mockRepository.clearPendingRequest() }
    }
}
