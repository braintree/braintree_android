package com.braintreepayments.api.core

import androidx.annotation.RestrictTo

/**
 * This class is responsible for holding parameters that are sent with analytic events.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class AnalyticsParamRepository(
    private val uuidHelper: UUIDHelper = UUIDHelper()
) {

    var linkType: LinkType? = null

    private lateinit var _sessionId: String

    /**
     * Session ID to tie analytics events together which is used for reporting conversion funnels.
     */
    val sessionId: String
        get() {
            if (!this::_sessionId.isInitialized) {
                _sessionId = uuidHelper.formattedUUID
            }
            return _sessionId
        }

    /**
     * Clears the session ID value from the repository
     */
    fun resetSessionId() {
        _sessionId = uuidHelper.formattedUUID
    }

    companion object {

        /**
         * Singleton instance of the AnalyticsParamRepository.
         */
        val instance: AnalyticsParamRepository by lazy { AnalyticsParamRepository() }
    }
}
