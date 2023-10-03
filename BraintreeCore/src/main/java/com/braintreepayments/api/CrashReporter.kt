package com.braintreepayments.api

import androidx.annotation.IntDef
import androidx.annotation.VisibleForTesting
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.ref.WeakReference

internal class CrashReporter @VisibleForTesting constructor(
    private val braintreeClientRef: WeakReference<BraintreeClient>
) : Thread.UncaughtExceptionHandler {

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(CAUSE_UNKNOWN, CAUSE_RELATED_TO_PAYPAL, CAUSE_RELATED_TO_BRAINTREE)
    internal annotation class Cause

    private var defaultExceptionHandler: Thread.UncaughtExceptionHandler? = null

    constructor(braintreeClient: BraintreeClient) :
            this(WeakReference<BraintreeClient>(braintreeClient))

    private fun registerExceptionHandler(handler: Thread.UncaughtExceptionHandler) {
        defaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(handler)
    }

    private fun restoreDefaultExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler(defaultExceptionHandler)
        defaultExceptionHandler = null
    }

    @Cause
    private fun determineExceptionCause(ex: Throwable): Int {
        val stringWriter = StringWriter()
        ex.printStackTrace(PrintWriter(stringWriter))
        return if (stringWriter.toString().contains("com.braintreepayments")) {
            CAUSE_RELATED_TO_BRAINTREE
        } else if (stringWriter.toString().contains("com.paypal")) {
            CAUSE_RELATED_TO_PAYPAL
        } else {
            CAUSE_UNKNOWN
        }
    }

    private fun handleExceptionWithDefaultBehavior(thread: Thread, ex: Throwable) {
        defaultExceptionHandler?.uncaughtException(thread, ex)
    }

    fun start() {
        registerExceptionHandler(this)
    }

    override fun uncaughtException(thread: Thread, exception: Throwable) {
        val braintreeClient = braintreeClientRef.get()
        if (braintreeClient == null) {
            handleExceptionWithDefaultBehavior(thread, exception)
            restoreDefaultExceptionHandler()
            return
        }
        @Cause val result = determineExceptionCause(exception)
        if (result == CAUSE_RELATED_TO_BRAINTREE || result == CAUSE_RELATED_TO_PAYPAL) {
            braintreeClient.reportCrash()
        }
        handleExceptionWithDefaultBehavior(thread, exception)
    }

    companion object {
        const val CAUSE_UNKNOWN = 0
        const val CAUSE_RELATED_TO_PAYPAL = 1
        const val CAUSE_RELATED_TO_BRAINTREE = 2
    }
}
