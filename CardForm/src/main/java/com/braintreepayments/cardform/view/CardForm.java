package com.braintreepayments.cardform.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.text.Editable;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.braintreepayments.cardform.OnCardFormFieldFocusedListener;
import com.braintreepayments.cardform.OnCardFormSubmitListener;
import com.braintreepayments.cardform.OnCardFormValidListener;
import com.braintreepayments.cardform.R;
import com.braintreepayments.cardform.utils.CardType;
import com.braintreepayments.cardform.view.CardEditText.OnCardTypeChangedListener;
import com.braintreepayments.cardform.view.FloatingLabelEditText.OnTextChangedListener;

public class CardForm extends LinearLayout implements
        OnCardTypeChangedListener, OnFocusChangeListener, OnClickListener,
        OnTextChangedListener, OnEditorActionListener {

    private static final String EXTRA_CARD_NUMBER_TEXT = "com.braintreepayments.cardform.EXTRA_CARD_NUMBER_TEXT";
    private static final String EXTRA_CVV_TEXT = "com.braintreepayments.cardform.EXTRA_CVV_TEXT";
    private static final String EXTRA_EXPIRATION_TEXT = "com.braintreepayments.cardform.EXTRA_EXPIRATION_TEXT";
    private static final String EXTRA_POSTAL_CODE_TEXT = "com.braintreepayments.cardform.EXTRA_POSTAL_CODE_TEXT";

    private CardEditText mCardNumber;
    private MonthYearEditText mExpirationView;
    private CvvEditText mCvvView;
    private PostalCodeEditText mPostalCode;

    private boolean mCardNumberRequired;
    private boolean mExpirationRequired;
    private boolean mCvvRequired;
    private boolean mPostalCodeRequired;

    private boolean mValid = false;

    private OnCardFormValidListener mOnCardFormValidListener;
    private OnCardFormSubmitListener mOnCardFormSubmitListener;
    private OnCardFormFieldFocusedListener mOnCardFormFieldFocusedListener;

    public CardForm(Context context) {
        super(context);
        init();
    }

    public CardForm(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    @TargetApi(VERSION_CODES.HONEYCOMB)
    public CardForm(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(VERSION_CODES.LOLLIPOP)
    public CardForm(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        inflate(getContext(), R.layout.bt_card_form_fields, this);

        mCardNumber = (CardEditText) findViewById(R.id.bt_card_form_card_number);
        mExpirationView = (MonthYearEditText) findViewById(R.id.bt_card_form_expiration);
        mCvvView = (CvvEditText) findViewById(R.id.bt_card_form_cvv);
        mPostalCode = (PostalCodeEditText) findViewById(R.id.bt_card_form_postal_code);

        mCardNumber.setFocusChangeListener(this);
        mExpirationView.setFocusChangeListener(this);
        mCvvView.setFocusChangeListener(this);
        mPostalCode.setFocusChangeListener(this);

        mCardNumber.setOnClickListener(this);
        mExpirationView.setOnClickListener(this);
        mCvvView.setOnClickListener(this);
        mPostalCode.setOnClickListener(this);

        mCardNumber.setOnCardTypeChangedListener(this);

        setRequiredFields(true, true, true, true,
                getContext().getString(R.string.bt_default_action_label));
    }

    /**
     * Set the required fields for the {@link com.braintreepayments.cardform.view.CardForm}.
     * If {@link #setRequiredFields(boolean, boolean, boolean, boolean, String)} is not called,
     * {@link com.braintreepayments.cardform.view.CardForm} defaults to all fields required
     * and a label of "Purchase"
     *
     * @param cardNumberRequired {@code true} to show and require a credit card number, {@code false} otherwise
     * @param expirationRequired {@code true} to show and require an expiration date, {@code false} otherwise
     * @param cvvRequired {@code true} to show and require a cvv, {@code false} otherwise
     * @param postalCodeRequired {@code true} to show and require a postal code, {@code false} otherwise
     * @param imeActionLabel the {@link java.lang.String} to display to the user to submit the form
     *   from the keyboard
     */
    public void setRequiredFields(boolean cardNumberRequired, boolean expirationRequired,
            boolean cvvRequired, boolean postalCodeRequired, String imeActionLabel) {
        mCardNumberRequired = cardNumberRequired;
        mExpirationRequired = expirationRequired;
        mCvvRequired = cvvRequired;
        mPostalCodeRequired = postalCodeRequired;

        if (cardNumberRequired) {
            mCardNumber.setTextChangedListener(this);
        } else {
            mCardNumber.setVisibility(View.GONE);
        }

        if (expirationRequired) {
            mExpirationView.setTextChangedListener(this);
        } else {
            mExpirationView.setVisibility(View.GONE);
        }

        if (cvvRequired || postalCodeRequired) {
            mExpirationView.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        } else {
            setIMEOptionsForLastEditTestField(mExpirationView, imeActionLabel);
        }

        if (cvvRequired) {
            mCvvView.setTextChangedListener(this);
            if (postalCodeRequired) {
                mCvvView.setImeOptions(EditorInfo.IME_ACTION_NEXT);
            } else {
                setIMEOptionsForLastEditTestField(mCvvView, imeActionLabel);
            }
        } else {
            mCvvView.setVisibility(View.GONE);
        }

        if (postalCodeRequired) {
            mPostalCode.setTextChangedListener(this);
            setIMEOptionsForLastEditTestField(mPostalCode, imeActionLabel);
        } else {
            mPostalCode.setVisibility(View.GONE);
        }

        mCardNumber.setOnCardTypeChangedListener(this);
    }

    private void setIMEOptionsForLastEditTestField(EditText editText, String imeActionLabel) {
        editText.setImeOptions(EditorInfo.IME_ACTION_GO);
        editText.setImeActionLabel(imeActionLabel, EditorInfo.IME_ACTION_GO);
        editText.setOnEditorActionListener(this);
    }

    /**
     * Set the listener to receive a callback when the card form becomes valid or invalid
     * @param listener to receive the callback
     */
    public void setOnCardFormValidListener(OnCardFormValidListener listener) {
        mOnCardFormValidListener = listener;
    }

    /**
     * Set the listener to receive a callback when the card form should be submitted.
     * Triggered from a keyboard by a {@link android.view.inputmethod.EditorInfo#IME_ACTION_GO} event
     * @param listener to receive the callback
     */
    public void setOnCardFormSubmitListener(OnCardFormSubmitListener listener) {
        mOnCardFormSubmitListener = listener;
    }

    /**
     * Set the listener to receive a callback when a field is focused
     * @param listener to receive the callback
     */
    public void setOnFormFieldFocusedListener(OnCardFormFieldFocusedListener listener) {
        mOnCardFormFieldFocusedListener = listener;
    }

    /**
     * Call {@link #onRestoreInstanceState(android.os.Bundle)} when resuming after an event such
     * as a rotation
     * @param savedInstanceState from {@link android.app.Activity#onCreate(android.os.Bundle)}
     */
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            restoreText(savedInstanceState, mCardNumber, EXTRA_CARD_NUMBER_TEXT);
            restoreText(savedInstanceState, mCvvView, EXTRA_CVV_TEXT);
            restoreText(savedInstanceState, mExpirationView, EXTRA_EXPIRATION_TEXT);
            restoreText(savedInstanceState, mPostalCode, EXTRA_POSTAL_CODE_TEXT);
        }
    }

    /**
     * Call when saving instance state before an event such as a rotation
     * @param outState from {@link android.app.Activity#onSaveInstanceState(android.os.Bundle)}
     */
    public void onSaveInstanceState(Bundle outState) {
        outState.putCharSequence(EXTRA_CARD_NUMBER_TEXT, mCardNumber.getText());
        outState.putCharSequence(EXTRA_CVV_TEXT, mCvvView.getText());
        outState.putCharSequence(EXTRA_EXPIRATION_TEXT, mExpirationView.getText());
        outState.putCharSequence(EXTRA_POSTAL_CODE_TEXT, mPostalCode.getText());
    }

    private void restoreText(Bundle savedInstanceState, TextView view, String extra) {
        if (savedInstanceState.containsKey(extra)) {
            view.setText(savedInstanceState.getCharSequence(extra));
        }
    }

    /**
     * Set {@link android.widget.EditText} fields as enabled or disabled
     * @param enabled {@code true} to enable all required fields, {@code false} to disable all
     * required fields
     */
    public void setEnabled(boolean enabled) {
        mCardNumber.setEnabled(enabled);
        mExpirationView.setEnabled(enabled);
        mCvvView.setEnabled(enabled);
        mPostalCode.setEnabled(enabled);
    }

    /**
     * @return {@code true} if all require fields are valid, otherwise {@code false}
     */
    public boolean isValid() {
        boolean valid = true;
        if (mCardNumberRequired) {
            valid = valid && mCardNumber.isValid();
        }
        if (mExpirationRequired) {
            valid = valid && mExpirationView.isValid();
        }
        if (mCvvRequired) {
            valid = valid && mCvvView.isValid();
        }
        if (mPostalCodeRequired) {
            valid = valid && mPostalCode.isValid();
        }
        return valid;
    }

    /**
     * Validate all required fields and mark invalid fields with an error indicator
     */
    public void validate() {
        if (mCardNumberRequired) {
            mCardNumber.validate();
        }
        if (mExpirationRequired) {
            mExpirationView.validate();
        }
        if (mCvvRequired) {
            mCvvView.validate();
        }
        if (mPostalCodeRequired) {
            mPostalCode.validate();
        }
    }

    /**
     * Set visual indicator on card number to indicate error
     */
    public void setCardNumberError() {
        if (mCardNumberRequired) {
            mCardNumber.setError();
            requestEditTextFocus(mCardNumber);
        }
    }

    /**
     * Set visual indicator on expiration to indicate error
     */
    public void setExpirationError() {
        if (mExpirationRequired) {
            mExpirationView.setError();
            if (!mCardNumberRequired || !mCardNumber.isFocused()) {
                requestEditTextFocus(mExpirationView);
            }
        }
    }

    /**
     * Set visual indicator on cvv to indicate error
     */
    public void setCvvError() {
        if (mCvvRequired) {
            mCvvView.setError();
            if ((!mCardNumberRequired && !mExpirationRequired) ||
                (!mCardNumber.isFocused() && !mExpirationView.isFocused())) {
                requestEditTextFocus(mCvvView);
            }
        }
    }

    /**
     * Set visual indicator on postal code to indicate error
     */
    public void setPostalCodeError() {
        if (mPostalCodeRequired) {
            mPostalCode.setError();
            if ((!mCardNumberRequired && !mExpirationRequired && !mCvvRequired) ||
                (!mCardNumber.isFocused() && !mExpirationView.isFocused() && !mCvvView.isFocused())) {
                requestEditTextFocus(mPostalCode);
            }
        }
    }

    private void requestEditTextFocus(EditText editText) {
        editText.requestFocus();
        ((InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE))
                .showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
    }

    /**
     * Attempt to close the soft keyboard. Will have no effect if the keyboard is not open.
     */
    public void closeSoftKeyboard() {
        ((InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(getWindowToken(), 0);
    }

    /**
     * @return the text in the card number field
     */
    public String getCardNumber() {
        return mCardNumber.getText().toString();
    }

    /**
     * @return the 2-digit month, formatted with a leading zero if necessary from the expiration
     * field. If no month has been specified, an empty string is returned.
     */
    public String getExpirationMonth() {
        return mExpirationView.getMonth();
    }

    /**
     * @return the 2- or 4-digit year depending on user input from the expiration field.
     * If no year has been specified, an empty string is returned.
     */
    public String getExpirationYear() {
        return mExpirationView.getYear();
    }

    /**
     * @return the text in the cvv field
     */
    public String getCvv() {
        return mCvvView.getText().toString();
    }

    /**
     * @return the text in the postal code field
     */
    public String getPostalCode() {
        return mPostalCode.getText().toString();
    }

    @Override
    public void onCardTypeChanged(CardType cardType) {
        mCvvView.setCardType(cardType);
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (hasFocus && mOnCardFormFieldFocusedListener != null) {
            mOnCardFormFieldFocusedListener.onCardFormFieldFocused(v);
        }
    }

    @Override
    public void onClick(View v) {
        if (mOnCardFormFieldFocusedListener != null) {
            mOnCardFormFieldFocusedListener.onCardFormFieldFocused(v);
        }
    }

    @Override
    public void onTextChanged(Editable editable) {
        boolean valid = isValid();
        if (mValid != valid) {
            mValid = valid;
            if (mOnCardFormValidListener != null) {
                mOnCardFormValidListener.onCardFormValid(valid);
            }
        }
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_GO && mOnCardFormSubmitListener != null) {
            mOnCardFormSubmitListener.onCardFormSubmit();
            return true;
        }
        return false;
    }

}
