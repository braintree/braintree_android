@file:OptIn(ExperimentalBetaApi::class)

package com.braintreepayments.api.paypalsavedpaymentmethod.xml

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.isVisible
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.paypalsavedpaymentmethod.R
import com.braintreepayments.api.paypalsavedpaymentmethod.imageloader.ImageLoader
import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalSavedPaymentMethod
import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalSavedPaymentMethodType
import com.braintreepayments.api.paypalsavedpaymentmethod.state.PayPalSavedPaymentMethodDisplayState
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalSavedPaymentMethodAppearance
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalSavedPaymentMethodContainerStyle
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalSavedPaymentMethodFundingInstrumentStyle
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalSavedPaymentMethodStyleResolver
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalSavedPaymentMethodViewStyle
import com.google.testing.junit.testparameterinjector.TestParameter
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestParameterInjector
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import kotlinx.coroutines.CompletableDeferred
import java.io.IOException

@RunWith(RobolectricTestParameterInjector::class)
class FundingInstrumentChipViewUnitTest {

    private val context: Context = RuntimeEnvironment.getApplication()
    private var sut = FundingInstrumentChipView(context)
    private val text: TextView get() = sut.findViewById(R.id.paypal_saved_payment_method_funding_instrument_text)
    private val icon: ImageView get() = sut.findViewById(R.id.paypal_saved_payment_method_funding_instrument_icon)
    private val iconContainer: View
        get() = sut.findViewById(R.id.paypal_saved_payment_method_funding_instrument_icon_container)
    private val iconShimmer: View
        get() = sut.findViewById(R.id.paypal_saved_payment_method_funding_instrument_icon_shimmer)
    private val editIcon: ImageView get() = sut.findViewById(R.id.paypal_saved_payment_method_edit_icon)
    private val card = PayPalSavedPaymentMethodDisplayState.Content(paymentMethod(PayPalSavedPaymentMethodType.CARD))
    private var editClicks = 0

    @Test
    fun `a card shows its masked digits, icon and edit icon, and reads its digits one by one`() {
        bind(card)

        assertEquals("••1234", text.text.toString())
        assertEquals("Visa, ending in 1 2 3 4", sut.contentDescription)
        assertTrue(iconContainer.isVisible)
        assertTrue(editIcon.isVisible)
    }

    @Test
    fun `screen readers hear PayPal named first when the logo and label are both hidden`() {
        bind(card, style = PayPalSavedPaymentMethodViewStyle(showPayPalLogo = false, showPayPalLabel = false))

        assertEquals("PayPal, Visa, ending in 1 2 3 4", sut.contentDescription)
    }

    @Test
    fun `screen readers don't hear PayPal again when the label still names it`() {
        bind(card, style = PayPalSavedPaymentMethodViewStyle(showPayPalLogo = false, showPayPalLabel = true))

        assertEquals("Visa, ending in 1 2 3 4", sut.contentDescription)
    }

    @Test
    fun `PayPal Credit shows its label without digits or an icon`() {
        bind(PayPalSavedPaymentMethodDisplayState.Content(paymentMethod(PayPalSavedPaymentMethodType.PAYPAL_CREDIT)))

        assertEquals("Visa", text.text.toString())
        assertEquals("Visa", sut.contentDescription)
        assertFalse(iconContainer.isVisible)
    }

    @Test
    fun `an editable chip shows touch feedback and calls back when tapped`() {
        bind(card)

        assertIsChipRipple(sut.foreground)
        assertTrue(sut.performClick())
        assertEquals(1, editClicks)
    }

    @Test
    fun `the touch feedback is the chip's own ripple on a dark theme too`() {
        val darkChip = FundingInstrumentChipView(ContextThemeWrapper(context, android.R.style.Theme_Material))

        val style = PayPalSavedPaymentMethodStyleResolver(context, PayPalSavedPaymentMethodViewStyle())
        darkChip.bind(card, style, editEnabled = true) {}

        assertIsChipRipple(darkChip.foreground)
    }

    @Test
    fun `card art shows a shimmer while it loads, then the image`() {
        val image = CompletableDeferred<Bitmap>()
        sut = chipWith(imageLoader { image.await() })

        bind(PayPalSavedPaymentMethodDisplayState.Content(cardWithArt()))
        idle()
        assertTrue(iconShimmer.isVisible)
        assertNull(icon.drawable)

        image.complete(cardArt())
        idle()
        assertFalse(iconShimmer.isVisible)
        assertTrue(icon.drawable is BitmapDrawable)
    }

