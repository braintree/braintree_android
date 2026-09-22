package com.braintreepayments.demo

import android.os.Bundle
import android.view.View
import androidx.annotation.CallSuper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.braintreepayments.api.paymentactions.PaymentActionRequest
import kotlinx.coroutines.launch

/**
 * Base fragment for payment method specific Payment Actions entry screens.
 *
 * The submit -> branch -> handleNextAction loop, SDK client, and flow state live in
 * [PaymentActionMethodViewModel]; subclasses capture payment method details, build a
 * [PaymentActionRequest], and call [submit]. Future integrations (e.g. 3D Secure) hook into
 * [onCustomerActionRequired] and drive [advance] from their result callbacks.
 */
abstract class PaymentActionMethodFragment : BaseFragment() {

    protected abstract val viewModel: PaymentActionMethodViewModel

    protected val paymentActionIdArg: String
        get() = viewModel.paymentActionId

    // Subclasses overriding onViewCreated must call super: this starts the one-shot event collector.
    @CallSuper
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { handleEvent(it) }
            }
        }
    }

    protected fun submit(request: PaymentActionRequest) = viewModel.submit(request)

    protected fun advance() = viewModel.advance()

    private fun handleEvent(event: PaymentActionMethodEvent) {
        when (event) {
            is PaymentActionMethodEvent.PaymentMethodRequired -> onPaymentMethodRequired(event.id)
            is PaymentActionMethodEvent.CustomerActionRequired -> onCustomerActionRequired(event.id)
            is PaymentActionMethodEvent.ServerActionRequired -> navigateToResult(
                PaymentActionMethodViewModel.STATE_SERVER_ACTION_REQUIRED,
                event.id,
                event.serverAction.name
            )
            is PaymentActionMethodEvent.Terminal -> navigateToResult(event.state, event.id, event.detail)
        }
    }

    protected open fun onPaymentMethodRequired(id: String) = Unit

    protected open fun onCustomerActionRequired(id: String) = Unit

    private fun navigateToResult(state: String, paymentActionId: String, detail: String = "") {
        val args = PaymentActionResultFragmentArgs.Builder(state)
            .setPaymentActionId(paymentActionId)
            .setDetail(detail)
            .build()
            .toBundle()
        findNavController().navigate(R.id.action_global_paymentActionResultFragment, args)
    }
}
