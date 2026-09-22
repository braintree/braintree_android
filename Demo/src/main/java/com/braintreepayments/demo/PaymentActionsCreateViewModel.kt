package com.braintreepayments.demo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class PaymentActionsCreateViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(PaymentActionsCreateUiState())
    val uiState: StateFlow<PaymentActionsCreateUiState> = _uiState.asStateFlow()

    fun createPaymentAction() {
        if (_uiState.value.isCreating) return
        _uiState.update {
            it.copy(
                isCreating = true,
                errorMessage = null,
                clientToken = null,
                paymentActionId = null,
                status = null,
            )
        }

        val confirmationMethod = _uiState.value.confirmationMethod
        val captureMethod = _uiState.value.captureMethod

        // Each create call mints a new single use client token bound to a new payment action.
        PaymentActionsMerchant.createPaymentAction(
            getApplication(),
            confirmationMethod,
            captureMethod,
        ) { result ->
            _uiState.update { state ->
                when (result) {
                    is CreatePaymentActionResult.Success -> state.copy(
                        isCreating = false,
                        clientToken = result.clientToken,
                        paymentActionId = result.paymentActionId,
                        status = result.paymentActionStatus
                    )
                    is CreatePaymentActionResult.Error -> state.copy(
                        isCreating = false,
                        errorMessage = result.error.message
                    )
                }
            }
        }
    }

    fun setConfirmationMethod(method: ConfirmationMethod) {
        _uiState.update { state ->
            if (state.isCreating || state.confirmationMethod == method) {
                state
            } else {
                state.copy(
                    confirmationMethod = method,
                    clientToken = null,
                    paymentActionId = null,
                    status = null,
                    errorMessage = null,
                )
            }
        }
    }

    fun setCaptureMethod(method: CaptureMethod) {
        _uiState.update { state ->
            if (state.isCreating || state.captureMethod == method) {
                state
            } else {
                state.copy(
                    captureMethod = method,
                    clientToken = null,
                    paymentActionId = null,
                    status = null,
                    errorMessage = null,
                )
            }
        }
    }
}

data class PaymentActionsCreateUiState(
    val confirmationMethod: ConfirmationMethod = ConfirmationMethod.AUTOMATIC,
    val captureMethod: CaptureMethod = CaptureMethod.AUTOMATIC,
    val isCreating: Boolean = false,
    val clientToken: String? = null,
    val paymentActionId: String? = null,
    val status: String? = null,
    val errorMessage: String? = null,
)
