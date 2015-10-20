package com.braintreepayments.api;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewStub;

import com.braintreepayments.api.dropin.R;
import com.braintreepayments.api.exceptions.AuthenticationException;
import com.braintreepayments.api.exceptions.AuthorizationException;
import com.braintreepayments.api.exceptions.ConfigurationException;
import com.braintreepayments.api.exceptions.DownForMaintenanceException;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.exceptions.ServerException;
import com.braintreepayments.api.exceptions.UnexpectedException;
import com.braintreepayments.api.exceptions.UpgradeRequiredException;
import com.braintreepayments.api.interfaces.BraintreeCancelListener;
import com.braintreepayments.api.interfaces.BraintreeErrorListener;
import com.braintreepayments.api.interfaces.PaymentMethodCreatedListener;
import com.braintreepayments.api.interfaces.PaymentMethodsUpdatedListener;
import com.braintreepayments.api.models.AndroidPayCard;
import com.braintreepayments.api.models.Card;
import com.braintreepayments.api.models.PayPalAccount;
import com.braintreepayments.api.models.PaymentMethod;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * {@link android.app.Activity} encompassing Braintree's Drop-In UI.
 */
public class BraintreePaymentActivity extends Activity implements PaymentMethodsUpdatedListener,
        PaymentMethodCreatedListener, BraintreeCancelListener, BraintreeErrorListener {

    /**
     * {@link com.braintreepayments.api.models.PaymentMethod} returned by successfully exiting the flow.
     */
    public static final String EXTRA_PAYMENT_METHOD = "com.braintreepayments.api.dropin.EXTRA_PAYMENT_METHOD";

    /**
     * Error messages are returned as the value of this key in the data intent in {@link android.app.Activity#onActivityResult(int, int, android.content.Intent)}
     * if {@code responseCode} is not {@link android.app.Activity#RESULT_OK} or {@link android.app.Activity#RESULT_CANCELED}
     */
    public static final String EXTRA_ERROR_MESSAGE = "com.braintreepayments.api.dropin.EXTRA_ERROR_MESSAGE";

    /**
     * The payment method flow halted due to a resolvable error (authentication, authorization, SDK upgrade required).
     * The reason for the error will be returned in a future release.
     */
    public static final int BRAINTREE_RESULT_DEVELOPER_ERROR = 2;

    /**
     * The payment method flow halted due to an error from the Braintree gateway.
     * The best recovery path is to try again with a new client token.
     */
    public static final int BRAINTREE_RESULT_SERVER_ERROR = 3;

    /**
     * The payment method flow halted due to the Braintree gateway going down for maintenance.
     * Try again later.
     */
    public static final int BRAINTREE_RESULT_SERVER_UNAVAILABLE = 4;

    static final String EXTRA_CHECKOUT_REQUEST = "com.braintreepayments.api.EXTRA_CHECKOUT_REQUEST";

    private static final String ON_PAYMENT_METHOD_ADD_FORM_KEY = "com.braintreepayments.api.dropin.PAYMENT_METHOD_ADD_FORM";

    @VisibleForTesting
    protected BraintreeFragment mBraintreeFragment;
    private AddPaymentMethodViewController mAddPaymentMethodViewController;
    private SelectPaymentMethodViewController mSelectPaymentMethodViewController;
    private AtomicBoolean mHavePaymentMethodsBeenReceived = new AtomicBoolean(false);
    private Bundle mSavedInstanceState;
    private PaymentRequest mPaymentRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bt_drop_in_ui);

        mSavedInstanceState = (savedInstanceState != null) ? savedInstanceState : new Bundle();
        mPaymentRequest = getIntent().getParcelableExtra(EXTRA_CHECKOUT_REQUEST);
        customizeActionBar();

        showLoadingView();
        try {
            mBraintreeFragment = getBraintreeFragment();

            if (mBraintreeFragment.hasFetchedPaymentMethods()) {
                if (mSavedInstanceState.getBoolean(ON_PAYMENT_METHOD_ADD_FORM_KEY)) {
                    showAddPaymentMethodView();
                } else {
                    onPaymentMethodsUpdated(mBraintreeFragment.getCachedPaymentMethods());
                }
            } else {
                TokenizationClient.getPaymentMethods(mBraintreeFragment);
                waitForData();
            }
        } catch (InvalidArgumentException e) {
            setResult(BRAINTREE_RESULT_DEVELOPER_ERROR, new Intent().putExtra(EXTRA_ERROR_MESSAGE, e));
            finish();
        }
    }

    @Override
    public void onPaymentMethodsUpdated(List<PaymentMethod> paymentMethods) {
        if (!mHavePaymentMethodsBeenReceived.get()) {
            mBraintreeFragment.sendAnalyticsEvent("appeared");
            mHavePaymentMethodsBeenReceived.set(true);
        }

        if (paymentMethods.size() == 0) {
            showAddPaymentMethodView();
        } else {
            initSelectPaymentMethodView();
        }
    }

    @Override
    public void onPaymentMethodCreated(final PaymentMethod paymentMethod) {
        if (paymentMethod instanceof Card) {
            if (StubbedView.CARD_FORM.mCurrentView) {
                mAddPaymentMethodViewController.showSuccess();
                Executors.newScheduledThreadPool(1).schedule(new Runnable() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                finalizeSelection(paymentMethod);
                            }
                        });
                    }
                }, 1, TimeUnit.SECONDS);
            } else {
                finishCreate();
            }
        } else if (paymentMethod instanceof PayPalAccount) {
            mBraintreeFragment.sendAnalyticsEvent("add-paypal.success");
            finishCreate();
        } else if (paymentMethod instanceof AndroidPayCard) {
            mBraintreeFragment.sendAnalyticsEvent("add-android-pay.success");
            finishCreate();
        }
    }

    private void finishCreate() {
        mAddPaymentMethodViewController.endSubmit();
        initSelectPaymentMethodView();
        mSelectPaymentMethodViewController.onPaymentMethodSelected(0);
    }

    @Override
    public void onCancel(int requestCode) {
        showAddPaymentMethodView();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if ((requestCode == AndroidPay.ANDROID_PAY_MASKED_WALLET_REQUEST_CODE ||
                requestCode == AndroidPay.ANDROID_PAY_FULL_WALLET_REQUEST_CODE) &&
                resultCode == RESULT_OK) {
            AndroidPay.onActivityResult(mBraintreeFragment, mPaymentRequest.getAndroidPayCart(),
                    mPaymentRequest.isAndroidPayBillingAgreement(), resultCode, data);
        } else if (resultCode != RESULT_OK) {
            showAddPaymentMethodView();
        }
    }

    @Override
    public void onError(Exception error) {
        if (error instanceof ErrorWithResponse) {
            mAddPaymentMethodViewController.setErrors((ErrorWithResponse) error);
        } else {
            // Falling back to add payment method if getPaymentMethods fails
            if (StubbedView.LOADING_VIEW.mCurrentView && !mHavePaymentMethodsBeenReceived.get() &&
                    mBraintreeFragment.getConfiguration() != null) {
                mBraintreeFragment.sendAnalyticsEvent("appeared");
                mHavePaymentMethodsBeenReceived.set(true);
                showAddPaymentMethodView();
            } else {
                if (error instanceof AuthenticationException ||
                        error instanceof AuthorizationException ||
                        error instanceof UpgradeRequiredException ||
                        error instanceof ConfigurationException) {
                    mBraintreeFragment.sendAnalyticsEvent("sdk.exit.developer-error");
                    setResult(BRAINTREE_RESULT_DEVELOPER_ERROR,
                            new Intent().putExtra(EXTRA_ERROR_MESSAGE, error));
                } else if (error instanceof ServerException ||
                        error instanceof UnexpectedException) {
                    mBraintreeFragment.sendAnalyticsEvent("sdk.exit.server-error");
                    setResult(BRAINTREE_RESULT_SERVER_ERROR,
                            new Intent().putExtra(EXTRA_ERROR_MESSAGE, error));
                } else if (error instanceof DownForMaintenanceException) {
                    mBraintreeFragment.sendAnalyticsEvent("sdk.exit.server-unavailable");
                    setResult(BRAINTREE_RESULT_SERVER_UNAVAILABLE,
                            new Intent().putExtra(EXTRA_ERROR_MESSAGE, error));
                }

                finish();
            }
        }
    }

    protected void finalizeSelection(PaymentMethod paymentMethod) {
        mBraintreeFragment.sendAnalyticsEvent("sdk.exit.success");

        Intent resultIntent = new Intent();
        resultIntent.putExtra(EXTRA_PAYMENT_METHOD, paymentMethod);
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    private void waitForData() {
        Executors.newScheduledThreadPool(1).schedule(new Runnable() {
            @Override
            public void run() {
                if (!mHavePaymentMethodsBeenReceived.get()) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mHavePaymentMethodsBeenReceived.set(true);
                            showAddPaymentMethodView();
                        }
                    });
                }
            }
        }, 10, TimeUnit.SECONDS);
        showLoadingView();
    }

    private void initSelectPaymentMethodView() {
        View selectMethodView = StubbedView.SELECT_VIEW.show(this);

        if (mSelectPaymentMethodViewController == null) {
            mSelectPaymentMethodViewController = new SelectPaymentMethodViewController(this,
                    mSavedInstanceState, selectMethodView, mBraintreeFragment, mPaymentRequest);
        } else {
            mSelectPaymentMethodViewController.setupPaymentMethod();
        }

        setActionBarUpEnabled(false);
    }

    protected void showAddPaymentMethodView() {
        initAddPaymentMethodView(StubbedView.CARD_FORM.show(this));

        if (mBraintreeFragment.getCachedPaymentMethods().size() > 0) {
            setActionBarUpEnabled(true);
        }
    }

    private void initAddPaymentMethodView(View paymentMethodView) {
        if (mAddPaymentMethodViewController == null) {
            mAddPaymentMethodViewController = new AddPaymentMethodViewController(this,
                    mSavedInstanceState, paymentMethodView, mBraintreeFragment, mPaymentRequest);
        }
    }

    protected void showLoadingView() {
        StubbedView.LOADING_VIEW.show(this);
    }

    private void setActionBarUpEnabled(boolean enabled) {
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(enabled);
        }
    }

    @VisibleForTesting
    protected BraintreeFragment getBraintreeFragment() throws InvalidArgumentException {
        return BraintreeFragment.newInstance(this, getAuthorization());
    }

    @VisibleForTesting
    protected String getAuthorization() {
        if (TextUtils.isEmpty(mPaymentRequest.getAuthorization())) {
            throw new IllegalArgumentException("A client token or client key must be specified " +
                    " in the " + PaymentRequest.class.getSimpleName());
        }

        return mPaymentRequest.getAuthorization();
    }

    private void customizeActionBar() {
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            if (TextUtils.isEmpty(mPaymentRequest.getActionBarTitle())) {
                actionBar.setTitle(getString(R.string.bt_default_action_bar_text));
            } else {
                actionBar.setTitle(mPaymentRequest.getActionBarTitle());
            }

            if (mPaymentRequest.getActionBarLogo() == 0) {
                actionBar.setLogo(new ColorDrawable(
                        getResources().getColor(android.R.color.transparent)));
            } else {
                actionBar.setLogo(mPaymentRequest.getActionBarLogo());
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (StubbedView.CARD_FORM.mCurrentView && mBraintreeFragment.getCachedPaymentMethods().size() > 0) {
            initSelectPaymentMethodView();
        } else if (mAddPaymentMethodViewController != null &&
                mAddPaymentMethodViewController.isSubmitting()) {
            // noop
        } else {
            mBraintreeFragment.sendAnalyticsEvent("sdk.exit.user-canceled");

            setResult(RESULT_CANCELED);
            finish();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (StubbedView.CARD_FORM.mCurrentView) {
            outState.putBoolean(ON_PAYMENT_METHOD_ADD_FORM_KEY, true);
        }

        saveState(mAddPaymentMethodViewController, outState);
        saveState(mSelectPaymentMethodViewController, outState);
    }

    private void saveState(BraintreeViewController viewController, Bundle outState) {
        if (viewController != null) {
            viewController.onSaveInstanceState(outState);
        }
    }

    /**
     * A simple interface to decide which of the central views should be displayed. The description
     * is not included here since it is included with all views if it is specified.
     */
    private enum StubbedView {
        LOADING_VIEW(R.id.bt_stub_loading_view, R.id.bt_inflated_loading_view),
        SELECT_VIEW(R.id.bt_stub_payment_methods_list, R.id.bt_inflated_payment_methods_list),
        CARD_FORM(R.id.bt_stub_payment_method_form, R.id.bt_inflated_payment_method_form);

        private final int mStubbedViewId;
        private final int mInflatedViewId;
        private boolean mCurrentView;

        private static int mAnimationDuration;

        StubbedView(int stubbedViewId, int inflatedViewId) {
            mStubbedViewId = stubbedViewId;
            mInflatedViewId = inflatedViewId;
            mCurrentView = false;
        }

        /**
         * @param activity activity for the views.
         * @return the inflated or found view.
         */
        @SuppressWarnings("unchecked")
        <T extends View> T inflateOrFind(BraintreePaymentActivity activity) {
            ViewStub stub = activity.findView(mStubbedViewId);
            if (stub != null) {
                return (T) stub.inflate();
            } else {
                return activity.findView(mInflatedViewId);
            }
        }

        /**
         * @param activity hosting activity for the views. Should always be {@code this}.
         * @return the displayed {@link View}.
         */
        @SuppressLint("NewApi")
        @SuppressWarnings("unchecked")
        <T extends View> T show(BraintreePaymentActivity activity) {
            for (StubbedView value : values()) {
                if (this != value) {
                    value.hide(activity);
                }
            }

            View inflated = inflateOrFind(activity);
            inflated.setAlpha(0f);
            inflated.setVisibility(View.VISIBLE);
            inflated.animate()
                    .alpha(1f)
                    .setDuration(getDuration(activity));

            mCurrentView = true;

            return (T) inflated;
        }

        /**
         * @param activity hosting activity for the views. Should always be {@code this}.
         */
        void hide(BraintreePaymentActivity activity) {
            ViewStub stub = activity.findView(mStubbedViewId);
            if (stub == null) {
                activity.findView(mInflatedViewId).setVisibility(View.GONE);
            }

            mCurrentView = false;
        }

        private long getDuration(Context context) {
            if (mAnimationDuration == 0) {
                mAnimationDuration = context.getResources().getInteger(
                        android.R.integer.config_shortAnimTime);
            }

            return mAnimationDuration;
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends View> T findView(int id) {
        return (T) findViewById(id);
    }

}
