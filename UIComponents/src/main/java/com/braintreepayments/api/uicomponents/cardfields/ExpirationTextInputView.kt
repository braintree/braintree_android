package com.braintreepayments.api.uicomponents.cardfields

import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import androidx.annotation.RestrictTo
import com.braintreepayments.api.uicomponents.R

// TODO change class to internal before releasing
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ExpirationTextInputView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseTextInputView(context, attrs, defStyleAttr) {

    private val formatter = ExpirationDateFormatter()

    init {
        setInputType(InputType.TYPE_CLASS_NUMBER)
        setHint(context.getString(R.string.expiration_hint))
        editText.addTextChangedListener(formatter)
    }

    internal fun getRawExpiration(): String = getText()?.toString()?.filter { it.isDigit() } ?: ""
}