    @Test
    fun `portrait card art scales into the fixed frame instead of growing the chip`() {
        sut = chipWith(imageLoader { Bitmap.createBitmap(60, 120, Bitmap.Config.ARGB_8888) })
        bind(PayPalSavedPaymentMethodDisplayState.Content(cardWithArt()))
        idle()

        sut.measure(
            View.MeasureSpec.makeMeasureSpec(CHIP_WIDTH, View.MeasureSpec.AT_MOST),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        sut.layout(0, 0, sut.measuredWidth, sut.measuredHeight)

        val resources = context.resources
        assertEquals(
            resources.getDimensionPixelSize(R.dimen.paypal_saved_payment_method_funding_instrument_icon_width),
            icon.width
        )
        assertEquals(
            resources.getDimensionPixelSize(R.dimen.paypal_saved_payment_method_funding_instrument_icon_height),
            icon.height
        )
    }

    @Test
    fun `card art that can't be loaded shows the generic card icon`() {
        sut = chipWith(failingImageLoader())

        bind(PayPalSavedPaymentMethodDisplayState.Content(cardWithArt()))
        idle()

        assertFalse(iconShimmer.isVisible)
        assertEquals(R.drawable.ic_fi_card_placeholder, shadowOf(icon.drawable).createdFromResId)
    }

    @Test
    fun `a product shown by its label leaves no space when its art can't be loaded`() {
        sut = chipWith(failingImageLoader())
        val credit = paymentMethod(PayPalSavedPaymentMethodType.PAYPAL_CREDIT).copy(imageUrl = ART_URL)

        bind(PayPalSavedPaymentMethodDisplayState.Content(credit))
        idle()

        assertFalse(iconShimmer.isVisible)
        assertFalse(iconContainer.isVisible)
        assertNull(icon.drawable)
    }

    @Test
    fun `card art whose load a detach cancelled loads again when the chip is attached again`() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        var requests = 0
        val loader = imageLoader { if (requests++ == 0) CompletableDeferred<Bitmap>().await() else cardArt() }
        val chip = FundingInstrumentChipView(activity, imageLoader = loader)
        val chipIcon: ImageView = chip.findViewById(R.id.paypal_saved_payment_method_funding_instrument_icon)
        val chipShimmer: View = chip.findViewById(R.id.paypal_saved_payment_method_funding_instrument_icon_shimmer)
        activity.setContentView(chip)
        chip.bind(
            PayPalSavedPaymentMethodDisplayState.Content(cardWithArt()),
            PayPalSavedPaymentMethodStyleResolver(activity, PayPalSavedPaymentMethodViewStyle()),
            editEnabled = true
        ) {}
        idle()

        // Detaching cancels the first load before it finishes; attaching again loads the art again.
        activity.setContentView(View(activity))
        idle()
        assertTrue(chipShimmer.isVisible)
        activity.setContentView(chip)
        idle()

        assertEquals(2, requests)
        assertFalse(chipShimmer.isVisible)
        assertTrue(chipIcon.drawable is BitmapDrawable)

        // Art that already loaded isn't requested again.
        activity.setContentView(View(activity))
        activity.setContentView(chip)
        idle()
        assertEquals(2, requests)
    }

    @Test
    fun `cached card art shows on the first frame without a shimmer or a request`() {
        val loader = imageLoader { error("not expected") }
        every { loader.peek(ART_URL) } returns cardArt()
        sut = chipWith(loader)

        bind(PayPalSavedPaymentMethodDisplayState.Content(cardWithArt()))

        assertFalse(iconShimmer.isVisible)
        assertTrue(icon.drawable is BitmapDrawable)
        coVerify(exactly = 0) { loader.load(any()) }
    }

    @Test
    fun `binding the same icon again keeps the current image`() {
        bind(card)
        val shown = icon.drawable

        bind(card)

        assertNotNull(shown)
        assertSame(shown, icon.drawable)
    }

    @Test
    fun `a long label shrinks so the edit icon keeps its space`() {
        val longLabel = paymentMethod(PayPalSavedPaymentMethodType.PAYPAL_CREDIT).copy(label = "A".repeat(200))
        bind(PayPalSavedPaymentMethodDisplayState.Content(longLabel))

        sut.measure(
            View.MeasureSpec.makeMeasureSpec(CHIP_WIDTH, View.MeasureSpec.AT_MOST),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        sut.layout(0, 0, sut.measuredWidth, sut.measuredHeight)

        assertTrue(editIcon.width > 0)
        assertTrue(editIcon.right <= sut.width - sut.paddingRight)
    }

    @Test
    fun `an email the buyer can't edit has no edit icon and isn't tappable`() {
        bind(email(isEditable = false))

        assertEquals(EMAIL, text.text.toString())
        assertEquals(EMAIL, sut.contentDescription)
        assertFalse(iconContainer.isVisible)
        assertFalse(editIcon.isVisible)
        assertNull(sut.foreground)
        assertFalse(sut.isClickable)
        sut.performClick()
        assertEquals(0, editClicks)
    }

    @Test
    fun `an email the buyer can edit has the edit icon and is tappable`() {
        bind(email(isEditable = true))

        assertTrue(editIcon.isVisible)
        assertTrue(sut.isClickable)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.N_MR1])
    fun `only an editable chip is a keyboard focus stop, before Android 8 too`() {
        bind(card)
        assertTrue(sut.isFocusable)

        bind(email(isEditable = false))
        assertFalse(sut.isFocusable)

        bind(email(isEditable = true))
        assertTrue(sut.isFocusable)
    }

