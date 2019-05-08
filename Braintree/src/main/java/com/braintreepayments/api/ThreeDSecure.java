package com.braintreepayments.api;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.braintreepayments.api.exceptions.BraintreeException;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.ConfigurationListener;
import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCallback;
import com.braintreepayments.api.interfaces.ThreeDSecureLookupListener;
import com.braintreepayments.api.internal.ClassHelper;
import com.braintreepayments.api.internal.ManifestValidator;
import com.braintreepayments.api.models.BraintreeRequestCodes;
import com.braintreepayments.api.models.CardBuilder;
import com.braintreepayments.api.models.CardNonce;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.api.models.ThreeDSecureAuthenticationResponse;
import com.braintreepayments.api.models.ThreeDSecureInfo;
import com.braintreepayments.api.models.ThreeDSecureLookup;
import com.braintreepayments.api.models.ThreeDSecureRequest;
import com.cardinalcommerce.cardinalmobilesdk.Cardinal;
import com.cardinalcommerce.cardinalmobilesdk.models.response.ValidateResponse;
import com.cardinalcommerce.cardinalmobilesdk.services.CardinalInitService;
import com.cardinalcommerce.shared.models.parameters.CardinalConfigurationParameters;
import com.cardinalcommerce.shared.models.parameters.CardinalEnvironment;

import org.json.JSONException;
import org.json.JSONObject;

