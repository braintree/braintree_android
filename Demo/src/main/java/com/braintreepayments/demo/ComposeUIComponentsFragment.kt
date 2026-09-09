package com.braintreepayments.demo

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.fragment.NavHostFragment
import com.braintreepayments.api.card.Card
import com.braintreepayments.api.core.PaymentMethodNonce
import com.braintreepayments.api.paypal.PayPalResult
import com.braintreepayments.api.paypal.PayPalTokenizeCallback
import com.braintreepayments.api.uicomponents.PayPalButtonColor
import com.braintreepayments.api.uicomponents.VenmoButtonColor
import com.braintreepayments.api.uicomponents.cardfields.CardFieldsResult
import com.braintreepayments.api.uicomponents.compose.CardFields
import com.braintreepayments.api.uicomponents.compose.PayPalButton
import com.braintreepayments.api.uicomponents.compose.VenmoButton
import com.braintreepayments.api.uicomponents.compose.rememberCardFieldsController
import com.braintreepayments.api.venmo.VenmoPaymentMethodUsage
import com.braintreepayments.api.venmo.VenmoRequest
import com.braintreepayments.api.venmo.VenmoResult
import com.braintreepayments.api.venmo.VenmoTokenizeCallback

private const val APP_LINK_RETURN_URL =
    "https://mobile-sdk-demo-site-838cead5d3ab.herokuapp.com/braintree-payments"
private const val DEEP_LINK_FALLBACK_SCHEME = "com.braintreepayments.demo.braintree"


class ComposeUIComponentsFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        val payPalRequest = paypalRequest(requireContext())
        return ComposeView(requireContext()).apply {
            setContent {
                var venmoStyle: VenmoButtonColor by remember { mutableStateOf(VenmoButtonColor.Blue) }
                var paypalStyle: PayPalButtonColor by remember { mutableStateOf(PayPalButtonColor.Blue) }

                val cardFieldsController = rememberCardFieldsController(
                    authorization = authStringArg,
                    // optional customer data
                    request = Card(
                        cardholderName = "John Doe",
                        postalCode = "12345"
                    )
                )
                val isFormValid by cardFieldsController.isFormValid.collectAsState()

                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    // Payment Buttons section
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            SectionLabel(stringResource(R.string.venmo))
                            SingleChoiceSegmentedButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { index ->
                                    venmoStyle = when (index) {
                                        0 -> VenmoButtonColor.Blue
                                        1 -> VenmoButtonColor.Black
                                        2 -> VenmoButtonColor.White
                                        else -> VenmoButtonColor.Blue
                                    }
                                }
                            )
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                VenmoButton(
                                    style = venmoStyle,
                                    venmoRequest = venmoRequest,
                                    authorization = authStringArg,
                                    appLinkReturnUrl = APP_LINK_RETURN_URL.toUri(),
                                    deepLinkFallbackUrlScheme = DEEP_LINK_FALLBACK_SCHEME,
                                    venmoTokenizeCallback = venmoTokenizeCallback
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            SectionLabel(stringResource(R.string.paypal))
                            SingleChoiceSegmentedButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { index ->
                                    paypalStyle = when (index) {
                                        0 -> PayPalButtonColor.Blue
                                        1 -> PayPalButtonColor.Black
                                        2 -> PayPalButtonColor.White
                                        else -> PayPalButtonColor.Blue
                                    }
                                }
                            )
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                PayPalButton(
                                    style = paypalStyle,
                                    payPalRequest = payPalRequest,
                                    authorization = authStringArg,
                                    appLinkReturnUrl = APP_LINK_RETURN_URL.toUri(),
                                    deepLinkFallbackUrlScheme = DEEP_LINK_FALLBACK_SCHEME,
                                    paypalTokenizeCallback = paypalTokenizeCallback
                                )
                            }
                        }
                    }

                    // Card Fields section
                    SectionLabel(
                        text = stringResource(R.string.card_fields),
                        modifier = Modifier.padding(top = 24.dp)
                    )
                    CardFields(
                        controller = cardFieldsController,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Button(
                        modifier = Modifier
                            .height(68.dp)
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Black,
                            contentColor = Color.White,
                            disabledContainerColor = Color.Gray,
                            disabledContentColor = Color.White
                        ),
                        enabled = isFormValid,
                        onClick = {
                            cardFieldsController.submit { result ->
                                when (result) {
                                    is CardFieldsResult.Success -> handleNonce(result.nonce)
                                    is CardFieldsResult.Failure -> handleError(result.error)
                                }
                            }
                        }
                    ) {
                        Text("Pay")
                    }
                }
            }
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        }
    }

    @Composable
    private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = modifier.padding(bottom = 4.dp)
        )
    }

    @Composable
    private fun SingleChoiceSegmentedButton(
        modifier: Modifier = Modifier,
        onClick: (Int) -> Unit = {}
    ) {
        var selectedIndex by remember { mutableIntStateOf(0) }
        val options = listOf(
            stringResource(R.string.blue_button_color_option),
            stringResource(R.string.black_button_color_option),
            stringResource(R.string.white_button_color_option)
        )

        SingleChoiceSegmentedButtonRow(modifier = modifier) {
            options.forEachIndexed { index, label ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    onClick = {
                        selectedIndex = index
                        onClick(index)
                    },
                    selected = index == selectedIndex,
                    icon = {},
                    label = {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1
                        )
                    }
                )
            }
        }
    }

    private fun handleNonce(nonce: PaymentMethodNonce) {
        onPaymentMethodNonceCreated(nonce)
        val action = ComposeUIComponentsFragmentDirections
            .actionComposeUiComponentsFragmentToDisplayNonceFragment(nonce)
        NavHostFragment.findNavController(this).navigate(action)
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
        false,
        null
    )

    private val paypalTokenizeCallback = PayPalTokenizeCallback { payPalResult ->
        when (payPalResult) {
            is PayPalResult.Success -> handleNonce(payPalResult.nonce)
            is PayPalResult.Cancel ->
                handleError(Exception("User did not complete PayPal payment flow"))
            is PayPalResult.Failure -> handleError(payPalResult.error)
        }
    }

    private val venmoRequest = VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE).apply {
        profileId = null
        shouldVault = false
        collectCustomerBillingAddress = true
        collectCustomerShippingAddress = true
        totalAmount = "0.20"
        subTotalAmount = "0.18"
        taxAmount = "0.01"
        shippingAmount = "0.01"
    }

    private val venmoTokenizeCallback = VenmoTokenizeCallback { venmoResult ->
        when (venmoResult) {
            is VenmoResult.Success -> handleNonce(venmoResult.nonce)
            is VenmoResult.Cancel ->
                handleError(Exception("User did not complete Venmo payment flow"))
            is VenmoResult.Failure -> handleError(venmoResult.error)
        }
    }
}