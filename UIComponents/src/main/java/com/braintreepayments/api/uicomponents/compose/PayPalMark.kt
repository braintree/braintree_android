@file:Suppress("UnusedPrivateMember")

package com.braintreepayments.api.uicomponents.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.braintreepayments.api.uicomponents.R

/**
 * The PayPal brand "Mark" — the full-color PayPal monogram centered in a 45x30dp white, rounded,
 * #CCCCCC-bordered box (Figma "Mark" node).
 *
 * This is a standalone view, separate from the Edit FI chip: in the design it sits before the
 * "PayPal" label in the merchant "Pay with" row, distinct from the funding-instrument icon shown
 * inside [EditFiComponentView]'s chip. Dimensions follow mobile guidelines (whole dp in
 * `res/values/dimens.xml`, colors in `res/values/colors.xml`) — not the JS SDK's fractional values.
 *
 * @param modifier Compose modifier for the outer box.
 */
@Composable
internal fun PayPalMark(modifier: Modifier = Modifier) {
    val cornerRadius = RoundedCornerShape(dimensionResource(R.dimen.paypal_mark_corner_radius))
    Box(
        modifier = modifier
            .width(dimensionResource(R.dimen.paypal_mark_width))
            .height(dimensionResource(R.dimen.paypal_mark_height))
            .border(
                width = dimensionResource(R.dimen.paypal_mark_border_width),
                color = colorResource(R.color.paypal_mark_border),
                shape = cornerRadius,
            )
            .background(color = colorResource(R.color.paypal_mark_background), shape = cornerRadius)
            .padding(
                start = dimensionResource(R.dimen.paypal_mark_padding_horizontal),
                top = dimensionResource(R.dimen.paypal_mark_padding_top),
                end = dimensionResource(R.dimen.paypal_mark_padding_horizontal),
                bottom = dimensionResource(R.dimen.paypal_mark_padding_bottom),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.paypal_monogram),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Preview(name = "PayPal Mark", showBackground = true)
@Composable
private fun PreviewPayPalMark() {
    PayPalMark(modifier = Modifier.padding(16.dp))
}
