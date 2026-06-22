package com.braintreepayments.api.uicomponents.cardfields

import android.content.Context
import android.text.InputFilter
import android.text.InputType
import android.util.AttributeSet
import androidx.annotation.RestrictTo
import com.braintreepayments.api.uicomponents.R

// TODO change class to internal before releasing
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class CvvTextInputView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseTextInputView(context, attrs, defStyleAttr) {

    private val formatter = CvvFormatter()

    init {
        setInputType(InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD)
        setHint(context.getString(R.string.cvv_hint))
        setCvvHintIcon(
            R.drawable.cvv_hint,
            context.getString(R.string.cvv_hint_icon_description)
        )
        setCvvHintClickListener { anchor -> CvvHintOverlay(context).show(anchor) }
        editText.addTextChangedListener(formatter)
    }

    fun linkTo(cardNumberView: CardNumberTextInputView) {
        cardNumberView.cardBrandChangeListener = CardNumberTextInputView.CardBrandChangeListener { brand ->
            updateCardBrand(brand)
        }
        updateCardBrand(cardNumberView.currentBrand)
    }

    internal fun updateCardBrand(brand: CardBrand) {
        formatter.updateCvvLength(brand.cvvLength)
        editText.filters = (editText.filters.filterNot { it is InputFilter.LengthFilter } +
            InputFilter.LengthFilter(brand.cvvLength)).toTypedArray()
    }

    internal fun getRawCvv(): String {
        return getText()?.toString()?.filter { it.isDigit() } ?: ""
    }
}
