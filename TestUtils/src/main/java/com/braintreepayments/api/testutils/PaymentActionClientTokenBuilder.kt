package com.braintreepayments.api.testutils

import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Fetches a fresh client token bound to a newly created payment action from the demo merchant
 * server's /create_payment_action endpoint. Each [build] call creates a new payment action.
 */
@Suppress("TooGenericExceptionThrown", "ThrowsCount", "MagicNumber")
class PaymentActionClientTokenBuilder {

    private var amount: String = "10.00"
    private var merchantAccountId: String = "abcd"
    private var confirmationMethod: PaymentActionConfirmationMethod = PaymentActionConfirmationMethod.AUTOMATIC
    private var captureMethod: PaymentActionCaptureMethod = PaymentActionCaptureMethod.AUTOMATIC

    fun amount(amount: String): PaymentActionClientTokenBuilder {
        this.amount = amount
        return this
    }

    fun merchantAccountId(merchantAccountId: String): PaymentActionClientTokenBuilder {
        this.merchantAccountId = merchantAccountId
        return this
    }

    fun confirmationMethod(confirmationMethod: PaymentActionConfirmationMethod): PaymentActionClientTokenBuilder {
        this.confirmationMethod = confirmationMethod
        return this
    }

    fun captureMethod(captureMethod: PaymentActionCaptureMethod): PaymentActionClientTokenBuilder {
        this.captureMethod = captureMethod
        return this
    }

    fun build(): CreatePaymentActionResponse {
        val connection = URL("https://braintree-demo-merchant-63b7a2204f6e.herokuapp.com/create_payment_action")
            .openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            connection.doOutput = true
            connection.doInput = true
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000

            val body = buildFormBody()
            OutputStreamWriter(connection.outputStream).use { it.write(body) }

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() }
                val truncated = errorBody?.take(500) ?: "(no error body)"
                throw RuntimeException("Failed to create payment action: HTTP $responseCode: $truncated")
            }

            val responseBody = BufferedReader(InputStreamReader(connection.inputStream))
                .use(BufferedReader::readText)
            parseResponse(responseBody)
        } catch (e: IOException) {
            throw RuntimeException("Error connecting to sample merchant server: ${e.message}", e)
        } catch (e: JSONException) {
            throw RuntimeException("Invalid response from sample merchant server: ${e.message}", e)
        } finally {
            connection.disconnect()
        }
    }

    private fun buildFormBody(): String {
        return "amount=${encode(amount)}" +
            "&merchant_account_id=${encode(merchantAccountId)}" +
            "&confirmation_method=${encode(confirmationMethod.name)}" +
            "&capture_method=${encode(captureMethod.name)}"
    }

    private fun encode(value: String): String {
        return URLEncoder.encode(value, "UTF-8")
    }

    private fun parseResponse(responseBody: String): CreatePaymentActionResponse {
        val json = JSONObject(responseBody)
        val paymentActionJson = json.getJSONObject("payment_action")
        return CreatePaymentActionResponse(
            clientToken = json.getString("client_token"),
            paymentAction = CreatePaymentActionResponse.PaymentAction(
                id = paymentActionJson.getString("id"),
                status = paymentActionJson.getString("status"),
            ),
        )
    }
}

enum class PaymentActionConfirmationMethod {
    AUTOMATIC,
    MANUAL,
}

enum class PaymentActionCaptureMethod {
    AUTOMATIC,
    MANUAL,
}

data class CreatePaymentActionResponse(
    val clientToken: String,
    val paymentAction: PaymentAction,
) {
    data class PaymentAction(
        val id: String,
        val status: String,
    )
}
