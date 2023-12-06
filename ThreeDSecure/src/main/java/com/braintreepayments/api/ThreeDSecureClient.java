package com.braintreepayments.api;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.cardinalcommerce.cardinalmobilesdk.models.ValidateResponse;

import org.json.JSONException;
import org.json.JSONObject;


/**
 * 3D Secure is a protocol that enables cardholders and issuers to add a layer of security to
 * e-commerce transactions via password entry at checkout.
 * <p>
 * One of the primary reasons to use 3D Secure is to benefit from a shift in liability from the
 * merchant to the issuer, which may result in interchange savings. Please read our online <a
 * href="https://developer.paypal.com/braintree/docs/guides/3d-secure/overview">documentation</a>
 * for a full explanation of 3D Secure.
 */
public class ThreeDSecureClient {

    private final CardinalClient cardinalClient;
    private final BraintreeClient braintreeClient;
    private final ThreeDSecureAPI api;

    /**
     * Initializes a new {@link ThreeDSecureClient} instance
     *
     * @param context an Android Context
     * @param authorization a Tokenization Key or Client Token used to authenticate
     */
    public ThreeDSecureClient(@NonNull Context context, @NonNull String authorization) {
        this(new BraintreeClient(context, authorization));
    }

    @VisibleForTesting
    ThreeDSecureClient(@NonNull BraintreeClient braintreeClient) {
        this(braintreeClient, new CardinalClient(),
                new ThreeDSecureAPI(braintreeClient));
    }

    @VisibleForTesting
    ThreeDSecureClient(BraintreeClient braintreeClient, CardinalClient cardinalClient,
                       ThreeDSecureAPI threeDSecureAPI) {
        this.cardinalClient = cardinalClient;
        this.braintreeClient = braintreeClient;
        this.api = threeDSecureAPI;
    }

    /**
     * Call this method to initiate the 3D Secure flow.
     * <p>
     * Verification is associated with a transaction amount and your merchant account. To specify a
     * different merchant account (or, in turn, currency), you will need to specify the merchant
     * account id when <a
     * href="https://developer.paypal.com/braintree/docs/start/hello-client#get-a-client-token">
     * generating a client token</a>
     * <p>
     * During lookup the original payment method nonce is consumed and a new one is returned, which
     * points to the original payment method, as well as the 3D Secure verification. Transactions
     * created with this nonce will be 3D Secure, and benefit from the appropriate liability shift
     * if authentication is successful or fail with a 3D Secure failure.
     *
     * @param context  Android context
     * @param request  the {@link ThreeDSecureRequest} with information used for authentication.
     * @param callback {@link ThreeDSecureResultCallback}
     */
    public void createPaymentAuthRequest(@NonNull final Context context,
                                         @NonNull final ThreeDSecureRequest request,
                                         @NonNull final ThreeDSecurePaymentAuthRequestCallback callback) {
        if (request.getAmount() == null || request.getNonce() == null) {
            callback.onThreeDSecurePaymentAuthRequest(new ThreeDSecurePaymentAuthRequest.Failure(new InvalidArgumentException(
                    "The ThreeDSecureRequest nonce and amount cannot be null")));
            return;
        }

        braintreeClient.getConfiguration((configuration, error) -> {
            if (configuration == null) {
                callback.onThreeDSecurePaymentAuthRequest(new ThreeDSecurePaymentAuthRequest.Failure(error));
                return;
            }

            if (!configuration.isThreeDSecureEnabled()) {
                callback.onThreeDSecurePaymentAuthRequest(new ThreeDSecurePaymentAuthRequest.Failure(new BraintreeException(
                        "Three D Secure is not enabled for this account. " +
                                "Please contact Braintree Support for assistance.")));
                return;
            }

            if (configuration.getCardinalAuthenticationJwt() == null) {
                callback.onThreeDSecurePaymentAuthRequest(new ThreeDSecurePaymentAuthRequest.Failure(
                        new BraintreeException("Merchant is not configured for 3DS 2.0. " +
                                "Please contact Braintree Support for assistance.")));
                return;
            }
            braintreeClient.sendAnalyticsEvent("three-d-secure.initialized");

            ThreeDSecureResultCallback internalResultCallback =
                    (threeDSecureResult, performLookupError) -> {
                if (threeDSecureResult != null) {
                    sendAnalyticsAndCallbackResult(threeDSecureResult, callback);
                } else {
                    callback.onThreeDSecurePaymentAuthRequest(new ThreeDSecurePaymentAuthRequest.Failure(performLookupError));
                }
            };

            CardinalInitializeCallback cardinalInitializeCallback =
                    (consumerSessionId, error1) -> {
                        if (consumerSessionId != null) {
                            api.performLookup(request,
                                    cardinalClient.getConsumerSessionId(), internalResultCallback);
                            braintreeClient.sendAnalyticsEvent(
                                    "three-d-secure.cardinal-sdk.init.setup-completed");
                        } else {
                            api.performLookup(request,
                                    cardinalClient.getConsumerSessionId(), internalResultCallback);
                            braintreeClient.sendAnalyticsEvent(
                                    "three-d-secure.cardinal-sdk.init.setup-failed");
                        }
                    };

            try {
                cardinalClient.initialize(context, configuration, request,
                        cardinalInitializeCallback);
            } catch (BraintreeException initializeException) {
                braintreeClient.sendAnalyticsEvent(
                        "three-d-secure.cardinal-sdk.init.failed");
                callback.onThreeDSecurePaymentAuthRequest(new ThreeDSecurePaymentAuthRequest.Failure(initializeException));
            }
        });
    }

