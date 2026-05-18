package com.braintreepayments.api.uicomponents.cardfields

import android.content.Context
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
        setTrailingIcon(
            R.drawable.cvv_hint_image,
            context.getString(R.string.cvv_hint_icon_description)
        )
        setTrailingIconClickListener { CvvHintOverlay(context).show() }
        editText.addTextChangedListener(formatter)
    }

    internal fun updateCardBrand(brand: CardBrand) {
        formatter.updateCvvLength(brand.cvvLength)
        val currentCvv = getRawCvv()
        if (currentCvv.length > brand.cvvLength) {
            setText(currentCvv.take(brand.cvvLength))
        }
    }

    internal fun getRawCvv(): String {
        return getText()?.toString()?.filter { it.isDigit() } ?: ""
    }
}
