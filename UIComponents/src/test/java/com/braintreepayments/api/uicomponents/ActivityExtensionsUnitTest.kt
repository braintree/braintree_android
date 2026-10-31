package com.braintreepayments.api.uicomponents

import android.content.Context
import android.content.ContextWrapper
import android.view.View
import androidx.activity.ComponentActivity
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ActivityExtensionsUnitTest {

    @Test
    fun `getActivity returns activity when view context is the activity`() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()

        assertSame(activity, View(activity).getActivity())
    }

    @Test
    fun `getActivity unwraps nested ContextWrappers`() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val wrapped = ContextWrapper(ContextWrapper(activity))

        assertSame(activity, View(wrapped).getActivity())
    }

    @Test
    fun `getActivity returns null for application context`() {
        val context: Context = ApplicationProvider.getApplicationContext()

        assertNull(View(context).getActivity())
    }
}
