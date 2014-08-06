package com.braintreepayments.api.dropin.view;

import android.content.Context;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.AttributeSet;

import com.braintreepayments.api.dropin.utils.DateValidator;

import java.util.regex.Pattern;

import static android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE;

/**
 * An {@link android.widget.EditText} for entering dates, used for card expiration dates.
 * Will automatically format input as it is entered.
 */
public class MonthYearEditText extends FloatingLabelEditText {

    private static final Pattern ILLEGAL_CHARACTERS = Pattern.compile("[^\\d/]");

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
        setInputType(InputType.TYPE_CLASS_DATETIME);
        addTextChangedListener(new AddSlashPaddingListener());
    }

    /**
     * @return the 2-digit month, formatted with a leading zero if necessary. If no month has been
     * specified, an empty string is returned.
     */
    public String getMonth() {
        return getString().split("/")[0];
    }

    /**
     * @return the 2- or 4-digit year depending on user input, formatted with leading zero if
     * necessary. If no year has been specified, an empty string is returned.
     */
    public String getYear() {
        String text = getString();
        int slashIndex = text.indexOf('/');
        if (slashIndex == -1) {
            return "";
        }
        return text.substring(slashIndex + 1);
    }

    /**
     * @return whether or not the input is a valid card expiration date.
     */
    @Override
    public boolean isValid() {
        return DateValidator.isValid(getString());
    }

    /**
     * Convenience method to get the input text as a {@link String}.
     */
    private String getString() {
        Editable editable = getText();
        return editable != null ? editable.toString() : "";
    }

    private static class AddSlashPaddingListener implements TextWatcher {

        /** Documents whether the most recent change was an addition or deletion. */
        private boolean mChangeWasAddition;

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        /**
         * @param before length of the changed region before the change
         * @param count length of the changed region after the change
         */
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            mChangeWasAddition = count > before;
        }

        @Override
        public void afterTextChanged(Editable editable) {
            if (mChangeWasAddition) {
                if (ILLEGAL_CHARACTERS.matcher(editable.toString()).find()) {
                    editable.replace(0, editable.length(),
                            ILLEGAL_CHARACTERS.matcher(editable.toString()).replaceAll(""));
                }

                if (editable.length() == 1 && Character.getNumericValue(editable.charAt(0)) >= 2) {
                    prependLeadingZero(editable);
                }

                if (editable.length() == 2 && !containsSlash(editable)) {
                    editable.append('/');
                }

                removeDoubleSlashes(editable);
            }

            for (int i = 0; i < editable.length(); i++) {
                if ('/' == editable.charAt(i) && !hasPaddingSpanAt(i, editable)) {
                    addPaddingSpanAt(i, editable);
                }
            }
        }

        private boolean containsSlash(Editable editable) {
            return editable.toString().contains("/");
        }

        private boolean hasPaddingSpanAt(int index, Editable editable) {
            return editable.getSpans(index, index + 1, PaddingSpan.class).length > 0;
        }

        private void addPaddingSpanAt(int index, Editable editable) {
            editable.setSpan(new PaddingSpan(1), index, index + 1, SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        private void prependLeadingZero(Editable editable) {
            char firstChar = editable.charAt(0);
            editable.replace(0, 1, "0").append(firstChar);
        }

        private void removeDoubleSlashes(Editable editable) {
            int index = editable.toString().indexOf("//");
            if (index != -1) {
                editable.replace(index, index + 2, "/");
            }
        }
    }
}
