package com.braintreepayments.api.datacollector

sealed class CallbackSubmitException(message: String) : Exception(message) {

    class SubmitError : CallbackSubmitException("Submit callback returned with error status")

    class SubmitTimeout : CallbackSubmitException("Submit callback returned timed out status")

    class Unknown(status: String) : CallbackSubmitException("An unknown error occurred. Please contact support.")
}
