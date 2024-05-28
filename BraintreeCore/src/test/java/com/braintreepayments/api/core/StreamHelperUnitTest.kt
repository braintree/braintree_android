package com.braintreepayments.api.core

import com.braintreepayments.api.testutils.FixturesHelper
import com.braintreepayments.api.core.StreamHelper.getString
import org.junit.Assert.assertEquals
import org.robolectric.RobolectricTestRunner
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class StreamHelperUnitTest {
    @Test
    @Throws(IOException::class)
    fun getString_readsAStringFromAStream() {
        val inputStream = FixturesHelper.streamFromString("Test string")
        assertEquals("Test string", getString(inputStream))
    }
}