    /**
     * Creates a stringified JSON object containing the information necessary to perform a lookup
     *
     * @param context  Android Context
     * @param request  the {@link ThreeDSecureRequest} that has a nonce and an optional UI
     *                 customization.
     * @param callback {@link ThreeDSecurePrepareLookupCallback}
     */
    public void prepareLookup(@NonNull final Context context,
                              @NonNull final ThreeDSecureRequest request,
                              @NonNull final ThreeDSecurePrepareLookupCallback callback) {

        final JSONObject lookupJSON = new JSONObject();
        try {
            lookupJSON
                    .put("authorizationFingerprint", braintreeClient.getAuthorization().getBearer())
                    .put("braintreeLibraryVersion",
                            "Android-" + BuildConfig.VERSION_NAME)
                    .put("nonce", request.getNonce())
                    .put("clientMetadata", new JSONObject()
                            .put("requestedThreeDSecureVersion", "2")
                            .put("sdkVersion", "Android/" + BuildConfig.VERSION_NAME));
        } catch (JSONException ignored) {
        }

        braintreeClient.getConfiguration((configuration, configError) -> {
            if (configuration == null) {
                callback.onResult(null, null, configError);
                return;
            }
            if (configuration.getCardinalAuthenticationJwt() == null) {
                Exception authError1 = new BraintreeException(
                        "Merchant is not configured for 3DS 2.0. " +
                                "Please contact Braintree Support for assistance.");
                callback.onResult(null, null, authError1);
                return;
            }

            CardinalInitializeCallback cardinalInitializeCallback =
                    (consumerSessionId, error) -> {
                        if (consumerSessionId != null) {
                            try {
                                lookupJSON.put("dfReferenceId",
                                        consumerSessionId);
                            } catch (JSONException ignored) {
                            }
                        }
                        callback.onResult(request, lookupJSON.toString(), null);
                    };

            try {
                cardinalClient.initialize(context, configuration, request,
                        cardinalInitializeCallback);
            } catch (BraintreeException initializeException) {
                braintreeClient.sendAnalyticsEvent(
                        "three-d-secure.cardinal-sdk.init.failed");
                callback.onResult(null, null, initializeException);
            }
        });
    }

    /**
     * Initialize a challenge from a server side lookup call.
     *
     * @param lookupResponse The lookup response from the server side call to lookup the 3D Secure
     *                       information.
     * @param callback       {@link ThreeDSecureResultCallback}
     */
    public void initializeChallengeWithLookupResponse(@NonNull final String lookupResponse, @NonNull
    final ThreeDSecurePaymentAuthRequestCallback callback) {
        braintreeClient.getConfiguration((configuration, error) -> {
            ThreeDSecureBundledResult result;
            try {
                result = ThreeDSecureBundledResult.fromJson(lookupResponse);
                sendAnalyticsAndCallbackResult(result, callback);
            } catch (JSONException e) {
                callback.onThreeDSecurePaymentAuthRequest(new ThreeDSecurePaymentAuthRequest.Failure(e));
            }
        });
    }

    void sendAnalyticsAndCallbackResult(ThreeDSecureBundledResult result,
                                        ThreeDSecurePaymentAuthRequestCallback callback) {
        ThreeDSecureLookup lookup = result.getLookup();

        boolean showChallenge = lookup.getAcsUrl() != null;
        String threeDSecureVersion = lookup.getThreeDSecureVersion();

        braintreeClient.sendAnalyticsEvent(
                String.format("three-d-secure.verification-flow.challenge-presented.%b",
                        showChallenge));
        braintreeClient.sendAnalyticsEvent(
                String.format("three-d-secure.verification-flow.3ds-version.%s",
                        threeDSecureVersion));

        if (!showChallenge) {
            ThreeDSecureNonce threeDSecureNonce = result.getThreeDSecureNonce();
            ThreeDSecureInfo info = threeDSecureNonce.getThreeDSecureInfo();

            braintreeClient.sendAnalyticsEvent(
                    String.format("three-d-secure.verification-flow.liability-shifted.%b",
                            info.isLiabilityShifted()));
            braintreeClient.sendAnalyticsEvent(
                    String.format("three-d-secure.verification-flow.liability-shift-possible.%b",
                            info.isLiabilityShiftPossible()));

            callback.onThreeDSecurePaymentAuthRequest(new ThreeDSecurePaymentAuthRequest.LaunchNotRequired(result.getThreeDSecureNonce(), result.getLookup()));
            return;
        }

        if (!threeDSecureVersion.startsWith("2.")) {
            String threeDSecureV1UnsupportedMessage =
                    "3D Secure v1 is deprecated and no longer supported. See https://developer.paypal.com/braintree/docs/guides/3d-secure/client-side/android/v4 for more information.";
            BraintreeException threeDSecureV1UnsupportedError =
                    new BraintreeException(threeDSecureV1UnsupportedMessage);
            callback.onThreeDSecurePaymentAuthRequest(new ThreeDSecurePaymentAuthRequest.Failure(threeDSecureV1UnsupportedError));
            return;
        }

        braintreeClient.sendAnalyticsEvent("three-d-secure.verification-flow.started");
        callback.onThreeDSecurePaymentAuthRequest(new ThreeDSecurePaymentAuthRequest.ReadyToLaunch(result));
    }

