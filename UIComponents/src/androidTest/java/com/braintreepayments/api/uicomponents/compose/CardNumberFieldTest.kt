package com.braintreepayments.api.uicomponents.compose

import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class                                                                                                                                                                                                                                          CardNumberFieldTest {

    @get:Rule
    val composeTestRule = createComposeRule()
    private lateinit var state: CardFieldsState

    @Before
    fun setUp() {
        composeTestRule.setContent {
            state = rememberCardFieldsState()
            CardFields(state = state)
        }
    }

    private fun cardNumberField() = composeTestRule.onNode(hasSetTextAction())

    /** Clicks empty space below the card number field to move focus away from it. */
    private fun clearFocus() {
        composeTestRule.onRoot().performTouchInput { click(bottomRight) }
    }

    @Test
    fun cardNumberField_displaysVisaBrand_whenCardStartsWith4() {
        cardNumberField().performTextInput("4")

        composeTestRule.onNodeWithContentDescription("Visa").assertExists()
    }

    @Test
    fun cardNumberField_displaysMastercardBrand_whenCardStartsWith51() {
        cardNumberField().performTextInput("51")

        composeTestRule.onNodeWithContentDescription("Mastercard").assertExists()
    }

    @Test
    fun cardNumberField_displaysAmexBrand_whenCardStartsWith34() {
        cardNumberField().performTextInput("34")

        composeTestRule.onNodeWithContentDescription("American Express").assertExists()
    }

    @Test
    fun cardNumberField_displaysDiscoverBrand_whenCardStartsWith6011() {
        cardNumberField().performTextInput("6011")

        composeTestRule.onNodeWithContentDescription("Discover").assertExists()
    }

    @Test
    fun cardNumberField_displaysJcbBrand_whenCardStartsWith35() {
        cardNumberField().performTextInput("35")

        composeTestRule.onNodeWithContentDescription("JCB").assertExists()
    }

    @Test
    fun cardNumberField_displaysUnionPayBrand_whenCardStartsWith621() {
        cardNumberField().performTextInput("621")

        composeTestRule.onNodeWithContentDescription("UnionPay").assertExists()
    }

    @Test
    fun cardNumberField_displaysDinersClubBrand_whenCardStartsWith36() {
        cardNumberField().performTextInput("36")

        composeTestRule.onNodeWithContentDescription("Diners Club").assertExists()
    }

    @Test
    fun cardNumberField_displaysMaestroBrand_whenCardStartsWith56() {
        cardNumberField().performTextInput("56")

        composeTestRule.onNodeWithContentDescription("Maestro").assertExists()
    }

    @Test
    fun cardNumberField_displaysHiperBrand_whenCardStartsWith637095() {
        cardNumberField().performTextInput("637095")

        composeTestRule.onNodeWithContentDescription("Hiper").assertExists()
    }

    @Test
    fun cardNumberField_displaysHipercardBrand_whenCardStartsWith606282() {
        cardNumberField().performTextInput("606282")

        composeTestRule.onNodeWithContentDescription("Hipercard").assertExists()
    }

    @Test
    fun cardNumberField_displaysEloBrand_whenCardStartsWith636297() {
        cardNumberField().performTextInput("636297")

        composeTestRule.onNodeWithContentDescription("Elo").assertExists()
    }

    @Test
    fun cardNumberField_displaysMirBrand_whenCardStartsWith2200() {
        cardNumberField().performTextInput("2200")

        composeTestRule.onNodeWithContentDescription("Mir").assertExists()
    }

    @Test
    fun cardNumberField_displaysVerveBrand_whenCardStartsWith506099() {
        cardNumberField().performTextInput("506099")

        composeTestRule.onNodeWithContentDescription("Verve").assertExists()
    }

    @Test
    fun cardNumberField_displaysUnknownBrand_whenCardPrefixMatchesNoBrand() {
        cardNumberField().performTextInput("99")

        composeTestRule.onNodeWithContentDescription("Unknown Card Brand").assertExists()
    }

    @Test
    fun cardNumberField_formatsVisaNumber_inGroupsOfFour() {
        cardNumberField().performTextInput("4111111111111111")

        composeTestRule.onNodeWithText("4111 1111 1111 1111").assertExists()
    }

    @Test
    fun cardNumberField_formatsAmexNumber_inFourSixFiveGroups() {
        cardNumberField().performTextInput("378282246310005")

        composeTestRule.onNodeWithText("3782 822463 10005").assertExists()
    }

    @Test
    fun cardNumberField_rejectsDigitsPastVisaMaxLength() {
        cardNumberField().performTextInput("41111111111111119999")

        composeTestRule.runOnIdle {
            assertEquals("4111111111111111", state.cardNumber.value.text)
        }
    }

    @Test
    fun cardNumberField_showsError_afterIncompleteInputAndBlur() {
        cardNumberField().performTextInput("4111111111")
        clearFocus()

        composeTestRule.onNodeWithText("Card number is invalid").assertExists()
    }

    @Test
    fun cardNumberField_clearsError_afterCorrectingInput() {
        cardNumberField().performTextInput("4111111111")
        clearFocus()
        composeTestRule.onNodeWithText("Card number is invalid").assertExists()

        cardNumberField().performTextClearance()
        cardNumberField().performTextInput("4111111111111111")
        clearFocus()

        composeTestRule.onNodeWithText("Card number is invalid").assertDoesNotExist()
    }

}
