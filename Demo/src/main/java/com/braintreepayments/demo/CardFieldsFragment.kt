package com.braintreepayments.demo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.navigation.fragment.NavHostFragment
import com.braintreepayments.api.card.Card
import com.braintreepayments.api.uicomponents.cardfields.CardFields
import com.braintreepayments.api.uicomponents.cardfields.CardFieldsResult

class CardFieldsFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_card_fields, container, false)

        val cardFields = view.findViewById<CardFields>(R.id.card_fields)
        val payButton = view.findViewById<Button>(R.id.pay_button)
        cardFields.initialize(authStringArg)
        // optional customer data
        cardFields.setPaymentRequest(
            Card(
                cardholderName = "John Doe",
                postalCode = "12345"
            )
        )

        cardFields.setCardFieldsResultCallback { result ->
            when (result) {
                is CardFieldsResult.Success -> {
                    // handle nonce
                    super.onPaymentMethodNonceCreated(result.nonce)
                    val action = CardFieldsFragmentDirections
                        .actionCardFieldsFragmentToDisplayNonceFragment(result.nonce)
                    NavHostFragment.findNavController(this).navigate(action)
                }
                is CardFieldsResult.Failure -> {
                    // display error
                    handleError(result.error)
                }
            }
        }

        // Card fields will tell the merchant if the card is valid, and they can enable the button
        cardFields.setOnValidationChangedListener { isFormValid ->
            payButton.isEnabled = isFormValid
        }

        // submit card info for processing
        payButton.setOnClickListener {
            cardFields.submit()
        }

        return view
    }
}
