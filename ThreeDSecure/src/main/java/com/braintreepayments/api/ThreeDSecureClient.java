package com.braintreepayments.api;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.FragmentActivity;

import com.cardinalcommerce.cardinalmobilesdk.models.ValidateResponse;

import org.json.JSONException;
import org.json.JSONObject;

import static android.app.Activity.RESULT_OK;
import static com.braintreepayments.api.BraintreeRequestCodes.THREE_D_SECURE;

/**
 * 3D Secure is a protocol that enables cardholders and issuers to add a layer of security
 * to e-commerce transactions via password entry at checkout.
 * <p>
 * One of the primary reasons to use 3D Secure is to benefit from a shift in liability from the
 * merchant to the issuer, which may result in interchange savings. Please read our online
 * <a href="https://developers.braintreepayments.com/guides/3d-secure/overview">documentation</a>
 * for a full explanation of 3D Secure.
 */
public class ThreeDSecureClient {

    private final CardinalClient cardinalClient;
    private final BraintreeClient braintreeClient;
    private final ThreeDSecureV1BrowserSwitchHelper browserSwitchHelper;

    public ThreeDSecureClient(BraintreeClient braintreeClient) {
        this(braintreeClient, new CardinalClient(), new ThreeDSecureV1BrowserSwitchHelper());
    }

    @VisibleForTesting
    ThreeDSecureClient(BraintreeClient braintreeClient, CardinalClient cardinalClient, ThreeDSecureV1BrowserSwitchHelper browserSwitchHelper) {
        this.cardinalClient = cardinalClient;
        this.braintreeClient = braintreeClient;
        this.browserSwitchHelper = browserSwitchHelper;
    }

    /**
     * Verification is associated with a transaction amount and your merchant account. To specify a
     * different merchant account (or, in turn, currency), you will need to specify the merchant
     * account id when <a href="https://developers.braintreepayments.com/android/sdk/overview/generate-client-token">
     * generating a client token</a>
     * <p>
     * During lookup the original payment method nonce is consumed and a new one is returned,
     * which points to the original payment method, as well as the 3D Secure verification.
     * Transactions created with this nonce will be 3D Secure, and benefit from the appropriate
     * liability shift if authentication is successful or fail with a 3D Secure failure.
     *  @param activity the {@link FragmentActivity} backing the http request.
     * @param request  the {@link ThreeDSecureRequest} with information used for authentication.
     * @param callback the {@link ThreeDSecureResultCallback} to invoke on completion.
     */
    public void performVerification(final FragmentActivity activity, final ThreeDSecureRequest request, final ThreeDSecureResultCallback callback) {
        performVerification(activity, request, new ThreeDSecureLookupCallback() {
            @Override
            public void onResult(ThreeDSecureRequest request, ThreeDSecureLookup lookup, Exception error) {
                braintreeClient.sendAnalyticsEvent("three-d-secure.perform-verification.default-lookup-listener");
                if (error != null) {
                    callback.onResult(null, error);
                } else {
                    continuePerformVerification(activity, request, lookup, callback);
                }
            }
        });
    }

