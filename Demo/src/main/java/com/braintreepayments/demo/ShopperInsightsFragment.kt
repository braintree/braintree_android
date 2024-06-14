package com.braintreepayments.demo

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.navigation.fragment.NavHostFragment
import com.braintreepayments.api.ExperimentalBetaApi
import com.braintreepayments.api.ShopperInsightsBuyerPhone
import com.braintreepayments.api.ShopperInsightsClient
import com.braintreepayments.api.ShopperInsightsRequest
import com.braintreepayments.api.ShopperInsightsResult
import com.braintreepayments.api.core.BraintreeClient
import com.braintreepayments.api.paypal.PayPalClient
import com.braintreepayments.api.paypal.PayPalLauncher
import com.braintreepayments.api.paypal.PayPalPaymentAuthRequest
import com.braintreepayments.api.paypal.PayPalPaymentAuthResult
import com.braintreepayments.api.paypal.PayPalPendingRequest
import com.braintreepayments.api.paypal.PayPalResult
import com.braintreepayments.api.venmo.VenmoClient
import com.braintreepayments.api.venmo.VenmoLauncher
import com.braintreepayments.api.venmo.VenmoPaymentAuthRequest
import com.braintreepayments.api.venmo.VenmoPaymentAuthResult
import com.braintreepayments.api.venmo.VenmoPaymentMethodUsage
import com.braintreepayments.api.venmo.VenmoPendingRequest
import com.braintreepayments.api.venmo.VenmoRequest
import com.braintreepayments.api.venmo.VenmoResult
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputLayout

/**
 * Fragment for handling shopping insights.
 */
@OptIn(ExperimentalBetaApi::class)
class ShopperInsightsFragment : BaseFragment() {

    private lateinit var responseTextView: TextView
    private lateinit var actionButton: Button
    private lateinit var payPalVaultButton: Button
    private lateinit var venmoButton: Button
    private lateinit var emailInput: TextInputLayout
    private lateinit var countryCodeInput: TextInputLayout
    private lateinit var nationalNumberInput: TextInputLayout
    private lateinit var emailNullSwitch: SwitchMaterial
    private lateinit var phoneNullSwitch: SwitchMaterial

    private lateinit var braintreeClient: BraintreeClient
    private lateinit var shopperInsightsClient: ShopperInsightsClient
    private lateinit var payPalClient: PayPalClient
    private lateinit var venmoClient: VenmoClient

    private val venmoLauncher: VenmoLauncher = VenmoLauncher()
    private val paypalLauncher: PayPalLauncher = PayPalLauncher()

    private lateinit var venmoPendingRequest: VenmoPendingRequest
    private lateinit var paypalPendingRequest: PayPalPendingRequest

    // TODO: Refactor Shopper Insights to remove BraintreeClient
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        braintreeClient = BraintreeClient(requireContext(), authStringArg)
        shopperInsightsClient = ShopperInsightsClient(braintreeClient)

        venmoClient = VenmoClient(requireContext(), super.getAuthStringArg())

        // TODO: What does manual browser switch do?
//        val useManualBrowserSwitch = Settings.isManualBrowserSwitchingEnabled(requireActivity())
        payPalClient = PayPalClient(requireContext(), super.getAuthStringArg())

        return inflater.inflate(R.layout.fragment_shopping_insights, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViews(view)

        actionButton.setOnClickListener { fetchShopperInsights() }
        venmoButton.setOnClickListener { launchVenmo() }
        payPalVaultButton.setOnClickListener { launchPayPalVault() }
    }

    private fun initializeViews(view: View) {
        responseTextView = view.findViewById(R.id.responseTextView)
        actionButton = view.findViewById(R.id.actionButton)
        payPalVaultButton = view.findViewById(R.id.payPalVaultButton)
        venmoButton = view.findViewById(R.id.venmoButton)
        emailInput = view.findViewById(R.id.emailInput)
        countryCodeInput = view.findViewById(R.id.countryCodeInput)
        nationalNumberInput = view.findViewById(R.id.nationalNumberInput)
        emailNullSwitch = view.findViewById(R.id.emailNullSwitch)
        phoneNullSwitch = view.findViewById(R.id.phoneNullSwitch)

        emailInput.editText?.setText("PR1_merchantname@personal.example.com")
        nationalNumberInput.editText?.setText("4082321001")
        countryCodeInput.editText?.setText("1")
    }

    override fun onResume() {
        super.onResume()
        handleVenmoReturnToApp()
        handlePayPalReturnToApp()
    }

