package com.braintreepayments.api.uicomponents.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.input.TextFieldValue
import androidx.test.platform.app.InstrumentationRegistry
import com.braintreepayments.api.uicomponents.R
import org.junit.Rule
import org.junit.Test

class CardCvvFieldTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private fun str(resId: Int) = context.getString(resId)

    private fun setCvvField(errorText: String? = null) {
        composeTestRule.setContent {
            var value by remember { mutableStateOf(TextFieldValue("")) }
            CardCvvField(
                value = value,
                onValueChange = { value = it },
                errorText = errorText
            )
        }
    }

    @Test
    fun typingDigits_updatesFieldText() {
        setCvvField()

        composeTestRule.onNodeWithContentDescription(str(R.string.cvv_accessibility))
            .performTextInput("123")

        composeTestRule.onNodeWithContentDescription(str(R.string.cvv_accessibility))
            .assert(hasText("123"))
    }

    @Test
    fun tappingHintIcon_showsHintPopup() {
        setCvvField()

        composeTestRule.onNodeWithContentDescription(str(R.string.cvv_hint_icon_description))
            .performClick()

        composeTestRule.onNodeWithText(str(R.string.cvv_overlay_body)).assertIsDisplayed()
    }

    @Test
    fun hintPopup_isNotShown_beforeIconIsTapped() {
        setCvvField()

        composeTestRule.onNodeWithText(str(R.string.cvv_overlay_body)).assertDoesNotExist()
    }

    @Test
    fun tappingCloseButton_dismissesHintPopup() {
        setCvvField()
        composeTestRule.onNodeWithContentDescription(str(R.string.cvv_hint_icon_description))
            .performClick()
        composeTestRule.onNodeWithText(str(R.string.cvv_overlay_body)).assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription(str(R.string.cvv_overlay_close_description))
            .performClick()

        composeTestRule.onNodeWithText(str(R.string.cvv_overlay_body)).assertDoesNotExist()
    }
}
