package com.braintreepayments.api.uicomponents.cardfields

import android.content.Context
import android.os.Build
import android.text.InputType
import android.util.AttributeSet
import android.view.View
import androidx.annotation.RestrictTo
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            editText.setAutofillHints(View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DATE)
        }
        editText.addTextChangedListener(formatter)
        editText.doAfterTextChanged { editable ->
            val digits = editable?.filter { it.isDigit() }?.toString() ?: ""
            if (digits.isEmpty() || (digits.length == 2 && ExpirationDateFormatter.isValidMonth(digits))) {
                setError(null)
            }
        }
    }

    internal fun getRawExpiration(): String = getText()?.toString()?.filter { it.isDigit() } ?: ""
}
