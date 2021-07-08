package com.braintreepayments.demo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.braintreepayments.api.*
import kotlinx.coroutines.launch

/**
 * A simple [Fragment] subclass.
 * create an instance of this fragment.
 */
class CardFragmentKtx : BaseFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_card_ktx, container, false)

        getBraintreeClient { braintreeClient ->
            tokenizeCard(braintreeClient)
        }
        return view
    }

    private fun tokenizeCard(braintreeClient: BraintreeClient) {
        val card = Card()
        card.number = "4111 1111 1111 1111"
        card.expirationMonth = "02"
        card.expirationYear = "2022"
        card.cvv = "123"

        val cardClient = CardClient(braintreeClient)
        lifecycleScope.launch {
            val cardNonce: CardNonce = cardClient.tokenize(card)
            print(cardNonce.string)
        }
    }
}