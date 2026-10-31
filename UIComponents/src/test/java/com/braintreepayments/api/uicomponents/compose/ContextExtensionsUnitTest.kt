package com.braintreepayments.api.uicomponents.compose

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ContextExtensionsUnitTest {

    @Test
    fun `findActivity returns the activity itself`() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()

        assertSame(activity, activity.findActivity())
    }

    @Test
    fun `findActivity unwraps nested ContextWrappers`() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()

        assertSame(activity, ContextWrapper(ContextWrapper(activity)).findActivity())
    }

    @Test
    fun `findActivity returns null when no activity in chain`() {
        val context: Context = ApplicationProvider.getApplicationContext()

        assertNull(ContextWrapper(context).findActivity())
    }
}
