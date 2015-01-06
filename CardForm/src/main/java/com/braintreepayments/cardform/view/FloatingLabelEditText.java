package com.braintreepayments.cardform.view;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.text.Editable;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.inputmethod.EditorInfo;

import com.braintreepayments.cardform.R;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.HONEYCOMB;
import static android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH;
import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR1;

/**
 * Parent {@link android.widget.EditText} for displaying floating hints when text has been entered.
 */
public abstract class FloatingLabelEditText extends ErrorEditText implements
        OnFocusChangeListener, TextWatcher {

    private static final int ANIMATION_DURATION_MILLIS = 300;

    public interface OnTextChangedListener {
        public void onTextChanged(Editable editable);
    }

    private OnFocusChangeListener mOnFocusChangeListener;
    private OnTextChangedListener mOnTextChangedListener;

    private TextPaint mHintPaint = new TextPaint();
    private ValueAnimator mHintAnimator;
    private ValueAnimator mFocusColorAnimator;
    private ValueAnimator mInactiveColorAnimator;
    private ValueAnimator mAlphaAnimator;

    private float mAnimatedHintHeight = -1;
    private int mAnimatedHintColor;
    private int mHintAlpha;

    private float mHorizontalTextOffset;
    private int mPreviousTextLength;

    protected boolean mRightToLeftLanguage;

    public FloatingLabelEditText(Context context) {
        super(context);
        init();
    }

    public FloatingLabelEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FloatingLabelEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        mRightToLeftLanguage = isRightToLeftLanguage();
        addTextChangedListener(this);
        mPreviousTextLength = getText().length();
        if (SDK_INT >= HONEYCOMB) {
            setupAnimations();
        }
    }

    @TargetApi(HONEYCOMB)
    private void setupAnimations() {
        Resources res = getResources();
        mHorizontalTextOffset = res.getDimension(R.dimen.bt_floating_edit_text_horizontal_offset);

        final float textSize = getTextSize();
        mHintAnimator = ValueAnimator.ofFloat(textSize * 1.75f, textSize);
        mHintAnimator.addUpdateListener(new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mAnimatedHintHeight = (Float) animation.getAnimatedValue();
                invalidate();
            }
        });
        mHintAnimator.setDuration(ANIMATION_DURATION_MILLIS);

        int inactiveColor = res.getColor(R.color.bt_light_gray);
        int activeColor = res.getColor(R.color.bt_blue);
        AnimatorUpdateListener animatorUpdateListener = new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mAnimatedHintColor = (Integer) animation.getAnimatedValue();
                invalidate();
            }
        };

        mFocusColorAnimator = ValueAnimator.ofInt(inactiveColor, activeColor);
        mFocusColorAnimator.setEvaluator(new ArgbEvaluator());
        mFocusColorAnimator.addUpdateListener(animatorUpdateListener);
        mFocusColorAnimator.setDuration(ANIMATION_DURATION_MILLIS);

        mInactiveColorAnimator = ValueAnimator.ofInt(activeColor, inactiveColor);
        mInactiveColorAnimator.setEvaluator(new ArgbEvaluator());
        mInactiveColorAnimator.addUpdateListener(animatorUpdateListener);
        mInactiveColorAnimator.setDuration(ANIMATION_DURATION_MILLIS);

        mAlphaAnimator = ValueAnimator.ofInt(0, 255);
        mAlphaAnimator.addUpdateListener(new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mHintAlpha = (Integer) animation.getAnimatedValue();
                invalidate();
            }
        });

        setOnFocusChangeListener(this);
    }

    /**
     * Set a listener to receive a callback when focus on this {@link com.braintreepayments.cardform.view.FloatingLabelEditText}
     * changes
     * @param listener the listener to call
     */
    public void setFocusChangeListener(OnFocusChangeListener listener) {
        mOnFocusChangeListener = listener;
    }

    /**
     * Set a listener to receive a callback when text changes in this
     * {@link com.braintreepayments.cardform.view.FloatingLabelEditText}
     * @param listener the listener to call
     */
    public void setTextChangedListener(OnTextChangedListener listener) {
        mOnTextChangedListener = listener;
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus){
        handleTextColorOnFocus(hasFocus);
        setErrorOnFocusChange(hasFocus);

        if (mOnFocusChangeListener != null) {
            mOnFocusChangeListener.onFocusChange(v, hasFocus);
        }
    }

    protected void setErrorOnFocusChange(boolean hasFocus) {
        if(!hasFocus && !isValid()) {
            setError();
        }
    }

    @SuppressWarnings("ResourceType")
    public void focusNext() {
        if (getImeActionId() == EditorInfo.IME_ACTION_GO) {
            return;
        }

        View next = focusSearch(View.FOCUS_FORWARD);
        if (next != null) {
            next.requestFocus();
        }
    }

    public abstract boolean isValid();

    public void validate() {
        if (isValid()) {
            clearError();
        } else {
            setError();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (getText().length() > 0) {
            mHintPaint.setColor(mAnimatedHintColor);
            mHintPaint.setTextSize(getPaint().getTextSize() * 2 / 3);
            mHintPaint.setAlpha(mHintAlpha);

            String hint = getHint().toString();
            canvas.drawText(hint, mHorizontalTextOffset, mAnimatedHintHeight, mHintPaint);
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

    @Override
    public void afterTextChanged(Editable editable) {
        if (mOnTextChangedListener != null) {
            mOnTextChangedListener.onTextChanged(editable);
        }
    }

    @Override
    @TargetApi(ICE_CREAM_SANDWICH)
    public void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter);
        if (SDK_INT >= ICE_CREAM_SANDWICH) {
            if (mPreviousTextLength == 0 && text.length() > 0 && !mHintAnimator.isStarted()) {
                mHintAnimator.start();
                mFocusColorAnimator.start();
                mAlphaAnimator.start();
            }
        }
        mPreviousTextLength = text.length();
    }

    @TargetApi(HONEYCOMB)
    protected void handleTextColorOnFocus(boolean hasFocus) {
        if (SDK_INT >= HONEYCOMB) {
            if (hasFocus) {
                mFocusColorAnimator.start();
            } else {
                mInactiveColorAnimator.start();
            }
        }
    }

    @TargetApi(JELLY_BEAN_MR1)
    private boolean isRightToLeftLanguage() {
        if (SDK_INT >= JELLY_BEAN_MR1) {
            if(getResources().getConfiguration().getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
                return true;
            }
        }
        return false;
    }

}