    private fun handlePayPalReturnToApp() {
        if(this::paypalPendingRequest.isInitialized) {
            when(val request = paypalPendingRequest) {
                is PayPalPendingRequest.Started -> {
                    val paypalPaymentAuthResult =
                        paypalLauncher.handleReturnToAppFromBrowser(request, Intent())
                    if (paypalPaymentAuthResult is PayPalPaymentAuthResult.Success) {
                        payPalClient.tokenize(paypalPaymentAuthResult) {
                            when (it) {
                                is PayPalResult.Success -> {
                                    val action = ShopperInsightsFragmentDirections.actionShopperInsightsFragmentToDisplayNonceFragment(
                                        it.nonce
                                    )
                                    NavHostFragment.findNavController(this).navigate(action)
                                }
                                is PayPalResult.Failure -> {}
                                is PayPalResult.Cancel -> {}
                            }
                        }
                    } else {
                        // No PayPal result i.e. not auth'd in
                        Toast.makeText(requireContext(), "Failed to authenticate user.", Toast.LENGTH_LONG)
                            .show()
                    }
                }
                is PayPalPendingRequest.Failure -> {

                }
            }
        }
    }

    private fun handleVenmoReturnToApp() {
        if(this::venmoPendingRequest.isInitialized) {
            when (val request = venmoPendingRequest) {
                is VenmoPendingRequest.Started -> {
                    val venmoPaymentAuthResult = venmoLauncher.handleReturnToApp(request, Intent())
                    if (venmoPaymentAuthResult is VenmoPaymentAuthResult.Success) {
                        venmoClient.tokenize(venmoPaymentAuthResult) {
                            when (it) {
                                is VenmoResult.Success -> {
                                    val action = ShopperInsightsFragmentDirections.actionShopperInsightsFragmentToDisplayNonceFragment(
                                        it.nonce
                                    )
                                    NavHostFragment.findNavController(this).navigate(action)
                                }
                                is VenmoResult.Failure -> {}
                                is VenmoResult.Cancel -> {}
                            }
                        }
                    } else {
                        // No venmo result i.e. not auth'd in
                        Toast.makeText(requireContext(), "Failed to authenticate user.", Toast.LENGTH_LONG)
                            .show()
                    }
                }

                is VenmoPendingRequest.Failure -> {}
            }
        }
    }

    private fun fetchShopperInsights() {
        val email =
            if (emailNullSwitch.isChecked) null else emailInput.editText?.text.toString()
        val countryCode =
            if (phoneNullSwitch.isChecked) null else countryCodeInput.editText?.text.toString()
        val nationalNumber =
            if (phoneNullSwitch.isChecked) null else nationalNumberInput.editText?.text.toString()

        val request = if (countryCode != null && nationalNumber != null) {
            ShopperInsightsRequest(email, ShopperInsightsBuyerPhone(countryCode, nationalNumber))
        } else {
            ShopperInsightsRequest(email, null)
        }

        shopperInsightsClient.getRecommendedPaymentMethods(
            request
        ) { result ->
            when (result) {
                is ShopperInsightsResult.Success -> {
                    if (result.response.isPayPalRecommended) {
                        payPalVaultButton.isEnabled = true
                        shopperInsightsClient.sendPayPalPresentedEvent()
                    }

                    if (result.response.isVenmoRecommended) {
                        venmoButton.isEnabled = true
                        shopperInsightsClient.sendVenmoPresentedEvent()
                    }

                    responseTextView.text =
                        """
                            Eligible in PayPal Network: ${result.response.isEligibleInPayPalNetwork}
                            PayPal Recommended: ${result.response.isPayPalRecommended}
                            Venmo Recommended: ${result.response.isVenmoRecommended}
                        """.trimIndent()
                }

                is ShopperInsightsResult.Failure -> {
                    responseTextView.text = result.error.toString()
                }
            }
        }
    }

    private fun launchPayPalVault() {
        shopperInsightsClient.sendPayPalSelectedEvent()

        payPalClient.createPaymentAuthRequest(
            requireContext(),
            PayPalRequestFactory.createPayPalVaultRequest(
                activity,
                emailInput.editText?.text.toString()
            )
        ) {
            if (it==null) return@createPaymentAuthRequest
            when(it) {
                is PayPalPaymentAuthRequest.Failure -> { handleError(it.error) }
                is PayPalPaymentAuthRequest.ReadyToLaunch -> {
                    paypalPendingRequest = paypalLauncher.launch(requireActivity(), it)
                }
            }
        }
    }

    private fun launchVenmo() {
        shopperInsightsClient.sendVenmoSelectedEvent()

        val venmoRequest = VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE)
        venmoRequest.profileId = null
        venmoRequest.collectCustomerBillingAddress = true
        venmoRequest.collectCustomerShippingAddress = true
        venmoRequest.totalAmount = "20"
        venmoRequest.subTotalAmount = "18"
        venmoRequest.taxAmount = "1"

        venmoClient.createPaymentAuthRequest(requireContext(), venmoRequest) {
            when(it) {
                is VenmoPaymentAuthRequest.ReadyToLaunch -> {
                    venmoPendingRequest = venmoLauncher.launch(requireActivity(), it)
                }
                is VenmoPaymentAuthRequest.Failure -> {
                    handleError(it.error)
                }
            }
        }
    }
}
