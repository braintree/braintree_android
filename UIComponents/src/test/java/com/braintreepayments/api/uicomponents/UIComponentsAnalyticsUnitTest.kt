package com.braintreepayments.api.uicomponents

import org.junit.Assert.assertEquals
import org.junit.Test

class UIComponentsAnalyticsUnitTest {

    @Test
    fun `UIComponentsAnalytics constants match expected event names`() {
        assertEquals("ui-components:paypal-button:presented", UIComponentsAnalytics.PAYPAL_BUTTON_PRESENTED)
        assertEquals("ui-components:paypal-button:selected", UIComponentsAnalytics.PAYPAL_BUTTON_SELECTED)

        assertEquals("ui-components:venmo-button:presented", UIComponentsAnalytics.VENMO_BUTTON_PRESENTED)
        assertEquals("ui-components:venmo-button:selected", UIComponentsAnalytics.VENMO_BUTTON_SELECTED)

        assertEquals("ui-components:pay-later-button:presented", UIComponentsAnalytics.PAY_LATER_BUTTON_PRESENTED)
        assertEquals("ui-components:pay-later-button:selected", UIComponentsAnalytics.PAY_LATER_BUTTON_SELECTED)

        assertEquals("ui-components:card-fields:presented", UIComponentsAnalytics.CARD_FIELDS_PRESENTED)
        assertEquals("ui-components:card-fields:validated", UIComponentsAnalytics.CARD_FIELDS_VALIDATED)

        assertEquals("compose", UIComponentsAnalytics.UI_TYPE_COMPOSE)
        assertEquals("xml", UIComponentsAnalytics.UI_TYPE_XML_VIEW)
    }
}