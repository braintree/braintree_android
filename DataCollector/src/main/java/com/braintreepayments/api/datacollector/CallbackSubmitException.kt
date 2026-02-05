package com.braintreepayments.api.datacollector

sealed class CallbackSubmitException(message: String) : Exception(message) {

    data object SubmitError : CallbackSubmitException("Submit callback returned with error status")

    data object SubmitTimeout : CallbackSubmitException("Submit callback returned timed out status")

    class Unknown(status: String) : CallbackSubmitException(
        "An unknown error occurred: $status. Please contact support.")
}
