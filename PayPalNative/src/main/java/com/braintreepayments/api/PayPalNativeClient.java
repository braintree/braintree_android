package com.braintreepayments.api;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.Nullable;
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

    private PayPalClient payPalClient;

    public PayPalNativeClient(BraintreeClient braintreeClient) {
        this.braintreeClient = braintreeClient;
        this.internalPayPalClient = new PayPalInternalClient(braintreeClient);
        this.tokenizationClient = new TokenizationClient(braintreeClient);
    }

    public void tokenizePayPalAccount(final FragmentActivity activity, final PayPalRequest request, final PayPalNativeTokenizeCallback callback) {
        if (request instanceof PayPalNativeCheckoutRequest) {
            sendNativeCheckoutRequest(activity, (PayPalNativeCheckoutRequest) request, callback);
        } else if (request instanceof PayPalNativeVaultRequest) {
            sendVaultRequest(activity, (PayPalNativeVaultRequest) request, callback);
        }
    }

    private void sendNativeCheckoutRequest(final FragmentActivity activity, final PayPalNativeCheckoutRequest request, final PayPalNativeTokenizeCallback callback) {
        //This is copied from PayPal module.
        braintreeClient.sendAnalyticsEvent("paypal.native.single-payment.selected");
        if (request.getShouldOfferPayLater()) {
            braintreeClient.sendAnalyticsEvent("paypal.native.single-payment.paylater.offered");
        }

        braintreeClient.getConfiguration(new ConfigurationCallback() {
            @Override
            public void onResult(Configuration configuration, Exception error) {

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

                // NOTE: the callback parameter is only necessary if PayPal Native XO needs
                // to callback an error before starting the native UI
                startPayPalNativeCheckout(activity, configuration, payPalClientId, request, callback);
            }
        });
    }

    private void sendVaultRequest(final FragmentActivity activity, final PayPalNativeVaultRequest payPalVaultRequest, final PayPalNativeTokenizeCallback callback) {
        //this one should default to the one we already have, once PayPalNative supports billing agreements, this should just default to native.
        payPalClient = new PayPalClient(braintreeClient);
        payPalClient.tokenizePayPalAccount(activity, payPalVaultRequest, new PayPalFlowStartedCallback() {
            @Override
            public void onResult(@Nullable Exception error) {
                callback.onResult(null, error);
            }
        });
    }

    private void startPayPalNativeCheckout(final FragmentActivity activity, final Configuration configuration, final String payPalClientId, final PayPalNativeCheckoutRequest payPalRequest, final PayPalNativeTokenizeCallback callback) {
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
                        createOrderActions.set(payPalResponse.getPairingId());
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
        Uri deepLinkUri = Uri.parse(String.format(
                "%s://onetouch/v1/success?paymentId=%s&token=%s&PayerID=%s",
                braintreeClient.getReturnUrlScheme(),
                approval.getData().getPaymentId(),
                approval.getData().getOrderId(),
                approval.getData().getPayerId()));
        try {
            JSONObject urlResponseData = parseUrlResponseData(deepLinkUri, payPalResponse.getSuccessUrl(), payPalResponse.getApprovalUrl(), "token");
            PayPalAccount payPalAccount = new PayPalAccount();
            payPalAccount.setClientMetadataId(payPalResponse.getClientMetadataId());
            payPalAccount.setSource("paypal-browser"); //TODO: check for valid sources
            payPalAccount.setUrlResponseData(urlResponseData);
            payPalAccount.setPaymentType("single-payment");

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

    public void onActivityResumed(final BrowserSwitchResult browserSwitchResult, final PayPalNativeOnActivityResumedCallback callback) {
        if (browserSwitchResult == null) {
            callback.onResult(null, new BraintreeException("BrowserSwitchResult cannot be null"));
            return;
        }
        JSONObject metadata = browserSwitchResult.getRequestMetadata();
        String paymentType = Json.optString(metadata, "payment-type", "unknown");
        boolean isVaultPayment = paymentType.equalsIgnoreCase("billing-agreement");

        if (isVaultPayment) {
            //it means it was a vault request
            payPalClient.onBrowserSwitchResult(browserSwitchResult, new PayPalBrowserSwitchResultCallback() {
                @Override
                public void onResult(@Nullable PayPalAccountNonce payPalAccountNonce, @Nullable Exception error) {
                    callback.onResult(payPalAccountNonce, error);
                }
            });
        }
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
        if (responseXoToken != null && TextUtils.equals(requestXoToken, responseXoToken)) {
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
