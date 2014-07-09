package com.braintreepayments.api.dropin.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.EditText;

import com.braintreepayments.api.dropin.R;

/**
 * Parent {@link android.widget.EditText} for storing and displaying error states.
 */
public class BraintreeEditText extends EditText {

    private boolean mError;

    public BraintreeEditText(Context context) {
        super(context);
        mError = false;
    }

    public BraintreeEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        mError = false;
    }

    public BraintreeEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mError = false;
    }

    @Override
    public void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter);
        clearError();
    }

    public boolean isError() {
        return mError;
    }

    public void setError() {
        mError = true;
        setBackground(R.drawable.bt_edittext_error_selector);
    }

    public void clearError() {
        mError = false;
        setBackground(R.drawable.bt_edittext_selector);
    }

    private void setBackground(int backgroundResourceId) {
        int paddingBottom = getPaddingBottom();
        int paddingLeft = getPaddingLeft();
        int paddingRight = getPaddingRight();
        int paddingTop = getPaddingTop();

        setBackgroundResource(backgroundResourceId);
        setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
    }
}
