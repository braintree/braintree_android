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

@OptIn(ExperimentalBetaApi::class)
class ShopperInsightsFragmentV2 : BaseFragment() {

    private var shopperInsightsClientSuccessfullyFetched by mutableStateOf(false)

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
                            modifier = Modifier.padding(4.dp).weight(1f)
                        )
                        TextField(
                            value = nationalNumberText,
                            onValueChange = { newValue -> nationalNumberText = newValue },
                            label = { Text("National Number") },
                            modifier = Modifier.padding(4.dp).weight(2f)
                        )
                    }

                    Button(enabled = shopperInsightsClientSuccessfullyFetched, onClick = { handleCreateCustomerSession() }) { Text(text = "Create customer session") }
                    Button(enabled = shopperInsightsClientSuccessfullyFetched, onClick = { handleUpdateCustomerSession() }) { Text(text = "Update customer session") }
                    Button(enabled = shopperInsightsClientSuccessfullyFetched, onClick = { handleGetRecommendations() }) { Text(text = "Get recommendations") }
                }
            }
        }
    }

    private fun initShopperInsightsClient() {
        fetchAuthorization { authResult ->
            when(authResult) {
                is BraintreeAuthorizationResult.Success -> {
                    shopperInsightsClient = ShopperInsightsClientV2(requireContext(), authResult.authString)
                    shopperInsightsClientSuccessfullyFetched = true
                }
                is BraintreeAuthorizationResult.Error -> {
                    // Handle error, e.g., show error message
                }
            }
        }
    }

    private fun handleCreateCustomerSession() {
        val customerSessionRequest = CustomerSessionRequest()
        shopperInsightsClient.createCustomerSession(customerSessionRequest) { result ->
            when (result) {
                is CustomerSessionResult.Success -> {
                    // Handle success, e.g., store sessionId
                    val sessionId = result.sessionId
                }
                is CustomerSessionResult.Failure -> {
                    // Handle failure, e.g., show error message
                    val error = result.error
                }
            }
        }
    }

    private fun handleUpdateCustomerSession() {
        val customerSessionRequest = CustomerSessionRequest()
        val requestId = "session-id" // Replace with actual session ID
        shopperInsightsClient.updateCustomerSession(customerSessionRequest, requestId) { result ->
            when (result) {
                is CustomerSessionResult.Success -> {
                    // Handle success
                }
                is CustomerSessionResult.Failure -> {
                    // Handle failure
                }
            }

        }
    }

    private fun handleGetRecommendations() {
        shopperInsightsClient.generateCustomerRecommendations { result ->
            when (result) {
                is CustomerRecommendationsResult.Success -> {
                    // Handle success, e.g., display recommendations
                    val recommendations = result.customerRecommendations.paymentRecommendations // Assuming sessionId contains recommendations
                }
                is CustomerRecommendationsResult.Failure -> {
                    // Handle failure, e.g., show error message
                    val error = result.error
                }
            }
        }
    }
}