    /**
     * Call this method from the {@link ThreeDSecureLauncherCallback} passed to the
     * {@link ThreeDSecureLauncher} used to launch the 3DS authentication challenge.
     *
     * @param paymentAuthResult a {@link ThreeDSecurePaymentAuthResult} received in {@link ThreeDSecureLauncherCallback}
     * @param callback       a {@link ThreeDSecureResultCallback}
     */
    public void tokenize(ThreeDSecurePaymentAuthResult paymentAuthResult,
                         ThreeDSecureTokenizeCallback callback) {
        Exception threeDSecureError = paymentAuthResult.getError();
        if (threeDSecureError != null) {
            callback.onThreeDSecureResult(new ThreeDSecureResult.Failure(threeDSecureError, null));
        } else {
            ThreeDSecureBundledResult threeDSecureBundledResult = paymentAuthResult.getThreeSecureResult();
            ValidateResponse validateResponse = paymentAuthResult.getValidateResponse();
            String jwt = paymentAuthResult.getJWT();

            braintreeClient.sendAnalyticsEvent(
                    String.format("three-d-secure.verification-flow.cardinal-sdk.action-code.%s",
                            validateResponse.getActionCode().name().toLowerCase()));

            switch (validateResponse.getActionCode()) {
                case FAILURE:
                case NOACTION:
                case SUCCESS:
                    api.authenticateCardinalJWT(threeDSecureBundledResult, jwt,
                            (threeDSecureResult1, error) -> {
                                if (threeDSecureResult1 != null) {
                                    if (threeDSecureResult1.hasError()) {
                                        braintreeClient.sendAnalyticsEvent(
                                                "three-d-secure.verification-flow.upgrade-payment-method.failure.returned-lookup-nonce");
                                        callback.onThreeDSecureResult(new ThreeDSecureResult.Failure(new BraintreeException(threeDSecureResult1.getErrorMessage()), threeDSecureResult1.getThreeDSecureNonce()));
                                    } else if (threeDSecureResult1.getThreeDSecureNonce() != null){
                                        braintreeClient.sendAnalyticsEvent(
                                                "three-d-secure.verification-flow.upgrade-payment-method.succeeded");
                                        sendLiabilityShiftedAnalytics(threeDSecureResult1);
                                        callback.onThreeDSecureResult(new ThreeDSecureResult.Success(threeDSecureResult1.getThreeDSecureNonce()));
                                    }
                                } else if (error != null) {
                                    braintreeClient.sendAnalyticsEvent(
                                            "three-d-secure.verification-flow.upgrade-payment-method.errored");
                                    callback.onThreeDSecureResult(new ThreeDSecureResult.Failure(error, null));
                                }
                            });

                    braintreeClient.sendAnalyticsEvent(
                            "three-d-secure.verification-flow.completed");
                    break;
                case ERROR:
                case TIMEOUT:
                    callback.onThreeDSecureResult(new ThreeDSecureResult.Failure(
                            new BraintreeException(validateResponse.getErrorDescription()), null));
                    braintreeClient.sendAnalyticsEvent("three-d-secure.verification-flow.failed");
                    break;
                case CANCEL:
                    callback.onThreeDSecureResult(ThreeDSecureResult.Cancel.INSTANCE);
                    braintreeClient.sendAnalyticsEvent("three-d-secure.verification-flow.canceled");
                    break;
            }
        }
    }

    private void sendLiabilityShiftedAnalytics(ThreeDSecureBundledResult threeDSecureBundledResult) {
        ThreeDSecureInfo info = threeDSecureBundledResult.getThreeDSecureNonce().getThreeDSecureInfo();

        braintreeClient.sendAnalyticsEvent(
                String.format("three-d-secure.verification-flow.liability-shifted.%b",
                        info.isLiabilityShifted()));
        braintreeClient.sendAnalyticsEvent(
                String.format("three-d-secure.verification-flow.liability-shift-possible.%b",
                        info.isLiabilityShiftPossible()));
    }
}
