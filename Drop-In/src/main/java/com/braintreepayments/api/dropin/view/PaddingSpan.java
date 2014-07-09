package com.braintreepayments.api.dropin.view;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.FontMetricsInt;
import android.text.style.ReplacementSpan;

/**
 * A {@link android.text.style.ReplacementSpan} used for spacing in {@link android.widget.EditText}
 * to space things out. Adds ' 's
 */
public class PaddingSpan extends ReplacementSpan {

    private final int mLeftPaddingChars;
    private final int mRightPaddingChars;

    public PaddingSpan(int paddingChars) {
        this(paddingChars, paddingChars);
    }

    public PaddingSpan(int leftPaddingChars, int rightPaddingChars) {
        mLeftPaddingChars = leftPaddingChars;
        mRightPaddingChars = rightPaddingChars;
    }

    @Override
    public int getSize(Paint paint, CharSequence text, int start, int end, FontMetricsInt fm) {
        float padding = paint.measureText(" ", 0, 1);
        float textSize = paint.measureText(text, start, end);
        return (int) (padding * (mLeftPaddingChars + mRightPaddingChars) + textSize);
    }

    @Override
    public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top, int y,
            int bottom, Paint paint) {
        StringBuilder string = new StringBuilder();
        for (int i = 0; i < mLeftPaddingChars; i++) {
            string.append(' ');
        }
        string.append(text.subSequence(start, end));
        for (int i = 0; i < mRightPaddingChars; i++) {
            string.append(' ');
        }

        canvas.drawText(string.toString(), x, y, paint);
    }
}
