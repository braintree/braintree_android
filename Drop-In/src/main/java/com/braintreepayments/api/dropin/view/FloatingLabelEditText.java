package com.braintreepayments.api.dropin.view;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.text.Editable;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.inputmethod.EditorInfo;

import com.braintreepayments.api.dropin.R;

import static android.os.Build.VERSION_CODES.HONEYCOMB;

/**
 * Parent {@link android.widget.EditText} for displaying floating hints when text has been entered.
 */
public abstract class FloatingLabelEditText extends BraintreeEditText implements
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

    @TargetApi(HONEYCOMB)
    private void init() {
        addTextChangedListener(this);
        mPreviousTextLength = getText().length();
        if (VERSION.SDK_INT >= HONEYCOMB) {
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
    }

    public void setFocusChangeListener(OnFocusChangeListener listener) {
        mOnFocusChangeListener = listener;
    }

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

    @TargetApi(VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter);
        if (VERSION.SDK_INT >= VERSION_CODES.ICE_CREAM_SANDWICH) {
            if (mPreviousTextLength == 0 && text.length() > 0 && !mHintAnimator.isStarted()) {
                mHintAnimator.start();
                mFocusColorAnimator.start();
                mAlphaAnimator.start();
            }
        }
        mPreviousTextLength = text.length();
    }

    @TargetApi(VERSION_CODES.HONEYCOMB)
    protected void handleTextColorOnFocus(boolean hasFocus) {
        if (VERSION.SDK_INT >= HONEYCOMB) {
            if (hasFocus) {
                mFocusColorAnimator.start();
            } else {
                mInactiveColorAnimator.start();
            }
        }
    }

}
