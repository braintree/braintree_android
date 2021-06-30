package com.braintreepayments.api;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.FragmentActivity;

import com.paypal.checkout.PayPalCheckout;
import com.paypal.checkout.approve.Approval;
import com.paypal.checkout.approve.OnApprove;
import com.paypal.checkout.cancel.OnCancel;
import com.paypal.checkout.config.CheckoutConfig;
import com.paypal.checkout.config.Environment;
import com.paypal.checkout.createorder.CreateOrder;
import com.paypal.checkout.createorder.CreateOrderActions;
import com.paypal.checkout.error.ErrorInfo;
import com.paypal.checkout.error.OnError;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

/**
 * Used to tokenize PayPal accounts using PayPal Native UI. For more information see the
 * <a href="https://developers.braintreepayments.com/guides/paypal/overview/android/">documentation</a>
 */
public class PayPalNativeClient {

    private final BraintreeClient braintreeClient;
    private final PayPalInternalClient internalPayPalClient;
    private final TokenizationClient tokenizationClient;
    private final PayPalClient payPalClient;

    public PayPalNativeClient(final BraintreeClient braintreeClient) {
        this(braintreeClient, new TokenizationClient(braintreeClient), new PayPalInternalClient(braintreeClient), new PayPalClient(braintreeClient));
    }

    @VisibleForTesting
    PayPalNativeClient(final BraintreeClient braintreeClient, final TokenizationClient tokenizationClient, final PayPalInternalClient internalPayPalClient, final PayPalClient payPalClient) {
        this.braintreeClient = braintreeClient;
        this.tokenizationClient = tokenizationClient;
        this.internalPayPalClient = internalPayPalClient;
        this.payPalClient = payPalClient;
    }

    public void tokenizePayPalAccount(final FragmentActivity activity, final PayPalRequest request, final PayPalNativeTokenizeCallback callback) {
        if (request instanceof PayPalNativeCheckoutRequest) {
            sendNativeSinglePaymentRequest(activity, (PayPalNativeCheckoutRequest) request, callback);
        } else if (request instanceof PayPalNativeVaultRequest) {
            sendNativeVaultRequest(activity, (PayPalNativeVaultRequest) request, callback);
        }
    }

    private void sendNativeSinglePaymentRequest(final FragmentActivity activity, final PayPalNativeCheckoutRequest request, final PayPalNativeTokenizeCallback callback) {
        braintreeClient.sendAnalyticsEvent("paypal.native.single-payment.selected");
        if (request.getShouldOfferPayLater()) {
            braintreeClient.sendAnalyticsEvent("paypal.native.single-payment.paylater.offered");
        }

        getBraintreeConfiguration(activity, request, callback);
    }

    private void sendNativeVaultRequest(final FragmentActivity activity, final PayPalNativeVaultRequest request, final PayPalNativeTokenizeCallback callback) {
        braintreeClient.sendAnalyticsEvent("paypal.native.billing-agreement.selected");
        if (request.getShouldOfferCredit()) {
            braintreeClient.sendAnalyticsEvent("paypal.native.billing-agreement.credit.offered");
        }

        getBraintreeConfiguration(activity, request, callback);
    }


    private void getBraintreeConfiguration(final FragmentActivity activity, final PayPalRequest payPalRequest, final PayPalNativeTokenizeCallback callback) {
        braintreeClient.getConfiguration(new ConfigurationCallback() {
            @Override
            public void onResult(@Nullable final Configuration configuration, @Nullable Exception error) {
                if (payPalConfigInvalid(configuration) || error != null) {
                    Exception configInvalidError = createPayPalNotEnabledError();
                    callback.onResult(null, configInvalidError);
                    return;
                }

                String payPalClientId = configuration.getPayPalClientId();
                if (payPalClientId == null) {
                    callback.onResult(null, new BraintreeException("Invalid PayPal Client ID"));
                    return;
                }

                startPayPalNativeCheckout(activity, configuration, payPalClientId, payPalRequest, callback);
            }
        });
    }

    private void startPayPalNativeCheckout(final FragmentActivity activity, final Configuration configuration, final String payPalClientId, final PayPalRequest payPalRequest, final PayPalNativeTokenizeCallback callback) {
        internalPayPalClient.sendRequest(activity, payPalRequest, new PayPalInternalClientCallback() {
            @Override
            public void onResult(final PayPalResponse payPalResponse, final Exception error) {
                if (payPalResponse != null) {
                    startNativeCheckout(activity, configuration, payPalClientId, payPalResponse, callback);
                } else {
                    callback.onResult(null, error);
                }
            }
        });
    }

