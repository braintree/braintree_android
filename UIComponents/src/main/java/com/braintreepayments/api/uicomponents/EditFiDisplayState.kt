package com.braintreepayments.api.uicomponents

/**
 * What the [com.braintreepayments.api.uicomponents.compose.EditFiComponentView] should render.
 * Mapped from the FI fetch / edit result by the owning component.
 */
sealed interface EditFiDisplayState {

    /** Initial FI fetch is in flight — show the shimmer skeleton. */
    data object Loading : EditFiDisplayState

    /** An FI is available — show its brand art, masked number, and the edit pencil. */
    data class Content(val fiSummary: FiSummary) : EditFiDisplayState

    /**
     * No FI could be resolved (fetch failed / no vaulted instrument) — show the no-FI fallback
     * pill: "<buyer email> | Pay in Full" with the edit pencil, no icon, no last4 (LLD §13.2).
     */
    data class NoFi(val buyerEmail: String) : EditFiDisplayState

    /**
     * The buyer's wallet is empty / disallowed and wallet cycling is not possible, so no FI can be
     * shown to select — prompt the buyer to add a card. Renders the amber chip
     * "⚠ To continue, add a card", where "add a card" is a link that opens the add-card flow.
     */
    data object AddCard : EditFiDisplayState
}
