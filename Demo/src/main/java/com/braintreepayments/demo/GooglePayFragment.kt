package com.braintreepayments.demo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.braintreepayments.api.core.PaymentMethodNonce
import com.braintreepayments.api.core.UserCanceledException
import com.braintreepayments.api.googlepay.GooglePayBillingAddressFormat
import com.braintreepayments.api.googlepay.GooglePayCheckoutOption
import com.braintreepayments.api.googlepay.GooglePayClient
import com.braintreepayments.api.googlepay.GooglePayLauncher
import com.braintreepayments.api.googlepay.GooglePayPaymentAuthRequest
import com.braintreepayments.api.googlepay.GooglePayReadinessResult
import com.braintreepayments.api.googlepay.GooglePayRequest
import com.braintreepayments.api.googlepay.GooglePayResult
import com.braintreepayments.api.googlepay.GooglePayShippingAddressParameters
import com.braintreepayments.api.googlepay.GooglePayTotalPriceStatus
import com.google.pay.button.ButtonType
import com.google.pay.button.PayButton
import androidx.compose.foundation.layout.Column
import org.json.JSONArray
import org.json.JSONObject

class GooglePayFragment : BaseFragment() {

    private lateinit var googlePayClient: GooglePayClient
    private lateinit var googlePayLauncher: GooglePayLauncher

    private val args: GooglePayFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        googlePayClient = GooglePayClient(requireContext(), args.authString)
        googlePayLauncher = GooglePayLauncher(this) { paymentAuthResult ->
            googlePayClient.tokenize(paymentAuthResult) { googlePayResult ->
                when (googlePayResult) {
                    is GooglePayResult.Failure -> handleError(googlePayResult.error)
                    is GooglePayResult.Success -> handleGooglePayActivityResult(googlePayResult.nonce)
                    is GooglePayResult.Cancel -> handleError(UserCanceledException("User canceled Google Pay"))
                }
            }
        }

        return ComposeView(requireContext()).apply {
            setContent {
                GooglePayScreen()
            }
        }
    }

    @Composable
    fun GooglePayScreen() {
        var isReadyToPay by remember { mutableStateOf(false) }
        var allowedPaymentMethods by remember { mutableStateOf("") }

        LaunchedEffect(Unit) {
            googlePayClient.isReadyToPay(requireActivity()) { result ->
                if (result is GooglePayReadinessResult.ReadyToPay) {
                    isReadyToPay = true
                    // Note: We avoid createPaymentAuthRequest here to prevent unnecessary analytics noise.
                    // Instead, we assume common card networks are supported for the demo.
                    val cardParams = JSONObject()
                        .put(
                            "allowedAuthMethods",
                            JSONArray().put("PAN_ONLY").put("CRYPTOGRAM_3DS")
                        )
                        .put(
                            "allowedCardNetworks",
                            JSONArray().put("VISA").put("MASTERCARD").put("AMEX").put("DISCOVER")
                                .put("JCB")
                        )

                    allowedPaymentMethods = JSONArray().put(
                        JSONObject().put("type", "CARD").put("parameters", cardParams)
                    ).toString()
                } else {
                    showDialog(
                        "Google Pay is not available. The following issues could be the cause:\n\n" +
                                "No user is logged in to the device.\n\n" +
                                "Google Play Services is missing or out of date."
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (isReadyToPay) {
                    if (allowedPaymentMethods.isNotEmpty()) {
                        PayButton(
                            modifier = Modifier.width(280.dp),
                            onClick = { launchGooglePay() },
                            allowedPaymentMethods = allowedPaymentMethods,
                            type = ButtonType.Buy
                        )
                    } else {
                        LaunchedEffect(Unit) {
                            showDialog(
                                "allowedPaymentMethods cannot be empty.\n\n" +
                                        "Please configure allowedPaymentMethods (e.g. CARD) " +
                                        "to display the Google Pay button."
                            )
                        }
                    }
                }
            }
        }
    }

    private fun launchGooglePay() {
        val activity = requireActivity()
        val googlePayRequest = GooglePayRequest(
            Settings.getGooglePayCurrency(activity),
            "1.00",
            GooglePayTotalPriceStatus.TOTAL_PRICE_STATUS_FINAL
        ).apply {
            totalPriceLabel = "Braintree Demo Payment"
            allowPrepaidCards = Settings.areGooglePayPrepaidCardsAllowed(activity)
            billingAddressFormat = GooglePayBillingAddressFormat.FULL
            isBillingAddressRequired = Settings.isGooglePayBillingAddressRequired(activity)
            isEmailRequired = Settings.isGooglePayEmailRequired(activity)
            isPhoneNumberRequired = Settings.isGooglePayPhoneNumberRequired(activity)
            isShippingAddressRequired = Settings.isGooglePayShippingAddressRequired(activity)
            shippingAddressParameters = GooglePayShippingAddressParameters(
                Settings.getGooglePayAllowedCountriesForShipping(requireContext())
            )
            checkoutOption = GooglePayCheckoutOption.COMPLETE_IMMEDIATE_PURCHASE
        }

        googlePayClient.createPaymentAuthRequest(googlePayRequest) { paymentAuthRequest ->
            when (paymentAuthRequest) {
                is GooglePayPaymentAuthRequest.ReadyToLaunch -> {
                    googlePayLauncher.launch(paymentAuthRequest)
                }

                is GooglePayPaymentAuthRequest.Failure -> {
                    handleError(paymentAuthRequest.error)
                }
            }
        }
    }

    private fun handleGooglePayActivityResult(paymentMethodNonce: PaymentMethodNonce) {
        super.onPaymentMethodNonceCreated(paymentMethodNonce)

        val action = GooglePayFragmentDirections.actionGooglePayFragmentToDisplayNonceFragment(
            paymentMethodNonce
        )
        findNavController().navigate(action)
    }
}
