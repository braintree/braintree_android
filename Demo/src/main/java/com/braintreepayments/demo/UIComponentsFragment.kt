package com.braintreepayments.demo

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.net.toUri
import androidx.navigation.fragment.NavHostFragment
import com.braintreepayments.api.card.Card
import com.braintreepayments.api.core.PaymentMethodNonce
import com.braintreepayments.api.paypal.PayPalPendingRequest
import com.braintreepayments.api.paypal.PayPalRequest
import com.braintreepayments.api.paypal.PayPalResult
import com.braintreepayments.api.paypal.PayPalTokenizeCallback
import com.braintreepayments.api.uicomponents.PayPalButton
import com.braintreepayments.api.uicomponents.PayPalButtonColor
import com.braintreepayments.api.uicomponents.PayPalLaunchCallback
import com.braintreepayments.api.uicomponents.VenmoButton
import com.braintreepayments.api.uicomponents.VenmoButtonColor
import com.braintreepayments.api.uicomponents.VenmoLaunchCallback
import com.braintreepayments.api.uicomponents.cardfields.CardFields
import com.braintreepayments.api.uicomponents.cardfields.CardFieldsResult
import com.braintreepayments.api.venmo.VenmoAccountNonce
import com.braintreepayments.api.venmo.VenmoLineItem
import com.braintreepayments.api.venmo.VenmoLineItemKind
import com.braintreepayments.api.venmo.VenmoPaymentMethodUsage
import com.braintreepayments.api.venmo.VenmoPendingRequest
import com.braintreepayments.api.venmo.VenmoRequest
import com.braintreepayments.api.venmo.VenmoResult
import com.braintreepayments.api.venmo.VenmoTokenizeCallback
import com.google.android.material.button.MaterialButtonToggleGroup

