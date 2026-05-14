package com.braintreepayments.demo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.braintreepayments.api.uicomponents.cardfields.CardNumberTextInputView
import com.braintreepayments.api.uicomponents.cardfields.ExpirationTextInputView

class CardFieldsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_card_fields, container, false)

        view.findViewById<CardNumberTextInputView>(R.id.card_number_input)
        view.findViewById<ExpirationTextInputView>(R.id.expiration_input)

        return view
    }
}
