package com.braintreepayments.api.paypalsavedpaymentmethod

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ContextExtensionsUnitTest {

    private val activity = Robolectric.buildActivity(Activity::class.java).get()

    @Test
    fun `findActivity returns the activity itself or the one a wrapper wraps`() {
        assertSame(activity, activity.findActivity())
        assertSame(activity, ContextWrapper(ContextWrapper(activity)).findActivity())
    }

    @Test
    fun `findActivity returns null for a non-activity context or a wrapper with no base context`() {
        assertNull(RuntimeEnvironment.getApplication().findActivity())
        assertNull(ContextWrapper(null).findActivity())
    }

    @Test
    fun `openUrl opens the url in the browser from an activity`() {
        val activity = mockk<Activity>(relaxed = true)
        val intent = slot<Intent>()
        every { activity.startActivity(capture(intent)) } returns Unit

        activity.openUrl(URL)

        assertEquals(Intent.ACTION_VIEW, intent.captured.action)
        assertEquals(URL, intent.captured.data.toString())
        assertEquals(0, intent.captured.flags and Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    @Test
    fun `openUrl starts a new task from outside an activity`() {
        val context = mockk<Context>(relaxed = true)
        val intent = slot<Intent>()
        every { context.startActivity(capture(intent)) } returns Unit

        context.openUrl(URL)

        assertEquals(Intent.FLAG_ACTIVITY_NEW_TASK, intent.captured.flags and Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    @Test
    fun `openUrl does nothing when no app can open the url`() {
        val context = mockk<Context> { every { startActivity(any()) } throws ActivityNotFoundException() }

        context.openUrl(URL)
    }

    private companion object {
        const val URL = "https://www.paypal.com/learn-more"
    }
}
