package com.braintreepayments.demo

import android.content.Context
import com.braintreepayments.demo.models.CreatePaymentActionResponse
import java.util.Locale
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

enum class ConfirmationMethod(val label: String) {
    AUTOMATIC("Auto"),
    MANUAL("Manual"),
}

enum class CaptureMethod(val label: String) {
    AUTOMATIC("Auto"),
    MANUAL("Manual"),
}

object PaymentActionsMerchant {

    fun createPaymentAction(
        context: Context,
        confirmationMethod: ConfirmationMethod = ConfirmationMethod.AUTOMATIC,
        captureMethod: CaptureMethod = CaptureMethod.AUTOMATIC,
        callback: CreatePaymentActionCallback,
    ) {
        // Payment action creation does not fall back to the gateway default merchant account,
        // unlike /client_token and /nonce/transaction, so a blank setting must be resolved here.
        val merchantAccountId = Settings.getMerchantAccountId(context)
            ?.takeIf { it.isNotBlank() }
            ?: DEMO_MERCHANT_ACCOUNT_ID

        DemoApplication
            .getApiClient(context)
            .createPaymentAction(
                AMOUNT,
                merchantAccountId,
                confirmationMethod.name,
                captureMethod.name
            )
            .enqueue(object : Callback<CreatePaymentActionResponse> {
                override fun onResponse(
                    call: Call<CreatePaymentActionResponse>,
                    response: Response<CreatePaymentActionResponse>,
                ) {
                    if (response.isSuccessful) {
                        val body = response.body()
                        val clientToken = body?.clientToken
                        val paymentActionId = body?.paymentAction?.id
                        val paymentActionStatus = body?.paymentAction?.status
                        if (clientToken.isNullOrEmpty() || paymentActionId.isNullOrEmpty() ||
                            paymentActionStatus.isNullOrEmpty()
                        ) {
                            val message =
                                "Unable to create payment action. Response body was missing required fields"
                            callback.onResult(CreatePaymentActionResult.Error(Exception(message)))
                        } else {
                            callback.onResult(
                                CreatePaymentActionResult.Success(clientToken, paymentActionId, paymentActionStatus)
                            )
                        }
                    } else {
                        val responseBody = response.errorBody()?.string().orEmpty().ifBlank { "empty response body" }
                        val errorMessage = String.format(
                            Locale.US,
                            "Unable to create payment action. Response Code: %d Response body: %s",
                            response.code(),
                            responseBody
                        )
                        callback.onResult(CreatePaymentActionResult.Error(Exception(errorMessage)))
                    }
                }

                override fun onFailure(call: Call<CreatePaymentActionResponse>, throwable: Throwable) {
                    callback.onResult(CreatePaymentActionResult.Error(Exception(throwable)))
                }
            })
    }

    private const val AMOUNT = "1.00"

    // The merchant account provisioned on the demo merchant server's gateway.
    private const val DEMO_MERCHANT_ACCOUNT_ID = "abcd"
}
