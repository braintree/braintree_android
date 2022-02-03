package com.braintreepayments.api;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
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

    // TODO - reorganize methods

    private final CardinalClient cardinalClient;
    private final BraintreeClient braintreeClient;
    private final ThreeDSecureV1BrowserSwitchHelper browserSwitchHelper;
    private ThreeDSecureListener listener;
    private ThreeDSecureAPI api;
    @VisibleForTesting
    ThreeDSecureLifecycleObserver observer;

    public ThreeDSecureClient(@NonNull BraintreeClient braintreeClient) {
        this(braintreeClient, new CardinalClient(), new ThreeDSecureV1BrowserSwitchHelper());
    }

    @VisibleForTesting
    ThreeDSecureClient(BraintreeClient braintreeClient, CardinalClient cardinalClient, ThreeDSecureV1BrowserSwitchHelper browserSwitchHelper) {
        this.cardinalClient = cardinalClient;
        this.braintreeClient = braintreeClient;
        this.browserSwitchHelper = browserSwitchHelper;
        this.api = new ThreeDSecureAPI(braintreeClient);
    }

    @VisibleForTesting
    void addObserver(@NonNull FragmentActivity activity) {
        if (observer == null) {
            observer = new ThreeDSecureLifecycleObserver(activity.getActivityResultRegistry(), this);
        }
        activity.getLifecycle().addObserver(observer);
    }


    // TODO - doc strings
    // TODO - add lifecycle observer when methods are invoked
    public void performVerification(@NonNull final FragmentActivity activity, @NonNull final ThreeDSecureRequest request) {
        addObserver(activity);
        performVerification(activity, request, new ThreeDSecureResultCallback() {
            @Override
            public void onResult(@Nullable ThreeDSecureResult threeDSecureResult, @Nullable Exception error) {
                if (threeDSecureResult != null) {
                    listener.onThreeDSecureSuccess(threeDSecureResult);
                } else if (error != null) {
                    listener.onThreeDSecureFailure(error);
                }
            }
        });
    }

    public void continuePerformVerification(@NonNull final FragmentActivity activity, @NonNull final ThreeDSecureRequest request, @NonNull final ThreeDSecureResult result) {
        addObserver(activity);
        continuePerformVerification(activity, request, result, new ThreeDSecureResultCallback() {
            @Override
            public void onResult(@Nullable ThreeDSecureResult threeDSecureResult, @Nullable Exception error) {
                if (threeDSecureResult != null) {
                    listener.onThreeDSecureSuccess(threeDSecureResult);
                } else if (error != null) {
                    listener.onThreeDSecureFailure(error);
                }
            }
        });
    }

    public void initializeChallengeWithLookupResponse(@NonNull FragmentActivity activity, @NonNull String lookupResponse) {
        addObserver(activity);
        initializeChallengeWithLookupResponse(activity, lookupResponse, new ThreeDSecureResultCallback() {
            @Override
            public void onResult(@Nullable ThreeDSecureResult threeDSecureResult, @Nullable Exception error) {
                if (threeDSecureResult != null) {
                    listener.onThreeDSecureSuccess(threeDSecureResult);
                } else if (error != null) {
                    listener.onThreeDSecureFailure(error);
                }
            }
        });
    }

    public void initializeChallengeWithLookupResponse(@NonNull final FragmentActivity activity, @Nullable final ThreeDSecureRequest request, @NonNull final String lookupResponse) {
        addObserver(activity);
        initializeChallengeWithLookupResponse(activity, request, lookupResponse, new ThreeDSecureResultCallback() {
            @Override
            public void onResult(@Nullable ThreeDSecureResult threeDSecureResult, @Nullable Exception error) {
                if (threeDSecureResult != null) {
                    listener.onThreeDSecureSuccess(threeDSecureResult);
                } else if (error != null) {
                    listener.onThreeDSecureFailure(error);
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
     * @param activity Android FragmentActivity
     * @param request  the {@link ThreeDSecureRequest} with information used for authentication.
     * @param callback {@link ThreeDSecureResultCallback}
     */
    public void performVerification(@NonNull final FragmentActivity activity, @NonNull final ThreeDSecureRequest request, @NonNull final ThreeDSecureResultCallback callback) {
        if (request.getAmount() == null || request.getNonce() == null) {
            callback.onResult(null, new InvalidArgumentException("The ThreeDSecureRequest nonce and amount cannot be null"));
            return;
        }

        braintreeClient.getConfiguration(new ConfigurationCallback() {
            @Override
            public void onResult(@Nullable final Configuration configuration, @Nullable Exception error) {
                if (configuration == null) {
                    callback.onResult(null, error);
                    return;
                }

                if (!configuration.isThreeDSecureEnabled()) {
                    callback.onResult(null, new BraintreeException("Three D Secure is not enabled for this account. " +
                            "Please contact Braintree Support for assistance."));
                    return;
                }

                boolean supportsBrowserSwitch = braintreeClient.canPerformBrowserSwitch(activity, THREE_D_SECURE);
                if (!supportsBrowserSwitch) {
                    braintreeClient.sendAnalyticsEvent("three-d-secure.invalid-manifest");
                    callback.onResult(null, new BraintreeException("AndroidManifest.xml is incorrectly configured or another app " +
                            "defines the same browser switch url as this app. See " +
                            "https://developers.braintreepayments.com/guides/client-sdk/android/#browser-switch " +
                            "for the correct configuration"));
                    return;
                }

                if (configuration.getCardinalAuthenticationJwt() == null && ThreeDSecureRequest.VERSION_2.equals(request.getVersionRequested())) {
                    callback.onResult(null, new BraintreeException("Merchant is not configured for 3DS 2.0. " +
                            "Please contact Braintree Support for assistance."));
                    return;
                }
                braintreeClient.sendAnalyticsEvent("three-d-secure.initialized");

                final String data = request.build(cardinalClient.getConsumerSessionId());

                if (ThreeDSecureRequest.VERSION_1.equals(request.getVersionRequested())) {
                    api.performThreeDSecureLookup(request, data, callback);
                    return;
                }

                cardinalClient.initialize(activity, configuration, request, new CardinalInitializeCallback() {
                    @Override
                    public void onResult(String consumerSessionId, Exception error) {
                        if (consumerSessionId != null) {
                            api.performThreeDSecureLookup(request, data, callback);
                            braintreeClient.sendAnalyticsEvent("three-d-secure.cardinal-sdk.init.setup-completed");
                        } else {
                            api.performThreeDSecureLookup(request, data, callback);
                            braintreeClient.sendAnalyticsEvent("three-d-secure.cardinal-sdk.init.setup-failed");
                        }
                    }
                });
            }
        });
    }

    /**
     * Continues the 3DS verification. Should be called from {@link ThreeDSecureResultCallback#onResult(ThreeDSecureResult, Exception)}
     *
     * @param activity Android FragmentActivity
     * @param request  the {@link ThreeDSecureRequest} with information used for authentication.
     * @param result   the {@link ThreeDSecureResult} returned for this request.
     *                 Contains information about the 3DS verification request that will
     *                 be invoked in this method.
     * @param callback {@link ThreeDSecureResultCallback}
     */
    public void continuePerformVerification(@NonNull final FragmentActivity activity, @NonNull final ThreeDSecureRequest request, @NonNull final ThreeDSecureResult result, @NonNull final ThreeDSecureResultCallback callback) {
        braintreeClient.getConfiguration(new ConfigurationCallback() {
            @Override
            public void onResult(@Nullable Configuration configuration, @Nullable Exception error) {
                startVerificationFlow(activity, configuration, request, result, callback);
            }
        });
    }

    /**
     * Creates a stringified JSON object containing the information necessary to perform a lookup
     *
     * @param context Android Context
     * @param request  the {@link ThreeDSecureRequest} that has a nonce and an optional UI customization.
     * @param callback {@link ThreeDSecurePrepareLookupCallback}
     */
    public void prepareLookup(@NonNull final Context context, @NonNull final ThreeDSecureRequest request, @NonNull final ThreeDSecurePrepareLookupCallback callback) {
        braintreeClient.getAuthorization(new AuthorizationCallback() {
            @Override
            public void onAuthorizationResult(@Nullable Authorization authorization, @Nullable Exception authError) {
                if (authorization != null) {

                    final JSONObject lookupJSON = new JSONObject();
                    try {
                        lookupJSON
                                .put("authorizationFingerprint", authorization.getBearer())
                                .put("braintreeLibraryVersion", "Android-" + BuildConfig.VERSION_NAME)
                                .put("nonce", request.getNonce())
                                .put("clientMetadata", new JSONObject()
                                        .put("requestedThreeDSecureVersion", "2")
                                        .put("sdkVersion", "Android/" + BuildConfig.VERSION_NAME));
                    } catch (JSONException ignored) {
                    }

                    braintreeClient.getConfiguration(new ConfigurationCallback() {
                        @Override
                        public void onResult(@Nullable Configuration configuration, @Nullable Exception configError) {
                            if (configuration == null) {
                                callback.onResult(null, null, configError);
                                return;
                            }
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
                } else {
                    callback.onResult(null, null, authError);
                }
            }
        });
    }

    /**
     * Initialize a challenge from a server side lookup call.
     *
     * @param activity Android FragmentActivity
     * @param lookupResponse The lookup response from the server side call to lookup the 3D Secure information.
     * @param callback {@link ThreeDSecureResultCallback}
     */
    public void initializeChallengeWithLookupResponse(@NonNull FragmentActivity activity, @NonNull String lookupResponse, @NonNull ThreeDSecureResultCallback callback) {
        initializeChallengeWithLookupResponse(activity, null, lookupResponse, callback);
    }

    /**
     * Initialize a challenge from a server side lookup call.
     *
     * @param activity Android FragmentActivity
     * @param request The {@link ThreeDSecureRequest} with optional UI customization.
     * @param lookupResponse The lookup response from the server side call to lookup the 3D Secure information.
     * @param callback {@link ThreeDSecureResultCallback}
     */
    public void initializeChallengeWithLookupResponse(@NonNull final FragmentActivity activity, @Nullable final ThreeDSecureRequest request, @NonNull final String lookupResponse, @NonNull final ThreeDSecureResultCallback callback) {
        braintreeClient.getConfiguration(new ConfigurationCallback() {
            @Override
            public void onResult(@Nullable Configuration configuration, @Nullable Exception error) {
                ThreeDSecureResult result;
                try {
                    result = ThreeDSecureResult.fromJson(lookupResponse);
                    startVerificationFlow(activity, configuration, request, result, callback);
                } catch (JSONException e) {
                    callback.onResult(null, e);
                }
            }
        });
    }

    private void startVerificationFlow(FragmentActivity activity, Configuration configuration, ThreeDSecureRequest request, ThreeDSecureResult result, ThreeDSecureResultCallback callback) {
        ThreeDSecureLookup lookup = result.getLookup();

        boolean showChallenge = lookup.getAcsUrl() != null;
        String threeDSecureVersion = lookup.getThreeDSecureVersion();

        braintreeClient.sendAnalyticsEvent(String.format("three-d-secure.verification-flow.challenge-presented.%b", showChallenge));
        braintreeClient.sendAnalyticsEvent(String.format("three-d-secure.verification-flow.3ds-version.%s", threeDSecureVersion));

        if (!showChallenge) {
            CardNonce cardNonce = result.getTokenizedCard();
            ThreeDSecureInfo info = cardNonce.getThreeDSecureInfo();

            braintreeClient.sendAnalyticsEvent(String.format("three-d-secure.verification-flow.liability-shifted.%b", info.isLiabilityShifted()));
            braintreeClient.sendAnalyticsEvent(String.format("three-d-secure.verification-flow.liability-shift-possible.%b", info.isLiabilityShiftPossible()));

            callback.onResult(result, null);
            return;
        }

        if (!threeDSecureVersion.startsWith("2.")) {
            String browserSwitchUrl = browserSwitchHelper.getUrl(
                    braintreeClient.getReturnUrlScheme(),
                    configuration.getAssetsUrl(),
                    request,
                    lookup);
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
        // TODO - unit test plz
        if (observer != null) {
             observer.launch(result);
        } else {
            Bundle extras = new Bundle();
            extras.putParcelable(ThreeDSecureActivity.EXTRA_THREE_D_SECURE_RESULT, result);

            Intent intent = new Intent(activity, ThreeDSecureActivity.class);
            intent.putExtras(extras);

            activity.startActivityForResult(intent, THREE_D_SECURE);
        }
    }

    /**
     * @param browserSwitchResult a {@link BrowserSwitchResult} with a {@link BrowserSwitchStatus}
     * @param callback            {@link ThreeDSecureResultCallback}
     */
    public void onBrowserSwitchResult(@NonNull BrowserSwitchResult browserSwitchResult, @NonNull final ThreeDSecureResultCallback callback) {
        // V1 flow
        //noinspection ConstantConditions
        if (browserSwitchResult == null) {
            callback.onResult(null, new BraintreeException("BrowserSwitchResult cannot be null"));
            return;
        }
        int status = browserSwitchResult.getStatus();
        switch (status) {
            case BrowserSwitchStatus.CANCELED:
                callback.onResult(null, new UserCanceledException("User canceled 3DS."));
                break;
            case BrowserSwitchStatus.SUCCESS:
            default:
                Uri deepLinkUrl = browserSwitchResult.getDeepLinkUrl();
                if (deepLinkUrl != null) {
                    String authResponse = deepLinkUrl.getQueryParameter("auth_response");
                    try {
                        ThreeDSecureResult result = ThreeDSecureResult.fromJson(authResponse);
                        if (result.hasError()) {
                            callback.onResult(null, new ErrorWithResponse(422, authResponse));
                        } else {
                            ThreeDSecureInfo info = result.getTokenizedCard().getThreeDSecureInfo();

                            braintreeClient.sendAnalyticsEvent(String.format("three-d-secure.verification-flow.liability-shifted.%b", info.isLiabilityShifted()));
                            braintreeClient.sendAnalyticsEvent(String.format("three-d-secure.verification-flow.liability-shift-possible.%b", info.isLiabilityShiftPossible()));

                            callback.onResult(result, null);                        }
                    } catch (JSONException e) {
                        callback.onResult(null, e);
                    }
                }
                break;
        }
    }

    /**
     * @param resultCode a code associated with the Activity result
     * @param data       Android Intent
     * @param callback   {@link ThreeDSecureResultCallback}
     */
    public void onActivityResult(int resultCode, @Nullable Intent data, @NonNull ThreeDSecureResultCallback callback) {
        // V2 flow
        if (resultCode != RESULT_OK) {
            callback.onResult(null, new UserCanceledException("User canceled 3DS."));
            return;
        }

        ThreeDSecureResult threeDSecureResult = data.getParcelableExtra(ThreeDSecureActivity.EXTRA_THREE_D_SECURE_RESULT);
        ValidateResponse validateResponse = (ValidateResponse) data.getSerializableExtra(ThreeDSecureActivity.EXTRA_VALIDATION_RESPONSE);
        String jwt = data.getStringExtra(ThreeDSecureActivity.EXTRA_JWT);

        braintreeClient.sendAnalyticsEvent(String.format("three-d-secure.verification-flow.cardinal-sdk.action-code.%s", validateResponse.getActionCode().name().toLowerCase()));

        switch (validateResponse.getActionCode()) {
            case FAILURE:
            case SUCCESS:
            case NOACTION:
                api.authenticateCardinalJWT(threeDSecureResult, jwt, callback);

                braintreeClient.sendAnalyticsEvent("three-d-secure.verification-flow.completed");
                break;
            case ERROR:
            case TIMEOUT:
                callback.onResult(null, new BraintreeException(validateResponse.getErrorDescription()));
                braintreeClient.sendAnalyticsEvent("three-d-secure.verification-flow.failed");
                break;
            case CANCEL:
                callback.onResult(null, new UserCanceledException("User canceled 3DS."));
                braintreeClient.sendAnalyticsEvent("three-d-secure.verification-flow.canceled");
                break;
        }
    }

    // TODO - unit test
    void onCardinalResult(CardinalResult cardinalResult) {
        Exception threeDSecureError = cardinalResult.getError();
        if (threeDSecureError != null && listener != null) {
            listener.onThreeDSecureFailure(threeDSecureError);
        } else {
            ThreeDSecureResult threeDSecureResult = cardinalResult.getThreeSecureResult();
            ValidateResponse validateResponse = cardinalResult.getValidateResponse();
            String jwt = cardinalResult.getJWT();

            braintreeClient.sendAnalyticsEvent(String.format("three-d-secure.verification-flow.cardinal-sdk.action-code.%s", validateResponse.getActionCode().name().toLowerCase()));

            switch (validateResponse.getActionCode()) {
                case FAILURE:
                case SUCCESS:
                case NOACTION:
                    api.authenticateCardinalJWT(threeDSecureResult, jwt, new ThreeDSecureResultCallback() {
                        @Override
                        public void onResult(@Nullable ThreeDSecureResult threeDSecureResult, @Nullable Exception error) {
                            if (threeDSecureResult != null) {
                                listener.onThreeDSecureSuccess(threeDSecureResult);
                            } else if (error != null) {
                                listener.onThreeDSecureFailure(error);
                            }
                        }
                    });
                    braintreeClient.sendAnalyticsEvent("three-d-secure.verification-flow.completed");
                    break;
                case ERROR:
                case TIMEOUT:
                    listener.onThreeDSecureFailure(new BraintreeException(validateResponse.getErrorDescription()));
                    braintreeClient.sendAnalyticsEvent("three-d-secure.verification-flow.failed");
                    break;
                case CANCEL:
                    listener.onThreeDSecureFailure(new UserCanceledException("User canceled 3DS."));
                    braintreeClient.sendAnalyticsEvent("three-d-secure.verification-flow.canceled");
                    break;
            }
        }
    }

    public void setListener(ThreeDSecureListener listener) {
        this.listener = listener;
    }

    void onBrowserSwitchResult(FragmentActivity activity) {
        BrowserSwitchResult browserSwitchResult = braintreeClient.deliverBrowserSwitchResult(activity);

        onBrowserSwitchResult(browserSwitchResult, new ThreeDSecureResultCallback() {
            @Override
            public void onResult(@Nullable ThreeDSecureResult threeDSecureResult, @Nullable Exception error) {
                if (threeDSecureResult != null) {
                    listener.onThreeDSecureSuccess(threeDSecureResult);
                } else if (error != null) {
                    listener.onThreeDSecureFailure(error);
                }
            }
        });
    }
}
