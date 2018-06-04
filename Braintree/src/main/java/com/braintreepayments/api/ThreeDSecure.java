package com.braintreepayments.api;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import com.braintreepayments.api.exceptions.BraintreeException;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.ConfigurationListener;
import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCallback;
import com.braintreepayments.api.internal.ManifestValidator;
import com.braintreepayments.api.models.BraintreeRequestCodes;
import com.braintreepayments.api.models.CardBuilder;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.api.models.ThreeDSecureAuthenticationResponse;
import com.braintreepayments.api.models.ThreeDSecureLookup;
import com.braintreepayments.api.models.ThreeDSecureRequest;
import com.braintreepayments.api.threedsecure.ThreeDSecureWebViewActivity;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 3D Secure is a protocol that enables cardholders and issuers to add a layer of security
 * to e-commerce transactions via password entry at checkout.
 *
 * One of the primary reasons to use 3D Secure is to benefit from a shift in liability from the
 * merchant to the issuer, which may result in interchange savings. Please read our online
 * <a href="https://developers.braintreepayments.com/guides/3d-secure/overview">documentation</a>
 * for a full explanation of 3D Secure.
 */
public class ThreeDSecure {

    protected static boolean sWebViewOverride = false;

    /**
     * The versioned path of the 3D Secure assets to use. Hosted by Braintree.
     */
    private static final String THREE_D_SECURE_ASSETS_PATH = "/mobile/three-d-secure-redirect/0.1.5";

    /**
     * Verification is associated with a transaction amount and your merchant account. To specify a
     * different merchant account (or, in turn, currency), you will need to specify the merchant
     * account id when <a href="https://developers.braintreepayments.com/android/sdk/overview/generate-client-token">
     *     generating a client token</a>
     *
     * During lookup the original payment method nonce is consumed and a new one is returned,
     * which points to the original payment method, as well as the 3D Secure verification.
     * Transactions created with this nonce will be 3D Secure, and benefit from the appropriate
     * liability shift if authentication is successful or fail with a 3D Secure failure.
     *
     * @param fragment the {@link BraintreeFragment} backing the http request. This fragment will
     *                  also be responsible for handling callbacks to it's listeners
     * @param cardBuilder The cardBuilder created from raw details. Will be tokenized before
     *                    the 3D Secure verification if performed.
     * @param amount The amount of the transaction in the current merchant account's currency
     */
    public static void performVerification(final BraintreeFragment fragment, final CardBuilder cardBuilder,
            final String amount) {
        TokenizationClient.tokenize(fragment, cardBuilder, new PaymentMethodNonceCallback() {
            @Override
            public void success(PaymentMethodNonce paymentMethodNonce) {
                performVerification(fragment, paymentMethodNonce.getNonce(), amount);
            }

            @Override
            public void failure(Exception exception) {
                fragment.postCallback(exception);
            }
        });
    }

    /**
     * Verification is associated with a transaction amount and your merchant account. To specify a
     * different merchant account (or, in turn, currency), you will need to specify the merchant
     * account id when <a href="https://developers.braintreepayments.com/android/sdk/overview/generate-client-token">
     *     generating a client token</a>
     *
     * During lookup the original payment method nonce is consumed and a new one is returned,
     * which points to the original payment method, as well as the 3D Secure verification.
     * Transactions created with this nonce will be 3D Secure, and benefit from the appropriate
     * liability shift if authentication is successful or fail with a 3D Secure failure.
     *
     * @param fragment the {@link BraintreeFragment} backing the http request. This fragment will
     *                  also be responsible for handling callbacks to it's listeners
     * @param nonce The nonce that represents a card to perform a 3D Secure verification against.
     * @param amount The amount of the transaction in the current merchant account's currency.
     */
    public static void performVerification(final BraintreeFragment fragment, final String nonce, final String amount) {
        ThreeDSecureRequest request = new ThreeDSecureRequest()
            .nonce(nonce)
            .amount(amount);

        performVerification(fragment, request);
    }

