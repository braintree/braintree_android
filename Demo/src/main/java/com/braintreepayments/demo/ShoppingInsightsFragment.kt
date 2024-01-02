package com.braintreepayments.demo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.switchmaterial.SwitchMaterial
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
    private lateinit var emailNullSwitch: SwitchMaterial
    private lateinit var phoneNullSwitch: SwitchMaterial

    private val viewModel: ShoppingInsightsViewModel by lazy {
        ViewModelProvider(this)[ShoppingInsightsViewModel::class.java]
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
        emailNullSwitch = view.findViewById(R.id.emailNullSwitch)
        phoneNullSwitch = view.findViewById(R.id.phoneNullSwitch)
    }

    private fun setupActionButton() {
        actionButton.setOnClickListener {
            val email = if (emailNullSwitch.isChecked) null else emailInput.editText?.text.toString()
            val countryCode = if (phoneNullSwitch.isChecked) null else countryCodeInput.editText?.text.toString()
            val nationalNumber = if (phoneNullSwitch.isChecked) null else nationalNumberInput.editText?.text.toString()
            viewModel.getRecommendedPaymentMethods(email, countryCode, nationalNumber)
                .observe(viewLifecycleOwner) {
                responseTextView.text = it.toString()
            }
        }
    }
}
