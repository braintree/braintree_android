package com.braintreepayments.demo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.braintreepayments.api.uicomponents.cardfields.BaseTextInputView

class CardFieldsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_card_fields, container, false)

        val cardInput = view.findViewById<BaseTextInputView>(R.id.card_number_input)
        cardInput.setHint("Card number")
        cardInput.setError("label")
        view.findViewById<BaseTextInputView>(R.id.cvv_input).setHint("CVV")

        val expirationInput = view.findViewById<BaseTextInputView>(R.id.expiration_input)
        expirationInput.setHint("Expiration")

        return view
    }
}
