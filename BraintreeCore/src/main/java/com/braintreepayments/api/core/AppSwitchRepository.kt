package com.braintreepayments.api.core

/**
 * Repository to hold the state of the app switch flow.
 */
class AppSwitchRepository {
    var isAppSwitchFlow = false

    companion object {
        val instance by lazy { AppSwitchRepository() }
    }
}
