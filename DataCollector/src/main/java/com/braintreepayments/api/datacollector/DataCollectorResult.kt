package com.braintreepayments.api.datacollector

/**
 * Result of collecting device data for fraud detection
 */
sealed class DataCollectorResult {

    /**
     * The device information was collected for fraud detection. Send [deviceData] to your server
     */
    class Success internal constructor(val deviceData: String) : DataCollectorResult()

    /**
     * There was an [error] during device data collection
     */
    class Failure internal constructor(val error: Exception) : DataCollectorResult()
}
