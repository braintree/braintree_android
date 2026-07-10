package com.braintreepayments.api.uicomponents

import androidx.annotation.DrawableRes
import com.braintreepayments.api.uicomponents.cardfields.CardBrand

/**
 * A summary of the funding instrument (FI) that PayPal will charge for a vaulted buyer.
 *
 * Used by [com.braintreepayments.api.uicomponents.compose.EditFiComponentView] to render the FI
 * chip. Populated from the BT backend `fetch_fi` / `fetch_selected_fi` response (LLD §8, §11).
 *
 * @property brand    the FI brand (e.g. "Visa", "Mastercard"); `null` when unknown.
 * @property last4    the last four digits of the FI; `null` when unavailable.
 * @property type     the FI category (card / bank / PayPal product / Pay Later). Drives the tile
 * icon and, for PayPal products, whether a product name is shown instead of a masked number.
 * @property displayName the product name to show instead of the masked number, e.g. "PayPal Credit
 * Card", "PayPal Cashback Mastercard", "Pay in 4", "Pay Monthly". Used by [FiType.PAYPAL] /
 * [FiType.PAY_LATER] tiles; `null` for card / bank tiles (which show `••last4`).
 * @property imageUrl remote brand-art URL returned by the backend; optional. Not yet loaded by the
 * UI (reserved for a future pass) — the component currently resolves a bundled brand drawable.
 */
data class FiSummary(
    val brand: String? = null,
    val last4: String? = null,
    val type: FiType = FiType.CARD,
    val displayName: String? = null,
    val imageUrl: String? = null,
) {

    /**
     * The tile icon drawable, or `null` when the tile shows no icon (Pay Later products render the
     * product name only). Resolution lives on the model (mirrors [CardBrand.iconRes]); the view
     * just reads it.
     *
     * Interim resolution: bundled `card_fields_cc_*` brand art, the generic card / bank glyphs, and
     * the PayPal wordmark (until the PDS monogram asset lands). Remote [imageUrl] loading — with
     * these bundled drawables as the §13.2 fallback — is wired in the API pass.
     */
    @get:DrawableRes
    internal val iconRes: Int?
        get() = when (type) {
            FiType.BANK -> R.drawable.edit_fi_generic_bank
            // Pay Later products (Pay in 4 / Pay Monthly): product name only, no icon.
            FiType.PAY_LATER -> null
            // PayPal-branded products (Credit, Credit Card, Cashback Mastercard): PayPal monogram.
            FiType.PAYPAL -> R.drawable.paypal_monogram
            FiType.CARD -> {
                // Reuse CardBrand's brand→art map rather than duplicating it. Brands with no
                // dedicated art (incl. UNKNOWN) fall back to the §13.2 generic glyph.
                val brandIcon = CardBrand.fromDisplayName(brand).iconRes
                if (brandIcon != R.drawable.card_fields_cc_unknown) {
                    brandIcon
                } else {
                    R.drawable.edit_fi_generic_card
                }
            }
        }
}

/**
 * The category of a [FiSummary], used to pick the tile icon and label style (LLD §13.2).
 *
 * - [CARD] / [BANK]: show brand art (or the generic card / bank fallback glyph) + `••last4`.
 * - [PAYPAL]: PayPal-branded product (e.g. PayPal Credit Card, PayPal Cashback Mastercard) — show
 *   the PayPal logo + the product [FiSummary.displayName].
 * - [PAY_LATER]: Pay Later product (e.g. Pay in 4, Pay Monthly) — show the product
 *   [FiSummary.displayName] only, with no icon.
 */
enum class FiType {
    CARD,
    BANK,
    PAYPAL,
    PAY_LATER,
}
