package com.braintreepayments.api

fun interface APITiming {
    fun sendEvent(startTime: Long, endTime: Long, endpoint: String)
}
