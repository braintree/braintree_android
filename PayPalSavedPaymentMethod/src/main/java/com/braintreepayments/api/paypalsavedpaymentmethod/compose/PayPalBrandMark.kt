package com.braintreepayments.api.paypalsavedpaymentmethod.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.braintreepayments.api.paypalsavedpaymentmethod.R
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalSavedPaymentMethodStyleResolver

/** The PayPal logo and label, either of which the merchant can hide. */
@Composable
internal fun PayPalBrandMark(resolvedStyle: PayPalSavedPaymentMethodStyleResolver, labelGap: Dp) {
    if (!resolvedStyle.showPayPalLogo && !resolvedStyle.showPayPalLabel) return
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.testTag(BRAND_MARK_TEST_TAG)) {
        if (resolvedStyle.showPayPalLogo) {
            Image(
                painter = painterResource(R.drawable.ic_paypal_brand_logo),
                // The label already says "PayPal".
                contentDescription = if (resolvedStyle.showPayPalLabel) {
                    null
                } else {
                    stringResource(R.string.paypal_saved_payment_method_label)
                },
                // Square box; the artwork fits inside it.
                modifier = Modifier.size(resolvedStyle.logoWidthDp.dp),
                contentScale = ContentScale.Fit
            )
        }
        if (resolvedStyle.showPayPalLabel) {
            Text(
                text = stringResource(R.string.paypal_saved_payment_method_label),
                color = Color(resolvedStyle.textColor),
                fontSize = resolvedStyle.labelFontSizeSp.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = resolvedStyle.fontFamily,
                // The gap sits between the logo and the label.
                modifier = Modifier.padding(start = if (resolvedStyle.showPayPalLogo) labelGap else 0.dp)
            )
        }
    }
}
