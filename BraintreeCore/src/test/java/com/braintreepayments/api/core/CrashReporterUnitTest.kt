package com.braintreepayments.api.core

import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.lang.ref.WeakReference

@RunWith(RobolectricTestRunner::class)
@Suppress("MaxLineLength")
class CrashReporterUnitTest {

    private lateinit var braintreeClient: BraintreeClient
    private lateinit var defaultExceptionHandler: Thread.UncaughtExceptionHandler

    @Before
    fun setup() {
        braintreeClient = mockk(relaxed = true)
        defaultExceptionHandler = mockk(relaxed = true)
    }

    @Test
    fun `when start is called, CrashReporter sets itself as the thread default exception handler`() {
        val sut = CrashReporter(braintreeClient)
        sut.start()
        assertSame(sut, Thread.getDefaultUncaughtExceptionHandler())
    }

    @Test
    fun `when braintreeClient weak reference is null, uncaughtException forwards to the default handler`() {
        Thread.setDefaultUncaughtExceptionHandler(defaultExceptionHandler)
        val thread = mockk<Thread>()
        val exception = Exception("error")

        val sut = CrashReporter(WeakReference(null))
        sut.start()
        sut.uncaughtException(thread, exception)
        verify { defaultExceptionHandler.uncaughtException(thread, exception) }
    }

    @Test
    fun `when braintreeClient weak reference is null, uncaughtException restores the original default handler`() {
        Thread.setDefaultUncaughtExceptionHandler(defaultExceptionHandler)
        val sut = CrashReporter(WeakReference(null))
        sut.start()
        sut.uncaughtException(mockk(), Exception("error"))
        assertSame(defaultExceptionHandler, Thread.getDefaultUncaughtExceptionHandler())
    }

    @Test
    fun `when stack trace has no known braintree or paypal cause, uncaughtException forwards to the default handler without reporting`() {
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
    fun `when stack trace contains a braintree class, uncaughtException reports the crash and forwards to the default handler`() {
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
    fun `when stack trace contains a paypal class, uncaughtException reports the crash and forwards to the default handler`() {
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
