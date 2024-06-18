package com.braintreepayments.api

class HttpTimingResponse constructor(
    var startTime: Long,
    var endTime: Long,
    var body: String? = null
)
