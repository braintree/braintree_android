//package com.braintreepayments.api.threedsecure;
//
//import android.content.Context;
//
//import androidx.annotation.NonNull;
//import androidx.annotation.VisibleForTesting;
//
//import com.braintreepayments.api.core.BraintreeClient;
//import com.braintreepayments.api.core.BraintreeException;
//import com.braintreepayments.api.core.BuildConfig;
//import com.braintreepayments.api.core.InvalidArgumentException;
//import com.cardinalcommerce.cardinalmobilesdk.models.ValidateResponse;
//
//import org.json.JSONException;
//import org.json.JSONObject;
//
//
///**
// * 3D Secure is a protocol that enables cardholders and issuers to add a layer of security to
// * e-commerce transactions via password entry at checkout.
// * <p>
// * One of the primary reasons to use 3D Secure is to benefit from a shift in liability from the
// * merchant to the issuer, which may result in interchange savings. Please read our online <a
// * href="https://developer.paypal.com/braintree/docs/guides/3d-secure/overview">documentation</a>
// * for a full explanation of 3D Secure.
// */
//public class ThreeDSecureClient {
//
//    private final CardinalClient cardinalClient;
//    private final BraintreeClient braintreeClient;
//    private final ThreeDSecureAPI api;
//
//    /**
//     * Initializes a new {@link ThreeDSecureClient} instance
//     *
//     * @param context an Android Context
//     * @param authorization a Tokenization Key or Client Token used to authenticate
//     */
//    public ThreeDSecureClient(@NonNull Context context, @NonNull String authorization) {
//        this(new BraintreeClient(context, authorization));
//    }
//
//    @VisibleForTesting
//    ThreeDSecureClient(@NonNull BraintreeClient braintreeClient) {
//        this(braintreeClient, new CardinalClient(),
//                new ThreeDSecureAPI(braintreeClient));
//    }
//
//    @VisibleForTesting
//    ThreeDSecureClient(BraintreeClient braintreeClient, CardinalClient cardinalClient,
//                       ThreeDSecureAPI threeDSecureAPI) {
//        this.cardinalClient = cardinalClient;
//        this.braintreeClient = braintreeClient;
//        this.api = threeDSecureAPI;
//    }
//
//    /**
//     * Call this method to initiate the 3D Secure flow.
//     * <p>
//     * Verification is associated with a transaction amount and your merchant account. To specify a
//     * different merchant account (or, in turn, currency), you will need to specify the merchant
//     * account id when <a
//     * href="https://developer.paypal.com/braintree/docs/start/hello-client#get-a-client-token">
//     * generating a client token</a>
//     * <p>
//     * During lookup the original payment method nonce is consumed and a new one is returned, which
//     * points to the original payment method, as well as the 3D Secure verification. Transactions
//     * created with this nonce will be 3D Secure, and benefit from the appropriate liability shift
//     * if authentication is successful or fail with a 3D Secure failure.
//     *
//     * @param context  Android context
//     * @param request  the {@link ThreeDSecureRequest} with information used for authentication.
//     * @param callback {@link ThreeDSecureResultCallback}
//     */
//    public void createPaymentAuthRequest(@NonNull final Context context,
//                                         @NonNull final ThreeDSecureRequest request,
//                                         @NonNull final ThreeDSecurePaymentAuthRequestCallback callback) {
//        braintreeClient.sendAnalyticsEvent(ThreeDSecureAnalytics.VERIFY_STARTED);
//        if (request.getAmount() == null || request.getNonce() == null) {
//            callbackCreatePaymentAuthFailure(callback, new ThreeDSecurePaymentAuthRequest.Failure(new InvalidArgumentException(
//                    "The ThreeDSecureRequest nonce and amount cannot be null")));
//            return;
//        }
//
//        braintreeClient.getConfiguration((configuration, error) -> {
//            if (configuration == null) {
//                callbackCreatePaymentAuthFailure(callback, new ThreeDSecurePaymentAuthRequest.Failure(error));
//                return;
//            }
//
//            if (!configuration.isThreeDSecureEnabled()) {
//                callbackCreatePaymentAuthFailure(callback, new ThreeDSecurePaymentAuthRequest.Failure(new BraintreeException(
//                        "Three D Secure is not enabled for this account. " +
//                                "Please contact Braintree Support for assistance.")));
//                return;
//            }
//
//            if (configuration.getCardinalAuthenticationJwt() == null) {
//                callbackCreatePaymentAuthFailure(callback, new ThreeDSecurePaymentAuthRequest.Failure(
//                        new BraintreeException("Merchant is not configured for 3DS 2.0. " +
//                                "Please contact Braintree Support for assistance.")));
//                return;
//            }
//
//            ThreeDSecureResultCallback internalResultCallback =
//                    (threeDSecureResult, performLookupError) -> {
//                if (threeDSecureResult != null) {
//                    braintreeClient.sendAnalyticsEvent(ThreeDSecureAnalytics.LOOKUP_SUCCEEDED);
//                    sendAnalyticsAndCallbackResult(threeDSecureResult, callback);
//                } else {
//                    braintreeClient.sendAnalyticsEvent(ThreeDSecureAnalytics.LOOKUP_FAILED);
//                    callbackCreatePaymentAuthFailure(callback, new ThreeDSecurePaymentAuthRequest.Failure(performLookupError));
//                }
//            };
//
//            CardinalInitializeCallback cardinalInitializeCallback =
//                    (consumerSessionId, error1) -> {
//                        if (consumerSessionId != null) {
//                            api.performLookup(request,
//                                    cardinalClient.getConsumerSessionId(), internalResultCallback);
//                        } else {
//                            api.performLookup(request,
//                                    cardinalClient.getConsumerSessionId(), internalResultCallback);
//                        }
//                    };
//
//            try {
//                cardinalClient.initialize(context, configuration, request,
//                        cardinalInitializeCallback);
//            } catch (BraintreeException initializeException) {
//                callbackCreatePaymentAuthFailure(callback, new ThreeDSecurePaymentAuthRequest.Failure(initializeException));
//            }
//        });
//    }
//
//    /**
//     * Creates a stringified JSON object containing the information necessary to perform a lookup
//     *
//     * @param context  Android Context
//     * @param request  the {@link ThreeDSecureRequest} that has a nonce and an optional UI
//     *                 customization.
//     * @param callback {@link ThreeDSecurePrepareLookupCallback}
//     */
//    public void prepareLookup(@NonNull final Context context,
//                              @NonNull final ThreeDSecureRequest request,
//                              @NonNull final ThreeDSecurePrepareLookupCallback callback) {
//
//        braintreeClient.sendAnalyticsEvent(ThreeDSecureAnalytics.VERIFY_STARTED);
//        final JSONObject lookupJSON = new JSONObject();
//        try {
//            lookupJSON
//                    .put("authorizationFingerprint", braintreeClient.getAuthorization().getBearer())
//                    .put("braintreeLibraryVersion",
//                            "Android-" + BuildConfig.VERSION_NAME)
//                    .put("nonce", request.getNonce())
//                    .put("clientMetadata", new JSONObject()
//                            .put("requestedThreeDSecureVersion", "2")
//                            .put("sdkVersion", "Android/" + BuildConfig.VERSION_NAME));
//        } catch (JSONException ignored) {
//        }
//
//        braintreeClient.getConfiguration((configuration, configError) -> {
//            if (configuration == null && configError != null) {
//                callbackPrepareLookupFailure(callback, new ThreeDSecurePrepareLookupResult.Failure(configError));
//                return;
//            }
//            if (configuration.getCardinalAuthenticationJwt() == null) {
//                Exception authError1 = new BraintreeException(
//                        "Merchant is not configured for 3DS 2.0. " +
//                                "Please contact Braintree Support for assistance.");
//                callbackPrepareLookupFailure(callback, new ThreeDSecurePrepareLookupResult.Failure(authError1));
//                return;
//            }
//
//            CardinalInitializeCallback cardinalInitializeCallback =
//                    (consumerSessionId, error) -> {
//                        if (consumerSessionId != null) {
//                            try {
//                                lookupJSON.put("dfReferenceId",
//                                        consumerSessionId);
//                            } catch (JSONException ignored) {
//                            }
//                        }
//                        braintreeClient.sendAnalyticsEvent(ThreeDSecureAnalytics.LOOKUP_SUCCEEDED);
//                        callback.onPrepareLookupResult(new ThreeDSecurePrepareLookupResult.Success(request, lookupJSON.toString()));
//                    };
//
//            try {
//                cardinalClient.initialize(context, configuration, request,
//                        cardinalInitializeCallback);
//            } catch (BraintreeException initializeException) {
//                callbackPrepareLookupFailure(callback, new ThreeDSecurePrepareLookupResult.Failure(initializeException));
//            }
//        });
//    }
//
//    /**
//     * Initialize a challenge from a server side lookup call.
//     *
//     * @param lookupResponse The lookup response from the server side call to lookup the 3D Secure
//     *                       information.
//     * @param callback       {@link ThreeDSecureResultCallback}
//     */
//    public void initializeChallengeWithLookupResponse(@NonNull final String lookupResponse, @NonNull
//    final ThreeDSecurePaymentAuthRequestCallback callback) {
//        braintreeClient.getConfiguration((configuration, error) -> {
//            ThreeDSecureParams result;
//            try {
//                result = ThreeDSecureParams.fromJson(lookupResponse);
//                sendAnalyticsAndCallbackResult(result, callback);
//            } catch (JSONException e) {
//                braintreeClient.sendAnalyticsEvent(ThreeDSecureAnalytics.LOOKUP_FAILED);
//                callbackCreatePaymentAuthFailure(callback, new ThreeDSecurePaymentAuthRequest.Failure(e));
//            }
//        });
//    }
//
//    void sendAnalyticsAndCallbackResult(ThreeDSecureParams result,
//                                        ThreeDSecurePaymentAuthRequestCallback callback) {
//        braintreeClient.sendAnalyticsEvent(ThreeDSecureAnalytics.LOOKUP_SUCCEEDED);
//        ThreeDSecureLookup lookup = result.getLookup();
//
//        boolean showChallenge = lookup.getAcsUrl() != null;
//        String threeDSecureVersion = lookup.getThreeDSecureVersion();
//
//
//        if (!showChallenge) {
//            callback.onThreeDSecurePaymentAuthRequest(new ThreeDSecurePaymentAuthRequest.LaunchNotRequired(result.getThreeDSecureNonce(), result.getLookup()));
//            return;
//        }
//
//        braintreeClient.sendAnalyticsEvent(ThreeDSecureAnalytics.CHALLENGE_REQUIRED);
//
//        if (!threeDSecureVersion.startsWith("2.")) {
//            String threeDSecureV1UnsupportedMessage =
//                    "3D Secure v1 is deprecated and no longer supported. See https://developer.paypal.com/braintree/docs/guides/3d-secure/client-side/android/v4 for more information.";
//            BraintreeException threeDSecureV1UnsupportedError =
//                    new BraintreeException(threeDSecureV1UnsupportedMessage);
//            callbackCreatePaymentAuthFailure(callback, new ThreeDSecurePaymentAuthRequest.Failure(threeDSecureV1UnsupportedError));
//            return;
//        }
//
//        callback.onThreeDSecurePaymentAuthRequest(new ThreeDSecurePaymentAuthRequest.ReadyToLaunch(result));
//    }
//
//    /**
//     * Call this method from the {@link ThreeDSecureLauncherCallback} passed to the
//     * {@link ThreeDSecureLauncher} used to launch the 3DS authentication challenge.
//     *
//     * @param paymentAuthResult a {@link ThreeDSecurePaymentAuthResult} received in {@link ThreeDSecureLauncherCallback}
//     * @param callback       a {@link ThreeDSecureResultCallback}
//     */
//    public void tokenize(ThreeDSecurePaymentAuthResult paymentAuthResult,
//                         ThreeDSecureTokenizeCallback callback) {
//        Exception threeDSecureError = paymentAuthResult.getError();
//        if (threeDSecureError != null) {
//            callbackTokenizeFailure(callback, new ThreeDSecureResult.Failure(threeDSecureError, null));
//        } else {
//            ThreeDSecureParams threeDSecureParams = paymentAuthResult.getThreeDSecureParams();
//            ValidateResponse validateResponse = paymentAuthResult.getValidateResponse();
//            String jwt = paymentAuthResult.getJwt();
//
//            switch (validateResponse.getActionCode()) {
//                case FAILURE:
//                case NOACTION:
//                case SUCCESS:
//                    api.authenticateCardinalJWT(threeDSecureParams, jwt,
//                            (threeDSecureResult1, error) -> {
//                                if (threeDSecureResult1 != null) {
//                                    if (threeDSecureResult1.hasError()) {
//                                        braintreeClient.sendAnalyticsEvent(ThreeDSecureAnalytics.JWT_AUTH_FAILED);
//                                        callbackTokenizeFailure(callback, new ThreeDSecureResult.Failure(new BraintreeException(threeDSecureResult1.getErrorMessage()), threeDSecureResult1.getThreeDSecureNonce()));
//                                    } else if (threeDSecureResult1.getThreeDSecureNonce() != null) {
//                                        braintreeClient.sendAnalyticsEvent(ThreeDSecureAnalytics.JWT_AUTH_SUCCEEDED);
//                                        callbackTokenizeSuccess(callback, new ThreeDSecureResult.Success(threeDSecureResult1.getThreeDSecureNonce()));
//                                    }
//                                } else if (error != null) {
//                                    braintreeClient.sendAnalyticsEvent(ThreeDSecureAnalytics.JWT_AUTH_FAILED);
//                                    callbackTokenizeFailure(callback, new ThreeDSecureResult.Failure(error, null));
//                                }
//                            });
//
//                    break;
//                case ERROR:
//                case TIMEOUT:
//                    callbackTokenizeFailure(callback, new ThreeDSecureResult.Failure(
//                            new BraintreeException(validateResponse.getErrorDescription()), null));
//                    break;
//                case CANCEL:
//                    callbackCancel(callback);
//                    break;
//            }
//        }
//    }
//
//    private void callbackCreatePaymentAuthFailure(
//        ThreeDSecurePaymentAuthRequestCallback callback,
//        @NonNull ThreeDSecurePaymentAuthRequest.Failure failure
//    ) {
//        braintreeClient.sendAnalyticsEvent(ThreeDSecureAnalytics.VERIFY_FAILED);
//        callback.onThreeDSecurePaymentAuthRequest(failure);
//    }
//
//    private void callbackPrepareLookupFailure(ThreeDSecurePrepareLookupCallback callback, ThreeDSecurePrepareLookupResult.Failure result) {
//        braintreeClient.sendAnalyticsEvent(ThreeDSecureAnalytics.LOOKUP_FAILED);
//        braintreeClient.sendAnalyticsEvent(ThreeDSecureAnalytics.VERIFY_FAILED);
//        callback.onPrepareLookupResult(result);
//    }
//
//    private void callbackTokenizeFailure(ThreeDSecureTokenizeCallback callback, ThreeDSecureResult.Failure result) {
//        braintreeClient.sendAnalyticsEvent(ThreeDSecureAnalytics.VERIFY_FAILED);
//        callback.onThreeDSecureResult(result);
//    }
//
//    private void callbackTokenizeSuccess(ThreeDSecureTokenizeCallback callback, ThreeDSecureResult.Success result) {
//        braintreeClient.sendAnalyticsEvent(ThreeDSecureAnalytics.VERIFY_SUCCEEDED);
//        callback.onThreeDSecureResult(result);
//    }
//
//    private void callbackCancel(ThreeDSecureTokenizeCallback callback) {
//        braintreeClient.sendAnalyticsEvent(ThreeDSecureAnalytics.VERIFY_CANCELED);
//        callback.onThreeDSecureResult(ThreeDSecureResult.Cancel.INSTANCE);
//    }
//}
