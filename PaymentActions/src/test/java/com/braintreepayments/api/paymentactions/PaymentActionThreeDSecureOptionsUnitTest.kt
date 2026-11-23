package com.braintreepayments.api.paymentactions

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PaymentActionThreeDSecureOptionsUnitTest {

    @Test
    fun `defaults match the 3D Secure module's ThreeDSecureRequest defaults`() {
        val options = PaymentActionThreeDSecureOptions()

        assertEquals(PaymentActionThreeDSecureUiType.BOTH, options.uiType)
        assertNull(options.renderTypes)
        assertNull(options.uiCustomization)
        assertNull(options.requestorAppUrl)
    }

    @Test
    fun `customization numbers default to 0, which means not set`() {
        assertEquals(0, PaymentActionThreeDSecureButtonCustomization().textFontSize)
        assertEquals(0, PaymentActionThreeDSecureButtonCustomization().cornerRadius)
        assertEquals(0, PaymentActionThreeDSecureLabelCustomization().headingTextFontSize)
        assertEquals(0, PaymentActionThreeDSecureTextBoxCustomization().borderWidth)
        assertEquals(0, PaymentActionThreeDSecureToolbarCustomization().textFontSize)
    }
}
