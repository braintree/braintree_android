package com.braintreepayments.demo

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

sealed class PaymentActionFlowState {
    data object Idle : PaymentActionFlowState()
    data object Submitting : PaymentActionFlowState()
    class PaymentMethodRequired(val id: String) : PaymentActionFlowState()
    class CustomerActionRequired(val id: String) : PaymentActionFlowState()
    class Processing(val id: String) : PaymentActionFlowState()
}

@Composable
internal fun PaymentActionFlowStatus(
    state: PaymentActionFlowState,
    onAdvance: () -> Unit,
) {
    when (state) {
        PaymentActionFlowState.Idle -> Unit
        PaymentActionFlowState.Submitting -> {
            Text(
                modifier = Modifier.padding(vertical = 8.dp),
                text = stringResource(R.string.loading)
            )
        }
        is PaymentActionFlowState.PaymentMethodRequired -> {
            Text(
                modifier = Modifier.padding(vertical = 8.dp),
                text = stringResource(R.string.payment_actions_id_placeholder, state.id)
            )
            Text(text = stringResource(R.string.payment_actions_select_method))
        }
        is PaymentActionFlowState.CustomerActionRequired -> {
            Text(
                modifier = Modifier.padding(vertical = 8.dp),
                text = stringResource(R.string.payment_actions_id_placeholder, state.id)
            )
            Button(
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .fillMaxWidth(),
                onClick = onAdvance
            ) {
                Text(text = stringResource(R.string.payment_actions_handle_next_action))
            }
        }
        is PaymentActionFlowState.Processing -> {
            Text(
                modifier = Modifier.padding(vertical = 8.dp),
                text = stringResource(R.string.payment_actions_id_placeholder, state.id)
            )
            Button(
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .fillMaxWidth(),
                onClick = onAdvance
            ) {
                Text(text = stringResource(R.string.payment_actions_handle_next_action))
            }
        }
    }
}
