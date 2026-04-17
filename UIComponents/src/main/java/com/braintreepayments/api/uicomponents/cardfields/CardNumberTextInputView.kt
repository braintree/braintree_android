package com.braintreepayments.api.uicomponents.cardfields

import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.DrawableRes
import androidx.annotation.RestrictTo
import com.braintreepayments.api.uicomponents.R

// TODO change class to internal before releasing
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class CardNumberTextInputView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseTextInputView(context, attrs, defStyleAttr) {

    init {
        setInputType(InputType.TYPE_CLASS_NUMBER)
        setHint(context.getString(R.string.card_number_hint))
        setCardBrandIcon(R.drawable.card_fields_unknown_cc, context.getString(R.string.card_icon_unknown))
    }

    internal fun setCardIcon(@DrawableRes iconRes: Int, contentDescription: String) {
        setCardBrandIcon(iconRes, contentDescription)
        editText.sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED)
    }

    internal fun getRawCardNumber(): String {
        return getText()?.toString()?.filter { it.isDigit() } ?: ""
    }
}
