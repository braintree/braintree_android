package com.braintreepayments.api.dropin.view;

import android.content.Context;
import android.text.Editable;
import android.text.InputType;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.AttributeSet;

import com.braintreepayments.api.dropin.R;
import com.braintreepayments.api.dropin.utils.CardType;

/**
 * An {@link android.widget.EditText} that displays Card icons based on the number entered.
 */
public class CardEditText extends FloatingLabelEditText implements TextWatcher {

    public static interface OnCardTypeChangedListener {
        void onCardTypeChanged(CardType cardType);
    }

    private CardType mCardType = CardType.UNKNOWN;
    private OnCardTypeChangedListener mOnCardTypeChangedListener;

    public CardEditText(Context context) {
        super(context);
        init();
    }

    public CardEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CardEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        setInputType(InputType.TYPE_CLASS_NUMBER);
        addTextChangedListener(this);
        setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_card_highlighted, 0);
    }

    public CardType getCardType() {
        return mCardType;
    }

    public void setOnCardTypeChangedListener(OnCardTypeChangedListener listener) {
        mOnCardTypeChangedListener = listener;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

    @Override
    public void afterTextChanged(Editable editable) {
        Object[] paddingSpans = editable.getSpans(0, editable.length(), PaddingSpan.class);
        for (Object span : paddingSpans) {
            editable.removeSpan(span);
        }

        updateCardType();

        addSpans(editable, mCardType.getSpaceIndices());
        setCompoundDrawablesWithIntrinsicBounds(0, 0, mCardType.getFrontResource(), 0);
    }

    @Override
    public boolean isValid() {
        return mCardType.validate(getText().toString());
    }

    private void updateCardType() {
        CardType type = CardType.forCardNumber(getText().toString());
        if (mCardType != type) {
            mCardType = type;
            if (mOnCardTypeChangedListener != null) {
                mOnCardTypeChangedListener.onCardTypeChanged(mCardType);
            }
        }
    }

    private void addSpans(Editable editable, int[] spaceIndices) {
        final int length = editable.length();
        for (int index : spaceIndices) {
            if (index <= length) {
                editable.setSpan(new PaddingSpan(0, 1), index - 1, index,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }

}
