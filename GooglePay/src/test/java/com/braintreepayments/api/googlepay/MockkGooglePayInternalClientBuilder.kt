package com.braintreepayments.api.googlepay

import android.content.Context
import androidx.fragment.app.FragmentActivity
import com.braintreepayments.api.core.Configuration
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.android.gms.wallet.IsReadyToPayRequest
import com.google.android.gms.wallet.PaymentData
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk

@Suppress("MagicNumber")
internal class MockkGooglePayInternalClientBuilder {

    private var isReadyToPay: Boolean = false
    private var isReadyToPayError: Exception? = null
    private var loadPaymentDataTask: Task<PaymentData> = buildCompletingTask(mockk(relaxed = true))

    fun isReadyToPay(isReadyToPay: Boolean): MockkGooglePayInternalClientBuilder {
        this.isReadyToPay = isReadyToPay
        return this
    }

    fun isReadyToPayError(isReadyToPayError: Exception): MockkGooglePayInternalClientBuilder {
        this.isReadyToPayError = isReadyToPayError
        return this
    }

    fun loadPaymentDataTask(task: Task<PaymentData>): MockkGooglePayInternalClientBuilder {
        this.loadPaymentDataTask = buildCompletingTask(task)
        return this
    }

    fun build(): GooglePayInternalClient {
        val googlePayInternalClient = mockk<GooglePayInternalClient>(relaxed = true)

        coEvery {
            googlePayInternalClient.isReadyToPay(
                any<FragmentActivity>(),
                any<Configuration>(),
                any<IsReadyToPayRequest>()
            )
        } answers { call ->

            isReadyToPayError?.let {
                GooglePayReadinessResult.NotReadyToPay(isReadyToPayError!!)
            }
            ?: let { GooglePayReadinessResult.ReadyToPay }
        }

        every {
            googlePayInternalClient.loadPaymentData(
                any<Context>(),
                any<GooglePayPaymentAuthRequestParams>()
            )
        } returns loadPaymentDataTask

        return googlePayInternalClient
    }

    companion object {
        fun buildCompletingTask(task: Task<PaymentData>): Task<PaymentData> {
            every { task.addOnCompleteListener(any()) } answers {
                firstArg<OnCompleteListener<PaymentData>>().onComplete(task)
                task
            }
            return task
        }
    }
}
