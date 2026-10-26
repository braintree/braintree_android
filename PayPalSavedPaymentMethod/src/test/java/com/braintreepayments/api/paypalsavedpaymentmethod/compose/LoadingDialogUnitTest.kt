package com.braintreepayments.api.paypalsavedpaymentmethod.compose

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LoadingDialogUnitTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        // The spinner animates forever, so the clock is driven by hand instead of waiting for the UI to go idle.
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent { LoadingDialog() }
        composeTestRule.mainClock.advanceTimeByFrame()
    }

    @Test
    fun `shows a spinner that screen readers announce as loading`() {
        composeTestRule.onNodeWithContentDescription("Loading").assertExists()
    }

    @Test
    fun `the spinner is an indeterminate progress indicator`() {
        composeTestRule.onNodeWithContentDescription("Loading")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
    }
}
