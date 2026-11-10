package com.braintreepayments.api.paypalsavedpaymentmethod.xml

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AttachedViewsUnitTest {

    private val sut = AttachedViews()

    @Test
    fun `a second view can't attach to a view model another view shows`() {
        sut.attach(KEY, FIRST)

        assertTrue(sut.isAttachedToAnotherView(KEY, SECOND))
        assertFalse(sut.attach(KEY, SECOND))
    }

    @Test
    fun `only the attached view can detach it, after which another view can attach`() {
        sut.attach(KEY, FIRST)

        sut.detach(KEY, SECOND)
        assertTrue(sut.isAttachedToAnotherView(KEY, SECOND))

        sut.detach(KEY, FIRST)
        assertTrue(sut.attach(KEY, SECOND))
    }

    @Test
    fun `views of different view models don't block each other`() {
        sut.attach(KEY, FIRST)

        assertTrue(sut.attach("another-key", SECOND))
    }

    private companion object {
        const val KEY = "view-model-key"
        const val FIRST = "view-1"
        const val SECOND = "view-2"
    }
}
