package com.braintreepayments.demo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.viewModels
import com.braintreepayments.api.paymentactions.CreditCard
import com.braintreepayments.cardform.view.CardForm

class PaymentActionsCardFragment : PaymentActionMethodFragment() {

    override val viewModel: PaymentActionMethodViewModel by viewModels()

    private var cardForm: CardForm? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                PaymentActionsCardScreen()
            }
        }
    }

    @Composable
    private fun PaymentActionsCardScreen() {
        val state by viewModel.flowState.collectAsState()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxWidth(),
                factory = { context ->
                    // The CardForm instance must only be created here; setup() must be called
                    // exactly once per instance or the form throws.
                    CardForm(context).apply {
                        cardRequired(true)
                            .expirationRequired(true)
                            .cvvRequired(true)
                            .postalCodeRequired(true)
                            .mobileNumberRequired(false)
                            .actionLabel(getString(R.string.payment_actions_submit))
                            .setup(requireActivity() as AppCompatActivity)
                        cardForm = this
                    }
                }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Button(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 4.dp),
                    enabled = state !is PaymentActionFlowState.Submitting,
                    onClick = { onAutofill() }
                ) {
                    Text(text = stringResource(R.string.autofill))
                }
                Button(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp),
                    enabled = state !is PaymentActionFlowState.Submitting,
                    onClick = { onSubmit() }
                ) {
                    Text(text = stringResource(R.string.payment_actions_submit))
                }
            }

            PaymentActionFlowStatus(state = state, onAdvance = { advance() })
        }
    }

    private fun onAutofill() {
        val form = cardForm ?: return
        val autofillHelper = AutofillHelper(form)
        autofillHelper.fillCardNumber("4111111111111111")
        autofillHelper.fillExpirationDate("01/27")
        autofillHelper.fillCVV("123")
        autofillHelper.fillPostalCode("12345")
    }

    private fun onSubmit() {
        val form = cardForm ?: return
        submit(
            CreditCard(
                number = form.cardNumber,
                expirationMonth = form.expirationMonth,
                expirationYear = form.expirationYear,
                cvv = form.cvv,
                postalCode = form.postalCode,
                countryCodeAlpha2 = "US",
            )
        )
    }
}
