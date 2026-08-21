package com.braintreepayments.api.paypalsavedpaymentmethod

import com.braintreepayments.api.core.ExperimentalBetaApi
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalBetaApi::class)
class PayPalCreditMessagingUtilsUnitTest {

    @Test
    fun buildContent_withTextAndImageMainItems_substitutesAlternativeTextForImages() {
        val content = JSONObject(
            """
            {"main_items":[
              {"type":"TEXT","text":"Or pay in 4 interest-free payments with "},
              {"type":"IMAGE","alternative_text":"PayPal","name":"paypal_logo"},
              {"type":"TEXT","text":"."}
            ]}
            """.trimIndent()
        )

        val result = PayPalCreditMessagingUtils.buildContent(content)

        assertEquals("Or pay in 4 interest-free payments with PayPal.", result.message)
        assertEquals("", result.learnMoreText)
        assertEquals("", result.learnMoreUrl)
    }

    @Test
    fun buildContent_withDisclaimerItems_appendsAfterMainItemsWithSpace() {
        val content = JSONObject(
            """
            {"main_items":[{"type":"TEXT","text":"As low as ${'$'}10/mo"}],
             "disclaimer_items":[{"type":"TEXT","text":"Available to US residents only."}]}
            """.trimIndent()
        )

        val result = PayPalCreditMessagingUtils.buildContent(content)

        assertEquals("As low as \$10/mo Available to US residents only.", result.message)
    }

    @Test
    fun buildContent_withoutDisclaimerItems_returnsMainItemsOnly() {
        val content = JSONObject(
            """{"main_items":[{"type":"TEXT","text":"As low as ${'$'}10/mo"}]}"""
        )

        val result = PayPalCreditMessagingUtils.buildContent(content)

        assertEquals("As low as \$10/mo", result.message)
    }

    @Test
    fun buildContent_withoutMainItems_returnsEmptyMessage() {
        val result = PayPalCreditMessagingUtils.buildContent(JSONObject())

        assertEquals("", result.message)
    }

    @Test
    fun buildContent_withLinkActionItem_extractsLearnMoreTextAndUrl() {
        val content = JSONObject(
            """
            {"action_items":[{"type":"LINK","text":"Learn more","click_url":"https://paypal.com/learn"}]}
            """.trimIndent()
        )

        val result = PayPalCreditMessagingUtils.buildContent(content)

        assertEquals("Learn more", result.learnMoreText)
        assertEquals("https://paypal.com/learn", result.learnMoreUrl)
    }

    @Test
    fun buildContent_withMultipleActionItems_picksFirstLinkType() {
        val content = JSONObject(
            """
            {"action_items":[
              {"type":"OTHER","text":"Ignore me","click_url":"https://paypal.com/ignore"},
              {"type":"LINK","text":"Learn more","click_url":"https://paypal.com/learn"}
            ]}
            """.trimIndent()
        )

        val result = PayPalCreditMessagingUtils.buildContent(content)

        assertEquals("Learn more", result.learnMoreText)
        assertEquals("https://paypal.com/learn", result.learnMoreUrl)
    }

    @Test
    fun buildContent_withoutActionItems_returnsEmptyLearnMoreFields() {
        val result = PayPalCreditMessagingUtils.buildContent(JSONObject())

        assertEquals("", result.learnMoreText)
        assertEquals("", result.learnMoreUrl)
    }
}
