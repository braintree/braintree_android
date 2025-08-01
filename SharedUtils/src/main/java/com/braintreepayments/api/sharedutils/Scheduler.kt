package com.braintreepayments.api.sharedutils

internal interface Scheduler {
    fun runOnMain(runnable: Runnable)
    fun runOnBackground(runnable: Runnable)
}
