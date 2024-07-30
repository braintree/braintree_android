package com.braintreepayments.api.paypal

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.json.JSONException
import org.json.JSONObject

/**
 * Represents the PayPal credit financing response.
 *
 * @property isCardAmountImmutable Indicates whether the card amount is editable after payer's
 * acceptance on PayPal side.
 * @property monthlyPayment Estimated amount per month that the customer will need to pay including
 * fees and interest.
 * @property payerAcceptance Status of whether the customer ultimately was approved for and chose to
 * make the payment using the approved installment credit.
 * @property totalCost Estimated total payment amount including interest and fees the user will pay
 * during the lifetime of the loan.
 * @property totalInterest Estimated interest or fees amount the payer will have to pay during the
 * lifetime of the loan.
 * @property term Length of financing terms in months.
 */
@Parcelize
data class PayPalCreditFinancing internal constructor(
    val isCardAmountImmutable: Boolean = false,
    val monthlyPayment: PayPalCreditFinancingAmount? = null,
    val payerAcceptance: Boolean = false,
    val totalCost: PayPalCreditFinancingAmount? = null,
    val totalInterest: PayPalCreditFinancingAmount? = null,
    val term: Int = 0,
) : Parcelable {

    companion object {
        private const val CARD_AMOUNT_IMMUTABLE_KEY = "cardAmountImmutable"
        private const val MONTHLY_PAYMENT_KEY = "monthlyPayment"
        private const val PAYER_ACCEPTANCE_KEY = "payerAcceptance"
        private const val TERM_KEY = "term"
        private const val TOTAL_COST_KEY = "totalCost"
        private const val TOTAL_INTEREST_KEY = "totalInterest"

        @JvmStatic
        @Throws(JSONException::class)
        fun fromJson(creditFinancing: JSONObject?): PayPalCreditFinancing {
            return if (creditFinancing == null) {
                PayPalCreditFinancing()
            } else {
                PayPalCreditFinancing(
                    isCardAmountImmutable = creditFinancing.optBoolean(
                        CARD_AMOUNT_IMMUTABLE_KEY,
                        false
                    ),
                    monthlyPayment = PayPalCreditFinancingAmount.fromJson(
                        creditFinancing.optJSONObject(MONTHLY_PAYMENT_KEY)
                    ),
                    payerAcceptance = creditFinancing.optBoolean(PAYER_ACCEPTANCE_KEY, false),
                    term = creditFinancing.optInt(TERM_KEY, 0),
                    totalCost = PayPalCreditFinancingAmount.fromJson(
                        creditFinancing.optJSONObject(TOTAL_COST_KEY)
                    ),
                    totalInterest = PayPalCreditFinancingAmount.fromJson(
                        creditFinancing.optJSONObject(TOTAL_INTEREST_KEY)
                    )
                )
            }
        }
    }
}
