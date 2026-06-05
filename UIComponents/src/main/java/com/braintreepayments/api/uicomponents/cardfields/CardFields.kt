package com.braintreepayments.api.uicomponents.cardfields

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.braintreepayments.api.uicomponents.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class CardFields internal constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    private val viewModel: CardFieldsViewModel = CardFieldsViewModel()
): FrameLayout(context, attrs, defStyleAttr) {

    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
    ) : this(context, attrs, defStyleAttr, CardFieldsViewModel())

    private val cardNumberView: CardNumberTextInputView
    private val expirationView: ExpirationTextInputView
    private val cvvView: CvvTextInputView
    private var observerScope: CoroutineScope? = null
    private var validationChangedListener: OnValidationChangedListener? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.card_fields_view, this, true)

        cardNumberView = findViewById(R.id.card_fields_card_number_input)
        expirationView = findViewById(R.id.card_fields_expiration_input)
        cvvView = findViewById(R.id.card_fields_cvv_input)

        cardNumberView.focusChangeCallback = { hasFocus ->
            viewModel.onFieldFocusChanged(CardField.CARD_NUMBER, hasFocus)
        }
        expirationView.focusChangeCallback = { hasFocus ->
            viewModel.onFieldFocusChanged(CardField.EXPIRY, hasFocus)
        }
        cvvView.focusChangeCallback = { hasFocus ->
            viewModel.onFieldFocusChanged(CardField.CVV, hasFocus)
        }
        cardNumberView.onTextChanged { viewModel.onCardNumberChanged(cardNumberView.getRawCardNumber()) }
        expirationView.onTextChanged { viewModel.onExpiryChanged(expirationView.getRawExpiration()) }
        cvvView.onTextChanged { viewModel.onCvvChanged(cvvView.getRawCvv()) }
    }

    fun interface OnValidationChangedListener {
        fun onValidationChanged(isFormValid: Boolean)
    }

    fun setOnValidationChangedListener(listener: OnValidationChangedListener?) {
        validationChangedListener = listener
        listener?.onValidationChanged(viewModel.isFormValid.value)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val scope = MainScope()
        // save the scope so that onDetachedFromWindow can cancel it
        observerScope = scope
        observeViewModel(scope)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        observerScope?.cancel()
        observerScope = null
    }

    private fun observeViewModel(scope: CoroutineScope) {
        scope.launch {
            viewModel.cardNumberValidation.collect { result ->
                cardNumberView.setError(result.requiredOrInvalidError())
                advanceFocusIfFieldIsValid(result, cardNumberView, expirationView)
            }
        }
        scope.launch {
            viewModel.expirationValidation.collect { result ->
                expirationView.setError(result.requiredOrInvalidError())
                advanceFocusIfFieldIsValid(result, expirationView, cvvView)
            }
        }
        scope.launch {
            viewModel.cvvValidation.collect { result ->
                cvvView.setError(result.requiredOrInvalidError())
            }
        }
        scope.launch {
            viewModel.detectedCardBrand.collect { brand ->
                cvvView.updateCardBrand(brand)
            }
        }
        scope.launch {
            viewModel.isFormValid.collect { isValid ->
                validationChangedListener?.onValidationChanged(isValid)
            }
        }
    }

    private fun ValidationResult.requiredOrInvalidError(): String? =
        if (this is ValidationResult.Invalid) context.getString(errorMessageRes) else null

    private fun BaseTextInputView.onTextChanged(block: () -> Unit) {
        addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) = block()
        })
    }

    private fun advanceFocusIfFieldIsValid(
        result: ValidationResult,
        from: BaseTextInputView,
        to: BaseTextInputView
    ) {
        if (result is ValidationResult.Valid && from.hasFocus()) {
            to.requestFocus()
        }
    }
}
