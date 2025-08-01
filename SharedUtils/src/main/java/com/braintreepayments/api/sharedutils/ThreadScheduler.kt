package com.braintreepayments.api.sharedutils

import android.os.Handler
import android.os.Looper
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

internal class ThreadScheduler(
    private val mainThreadHandler: Handler = Handler(Looper.getMainLooper()),
    private val backgroundThreadService: ExecutorService = Executors.newCachedThreadPool()
) : Scheduler {

    override fun runOnBackground(runnable: Runnable) {
        backgroundThreadService.submit(runnable)
    }

    override fun runOnMain(runnable: Runnable) {
        mainThreadHandler.post(runnable)
    }
}
