package com.braintreepayments.api

fun interface BTAPITiming {
    fun sendEvent(startTime: Long, endTime: Long, endpoint: String)
}
