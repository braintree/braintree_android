package com.braintreepayments.demo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.braintreepayments.api.uicomponents.cardfields.CardFields

class CardFieldsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_card_fields, container, false)

        val cardFields = view.findViewById<CardFields>(R.id.card_fields)
        val payButton = view.findViewById<Button>(R.id.pay_button)

        //Card fields will tell the merchant if the card is valid, and they can enable the button
        cardFields.setOnValidationChangedListener { isFormValid ->
            payButton.isEnabled = isFormValid
        }

        // placeholder for tokenization
        payButton.setOnClickListener {
            Toast.makeText(requireContext(), R.string.card_fields_valid_toast, Toast.LENGTH_SHORT)
                .show()
        }

        return view
    }
}