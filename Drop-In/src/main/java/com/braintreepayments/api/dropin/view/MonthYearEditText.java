package com.braintreepayments.api.dropin.view;

import android.content.Context;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputFilter.LengthFilter;
import android.text.InputType;
import android.text.Spanned;
import android.util.AttributeSet;

import com.braintreepayments.api.dropin.utils.DateValidator;

/**
 * An {@link android.widget.EditText} for entering dates, used for card expiration dates.
 * Will automatically format input as it is entered.
 */
public class MonthYearEditText extends FloatingLabelEditText {

    private boolean mChangeWasAddition;

    public MonthYearEditText(Context context) {
        super(context);
        init();
    }

    public MonthYearEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MonthYearEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        setInputType(InputType.TYPE_CLASS_NUMBER);
        InputFilter[] filters = { new LengthFilter(6) };
        setFilters(filters);
    }

    /**
     * @return the 2-digit month, formatted with a leading zero if necessary. If no month has been
     * specified, an empty string is returned.
     */
    public String getMonth() {
        String string = getString();
        if (string.length() < 2) {
            return "";
        }
        return getString().substring(0,2);
    }

    /**
     * @return the 2- or 4-digit year depending on user input.
     * If no year has been specified, an empty string is returned.
     */
    public String getYear() {
        String string = getString();
        if (string.length() == 4 || string.length() == 6) {
            return getString().substring(2);
        }
        return "";
    }

    /**
     * @return whether or not the input is a valid card expiration date.
     */
    @Override
    public boolean isValid() {
        return DateValidator.isValid(getMonth(), getYear());
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        mChangeWasAddition = count > before;
    }

    @Override
    public void afterTextChanged(Editable editable) {
        if (mChangeWasAddition) {
            if (editable.length() == 1 && Character.getNumericValue(editable.charAt(0)) >= 2) {
                prependLeadingZero(editable);
            }
        }

        Object[] paddingSpans = editable.getSpans(0, editable.length(), AppendSlashSpan.class);
        for (Object span : paddingSpans) {
            editable.removeSpan(span);
        }

        if (!mRightToLeftLanguage) {
            addDateSlash(editable);
        }

        super.afterTextChanged(editable);

        if ((getSelectionStart() == 4 && !editable.toString().endsWith("20")) || (getSelectionStart() == 6)) {
            focusNext();
        }
    }

    private void prependLeadingZero(Editable editable) {
        char firstChar = editable.charAt(0);
        editable.replace(0, 1, "0").append(firstChar);
    }

    private void addDateSlash(Editable editable) {
        final int index = 2;
        final int length = editable.length();
        if (index <= length) {
            editable.setSpan(new AppendSlashSpan(), index - 1, index,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    /**
     * Convenience method to get the input text as a {@link String}.
     */
    private String getString() {
        Editable editable = getText();
        return editable != null ? editable.toString() : "";
    }
}
