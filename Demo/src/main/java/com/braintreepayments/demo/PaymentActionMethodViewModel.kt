package com.braintreepayments.demo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.braintreepayments.api.paymentactions.PaymentActionRequest
import com.braintreepayments.api.paymentactions.PaymentActionResult
import com.braintreepayments.api.paymentactions.PaymentActionsClient
import com.braintreepayments.api.paymentactions.ServerAction
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * Owns the method agnostic submit -> branch -> handleNextAction loop driven by [PaymentActionResult].
 * Subclasses capture payment method details, build a [PaymentActionRequest], and call [submit].
 * Future integrations (e.g. 3D Secure) hook into the CustomerActionRequired event and drive
 * [advance] from their result callbacks.
 *
 * The client's callback overloads are intentionally not used: they launch into the client's own
 * scope without error handling and the service's getPaymentAction is still a stub that throws
 * NotImplementedError (an Error, not an Exception).
 */
class PaymentActionMethodViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {

    private val clientToken: String = requireNotNull(savedStateHandle[ARG_CLIENT_TOKEN])
    val paymentActionId: String = savedStateHandle.get<String>(ARG_PAYMENT_ACTION_ID).orEmpty()

    private val _flowState = MutableStateFlow<PaymentActionFlowState>(PaymentActionFlowState.Idle)
    val flowState: StateFlow<PaymentActionFlowState> = _flowState.asStateFlow()

    private val _events = Channel<PaymentActionMethodEvent>(Channel.BUFFERED)
    val events: Flow<PaymentActionMethodEvent> = _events.receiveAsFlow()

    private var paymentActionsClient: PaymentActionsClient? = null
    private var inFlight: Job? = null

    fun submit(request: PaymentActionRequest) = launchStep { client().submitForPaymentAction(request) }

    fun advance() = launchStep { client().handleNextAction() }

    private fun launchStep(block: suspend () -> PaymentActionResult) {
        if (inFlight?.isActive == true) return
        _flowState.value = PaymentActionFlowState.Submitting
        inFlight = viewModelScope.launch { route(runCatchingErrors(block)) }
    }

    // Constructed on first use, inside runCatchingErrors: BraintreeClient construction parses the
    // authorization (throws on a malformed token) and overwrites the global MerchantRepository.
    private fun client(): PaymentActionsClient {
        return paymentActionsClient ?: PaymentActionsClient(getApplication(), clientToken).also {
            paymentActionsClient = it
        }
    }

    // Throwable, not Exception: PaymentActionsService.getPaymentAction is a stub that throws
    // NotImplementedError (an Error); the demo must surface it, not crash.
    @Suppress("TooGenericExceptionCaught")
    private suspend fun runCatchingErrors(block: suspend () -> PaymentActionResult): PaymentActionResult {
        return try {
            block()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (t: Throwable) {
            PaymentActionResult.Failure(t)
        }
    }

    private suspend fun route(result: PaymentActionResult) {
        when (result) {
            is PaymentActionResult.Completed ->
                _events.send(PaymentActionMethodEvent.Terminal(STATE_COMPLETED, result.id))
            is PaymentActionResult.Canceled ->
                _events.send(PaymentActionMethodEvent.Terminal(STATE_CANCELED, result.id))
            is PaymentActionResult.Expired ->
                _events.send(PaymentActionMethodEvent.Terminal(STATE_EXPIRED, result.id))
            is PaymentActionResult.Unknown ->
                _events.send(PaymentActionMethodEvent.Terminal(STATE_UNKNOWN, result.id))
            is PaymentActionResult.Failure -> _events.send(
                PaymentActionMethodEvent.Terminal(
                    STATE_FAILURE,
                    "",
                    result.error.message ?: result.error.toString()
                )
            )
            is PaymentActionResult.ServerActionRequired ->
                _events.send(PaymentActionMethodEvent.ServerActionRequired(result.id, result.serverAction))
            is PaymentActionResult.PaymentMethodRequired -> {
                _flowState.value = PaymentActionFlowState.PaymentMethodRequired(result.id)
                _events.send(PaymentActionMethodEvent.PaymentMethodRequired(result.id))
            }
            is PaymentActionResult.CustomerActionRequired -> {
                _flowState.value = PaymentActionFlowState.CustomerActionRequired(result.id)
                _events.send(PaymentActionMethodEvent.CustomerActionRequired(result.id))
            }
            // TODO: replace manual advance with bounded polling + backoff
            is PaymentActionResult.Processing -> _flowState.value = PaymentActionFlowState.Processing(result.id)
        }
    }

    companion object {
        const val ARG_CLIENT_TOKEN = "clientToken"
        const val ARG_PAYMENT_ACTION_ID = "paymentActionId"

        const val STATE_COMPLETED = "COMPLETED"
        const val STATE_CANCELED = "CANCELED"
        const val STATE_EXPIRED = "EXPIRED"
        const val STATE_FAILURE = "FAILURE"
        const val STATE_UNKNOWN = "UNKNOWN"
        const val STATE_SERVER_ACTION_REQUIRED = "SERVER_ACTION_REQUIRED"
    }
}

sealed class PaymentActionMethodEvent {
    class PaymentMethodRequired(val id: String) : PaymentActionMethodEvent()
    class CustomerActionRequired(val id: String) : PaymentActionMethodEvent()
    class ServerActionRequired(val id: String, val serverAction: ServerAction) : PaymentActionMethodEvent()
    class Terminal(val state: String, val id: String, val detail: String = "") : PaymentActionMethodEvent()
}
