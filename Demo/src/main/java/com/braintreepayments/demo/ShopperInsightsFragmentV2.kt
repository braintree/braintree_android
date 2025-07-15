package com.braintreepayments.demo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.shopperinsights.v2.CustomerRecommendationsResult
import com.braintreepayments.api.shopperinsights.v2.CustomerSessionRequest
import com.braintreepayments.api.shopperinsights.v2.CustomerSessionResult
import com.braintreepayments.api.shopperinsights.v2.ShopperInsightsClientV2
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.flow.update

@OptIn(ExperimentalBetaApi::class)
class ShopperInsightsFragmentV2 : BaseFragment() {

    private var shopperInsightsClientSuccessfullyInstantiated by mutableStateOf(false)
    private val viewModel = ShopperInsightsV2ViewModel()
    private lateinit var shopperInsightsClient: ShopperInsightsClientV2

    @OptIn(ExperimentalFoundationApi::class)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        initShopperInsightsClient()

        return ComposeView(requireContext()).apply {
            setContent {
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

                    Button(
                        enabled = shopperInsightsClientSuccessfullyInstantiated,
                        onClick = { handleCreateCustomerSession(emailText, countryCodeText, nationalNumberText) }) {
                        Text(text = "Create customer session")
                    }
                    Button(
                        enabled = shopperInsightsClientSuccessfullyInstantiated,
                        onClick = { handleUpdateCustomerSession(emailText, countryCodeText, nationalNumberText) }) {
                        Text(text = "Update customer session")
                    }
                    Button(
                        enabled = shopperInsightsClientSuccessfullyInstantiated,
                        onClick = { handleGetRecommendations(viewModel.sessionId.value) }) {
                        Text(text = "Get recommendations")
                    }
                    val sessionId = viewModel.sessionId.collectAsState().value
                    Text(if (sessionId.isNotEmpty()) "Session Id = $sessionId" else "")
                    val recommendations = viewModel.recommendations.collectAsState().value
                    Text(if (recommendations.isNotEmpty()) "Recommendations = " else "")
                }
            }
        }
    }

    private fun initShopperInsightsClient() {
        fetchAuthorization { authResult ->
            when(authResult) {
                is BraintreeAuthorizationResult.Success -> {
                    shopperInsightsClient = ShopperInsightsClientV2(requireContext(), authResult.authString)
                    shopperInsightsClientSuccessfullyInstantiated = true
                }
                is BraintreeAuthorizationResult.Error -> {
                    // Handle error, e.g., show error message
                }
            }
        }
    }

    private fun handleCreateCustomerSession(emailText: String, countryCodeText: String, nationalNumberText: String) {
        val customerSessionRequest = CustomerSessionRequest(
            hashedEmail = emailText.sha256(),
//            hashedPhoneNumber = nationalNumberText.sha256()
        )
        shopperInsightsClient.createCustomerSession(customerSessionRequest) { result ->
            when (result) {
                is CustomerSessionResult.Success -> {
                    viewModel.sessionId.update { result.sessionId }
                }
                is CustomerSessionResult.Failure -> {
                    // Handle failure, e.g., show error message
                    val error = result.error
                }
            }
        }
    }

    private fun handleUpdateCustomerSession(emailText: String, countryCodeText: String, nationalNumberText: String) {
        val customerSessionRequest = CustomerSessionRequest(hashedEmail = emailText.sha256())
        val requestId = "session-id-${UUID.randomUUID()}"
        shopperInsightsClient.updateCustomerSession(customerSessionRequest, requestId) { result ->
            when (result) {
                is CustomerSessionResult.Success -> {
                    viewModel.sessionId.update { result.sessionId }
                }
                is CustomerSessionResult.Failure -> {
                    // Handle failure
                }
            }

        }
    }

    private fun handleGetRecommendations(sessionId: String) {
        shopperInsightsClient.generateCustomerRecommendations(sessionId = sessionId) { result ->
            when (result) {
                is CustomerRecommendationsResult.Success -> {
                    result.customerRecommendations.isInPayPalNetwork?.let {
                        viewModel.isInPayPalNetwork.update { it }
                    }
                    result.customerRecommendations.paymentRecommendations?.let {
                        viewModel.recommendations.update { it }
                    }
                }
                is CustomerRecommendationsResult.Failure -> {
                    // Handle failure, e.g., show error message
                    val error = result.error
                }
            }
        }
    }
}

fun String.sha256(): String {
    return hashString(this, "SHA-256")
}

private fun hashString(input: String, algorithm: String = "SHA-256"): String {
    return MessageDigest
        .getInstance(algorithm)
        .digest(input.toByteArray())
        .toString()
}
