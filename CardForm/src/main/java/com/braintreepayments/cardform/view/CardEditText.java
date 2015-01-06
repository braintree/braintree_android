package com.braintreepayments.cardform.view;

import android.content.Context;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputFilter.LengthFilter;
import android.text.InputType;
import android.text.Spanned;
import android.util.AttributeSet;

import com.braintreepayments.cardform.R;
import com.braintreepayments.cardform.utils.CardType;

/**
 * An {@link android.widget.EditText} that displays Card icons based on the number entered.
 */
public class CardEditText extends FloatingLabelEditText {

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
        setCardIcon(R.drawable.bt_card_highlighted);
    }

    /**
     * @return The {@link com.braintreepayments.cardform.utils.CardType} currently entered in
     * the {@link android.widget.EditText}
     */
    public CardType getCardType() {
        return mCardType;
    }

    /**
     * Receive a callback when the {@link com.braintreepayments.cardform.utils.CardType} changes
     * @param listener to be called when the {@link com.braintreepayments.cardform.utils.CardType}
     *  changes
     */
    public void setOnCardTypeChangedListener(OnCardTypeChangedListener listener) {
        mOnCardTypeChangedListener = listener;
    }

    @Override
    public void afterTextChanged(Editable editable) {
        Object[] paddingSpans = editable.getSpans(0, editable.length(), SpaceSpan.class);
        for (Object span : paddingSpans) {
            editable.removeSpan(span);
        }

        updateCardType();
        setCardIcon(mCardType.getFrontResource());

        if (!mRightToLeftLanguage) {
            addSpans(editable, mCardType.getSpaceIndices());
        }

        super.afterTextChanged(editable);

        if (mCardType.getMaxCardLength() == getSelectionStart()) {
            validate();

            if (isValid()) {
                focusNext();
            }
        }
    }

    @Override
    public boolean isValid() {
        return mCardType.validate(getText().toString());
    }

    private void updateCardType() {
        CardType type = CardType.forCardNumber(getText().toString());
        if (mCardType != type) {
            mCardType = type;

            InputFilter[] filters = {new LengthFilter(mCardType.getMaxCardLength())};
            setFilters(filters);
            invalidate();

            if (mOnCardTypeChangedListener != null) {
                mOnCardTypeChangedListener.onCardTypeChanged(mCardType);
            }
        }
    }

    private void addSpans(Editable editable, int[] spaceIndices) {
        final int length = editable.length();
        for (int index : spaceIndices) {
            if (index <= length) {
                editable.setSpan(new SpaceSpan(), index - 1, index,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }

    private void setCardIcon(int icon) {
        if(mRightToLeftLanguage) {
            setCompoundDrawablesWithIntrinsicBounds(icon, 0, 0, 0);
        } else {
            setCompoundDrawablesWithIntrinsicBounds(0, 0, icon, 0);
        }
    }

}
