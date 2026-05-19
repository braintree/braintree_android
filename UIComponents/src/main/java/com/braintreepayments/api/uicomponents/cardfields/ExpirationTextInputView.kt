package com.braintreepayments.api.uicomponents.cardfields

import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import androidx.annotation.RestrictTo
import androidx.autofill.HintConstants
import androidx.core.view.ViewCompat
import androidx.core.widget.doAfterTextChanged
import com.braintreepayments.api.uicomponents.R

// TODO change class to internal before releasing
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ExpirationTextInputView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseTextInputView(context, attrs, defStyleAttr) {

    internal val formatter = ExpirationDateFormatter()

    init {
        formatter.onMonthRejected = {
            setError(context.getString(R.string.expiration_invalid_month_error))
        }
        setInputType(InputType.TYPE_CLASS_NUMBER)
        setHint(context.getString(R.string.expiration_hint))
        editText.contentDescription = context.getString(R.string.expiration_hint_accessibility)
        ViewCompat.setAutofillHints(editText, HintConstants.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DATE)
        editText.addTextChangedListener(formatter)
        editText.doAfterTextChanged { editable ->
            val digits = editable?.filter { it.isDigit() }?.toString() ?: ""
            if (digits.isEmpty() || (digits.length == 2 && ExpirationDateFormatter.isValidMonthNumber(digits))) {
                setError(null)
            }
        }
    }

    internal fun getRawExpiration(): String = getText()?.toString()?.filter { it.isDigit() } ?: ""
}
