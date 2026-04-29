package com.braintreepayments.api.sepadirectdebit

import android.content.Intent
import android.net.Uri
import android.util.Base64
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.braintreepayments.api.core.BraintreeRequestCodes
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4ClassRunner::class)
class SEPADirectDebitLauncherTest {

    private val returnUrlScheme = "com.braintreepayments.test"

    @Test
    fun handleReturnToApp_withSuccessDeepLink_returnsSuccess() {
        val launcher = SEPADirectDebitLauncher()
        val pendingRequest = buildPendingRequest()

        val intent = Intent().apply {
            data = Uri.parse("$returnUrlScheme://sepa/success?success=true")
        }

        val result = launcher.handleReturnToApp(pendingRequest, intent)
        assertTrue(
            "Expected Success but got ${result::class.simpleName}",
            result is SEPADirectDebitPaymentAuthResult.Success
        )
    }

    @Test
    fun handleReturnToApp_withCancelDeepLink_returnsSuccess() {
        val launcher = SEPADirectDebitLauncher()
        val pendingRequest = buildPendingRequest()

        val intent = Intent().apply {
            data = Uri.parse("$returnUrlScheme://sepa/cancel")
        }

        val result = launcher.handleReturnToApp(pendingRequest, intent)
        assertTrue(
            "Expected Success but got ${result::class.simpleName}",
            result is SEPADirectDebitPaymentAuthResult.Success
        )
    }

    @Test
    fun handleReturnToApp_withNonMatchingIntent_returnsNoResult() {
        val launcher = SEPADirectDebitLauncher()
        val pendingRequest = buildPendingRequest()

        val intent = Intent()

        val result = launcher.handleReturnToApp(pendingRequest, intent)
        assertTrue(
            "Expected NoResult but got ${result::class.simpleName}",
            result is SEPADirectDebitPaymentAuthResult.NoResult
        )
    }

    private fun buildPendingRequest(): SEPADirectDebitPendingRequest.Started {
        val metadata = JSONObject()
            .put("ibanLastFour", "6610")
            .put("customerId", "a-customer-id")
            .put("bankReferenceToken", "QkEtWDZDQkpCUU5TWENDVw")
            .put("mandateType", "RECURRENT")

        val pendingRequestJson = JSONObject()
            .put("requestCode", BraintreeRequestCodes.SEPA_DEBIT.code)
            .put("url", "https://api.test19.stage.paypal.com/directdebit/mandate/authorize")
            .put("returnUrlScheme", returnUrlScheme)
            .put("metadata", metadata)

        val pendingRequestString = Base64.encodeToString(
            pendingRequestJson.toString().toByteArray(Charsets.UTF_8),
            Base64.DEFAULT
        )

        return SEPADirectDebitPendingRequest.Started(pendingRequestString)
    }
}
