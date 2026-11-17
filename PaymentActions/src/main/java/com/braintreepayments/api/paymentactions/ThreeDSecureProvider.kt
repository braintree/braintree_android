package com.braintreepayments.api.paymentactions

/**
 * The provider-neutral boundary between the [NextActionHandler] drive loop and the 3D Secure
 * provider SDK (Cardinal today, via [CardinalThreeDSecureProvider]).
 */
internal interface ThreeDSecureProvider {

    /**
     * Runs the provider's session setup, which performs 3D Secure device data collection. Has no UI.
     *
     * @return [DeviceDataResult.Collected] with the reference id to signal back to the server, or
     *   [DeviceDataResult.Failed] if setup could not complete.
     */
    suspend fun collectDeviceData(): DeviceDataResult
}

/**
 * Outcome of [ThreeDSecureProvider.collectDeviceData].
 */
internal sealed class DeviceDataResult {

    /**
     * Device data collection completed. [dfReferenceId] is sent to the server via
     * [CustomerCompletedAction.DeviceDataCollection].
     */
    data class Collected(val dfReferenceId: String) : DeviceDataResult()

    /**
     * Device data collection could not complete.
     */
    data class Failed(val error: Exception) : DeviceDataResult()
}
