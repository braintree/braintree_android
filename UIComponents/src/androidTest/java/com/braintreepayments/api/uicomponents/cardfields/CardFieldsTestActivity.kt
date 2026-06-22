package com.braintreepayments.api.uicomponents.cardfields

import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

class CardFieldsTestActivity : AppCompatActivity() {

    lateinit var cardFields: CardFields
    lateinit var payButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cardFields = CardFields(this)
        payButton = Button(this).apply {
            text = "Pay"
            isEnabled = false
        }

        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            addView(cardFields, ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ))
            addView(payButton, ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ))
        })

        cardFields.setOnValidationChangedListener { isValid ->
            payButton.isEnabled = isValid
        }
    }
}
