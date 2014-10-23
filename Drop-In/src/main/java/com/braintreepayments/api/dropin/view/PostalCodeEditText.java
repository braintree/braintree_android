package com.braintreepayments.api.dropin.view;

import android.content.Context;
import android.text.InputFilter;
import android.text.InputFilter.LengthFilter;
import android.text.InputType;
import android.util.AttributeSet;

/**
 * Input for postal codes. Validated for presence only due to the wide variation of postal code formats worldwide.
 */
public class PostalCodeEditText extends FloatingLabelEditText {
    public PostalCodeEditText(Context context) {
        super(context);
        init();
    }

    public PostalCodeEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PostalCodeEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        setInputType(InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS);
        InputFilter[] filters = { new LengthFilter(16) };
        setFilters(filters);
    }

    @Override
    public boolean isValid() {
        return getText().toString().length() > 0;
    }

}
