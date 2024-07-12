package com.braintreepayments.demo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.navigation.fragment.findNavController
import com.braintreepayments.api.core.PaymentMethodNonce

class MainFragment : BaseFragment() {

    private var nonce: PaymentMethodNonce? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_main, container, false)

        val googlePayButton = view.findViewById<Button>(R.id.google_pay)
        val cardsButton = view.findViewById<Button>(R.id.card)
        val payPalButton = view.findViewById<Button>(R.id.paypal)
        val venmoButton = view.findViewById<Button>(R.id.venmo)
        val visaCheckoutButton = view.findViewById<Button>(R.id.visa_checkout)
        val localPaymentsButton = view.findViewById<Button>(R.id.local_payment)
        val sepaDirectDebitButton = view.findViewById<Button>(R.id.sepa_debit)
        val payPalMessagingButton = view.findViewById<Button>(R.id.paypal_messaging)
        val shopperInsightsButton = view.findViewById<Button>(R.id.shopper_insights)

        cardsButton.setOnClickListener { this.launchCards() }
        payPalButton.setOnClickListener { this.launchPayPal() }
        localPaymentsButton.setOnClickListener { this.launchLocalPayment() }
        googlePayButton.setOnClickListener { this.launchGooglePay() }
        visaCheckoutButton.setOnClickListener { this.launchVisaCheckout() }
        venmoButton.setOnClickListener { this.launchVenmo() }
        sepaDirectDebitButton.setOnClickListener { this.launchSEPADirectDebit() }
        payPalMessagingButton.setOnClickListener { this.launchPayPalMessaging() }
        shopperInsightsButton.setOnClickListener { this.launchShoppingInsights() }

        return view
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null && savedInstanceState.containsKey(KEY_NONCE)) {
            nonce = savedInstanceState.getParcelable(KEY_NONCE)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (nonce != null) {
            outState.putParcelable(KEY_NONCE, nonce)
        }
    }

    private fun launchGooglePay() {
        fetchAuthorization { authString ->
            val action = MainFragmentDirections.actionMainFragmentToGooglePayFragment()
                .setAuthString(authString)
            findNavController().navigate(action)
        }
    }

    private fun launchCards() {
        fetchAuthorization { authString ->
            val action = MainFragmentDirections.actionMainFragmentToCardFragment()
            action.setShouldCollectDeviceData(Settings.shouldCollectDeviceData(activity))
            action.setAuthString(authString)
            findNavController().navigate(action)
        }
    }

    private fun launchPayPal() {
        fetchAuthorization { authString ->
            val action = MainFragmentDirections.actionMainFragmentToPayPalFragment()
            action.setShouldCollectDeviceData(Settings.shouldCollectDeviceData(activity))
            action.setAuthString(authString)
            findNavController().navigate(action)
        }
    }

    private fun launchVenmo() {
        fetchAuthorization { authString ->
            val action = MainFragmentDirections.actionMainFragmentToVenmoFragment()
                .setAuthString(authString)
            findNavController().navigate(action)
        }
    }

    private fun launchVisaCheckout() {
        fetchAuthorization { authString ->
            val action = MainFragmentDirections.actionMainFragmentToVisaCheckoutFragment()
                .setAuthString(authString)
            findNavController().navigate(action)
        }
    }

    private fun launchLocalPayment() {
        fetchAuthorization { authString ->
            val action = MainFragmentDirections.actionMainFragmentToLocalPaymentFragment()
                .setAuthString(authString)
            findNavController().navigate(action)
        }
    }

    private fun launchSEPADirectDebit() {
        fetchAuthorization { authString ->
            val action = MainFragmentDirections.actionMainFragmentToSepaDirectDebitFragment()
                .setAuthString(authString)
            findNavController().navigate(action)
        }
    }

    private fun launchPayPalMessaging() {
        fetchAuthorization { authString ->
            val action = MainFragmentDirections.actionMainFragmentToPayPalMessagingFragment()
                .setAuthString(authString)
            findNavController().navigate(action)
        }
    }

    private fun launchShoppingInsights() {
        fetchAuthorization { authString ->
            val action = MainFragmentDirections.actionMainFragmentToShoppingInsightsFragment()
                .setAuthString(authString)
            findNavController().navigate(action)
        }
    }

    companion object {
        const val EXTRA_COLLECT_DEVICE_DATA: String = "collect_device_data"

        private const val KEY_NONCE = "nonce"
    }
}
