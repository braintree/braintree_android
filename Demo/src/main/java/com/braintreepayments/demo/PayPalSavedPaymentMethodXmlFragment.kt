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

    // The token and amount the component was last loaded with, kept so it can be set up again after recreation.
    private var loadedClientToken: String? = null
    private var loadedAmount: String = DEFAULT_AMOUNT
    private var styleConfig by mutableStateOf(PayPalSavedPaymentMethodStyleConfig())
    private var isStyleEditorOpen by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vaultHelper = SavedPaymentMethodVaultHelper(this) { result ->
            if (!isAdded || view == null) return@SavedPaymentMethodVaultHelper
            vaultButton.showBusy(
                isBusy = false,
                busyText = R.string.paypal_saved_payment_method_vaulting,
                idleText = R.string.paypal_saved_payment_method_vault,
                isEnabledWhenIdle = Settings.isSandbox(requireContext())
            )
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
        // The field keeps the buyer's own amount when recreated.
        if (savedInstanceState == null) amountField.setText(DEFAULT_AMOUNT)

        val isSandbox = Settings.isSandbox(requireContext())
        vaultButton = view.findViewById(R.id.vault)
        vaultButton.isEnabled = isSandbox
        vaultButton.setOnClickListener {
            vaultButton.showBusy(
                isBusy = true,
                busyText = R.string.paypal_saved_payment_method_vaulting,
                idleText = R.string.paypal_saved_payment_method_vault
            )
            vaultHelper.start()
        }
        // The Demo merchant is also used for Production testing, where this would vault a real PayPal account.
        view.findViewById<TextView>(R.id.vault_caption).setText(
            if (isSandbox) {
                R.string.paypal_saved_payment_method_vault_caption
            } else {
                R.string.paypal_saved_payment_method_vault_sandbox_only
            }
        )

        savedTokenView = view.findViewById(R.id.saved_token)

        loadButton = view.findViewById(R.id.load)
        loadButton.setOnClickListener {
            loadButton.showBusy(isBusy = true, LOADING_TEXT, LOAD_TEXT)
            SavedPaymentMethodVaultHelper.fetchClientToken(requireContext(), clientTokenField.text.toString()) {
                if (this.view == null) return@fetchClientToken
                loadButton.showBusy(isBusy = false, LOADING_TEXT, LOAD_TEXT)
                it.onSuccess { clientToken ->
                    nonceView.isVisible = false
                    createTransactionButton.isVisible = false
                    load(clientToken, amountField.text.toString().trim())
                }.onFailure { error -> handleError(error as? Exception ?: Exception(error)) }
            }
        }
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

        savedInstanceState?.getString(KEY_CLIENT_TOKEN)?.let { clientToken ->
            load(clientToken, savedInstanceState.getString(KEY_AMOUNT) ?: DEFAULT_AMOUNT)
        }
    }

    override fun onResume() {
        super.onResume()
        // Picks up a token entered in Settings.
        showSavedToken()
        vaultHelper.handleReturn()
        val store = PendingRequestStore.getInstance()
        store.getPayPalPendingRequest(requireContext())?.let { pendingRequest ->
            if (savedPaymentMethodView.handleReturnToApp(pendingRequest, requireActivity().intent)) {
                store.clearPayPalPendingRequest(requireContext())
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        loadedClientToken?.let { outState.putString(KEY_CLIENT_TOKEN, it) }
        outState.putString(KEY_AMOUNT, loadedAmount)
    }

    private fun showSavedToken() {
        savedTokenView.text = Settings.getSavedPaymentMethodToken(requireContext())
            ?.let { getString(R.string.paypal_saved_payment_method_token_value, it) }
            ?: getString(R.string.paypal_saved_payment_method_token_none)
    }

    private fun load(clientToken: String, amount: String) {
        loadedClientToken = clientToken
        loadedAmount = amount
        savedPaymentMethodView.isVisible = true
        savedPaymentMethodView.initialize(
            authorization = clientToken,
            appLinkReturnUrl = APP_LINK_RETURN_URL.toUri(),
            payPalCheckoutRequest = checkoutRequest(amount),
            request = PayPalSavedPaymentMethodRequest(amount = amount, currencyCode = CURRENCY_CODE),
            launchCallback = launchCallback,
            tokenizeCallback = tokenizeCallback,
            deepLinkFallbackUrlScheme = DEEP_LINK_FALLBACK_URL_SCHEME
        )
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

    private val launchCallback = PayPalSavedPaymentMethodLaunchCallback { pendingRequest ->
        PendingRequestStore.getInstance().putPayPalPendingRequest(requireContext(), pendingRequest)
    }

    private val tokenizeCallback = PayPalTokenizeCallback { result ->
        PendingRequestStore.getInstance().clearPayPalPendingRequest(requireContext())
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
        const val DEFAULT_AMOUNT = "10.00"
        val LOADING_TEXT = R.string.paypal_saved_payment_method_loading
        val LOAD_TEXT = R.string.paypal_saved_payment_method_load
        const val CURRENCY_CODE = "USD"
        const val KEY_CLIENT_TOKEN = "client_token"
        const val KEY_AMOUNT = "amount"
        const val APP_LINK_RETURN_URL = "https://mobile-sdk-demo-site-838cead5d3ab.herokuapp.com/braintree-payments"
        const val DEEP_LINK_FALLBACK_URL_SCHEME = "com.braintreepayments.demo.braintree"
    }
}

// Disables the button and shows busyText while the action runs.
private fun Button.showBusy(
    isBusy: Boolean,
    @StringRes busyText: Int,
    @StringRes idleText: Int,
    isEnabledWhenIdle: Boolean = true
) {
    isEnabled = !isBusy && isEnabledWhenIdle
    setText(if (isBusy) busyText else idleText)
}
