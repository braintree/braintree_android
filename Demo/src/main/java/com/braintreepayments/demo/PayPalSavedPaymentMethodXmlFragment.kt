package com.braintreepayments.demo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.net.toUri
import androidx.core.os.BundleCompat
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.paypal.PayPalAccountNonce
import com.braintreepayments.api.paypal.PayPalCheckoutRequest
import com.braintreepayments.api.paypal.PayPalPaymentUserAction
import com.braintreepayments.api.paypal.PayPalResult
import com.braintreepayments.api.paypal.PayPalTokenizeCallback
import com.braintreepayments.api.paypalsavedpaymentmethod.PayPalSavedPaymentMethodRequest
import com.braintreepayments.api.paypalsavedpaymentmethod.xml.PayPalSavedPaymentMethodLaunchCallback
import com.braintreepayments.api.paypalsavedpaymentmethod.xml.PayPalSavedPaymentMethodView
import com.braintreepayments.demo.SavedPaymentMethodDemo.APP_LINK_RETURN_URL
import com.braintreepayments.demo.SavedPaymentMethodDemo.DEEP_LINK_FALLBACK_URL_SCHEME
import com.braintreepayments.demo.SavedPaymentMethodDemo.DEFAULT_AMOUNT
import com.braintreepayments.demo.SavedPaymentMethodDemo.DEFAULT_CURRENCY
import com.braintreepayments.demo.SavedPaymentMethodDemo.validAmountOrNull
import com.braintreepayments.demo.SavedPaymentMethodDemo.validCurrencyOrNull

/**
 * Demonstrates the XML `PayPalSavedPaymentMethodView`. The component needs a client token that carries the buyer's
 * `paymentMethodIdJwt`: Load fetches one from the merchant server with the token saved by Vault PayPal Account, or uses
 * a pasted one. Unlike the Compose view, the merchant stores the pending request, passes it back in `onResume`, and
 * clears it once the result arrives.
 */
@OptIn(ExperimentalBetaApi::class)
class PayPalSavedPaymentMethodXmlFragment : BaseFragment() {

    private lateinit var savedPaymentMethodView: PayPalSavedPaymentMethodView
    private lateinit var nonceView: TextView
    private lateinit var createTransactionButton: Button
    private lateinit var vaultButton: Button
    private lateinit var loadButton: Button
    private lateinit var savedTokenView: TextView
    private lateinit var vaultHelper: SavedPaymentMethodVaultHelper

    // The token, amount and currency the component was last loaded with, kept so it can be set up again after
    // recreation.
    private var loadedClientToken: String? = null
    private var loadedAmount: String = DEFAULT_AMOUNT
    private var loadedCurrency: String = DEFAULT_CURRENCY
    private var styleConfig by mutableStateOf(PayPalSavedPaymentMethodStyleConfig())
    private var isStyleEditorOpen by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vaultHelper = SavedPaymentMethodVaultHelper(this) { result ->
            if (!isAdded || view == null) return@SavedPaymentMethodVaultHelper
            vaultButton.showBusy(isBusy = false, VAULTING_TEXT, VAULT_TEXT)
            result
                .onSuccess {
                    showSavedToken()
                    showDialog(getString(R.string.paypal_saved_payment_method_vault_saved))
                }
                .onFailure { handleError(it as? Exception ?: Exception(it)) }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_paypal_saved_payment_method_xml, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        savedPaymentMethodView = view.findViewById(R.id.saved_payment_method_view)
        nonceView = view.findViewById(R.id.nonce)
        createTransactionButton = view.findViewById(R.id.create_transaction)
        val clientTokenField = view.findViewById<EditText>(R.id.client_token)
        val amountField = view.findViewById<EditText>(R.id.amount)
        val currencyField = view.findViewById<EditText>(R.id.currency)
        // The fields keep the buyer's own values when recreated.
        if (savedInstanceState == null) {
            amountField.setText(DEFAULT_AMOUNT)
            currencyField.setText(DEFAULT_CURRENCY)
        }

        vaultButton = view.findViewById(R.id.vault)
        vaultButton.setOnClickListener {
            vaultButton.showBusy(isBusy = true, VAULTING_TEXT, VAULT_TEXT)
            vaultHelper.start()
        }

        savedTokenView = view.findViewById(R.id.saved_token)

        loadButton = view.findViewById(R.id.load)
        loadButton.setOnClickListener {
            val amount = validAmountOrNull(amountField.text.toString())
            val currency = validCurrencyOrNull(currencyField.text.toString())
            if (amount == null || currency == null) {
                showDialog(getString(if (amount == null) INVALID_AMOUNT_TEXT else INVALID_CURRENCY_TEXT))
                return@setOnClickListener
            }
            loadButton.showBusy(isBusy = true, LOADING_TEXT, LOAD_TEXT)
            Merchant().fetchSavedPaymentMethodClientToken(requireContext(), clientTokenField.text.toString()) {
                if (this.view == null) return@fetchSavedPaymentMethodClientToken
                loadButton.showBusy(isBusy = false, LOADING_TEXT, LOAD_TEXT)
                when (it) {
                    is FetchClientTokenResult.Success -> {
                        nonceView.isVisible = false
                        createTransactionButton.isVisible = false
                        load(it.clientToken, amount, currency)
                    }
                    is FetchClientTokenResult.Error -> handleError(it.error)
                }
            }
        }
        savedInstanceState?.let {
            BundleCompat.getSerializable(it, KEY_STYLE_CONFIG, PayPalSavedPaymentMethodStyleConfig::class.java)
        }?.let { restored ->
            styleConfig = restored
            savedPaymentMethodView.style = restored.toStyle()
        }
        setUpStyleEditor(view)

        savedInstanceState?.getString(KEY_CLIENT_TOKEN)?.let { clientToken ->
            val amount = savedInstanceState.getString(KEY_AMOUNT, DEFAULT_AMOUNT)
            load(clientToken, amount, savedInstanceState.getString(KEY_CURRENCY, DEFAULT_CURRENCY))
        }
    }

