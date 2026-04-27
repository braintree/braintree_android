package com.braintreepayments.api.sharedutils

import android.os.Looper
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4ClassRunner::class)
class ThreadSchedulerTest {

    private lateinit var sut: ThreadScheduler

    @Before
    fun setUp() {
        sut = ThreadScheduler()
    }

    @Test
    fun runOnMain_executesRunnableOnMainThread() {
        val latch = CountDownLatch(1)
        var executedOnMainThread = false

        sut.runOnMain {
            executedOnMainThread = (Looper.myLooper() == Looper.getMainLooper())
            latch.countDown()
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS))
        assertTrue(executedOnMainThread)
    }

    @Test
    fun runOnBackground_executesRunnableOffMainThread() {
        val latch = CountDownLatch(1)
        var executedOffMainThread = false

        sut.runOnBackground {
            executedOffMainThread = (Looper.myLooper() != Looper.getMainLooper())
            latch.countDown()
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS))
        assertTrue(executedOffMainThread)
    }

    @Test
    fun runOnBackground_executesConcurrently() {
        val taskCount = 3
        val latch = CountDownLatch(taskCount)
        val threadNames = mutableSetOf<String>()

        repeat(taskCount) {
            sut.runOnBackground {
                synchronized(threadNames) {
                    threadNames.add(Thread.currentThread().name)
                }
                Thread.sleep(100)
                latch.countDown()
            }
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS))
        assertTrue(threadNames.size > 1)
    }

    @Test
    fun runOnMain_executesMultipleRunnablesInOrder() {
        val results = mutableListOf<Int>()
        val latch = CountDownLatch(3)

        sut.runOnMain {
            results.add(1)
            latch.countDown()
        }
        sut.runOnMain {
            results.add(2)
            latch.countDown()
        }
        sut.runOnMain {
            results.add(3)
            latch.countDown()
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS))
        assertEquals(listOf(1, 2, 3), results)
    }
}
