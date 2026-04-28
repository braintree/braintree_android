package com.braintreepayments.api.americanexpress

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.braintreepayments.api.testutils.Fixtures
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4ClassRunner::class)
class AmericanExpressClientTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test(timeout = 30000)
    fun getRewardsBalance_withInvalidNonce_returnsFailure() {
        val sut = AmericanExpressClient(context, Fixtures.TOKENIZATION_KEY)
        val latch = CountDownLatch(1)
        var result: AmericanExpressResult? = null

        sut.getRewardsBalance("fake-invalid-nonce", "USD") { amexResult ->
            result = amexResult
            latch.countDown()
        }

        latch.await(20, TimeUnit.SECONDS)
        assertTrue(
            "Expected Failure but got ${result?.javaClass?.simpleName}",
            result is AmericanExpressResult.Failure
        )
    }

    @Test(timeout = 30000)
    fun getRewardsBalance_withEmptyNonce_returnsFailure() {
        val sut = AmericanExpressClient(context, Fixtures.TOKENIZATION_KEY)
        val latch = CountDownLatch(1)
        var result: AmericanExpressResult? = null

        sut.getRewardsBalance("", "USD") { amexResult ->
            result = amexResult
            latch.countDown()
        }

        latch.await(20, TimeUnit.SECONDS)
        assertTrue(
            "Expected Failure but got ${result?.javaClass?.simpleName}",
            result is AmericanExpressResult.Failure
        )
    }
}