    private void startNativeCheckout(final FragmentActivity activity, final Configuration configuration, final String clientId, final PayPalResponse payPalResponse, final PayPalNativeTokenizeCallback callback) {
        final String redirectUrl = getPayPalReturnUrl(activity);
        Environment environment = Environment.SANDBOX;
        if ("production".equalsIgnoreCase(configuration.getEnvironment())) {
            environment = Environment.LIVE;
        }

        CheckoutConfig checkoutConfig = new CheckoutConfig(
                activity.getApplication(),
                clientId,
                environment,
                redirectUrl
        );
        PayPalCheckout.setConfig(checkoutConfig);
        PayPalCheckout.start(
                new CreateOrder() {
                    @Override
                    public void create(final CreateOrderActions createOrderActions) {
                        final String pairingId = payPalResponse.getPairingId();
                        if (payPalResponse.isBillingAgreement()) {
                            createOrderActions.setBillingAgreementId(pairingId);
                        } else {
                            createOrderActions.set(pairingId);
                        }
                    }
                },
                new OnApprove() {
                    @Override
                    public void onApprove(final Approval approval) {
                        braintreeClient.sendAnalyticsEvent("paypal.native.approved");
                        handleNativeCheckoutApproval(approval, payPalResponse, callback);
                    }
                },
                new OnCancel() {
                    @Override
                    public void onCancel() {
                        braintreeClient.sendAnalyticsEvent("paypal.native.client_cancel");
                        callback.onResult(null, new BraintreeException("Canceled"));
                    }
                },
                new OnError() {
                    @Override
                    public void onError(final ErrorInfo errorInfo) {
                        braintreeClient.sendAnalyticsEvent("paypal.native.error");
                        callback.onResult(null, new BraintreeException(errorInfo.getReason()));
                    }
                }
        );
    }

    private void handleNativeCheckoutApproval(final Approval approval, final PayPalResponse payPalResponse, final PayPalNativeTokenizeCallback callback) {
        Uri deepLinkUri;
        String tokenKey;
        if (payPalResponse.isBillingAgreement()) {
            deepLinkUri = Uri.parse(String.format(
                    "%s://onetouch/v1/success?token=%s&ba_token=%s",
                    braintreeClient.getReturnUrlScheme(),
                    approval.getData().getOrderId(),
                    payPalResponse.getPairingId())); //The approval object will return the returnURL
            tokenKey = "ba_token";
        } else {
            deepLinkUri = Uri.parse(String.format(
                    "%s://onetouch/v1/success?paymentId=%s&token=%s&PayerID=%s",
                    braintreeClient.getReturnUrlScheme(),
                    approval.getData().getPaymentId(),
                    approval.getData().getOrderId(),
                    approval.getData().getPayerId()));
            tokenKey = "token";
        }
        try {
            JSONObject urlResponseData = parseUrlResponseData(deepLinkUri, payPalResponse.getSuccessUrl(), payPalResponse.getApprovalUrl(), tokenKey);
            PayPalAccount payPalAccount = new PayPalAccount();
            payPalAccount.setClientMetadataId(payPalResponse.getClientMetadataId());
            payPalAccount.setSource("paypal-browser"); //TODO: check for valid sources
            payPalAccount.setUrlResponseData(urlResponseData);

            String paymentType = payPalResponse.isBillingAgreement()
                    ? "billing-agreement" : "single-payment";
            payPalAccount.setPaymentType(paymentType);

            if (payPalResponse.getMerchantAccountId() != null) {
                payPalAccount.setMerchantAccountId(payPalResponse.getMerchantAccountId());
            }

            if (payPalResponse.getIntent() != null) {
                payPalAccount.setIntent(payPalResponse.getIntent());
            }
            tokenizationClient.tokenizeREST(payPalAccount, new TokenizeCallback() {
                @Override
                public void onResult(JSONObject tokenizationResponse, Exception exception) {
                    if (tokenizationResponse != null) {
                        try {
                            PayPalAccountNonce payPalAccountNonce = PayPalAccountNonce.fromJSON(tokenizationResponse);
                            if (payPalAccountNonce.getCreditFinancing() != null) {
                                braintreeClient.sendAnalyticsEvent("paypal.credit.accepted");
                            }
                            callback.onResult(payPalAccountNonce, null);
                        } catch (JSONException e) {
                            callback.onResult(null, e);
                        }
                    } else {
                        callback.onResult(null, exception);
                    }
                }
            });
        } catch (final JSONException | BraintreeException e) {
            callback.onResult(null, e);
        }

    }

    private String getPayPalReturnUrl(Context context) {
        if (context != null) {
            return String.format("%s://paypalpay", context.getPackageName().toLowerCase(Locale.ROOT)).replace("_", "");
        }
        return null;
    }

    private static Exception createPayPalNotEnabledError() {
        return new BraintreeException("PayPal is not enabled. " +
                "See https://developers.braintreepayments.com/guides/paypal/overview/android/ " +
                "for more information.");
    }

    private static boolean payPalConfigInvalid(Configuration configuration) {
        return (configuration == null || !configuration.isPayPalEnabled());
    }

    private JSONObject parseUrlResponseData(Uri uri, String successUrl, String approvalUrl, String tokenKey) throws JSONException, BraintreeException {
        String status = uri.getLastPathSegment();

        if (!Uri.parse(successUrl).getLastPathSegment().equals(status)) {
            throw new BraintreeException("User canceled.");
        }

        String requestXoToken = Uri.parse(approvalUrl).getQueryParameter(tokenKey);
        String responseXoToken = uri.getQueryParameter(tokenKey);
        if (responseXoToken != null && requestXoToken.equals(responseXoToken)) {
            JSONObject client = new JSONObject();
            client.put("environment", null);

            JSONObject urlResponseData = new JSONObject();
            urlResponseData.put("client", client);

            JSONObject response = new JSONObject();
            response.put("webURL", uri.toString());
            urlResponseData.put("response", response);

            urlResponseData.put("response_type", "web");

            return urlResponseData;
        } else {
            throw new BraintreeException("The response contained inconsistent data.");
        }
    }
}
