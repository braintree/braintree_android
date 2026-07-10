package com.braintreepayments.api.uicomponents

import androidx.compose.ui.graphics.Color

/**
 * Theming for the [com.braintreepayments.api.uicomponents.compose.EditFiComponentView].
 *
 * Models the FI "pill" shown next to the PayPal label (brand art + masked number + edit pencil)
 * per the View/Edit FI design. Defaults match the design spec; merchants may override colors.
 *
 * @property chipBackgroundColor background of the FI pill.
 * @property primaryTextColor    color of the masked-number / fallback label.
 * @property editIconTint        tint applied to the edit pencil icon.
 * @property addCardBackgroundColor background of the "add a card" chip (empty/disallowed wallet).
 * @property warningIconTint     tint applied to the warning glyph in the "add a card" chip.
 */
@Suppress("MagicNumber")
data class EditFiComponentStyle(
    val chipBackgroundColor: Color = Color(0xFFF5F7FA),
    val primaryTextColor: Color = Color(0xFF000000),
    val editIconTint: Color = Color(0xFF000000),
    val addCardBackgroundColor: Color = Color(0xFFFFF5E1),
    val warningIconTint: Color = Color(0xFFAA7100),
)
