package com.braintreepayments.cardform.view;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.FontMetricsInt;
import android.text.style.ReplacementSpan;

/**
 * A {@link android.text.style.ReplacementSpan} used for slashes in {@link android.widget.EditText}.
 * Adds ' / '
 */
public class SlashSpan extends ReplacementSpan {

    @Override
    public int getSize(Paint paint, CharSequence text, int start, int end, FontMetricsInt fm) {
        float padding = paint.measureText(" ", 0, 1) * 2;
        float slash = paint.measureText("/", 0, 1);
        float textSize = paint.measureText(text, start, end);
        return (int) (padding + slash + textSize);
    }

    @Override
    public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top, int y,
            int bottom, Paint paint) {
        canvas.drawText(text.subSequence(start, end) + " / ", x, y, paint);
    }
}

