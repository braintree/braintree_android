package com.braintreepayments.api.googlepay

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.wallet.AutoResolveHelper
import com.google.android.gms.wallet.PaymentDataRequest
import com.google.android.gms.wallet.Wallet
import com.google.android.gms.wallet.Wallet.WalletOptions
import com.google.android.gms.wallet.WalletConstants

class GooglePayActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null && savedInstanceState.getBoolean(EXTRA_RECREATING)) {
            return
        }

        val paymentsClient = Wallet.getPaymentsClient(
            this, WalletOptions.Builder()
                .setEnvironment(
                    intent.getIntExtra(
                        EXTRA_ENVIRONMENT,
                        WalletConstants.ENVIRONMENT_TEST
                    )
                )
                .build()
        )

        val request = intent.getParcelableExtra<PaymentDataRequest>(EXTRA_PAYMENT_DATA_REQUEST)
        AutoResolveHelper.resolveTask(paymentsClient.loadPaymentData(request!!), this, REQUEST_CODE)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(EXTRA_RECREATING, true)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        setResult(resultCode, data)
        finish()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    companion object {
        const val EXTRA_ENVIRONMENT: String =
            "com.braintreepayments.api.EXTRA_ENVIRONMENT"
        const val EXTRA_PAYMENT_DATA_REQUEST: String =
            "com.braintreepayments.api.EXTRA_PAYMENT_DATA_REQUEST"

        private const val EXTRA_RECREATING = "com.braintreepayments.api.EXTRA_RECREATING"

        private const val REQUEST_CODE = 1
    }
}