    /**
     * Verification is associated with a transaction amount and your merchant account. To specify a
     * different merchant account (or, in turn, currency), you will need to specify the merchant
     * account id when <a href="https://developers.braintreepayments.com/android/sdk/overview/generate-client-token">
     * generating a client token</a>
     * <p>
     * During lookup the original payment method nonce is consumed and a new one is returned,
     * which points to the original payment method, as well as the 3D Secure verification.
     * Transactions created with this nonce will be 3D Secure, and benefit from the appropriate
     * liability shift if authentication is successful or fail with a 3D Secure failure.
     *
     * @param activity the {@link FragmentActivity} backing the http request.
     * @param request  the {@link ThreeDSecureRequest} with information used for authentication.
     */
    public void performVerification(final FragmentActivity activity, final ThreeDSecureRequest request, final ThreeDSecureLookupCallback callback) {
        if (request.getAmount() == null || request.getNonce() == null) {
            callback.onResult(null, null, new InvalidArgumentException("The ThreeDSecureRequest nonce and amount cannot be null"));
            return;
        }

        braintreeClient.getConfiguration(new ConfigurationCallback() {
            @Override
            public void onResult(@Nullable final Configuration configuration, @Nullable Exception error) {
                if (!configuration.isThreeDSecureEnabled()) {
                    callback.onResult(null, null, new BraintreeException("Three D Secure is not enabled for this account. " +
                            "Please contact Braintree Support for assistance."));
                    return;
                }

                boolean supportsBrowserSwitch = braintreeClient.canPerformBrowserSwitch(activity, THREE_D_SECURE);
                if (!supportsBrowserSwitch) {
                    braintreeClient.sendAnalyticsEvent("three-d-secure.invalid-manifest");
                    callback.onResult(null, null, new BraintreeException("AndroidManifest.xml is incorrectly configured or another app " +
                            "defines the same browser switch url as this app. See " +
                            "https://developers.braintreepayments.com/guides/client-sdk/android/#browser-switch " +
                            "for the correct configuration"));
                    return;
                }

                if (configuration.getCardinalAuthenticationJwt() == null && ThreeDSecureRequest.VERSION_2.equals(request.getVersionRequested())) {
                    callback.onResult(null, null, new BraintreeException("Merchant is not configured for 3DS 2.0. " +
                            "Please contact Braintree Support for assistance."));
                    return;
                }
                braintreeClient.sendAnalyticsEvent("three-d-secure.initialized");

                if (ThreeDSecureRequest.VERSION_1.equals(request.getVersionRequested())) {
                    performThreeDSecureLookup(request, callback);
                    return;
                }

                cardinalClient.initialize(activity, configuration, request, new CardinalInitializeCallback() {
                    @Override
                    public void onResult(String consumerSessionId, Exception error) {
                        if (consumerSessionId != null) {
                            performThreeDSecureLookup(request, callback);
                            braintreeClient.sendAnalyticsEvent("three-d-secure.cardinal-sdk.init.setup-completed");
                        } else {
                            performThreeDSecureLookup(request, callback);
                            braintreeClient.sendAnalyticsEvent("three-d-secure.cardinal-sdk.init.setup-failed");
                        }
                    }
                });
            }
        });
    }

    /**
     * Continues the 3DS verification. Should be called from {@link ThreeDSecureLookupCallback#onResult(ThreeDSecureRequest, ThreeDSecureLookup, Exception)}
     * @param activity           the {@link FragmentActivity} backing the http request.
     * @param request            the {@link ThreeDSecureRequest} with information used for authentication.
     * @param threeDSecureLookup the {@link ThreeDSecureLookup} returned for this request.
 *                           Contains information about the 3DS verification request that will
 *                           be invoked in this method.
     * @param callback           the {@link ThreeDSecureResultCallback} to handle the result.
     */
    public void continuePerformVerification(final FragmentActivity activity, final ThreeDSecureRequest request, final ThreeDSecureLookup threeDSecureLookup, final ThreeDSecureResultCallback callback) {
        braintreeClient.getConfiguration(new ConfigurationCallback() {
            @Override
            public void onResult(@Nullable Configuration configuration, @Nullable Exception error) {

                boolean showChallenge = threeDSecureLookup.getAcsUrl() != null;
                String threeDSecureVersion = threeDSecureLookup.getThreeDSecureVersion();

                braintreeClient.sendAnalyticsEvent(String.format("three-d-secure.verification-flow.challenge-presented.%b", showChallenge));
                braintreeClient.sendAnalyticsEvent(String.format("three-d-secure.verification-flow.3ds-version.%s", threeDSecureVersion));

                if (!showChallenge) {
                    CardNonce cardNonce = threeDSecureLookup.getCardNonce();
                    ThreeDSecureInfo info = cardNonce.getThreeDSecureInfo();

                    braintreeClient.sendAnalyticsEvent(String.format("three-d-secure.verification-flow.liability-shifted.%b", info.isLiabilityShifted()));
                    braintreeClient.sendAnalyticsEvent(String.format("three-d-secure.verification-flow.liability-shift-possible.%b", info.isLiabilityShiftPossible()));

                    callback.onResult(cardNonce, null);
                    return;
                }

                if (!threeDSecureVersion.startsWith("2.")) {
                    String browserSwitchUrl = browserSwitchHelper.getUrl(
                            braintreeClient.getReturnUrlScheme(),
                            configuration.getAssetsUrl(),
                            request,
                            threeDSecureLookup);
                    BrowserSwitchOptions browserSwitchOptions = new BrowserSwitchOptions()
                            .requestCode(THREE_D_SECURE)
                            .returnUrlScheme(braintreeClient.getReturnUrlScheme())
                            .url(Uri.parse(browserSwitchUrl));
                    try {
                        braintreeClient.startBrowserSwitch(activity, browserSwitchOptions);
                    } catch (BrowserSwitchException e) {
                        callback.onResult(null, e);
                    }
                    return;
                }

                // perform cardinal authentication
                braintreeClient.sendAnalyticsEvent("three-d-secure.verification-flow.started");

                Bundle extras = new Bundle();
                extras.putParcelable(ThreeDSecureActivity.EXTRA_THREE_D_SECURE_LOOKUP, threeDSecureLookup);

                Intent intent = new Intent(activity, ThreeDSecureActivity.class);
                intent.putExtras(extras);

                activity.startActivityForResult(intent, THREE_D_SECURE);
            }
        });
    }

