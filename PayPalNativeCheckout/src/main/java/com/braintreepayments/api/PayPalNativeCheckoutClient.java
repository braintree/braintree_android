package com.braintreepayments.api;

import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;

import com.paypal.checkout.PayPalCheckout;
import com.paypal.checkout.config.CheckoutConfig;
import com.paypal.checkout.config.Environment;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Used to tokenize PayPal accounts. For more information see the
 * <a href="https://developer.paypal.com/braintree/docs/guides/paypal/overview/android/v4">documentation</a>
 */
public class PayPalNativeCheckoutClient {

    private final BraintreeClient braintreeClient;
    private final PayPalNativeCheckoutInternalClient internalPayPalClient;

    private PayPalNativeCheckoutListener listener;

    /**
     * Create a new instance of {@link PayPalNativeCheckoutClient} from within a Fragment using a {@link BraintreeClient}.
     *
     * @param fragment        a {@link Fragment
     * @param braintreeClient a {@link BraintreeClient}
     */
    public PayPalNativeCheckoutClient(@NonNull Fragment fragment, @NonNull BraintreeClient braintreeClient) {
        this(fragment.getActivity(), fragment.getLifecycle(), braintreeClient, new PayPalNativeCheckoutInternalClient(braintreeClient));
    }

    @VisibleForTesting
    PayPalNativeCheckoutClient(FragmentActivity activity, Lifecycle lifecycle, BraintreeClient braintreeClient, PayPalNativeCheckoutInternalClient internalPayPalClient) {
        this.braintreeClient = braintreeClient;
        this.internalPayPalClient = internalPayPalClient;
    }

    /**
     * Add a {@link PayPalNativeCheckoutListener} to your client to receive results or errors from the PayPal flow.
     * This method must be invoked on a {@link PayPalNativeCheckoutClient (Fragment, BraintreeClient)} or
     * {@link PayPalNativeCheckoutClient (FragmentActivity, BraintreeClient)} in order to receive results.
     *
     * @param listener a {@link PayPalNativeCheckoutListener}
     */
    public void setListener(PayPalNativeCheckoutListener listener) {
        this.listener = listener;
    }

    private static boolean payPalConfigInvalid(Configuration configuration) {
        return (configuration == null || !configuration.isPayPalEnabled());
    }

    private boolean browserSwitchNotPossible(FragmentActivity activity) {
        return !braintreeClient.canPerformBrowserSwitch(activity, BraintreeRequestCodes.PAYPAL);
    }

    //TODO NEXT_MAJOR_VERSION
    //Change link to docs to https://developer.paypal.com/braintree/docs/guides/paypal/overview/android/v4
    private static Exception createPayPalError() {
        return new BraintreeException("PayPal is not enabled. " +
                "See https://developers.braintreepayments.com/guides/paypal/overview/android/ " +
                "for more information.");
    }

    //TODO NEXT_MAJOR_VERSION
    //Change link to docs to https://developer.paypal.com/braintree/docs/guides/client-sdk/setup/android/v4#browser-switch-setup
    private static Exception createBrowserSwitchError() {
        return new BraintreeException("AndroidManifest.xml is incorrectly configured or another app " +
                "defines the same browser switch url as this app. See " +
                "https://developers.braintreepayments.com/guides/client-sdk/android/#browser-switch " +
                "for the correct configuration");
    }

    /**
     * Tokenize a PayPal account for vault or checkout.
     * <p>
     * This method must be invoked on a {@link PayPalNativeCheckoutClient (Fragment, BraintreeClient)} or
     * {@link PayPalNativeCheckoutClient (FragmentActivity, BraintreeClient)} in order to receive results.
     *
     * @param activity      Android FragmentActivity
     * @param payPalRequest a {@link PayPalNativeRequest} used to customize the request.
     */
    public void tokenizePayPalAccount(@NonNull final FragmentActivity activity, @NonNull final PayPalNativeRequest payPalRequest) {
        tokenizePayPalAccount(activity, payPalRequest, error -> {
            if (error != null) {
                listener.onPayPalFailure(error);
            }
        });
    }

