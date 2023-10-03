package com.braintreepayments.api

import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.lang.ref.WeakReference

@RunWith(RobolectricTestRunner::class)
class CrashReporterUnitTest {

    private lateinit var braintreeClient: BraintreeClient
    private lateinit var defaultExceptionHandler: Thread.UncaughtExceptionHandler

    @Before
    fun setup() {
        braintreeClient = mockk(relaxed = true)
        defaultExceptionHandler = mockk(relaxed = true)
    }

    @Test
    fun start_setsSelfAsThreadDefaultExceptionHandler() {
        val sut = CrashReporter(braintreeClient)
        sut.start()
        assertSame(sut, Thread.getDefaultUncaughtExceptionHandler())
    }

    @Test
    fun uncaughtExceptionHandler_whenClientReferenceNull_forwardsToExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler(defaultExceptionHandler)
        val thread = mockk<Thread>()
        val exception = Exception("error")

        val sut = CrashReporter(WeakReference(null))
        sut.start()
        sut.uncaughtException(thread, exception)
        verify { defaultExceptionHandler.uncaughtException(thread, exception) }
    }

    @Test
    fun uncaughtExceptionHandler_whenClientReferenceNull_restoresOriginalDefaultExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler(defaultExceptionHandler)
        val sut = CrashReporter(WeakReference(null))
        sut.start()
        sut.uncaughtException(mockk(), Exception("error"))
        assertSame(defaultExceptionHandler, Thread.getDefaultUncaughtExceptionHandler())
    }

    @Test
    fun uncaughtExceptionHandler_whenCauseUnknown_forwardsInvocationToDefaultExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler(defaultExceptionHandler)
        val thread = mockk<Thread>()
        val exception = Exception()
        exception.stackTrace =
            arrayOf(StackTraceElement("test",
                "test",
                "test",
                1))
        val sut = CrashReporter(braintreeClient)
        sut.start()
        sut.uncaughtException(thread, exception)

        verify { defaultExceptionHandler.uncaughtException(thread, exception) }
    }

    @Test
    fun uncaughtExceptionHandler_whenBraintreeInStackTrace_reportsCrashToExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler(defaultExceptionHandler)
        val thread = mockk<Thread>()
        val exception = Exception()
        exception.stackTrace = arrayOf(
            StackTraceElement("com.braintreepayments.api.CrashReporting",
                "test",
                "test",
                1)
        )
        val sut = CrashReporter(braintreeClient)
        sut.start()
        sut.uncaughtException(thread, exception)

        verify { braintreeClient.reportCrash() }
        verify { defaultExceptionHandler.uncaughtException(thread, exception) }
    }

    @Test
    fun uncaughtExceptionHandler_whenPayPalInStackTrace_reportsCrashToExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler(defaultExceptionHandler)
        val thread = mockk<Thread>()
        val exception = Exception()
        exception.stackTrace = arrayOf(
            StackTraceElement("com.paypal.CrashReporting",
                "test",
                "test",
                1)
        )
        val sut = CrashReporter(braintreeClient)
        sut.start()
        sut.uncaughtException(thread, exception)

        verify { braintreeClient.reportCrash() }
        verify { defaultExceptionHandler.uncaughtException(thread, exception) }
    }
}
