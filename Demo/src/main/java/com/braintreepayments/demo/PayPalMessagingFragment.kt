package com.braintreepayments.demo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.paypalmessaging.PayPalMessagingColor
import com.braintreepayments.api.paypalmessaging.PayPalMessagingListener
import com.braintreepayments.api.paypalmessaging.PayPalMessagingLogoType
import com.braintreepayments.api.paypalmessaging.PayPalMessagingOfferType
import com.braintreepayments.api.paypalmessaging.PayPalMessagingRequest
import com.braintreepayments.api.paypalmessaging.PayPalMessagingTextAlignment
import com.braintreepayments.api.paypalmessaging.PayPalMessagingView

@OptIn(ExperimentalBetaApi::class)
class PayPalMessagingFragment : BaseFragment(), PayPalMessagingListener {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_paypal_messaging, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val payPalMessagingRequest = PayPalMessagingRequest(
            amount = 2.0,
            pageType = null,
            offerType = PayPalMessagingOfferType.PAY_LATER_LONG_TERM,
            buyerCountry = "US",
            logoType = PayPalMessagingLogoType.PRIMARY,
            textAlignment = PayPalMessagingTextAlignment.CENTER,
            color = PayPalMessagingColor.BLACK
        )

        val payPalMessagingView = PayPalMessagingView(requireContext(), authStringArg)
        payPalMessagingView.setListener(this)
        payPalMessagingView.start(payPalMessagingRequest)
        payPalMessagingView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        val messagingView: LinearLayout = view.findViewById(R.id.content)
        messagingView.addView(payPalMessagingView)
    }

    override fun onPayPalMessagingClick() {
        println("User clicked on the PayPalMessagingView")
    }

    override fun onPayPalMessagingApply() {
        println("User is attempting to apply for PayPal Credit")
    }

    override fun onPayPalMessagingLoading() {
        println("Loading PayPalMessagingView")
    }

    override fun onPayPalMessagingSuccess() {
        println("PayPalMessagingView displayed to user")
    }

    override fun onPayPalMessagingFailure(error: Exception) {
        println("PayPalMessagingView returned the error:" + error.message)
    }
}
