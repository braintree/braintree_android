package com.braintreepayments.demo

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.navigation.fragment.NavHostFragment
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.core.UserCanceledException
import com.braintreepayments.api.paypal.PayPalClient
import com.braintreepayments.api.paypal.PayPalLauncher
import com.braintreepayments.api.paypal.PayPalPaymentAuthRequest
import com.braintreepayments.api.paypal.PayPalPaymentAuthResult
import com.braintreepayments.api.paypal.PayPalPendingRequest
import com.braintreepayments.api.paypal.PayPalResult
import com.braintreepayments.api.shopperinsights.ShopperInsightsBuyerPhone
import com.braintreepayments.api.shopperinsights.ShopperInsightsClient
import com.braintreepayments.api.shopperinsights.ShopperInsightsRequest
import com.braintreepayments.api.shopperinsights.ShopperInsightsResult
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
@SuppressLint("SetTextI18n")
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
    private lateinit var shopperInsightsSessionIdNullSwitch: SwitchMaterial

    private lateinit var shopperInsightsClient: ShopperInsightsClient
    private lateinit var payPalClient: PayPalClient
    private lateinit var venmoClient: VenmoClient

    private val venmoLauncher: VenmoLauncher = VenmoLauncher()
    private val paypalLauncher: PayPalLauncher = PayPalLauncher()

    private lateinit var venmoStartedPendingRequest: VenmoPendingRequest.Started
    private lateinit var paypalStartedPendingRequest: PayPalPendingRequest.Started

    private var sessionId: String = "test-shopper-session-id"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        shopperInsightsClient = ShopperInsightsClient(requireContext(), authStringArg, sessionId)

        venmoClient = VenmoClient(requireContext(), super.getAuthStringArg(), null)
        payPalClient = PayPalClient(
            requireContext(), super.getAuthStringArg(),
            Uri.parse("https://mobile-sdk-demo-site-838cead5d3ab.herokuapp.com/")
        )

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
        if (this::paypalStartedPendingRequest.isInitialized) {
            val paypalPaymentAuthResult =
                paypalLauncher.handleReturnToApp(paypalStartedPendingRequest, requireActivity().intent)
            if (paypalPaymentAuthResult is PayPalPaymentAuthResult.Success) {
                payPalClient.tokenize(paypalPaymentAuthResult) {
                    when (it) {
                        is PayPalResult.Success -> {
                            val action =
                                ShopperInsightsFragmentDirections
                                    .actionShopperInsightsFragmentToDisplayNonceFragment(
                                        it.nonce
                                    )
                            NavHostFragment.findNavController(this).navigate(action)
                        }

                        is PayPalResult.Failure -> {
                            handleError(it.error)
                        }

                        is PayPalResult.Cancel -> {
                            handleError(UserCanceledException("User canceled PayPal"))
                        }
                    }
                }
            } else {
                handleError(Exception("User did not complete payment flow"))
            }
        }
    }

    private fun handleVenmoReturnToApp() {
        if (this::venmoStartedPendingRequest.isInitialized) {
            val venmoPaymentAuthResult =
                venmoLauncher.handleReturnToApp(venmoStartedPendingRequest, requireActivity().intent)
            if (venmoPaymentAuthResult is VenmoPaymentAuthResult.Success) {
                venmoClient.tokenize(venmoPaymentAuthResult) {
                    when (it) {
                        is VenmoResult.Success -> {
                            val action =
                                ShopperInsightsFragmentDirections
                                    .actionShopperInsightsFragmentToDisplayNonceFragment(
                                        it.nonce
                                    )
                            NavHostFragment.findNavController(this).navigate(action)
                        }

                        is VenmoResult.Failure -> {
                            handleError(it.error)
                        }

                        is VenmoResult.Cancel -> {
                            handleError(UserCanceledException("User canceled Venmo"))
                        }
                    }
                }
            } else {
                handleError(Exception("User did not complete payment flow"))
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
            request,
            """{"exp_name":"PaymentReady","treatment_name":"test"}"""
        ) { result ->
            when (result) {
                is ShopperInsightsResult.Success -> {
                    if (result.response.isPayPalRecommended) {
                        payPalVaultButton.isEnabled = true
                        shopperInsightsClient.sendPayPalPresentedEvent(
                            """{"exp_name":"PaymentReady","treatment_name":"control"}""",
                            listOf("PayPal", "Venmo", "other")
                        )
                    }

                    if (result.response.isVenmoRecommended) {
                        venmoButton.isEnabled = true
                        shopperInsightsClient.sendVenmoPresentedEvent(
                            """{"exp_name":"PaymentReady","treatment_name":"test"}""",
                            listOf("Venmo", "PayPal", "other")
                        )
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
                emailInput.editText?.text.toString(),
                countryCodeInput.editText?.text.toString(),
                nationalNumberInput.editText?.text.toString(),
                sessionId

            )
        ) { authRequest ->
            when (authRequest) {
                is PayPalPaymentAuthRequest.Failure -> {
                    handleError(authRequest.error)
                }

                is PayPalPaymentAuthRequest.ReadyToLaunch -> {
                    when (val paypalPendingRequest = paypalLauncher.launch(requireActivity(), authRequest)) {
                        is PayPalPendingRequest.Started -> {
                            paypalStartedPendingRequest = paypalPendingRequest
                        }

                        is PayPalPendingRequest.Failure -> {
                            Toast.makeText(
                                requireContext(),
                                paypalPendingRequest.error.message,
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
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
            when (it) {
                is VenmoPaymentAuthRequest.ReadyToLaunch -> {
                    when (val venmoPendingRequest = venmoLauncher.launch(requireActivity(), it)) {
                        is VenmoPendingRequest.Started -> {
                            venmoStartedPendingRequest = venmoPendingRequest
                        }

                        is VenmoPendingRequest.Failure -> {
                            Toast.makeText(
                                requireContext(),
                                venmoPendingRequest.error.message,
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }

                is VenmoPaymentAuthRequest.Failure -> {
                    handleError(it.error)
                }
            }
        }
    }
}