    /**
     * Creates a stringified JSON object containing the information necessary to perform a lookup
     *
     * @param context the {@link Context} backing the http request.
     * @param request  the {@link ThreeDSecureRequest} that has a nonce and an optional UI customization.
     * @param callback the {@link ThreeDSecurePrepareLookupCallback} that will receive the JSON string to send to the server for lookup.
     */
    public void prepareLookup(final Context context, final ThreeDSecureRequest request, final ThreeDSecurePrepareLookupCallback callback) {
        final JSONObject lookupJSON = new JSONObject();
        try {
            lookupJSON
                    .put("authorizationFingerprint", braintreeClient.getAuthorization().getBearer())
                    .put("braintreeLibraryVersion", "Android-" + BuildConfig.VERSION_NAME)
                    .put("nonce", request.getNonce())
                    .put("clientMetadata", new JSONObject()
                            .put("requestedThreeDSecureVersion", "2")
                            .put("sdkVersion", "Android/" + BuildConfig.VERSION_NAME));
        } catch (JSONException ignored) {
        }

        braintreeClient.getConfiguration(new ConfigurationCallback() {
            @Override
            public void onResult(@Nullable Configuration configuration, @Nullable Exception error) {
                if (configuration.getCardinalAuthenticationJwt() == null) {
                    Exception authError = new BraintreeException("Merchant is not configured for 3DS 2.0. " +
                            "Please contact Braintree Support for assistance.");
                    callback.onResult(null, null, authError);
                    return;
                }

                cardinalClient.initialize(context, configuration, request, new CardinalInitializeCallback() {
                    @Override
                    public void onResult(String consumerSessionId, Exception error) {
                        if (consumerSessionId != null) {
                            try {
                                lookupJSON.put("dfReferenceId", consumerSessionId);
                            } catch (JSONException ignored) {
                            }
                        }
                        callback.onResult(request, lookupJSON.toString(), null);
                    }
                });
            }
        });
    }
    
    void initializeChallengeWithLookupResponse(FragmentActivity activity, String lookupResponse, ThreeDSecureInitializeChallengeCallback callback) {
        initializeChallengeWithLookupResponse(activity, null, lookupResponse, callback);
    }

