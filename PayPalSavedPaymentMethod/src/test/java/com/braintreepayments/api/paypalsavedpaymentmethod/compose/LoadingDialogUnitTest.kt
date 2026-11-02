package com.braintreepayments.api.paypalsavedpaymentmethod.compose

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
        // It animates forever, so the clock is advanced by hand.
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent { LoadingDialog() }
        composeTestRule.mainClock.advanceTimeByFrame()
    }

    @Test
    fun `shows a spinner that screen readers announce as loading`() {
        composeTestRule.onNodeWithContentDescription("Loading").assertExists()
    }
}
