package com.braintreepayments.api.uicomponents.cardfields

import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import androidx.autofill.HintConstants
import androidx.core.view.ViewCompat
import com.braintreepayments.api.uicomponents.R

internal class ExpirationTextInputView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseTextInputView(context, attrs, defStyleAttr) {

    internal val formatter = ExpirationDateFormatter()

    init {
        setInputType(InputType.TYPE_CLASS_NUMBER)
        setHint(context.getString(R.string.expiration_hint))
        editText.contentDescription = context.getString(R.string.expiration_hint_accessibility)
        ViewCompat.setAutofillHints(editText, HintConstants.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DATE)
        editText.addTextChangedListener(formatter)
    }

    internal fun getRawExpiration(): String = getText()?.toString()?.filter { it.isDigit() } ?: ""
}
