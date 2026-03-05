package com.braintreepayments.api.uicomponents.compose

import androidx.annotation.RestrictTo
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class PayPalComposeButtonViewModel(
    private val repository: PayPalPendingRequestRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val coroutineScope: CoroutineScope = CoroutineScope(dispatcher),
) : ViewModel() {

    fun storePendingRequest(pendingRequest: String) {
        coroutineScope.launch {
            repository.storePendingRequest(pendingRequest)
        }
    }

    suspend fun getPendingRequest(): String? {
        return repository.getPendingRequest()
    }

    fun clearPendingRequest() {
        coroutineScope.launch {
            repository.clearPendingRequest()
        }
    }
}
