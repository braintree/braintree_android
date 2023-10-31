package com.braintreepayments.api

import android.net.Uri
import org.json.JSONObject
import org.junit.Test
import org.mockito.Mockito

class KotlinTest {

    @Test
    fun test() {
        val payPalBrowserSwitchResultCallback = Mockito.mock(
            PayPalBrowserSwitchResultCallback::class.java
        )
        val payPalAccountNonce = Mockito.mock(
            PayPalAccountNonce::class.java
        )
        val payPalInternalClient = MockPayPalInternalClientBuilder()
            .tokenizeSuccess(payPalAccountNonce)
            .build()

        val approvalUrl =
            "sample-scheme://onetouch/v1/success?PayerID=HERMES-SANDBOX-PAYER-ID&paymentId=HERMES-SANDBOX-PAYMENT-ID&token=EC-HERMES-SANDBOX-EC-TOKEN"

        val browserSwitchResult = Mockito.mock(
            BrowserSwitchResult::class.java
        )
        Mockito.`when`(browserSwitchResult.status).thenReturn(BrowserSwitchStatus.SUCCESS)

        Mockito.`when`(browserSwitchResult.requestMetadata).thenReturn(
            JSONObject()
                .put("client-metadata-id", "sample-client-metadata-id")
                .put("merchant-account-id", "sample-merchant-account-id")
                .put("intent", "authorize")
                .put("approval-url", approvalUrl)
                .put("success-url", "https://example.com/success")
                .put("payment-type", "single-payment")
        )

        val uri = Uri.parse(approvalUrl)
        Mockito.`when`(browserSwitchResult.deepLinkUrl).thenReturn(uri)

        val payPalBrowserSwitchResult = PayPalBrowserSwitchResult(browserSwitchResult)
        val braintreeClient = MockBraintreeClientBuilder().build()
        val sut = PayPalClient(braintreeClient, payPalInternalClient)

        val payPalLauncher = PayPalLauncher(null)

        sut.tokenizePayPalAccount(null, null) { paymentAuthResponse ->
            when(paymentAuthResponse) {
                is PaymentAuthRequest.Ready -> {
                    payPalLauncher.launch(activity, paymentAuthResponse.launchRequest)
                }
                is PaymentAuthRequest.Failure -> {

                }
            }
        }

        sut.onBrowserSwitchResult(payPalBrowserSwitchResult) { payPalResult ->
            when(payPalResult) {
                is PaymentResult.Success -> {
                    val payPalAccountNonce = payPalResult.nonce as PayPalAccountNonce
                }
                is PaymentResult.Cancel -> {

                }
                is PaymentResult.Failure -> {

                }
            }
        }



        sut.test("test", object {
            override fun onResult(nonce: PayPalAccountNonce) {
                // handle result
            }

            override fun onError(error: Exception) {
                // handle error
            }

            override fun onCancel() {
                // handle cancel
            }
        })

    }

}