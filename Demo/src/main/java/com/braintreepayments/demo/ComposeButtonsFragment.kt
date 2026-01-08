package com.braintreepayments.demo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.navigation.fragment.NavHostFragment
import com.braintreepayments.api.core.PaymentMethodNonce
import com.braintreepayments.api.paypal.PayPalPendingRequest
import com.braintreepayments.api.paypal.PayPalRequest
import com.braintreepayments.api.uicomponents.PayPalButton
import com.braintreepayments.api.uicomponents.PayPalButtonComposeImpl
import com.braintreepayments.api.uicomponents.PayPalLaunchCallback

class ComposeButtonsFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        return ComposeView(requireContext()).apply {
            //PayPal flow setup
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

            val payPalLaunchCallback = PayPalLaunchCallback { request: PayPalPendingRequest? ->
                if (request is PayPalPendingRequest.Started) {
                    storePayPalPendingRequest(request)
                } else if (request is PayPalPendingRequest.Failure) {
                    handleError(request.error)
                }
            }
            setContent {
                PayPalButtonComposeImpl(
                    payPalRequest = payPalRequest,
                    authorization = authStringArg,
                    appLinkReturnUrl = "https://mobile-sdk-demo-site-838cead5d3ab.herokuapp.com/braintree-payments".toUri(),
                    deepLinkFallbackUrlScheme = "com.braintreepayments.demo.braintree",
                    payPalLaunchCallback = payPalLaunchCallback
                )
//                ComposePayPalButton(
//                    this@ComposeButtonsFragment,
//                    authStringArg,
//                    payPalRequest,
//                    payPalLaunchCallback
//                )
            }
        }
    }

    override fun onResume() {
        super.onResume()

        //PayPal flow after returning to app
//        val pendingRequest: PayPalPendingRequest.Started? = getPayPalPendingRequest()
//        if (pendingRequest != null) {
//            payPalButton.handleReturnToApp(
//                pendingRequest, requireActivity().intent,
//                PayPalTokenizeCallback { payPalResult: PayPalResult? ->
//                    when (payPalResult) {
//                        is PayPalResult.Success -> {
//                            handlePayPalResult(payPalResult.nonce)
//                        }
//
//                        is PayPalResult.Cancel -> {
//                            handleError(Exception("User did not complete payment flow"))
//                        }
//
//                        is PayPalResult.Failure -> {
//                            handleError(payPalResult.error)
//                        }
//
//                        null -> { handleError(Exception("Unexpected result: null.")) }
//                    }
//                }
//            )
//            clearPayPalPendingRequest()
//            requireActivity().intent.setData(null)
//        }
    }

    private fun handlePayPalResult(paymentMethodNonce: PaymentMethodNonce?) {
        if (paymentMethodNonce != null) {
            super.onPaymentMethodNonceCreated(paymentMethodNonce)

            val action =
                PaymentButtonsFragmentDirections.actionPaymentButtonsFragmentToDisplayNonceFragment(
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

@Composable
fun ComposePayPalButton(
    activityResultCaller: ActivityResultCaller,
    authString: String,
    payPalRequest: PayPalRequest,
    payPalLaunchCallback: PayPalLaunchCallback
) {
    val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) {
        println("Result: $it")
    }
    AndroidView(
        modifier = Modifier.fillMaxSize(), // Occupy the max size in the Compose UI tree
        factory = { context ->
            // Creates view
            PayPalButton(context).apply {
                initialize(
                    null, // ActivityResultCaller
                    authString,
                    "https://mobile-sdk-demo-site-838cead5d3ab.herokuapp.com/braintree-payments".toUri(),
                    "com.braintreepayments.demo.braintree"
                )
                setPayPalRequest(payPalRequest)
                this.payPalLaunchCallback = payPalLaunchCallback
                // Sets up listeners for View -> Compose communication
                setOnClickListener {
//                    onButtonClick()
                }
            }
        },
        update = { view ->
            // View's been inflated or state read in this block has been updated
            // Add logic here if necessary

            // As selectedItem is read here, AndroidView will recompose
            // whenever the state changes
            // Example of Compose -> View communication
//            view.selectedItem = selectedItem
        }
    )
}
