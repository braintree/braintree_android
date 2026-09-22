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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs

class PaymentActionResultFragment : Fragment() {

    private val args: PaymentActionResultFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                PaymentActionResultScreen()
            }
        }
    }

    @Composable
    private fun PaymentActionResultScreen() {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(text = stringResource(R.string.payment_actions_status_placeholder, args.status))

            if (args.paymentActionId.isNotEmpty()) {
                Text(text = stringResource(R.string.payment_actions_id_placeholder, args.paymentActionId))
            }

            if (args.detail.isNotEmpty()) {
                Text(text = stringResource(R.string.payment_actions_detail_placeholder, args.detail))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { findNavController().popBackStack() }
            ) {
                Text(text = stringResource(R.string.payment_actions_done))
            }
        }
    }
}
