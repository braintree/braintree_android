package com.braintreepayments.demo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.fragment.findNavController
import com.braintreepayments.api.core.PaymentMethodNonce

class MainFragment : BaseFragment() {

    private var nonce: PaymentMethodNonce? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {

                    Card(
                        modifier = Modifier.padding(12.dp),
                    ) {
                        Text(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(top = 8.dp),
                            text = "PayPal",
                            style = MaterialTheme.typography.headlineSmall
                        )

                        PaymentModuleButton(R.string.paypal_button) { launchPayPal() }
                        PaymentModuleButton(R.string.shopper_insights_button) { launchShopperInsights() }
                        PaymentModuleButton(R.string.paypal_messaging_button) { launchPayPalMessaging() }
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    Card(
                        modifier = Modifier.padding(12.dp),
                    ) {
                        Text(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(top = 8.dp),
                            text = "Other",
                            style = MaterialTheme.typography.headlineSmall
                        )

                        PaymentModuleButton(R.string.venmo) { launchVenmo() }
                        PaymentModuleButton(R.string.cards) { launchCards() }
                        PaymentModuleButton(R.string.google_pay) { launchGooglePay() }
                        PaymentModuleButton(R.string.visa_checkout_button) { launchVisaCheckout() }
                        PaymentModuleButton(R.string.local_payment_button) { launchLocalPayment() }
                        PaymentModuleButton(R.string.sepa_direct_debit_button) { launchSEPADirectDebit() }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }
    }

    @Composable
    private fun PaymentModuleButton(
        stringResource: Int,
        onClick: () -> Unit,
    ) {
        Button(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .fillMaxWidth(),
            onClick = onClick
        ) {
            Text(text = stringResource(stringResource))
        }
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

    private fun launchShopperInsights() {
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
