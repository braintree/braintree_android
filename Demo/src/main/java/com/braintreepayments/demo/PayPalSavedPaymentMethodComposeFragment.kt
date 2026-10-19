package com.braintreepayments.demo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.fragment.findNavController
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.paypal.PayPalAccountNonce
import com.braintreepayments.api.paypal.PayPalCheckoutRequest
import com.braintreepayments.api.paypal.PayPalPaymentUserAction
import com.braintreepayments.api.paypal.PayPalResult
import com.braintreepayments.api.paypal.PayPalTokenizeCallback
import com.braintreepayments.api.paypalsavedpaymentmethod.PayPalSavedPaymentMethodRequest
import com.braintreepayments.api.paypalsavedpaymentmethod.compose.PayPalSavedPaymentMethodView

/**
 * Demonstrates the Compose `PayPalSavedPaymentMethodView`. The component needs a client token generated with the
 * buyer's payment method ID, so paste one that carries a `paymentMethodIdJwt`.
 */
class PayPalSavedPaymentMethodComposeFragment : BaseFragment() {

    private val nonceState = mutableStateOf<PayPalAccountNonce?>(null)

    // Bumped on every resume, so a change made in the Demo settings applies when the buyer comes back here.
    private val resumeCount = mutableIntStateOf(0)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setContent { SavedPaymentMethodScreen() }
    }

    @OptIn(ExperimentalBetaApi::class)
    @Composable
    private fun SavedPaymentMethodScreen() {
        var clientToken by rememberSaveable { mutableStateOf("") }
        var amount by rememberSaveable { mutableStateOf(DEFAULT_AMOUNT) }
        var styleConfig by remember { mutableStateOf(PayPalSavedPaymentMethodStyleConfig()) }
        var isStyleEditorOpen by remember { mutableStateOf(false) }
        // The token and amount the component was last loaded with. Only Load reads the fields, and each tap
        // bumps loadCount so the component reloads from scratch, even with the same token.
        var loadedClientToken by rememberSaveable { mutableStateOf("") }
        var loadedAmount by rememberSaveable { mutableStateOf("") }
        var loadCount by rememberSaveable { mutableStateOf(0) }
        val nonce by nonceState

        val style = remember(styleConfig) { styleConfig.toStyle() }
        val checkoutRequest = remember(loadedAmount, resumeCount.intValue) { checkoutRequest(loadedAmount) }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.paypal_saved_payment_method_compose_button),
                style = MaterialTheme.typography.titleMedium
            )
            OutlinedTextField(
                value = clientToken,
                onValueChange = { clientToken = it },
                label = { Text(stringResource(R.string.paypal_saved_payment_method_client_token_label)) },
                modifier = Modifier.fillMaxWidth(),
                maxLines = CLIENT_TOKEN_MAX_LINES
            )
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text(stringResource(R.string.paypal_saved_payment_method_amount_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
            OutlinedButton(onClick = { isStyleEditorOpen = true }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.paypal_saved_payment_method_edit_style))
            }
            Button(
                onClick = {
                    if (clientToken.isBlank()) {
                        showDialog(getString(R.string.paypal_saved_payment_method_missing_token))
                    } else {
                        nonceState.value = null
                        loadedClientToken = clientToken.trim()
                        loadedAmount = amount.trim()
                        loadCount++
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.paypal_saved_payment_method_load))
            }

            if (loadedClientToken.isNotBlank()) {
                HorizontalDivider()
                key(loadCount) {
                    PayPalSavedPaymentMethodView(
                        payPalCheckoutRequest = checkoutRequest,
                        request = PayPalSavedPaymentMethodRequest(amount = loadedAmount, currencyCode = CURRENCY_CODE),
                        authorization = loadedClientToken,
                        appLinkReturnUrl = APP_LINK_RETURN_URL.toUri(),
                        modifier = Modifier.fillMaxWidth(),
                        deepLinkFallbackUrlScheme = DEEP_LINK_FALLBACK_URL_SCHEME,
                        style = style,
                        paypalTokenizeCallback = tokenizeCallback
                    )
                }
            }

            nonce?.let {
                Text(stringResource(R.string.nonce_placeholder, it.string))
                Button(onClick = { createTransaction(it, loadedAmount) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.create_transaction))
                }
            }
        }

        if (isStyleEditorOpen) {
            PayPalSavedPaymentMethodStyleEditor(
                config = styleConfig,
                onConfigChange = { styleConfig = it },
                onDismiss = { isStyleEditorOpen = false }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        resumeCount.intValue++
    }

    private fun checkoutRequest(amount: String) = PayPalCheckoutRequest(
        amount = amount,
        hasUserLocationConsent = true,
        currencyCode = CURRENCY_CODE,
        enablePayPalAppSwitch = Settings.isPayPalAppSwithEnabled(requireContext()),
        userAction = if (Settings.isPayPalUseractionCommitEnabled(requireContext())) {
            PayPalPaymentUserAction.USER_ACTION_COMMIT
        } else {
            PayPalPaymentUserAction.USER_ACTION_DEFAULT
        }
    )

    private fun createTransaction(nonce: PayPalAccountNonce, amount: String) {
        val action = PayPalSavedPaymentMethodComposeFragmentDirections
            .actionPayPalSavedPaymentMethodComposeFragmentToCreateTransactionFragment(nonce)
            .setTransactionAmount(amount)
        findNavController().navigate(action)
    }

    private val tokenizeCallback = PayPalTokenizeCallback { result ->
        when (result) {
            is PayPalResult.Success -> {
                onPaymentMethodNonceCreated(result.nonce)
                nonceState.value = result.nonce
            }
            is PayPalResult.Cancel -> handleError(Exception(getString(R.string.paypal_saved_payment_method_canceled)))
            is PayPalResult.Failure -> handleError(result.error)
        }
    }

    private companion object {
        const val DEFAULT_AMOUNT = "10.00"
        const val CURRENCY_CODE = "USD"
        const val CLIENT_TOKEN_MAX_LINES = 4
        const val APP_LINK_RETURN_URL = "https://mobile-sdk-demo-site-838cead5d3ab.herokuapp.com/braintree-payments"
        const val DEEP_LINK_FALLBACK_URL_SCHEME = "com.braintreepayments.demo.braintree"
    }
}
