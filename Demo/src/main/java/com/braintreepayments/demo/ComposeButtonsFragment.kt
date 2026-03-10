package com.braintreepayments.demo

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.core.net.toUri
import androidx.navigation.fragment.NavHostFragment
import com.braintreepayments.api.core.PaymentMethodNonce
import com.braintreepayments.api.paypal.PayPalResult
import com.braintreepayments.api.paypal.PayPalTokenizeCallback
import com.braintreepayments.api.uicomponents.PayPalButtonColor
import com.braintreepayments.api.uicomponents.VenmoButtonColor
import com.braintreepayments.api.uicomponents.compose.PayPalSmartButton
import com.braintreepayments.api.uicomponents.compose.VenmoSmartButton
import com.braintreepayments.api.venmo.VenmoPaymentMethodUsage
import com.braintreepayments.api.venmo.VenmoRequest
import com.braintreepayments.api.venmo.VenmoResult
import com.braintreepayments.api.venmo.VenmoTokenizeCallback

class ComposeButtonsFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        val paypalRequest = paypalRequest(requireContext())
        return ComposeView(requireContext()).apply {
            setContent {
                var paypalStyle: PayPalButtonColor by remember { mutableStateOf(PayPalButtonColor.Blue) }
                var venmoStyle: VenmoButtonColor by remember { mutableStateOf(VenmoButtonColor.Blue) }
                Column {
                    SingleChoiceSegmentedButton(onClick = { selectedIndex ->
                        paypalStyle = when (selectedIndex) {
                            0 -> PayPalButtonColor.Blue
                            1 -> PayPalButtonColor.Black
                            2 -> PayPalButtonColor.White
                            else -> PayPalButtonColor.Blue
                        }
                    })
                    PayPalSmartButton(
                        style = paypalStyle,
                        payPalRequest = paypalRequest,
                        authorization = authStringArg,
                        appLinkReturnUrl =
                            "https://mobile-sdk-demo-site-838cead5d3ab.herokuapp.com/braintree-payments".toUri(),
                        deepLinkFallbackUrlScheme = "com.braintreepayments.demo.braintree",
                        paypalTokenizeCallback = paypalTokenizeCallback
                    )

                    SingleChoiceSegmentedButton(onClick = { selectedIndex ->
                        venmoStyle = when (selectedIndex) {
                            0 -> VenmoButtonColor.Blue
                            1 -> VenmoButtonColor.Black
                            2 -> VenmoButtonColor.White
                            else -> VenmoButtonColor.Blue
                        }
                    })
                    VenmoSmartButton(
                        style = venmoStyle,
                        venmoRequest = venmoRequest,
                        authorization = authStringArg,
                        appLinkReturnUrl =
                            "https://mobile-sdk-demo-site-838cead5d3ab.herokuapp.com/braintree-payments".toUri(),
                        deepLinkFallbackUrlScheme = "com.braintreepayments.demo.braintree",
                        venmoTokenizeCallback = venmoTokenizeCallback
                    )
                }
            }
        }
    }

    @Composable
    fun SingleChoiceSegmentedButton(modifier: Modifier = Modifier, onClick: (Int) -> Unit = {}) {
        var selectedIndex by remember { mutableIntStateOf(0) }
        val options = listOf("Primary", "Secondary", "Tertiary")

        SingleChoiceSegmentedButtonRow {
            options.forEachIndexed { index, label ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = options.size
                    ),
                    onClick = {
                        selectedIndex = index
                        onClick(selectedIndex)
                    },
                    selected = index == selectedIndex,
                    label = { Text(label) }
                )
            }
        }
    }

    private fun handlePayPalResult(paymentMethodNonce: PaymentMethodNonce?) {
        if (paymentMethodNonce != null) {
            super.onPaymentMethodNonceCreated(paymentMethodNonce)

            val action =
                ComposeButtonsFragmentDirections.actionComposePaymentButtonsFragmentToDisplayNonceFragment(
                    paymentMethodNonce
                )
            NavHostFragment.findNavController(this).navigate(action)
        }
    }

    private fun handleVenmoResult(paymentMethodNonce: PaymentMethodNonce?) {
        if (paymentMethodNonce != null) {
            super.onPaymentMethodNonceCreated(paymentMethodNonce)

            val action =
                ComposeButtonsFragmentDirections.actionComposePaymentButtonsFragmentToDisplayNonceFragment(
                    paymentMethodNonce
                )
            NavHostFragment.findNavController(this).navigate(action)
        }
    }

    private fun paypalRequest(context: Context) = PayPalRequestFactory.createPayPalCheckoutRequest(
        context,
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

    private val paypalTokenizeCallback = PayPalTokenizeCallback { payPalResult ->
        when (payPalResult) {
            is PayPalResult.Success -> {
                handlePayPalResult(payPalResult.nonce)
            }

            is PayPalResult.Cancel -> {
                handleError(Exception("User did not complete PayPal payment flow"))
            }

            is PayPalResult.Failure -> {
                handleError(payPalResult.error)
            }
        }
    }

    private val venmoRequest = VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE).apply {
        profileId = null
        shouldVault = shouldVault
        collectCustomerBillingAddress = true
        collectCustomerShippingAddress = true
        totalAmount = "20"
        subTotalAmount = "18"
        taxAmount = "1"
        shippingAmount = "1"
    }

    private val venmoTokenizeCallback = VenmoTokenizeCallback { venmoResult ->
        when (venmoResult) {
            is VenmoResult.Success -> {
                handleVenmoResult(venmoResult.nonce)
            }

            is VenmoResult.Cancel -> {
                handleError(Exception("User did not complete Venmo payment flow"))
            }

            is VenmoResult.Failure -> {
                handleError(venmoResult.error)
            }
        }
    }
}
