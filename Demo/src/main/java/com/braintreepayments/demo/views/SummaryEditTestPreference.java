package com.braintreepayments.demo.views;

import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.EditTextPreference;

public class SummaryEditTestPreference extends EditTextPreference {

    private String summaryString;

    public SummaryEditTestPreference(Context context) {
        super(context);
        init();
    }

    public SummaryEditTestPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SummaryEditTestPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public SummaryEditTestPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        summaryString = super.getSummary().toString();
    }

    /**
     * Returns the summary of this EditTextPreference. If the summary has a
     * {@link java.lang.String#format String formatting} marker in it (i.e. "%s" or "%1$s"), then the current value
     * will be substituted in its place.
     *
     * @return the summary with appropriate string substitution
     */
    @Override
    public CharSequence getSummary() {
        final CharSequence text = getText();
        return String.format(summaryString, text == null ? "" : text);
    }
}