    void initializeChallengeWithLookupResponse(final FragmentActivity activity, final ThreeDSecureRequest threeDSecureRequest, final String lookupResponse, final ThreeDSecureInitializeChallengeCallback callback) {
        braintreeClient.getConfiguration(new ConfigurationCallback() {
            @Override
            public void onResult(@Nullable Configuration configuration, @Nullable Exception error) {
                if (configuration != null) {
                    try {
                        ThreeDSecureLookup threeDSecureLookup = ThreeDSecureLookup.fromJson(lookupResponse);

                        boolean showChallenge = threeDSecureLookup.getAcsUrl() != null;
                        String threeDSecureVersion = threeDSecureLookup.getThreeDSecureVersion();

                        if (!showChallenge) {
                            CardNonce cardNonce = threeDSecureLookup.getCardNonce();
                            ThreeDSecureInfo info = cardNonce.getThreeDSecureInfo();

                            braintreeClient.sendAnalyticsEvent(String.format("three-d-secure.verification-flow.liability-shifted.%b", info.isLiabilityShifted()));
                            braintreeClient.sendAnalyticsEvent(String.format("three-d-secure.verification-flow.liability-shift-possible.%b", info.isLiabilityShiftPossible()));

                            callback.onResult(cardNonce, null);
                            return;
                        }

                        if (!threeDSecureVersion.startsWith("2.")) {
                            String browserSwitchUrl = browserSwitchHelper.getUrl(
                                    braintreeClient.getReturnUrlScheme(),
                                    configuration.getAssetsUrl(),
                                    threeDSecureRequest,
                                    threeDSecureLookup);

                            BrowserSwitchOptions browserSwitchOptions = new BrowserSwitchOptions()
                                    .requestCode(THREE_D_SECURE)
                                    .returnUrlScheme(braintreeClient.getReturnUrlScheme())
                                    .url(Uri.parse(browserSwitchUrl));
                            try {
                                braintreeClient.startBrowserSwitch(activity, browserSwitchOptions);
                            } catch (BrowserSwitchException e) {
                                callback.onResult(null, e);
                            }
                            return;
                        }

                        // perform cardinal authentication
                        braintreeClient.sendAnalyticsEvent("three-d-secure.verification-flow.started");

                        Bundle extras = new Bundle();
                        extras.putParcelable(ThreeDSecureActivity.EXTRA_THREE_D_SECURE_LOOKUP, threeDSecureLookup);

                        Intent intent = new Intent(activity, ThreeDSecureActivity.class);
                        intent.putExtras(extras);
                        activity.startActivityForResult(intent, BraintreeRequestCodes.THREE_D_SECURE);

                    } catch (JSONException e) {
                        callback.onResult(null, e);
                    }
                }
            }
        });
    }

    private void notify3DSComplete(CardNonce cardNonce, ThreeDSecureResultCallback callback) {
        ThreeDSecureInfo info = cardNonce.getThreeDSecureInfo();

        braintreeClient.sendAnalyticsEvent(String.format("three-d-secure.verification-flow.liability-shifted.%b", info.isLiabilityShifted()));
        braintreeClient.sendAnalyticsEvent(String.format("three-d-secure.verification-flow.liability-shift-possible.%b", info.isLiabilityShiftPossible()));

        callback.onResult(cardNonce, null);
    }

    void authenticateCardinalJWT(final ThreeDSecureLookup threeDSecureLookup, final String cardinalJWT, final ThreeDSecureResultCallback callback) {
        final CardNonce lookupCardNonce = threeDSecureLookup.getCardNonce();

        braintreeClient.sendAnalyticsEvent("three-d-secure.verification-flow.upgrade-payment-method.started");

        final String lookupNonce = lookupCardNonce.getNonce();
        JSONObject body = new JSONObject();
        try {
            body.put("jwt", cardinalJWT);
            body.put("paymentMethodNonce", lookupNonce);
        } catch (JSONException ignored) {
        }

        String url = TokenizationClient.versionedPath(TokenizationClient.PAYMENT_METHOD_ENDPOINT + "/" + lookupNonce + "/three_d_secure/authenticate_from_jwt");
        String data = body.toString();

        braintreeClient.sendPOST(url, data, new HttpResponseCallback() {
            @Override
            public void success(String responseBody) {
                ThreeDSecureAuthenticationResponse authenticationResponse = ThreeDSecureAuthenticationResponse.fromJson(responseBody);

                // NEXT_MAJOR_VERSION
                // Remove this line. Pass back lookupCardNonce + error message if there are errors.
                // Otherwise pass back authenticationResponse.getCardNonce().
                CardNonce nonce = ThreeDSecureAuthenticationResponse.getNonceWithAuthenticationDetails(responseBody, lookupCardNonce);

                if (authenticationResponse.getErrors() != null) {
                    braintreeClient.sendAnalyticsEvent("three-d-secure.verification-flow.upgrade-payment-method.failure.returned-lookup-nonce");
                    nonce.getThreeDSecureInfo().setErrorMessage(authenticationResponse.getErrors());
                } else {
                    braintreeClient.sendAnalyticsEvent("three-d-secure.verification-flow.upgrade-payment-method.succeeded");
                }

                notify3DSComplete(nonce, callback);
            }

            @Override
            public void failure(Exception exception) {
                braintreeClient.sendAnalyticsEvent("three-d-secure.verification-flow.upgrade-payment-method.errored");
                callback.onResult(null, exception);
            }
        });
    }

