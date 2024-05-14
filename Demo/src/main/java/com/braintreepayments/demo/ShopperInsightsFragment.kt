package com.braintreepayments.demo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.navigation.fragment.NavHostFragment
import com.braintreepayments.api.BraintreeClient
import com.braintreepayments.api.PayPalAccountNonce
import com.braintreepayments.api.PayPalClient
import com.braintreepayments.api.PayPalListener
import com.braintreepayments.api.ShopperInsightsBuyerPhone
import com.braintreepayments.api.ShopperInsightsClient
import com.braintreepayments.api.ShopperInsightsRequest
import com.braintreepayments.api.ShopperInsightsResult
import com.braintreepayments.api.ExperimentalBetaApi
import com.braintreepayments.api.VenmoAccountNonce
import com.braintreepayments.api.VenmoClient
import com.braintreepayments.api.VenmoListener
import com.braintreepayments.api.VenmoPaymentMethodUsage
import com.braintreepayments.api.VenmoRequest
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputLayout

/**
 * Fragment for handling shopping insights.
 */
@OptIn(ExperimentalBetaApi::class)
class ShopperInsightsFragment : BaseFragment(), PayPalListener, VenmoListener {

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        braintreeClient = getBraintreeClient()
        shopperInsightsClient = ShopperInsightsClient(braintreeClient)

        venmoClient = VenmoClient(this, braintreeClient)
        venmoClient.setListener(this)

        val useManualBrowserSwitch = Settings.isManualBrowserSwitchingEnabled(requireActivity())
        if (useManualBrowserSwitch) {
            payPalClient = PayPalClient(braintreeClient)
        } else {
            payPalClient = PayPalClient(this, braintreeClient)
            payPalClient.setListener(this)
        }

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
        payPalClient.tokenizePayPalAccount(
            requireActivity(),
            PayPalRequestFactory.createPayPalVaultRequest(activity, emailInput.editText?.text.toString(), "")
        )
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

        venmoClient.tokenizeVenmoAccount(requireActivity(), venmoRequest)
    }

    override fun onPayPalSuccess(payPalAccountNonce: PayPalAccountNonce) {
        super.onPaymentMethodNonceCreated(payPalAccountNonce)
        val action = ShopperInsightsFragmentDirections.actionShopperInsightsFragmentToDisplayNonceFragment(
            payPalAccountNonce
        )
        NavHostFragment.findNavController(this).navigate(action)
    }

    override fun onPayPalFailure(error: Exception) {
        handleError(error)
    }

    override fun onVenmoSuccess(venmoAccountNonce: VenmoAccountNonce) {
        super.onPaymentMethodNonceCreated(venmoAccountNonce)

        val action = ShopperInsightsFragmentDirections.actionShopperInsightsFragmentToDisplayNonceFragment(
            venmoAccountNonce
        )
        NavHostFragment.findNavController(this).navigate(action)
    }

    override fun onVenmoFailure(error: Exception) {
        handleError(error)
    }
}
