package com.braintreepayments.demo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController

class PaymentActionsFragment : BaseFragment() {

    private val viewModel: PaymentActionsCreateViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                PaymentActionsScreen()
            }
        }
    }

    @Composable
    private fun PaymentActionsScreen() {
        val state by viewModel.uiState.collectAsState()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Button(
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .fillMaxWidth(),
                enabled = !state.isCreating,
                onClick = { viewModel.createPaymentAction() }
            ) {
                Text(text = stringResource(R.string.payment_actions_create))
            }

            if (state.isCreating) {
                Text(text = stringResource(R.string.payment_actions_creating))
            }

            state.errorMessage?.let { message ->
                Text(text = message)
            }

            state.paymentActionId?.let { id ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.payment_actions_id_placeholder, id),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            state.status?.let { status ->
                Text(
                    text = stringResource(R.string.payment_actions_status_placeholder, status),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            state.clientToken?.let { token ->
                Text(
                    text = stringResource(
                        R.string.payment_actions_client_token_placeholder,
                        token.take(CLIENT_TOKEN_PREVIEW_LENGTH)
                    ),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.payment_actions_select_method),
                style = MaterialTheme.typography.headlineSmall
            )

            PaymentActionsButton(
                label = stringResource(R.string.payment_actions_credit_card),
                enabled = state.clientToken != null,
                onClick = { launchCreditCard(state) }
            )

            Spacer(modifier = Modifier.height(4.dp))
        }
    }

    @Composable
    private fun PaymentActionsButton(
        label: String,
        enabled: Boolean,
        onClick: () -> Unit,
    ) {
        Button(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .fillMaxWidth(),
            enabled = enabled,
            onClick = onClick
        ) {
            Text(text = label)
        }
    }

    private fun launchCreditCard(state: PaymentActionsCreateUiState) {
        val clientToken = state.clientToken ?: return
        val action = PaymentActionsFragmentDirections
            .actionPaymentActionsFragmentToPaymentActionsCardFragment(clientToken)
            .setPaymentActionId(state.paymentActionId.orEmpty())
        findNavController().navigate(action)
    }

    companion object {
        private const val CLIENT_TOKEN_PREVIEW_LENGTH = 12
    }
}
