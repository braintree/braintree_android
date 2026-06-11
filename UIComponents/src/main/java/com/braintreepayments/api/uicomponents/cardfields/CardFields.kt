package com.braintreepayments.api.uicomponents.cardfields

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.braintreepayments.api.card.Card
import com.braintreepayments.api.card.CardClient
import com.braintreepayments.api.card.CardResult
import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.uicomponents.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

@Suppress("TooManyFunctions")
class CardFields internal constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    private val viewModel: CardFieldsViewModel = CardFieldsViewModel(),
    private var cardClient: CardClient? = null
) : FrameLayout(context, attrs, defStyleAttr) {

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
    private var request: Card? = null
    private var resultCallback: CardFieldsResultCallback? = null

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

    /**
     * Initializes the card tokenization flow. Must be called before [submit].
     * @param authorization a tokenization key or client token.
     */
    fun initialize(authorization: String) {
        cardClient = CardClient(context, authorization)
    }

    /**
     * Optionally accepts a [Card] with additional data to be included in the tokenization request, such as
     * cardholder name or billing address. The card number, expiration, and CVV provided by the user will
     * override any values set on the [Card] object.
     */
    fun setPaymentRequest(card: Card?) {
        request = card
    }

    /**
     * Register a callback to receive the [CardFieldsResult] from [submit]
     */
    fun setCardFieldsResultCallback(callback: CardFieldsResultCallback?) {
        resultCallback = callback
    }

    /**
     * Tokenizes the card details entered by the user, along with any additional data provided in
     * [setPaymentRequest]. The result is returned via the [CardFieldsResultCallback] registered in
     * [setCardFieldsResultCallback].
     *
     * If called before [initialize], delivers a [CardFieldsResult.Failure].
     */
    fun submit() {
        val client = cardClient
        if (client == null) {
            resultCallback?.onCardFieldsResult(
                CardFieldsResult.Failure(
                    BraintreeException(
                        "CardFields must be initialized by calling initialize() before submit()")
                )
            )
            return
        }
        client.tokenize(buildCard()) { cardResult ->
            val result = when (cardResult) {
                is CardResult.Success -> {
                    CardFieldsResult.Success(cardResult.nonce)
                }
                is CardResult.Failure -> {
                    CardFieldsResult.Failure(cardResult.error)
                }
            }
            resultCallback?.onCardFieldsResult(result)
        }
    }

    /**
     * Builds a [Card] from the user input merged with any additional data from [setPaymentRequest].
     * [copy] returns a new instance with the typed fields updated, so the merchant's [request] fields
     * remain unchanged.
     */
    private fun buildCard(): Card {
        val rawExpiration = expirationView.getRawExpiration()
        return (request ?: Card()).copy(
            number = cardNumberView.getRawCardNumber(),
            expirationMonth = rawExpiration.take(2),
            expirationYear = rawExpiration.drop(2),
            cvv = cvvView.getRawCvv()
        )
    }

    /**
     * Wires each of the ViewModel's `StateFlow`s to the corresponding child view. Every flow is
     * collected in its own coroutine. The scope is cancelled in [onDetachedFromWindow].
     */
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
            // drop(1) discards State Flow's replayed first value
            viewModel.isFormValid.drop(1).collect { isValid ->
                validationChangedListener?.onValidationChanged(isValid)
            }
        }
    }

    /**
     * Maps a [ValidationResult] to the error string the child view should display, or `null` to
     * clear it.
     */
    private fun ValidationResult.requiredOrInvalidError(): String? =
        if (this is ValidationResult.Invalid) context.getString(errorMessageRes) else null

    private fun BaseTextInputView.onTextChanged(block: () -> Unit) {
        addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) = block()
        })
    }

    /**
     * Auto-advances focus to the next field once [from] becomes valid.
     * Focus only advances if [from] has focus, so the user can still tap back into a previous field
     * and edit it without being forced forward again.
     */
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