@Suppress("TooManyFunctions")
class UIComponentsFragment : BaseFragment() {
    private lateinit var payPalButton: PayPalButton
    private lateinit var payPalToggleGroup: MaterialButtonToggleGroup
    private lateinit var venmoButton: VenmoButton
    private lateinit var venmoRequest: VenmoRequest
    private lateinit var venmoToggleGroup: MaterialButtonToggleGroup

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_ui_components, container, false)
        payPalButton = view.findViewById(R.id.pp_payment_button)
        payPalToggleGroup = view.findViewById(R.id.pp_button_toggle_group)
        venmoButton = view.findViewById(R.id.venmo_payment_button)
        venmoToggleGroup = view.findViewById(R.id.venmo_button_toggle_group)

        setupCardFields(view)
        setupColorToggles()
        setupPayPalButton()
        setupVenmoButton()
        return view
    }

    private fun setupCardFields(view: View) {
        val cardFields = view.findViewById<CardFields>(R.id.card_fields)
        val payButton = view.findViewById<Button>(R.id.pay_button)
        cardFields.initialize(authStringArg)
        // optional customer data
        cardFields.setPaymentRequest(
            Card(
                cardholderName = "John Doe",
                postalCode = "12345"
            )
        )

        cardFields.setCardFieldsResultCallback { result ->
            when (result) {
                is CardFieldsResult.Success -> {
                    // handle nonce
                    super.onPaymentMethodNonceCreated(result.nonce)
                    val action = UIComponentsFragmentDirections
                        .actionUiComponentsFragmentToDisplayNonceFragment(result.nonce)
                    NavHostFragment.Companion.findNavController(this).navigate(action)
                }
                is CardFieldsResult.Failure -> {
                    // display error
                    handleError(result.error)
                }
            }
        }

        // Card fields will tell the merchant if the card is valid, and they can enable the button
        cardFields.setOnValidationChangedListener { isFormValid ->
            payButton.isEnabled = isFormValid
        }

        // submit card info for processing
        payButton.setOnClickListener {
            cardFields.submit()
        }
    }

    private fun setupColorToggles() {
        payPalToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.button_pp_blue -> payPalButton.setButtonColor(PayPalButtonColor.Blue)
                    R.id.button_pp_black -> payPalButton.setButtonColor(PayPalButtonColor.Black)
                    R.id.button_pp_white -> payPalButton.setButtonColor(PayPalButtonColor.White)
                }
            }
        }
        venmoToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.button_venmo_blue -> venmoButton.setButtonColor(VenmoButtonColor.Blue)
                    R.id.button_venmo_black -> venmoButton.setButtonColor(VenmoButtonColor.Black)
                    R.id.button_venmo_white -> venmoButton.setButtonColor(VenmoButtonColor.White)
                }
            }
        }
    }

    private fun setupPayPalButton() {
        val payPalRequest: PayPalRequest = PayPalRequestFactory.createPayPalCheckoutRequest(
            requireContext(),
            "10.0",
            null,
            null,
            null,
            false,
            null,
            false,
            false,
            false
        )

        payPalButton.initialize(
            this,
            super.getAuthStringArg(),
            "https://mobile-sdk-demo-site-838cead5d3ab.herokuapp.com/braintree-payments".toUri(),
            "com.braintreepayments.demo.braintree"
        )
        payPalButton.setPayPalRequest(payPalRequest)
        payPalButton.payPalLaunchCallback = PayPalLaunchCallback { request: PayPalPendingRequest? ->
            if (request is PayPalPendingRequest.Started) {
                storePayPalPendingRequest(request)
            } else if (request is PayPalPendingRequest.Failure) {
                handleError(request.error)
            }
        }
    }

    private fun setupVenmoButton() {
        val activity = requireActivity()
        activity.setProgressBarIndeterminateVisibility(true)

        val shouldVault =
            Settings.vaultVenmo(activity) && !TextUtils.isEmpty(Settings.getCustomerId(activity))

        val venmoPaymentMethodUsage =
            if (shouldVault) VenmoPaymentMethodUsage.MULTI_USE else VenmoPaymentMethodUsage.SINGLE_USE
        venmoRequest = VenmoRequest(venmoPaymentMethodUsage).apply {
            profileId = null
            this.shouldVault = shouldVault
            collectCustomerBillingAddress = true
            collectCustomerShippingAddress = true
            totalAmount = "20"
            subTotalAmount = "18"
            taxAmount = "1"
            shippingAmount = "1"
            lineItems = arrayListOf(
                VenmoLineItem(VenmoLineItemKind.CREDIT, "Some Item", 1, "2"),
                VenmoLineItem(VenmoLineItemKind.DEBIT, "Two Items", 2, "10")
            )
        }

        venmoButton.initialize(
            this,
            super.getAuthStringArg(),
            "https://mobile-sdk-demo-site-838cead5d3ab.herokuapp.com/braintree-payments".toUri(),
            "com.braintreepayments.demo.braintree"
        )

        venmoButton.setVenmoRequest(venmoRequest)
        venmoButton.venmoLaunchCallback = VenmoLaunchCallback { request: VenmoPendingRequest? ->
            if (request is VenmoPendingRequest.Started) {
                storeVenmoPendingRequest(request)
            } else if (request is VenmoPendingRequest.Failure) {
                handleError(request.error)
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // PayPal flow after returning to app
        val pendingRequest = getPayPalPendingRequest()
        if (pendingRequest != null) {
            payPalButton.handleReturnToApp(
                pendingRequest, requireActivity().intent,
                PayPalTokenizeCallback { payPalResult: PayPalResult? ->
                    when (payPalResult) {
                        is PayPalResult.Success -> handlePayPalResult(payPalResult.nonce)
                        is PayPalResult.Cancel ->
                            handleError(Exception("User did not complete payment flow"))
                        is PayPalResult.Failure -> handleError(payPalResult.error)
                        else -> Unit
                    }
                }
            )
            clearPayPalPendingRequest()
            requireActivity().intent.data = null
        }

        // Venmo flow after returning to app
        val venmoPendingRequest = getVenmoPendingRequest()
        if (venmoPendingRequest != null) {
            venmoButton.handleReturnToApp(
                venmoPendingRequest,
                requireActivity().intent,
                VenmoTokenizeCallback { venmoResult: VenmoResult? ->
                    when (venmoResult) {
                        is VenmoResult.Success -> handleVenmoAccountNonce(venmoResult.nonce)
                        is VenmoResult.Failure -> handleError(venmoResult.error)
                        is VenmoResult.Cancel ->
                            handleError(Exception("User did not complete payment flow"))
                        else -> Unit
                    }
                }
            )
            clearVenmoPendingRequest()
            requireActivity().intent.data = null
        }
    }

    private fun handleVenmoAccountNonce(venmoAccountNonce: VenmoAccountNonce) {
        onPaymentMethodNonceCreated(venmoAccountNonce)
        val action = UIComponentsFragmentDirections
            .actionUiComponentsFragmentToDisplayNonceFragment(venmoAccountNonce)
        NavHostFragment.findNavController(this).navigate(action)
    }

    private fun handlePayPalResult(paymentMethodNonce: PaymentMethodNonce?) {
        if (paymentMethodNonce != null) {
            onPaymentMethodNonceCreated(paymentMethodNonce)
            val action = UIComponentsFragmentDirections
                .actionUiComponentsFragmentToDisplayNonceFragment(paymentMethodNonce)
            NavHostFragment.findNavController(this).navigate(action)
        }
    }

    private fun storeVenmoPendingRequest(request: VenmoPendingRequest.Started) =
        PendingRequestStore.getInstance().putVenmoPendingRequest(requireContext(), request)

    private fun getVenmoPendingRequest(): VenmoPendingRequest.Started? =
        PendingRequestStore.getInstance().getVenmoPendingRequest(requireContext())

    private fun clearVenmoPendingRequest() =
        PendingRequestStore.getInstance().clearVenmoPendingRequest(requireContext())

    private fun storePayPalPendingRequest(request: PayPalPendingRequest.Started) =
        PendingRequestStore.getInstance().putPayPalPendingRequest(requireContext(), request)

    private fun getPayPalPendingRequest(): PayPalPendingRequest.Started? =
        PendingRequestStore.getInstance().getPayPalPendingRequest(requireContext())

    private fun clearPayPalPendingRequest() =
        PendingRequestStore.getInstance().clearPayPalPendingRequest(requireContext())
}
