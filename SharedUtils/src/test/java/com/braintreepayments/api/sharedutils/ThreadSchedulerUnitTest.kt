package com.braintreepayments.api.sharedutils

import android.os.Handler
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import java.util.concurrent.ExecutorService

class ThreadSchedulerUnitTest {

    private lateinit var mainThreadHandler: Handler
    private lateinit var backgroundThreadPool: ExecutorService

    @Before
    fun before() {
        mainThreadHandler = mockk(relaxed = true)
        backgroundThreadPool = mockk(relaxed = true)
    }

    @Test
    fun `runOnBackground submits runnable to thread pool`() {
        val sut = ThreadScheduler(mainThreadHandler, backgroundThreadPool)
        val runnable = Runnable {}

        sut.runOnBackground(runnable)

        verify { backgroundThreadPool.submit(runnable) }
    }

    @Test
    fun `runOnMain posts runnable to handler`() {
        val sut = ThreadScheduler(mainThreadHandler, backgroundThreadPool)
        val runnable = Runnable {}

        sut.runOnMain(runnable)

        verify { mainThreadHandler.post(runnable) }
    }
}
