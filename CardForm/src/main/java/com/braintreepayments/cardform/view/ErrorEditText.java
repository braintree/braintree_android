package com.braintreepayments.cardform.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.EditText;

import com.braintreepayments.cardform.R;

/**
 * Parent {@link android.widget.EditText} for storing and displaying error states.
 */
public class ErrorEditText extends EditText {

    private boolean mError;

    public ErrorEditText(Context context) {
        super(context);
        mError = false;
    }

    public ErrorEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        mError = false;
    }

    public ErrorEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mError = false;
    }

    @Override
    public void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter);
        clearError();
    }

    /**
     * @return the current error state of the {@link android.widget.EditText}
     */
    public boolean isError() {
        return mError;
    }

    /**
     * Set a visual indication that the {@link android.widget.EditText} contains an error
     * and track the error state.
     */
    public void setError() {
        mError = true;
        setBackground(R.drawable.bt_field_error_selector);
    }

    /**
     * Remove the visual indication of an error and track the error state.
     */
    public void clearError() {
        mError = false;
        setBackground(R.drawable.bt_field_selector);
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
