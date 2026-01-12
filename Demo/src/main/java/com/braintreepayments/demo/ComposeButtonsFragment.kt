package com.braintreepayments.demo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.core.net.toUri
import androidx.navigation.fragment.NavHostFragment
import com.braintreepayments.api.core.PaymentMethodNonce
import com.braintreepayments.api.paypal.PayPalPendingRequest
import com.braintreepayments.api.paypal.PayPalResult
import com.braintreepayments.api.paypal.PayPalTokenizeCallback
import com.braintreepayments.api.uicomponents.PayPalButtonComposeImpl
import com.braintreepayments.api.uicomponents.PayPalLaunchCallback

class ComposeButtonsFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        //PayPal flow setup
        val payPalRequest = PayPalRequestFactory.createPayPalCheckoutRequest(
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

        return ComposeView(requireContext()).apply {
            val payPalLaunchCallback = PayPalLaunchCallback { request: PayPalPendingRequest? ->
                if (request is PayPalPendingRequest.Started) {
                    storePayPalPendingRequest(request)
                } else if (request is PayPalPendingRequest.Failure) {
                    handleError(request.error)
                }
            }

            val paypalTokenizeCallback = PayPalTokenizeCallback { payPalResult ->
                when (payPalResult) {
                    is PayPalResult.Success -> {
                        handlePayPalResult(payPalResult.nonce)
                    }

                    is PayPalResult.Cancel -> {
                        handleError(Exception("User did not complete payment flow"))
                    }

                    is PayPalResult.Failure -> {
                        handleError(payPalResult.error)
                    }
                }
            }
            setContent {
                PayPalButtonComposeImpl(
                    payPalRequest = payPalRequest,
                    authorization = authStringArg,
                    appLinkReturnUrl = "https://mobile-sdk-demo-site-838cead5d3ab.herokuapp.com/braintree-payments".toUri(),
                    deepLinkFallbackUrlScheme = "com.braintreepayments.demo.braintree",
                    paypalLaunchCallback = payPalLaunchCallback,
                    paypalTokenizeCallback = paypalTokenizeCallback
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

    private fun storePayPalPendingRequest(request: PayPalPendingRequest.Started) {
        PendingRequestStore.getInstance().putPayPalPendingRequest(requireContext(), request)
    }

    private fun getPayPalPendingRequest(): PayPalPendingRequest.Started? {
        return PendingRequestStore.getInstance().getPayPalPendingRequest(requireContext())
    }

    private fun clearPayPalPendingRequest() {
        PendingRequestStore.getInstance().clearPayPalPendingRequest(requireContext())
    }
}
