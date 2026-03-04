package com.braintreepayments.api.uicomponents.compose

import androidx.annotation.RestrictTo
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class PayPalComposeButtonViewModel(
    val repository: PayPalPendingRequestRepository
) : ViewModel() {

    fun storePendingRequest(pendingRequest: String) {
        viewModelScope.launch {
            repository.storePendingRequest(pendingRequest)
        }
    }

    suspend fun getPendingRequest(): String? {
        return repository.getPendingRequest()
    }

    fun clearPendingRequest() {
        viewModelScope.launch {
            repository.clearPendingRequest()
        }
    }
}
