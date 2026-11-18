package com.braintreepayments.demo

import android.content.Context
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.braintreepayments.api.BrowserSwitchException
import com.braintreepayments.api.paypal.PayPalClient
import com.braintreepayments.api.paypal.PayPalLauncher
import com.braintreepayments.api.paypal.PayPalPaymentAuthRequest
import com.braintreepayments.api.paypal.PayPalPaymentAuthResult
import com.braintreepayments.api.paypal.PayPalPendingRequest
import com.braintreepayments.api.paypal.PayPalResult
import com.braintreepayments.api.paypal.PayPalVaultRequest
import com.braintreepayments.demo.models.PaymentMethodToken
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.UUID

/**
 * Vaults a PayPal account through the Demo merchant server and saves its payment method token to the Demo settings,
 * so the saved payment method screens can fetch a client token that carries a `paymentMethodIdJwt`. Sandbox only.
 *
 * Create it in the fragment's `onCreate`, since [PayPalLauncher] registers for activity results, and call
 * [handleReturn] from `onResume`. Its pending request is kept apart from the component's, so the two flows don't
 * consume each other's returns.
 */
class SavedPaymentMethodVaultHelper(
    private val fragment: Fragment,
    private val onResult: (Result<String>) -> Unit
) {

    private val context: Context = fragment.requireContext().applicationContext
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val payPalLauncher = PayPalLauncher(fragment)

    init {
        // A pending request survives the process being killed while the buyer is in PayPal.
        preferences.getString(KEY_PENDING_REQUEST, null)?.let { pendingRequest ->
            try {
                payPalLauncher.restorePendingRequest(pendingRequest)
            } catch (ignored: BrowserSwitchException) {
                clearPendingRequest()
            }
        }
    }

    /** Fetches a client token for the buyer and opens PayPal to vault their account. */
    fun start() {
        Merchant().fetchClientToken(context) { result ->
            when (result) {
                is FetchClientTokenResult.Success -> launchPayPal(result.clientToken)
                is FetchClientTokenResult.Error -> onResult(Result.failure(result.error))
            }
        }
    }

    /** Completes a vault the buyer is returning from. Does nothing when no vault was started. */
    fun handleReturn() {
        val pendingRequest = preferences.getString(KEY_PENDING_REQUEST, null) ?: return
        val authorization = preferences.getString(KEY_AUTHORIZATION, null)
        clearPendingRequest()
        val intent = fragment.requireActivity().intent
        val authResult = payPalLauncher.handleReturnToApp(PayPalPendingRequest.Started(pendingRequest), intent)
        intent.data = null
        when {
            authResult is PayPalPaymentAuthResult.Success && authorization != null ->
                payPalClient(authorization).tokenize(authResult) { result ->
                    when (result) {
                        is PayPalResult.Success -> vault(result.nonce.string)
                        is PayPalResult.Failure -> onResult(Result.failure(result.error))
                        is PayPalResult.Cancel -> onResult(Result.failure(Exception(canceledMessage())))
                    }
                }
            authResult is PayPalPaymentAuthResult.Failure -> onResult(Result.failure(authResult.error))
            else -> onResult(Result.failure(Exception(canceledMessage())))
        }
    }

    private fun launchPayPal(authorization: String) {
        val request = PayPalVaultRequest(
            hasUserLocationConsent = true,
            enablePayPalAppSwitch = Settings.isPayPalAppSwithEnabled(context)
        )
        payPalClient(authorization).createPaymentAuthRequest(context, request) { authRequest ->
            when (authRequest) {
                is PayPalPaymentAuthRequest.ReadyToLaunch ->
                    when (val pendingRequest = payPalLauncher.launch(fragment.requireActivity(), authRequest)) {
                        is PayPalPendingRequest.Started -> preferences.edit {
                            putString(KEY_PENDING_REQUEST, pendingRequest.pendingRequestString)
                            putString(KEY_AUTHORIZATION, authorization)
                        }
                        is PayPalPendingRequest.Failure -> onResult(Result.failure(pendingRequest.error))
                    }
                is PayPalPaymentAuthRequest.Failure -> onResult(Result.failure(authRequest.error))
            }
        }
    }

    /**
     * The merchant server rejects vaulting a nonce to a customer other than the client token's ("Customer ID is
     * invalid"), so it's vaulted to the Customer setting. Without one, a new customer is created first.
     */
    private fun vault(nonce: String) {
        val customerId = Settings.getCustomerId(context)?.trim()
        if (!customerId.isNullOrEmpty()) {
            vault(nonce, customerId)
            return
        }
        val newCustomerId = UUID.randomUUID().toString()
        DemoApplication.getApiClient(context).createCustomer(newCustomerId).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    vault(nonce, newCustomerId)
                } else {
                    onResult(Result.failure(Exception("Couldn't create a customer (HTTP ${response.code()})")))
                }
            }

            override fun onFailure(call: Call<ResponseBody>, error: Throwable) {
                onResult(Result.failure(Exception(error)))
            }
        })
    }

    private fun vault(nonce: String, customerId: String) {
        DemoApplication.getApiClient(context).vaultPaymentMethod(customerId, nonce)
            .enqueue(object : Callback<PaymentMethodToken> {
                override fun onResponse(call: Call<PaymentMethodToken>, response: Response<PaymentMethodToken>) {
                    val body = response.body()
                    val token = body?.token
                    if (token.isNullOrEmpty()) {
                        val message = body?.message
                            ?: context.getString(R.string.paypal_saved_payment_method_vault_failed)
                        onResult(Result.failure(Exception(message)))
                    } else {
                        Settings.setSavedPaymentMethodToken(context, token)
                        onResult(Result.success(token))
                    }
                }

                override fun onFailure(call: Call<PaymentMethodToken>, error: Throwable) {
                    onResult(Result.failure(Exception(error)))
                }
            })
    }

    private fun payPalClient(authorization: String) =
        PayPalClient(context, authorization, APP_LINK_RETURN_URL.toUri(), DEEP_LINK_FALLBACK_URL_SCHEME)

    private fun clearPendingRequest() {
        preferences.edit {
            remove(KEY_PENDING_REQUEST)
            remove(KEY_AUTHORIZATION)
        }
    }

    private fun canceledMessage() = context.getString(R.string.paypal_saved_payment_method_vault_canceled)

    companion object {
        private const val PREFERENCES_NAME = "PAYPAL_SAVED_PAYMENT_METHOD_VAULT"
        private const val KEY_PENDING_REQUEST = "pending_request"
        private const val KEY_AUTHORIZATION = "authorization"
        private const val APP_LINK_RETURN_URL =
            "https://mobile-sdk-demo-site-838cead5d3ab.herokuapp.com/braintree-payments"
        private const val DEEP_LINK_FALLBACK_URL_SCHEME = "com.braintreepayments.demo.braintree"

        /**
         * Uses [pastedClientToken] when one is pasted, otherwise fetches one from the merchant server with the saved
         * payment method token, so it carries that payment method's `paymentMethodIdJwt`.
         */
        fun fetchClientToken(context: Context, pastedClientToken: String, onResult: (Result<String>) -> Unit) {
            if (pastedClientToken.isNotBlank()) {
                onResult(Result.success(pastedClientToken.trim()))
                return
            }
            Merchant().fetchClientToken(context, Settings.getSavedPaymentMethodToken(context)) { result ->
                when (result) {
                    is FetchClientTokenResult.Success -> onResult(Result.success(result.clientToken))
                    is FetchClientTokenResult.Error -> onResult(Result.failure(result.error))
                }
            }
        }
    }
}
