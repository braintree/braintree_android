package com.braintreepayments.api

interface BTAPITiming {
    fun sendEvent(startTime: Long, endTime: Long, endpoint: String)
}
