package com.braintreepayments.demo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.navigation.fragment.NavHostFragment
import com.braintreepayments.api.card.Card
import com.braintreepayments.api.uicomponents.cardfields.CardFieldsResult
import com.braintreepayments.api.uicomponents.compose.CardFields
import com.braintreepayments.api.uicomponents.compose.rememberCardFieldsState

class ComposeCardFieldsFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        return ComposeView(requireContext()).apply {
            setContent {
                val state = rememberCardFieldsState()
                val isFormValid by state.isFormValid.collectAsState()

                LaunchedEffect(Unit) {
                    state.initialize(context, authStringArg)
                    state.setPaymentRequest(Card(cardholderName = "John Doe", postalCode = "12345"))
                }

                Column(modifier = Modifier.padding(16.dp)) {
                    CardFields(state = state)
                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .padding(top = 20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Black
                        ),
                        enabled = isFormValid,
                        onClick = {
                            state.submit { result ->
                                when (result) {
                                    is CardFieldsResult.Success -> {
                                        onPaymentMethodNonceCreated(result.nonce)
                                        val action = ComposeCardFieldsFragmentDirections
                                            .actionComposeCardFieldsFragmentToDisplayNonceFragment(result.nonce)
                                        NavHostFragment.findNavController(this@ComposeCardFieldsFragment)
                                            .navigate(action)
                                    }
                                    is CardFieldsResult.Failure -> handleError(result.error)
                                }
                            }
                        }
                    ) {
                        Text(getString(R.string.card_fields_pay))
                    }
                }
            }
        }
    }
}
