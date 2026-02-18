package com.braintreepayments.api.core

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.braintreepayments.api.testutils.Fixtures
import com.braintreepayments.api.testutils.TestClientTokenBuilder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch

@RunWith(AndroidJUnit4ClassRunner::class)
class BraintreeClientTest {

    private lateinit var context: Context
    private lateinit var countDownLatch: CountDownLatch
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
    private val coroutineScope: CoroutineScope = CoroutineScope(mainDispatcher)

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        countDownLatch = CountDownLatch(1)
    }

    @Throws(InterruptedException::class)
    @Test(timeout = 10000)
    fun configuration_succeedsWithATokenizationKey() {
        val sut = BraintreeClient(context, Fixtures.TOKENIZATION_KEY)

        coroutineScope.launch {
            val configuration = sut.getConfiguration()
            assertNotNull(configuration)
            countDownLatch.countDown()
        }

        countDownLatch.await()
    }

    @Throws(InterruptedException::class)
    @Test(timeout = 10000)
    fun configuration_succeedsWithAClientToken() {
        val clientToken = TestClientTokenBuilder().build()
        val sut = BraintreeClient(context, clientToken)

        coroutineScope.launch {
            val configuration = sut.getConfiguration()
            assertNotNull(configuration)
            countDownLatch.countDown()
        }

        countDownLatch.await()
    }
}
