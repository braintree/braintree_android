package com.braintreepayments.api.paypalsavedpaymentmethod.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ShimmerPlaceholderUnitTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        // The highlight sweeps forever, so the clock is driven by hand instead of waiting for the UI to go idle.
        composeTestRule.mainClock.autoAdvance = false
    }

    @Test
    fun `announces the given content description`() {
        render { ShimmerPlaceholder(contentDescription = DESCRIPTION) }

        composeTestRule.onNodeWithContentDescription(DESCRIPTION).assertExists()
    }

    @Test
    fun `is hidden from screen readers when the content description is null`() {
        render { ShimmerPlaceholder(contentDescription = null) }

        composeTestRule.onNode(hasContentDescription(DESCRIPTION)).assertDoesNotExist()
    }

    @Test
    fun `uses the given height`() {
        render {
            Box(Modifier.testTag(TAG)) {
                ShimmerPlaceholder(contentDescription = null, height = 20.dp)
            }
        }

        composeTestRule.onNodeWithTag(TAG).assertHeightIsEqualTo(20.dp)
    }

    @Test
    fun `fills the width it's given, including the trailing gap`() {
        render {
            Box(Modifier.testTag(TAG)) {
                ShimmerPlaceholder(contentDescription = null, modifier = Modifier.width(100.dp), trailingGap = 30.dp)
            }
        }

        composeTestRule.onNodeWithTag(TAG).assertWidthIsEqualTo(100.dp)
    }

    private fun render(content: @Composable () -> Unit) {
        composeTestRule.setContent(content)
        composeTestRule.mainClock.advanceTimeByFrame()
    }

    private companion object {
        const val DESCRIPTION = "Loading saved payment method"
        const val TAG = "shimmer-container"
    }
}