    /**
     * Verification is associated with a transaction amount and your merchant account. To specify a
     * different merchant account (or, in turn, currency), you will need to specify the merchant
     * account id when <a href="https://developers.braintreepayments.com/android/sdk/overview/generate-client-token">
     *     generating a client token</a>
     *
     * During lookup the original payment method nonce is consumed and a new one is returned,
     * which points to the original payment method, as well as the 3D Secure verification.
     * Transactions created with this nonce will be 3D Secure, and benefit from the appropriate
     * liability shift if authentication is successful or fail with a 3D Secure failure.
     *
     * @param fragment the {@link BraintreeFragment} backing the http request. This fragment will
     *                  also be responsible for handling callbacks to it's listeners
     * @param request the {@link ThreeDSecureRequest} with information used for authentication.
     */
    public static void performVerification(final BraintreeFragment fragment, final ThreeDSecureRequest request) {
        if (request.getAmount() == null || request.getNonce() == null) {
            fragment.postCallback(new InvalidArgumentException("The ThreeDSecureRequest nonce and amount cannot be null"));
            return;
        }

        fragment.waitForConfiguration(new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(Configuration configuration) {
                if (!configuration.isThreeDSecureEnabled()) {
                    fragment.postCallback(new BraintreeException("Three D Secure is not enabled in the control panel"));
                    return;
                }

                final boolean supportsBrowserSwitch = ManifestValidator.isUrlSchemeDeclaredInAndroidManifest(
                        fragment.getApplicationContext(), fragment.getReturnUrlScheme(), BraintreeBrowserSwitchActivity.class) && !sWebViewOverride;

                if (!supportsBrowserSwitch && !ManifestValidator.isActivityDeclaredInAndroidManifest(
                        fragment.getApplicationContext(), ThreeDSecureWebViewActivity.class)) {
                    fragment.postCallback(new BraintreeException("ThreeDSecureWebViewActivity is not declared in " +
                            "AndroidManifest.xml"));
                    return;
                }

                fragment.getHttpClient().post(TokenizationClient.versionedPath(
                        TokenizationClient.PAYMENT_METHOD_ENDPOINT + "/" + request.getNonce() +
                                "/three_d_secure/lookup"), request.build(), new HttpResponseCallback() {
                    @Override
                    public void success(String responseBody) {
                        try {
                            ThreeDSecureLookup threeDSecureLookup = ThreeDSecureLookup.fromJson(responseBody);
                            if (threeDSecureLookup.getAcsUrl() != null) {
                                if (supportsBrowserSwitch) {
                                    launchBrowserSwitch(fragment, threeDSecureLookup);
                                } else {
                                    launchWebView(fragment, threeDSecureLookup);
                                }
                            } else {
                                fragment.postCallback(threeDSecureLookup.getCardNonce());
                            }
                        } catch (JSONException e) {
                            fragment.postCallback(e);
                        }
                    }

                    @Override
                    public void failure(Exception exception) {
                        fragment.postCallback(exception);
                    }
                });
            }
        });
    }

    protected static void onActivityResult(BraintreeFragment fragment, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            ThreeDSecureAuthenticationResponse authenticationResponse;
            Uri resultUri = data.getData();
            if (resultUri != null) {
                authenticationResponse = ThreeDSecureAuthenticationResponse.fromJson(resultUri.getQueryParameter("auth_response"));
            } else {
                authenticationResponse =
                        data.getParcelableExtra(ThreeDSecureWebViewActivity.EXTRA_THREE_D_SECURE_RESULT);
            }
            if (authenticationResponse.isSuccess()) {
                fragment.postCallback(authenticationResponse.getCardNonce());
            } else if (authenticationResponse.getException() != null) {
                fragment.postCallback(new BraintreeException(authenticationResponse.getException()));
            } else {
                fragment.postCallback(new ErrorWithResponse(422, authenticationResponse.getErrors()));
            }
        }
    }

    private static void launchWebView(BraintreeFragment fragment, ThreeDSecureLookup threeDSecureLookup) {
        Intent intent = new Intent(fragment.getApplicationContext(), ThreeDSecureWebViewActivity.class)
                .putExtra(ThreeDSecureWebViewActivity.EXTRA_THREE_D_SECURE_LOOKUP, threeDSecureLookup);

        fragment.startActivityForResult(intent, BraintreeRequestCodes.THREE_D_SECURE);
    }

    private static void launchBrowserSwitch(BraintreeFragment fragment, ThreeDSecureLookup threeDSecureLookup) {
        String assetsBaseUrl = fragment.getConfiguration().getAssetsUrl() + THREE_D_SECURE_ASSETS_PATH;
        String returnUrl = String.format("%s/redirect.html?redirect_url=%s://x-callback-url/braintree/threedsecure?",
                assetsBaseUrl,
                fragment.getReturnUrlScheme());
        Uri redirectUri = Uri.parse(assetsBaseUrl + "/index.html")
                .buildUpon()
                .appendQueryParameter("AcsUrl", threeDSecureLookup.getAcsUrl())
                .appendQueryParameter("PaReq", threeDSecureLookup.getPareq())
                .appendQueryParameter("MD", threeDSecureLookup.getMd())
                .appendQueryParameter("TermUrl", threeDSecureLookup.getTermUrl())
                .appendQueryParameter("ReturnUrl", returnUrl)
                .build();

        fragment.browserSwitch(BraintreeRequestCodes.THREE_D_SECURE, redirectUri.toString());
    }
}
