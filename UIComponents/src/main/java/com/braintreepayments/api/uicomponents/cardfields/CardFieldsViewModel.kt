 package com.braintreepayments.api.uicomponents.cardfields

 import androidx.annotation.StringRes
 import androidx.lifecycle.ViewModel
 import com.braintreepayments.api.uicomponents.R
 import kotlinx.coroutines.flow.MutableStateFlow
 import kotlinx.coroutines.flow.StateFlow
 import java.util.Calendar

 @Suppress("TooManyFunctions")
 internal class CardFieldsViewModel(
    private val cardNumberValidationUseCase: CardNumberValidationUseCase = CardNumberValidationUseCase(),
    private val expiryValidationUseCase: ExpiryValidationUseCase = ExpiryValidationUseCase(),
    private val cvvValidationUseCase: CvvValidationUseCase = CvvValidationUseCase(),
    private val currentDateProvider: () -> Pair<Int, Int> = {
        val cal = Calendar.getInstance()
        Pair(cal.get(Calendar.MONTH) + 1, cal.get(Calendar.YEAR))
    }
 ) : ViewModel() {

    // Backing property to avoid state updates from other classes
    private val _cardNumberError = MutableStateFlow<Int?>(null)
    // The UI collects from this StateFlow to get its state updates
    val cardNumberError: StateFlow<Int?> = _cardNumberError
    private val _expiryError = MutableStateFlow<Int?>(null)
    val expiryError: StateFlow<Int?> = _expiryError
    private val _cvvError = MutableStateFlow<Int?>(null)
    val cvvError: StateFlow<Int?> = _cvvError
    private val _detectedCardBrand = MutableStateFlow(CardBrand.UNKNOWN)
    val detectedCardBrand: StateFlow<CardBrand> = _detectedCardBrand
    private val _isValid = MutableStateFlow(false)
    val isValid: StateFlow<Boolean> = _isValid
    private var cardNumberTouched = false
    private var expiryTouched = false
    private var cvvTouched = false

    private var cardNumberIsValid = false
    private var expiryIsValid = false
    private var cvvIsValid = false

    private var currentCardNumber = ""
    private var currentCvv = ""

    fun onCardNumberChanged(cardNumber: String) {
        currentCardNumber = cardNumber
        val result = cardNumberValidationUseCase(cardNumber)
        val previousBrand = _detectedCardBrand.value
        _detectedCardBrand.value = result.detectedBrand
        cardNumberIsValid = result.isValid

        updateFieldError(
            isTouched = cardNumberTouched,
            isValid = result.isValid,
            isEmpty = cardNumber.isEmpty(),
            errorFlow = _cardNumberError,
            errorRes = R.string.card_number_error
        )

        if (previousBrand != result.detectedBrand) {
            revalidateCvv()
        }

        updateIsValid()
    }

    fun onExpiryChanged(rawExpiration: String) {
        val (currentMonth, currentYear) = currentDateProvider()
        expiryIsValid = expiryValidationUseCase(rawExpiration, currentMonth, currentYear)

        updateFieldError(
            isTouched = expiryTouched,
            isValid = expiryIsValid,
            isEmpty = rawExpiration.isEmpty(),
            errorFlow = _expiryError,
            errorRes = R.string.expiration_error
        )

        updateIsValid()
    }

    fun onCvvChanged(cvv: String) {
        currentCvv = cvv
        cvvIsValid = cvvValidationUseCase(cvv, _detectedCardBrand.value)

        updateFieldError(
            isTouched = cvvTouched,
            isValid = cvvIsValid,
            isEmpty = cvv.isEmpty(),
            errorFlow = _cvvError,
            errorRes = R.string.cvv_error
        )

        updateIsValid()
    }

    fun onFieldFocusChanged(field: CardField, hasFocus: Boolean) {
        if (hasFocus) return

        when (field) {
            CardField.CARD_NUMBER -> {
                cardNumberTouched = true
                showErrorIfInvalid(
                    cardNumberIsValid, currentCardNumber.isEmpty(),
                    _cardNumberError, R.string.card_number_error
                )
            }
            CardField.EXPIRY -> {
                expiryTouched = true
                showErrorIfInvalid(expiryIsValid, false, _expiryError, R.string.expiration_error)
            }
            CardField.CVV -> {
                cvvTouched = true
                showErrorIfInvalid(cvvIsValid, currentCvv.isEmpty(), _cvvError, R.string.cvv_error)
            }
        }
    }

    private fun revalidateCvv() {
        cvvIsValid = cvvValidationUseCase(currentCvv, _detectedCardBrand.value)

        updateFieldError(
            isTouched = cvvTouched,
            isValid = cvvIsValid,
            isEmpty = currentCvv.isEmpty(),
            errorFlow = _cvvError,
            errorRes = R.string.cvv_error
        )

        updateIsValid()
    }

    private fun updateFieldError(
        isTouched: Boolean,
        isValid: Boolean,
        isEmpty: Boolean,
        errorFlow: MutableStateFlow<Int?>,
        @StringRes errorRes: Int
    ) {
        if (!isTouched) return

        errorFlow.value = if (isValid || isEmpty) null else errorRes
    }

    private fun showErrorIfInvalid(
        isValid: Boolean,
        isEmpty: Boolean,
        errorFlow: MutableStateFlow<Int?>,
        @StringRes errorRes: Int
    ) {
        errorFlow.value = if (isValid || isEmpty) null else errorRes
    }

    private fun updateIsValid() {
        _isValid.value = cardNumberIsValid && expiryIsValid && cvvIsValid
    }
 }
