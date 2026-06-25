package com.braintreepayments.api.uicomponents.cardfields

import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.DrawableRes
import com.braintreepayments.api.uicomponents.R

internal class CardNumberTextInputView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseTextInputView(context, attrs, defStyleAttr) {

    internal fun interface CardBrandChangeListener {
        fun onCardBrandChanged(brand: CardBrand)
    }

    internal var cardBrandChangeListener: CardBrandChangeListener? = null

    private val formatter = CardNumberFormatter { brand ->
        setCardIcon(brand.iconRes, context.getString(brand.iconContentDescriptionRes))
        cardBrandChangeListener?.onCardBrandChanged(brand)
    }

    internal val currentBrand: CardBrand
        get() = formatter.currentBrand

    init {
        setInputType(InputType.TYPE_CLASS_NUMBER)
        setHint(context.getString(R.string.card_number_hint))
        setCardBrandIcon(
            CardBrand.UNKNOWN.iconRes,
            context.getString(CardBrand.UNKNOWN.iconContentDescriptionRes)
        )
        editText.addTextChangedListener(formatter)
    }

    internal fun setCardIcon(@DrawableRes iconRes: Int, contentDescription: String) {
        setCardBrandIcon(iconRes, contentDescription)
        editText.sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED)
    }

    internal fun getRawCardNumber(): String {
        return getText()?.toString()?.filter { it.isDigit() } ?: ""
    }
}
