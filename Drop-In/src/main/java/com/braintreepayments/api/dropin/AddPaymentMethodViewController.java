package com.braintreepayments.api.dropin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ScrollView;

import com.braintreepayments.api.Braintree;
import com.braintreepayments.api.dropin.view.LoadingHeader;
import com.braintreepayments.api.dropin.view.PaymentButton;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.exceptions.ErrorWithResponse.BraintreeError;
import com.braintreepayments.api.exceptions.UnexpectedException;
import com.braintreepayments.api.models.CardBuilder;
import com.braintreepayments.cardform.OnCardFormFieldFocusedListener;
import com.braintreepayments.cardform.OnCardFormSubmitListener;
import com.braintreepayments.cardform.OnCardFormValidListener;
import com.braintreepayments.cardform.view.CardForm;

/**
 * {@link com.braintreepayments.api.dropin.BraintreeViewController} for coordinating the Add Payment Method form.
 * Responsible for managing views and form element bindings associated with adding a payment method.
 */
public class AddPaymentMethodViewController extends BraintreeViewController
        implements OnClickListener, OnCardFormSubmitListener, OnCardFormValidListener,
        OnCardFormFieldFocusedListener {

    private static final String EXTRA_FORM_IS_SUBMITTING = "com.braintreepayments.api.dropin.EXTRA_FORM_IS_SUBMITTING";
    private static final String EXTRA_SUBMIT_BUTTON_ENABLED = "com.braintreepayments.api.dropin.EXTRA_SUBMIT_BUTTON_ENABLED";

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
    private ScrollView mScrollView;

    private boolean mIsSubmitting;

    public AddPaymentMethodViewController(BraintreePaymentActivity activity,
            Bundle savedInstanceState, View root, Braintree braintree, Customization customization) {
        super(activity, root, braintree, customization);
        mIsSubmitting = false;

        initViews();
        restoreState(savedInstanceState);
    }

    private void initViews() {
        mLoadingHeader = findView(R.id.bt_header_container);
        mScrollView = findView(R.id.bt_form_scroll_container);
        mDescription = findView(R.id.bt_description_container);
        mPaymentButton = findView(R.id.bt_payment_button);
        mCardForm = findView(R.id.bt_card_form);
        mSubmitButton = findView(R.id.bt_card_form_submit_button);

        mPaymentButton.initialize(getActivity(), mBraintree);

        mCardForm.setRequiredFields(true, true, mBraintree.isCvvChallenegePresent(),
                mBraintree.isPostalCodeChallengePresent(), getCustomizedCallToAction());
        mCardForm.setOnCardFormValidListener(this);
        mCardForm.setOnCardFormSubmitListener(this);
        mCardForm.setOnFormFieldFocusedListener(this);

        mSubmitButton.setOnClickListener(this);
        mSubmitButton.setText(getSubmitButtonText());
    }

    private void restoreState(Bundle savedInstanceState) {
        mCardForm.onRestoreInstanceState(savedInstanceState);

        if (savedInstanceState.containsKey(EXTRA_FORM_IS_SUBMITTING)) {
            mIsSubmitting = savedInstanceState.getBoolean(EXTRA_FORM_IS_SUBMITTING);
            if (mIsSubmitting) {
                setUIForSubmit();
            }
        }

        if (savedInstanceState.containsKey(EXTRA_SUBMIT_BUTTON_ENABLED)) {
            mSubmitButton.setEnabled(savedInstanceState.getBoolean(EXTRA_SUBMIT_BUTTON_ENABLED));
        }

        if (mCardForm.isValid()) {
            setEnabledSubmitButtonStyle();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        mCardForm.onSaveInstanceState(outState);
        outState.putBoolean(EXTRA_FORM_IS_SUBMITTING, mIsSubmitting);
        outState.putBoolean(EXTRA_SUBMIT_BUTTON_ENABLED, mSubmitButton.isEnabled());
    }

    @Override
    public void onClick(View v) {
        if (v == mSubmitButton) {
            if (mCardForm.isValid()) {
                startSubmit();
                mBraintree.create(getCardBuilder());
            } else {
                mCardForm.validate();
                setDisabledSubmitButtonStyle();
            }
        }
    }

    @Override
    public void onCardFormValid(boolean valid) {
        if(valid) {
            setEnabledSubmitButtonStyle();
        } else {
            setDisabledSubmitButtonStyle();
        }
    }

    @Override
    public void onCardFormSubmit() {
        mSubmitButton.performClick();
    }

    @Override
    public void onCardFormFieldFocused(final View field) {
        mScrollView.postDelayed(new Runnable() {
            public void run() {
                mScrollView.smoothScrollTo(0, field.getTop());
            }
        }, 100);
    }

    private CardBuilder getCardBuilder() {
        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber(mCardForm.getCardNumber())
                .expirationMonth(mCardForm.getExpirationMonth())
                .expirationYear(mCardForm.getExpirationYear())
                .integration(INTEGRATION_METHOD);

        if (mBraintree.isCvvChallenegePresent()) {
            cardBuilder.cvv(mCardForm.getCvv());
        }
        if (mBraintree.isPostalCodeChallengePresent()) {
            cardBuilder.postalCode(mCardForm.getPostalCode());
        }

        return cardBuilder;
    }

    public void onPaymentResult(int requestCode, int resultCode, Intent data) {
        mIsSubmitting = true;
        mPaymentButton.onActivityResult(requestCode, resultCode, data);
    }

    public void setErrors(ErrorWithResponse error) {
        showErrorUI();
        endSubmit();

        if(error.errorFor("creditCard") != null) {
            mBraintree.sendAnalyticsEvent("add-card.failed");

            BraintreeError cardErrors = error.errorFor("creditCard");
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
        } else {
            getActivity().onUnrecoverableError(new UnexpectedException(error.getMessage()));
        }
    }

    private void showErrorUI() {
        mLoadingHeader.setError(getActivity().getString(R.string.bt_invalid_card));
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
        mSubmitButton.setBackgroundResource(R.drawable.bt_submit_button_background);
    }

    private void setDisabledSubmitButtonStyle() {
        mSubmitButton.setBackgroundResource(R.color.bt_button_disabled_color);
    }

}
