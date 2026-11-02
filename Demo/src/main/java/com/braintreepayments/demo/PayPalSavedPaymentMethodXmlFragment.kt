package com.braintreepayments.demo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
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
 * Demonstrates the XML `PayPalSavedPaymentMethodView`. The component needs a client token generated with the buyer's
 * payment method ID, so paste one that carries a `paymentMethodIdJwt`. Unlike the Compose view, the merchant stores
 * the pending request, passes it back in `onResume`, and clears it once the result arrives.
 */
@OptIn(ExperimentalBetaApi::class)
class PayPalSavedPaymentMethodXmlFragment : BaseFragment() {

    private lateinit var savedPaymentMethodView: PayPalSavedPaymentMethodView
    private lateinit var nonceView: TextView
    private lateinit var createTransactionButton: Button

    // The token and amount the component was last loaded with, kept so it can be set up again after recreation.
    private var loadedClientToken: String? = null
    private var loadedAmount: String = DEFAULT_AMOUNT
    private var styleConfig by mutableStateOf(PayPalSavedPaymentMethodStyleConfig())
    private var isStyleEditorOpen by mutableStateOf(false)

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

        view.findViewById<Button>(R.id.load).setOnClickListener {
            val clientToken = clientTokenField.text.toString().trim()
            if (clientToken.isBlank()) {
                showDialog(getString(R.string.paypal_saved_payment_method_missing_token))
            } else {
                nonceView.isVisible = false
                createTransactionButton.isVisible = false
                load(clientToken, amountField.text.toString().trim())
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
        const val CURRENCY_CODE = "USD"
        const val KEY_CLIENT_TOKEN = "client_token"
        const val KEY_AMOUNT = "amount"
        const val APP_LINK_RETURN_URL = "https://mobile-sdk-demo-site-838cead5d3ab.herokuapp.com/braintree-payments"
        const val DEEP_LINK_FALLBACK_URL_SCHEME = "com.braintreepayments.demo.braintree"
    }
}
