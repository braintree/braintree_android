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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
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
import com.braintreepayments.demo.SavedPaymentMethodDemo.APP_LINK_RETURN_URL
import com.braintreepayments.demo.SavedPaymentMethodDemo.DEEP_LINK_FALLBACK_URL_SCHEME
import com.braintreepayments.demo.SavedPaymentMethodDemo.DEFAULT_AMOUNT
import com.braintreepayments.demo.SavedPaymentMethodDemo.DEFAULT_CURRENCY
import com.braintreepayments.demo.SavedPaymentMethodDemo.validAmountOrNull
import com.braintreepayments.demo.SavedPaymentMethodDemo.validCurrencyOrNull

/**
 * Demonstrates the Compose `PayPalSavedPaymentMethodView`. The component needs a client token that carries the buyer's
 * `paymentMethodIdJwt`: Load fetches one from the merchant server with the token saved by Vault PayPal Account, or uses
 * a pasted one.
 */
class PayPalSavedPaymentMethodComposeFragment : BaseFragment() {

    private val nonceState = mutableStateOf<PayPalAccountNonce?>(null)
    private val isVaulting = mutableStateOf(false)
    private val savedToken = mutableStateOf<String?>(null)
    private lateinit var vaultHelper: SavedPaymentMethodVaultHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vaultHelper = SavedPaymentMethodVaultHelper(this) { result ->
            isVaulting.value = false
            if (!isAdded) return@SavedPaymentMethodVaultHelper
            result
                .onSuccess {
                    savedToken.value = it
                    showDialog(getString(R.string.paypal_saved_payment_method_vault_saved))
                }
                .onFailure { handleError(it as? Exception ?: Exception(it)) }
        }
    }

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
        var currency by rememberSaveable { mutableStateOf(DEFAULT_CURRENCY) }
        var styleConfig by rememberSaveable { mutableStateOf(PayPalSavedPaymentMethodStyleConfig()) }
        var isStyleEditorOpen by remember { mutableStateOf(false) }
        // The token, amount and currency the component was last loaded with. Only Load reads the fields, and each tap
        // bumps loadCount so the component reloads from scratch, even with the same token.
        var loadedClientToken by rememberSaveable { mutableStateOf("") }
        var loadedAmount by rememberSaveable { mutableStateOf("") }
        var loadedCurrency by rememberSaveable { mutableStateOf(DEFAULT_CURRENCY) }
        var loadCount by rememberSaveable { mutableStateOf(0) }
        var isLoading by remember { mutableStateOf(false) }
        val nonce by nonceState

        val style = remember(styleConfig) { styleConfig.toStyle() }
        // Built on Load, as in the XML screen, so a change in the Demo settings applies on the next Load.
        val checkoutRequest = remember(loadedAmount, loadedCurrency, loadCount) {
            checkoutRequest(loadedAmount, loadedCurrency)
        }

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
            OutlinedButton(
                onClick = {
                    isVaulting.value = true
                    vaultHelper.start()
                },
                enabled = !isVaulting.value,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    stringResource(
                        if (isVaulting.value) {
                            R.string.paypal_saved_payment_method_vaulting
                        } else {
                            R.string.paypal_saved_payment_method_vault
                        }
                    )
                )
            }
            Text(
                text = stringResource(R.string.paypal_saved_payment_method_vault_caption),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = savedToken.value?.let { stringResource(R.string.paypal_saved_payment_method_token_value, it) }
                    ?: stringResource(R.string.paypal_saved_payment_method_token_none),
                style = MaterialTheme.typography.bodyMedium
            )
            HorizontalDivider()
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
            OutlinedTextField(
                value = currency,
                onValueChange = { currency = it },
                label = { Text(stringResource(R.string.paypal_saved_payment_method_currency_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
            )
            OutlinedButton(onClick = { isStyleEditorOpen = true }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.paypal_saved_payment_method_edit_style))
            }
            Button(
                onClick = {
                    val loadAmount = validAmountOrNull(amount)
                    val loadCurrency = validCurrencyOrNull(currency)
                    if (loadAmount == null || loadCurrency == null) {
                        showDialog(
                            getString(
                                if (loadAmount == null) {
                                    R.string.paypal_saved_payment_method_invalid_amount
                                } else {
                                    R.string.paypal_saved_payment_method_invalid_currency
                                }
                            )
                        )
                        return@Button
                    }
                    isLoading = true
                    Merchant().fetchSavedPaymentMethodClientToken(requireContext(), clientToken) { result ->
                        isLoading = false
                        if (!isAdded) return@fetchSavedPaymentMethodClientToken
                        when (result) {
                            is FetchClientTokenResult.Success -> {
                                nonceState.value = null
                                loadedClientToken = result.clientToken
                                loadedAmount = loadAmount
                                loadedCurrency = loadCurrency
                                loadCount++
                            }
                            is FetchClientTokenResult.Error -> handleError(result.error)
                        }
                    }
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    stringResource(
                        if (isLoading) {
                            R.string.paypal_saved_payment_method_loading
                        } else {
                            R.string.paypal_saved_payment_method_load
                        }
                    )
                )
            }

            if (loadedClientToken.isNotBlank()) {
                HorizontalDivider()
                key(loadCount) {
                    PayPalSavedPaymentMethodView(
                        payPalCheckoutRequest = checkoutRequest,
                        request = PayPalSavedPaymentMethodRequest(amount = loadedAmount, currencyCode = loadedCurrency),
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
        // Picks up a token entered in Settings.
        savedToken.value = Settings.getSavedPaymentMethodToken(requireContext())
        vaultHelper.handleReturn()
    }

    private fun checkoutRequest(amount: String, currency: String) = PayPalCheckoutRequest(
        amount = amount,
        hasUserLocationConsent = true,
        currencyCode = currency,
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
        const val CLIENT_TOKEN_MAX_LINES = 4
    }
}
