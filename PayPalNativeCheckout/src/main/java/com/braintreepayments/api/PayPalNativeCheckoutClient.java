package com.braintreepayments.api;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.paypal.checkout.PayPalCheckout;
import com.paypal.checkout.approve.ApprovalData;
import com.paypal.checkout.config.CheckoutConfig;
import com.paypal.checkout.config.Environment;
import com.paypal.checkout.config.SettingsConfig;
import com.paypal.checkout.config.UIConfig;

import com.paypal.pyplcheckout.common.instrumentation.PEnums;
import com.paypal.pyplcheckout.common.instrumentation.PLog;

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
     * @param fragment        a {@link Fragment
     * @param braintreeClient a {@link BraintreeClient}
     * @deprecated see {@link PayPalNativeCheckoutClient#PayPalNativeCheckoutClient(BraintreeClient)}
     * <p>
     * Create a new instance of {@link PayPalNativeCheckoutClient} from within a Fragment using a {@link BraintreeClient}.
     */
    @Deprecated
    public PayPalNativeCheckoutClient(@NonNull Fragment fragment, @NonNull BraintreeClient braintreeClient) {
        this(braintreeClient, new PayPalNativeCheckoutInternalClient(braintreeClient));
    }

    /**
     * Create a new instance of {@link PayPalNativeCheckoutClient} using a {@link BraintreeClient}.
     *
     * @param braintreeClient a {@link BraintreeClient}
     */
    public PayPalNativeCheckoutClient(@NonNull BraintreeClient braintreeClient) {
        this(braintreeClient, new PayPalNativeCheckoutInternalClient(braintreeClient));
    }

    @VisibleForTesting
    PayPalNativeCheckoutClient(BraintreeClient braintreeClient, PayPalNativeCheckoutInternalClient internalPayPalClient) {
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

    /**
     * @param activity      Android FragmentActivity
     * @param payPalRequest a {@link PayPalNativeRequest} used to customize the request.
     * @deprecated see {@link PayPalNativeCheckoutClient#launchNativeCheckout(FragmentActivity, PayPalNativeRequest)}
     * <p>
     * Tokenize a PayPal account for vault or checkout.
     * <p>
     * This method must be invoked on a {@link PayPalNativeCheckoutClient (Fragment, BraintreeClient)} or
     * {@link PayPalNativeCheckoutClient (FragmentActivity, BraintreeClient)} in order to receive results.
     */
    @Deprecated
    public void tokenizePayPalAccount(@NonNull final FragmentActivity activity, @NonNull final PayPalNativeRequest payPalRequest) throws Exception {
        braintreeClient.sendAnalyticsEvent("paypal-native.tokenize.started");
        // NEXT_MAJOR_VERSION: remove tokenizePayPalAccount method and refactor tests to center
        // around launchNativeCheckout in the future. Keeping the tests as they are for now allows
        // us to maintain test coverage across both the tokenizePayPalAccount and launchNativeCheckout methods
        boolean isCheckoutRequest = payPalRequest instanceof PayPalNativeCheckoutRequest;
        boolean isVaultRequest = payPalRequest instanceof PayPalNativeCheckoutVaultRequest;
        if (isCheckoutRequest || isVaultRequest) {
            launchNativeCheckout(activity, payPalRequest);
        } else {
            braintreeClient.sendAnalyticsEvent("paypal-native.tokenize.invalid-request.failed");
            String message = "Unsupported request type. Please use either a "
                    + "PayPalNativeCheckoutRequest or a PayPalNativeCheckoutVaultRequest.";
            throw new Exception(message);
        }
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
    public void launchNativeCheckout(@NonNull final FragmentActivity activity, @NonNull final PayPalNativeRequest payPalRequest) {
        braintreeClient.sendAnalyticsEvent("paypal-native.tokenize.started");
        if (payPalRequest instanceof PayPalNativeCheckoutRequest) {
            sendCheckoutRequest(activity, (PayPalNativeCheckoutRequest) payPalRequest);
            braintreeClient.sendAnalyticsEvent("paypal-native.tokenize.succeeded");
        } else if (payPalRequest instanceof PayPalNativeCheckoutVaultRequest) {
            sendVaultRequest(activity, (PayPalNativeCheckoutVaultRequest) payPalRequest);
            braintreeClient.sendAnalyticsEvent("paypal-native.tokenize.succeeded");
        } else if (listener != null) {
            braintreeClient.sendAnalyticsEvent("paypal-native.tokenize.invalid-request.failed");
            String message = "Unsupported request type. Please use either a "
                    + "PayPalNativeCheckoutRequest or a PayPalNativeCheckoutVaultRequest.";
            listener.onPayPalFailure(new BraintreeException(message));
        }
    }

    private void sendCheckoutRequest(final FragmentActivity activity, final PayPalNativeCheckoutRequest payPalCheckoutRequest) {
        braintreeClient.sendAnalyticsEvent("paypal-native.single-payment.selected");
        if (payPalCheckoutRequest.getShouldOfferPayLater()) {
            braintreeClient.sendAnalyticsEvent("paypal-native.single-payment.paylater.offered");
        }

        braintreeClient.getConfiguration((configuration, error) -> {
            sendPayPalRequest(activity, payPalCheckoutRequest, configuration);
        });
    }

    private void sendVaultRequest(final FragmentActivity activity, final PayPalNativeCheckoutVaultRequest payPalVaultRequest) {
        braintreeClient.sendAnalyticsEvent("paypal-native.billing-agreement.selected");
        if (payPalVaultRequest.getShouldOfferCredit()) {
            braintreeClient.sendAnalyticsEvent("paypal-native.billing-agreement.credit.offered");
        }

        braintreeClient.getConfiguration((configuration, error) -> {
            sendPayPalRequest(activity, payPalVaultRequest, configuration);
        });
    }

    private void sendPayPalRequest(
            final FragmentActivity activity,
            final PayPalNativeRequest payPalRequest,
            final Configuration configuration
    ) {
        internalPayPalClient.sendRequest(activity, payPalRequest, (payPalResponse, error) -> {
            if (payPalResponse != null) {
                String analyticsPrefix = payPalRequest instanceof PayPalNativeCheckoutVaultRequest ? "billing-agreement" : "single-payment";
                braintreeClient.sendAnalyticsEvent(String.format("paypal-native.%s.started", analyticsPrefix));

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
                                null,
                                null,
                                null,
                                new SettingsConfig(),
                                new UIConfig(
                                        false
                                )
                        )
                );

                String infoMessage = "BrainTree";

                PLog.transition(
                    PEnums.TransitionName.BRAINTREE_ROUTING,
                    PEnums.Outcome.THIRD_PARTY,
                    PEnums.EventCode.E233,
                    PEnums.StateName.BRAINTREE,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    infoMessage
                );

                registerCallbacks(configuration, payPalRequest, payPalResponse);

                PayPalCheckout.startCheckout(createOrderActions -> {
                    if (payPalRequest instanceof PayPalNativeCheckoutRequest) {
                        createOrderActions.set(payPalResponse.getPairingId());
                        braintreeClient.sendAnalyticsEvent("paypal-native.single-payment.succeeded");
                    } else if (payPalRequest instanceof PayPalNativeCheckoutVaultRequest) {
                        createOrderActions.setBillingAgreementId(payPalResponse.getPairingId());
                        braintreeClient.sendAnalyticsEvent("paypal-native.billing-agreement.succeeded");
                    }
                });
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
                    braintreeClient.sendAnalyticsEvent("paypal-native.on-approve.started");
                    PayPalNativeCheckoutAccount payPalAccount = setupAccount(payPalRequest, approval.getData());
                    internalPayPalClient.tokenize(payPalAccount, (payPalAccountNonce, error) -> {
                        if (payPalAccountNonce != null) {
                            braintreeClient.sendAnalyticsEvent("paypal-native.on-approve.succeeded");
                            listener.onPayPalSuccess(payPalAccountNonce);
                        } else {
                            braintreeClient.sendAnalyticsEvent("paypal-native.on-approve.failed");
                            listener.onPayPalFailure(new Exception("PaypalAccountNonce is null"));
                        }
                    });
                },
                null,
                () -> {
                    braintreeClient.sendAnalyticsEvent("paypal-native.canceled");
                    listener.onPayPalFailure(new Exception("User has canceled"));
                },
                errorInfo -> {
                    braintreeClient.sendAnalyticsEvent("paypal-native.on-error.failed");
                    listener.onPayPalFailure(new Exception(errorInfo.getError().getMessage()));
                }
        );
    }

    @VisibleForTesting
    PayPalNativeCheckoutAccount setupAccount(
        final PayPalNativeRequest payPalRequest,
        final ApprovalData approvalData
    ) {
        PayPalNativeCheckoutAccount payPalAccount = new PayPalNativeCheckoutAccount();

        String merchantAccountId = payPalRequest.getMerchantAccountId();
        String paymentType = payPalRequest instanceof PayPalNativeCheckoutVaultRequest ? "billing-agreement" : "single-payment";
        String riskId = null;
        if (payPalRequest.getRiskCorrelationId() != null) {
            riskId = payPalRequest.getRiskCorrelationId();
        } else {
            if (approvalData.getCorrelationIds() != null) {
                riskId = approvalData.getCorrelationIds().getRiskCorrelationId().getId();
            }
        }
        payPalAccount.setClientMetadataId(riskId);
        payPalAccount.setSource("paypal-browser");
        payPalAccount.setPaymentType(paymentType);
        if (approvalData.getCart() != null) {
            payPalAccount.setIntent(approvalData.getCart().getIntent());
        }

        try {
            JSONObject client = new JSONObject();
            client.put("platform", "android");
            client.put("product_name", "PayPal");
            client.put("paypal_sdk_version", "version");
            payPalAccount.setClient(client);

            JSONObject urlResponseData = new JSONObject();
            JSONObject response = new JSONObject();

            if (approvalData.getCart() != null && approvalData.getCart().getReturnUrl() != null) {
                response.put("webURL", approvalData.getCart().getReturnUrl().getHref());
            }
            urlResponseData.put("response", response);
            urlResponseData.put("response_type", "web");
            payPalAccount.setUrlResponseData(response);
        } catch (JSONException jsonException) {
            listener.onPayPalFailure(jsonException);
        }

        if (merchantAccountId != null) {
            payPalAccount.setMerchantAccountId(merchantAccountId);
        }

        return payPalAccount;
    }
}
