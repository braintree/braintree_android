package com.braintreepayments.api.paypalsavedpaymentmethod

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import com.braintreepayments.api.paypalsavedpaymentmethod.state.PayPalCreditMessagingContent
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
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
    fun `openLearnMore opens a page that can't be embedded in the browser`() {
        val intent = openLearnMore(mockk<Activity>(relaxed = true), isEmbeddable = false)

        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals(URL, intent.data.toString())
        assertFalse(intent.hasExtra(CUSTOM_TABS_SESSION_EXTRA))
        assertEquals(0, intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    @Test
    fun `openLearnMore opens a page that can be embedded in a Custom Tab`() {
        val intent = openLearnMore(mockk<Activity>(relaxed = true), isEmbeddable = true)

        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals(URL, intent.data.toString())
        assertTrue(intent.hasExtra(CUSTOM_TABS_SESSION_EXTRA))
        assertEquals(0, intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    @Test
    fun `openLearnMore starts a new task from outside an activity`() {
        listOf(false, true).forEach { isEmbeddable ->
            val intent = openLearnMore(mockk<Context>(relaxed = true), isEmbeddable)

            assertEquals(Intent.FLAG_ACTIVITY_NEW_TASK, intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    @Test
    fun `openLearnMore does nothing when no app can open the url`() {
        val context = mockk<Context> { every { startActivity(any()) } throws ActivityNotFoundException() }

        context.openLearnMore(learnMore(isEmbeddable = false))
        context.openLearnMore(learnMore(isEmbeddable = true))
    }

    private fun openLearnMore(context: Context, isEmbeddable: Boolean): Intent {
        val intent = slot<Intent>()
        every { context.startActivity(capture(intent)) } returns Unit
        context.openLearnMore(learnMore(isEmbeddable))
        return intent.captured
    }

    private fun learnMore(isEmbeddable: Boolean) =
        PayPalCreditMessagingContent.LearnMore(text = "Learn more", url = URL, isEmbeddable = isEmbeddable)

    private companion object {
        const val URL = "https://www.paypal.com/learn-more"
        const val CUSTOM_TABS_SESSION_EXTRA = "android.support.customtabs.extra.SESSION"
    }
}
