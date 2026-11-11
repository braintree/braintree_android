package com.braintreepayments.api.paypalsavedpaymentmethod.xml

import android.graphics.drawable.Animatable
import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.activity.ComponentActivity
import com.braintreepayments.api.paypalsavedpaymentmethod.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EditLoadingDialogUnitTest {

    private val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
    private val sut = EditLoadingDialog(activity).apply { show() }
    private val root = sut.findViewById<ViewGroup>(android.R.id.content).getChildAt(0) as FrameLayout
    private val spinner = root.getChildAt(0) as ProgressBar

    @Test
    fun `blocks the screen until it is dismissed`() {
        assertTrue(sut.isShowing)
        assertFalse(checkNotNull(sut.window).attributes.dimAmount > 0f)
        sut.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK))
        sut.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK))
        assertTrue(sut.isShowing)
    }

    @Test
    fun `shows the spinner at the Compose size with its own animated drawable`() {
        val size =
            activity.resources.getDimensionPixelSize(R.dimen.paypal_saved_payment_method_loading_dialog_spinner_size)

        assertEquals(size, spinner.layoutParams.width)
        assertEquals(size, spinner.layoutParams.height)
        assertTrue(spinner.isIndeterminate)
        assertTrue(spinner.indeterminateDrawable is Animatable)
    }

    @Test
    fun `screen readers announce loading`() {
        assertEquals("Loading", spinner.contentDescription)
    }
}
