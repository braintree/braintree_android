package com.braintreepayments.api.uicomponents.compose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class PayPalComposeButtonViewModel(
    val repository: PayPalPendingRequestRepository
): ViewModel() {

    fun storePendingRequest(pendingRequest: String) {
        viewModelScope.launch {
            repository.storePendingRequest(pendingRequest)
        }
    }

    fun getPendingRequest(): String? {
        var pendingRequest: String? = null
        viewModelScope.launch {
            pendingRequest = repository.getPendingRequest()
        }
        return pendingRequest
    }

    fun clearPendingRequest() {
        viewModelScope.launch {
            repository.clearPendingRequest()
        }
    }
}
