package com.braintreepayments.api.datacollector

/**
 * Callback for receiving result of [DataCollector.collectDeviceData]
 */
fun interface DataCollectorCallback {
    /**
     * @param dataCollectorResult the [DataCollectorResult] of collecting device data
     */
    fun onDataCollectorResult(dataCollectorResult: DataCollectorResult)
}