    @Test
    fun `the chip is disabled while an edit is in progress and ignores programmatic and accessibility clicks`() {
        bind(card, editEnabled = false)

        assertFalse(sut.isEnabled)
        sut.performClick()
        ViewCompat.performAccessibilityAction(sut, AccessibilityNodeInfoCompat.ACTION_CLICK, null)
        assertEquals(0, editClicks)
    }

    @Test
    fun `a state without a funding instrument clears the previous one`(@TestParameter noChip: NoChipState) {
        bind(card)

        bind(noChip.state)

        assertEquals("", text.text.toString())
        assertNull(sut.contentDescription)
        assertFalse(iconContainer.isVisible)
        assertNull(icon.drawable)
        assertFalse(editIcon.isVisible)
        assertFalse(sut.isClickable)
    }

    @Test
    fun `the text keeps its fixed color whatever the style's text color`() {
        val style = PayPalSavedPaymentMethodViewStyle(
            componentAppearance = PayPalSavedPaymentMethodAppearance(textColor = Color.WHITE)
        )

        bind(card, style = style)

        assertEquals(
            ContextCompat.getColor(context, R.color.paypal_saved_payment_method_funding_instrument_chip_text),
            text.currentTextColor
        )
    }

    @Test
    fun `screen readers get a button with the change payment method action only when editable`() {
        bind(card)
        assertEquals(Button::class.java.name, sut.nodeInfo().className)
        assertEquals("Change payment method", sut.clickActionLabel())

        bind(email(isEditable = false))
        assertNull(sut.clickActionLabel())
    }

    @Test
    fun `the edit icon uses the style's size`() {
        val style = PayPalSavedPaymentMethodViewStyle(
            container = PayPalSavedPaymentMethodContainerStyle(
                fundingInstrument = PayPalSavedPaymentMethodFundingInstrumentStyle(editIconSizeDp = 20f)
            )
        )

        bind(card, style = style)

        assertEquals(20f.dpToPx(context.resources), editIcon.layoutParams.width)
        assertEquals(20f.dpToPx(context.resources), editIcon.layoutParams.height)
    }

    private fun bind(
        state: PayPalSavedPaymentMethodDisplayState,
        editEnabled: Boolean = true,
        style: PayPalSavedPaymentMethodViewStyle = PayPalSavedPaymentMethodViewStyle()
    ) = sut.bind(state, PayPalSavedPaymentMethodStyleResolver(context, style), editEnabled) { editClicks++ }

    // The chip's fixed ripple, masked to its rounded corners. The theme's selectableItemBackground is also a
    // ripple, but its mask is a plain color, not a rounded shape.
    private fun assertIsChipRipple(foreground: Drawable?) {
        assertTrue(foreground is RippleDrawable)
        val mask = (foreground as RippleDrawable).findDrawableByLayerId(android.R.id.mask)
        assertTrue(mask is GradientDrawable)
        assertEquals(
            context.resources.getDimension(R.dimen.paypal_saved_payment_method_funding_instrument_chip_corner_radius),
            (mask as GradientDrawable).cornerRadius
        )
    }

    private fun chipWith(loader: ImageLoader) = FundingInstrumentChipView(context, imageLoader = loader)

    private fun imageLoader(load: suspend () -> Bitmap): ImageLoader = mockk {
        every { peek(any()) } returns null
        coEvery { load(ART_URL) } coAnswers { load() }
    }

    private fun failingImageLoader() = imageLoader { throw IOException("Unexpected code 404") }

    private fun cardArt(): Bitmap = Bitmap.createBitmap(96, 60, Bitmap.Config.ARGB_8888)

    private fun cardWithArt() = paymentMethod(PayPalSavedPaymentMethodType.CARD).copy(imageUrl = ART_URL)

    private fun email(isEditable: Boolean) = PayPalSavedPaymentMethodDisplayState.NoFundingInstrument(EMAIL, isEditable)

    private fun paymentMethod(type: PayPalSavedPaymentMethodType) = PayPalSavedPaymentMethod(
        type = type,
        label = "Visa",
        imageUrl = null,
        lastDigits = "1234",
        subtype = null
    )

    /** The states with no funding instrument to show. */
    enum class NoChipState(internal val state: PayPalSavedPaymentMethodDisplayState) {
        LOADING(PayPalSavedPaymentMethodDisplayState.Loading),
        NO_NETWORK(PayPalSavedPaymentMethodDisplayState.NoNetwork),
        ERROR(PayPalSavedPaymentMethodDisplayState.Error),
        HIDDEN(PayPalSavedPaymentMethodDisplayState.Hidden)
    }

    private companion object {
        const val CHIP_WIDTH = 300
        const val ART_URL = "https://www.paypalobjects.com/images/visa.png"
        const val EMAIL = "buyer@example.com"
    }
}
