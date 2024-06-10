package com.braintreepayments.api

interface BTAPITimming {
    fun sendEvent(startTime: Long, endTime: Long, endpoint: String)
}
