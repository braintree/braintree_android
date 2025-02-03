package com.braintreepayments.api.core.atomicevent

class AtomicEventConstants {

    object InteractionType {
        const val PAY_WITH_PAYPAL = "Pay_With_PayPal"
        const val CLICK = "click"
        const val APPROVE_BILLING_AGREEMENT = "Approve_Billing_Agreement"
        const val CLICK_CANCEL_AND_RETURN_TO_MERCHANT = "Click_Cancel_And_Return_To_Merchant"
        const val RENDER = "render"
    }

    object NavType {
        const val NAVIGATE = "navigate"
    }

    object Task {
        const val SELECT_VAULTED_CHECKOUT = "select_vaulted_checkout_bt"
        const val SELECT_AGREE_AND_CONTINUE = "select_agree_and_continue"
        const val SELECT_CANCEL_AND_RETURN_TO_MERCHANT_LINK = "select_cancel_and_return_to_merchant_link"
    }

    object Flow {
        const val MODXO_VAULTED_NOT_RECURRING = "modxo_vaulted_not_recurring"
    }

    object PATH {
        const val PAY = "merchant_app/pay"
    }

    object VIEW {
        const val RETURN_TO_MERCHANT = "return_to_merchant"
        const val PAY_WITH_MODULE = "pay_with_module"
    }

    companion object {
        const val PLATFORM = "android_app"
    }
}