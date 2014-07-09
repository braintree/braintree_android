package com.braintreepayments.api;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.braintreepayments.api.ClientToken.PayPal;
import com.braintreepayments.api.exceptions.ConfigurationException;
import com.braintreepayments.api.models.PayPalAccountBuilder;
import com.paypal.android.sdk.payments.PayPalAuthorization;
import com.paypal.android.sdk.payments.PayPalConfiguration;
import com.paypal.android.sdk.payments.PayPalFuturePaymentActivity;
import com.paypal.android.sdk.payments.PayPalService;

import org.json.JSONException;

public class PayPalHelper {
    private PayPalHelper() {
        throw new IllegalStateException("Non-instantiable class.");
    }

    protected static void startPaypal(Context context, ClientToken clientToken) {
        context.startService(buildPayPalServiceIntent(context, clientToken));
    }

    protected static void launchPayPal(Activity activity, int requestCode) {
        activity.startActivityForResult(new Intent(activity, PayPalFuturePaymentActivity.class),
                requestCode);
    }

    protected static void stopPaypalService(Context context) {
        context.stopService(new Intent(context, PayPalService.class));
    }

    public static PayPalAccountBuilder getBuilderFromActivity(int resultCode, Intent data) throws ConfigurationException {
        if (resultCode == Activity.RESULT_OK) {
            PayPalAuthorization authorization = data.getParcelableExtra(
                    PayPalFuturePaymentActivity.EXTRA_RESULT_AUTHORIZATION);
            PayPalAccountBuilder payPalAccountBuilder = new PayPalAccountBuilder()
                    .authorizationCode(authorization.getAuthorizationCode());

            try {
                String email = authorization.toJSONObject()
                        .getJSONObject("user")
                        .getString("display_string");
                payPalAccountBuilder.email(email);
            } catch (JSONException e) {
                // If email was not included, don't set it
            }

            return payPalAccountBuilder;
        } else if (resultCode == PayPalFuturePaymentActivity.RESULT_EXTRAS_INVALID) {
            throw new ConfigurationException();
        }

        return null;
    }

    protected static PayPalConfiguration buildPayPalConfiguration(ClientToken clientToken) {
        PayPalConfiguration paypalConfiguration = new PayPalConfiguration();

        PayPal paypal = clientToken.getPayPal();

        if (paypal.getEnvironment().equals("live")) {
            paypalConfiguration.environment(PayPalConfiguration.ENVIRONMENT_PRODUCTION);
        } else if (paypal.getEnvironment().equals("offline")) {
            paypalConfiguration.environment(PayPalConfiguration.ENVIRONMENT_NO_NETWORK);
        } else {
            paypalConfiguration.environment(paypal.getEnvironment());
        }

        return paypalConfiguration
                .clientId(paypal.getClientId())
                .merchantName(paypal.getDisplayName())
                .merchantUserAgreementUri(Uri.parse(paypal.getUserAgreementUrl()))
                .merchantPrivacyPolicyUri(Uri.parse(paypal.getPrivacyUrl()));
    }

    protected static Intent buildPayPalServiceIntent(Context context, ClientToken clientToken) {
        Intent intent = new Intent(context, PayPalService.class);
        intent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, buildPayPalConfiguration(
                clientToken));

        if(clientToken.getPayPal().getEnvironment().equals("custom")) {
            intent.putExtra("com.paypal.android.sdk.baseEnvironmentUrl",
                    clientToken.getPayPal().getDirectBaseUrl());
            intent.putExtra("com.paypal.android.sdk.enableStageSsl", false);
        }

        return intent;
    }
}
