package com.braintreepayments.demo

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.fragment.NavHostFragment
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.core.UserCanceledException
import com.braintreepayments.api.paypal.PayPalClient
import com.braintreepayments.api.paypal.PayPalLauncher
import com.braintreepayments.api.paypal.PayPalPaymentAuthRequest
import com.braintreepayments.api.paypal.PayPalPaymentAuthResult
import com.braintreepayments.api.paypal.PayPalPendingRequest
import com.braintreepayments.api.paypal.PayPalResult
import com.braintreepayments.api.shopperinsights.ButtonType
import com.braintreepayments.api.venmo.VenmoClient
import com.braintreepayments.api.venmo.VenmoLauncher
import com.braintreepayments.api.venmo.VenmoPaymentAuthRequest
import com.braintreepayments.api.venmo.VenmoPaymentAuthResult
import com.braintreepayments.api.venmo.VenmoPaymentMethodUsage
import com.braintreepayments.api.venmo.VenmoPendingRequest
import com.braintreepayments.api.venmo.VenmoRequest
import com.braintreepayments.api.venmo.VenmoResult

@OptIn(ExperimentalBetaApi::class)
class ShopperInsightsFragmentV2 : BaseFragment() {

    private var shopperInsightsClientSuccessfullyInstantiated by mutableStateOf(false)
    private val viewModel = ShopperInsightsV2ViewModel()
    private lateinit var payPalClient: PayPalClient
    private lateinit var venmoClient: VenmoClient

    private val venmoLauncher: VenmoLauncher = VenmoLauncher()
    private val paypalLauncher: PayPalLauncher = PayPalLauncher()

    private lateinit var paypalStartedPendingRequest: PayPalPendingRequest.Started
    private lateinit var venmoStartedPendingRequest: VenmoPendingRequest.Started

    @OptIn(ExperimentalFoundationApi::class)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        fetchAuthorization { authResult ->
            when (authResult) {
                is BraintreeAuthorizationResult.Success -> {
                    viewModel.initShopperInsightsClient(requireContext(), authResult.authString)
                    shopperInsightsClientSuccessfullyInstantiated = true
                }
                is BraintreeAuthorizationResult.Error -> {
                    Toast.makeText(context, "Auth failed: ${authResult.error.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        venmoClient = VenmoClient(requireContext(), super.getAuthStringArg(), null)
        payPalClient = PayPalClient(
            requireContext(),
            super.getAuthStringArg(),
            Uri.parse("https://mobile-sdk-demo-site-838cead5d3ab.herokuapp.com/braintree-payments"),
            "com.braintreepayments.demo.braintree"
        )

        return ComposeView(requireContext()).apply {
            setContent {
                MainContent()
            }
        }
    }

    @Composable
    private fun MainContent() {
        Column(modifier = Modifier.padding(8.dp)) {
            var emailText by rememberSaveable { mutableStateOf("PR1_merchantname@personal.example.com") }
            var countryCodeText by rememberSaveable { mutableStateOf("1") }
            var nationalNumberText by rememberSaveable { mutableStateOf("4082321001") }

            TextField(
                value = emailText,
                onValueChange = { newValue -> emailText = newValue },
                label = { Text("Email") },
                modifier = Modifier.padding(4.dp),
            )
            Row {
                TextField(
                    value = countryCodeText,
                    onValueChange = { newValue -> countryCodeText = newValue },
                    label = { Text("Country code") },
                    modifier = Modifier
                        .padding(4.dp)
                        .weight(1f)
                )
                TextField(
                    value = nationalNumberText,
                    onValueChange = { newValue -> nationalNumberText = newValue },
                    label = { Text("National Number") },
                    modifier = Modifier
                        .padding(4.dp)
                        .weight(2f)
                )
            }

            val currentSessionId = viewModel.sessionId.collectAsState().value
            TextField(
                value = currentSessionId,
                onValueChange = {},
                label = { Text("Session ID") },
                enabled = true,
                modifier = Modifier.padding(4.dp)
            )

            Button(
                enabled = shopperInsightsClientSuccessfullyInstantiated,
                onClick = {
                    viewModel.resetRecommendationsCompleted()
                    handleCreateCustomerSession(emailText, countryCodeText, nationalNumberText)
                }
            ) {
                Text(text = "Create customer session")
            }
            Button(
                enabled = shopperInsightsClientSuccessfullyInstantiated,
                onClick = {
                    viewModel.resetRecommendationsCompleted()
                    handleUpdateCustomerSession(emailText, countryCodeText, nationalNumberText, currentSessionId)
                }
            ) {
                Text(text = "Update customer session")
            }
            Button(
                enabled = shopperInsightsClientSuccessfullyInstantiated,
                onClick = {
                    handleGetRecommendations(viewModel.sessionId.value)
                }
            ) {
                Text(text = "Get recommendations")
            }
            val sessionId = viewModel.sessionId.collectAsState().value
            val recommendations = viewModel.recommendations.collectAsState().value

            val recommendationsCompleted = viewModel.recommendationsCompleted.collectAsState().value
            if (recommendationsCompleted) {
                Text(if (recommendations.isNotEmpty()) "Recommendations = $recommendations" else "")
                val isInPayPalNetwork = viewModel.isInPayPalNetwork.collectAsState().value
                if (isInPayPalNetwork && recommendations.first().paymentOption == "PAYPAL") {
                    Button(
                        enabled = true,
                        onClick = { launchPayPalVault(emailText, countryCodeText, nationalNumberText, sessionId) }
                    ) {
                        Text(text = "PayPal")
                    }
                }
                if (isInPayPalNetwork && recommendations.first().paymentOption == "VENMO") {
                    Button(
                        enabled = true,
                        onClick = { launchVenmo(sessionId) }
                    ) {
                        Text(text = "Venmo")
                    }
                }

                if (!isInPayPalNetwork) {
                    Text("In PayPal Network = $isInPayPalNetwork")
                }
            }

            val error = viewModel.error.collectAsState().value
            if (error.isNotEmpty()) {
                Toast.makeText(LocalContext.current, error, Toast.LENGTH_LONG).show()
            }
        }
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

    private fun launchPayPalVault(
        emailText: String,
        countryCodeText: String,
        nationalNumberText: String,
        sessionId: String
    ) {
        viewModel.sendSelectedEvent(
            ButtonType.PAYPAL,
            sessionId
        )

        payPalClient.createPaymentAuthRequest(
            requireContext(),
            PayPalRequestFactory.createPayPalVaultRequest(
                activity,
                emailText,
                countryCodeText,
                nationalNumberText,
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
                            Toast.makeText(requireContext(), paypalPendingRequest.error.message, Toast.LENGTH_LONG)
                                .show()
                        }
                    }
                }
            }
        }
    }

    private fun launchVenmo(
        sessionId: String
    ) {
        viewModel.sendSelectedEvent(
            ButtonType.VENMO,
            sessionId
        )

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

    private fun handleCreateCustomerSession(
        emailText: String,
        countryCodeText: String,
        nationalNumberText: String
    ) {
        viewModel.handleCreateCustomerSession(emailText, nationalNumberText)
    }

    private fun handleUpdateCustomerSession(
        emailText: String,
        countryCodeText: String,
        nationalNumberText: String,
        sessionId: String
    ) {
        viewModel.handleUpdateCustomerSession(emailText, nationalNumberText, sessionId)
    }

    private fun handleGetRecommendations(sessionId: String) {
        viewModel.handleGetRecommendations(sessionId)
    }
}
