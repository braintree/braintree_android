package com.braintreepayments.demo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.textfield.TextInputLayout

/**
 * Fragment for handling shopping insights.
 */
class ShoppingInsightsFragment : Fragment() {

    private lateinit var responseTextView: TextView
    private lateinit var actionButton: Button
    private lateinit var emailInput: TextInputLayout
    private lateinit var countryCodeInput: TextInputLayout
    private lateinit var nationalNumberInput: TextInputLayout
    private val viewModel: ShoppingInsightViewModel by lazy {
        ViewModelProvider(this)[ShoppingInsightViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_shopping_insights, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViews(view)
        setupActionButton()
    }

    private fun initializeViews(view: View) {
        responseTextView = view.findViewById(R.id.responseTextView)
        actionButton = view.findViewById(R.id.actionButton)
        emailInput = view.findViewById(R.id.emailInput)
        countryCodeInput = view.findViewById(R.id.countryCodeInput)
        nationalNumberInput = view.findViewById(R.id.nationalNumberInput)
    }

    private fun setupActionButton() {
        actionButton.setOnClickListener {
            val email = emailInput.editText?.text.toString()
            val countryCode = countryCodeInput.editText?.text.toString()
            val nationalNumber = nationalNumberInput.editText?.text.toString()
            viewModel.getRecommendedPaymentMethods(email, countryCode, nationalNumber)
                .observe(viewLifecycleOwner) {
                responseTextView.text = it.toString()
            }
        }
    }
}
