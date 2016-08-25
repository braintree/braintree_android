package com.braintreepayments.api;

import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.ImageButton;

import com.braintreepayments.api.dropin.view.LoadingHeader;
import com.braintreepayments.api.exceptions.BraintreeError;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.exceptions.UnexpectedException;
import com.braintreepayments.api.models.CardBuilder;
import com.braintreepayments.cardform.OnCardFormFieldFocusedListener;
import com.braintreepayments.cardform.OnCardFormSubmitListener;
import com.braintreepayments.cardform.OnCardFormValidListener;
import com.braintreepayments.cardform.view.CardForm;

/**
 * {@link BraintreeViewController} for coordinating the Add Payment Method form.
 * Responsible for managing views and form element bindings associated with adding a payment method.
 */
public class AddPaymentMethodViewController extends BraintreeViewController implements OnClickListener,
        OnCardFormSubmitListener, OnCardFormValidListener, OnCardFormFieldFocusedListener {

    private static final String EXTRA_FORM_IS_SUBMITTING = "com.braintreepayments.dropin.EXTRA_FORM_IS_SUBMITTING";
    private static final String EXTRA_SUBMIT_BUTTON_ENABLED = "com.braintreepayments.dropin.EXTRA_SUBMIT_BUTTON_ENABLED";
    private static final String EXTRA_FOCUS_EVENT_SENT = "com.braintreepayments.dropin.EXTRA_FOCUS_EVENT_SENT";

    private static final String INTEGRATION_METHOD = "dropin";

    /**
     * When adding new views, make sure to update {@link #onSaveInstanceState}, {@link #restoreState(Bundle)}
     * and the proper tests.
     */
    private PaymentButton mPaymentButton;
    private CardForm mCardForm;
    private View mDescription;
    private Button mSubmitButton;

    private LoadingHeader mLoadingHeader;

    private boolean mIsSubmitting;
    private boolean mFocusEventSent;

    public AddPaymentMethodViewController(BraintreePaymentActivity activity,
            Bundle savedInstanceState, View root, BraintreeFragment braintreeFragment,
            PaymentRequest paymentRequest) {
        super(activity, root, braintreeFragment, paymentRequest);
        mIsSubmitting = false;

        initViews();
        restoreState(savedInstanceState);
    }

    private void initViews() {
        mLoadingHeader = findView(com.braintreepayments.api.dropin.R.id.bt_header_container);
        mDescription = findView(com.braintreepayments.api.dropin.R.id.bt_description_container);
        mPaymentButton = (PaymentButton) mActivity.getFragmentManager()
                .findFragmentById(com.braintreepayments.api.dropin.R.id.bt_payment_button);
        mCardForm = findView(com.braintreepayments.api.dropin.R.id.bt_card_form);
        mSubmitButton = findView(com.braintreepayments.api.dropin.R.id.bt_card_form_submit_button);

        mPaymentButton.setOnClickListener(this);

        mPaymentRequest.androidPayRequestCode(AndroidPay.ANDROID_PAY_MASKED_WALLET_REQUEST_CODE);
        try {
            mPaymentButton.setPaymentRequest(mPaymentRequest);
        } catch (InvalidArgumentException ignored) {
            // already checked in BraintreePaymentActivity
        }

        mCardForm.setRequiredFields(mActivity, true, true,
                mBraintreeFragment.getConfiguration().isCvvChallengePresent(),
                mBraintreeFragment.getConfiguration().isPostalCodeChallengePresent(),
                getCustomizedCallToAction());
        mCardForm.useDialogForExpirationDateEntry(mActivity, false);
        mCardForm.setOnCardFormValidListener(this);
        mCardForm.setOnCardFormSubmitListener(this);
        mCardForm.setOnFormFieldFocusedListener(this);

        mSubmitButton.setOnClickListener(this);
        mSubmitButton.setText(getSubmitButtonText());
    }

    private void restoreState(Bundle savedInstanceState) {
        if (savedInstanceState.containsKey(EXTRA_FORM_IS_SUBMITTING)) {
            mIsSubmitting = savedInstanceState.getBoolean(EXTRA_FORM_IS_SUBMITTING);
            if (mIsSubmitting) {
                setUIForSubmit();
            }
        }

        if (savedInstanceState.containsKey(EXTRA_SUBMIT_BUTTON_ENABLED)) {
            mSubmitButton.setEnabled(savedInstanceState.getBoolean(EXTRA_SUBMIT_BUTTON_ENABLED));
        }

        mFocusEventSent = savedInstanceState.getBoolean(EXTRA_FOCUS_EVENT_SENT);

        if (mCardForm.isValid()) {
            setEnabledSubmitButtonStyle();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(EXTRA_FORM_IS_SUBMITTING, mIsSubmitting);
        outState.putBoolean(EXTRA_SUBMIT_BUTTON_ENABLED, mSubmitButton.isEnabled());
        outState.putBoolean(EXTRA_FOCUS_EVENT_SENT, mFocusEventSent);
    }

    @Override
    public void onClick(View v) {
        if (v == mPaymentButton.getView()) {
            mActivity.showLoadingView();
        } else if (v == mSubmitButton) {
            if (mCardForm.isValid()) {
                startSubmit();
                Card.tokenize(mBraintreeFragment, getCardBuilder());
                mBraintreeFragment.sendAnalyticsEvent("card.form.submitted.succeeded");
            } else {
                mCardForm.validate();
                setDisabledSubmitButtonStyle();
                mBraintreeFragment.sendAnalyticsEvent("card.form.submitted.failed");
            }
        }
    }

    @Override
    public void onCardFormValid(boolean valid) {
        ImageButton paypalButton = ((ImageButton) mActivity.findViewById(com.braintreepayments.api.dropin.R.id.bt_paypal_monogram_button));
        if(valid) {
            setEnabledSubmitButtonStyle();

            if (paypalButton != null && paypalButton.getVisibility() == View.VISIBLE) {
                AlphaAnimation alphaUp = new AlphaAnimation(1.0f, 0.4f);
                alphaUp.setFillAfter(true);
                alphaUp.setDuration(mActivity.getResources().getInteger(android.R.integer.config_shortAnimTime));
                paypalButton.startAnimation(alphaUp);
            }
        } else {
            setDisabledSubmitButtonStyle();

            if (paypalButton != null && paypalButton.getVisibility() == View.VISIBLE) {
                AlphaAnimation alphaUp = new AlphaAnimation(0.4f, 1.0f);
                alphaUp.setFillAfter(true);
                alphaUp.setDuration(mActivity.getResources().getInteger(android.R.integer.config_shortAnimTime));
                paypalButton.startAnimation(alphaUp);
            }
        }
    }

    @Override
    public void onCardFormSubmit() {
        mSubmitButton.performClick();
    }

    @Override
    public void onCardFormFieldFocused(final View field) {
        if (!mFocusEventSent) {
            mBraintreeFragment.sendAnalyticsEvent("card.form.focused");
            mFocusEventSent = true;
        }
    }

    private CardBuilder getCardBuilder() {
        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber(mCardForm.getCardNumber())
                .expirationMonth(mCardForm.getExpirationMonth())
                .expirationYear(mCardForm.getExpirationYear())
                .integration(INTEGRATION_METHOD);

        if (mBraintreeFragment.getConfiguration().isCvvChallengePresent()) {
            cardBuilder.cvv(mCardForm.getCvv());
        }
        if (mBraintreeFragment.getConfiguration().isPostalCodeChallengePresent()) {
            cardBuilder.postalCode(mCardForm.getPostalCode());
        }

        return cardBuilder;
    }

    public void setErrors(ErrorWithResponse error) {
        endSubmit();

        BraintreeError cardErrors = error.errorFor("creditCard");
        if(cardErrors != null) {
            mBraintreeFragment.sendAnalyticsEvent("add-card.failed");

            if(cardErrors.errorFor("number") != null) {
                mCardForm.setCardNumberError();
            }

            if(cardErrors.errorFor("expirationYear") != null ||
                    cardErrors.errorFor("expirationMonth") != null ||
                    cardErrors.errorFor("expirationDate") != null ) {
                mCardForm.setExpirationError();
            }

            if (cardErrors.errorFor("cvv") != null) {
                mCardForm.setCvvError();
            }

            if(cardErrors.errorFor("billingAddress") != null) {
                mCardForm.setPostalCodeError();
            }

            showErrorUI();
        } else {
            mActivity.onError(new UnexpectedException(error.getMessage()));
        }
    }

    private void showErrorUI() {
        mLoadingHeader.setError(mActivity.getString(com.braintreepayments.api.dropin.R.string.bt_invalid_card));
    }

    protected boolean isSubmitting() {
        return mIsSubmitting;
    }

    protected void endSubmit() {
        setDisabledSubmitButtonStyle();
        mCardForm.setEnabled(true);
        mSubmitButton.setEnabled(true);
        mIsSubmitting = false;
    }

    private void startSubmit() {
        mCardForm.closeSoftKeyboard();
        mIsSubmitting = true;
        setUIForSubmit();
    }

    private void setUIForSubmit() {
        mCardForm.setEnabled(false);
        mSubmitButton.setEnabled(false);

        mDescription.setVisibility(View.GONE);
        mLoadingHeader.setLoading();
    }

    protected void showSuccess() {
        mLoadingHeader.setSuccessful();
    }

    private void setEnabledSubmitButtonStyle() {
        mSubmitButton.setBackgroundResource(com.braintreepayments.api.dropin.R.drawable.bt_submit_button_background);
    }

    private void setDisabledSubmitButtonStyle() {
        mSubmitButton.setBackgroundResource(com.braintreepayments.api.dropin.R.color.bt_button_disabled_color);
    }
}