    /**
     * Tokenize a PayPal account for vault or checkout.
     * <p>
     * Deprecated. Use {@link PayPalNativeCheckoutClient#tokenizePayPalAccount(FragmentActivity, PayPalNativeRequest)}
     *
     * @param activity      Android FragmentActivity
     * @param payPalRequest a {@link PayPalNativeRequest} used to customize the request.
     * @param callback      {@link PayPalNativeCheckoutFlowStartedCallback}
     */
    @Deprecated
    public void tokenizePayPalAccount(@NonNull final FragmentActivity activity, @NonNull final PayPalNativeRequest payPalRequest, @NonNull final PayPalNativeCheckoutFlowStartedCallback callback) {
        if (payPalRequest instanceof PayPalNativeCheckoutRequest) {
            sendCheckoutRequest(activity, (PayPalNativeCheckoutRequest) payPalRequest, callback);
        } else if (payPalRequest instanceof PayPalNativeCheckoutVaultRequest) {
            sendVaultRequest(activity, (PayPalNativeCheckoutVaultRequest) payPalRequest, callback);
        }
    }

    /**
     * @param activity              Android FragmentActivity
     * @param payPalCheckoutRequest a {@link PayPalNativeCheckoutRequest} used to customize the request.
     * @param callback              {@link PayPalNativeCheckoutFlowStartedCallback}
     * @deprecated Use {@link PayPalNativeCheckoutClient#tokenizePayPalAccount(FragmentActivity, PayPalNativeRequest, PayPalNativeCheckoutFlowStartedCallback)} instead.
     * Starts the One-Time Payment (Checkout) flow for PayPal.
     */
    @Deprecated
    public void requestOneTimePayment(@NonNull final FragmentActivity activity, @NonNull final PayPalNativeCheckoutRequest payPalCheckoutRequest, @NonNull final PayPalNativeCheckoutFlowStartedCallback callback) {
        tokenizePayPalAccount(activity, payPalCheckoutRequest, callback);
    }

    /**
     * @param activity           Android FragmentActivity
     * @param payPalVaultRequest a {@link PayPalNativeCheckoutVaultRequest} used to customize the request.
     * @param callback           {@link PayPalNativeCheckoutFlowStartedCallback}
     * @deprecated Use {@link PayPalNativeCheckoutClient#tokenizePayPalAccount(FragmentActivity, PayPalNativeRequest, PayPalNativeCheckoutFlowStartedCallback)} instead.
     * Starts the Billing Agreement (Vault) flow for PayPal.
     */
    @Deprecated
    public void requestBillingAgreement(@NonNull final FragmentActivity activity, @NonNull final PayPalNativeCheckoutVaultRequest payPalVaultRequest, @NonNull final PayPalNativeCheckoutFlowStartedCallback callback) {
        tokenizePayPalAccount(activity, payPalVaultRequest, callback);
    }

    private void sendCheckoutRequest(final FragmentActivity activity, final PayPalNativeCheckoutRequest payPalCheckoutRequest, final PayPalNativeCheckoutFlowStartedCallback callback) {
        braintreeClient.sendAnalyticsEvent("paypal.single-payment.selected");
        if (payPalCheckoutRequest.getShouldOfferPayLater()) {
            braintreeClient.sendAnalyticsEvent("paypal.single-payment.paylater.offered");
        }

        braintreeClient.getConfiguration((configuration, error) -> {

            if (payPalConfigInvalid(configuration)) {
                Exception configInvalidError = createPayPalError();
                callback.onResult(configInvalidError);
                return;
            }

            if (browserSwitchNotPossible(activity)) {
                braintreeClient.sendAnalyticsEvent("paypal.invalid-manifest");
                Exception manifestInvalidError = createBrowserSwitchError();
                callback.onResult(manifestInvalidError);
                return;
            }
            sendPayPalRequest(
                activity,
                payPalCheckoutRequest,
                configuration,
                callback
            );
        });

    }

