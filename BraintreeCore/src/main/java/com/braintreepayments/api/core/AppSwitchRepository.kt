package com.braintreepayments.api.core

// should possibly be a higher level repo
class AppSwitchRepository {
    var isAppSwitchFlow = false

    companion object {
        val instance by lazy { AppSwitchRepository() }
    }
}