    public void onBrowserSwitchResult(BrowserSwitchResult browserSwitchResult, final ThreeDSecureResultCallback callback) {
        // V1 flow
        int status = browserSwitchResult.getStatus();
        switch (status) {
            case BrowserSwitchStatus.CANCELED:
                callback.onResult(null, new BraintreeException("user canceled"));
                break;
            case BrowserSwitchStatus.SUCCESS:
            default:
                Uri deepLinkUrl = browserSwitchResult.getDeepLinkUrl();
                if (deepLinkUrl != null) {
                    String authResponse = deepLinkUrl.getQueryParameter("auth_response");
                    ThreeDSecureAuthenticationResponse authenticationResponse = ThreeDSecureAuthenticationResponse.fromJson(authResponse);

                    // NEXT_MAJOR_VERSION Make isSuccess package-private so that we have access to it, but merchants do not
                    if (authenticationResponse.isSuccess()) {
                        notify3DSComplete(authenticationResponse.getCardNonce(), callback);
                    } else {
                        callback.onResult(null, new ErrorWithResponse(422, authResponse));
                    }
                }
                break;
        }
    }

    public void onActivityResult(int resultCode, Intent data, ThreeDSecureResultCallback callback) {
        // V2 flow
        if (resultCode != RESULT_OK) {
            callback.onResult(null, new BraintreeException("user cancelled"));
            return;
        }

        ThreeDSecureLookup threeDSecureLookup = data.getParcelableExtra(ThreeDSecureActivity.EXTRA_THREE_D_SECURE_LOOKUP);
        ValidateResponse validateResponse = (ValidateResponse) data.getSerializableExtra(ThreeDSecureActivity.EXTRA_VALIDATION_RESPONSE);
        String jwt = data.getStringExtra(ThreeDSecureActivity.EXTRA_JWT);

        braintreeClient.sendAnalyticsEvent(String.format("three-d-secure.verification-flow.cardinal-sdk.action-code.%s", validateResponse.getActionCode().name().toLowerCase()));

        switch (validateResponse.getActionCode()) {
            case FAILURE:
            case SUCCESS:
            case NOACTION:
                authenticateCardinalJWT(threeDSecureLookup, jwt, callback);

                braintreeClient.sendAnalyticsEvent("three-d-secure.verification-flow.completed");
                break;
            case ERROR:
            case TIMEOUT:
                callback.onResult(null, new BraintreeException(validateResponse.getErrorDescription()));
                braintreeClient.sendAnalyticsEvent("three-d-secure.verification-flow.failed");
                break;
            case CANCEL:
                callback.onResult(null, new BraintreeException("user canceled 3DS"));
                braintreeClient.sendAnalyticsEvent("three-d-secure.verification-flow.canceled");
                break;
        }
    }

    private void performThreeDSecureLookup(final ThreeDSecureRequest request, final ThreeDSecureLookupCallback callback) {
        String url = TokenizationClient.versionedPath(TokenizationClient.PAYMENT_METHOD_ENDPOINT + "/" + request.getNonce() + "/three_d_secure/lookup");
        String data = request.build(cardinalClient.getConsumerSessionId());

        braintreeClient.sendPOST(url, data, new HttpResponseCallback() {
            @Override
            public void success(String responseBody) {
                try {
                    ThreeDSecureLookup threeDSecureLookup = ThreeDSecureLookup.fromJson(responseBody);
                    callback.onResult(request, threeDSecureLookup, null);
                } catch (JSONException exception) {
                    callback.onResult( null, null, exception);
                }
            }

            @Override
            public void failure(Exception exception) {
                callback.onResult(null, null, exception);
            }
        });
    }
}
