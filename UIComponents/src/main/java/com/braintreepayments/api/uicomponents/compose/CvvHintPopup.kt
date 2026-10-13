package com.braintreepayments.api.uicomponents.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.braintreepayments.api.uicomponents.R

/**
 * Positions the popup so its bottom-end corner sits just above the anchor's top-end corner,
 * using the anchor's actual measured bounds rather than an externally tracked height.
 */
private object AboveAnchorEndPopupPositionProvider : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val x = anchorBounds.right - popupContentSize.width
        val y = anchorBounds.top - popupContentSize.height
        return IntOffset(x, y)
    }
}

/**
 * Compose equivalent of [com.braintreepayments.api.uicomponents.cardfields.CvvHintOverlay]. Must be
 * called from within the same [Box][androidx.compose.foundation.layout.Box] as the CVV field's
 * trailing icon so it anchors above it.
 */
@Composable
internal fun CvvHintPopup(onDismissRequest: () -> Unit) {
    Popup(
        popupPositionProvider = AboveAnchorEndPopupPositionProvider,
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true)
    ) {
        val cornerRadius = dimensionResource(R.dimen.card_field_corner_radius)
        val borderWidth = dimensionResource(R.dimen.card_field_border_width)
        val elevation = dimensionResource(R.dimen.cvv_overlay_elevation)
        Column(
            modifier = Modifier
                .width(dimensionResource(R.dimen.cvv_overlay_max_width))
                .shadow(elevation, RoundedCornerShape(cornerRadius))
                .clip(RoundedCornerShape(cornerRadius))
                .background(colorResource(R.color.card_field_background))
                .border(borderWidth, Color.White, RoundedCornerShape(cornerRadius))
                .padding(dimensionResource(R.dimen.cvv_overlay_padding))
        ) {
            val closeIconSize = dimensionResource(R.dimen.cvv_overlay_close_icon_size)
            val headerTextSize = spDimensionResource(R.dimen.cvv_overlay_header_text_size)
            val bodyTextSize = spDimensionResource(R.dimen.cvv_overlay_body_text_size)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.cvv_overlay_header),
                    modifier = Modifier.weight(1f),
                    color = colorResource(R.color.card_field_text),
                    fontSize = headerTextSize,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismissRequest, modifier = Modifier.size(closeIconSize)) {
                    Icon(
                        painter = painterResource(R.drawable.cvv_hint_close),
                        contentDescription = stringResource(R.string.cvv_overlay_close_description),
                        modifier = Modifier.size(closeIconSize)
                    )
                }
            }
            Text(
                text = stringResource(R.string.cvv_overlay_body),
                modifier = Modifier.padding(
                    top = dimensionResource(R.dimen.cvv_overlay_body_padding_top),
                    end = dimensionResource(R.dimen.cvv_overlay_body_padding_end)
                ),
                color = colorResource(R.color.card_field_text),
                fontSize = bodyTextSize
            )
        }
    }
}