    private void sendVaultRequest(final FragmentActivity activity, final PayPalNativeCheckoutVaultRequest payPalVaultRequest, final PayPalNativeCheckoutFlowStartedCallback callback) {
        braintreeClient.sendAnalyticsEvent("paypal.billing-agreement.selected");
        if (payPalVaultRequest.getShouldOfferCredit()) {
            braintreeClient.sendAnalyticsEvent("paypal.billing-agreement.credit.offered");
        }

        braintreeClient.getConfiguration((configuration, error) -> {
            if (payPalConfigInvalid(configuration)) {
                Exception configInvalidError = createPayPalError();
                callback.onResult(configInvalidError);
                return;
            }

            if (browserSwitchNotPossible(activity)) {
                braintreeClient.sendAnalyticsEvent("paypal.invalid-manifest");
                Exception manifestInvalidError = createBrowserSwitchError();
                callback.onResult(manifestInvalidError);
                return;
            }

            sendPayPalRequest(
                activity,
                payPalVaultRequest,
                configuration,
                callback
            );
        });
    }

    private void sendPayPalRequest(
        final FragmentActivity activity,
        final PayPalNativeRequest payPalRequest,
        final Configuration configuration,
        final PayPalNativeCheckoutFlowStartedCallback callback
    ) {
        internalPayPalClient.sendRequest(activity, payPalRequest, (payPalResponse, error) -> {
            if (payPalResponse != null) {
                String analyticsPrefix = getAnalyticsEventPrefix(payPalRequest);
                braintreeClient.sendAnalyticsEvent(String.format("%s.app-switch.started", analyticsPrefix));

                Environment environment;
                if ("sandbox".equals(configuration.getEnvironment())) {
                    environment = Environment.SANDBOX;
                } else {
                    environment = Environment.LIVE;
                }

                // Start PayPalCheckout flow
                PayPalCheckout.setConfig(
                    new CheckoutConfig(
                        activity.getApplication(),
                        configuration.getPayPalClientId(),
                        environment,
                        payPalRequest.getReturnUrl()
                    )
                );

                registerCallbacks(configuration, payPalRequest, payPalResponse);

                PayPalCheckout.startCheckout(createOrderActions -> {
                    if (payPalRequest instanceof PayPalNativeCheckoutRequest) {
                        createOrderActions.set(payPalResponse.getPairingId());
                    } else if (payPalRequest instanceof PayPalNativeCheckoutVaultRequest) {
                        createOrderActions.setBillingAgreementId(payPalResponse.getPairingId());
                    } else {
                        callback.onResult(new Exception("Unsupported action type"));
                    }
                });
                callback.onResult(null);
            } else {
                callback.onResult(error);
            }
        });
    }

    private void registerCallbacks(
        final Configuration configuration,
        final PayPalNativeRequest payPalRequest,
        final PayPalNativeCheckoutResponse payPalResponse
    ) {
        PayPalCheckout.registerCallbacks(
                approval -> {
                    PayPalNativeCheckoutAccount payPalAccount = setupAccount(configuration, payPalRequest, payPalResponse);
                    internalPayPalClient.tokenize(payPalAccount, (payPalAccountNonce, error) -> {
                        if (payPalAccountNonce != null) {
                            listener.onPayPalSuccess(payPalAccountNonce);
                        } else {
                            listener.onPayPalFailure(new Exception("PaypalAccountNonce is null"));
                        }
                    });
                },
                null,
                () -> listener.onPayPalFailure(new Exception("User has canceled")),
                errorInfo -> listener.onPayPalFailure(new Exception(errorInfo.getError().getMessage()))
        );
    }

    private PayPalNativeCheckoutAccount setupAccount(
        final Configuration configuration,
        final PayPalNativeRequest payPalRequest,
        final PayPalNativeCheckoutResponse payPalResponse
    ) {
        PayPalNativeCheckoutAccount payPalAccount = new PayPalNativeCheckoutAccount();

        String merchantAccountId = payPalRequest.getMerchantAccountId();
        String paymentType = payPalRequest instanceof PayPalNativeCheckoutVaultRequest ? "billing-agreement" : "single-payment";
        payPalAccount.setClientMetadataId(configuration.getPayPalClientId());
        payPalAccount.setIntent(payPalResponse.getIntent());
        payPalAccount.setSource("paypal-browser");
        payPalAccount.setPaymentType(paymentType);

        if (merchantAccountId != null) {
            payPalAccount.setMerchantAccountId(merchantAccountId);
        }

        return payPalAccount;
    }

    private static String getAnalyticsEventPrefix(PayPalNativeRequest request) {
        return request instanceof PayPalNativeCheckoutVaultRequest ? "paypal.billing-agreement" : "paypal.single-payment";
    }
}
