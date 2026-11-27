package com.braintreepayments.api.paypalsavedpaymentmethod

internal object PayPalSavedPaymentMethodAnalytics {

    // Render events
    const val RENDER_STARTED = "paypal-saved-payment-method:render:started"
    const val RENDER_PRESENTED = "paypal-saved-payment-method:render:presented"

    // Funding instrument events
    const val FUNDING_INSTRUMENT_PRESENTED = "paypal-saved-payment-method:render:funding-instrument:presented"
    const val FUNDING_INSTRUMENT_FAILED = "paypal-saved-payment-method:render:funding-instrument:failed"
    const val FUNDING_INSTRUMENT_EDIT_SELECTED =
        "paypal-saved-payment-method:render:funding-instrument-edit:selected"

    // Credit messaging events
    const val CREDIT_MESSAGING_PRESENTED = "paypal-saved-payment-method:render:credit-messaging:presented"
    const val CREDIT_MESSAGING_FAILED = "paypal-saved-payment-method:render:credit-messaging:failed"
    const val CREDIT_MESSAGING_LEARN_MORE_SELECTED =
        "paypal-saved-payment-method:render:credit-messaging-learn-more:selected"

    // ui_type: the view the events come from
    const val UI_TYPE_COMPOSE = "compose"
    const val UI_TYPE_XML = "xml"

    // render_trigger: the initial funding instrument fetch, or the refetch after an edit
    const val RENDER_TRIGGER_STICKY_FI = "sticky_fi"
    const val RENDER_TRIGGER_FI_FROM_APPROVED_CHECKOUT = "fi_from_approved_checkout"

    // shown_content: what a render showed
    const val SHOWN_CONTENT_FUNDING_INSTRUMENT = "funding_instrument"
    const val SHOWN_CONTENT_EMAIL_ONLY = "email_only"
    const val SHOWN_CONTENT_BRAND_ONLY = "brand_only"
    const val SHOWN_CONTENT_HIDDEN = "hidden"
}