import static androidx.appcompat.app.AppCompatActivity.RESULT_OK;
import static com.braintreepayments.api.models.BraintreeRequestCodes.THREE_D_SECURE;

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
    private static String sDFReferenceId;

    /**
     * The versioned path of the 3D Secure assets to use. Hosted by Braintree.
     */
    private static final String THREE_D_SECURE_ASSETS_PATH = "/mobile/three-d-secure-redirect/0.1.5";

    /**
     * @deprecated Use {{@link #performVerification(BraintreeFragment, CardBuilder, ThreeDSecureRequest)}} for 3DS 2.0.
     * <p>
     * The amount can be provided via {@link ThreeDSecureRequest#amount(String)}.
     */
    @Deprecated
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
     * @deprecated Use {{@link #performVerification(BraintreeFragment, ThreeDSecureRequest)}} for 3DS 2.0.
     * <p>
     * The nonce can be provided via {@link ThreeDSecureRequest#nonce(String)}.
     * <p>
     * The amount can be provided via {@link ThreeDSecureRequest#amount(String)}.
     */
    @Deprecated
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
     * @param cardBuilder The cardBuilder created from raw details. Will be tokenized before
     *                    the 3D Secure verification if performed.
     * @param request the {@link ThreeDSecureRequest} with information used for authentication.
     *                Note that the nonce will be replaced with the nonce generated from the
     *                cardBuilder.
     */
    public static void performVerification(final BraintreeFragment fragment,
                                           final CardBuilder cardBuilder,
                                           final ThreeDSecureRequest request) {
        if (request.getAmount() == null) {
            fragment.postCallback(new InvalidArgumentException("The ThreeDSecureRequest amount cannot be null"));
            return;
        }

        TokenizationClient.tokenize(fragment, cardBuilder, new PaymentMethodNonceCallback() {
            @Override
            public void success(PaymentMethodNonce paymentMethodNonce) {
                request.nonce(paymentMethodNonce.getNonce());

                performVerification(fragment, request);
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
     * @param request the {@link ThreeDSecureRequest} with information used for authentication.
     */
    public static void performVerification(final BraintreeFragment fragment, final ThreeDSecureRequest request) {
        ThreeDSecureLookupListener lookupListener = new ThreeDSecureLookupListener() {
            @Override
            public void onLookupComplete(ThreeDSecureRequest request, ThreeDSecureLookup lookup) {
                fragment.sendAnalyticsEvent("three-d-secure.perform-verification.default-lookup-listener");

                continuePerformVerification(fragment, request, lookup);
            }
        };

        performVerification(fragment, request, lookupListener);
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
    public static void performVerification(final BraintreeFragment fragment,
                                           final ThreeDSecureRequest request,
                                           final ThreeDSecureLookupListener lookupListener) {
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
                        fragment.getApplicationContext(), fragment.getReturnUrlScheme(), BraintreeBrowserSwitchActivity.class);

                if (!supportsBrowserSwitch) {
                    fragment.sendAnalyticsEvent("three-d-secure.invalid-manifest");
                    fragment.postCallback(new BraintreeException("BraintreeBrowserSwitchActivity missing, " +
                            "incorrectly configured in AndroidManifest.xml or another app defines the same browser " +
                            "switch url as this app. See " +
                            "https://developers.braintreepayments.com/guides/client-sdk/android/v2#browser-switch " +
                            "for the correct configuration"));
                    return;
                }

                fragment.sendAnalyticsEvent("three-d-secure.initialized");

                fragment.getHttpClient().post(TokenizationClient.versionedPath(
                        TokenizationClient.PAYMENT_METHOD_ENDPOINT + "/" + request.getNonce() +
                                "/three_d_secure/lookup"), request.build(sDFReferenceId), new HttpResponseCallback() {
                    @Override
                    public void success(String responseBody) {
                        try {
                            final ThreeDSecureLookup threeDSecureLookup = ThreeDSecureLookup.fromJson(responseBody);

                            lookupListener.onLookupComplete(request, threeDSecureLookup);
                        } catch (JSONException exception) {
                            fragment.postCallback(exception);
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

    /**
     * Continues the 3DS verification. Should be called from {@link ThreeDSecureLookupListener#onLookupComplete(ThreeDSecureRequest, ThreeDSecureLookup)}
     *
     * @param fragment the {@link BraintreeFragment} backing the http request. This fragment will
     *                  also be responsible for handling callbacks to it's listeners
     * @param request the {@link ThreeDSecureRequest} with information used for authentication.
     * @param threeDSecureLookup the {@link ThreeDSecureLookup} returned for this request.
     *                           Contains information about the 3DS verification request that will
     *                           be invoked in this method.
     */
    public static void continuePerformVerification(final BraintreeFragment fragment,
                                                   final ThreeDSecureRequest request,
                                                   final ThreeDSecureLookup threeDSecureLookup) {
        boolean showChallenge = threeDSecureLookup.getAcsUrl() != null;
        String threeDSecureVersion = threeDSecureLookup.getThreeDSecureVersion();

        fragment.sendAnalyticsEvent(String.format("three-d-secure.verification-flow.challenge-presented.%b", showChallenge));
        fragment.sendAnalyticsEvent(String.format("three-d-secure.verification-flow.3ds-version.%s", threeDSecureVersion));

        if (!showChallenge) {
            completeVerificationFlowWithNoncePayload(fragment, threeDSecureLookup.getCardNonce());
            return;
        }

        if (!threeDSecureVersion.startsWith("2.")) {
            launchBrowserSwitch(fragment, threeDSecureLookup);
            return;
        }

        performCardinalAuthentication(fragment, threeDSecureLookup);
    }

    protected static void performCardinalAuthentication(final BraintreeFragment fragment, final ThreeDSecureLookup threeDSecureLookup) {
        fragment.sendAnalyticsEvent("three-d-secure.verification-flow.started");

        Bundle extras = new Bundle();
        extras.putParcelable(ThreeDSecureActivity.EXTRA_THREE_D_SECURE_LOOKUP, threeDSecureLookup);

        Intent intent = new Intent(fragment.getApplicationContext(), ThreeDSecureActivity.class);
        intent.putExtras(extras);

        fragment.startActivityForResult(intent, BraintreeRequestCodes.THREE_D_SECURE);
    }

    protected static void authenticateCardinalJWT(final BraintreeFragment fragment, final ThreeDSecureLookup threeDSecureLookup, final String cardinalJWT) {
        final CardNonce cardNonce = threeDSecureLookup.getCardNonce();

        fragment.sendAnalyticsEvent("three-d-secure.verification-flow.upgrade-payment-method.started");

        final String nonce = cardNonce.getNonce();
        JSONObject body = new JSONObject();
        try {
            body.put("jwt", cardinalJWT);
            body.put("paymentMethodNonce", nonce);
        } catch (JSONException ignored) {}

        fragment.getHttpClient().post(TokenizationClient.versionedPath(
                TokenizationClient.PAYMENT_METHOD_ENDPOINT + "/" + nonce +
                        "/three_d_secure/authenticate_from_jwt"), body.toString(), new HttpResponseCallback() {
            @Override
            public void success(String responseBody) {
                ThreeDSecureAuthenticationResponse authenticationResponse = ThreeDSecureAuthenticationResponse.fromJson(responseBody);
                ThreeDSecureInfo authenticationResponseThreeDSecureInfo = authenticationResponse.getThreeDSecureInfo();

                if (authenticationResponse.isSuccess()) {
                    fragment.sendAnalyticsEvent("three-d-secure.verification-flow.upgrade-payment-method.succeeded");

                    completeVerificationFlowWithNoncePayload(fragment, authenticationResponse.getCardNonce());
                } else if (authenticationResponseThreeDSecureInfo != null && authenticationResponseThreeDSecureInfo.isLiabilityShiftPossible()) {
                    fragment.sendAnalyticsEvent("three-d-secure.verification-flow.upgrade-payment-method.liability-shift-possible");

                    completeVerificationFlowWithNoncePayload(fragment, cardNonce);
                } else {
                    fragment.sendAnalyticsEvent("three-d-secure.verification-flow.upgrade-payment-method.errored");

                    // TODO: This isn't a GraphQL request, but the response uses GraphQL style errors. How do we want to parse them?
                    fragment.postCallback(ErrorWithResponse.fromGraphQLJson(authenticationResponse.getErrors()));
                }
            }

            @Override
            public void failure(Exception exception) {
                fragment.sendAnalyticsEvent("three-d-secure.verification-flow.upgrade-payment-method.errored");

                fragment.postCallback(exception);
            }
        });
    }

    protected static void onActivityResult(BraintreeFragment fragment, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            return;

        }
        Uri resultUri = data.getData();

        if (resultUri != null) {
            // V1 flow
            ThreeDSecureAuthenticationResponse authenticationResponse = ThreeDSecureAuthenticationResponse
                    .fromJson(resultUri.getQueryParameter("auth_response"));

            if (authenticationResponse.isSuccess()) {
                completeVerificationFlowWithNoncePayload(fragment, authenticationResponse.getCardNonce());
            } else if (authenticationResponse.getException() != null) {
                fragment.postCallback(new BraintreeException(authenticationResponse.getException()));
            } else {
                fragment.postCallback(new ErrorWithResponse(422, authenticationResponse.getErrors()));
            }
        } else {
            // V2 flow
            ThreeDSecureLookup threeDSecureLookup = data.getParcelableExtra(ThreeDSecureActivity.EXTRA_THREE_D_SECURE_LOOKUP);
            ValidateResponse validateResponse = (ValidateResponse) data.getSerializableExtra(ThreeDSecureActivity.EXTRA_VALIDATION_RESPONSE);
            String jwt = data.getStringExtra(ThreeDSecureActivity.EXTRA_JWT);

            fragment.sendAnalyticsEvent(String.format("three-d-secure.verification-flow.cardinal-sdk.action-code.%s", validateResponse.getActionCode().name().toLowerCase()));

            switch (validateResponse.getActionCode()) {
                case FAILURE:
                case SUCCESS:
                case NOACTION:
                    authenticateCardinalJWT(fragment, threeDSecureLookup, jwt);

                    fragment.sendAnalyticsEvent("three-d-secure.verification-flow.completed");
                    break;
                case ERROR:
                    fragment.postCallback(new BraintreeException(validateResponse.errorDescription));
                    fragment.sendAnalyticsEvent("three-d-secure.verification-flow.failed");
                    break;
                case CANCEL:
                    fragment.postCancelCallback(BraintreeRequestCodes.THREE_D_SECURE);
                    fragment.sendAnalyticsEvent("three-d-secure.verification-flow.canceled");
                    break;
            }
        }
    }

    private static void completeVerificationFlowWithNoncePayload(BraintreeFragment fragment, CardNonce noncePayload) {
        ThreeDSecureInfo info = noncePayload.getThreeDSecureInfo();

        fragment.sendAnalyticsEvent(String.format("three-d-secure.verification-flow.liability-shifted.%b", info.isLiabilityShifted()));
        fragment.sendAnalyticsEvent(String.format("three-d-secure.verification-flow.liability-shift-possible.%b", info.isLiabilityShiftPossible()));

        fragment.postCallback(noncePayload);
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

        fragment.browserSwitch(THREE_D_SECURE, redirectUri.toString());
    }

    static void configureCardinal(final BraintreeFragment fragment) {
        boolean cardinalSdkAvailable = ClassHelper.isClassAvailable(
                "com.cardinalcommerce.cardinalmobilesdk.Cardinal");

        if (cardinalSdkAvailable) {
            fragment.waitForConfiguration(new ConfigurationListener() {
                @Override
                public void onConfigurationFetched(Configuration configuration) {
                    CardinalEnvironment cardinalEnvironment = CardinalEnvironment.STAGING;
                    if ("production".equalsIgnoreCase(configuration.getEnvironment())) {
                        cardinalEnvironment = CardinalEnvironment.PRODUCTION;
                    }

                    CardinalConfigurationParameters cardinalConfigurationParameters = new CardinalConfigurationParameters();
                    cardinalConfigurationParameters.setEnvironment(cardinalEnvironment);
                    cardinalConfigurationParameters.setTimeout(8000);
                    cardinalConfigurationParameters.setEnableQuickAuth(false);
                    cardinalConfigurationParameters.setEnableDFSync(true);

                    Cardinal cardinal= Cardinal.getInstance();
                    cardinal.configure(fragment.getApplicationContext(), cardinalConfigurationParameters);
                    cardinal.init(configuration.getCardinalAuthenticationJwt(), new CardinalInitService() {
                        @Override
                        public void onSetupCompleted(String consumerSessionId) {
                            sDFReferenceId = consumerSessionId;

                            fragment.sendAnalyticsEvent("three-d-secure.cardinal-sdk.init.setup-completed");
                        }

                        @Override
                        public void onValidated(ValidateResponse validateResponse, String serverJWT) {
                            fragment.sendAnalyticsEvent("three-d-secure.cardinal-sdk.init.setup-failed");
                        }
                    });
                }
            });
        }
    }
}
