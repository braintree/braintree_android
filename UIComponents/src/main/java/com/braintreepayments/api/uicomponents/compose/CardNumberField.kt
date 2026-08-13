package com.braintreepayments.api.uicomponents.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import com.braintreepayments.api.uicomponents.R
import com.braintreepayments.api.uicomponents.cardfields.CardBrand

/** Compose equivalent of [com.braintreepayments.api.uicomponents.cardfields.CardNumberTextInputView]. */
@Composable
internal fun CardNumberField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    brand: CardBrand,
    modifier: Modifier = Modifier,
    errorText: String? = null,
    focusRequester: FocusRequester? = null,
    onFocusChanged: (Boolean) -> Unit = {}
) {
    CardFieldTextField(
        value = value,
        onValueChange = onValueChange,
        hint = stringResource(R.string.card_number_hint),
        modifier = modifier,
        errorText = errorText,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        visualTransformation = CardNumberVisualTransformation(brand.formatGaps),
        focusRequester = focusRequester,
        onFocusChanged = onFocusChanged,
        leadingIcon = {
            Image(
                painter = painterResource(brand.iconRes),
                contentDescription = stringResource(brand.iconContentDescriptionRes),
                modifier = Modifier
                    .padding(end = dimensionResource(R.dimen.card_icon_margin))
                    .size(
                        width = dimensionResource(R.dimen.card_icon_width),
                        height = dimensionResource(R.dimen.card_icon_height)
                    )
                    .clip(RoundedCornerShape(dimensionResource(R.dimen.card_icon_corner_radius)))
            )
        }
    )
}
