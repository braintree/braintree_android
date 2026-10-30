@file:OptIn(ExperimentalBetaApi::class)

package com.braintreepayments.api.paypalsavedpaymentmethod.compose

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChild
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.paypalsavedpaymentmethod.imageloader.PLACEHOLDER_IMAGE_TEST_TAG
import com.braintreepayments.api.paypalsavedpaymentmethod.state.PayPalCreditMessagingContent
import com.braintreepayments.api.paypalsavedpaymentmethod.state.PayPalCreditMessagingDisplayState
import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalSavedPaymentMethod
import com.braintreepayments.api.paypalsavedpaymentmethod.state.PayPalSavedPaymentMethodDisplayState
import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalSavedPaymentMethodType
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalSavedPaymentMethodViewStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PayPalSavedPaymentMethodContentUnitTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val content = mutableStateOf<Content?>(null)
    private var editClicks = 0
    private val openedUrls = mutableListOf<String>()

    @Before
    fun setUp() {
        // The shimmer sweeps forever, so the clock is driven by hand instead of waiting for the UI to go idle.
        composeTestRule.mainClock.autoAdvance = false
    }

    @Test
    fun `while loading it shows the logo and label with a shimmer, and the Pay Later shimmer`() {
        render(PayPalSavedPaymentMethodDisplayState.Loading, PayPalCreditMessagingDisplayState.Loading)

        composeTestRule.onNodeWithTag(BRAND_MARK_TEST_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(FUNDING_INSTRUMENT_SHIMMER_TEST_TAG).onChild()
            .assertContentDescriptionEquals("Loading saved payment method")
        composeTestRule.onNodeWithTag(CREDIT_MESSAGING_TEST_TAG).assertIsDisplayed()
        assertDoesNotExist(FUNDING_INSTRUMENT_CHIP_TEST_TAG)
    }

    @Test
    fun `a card shows its masked digits, and screen readers read it as a button that changes the payment method`() {
        render(CARD)

        // The chip's text is merged into the one element screen readers read.
        composeTestRule.onNodeWithTag(FUNDING_INSTRUMENT_CHIP_TEST_TAG)
            .assertContentDescriptionEquals("Visa, ending in 1 2 3 4")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assert(SemanticsMatcher("has the change payment method action") {
                it.config.getOrNull(SemanticsActions.OnClick)?.label == "Change payment method"
            })
    }

    @Test
    fun `tapping the chip calls back, and the chip is disabled while an edit is in progress`() {
        render(CARD)
        composeTestRule.onNodeWithTag(FUNDING_INSTRUMENT_CHIP_TEST_TAG).assertIsEnabled().performClick()
        assertEquals(1, editClicks)

        render(CARD, editEnabled = false)
        composeTestRule.onNodeWithTag(FUNDING_INSTRUMENT_CHIP_TEST_TAG).assertIsNotEnabled()
    }

    @Test
    fun `an email the buyer can't edit isn't a button`() {
        render(PayPalSavedPaymentMethodDisplayState.NoFundingInstrument("buyer@example.com", isEditable = false))

        composeTestRule.onNodeWithTag(FUNDING_INSTRUMENT_CHIP_TEST_TAG)
            .assertContentDescriptionEquals("buyer@example.com")
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnClick))
    }

    @Test
    fun `screen readers hear PayPal first when the logo and label are hidden`() {
        render(CARD, style = PayPalSavedPaymentMethodViewStyle(showPayPalLogo = false, showPayPalLabel = false))

        assertDoesNotExist(BRAND_MARK_TEST_TAG)
        composeTestRule.onNodeWithTag(FUNDING_INSTRUMENT_CHIP_TEST_TAG)
            .assertContentDescriptionEquals("PayPal, Visa, ending in 1 2 3 4")
    }

    @Test
    fun `the logo is read as PayPal only when the label is hidden`() {
        render(CARD)
        composeTestRule.onNodeWithText("PayPal").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("PayPal").assertDoesNotExist()

        render(CARD, style = PayPalSavedPaymentMethodViewStyle(showPayPalLabel = false))

        composeTestRule.onNodeWithText("PayPal").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("PayPal").assertIsDisplayed()
    }

    @Test
    fun `a card without card art shows the generic icon, PayPal Credit shows none`() {
        render(CARD)
        composeTestRule.onNodeWithTag(PLACEHOLDER_IMAGE_TEST_TAG, useUnmergedTree = true).assertExists()

        render(PAYPAL_CREDIT)

        composeTestRule.onNodeWithTag(PLACEHOLDER_IMAGE_TEST_TAG, useUnmergedTree = true).assertDoesNotExist()
        composeTestRule.onNodeWithTag(FUNDING_INSTRUMENT_CHIP_TEST_TAG).assertContentDescriptionEquals("PayPal Credit")
    }

    @Test
    fun `the Pay Later message lines up with the label, or the leading edge when the logo is hidden`() {
        render(CARD, MESSAGE)
        assertTrue(abs(bounds(CREDIT_MESSAGING_TEST_TAG).left - textBounds("PayPal").left) <= 1f)

        render(CARD, MESSAGE, style = PayPalSavedPaymentMethodViewStyle(showPayPalLogo = false))

        assertTrue(abs(bounds(CREDIT_MESSAGING_TEST_TAG).left - bounds(BRAND_MARK_TEST_TAG).left) <= 1f)
    }

    @Test
    fun `an error or offline state shows only the logo and label`() {
        listOf(PayPalSavedPaymentMethodDisplayState.Error, PayPalSavedPaymentMethodDisplayState.NoNetwork).forEach {
            render(it, MESSAGE)

            composeTestRule.onNodeWithTag(BRAND_MARK_TEST_TAG).assertIsDisplayed()
            assertDoesNotExist(FUNDING_INSTRUMENT_CHIP_TEST_TAG)
            assertDoesNotExist(CREDIT_MESSAGING_TEST_TAG)
        }
    }

    @Test
    fun `an error with the logo and label both hidden renders nothing`() {
        val noBrand = PayPalSavedPaymentMethodViewStyle(showPayPalLogo = false, showPayPalLabel = false)

        render(PayPalSavedPaymentMethodDisplayState.NoNetwork, style = noBrand)
        assertDoesNotExist(FUNDING_INSTRUMENT_CHIP_TEST_TAG)
        assertDoesNotExist(BRAND_MARK_TEST_TAG)
        composeTestRule.onRoot().assert(SemanticsMatcher("has no children") { it.children.isEmpty() })

        render(CARD, style = noBrand)
        composeTestRule.onNodeWithTag(FUNDING_INSTRUMENT_CHIP_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun `hidden renders nothing`() {
        render(PayPalSavedPaymentMethodDisplayState.Hidden, MESSAGE)

        assertDoesNotExist(BRAND_MARK_TEST_TAG)
        assertDoesNotExist(FUNDING_INSTRUMENT_CHIP_TEST_TAG)
        assertDoesNotExist(CREDIT_MESSAGING_TEST_TAG)
    }

    @Test
    fun `the Pay Later message opens its Learn more link`() {
        render(CARD, MESSAGE)

        val text = composeTestRule.onNodeWithText("Pay in 4. Learn more")
            .fetchSemanticsNode().config[SemanticsProperties.Text].single()
        val link = text.getLinkAnnotations(0, text.length).single().item as LinkAnnotation.Clickable
        link.linkInteractionListener?.onClick(link)

        assertEquals(listOf("https://www.paypal.com/learn-more"), openedUrls)
    }

    @Test
    fun `the Pay Later message is hidden after an edit or when the style turns it off`() {
        render(CARD, MESSAGE, isEditCompleted = true)
        assertDoesNotExist(CREDIT_MESSAGING_TEST_TAG)

        render(CARD, MESSAGE, style = PayPalSavedPaymentMethodViewStyle(showPayPalCreditMessaging = false))
        assertDoesNotExist(CREDIT_MESSAGING_TEST_TAG)
    }

    @Test
    fun `the chip sits next to the logo and label when it fits`() {
        render(CARD, width = WIDE)

        val brand = bounds(BRAND_MARK_TEST_TAG)
        val chip = bounds(FUNDING_INSTRUMENT_CHIP_TEST_TAG)
        assertTrue(chip.left >= brand.right)
        assertTrue(abs(chip.center.y - brand.center.y) <= 1f)
    }

    @Test
    fun `the chip goes below the logo and label when it doesn't fit`() {
        render(LONG_EMAIL, width = NARROW)

        val brand = bounds(BRAND_MARK_TEST_TAG)
        val chip = bounds(FUNDING_INSTRUMENT_CHIP_TEST_TAG)
        assertTrue(chip.top >= brand.bottom)
        assertTrue(chip.left <= brand.left + 1f)
    }

    @Test
    fun `the loading shimmer never stacks`() {
        render(PayPalSavedPaymentMethodDisplayState.Loading, width = NARROW)

        assertTrue(bounds(FUNDING_INSTRUMENT_SHIMMER_TEST_TAG).left >= bounds(BRAND_MARK_TEST_TAG).right)
    }

    @Test
    fun `right-to-left layouts place the chip to the left of the logo and label`() {
        render(CARD, width = WIDE, layoutDirection = LayoutDirection.Rtl)

        assertTrue(bounds(FUNDING_INSTRUMENT_CHIP_TEST_TAG).right <= bounds(BRAND_MARK_TEST_TAG).left)
    }

    @Suppress("LongParameterList")
    private fun render(
        fundingInstrumentState: PayPalSavedPaymentMethodDisplayState,
        creditMessagingState: PayPalCreditMessagingDisplayState = PayPalCreditMessagingDisplayState.Hidden,
        style: PayPalSavedPaymentMethodViewStyle = PayPalSavedPaymentMethodViewStyle(),
        isEditCompleted: Boolean = false,
        editEnabled: Boolean = true,
        width: Int = WIDE,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr
    ) {
        val isFirstRender = content.value == null
        content.value = Content(
            fundingInstrumentState,
            creditMessagingState,
            style,
            isEditCompleted,
            editEnabled,
            width,
            layoutDirection
        )
        if (isFirstRender) {
            composeTestRule.setContent {
                val current = content.value ?: return@setContent
                CompositionLocalProvider(LocalLayoutDirection provides current.layoutDirection) {
                    PayPalSavedPaymentMethodContent(
                        fundingInstrumentState = current.fundingInstrumentState,
                        creditMessagingState = current.creditMessagingState,
                        style = current.style,
                        onEditClick = { editClicks++ },
                        onLearnMoreClick = { openedUrls += it },
                        modifier = Modifier.width(current.width.dp),
                        editEnabled = current.editEnabled,
                        isEditCompleted = current.isEditCompleted
                    )
                }
            }
        }
        Snapshot.sendApplyNotifications()
        composeTestRule.mainClock.advanceTimeByFrame()
    }

    private fun bounds(tag: String): Rect = composeTestRule.onNodeWithTag(tag).getBoundsInRoot().toRect()

    private fun textBounds(text: String): Rect = composeTestRule.onNodeWithText(text).getBoundsInRoot().toRect()

    private fun DpRect.toRect() = Rect(left.value, top.value, right.value, bottom.value)

    private fun assertDoesNotExist(tag: String) = composeTestRule.onNodeWithTag(tag).assertDoesNotExist()

    private data class Content(
        val fundingInstrumentState: PayPalSavedPaymentMethodDisplayState,
        val creditMessagingState: PayPalCreditMessagingDisplayState,
        val style: PayPalSavedPaymentMethodViewStyle,
        val isEditCompleted: Boolean,
        val editEnabled: Boolean,
        val width: Int,
        val layoutDirection: LayoutDirection
    )

    private companion object {
        const val WIDE = 600
        const val NARROW = 240
        val CARD = PayPalSavedPaymentMethodDisplayState.Content(
            PayPalSavedPaymentMethod(PayPalSavedPaymentMethodType.CARD, "Visa", null, "1234", null)
        )
        val PAYPAL_CREDIT = PayPalSavedPaymentMethodDisplayState.Content(
            PayPalSavedPaymentMethod(PayPalSavedPaymentMethodType.PAYPAL_CREDIT, "PayPal Credit", null, null, null)
        )
        val LONG_EMAIL = PayPalSavedPaymentMethodDisplayState.NoFundingInstrument(
            "a.very.long.buyer.email.address@example.com",
            isEditable = true
        )
        val MESSAGE = PayPalCreditMessagingDisplayState.Content(
            PayPalCreditMessagingContent(
                "Pay in 4.",
                PayPalCreditMessagingContent.LearnMore(
                    "Learn more",
                    "https://www.paypal.com/learn-more",
                    isEmbeddable = false
                )
            )
        )
    }
}