    override fun onResume() {
        super.onResume()
        // Picks up a token entered in Settings.
        showSavedToken()
        vaultHelper.handleReturn()
        val store = PendingRequestStore.getInstance()
        store.getPayPalSavedPaymentMethodPendingRequest(requireContext())?.let { pendingRequest ->
            if (savedPaymentMethodView.handleReturnToApp(pendingRequest, requireActivity().intent)) {
                store.clearPayPalSavedPaymentMethodPendingRequest(requireContext())
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // A client token in the saved state is acceptable for the Demo; an app shouldn't persist one this way.
        loadedClientToken?.let { outState.putString(KEY_CLIENT_TOKEN, it) }
        outState.putString(KEY_AMOUNT, loadedAmount)
        outState.putString(KEY_CURRENCY, loadedCurrency)
        outState.putSerializable(KEY_STYLE_CONFIG, styleConfig)
    }

    private fun setUpStyleEditor(view: View) {
        view.findViewById<Button>(R.id.edit_style).setOnClickListener { isStyleEditorOpen = true }
        view.findViewById<ComposeView>(R.id.style_editor).setContent {
            if (isStyleEditorOpen) {
                PayPalSavedPaymentMethodStyleEditor(
                    config = styleConfig,
                    onConfigChange = {
                        styleConfig = it
                        savedPaymentMethodView.style = it.toStyle()
                    },
                    onDismiss = { isStyleEditorOpen = false }
                )
            }
        }
    }

    private fun showSavedToken() {
        savedTokenView.text = Settings.getSavedPaymentMethodToken(requireContext())
            ?.let { getString(R.string.paypal_saved_payment_method_token_value, it) }
            ?: getString(R.string.paypal_saved_payment_method_token_none)
    }

    private fun load(clientToken: String, amount: String, currency: String) {
        loadedClientToken = clientToken
        loadedAmount = amount
        loadedCurrency = currency
        savedPaymentMethodView.isVisible = true
        savedPaymentMethodView.initialize(
            authorization = clientToken,
            appLinkReturnUrl = APP_LINK_RETURN_URL.toUri(),
            payPalCheckoutRequest = checkoutRequest(amount, currency),
            request = PayPalSavedPaymentMethodRequest(amount = amount, currencyCode = currency),
            launchCallback = launchCallback,
            tokenizeCallback = tokenizeCallback,
            deepLinkFallbackUrlScheme = DEEP_LINK_FALLBACK_URL_SCHEME
        )
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

    private val launchCallback = PayPalSavedPaymentMethodLaunchCallback { pendingRequest ->
        PendingRequestStore.getInstance().putPayPalSavedPaymentMethodPendingRequest(requireContext(), pendingRequest)
    }

    private val tokenizeCallback = PayPalTokenizeCallback { result ->
        PendingRequestStore.getInstance().clearPayPalSavedPaymentMethodPendingRequest(requireContext())
        when (result) {
            is PayPalResult.Success -> showNonce(result.nonce)
            is PayPalResult.Cancel -> handleError(Exception(getString(R.string.paypal_saved_payment_method_canceled)))
            is PayPalResult.Failure -> handleError(result.error)
        }
    }

    private fun showNonce(nonce: PayPalAccountNonce) {
        onPaymentMethodNonceCreated(nonce)
        nonceView.text = getString(R.string.nonce_placeholder, nonce.string)
        nonceView.isVisible = true
        createTransactionButton.isVisible = true
        createTransactionButton.setOnClickListener {
            val action = PayPalSavedPaymentMethodXmlFragmentDirections
                .actionPayPalSavedPaymentMethodXmlFragmentToCreateTransactionFragment(nonce)
                .setTransactionAmount(loadedAmount)
            findNavController().navigate(action)
        }
    }

    private companion object {
        val VAULTING_TEXT = R.string.paypal_saved_payment_method_vaulting
        val VAULT_TEXT = R.string.paypal_saved_payment_method_vault
        val INVALID_AMOUNT_TEXT = R.string.paypal_saved_payment_method_invalid_amount
        val INVALID_CURRENCY_TEXT = R.string.paypal_saved_payment_method_invalid_currency
        val LOADING_TEXT = R.string.paypal_saved_payment_method_loading
        val LOAD_TEXT = R.string.paypal_saved_payment_method_load
        const val KEY_CLIENT_TOKEN = "client_token"
        const val KEY_AMOUNT = "amount"
        const val KEY_CURRENCY = "currency"
        const val KEY_STYLE_CONFIG = "style_config"
    }
}

// Disables the button and shows busyText while the action runs.
private fun Button.showBusy(isBusy: Boolean, @StringRes busyText: Int, @StringRes idleText: Int) {
    isEnabled = !isBusy
    setText(if (isBusy) busyText else idleText)
}
