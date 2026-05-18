package com.braintreepayments.api.uicomponents.cardfields

import android.content.Context
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.test.core.app.ApplicationProvider
import com.braintreepayments.api.uicomponents.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowDialog

@RunWith(RobolectricTestRunner::class)
class CvvTextInputViewUnitTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `constructor sets input type to number password`() {
        val view = CvvTextInputView(context)

        val editText = view.findViewById<EditText>(R.id.text_input_edit_text)
        assertEquals(
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD,
            editText.inputType
        )
    }

    @Test
    fun `constructor sets cvv hint`() {
        val view = CvvTextInputView(context)

        val editText = view.findViewById<EditText>(R.id.text_input_edit_text)
        assertEquals("CVV", editText.contentDescription.toString())
    }

    @Test
    fun `getRawCvv returns empty string when no text`() {
        val view = CvvTextInputView(context)

        assertEquals("", view.getRawCvv())
    }

    @Test
    fun `getRawCvv returns raw digits`() {
        val view = CvvTextInputView(context)

        view.setText("123")

        assertEquals("123", view.getRawCvv())
    }

    @Test
    fun `typing more than 3 digits truncates to 3 by default`() {
        val view = CvvTextInputView(context)

        view.setText("1234")

        assertEquals("123", view.getRawCvv())
    }

    @Test
    fun `updateCardBrand with AMEX allows 4 digits`() {
        val view = CvvTextInputView(context)

        view.updateCardBrand(CardBrand.AMEX)
        view.setText("1234")

        assertEquals("1234", view.getRawCvv())
    }

    @Test
    fun `updateCardBrand with AMEX truncates to 4 digits`() {
        val view = CvvTextInputView(context)

        view.updateCardBrand(CardBrand.AMEX)
        view.setText("12345")

        assertEquals("1234", view.getRawCvv())
    }

    @Test
    fun `updateCardBrand from AMEX to VISA truncates existing 4-digit cvv to 3`() {
        val view = CvvTextInputView(context)

        view.updateCardBrand(CardBrand.AMEX)
        view.setText("1234")
        view.updateCardBrand(CardBrand.VISA)

        assertEquals("123", view.getRawCvv())
    }

    @Test
    fun `updateCardBrand with VISA allows 3 digits`() {
        val view = CvvTextInputView(context)

        view.updateCardBrand(CardBrand.VISA)
        view.setText("123")

        assertEquals("123", view.getRawCvv())
    }

    @Test
    fun `updateCardBrand with VISA truncates to 3 digits`() {
        val view = CvvTextInputView(context)

        view.updateCardBrand(CardBrand.VISA)
        view.setText("1234")

        assertEquals("123", view.getRawCvv())
    }

    @Test
    fun `updateCardBrand with UNKNOWN keeps 3 digit limit`() {
        val view = CvvTextInputView(context)

        view.updateCardBrand(CardBrand.UNKNOWN)
        view.setText("1234")

        assertEquals("123", view.getRawCvv())
    }

    @Test
    fun `constructor adds trailing icon with cvv hint content description`() {
        val view = CvvTextInputView(context)

        val trailingIcon = findTrailingIconView(view)
        assertNotNull(trailingIcon)
        assertEquals("CVV security code hint", trailingIcon?.contentDescription.toString())
    }

    @Test
    fun `clicking trailing icon shows cvv hint overlay`() {
        val view = CvvTextInputView(context)

        findTrailingIconView(view)?.performClick()

        val dialog = ShadowDialog.getLatestDialog()
        assertNotNull(dialog)
        assertTrue(dialog!!.isShowing)
    }

    @Test
    fun `clicking close button dismisses cvv hint overlay`() {
        val view = CvvTextInputView(context)
        findTrailingIconView(view)?.performClick()
        val dialog = ShadowDialog.getLatestDialog()!!

        dialog.findViewById<View>(R.id.close_button).performClick()

        assertFalse(dialog.isShowing)
    }

    private fun findTrailingIconView(parent: CvvTextInputView): ImageView? {
        val container = parent.findViewById<FrameLayout>(R.id.input_container)
        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i)
            if (child is ImageView) return child
        }
        return null
    }
}
