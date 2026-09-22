package com.braintreepayments.demo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
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
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 16.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                PaymentActionsMethodSelector(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.payment_actions_confirmation_method),
                    options = ConfirmationMethod.entries,
                    optionLabel = ConfirmationMethod::label,
                    selected = state.confirmationMethod,
                    enabled = !state.isCreating,
                    onSelected = viewModel::setConfirmationMethod,
                )
                Spacer(modifier = Modifier.width(8.dp))
                PaymentActionsMethodSelector(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.payment_actions_capture_method),
                    options = CaptureMethod.entries,
                    optionLabel = CaptureMethod::label,
                    selected = state.captureMethod,
                    enabled = !state.isCreating,
                    onSelected = viewModel::setCaptureMethod,
                )
            }

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
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.payment_actions_select_method),
                style = MaterialTheme.typography.titleLarge
            )

            PaymentActionsButton(
                label = stringResource(R.string.payment_actions_credit_card),
                enabled = state.clientToken != null,
                onClick = { launchCreditCard(state) }
            )
        }
    }

    @Composable
    private fun <T : Enum<T>> PaymentActionsMethodSelector(
        modifier: Modifier,
        label: String,
        options: List<T>,
        optionLabel: (T) -> String,
        selected: T,
        enabled: Boolean,
        onSelected: (T) -> Unit,
    ) {
        Column(modifier = modifier) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                options.forEachIndexed { index, option ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                        onClick = { onSelected(option) },
                        selected = option == selected,
                        icon = {},
                        label = {
                            Text(
                                text = optionLabel(option),
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        enabled = enabled,
                    )
                }
            }
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
