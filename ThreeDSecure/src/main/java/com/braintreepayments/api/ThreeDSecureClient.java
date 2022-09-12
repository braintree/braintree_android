package com.braintreepayments.api;

import static android.app.Activity.RESULT_OK;
import static com.braintreepayments.api.BraintreeRequestCodes.THREE_D_SECURE;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;

import com.cardinalcommerce.cardinalmobilesdk.models.ValidateResponse;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 3D Secure is a protocol that enables cardholders and issuers to add a layer of security
 * to e-commerce transactions via password entry at checkout.
 * <p>
 * One of the primary reasons to use 3D Secure is to benefit from a shift in liability from the
 * merchant to the issuer, which may result in interchange savings. Please read our online
 * <a href="https://developer.paypal.com/braintree/docs/guides/3d-secure/overview">documentation</a>
 * for a full explanation of 3D Secure.
 */
public class ThreeDSecureClient {

    private final CardinalClient cardinalClient;
    private final BraintreeClient braintreeClient;
    private final ThreeDSecureV1BrowserSwitchHelper browserSwitchHelper;
    private final ThreeDSecureAPI api;
    private ThreeDSecureListener listener;

    @VisibleForTesting
    BrowserSwitchResult pendingBrowserSwitchResult;

    @VisibleForTesting
    ThreeDSecureLifecycleObserver observer;

    /**
     * Create a new instance of {@link ThreeDSecureClient} from within an Activity using a {@link BraintreeClient}.
     *
     * @param activity a {@link FragmentActivity}
     * @param braintreeClient a {@link BraintreeClient}
     */
    public ThreeDSecureClient(@NonNull FragmentActivity activity, @NonNull BraintreeClient braintreeClient) {
        this(activity, activity.getLifecycle(), braintreeClient, new CardinalClient(), new ThreeDSecureV1BrowserSwitchHelper(), new ThreeDSecureAPI(braintreeClient));
    }

    /**
     * Create a new instance of {@link ThreeDSecureClient} from within a Fragment using a {@link BraintreeClient}.
     *
     * @param fragment a {@link Fragment}
     * @param braintreeClient a {@link BraintreeClient}
     */
    public ThreeDSecureClient(@NonNull Fragment fragment, @NonNull BraintreeClient braintreeClient) {
        this(fragment.requireActivity(), fragment.getLifecycle(), braintreeClient, new CardinalClient(), new ThreeDSecureV1BrowserSwitchHelper(), new ThreeDSecureAPI(braintreeClient));
    }

    /**
     * Create a new instance of {@link ThreeDSecureClient} using a {@link BraintreeClient}.
     *
     * Deprecated. Use {@link ThreeDSecureClient(Fragment, BraintreeClient)} or
     * {@link ThreeDSecureClient(FragmentActivity, BraintreeClient)} instead.
     *
     * @param braintreeClient a {@link BraintreeClient}
     */
    @Deprecated
    public ThreeDSecureClient(@NonNull BraintreeClient braintreeClient) {
        this(null, null, braintreeClient, new CardinalClient(), new ThreeDSecureV1BrowserSwitchHelper(), new ThreeDSecureAPI(braintreeClient));
    }

    @VisibleForTesting
    ThreeDSecureClient(FragmentActivity activity, Lifecycle lifecycle, BraintreeClient braintreeClient, CardinalClient cardinalClient, ThreeDSecureV1BrowserSwitchHelper browserSwitchHelper, ThreeDSecureAPI threeDSecureAPI) {
        this.cardinalClient = cardinalClient;
        this.braintreeClient = braintreeClient;
        this.browserSwitchHelper = browserSwitchHelper;
        this.api = threeDSecureAPI;
        if (activity != null && lifecycle != null) {
            addObserver(activity, lifecycle);
        }
    }

    @VisibleForTesting
    void addObserver(@NonNull FragmentActivity activity, @NonNull Lifecycle lifecycle) {
        observer = new ThreeDSecureLifecycleObserver(activity.getActivityResultRegistry(), this);
        lifecycle.addObserver(observer);
    }

    /**
     * Add a {@link ThreeDSecureListener} to your client to receive results or errors from the 3DS payment flow.
     * This method must be invoked on a {@link ThreeDSecureClient(Fragment, BraintreeClient)} or
     * {@link ThreeDSecureClient(FragmentActivity, BraintreeClient)} in order to receive results.
     *
     * @param listener a {@link ThreeDSecureListener}
     */
    public void setListener(ThreeDSecureListener listener) {
        this.listener = listener;
        if (pendingBrowserSwitchResult != null) {
            deliverBrowserSwitchResultToListener(pendingBrowserSwitchResult);
        }
    }

    // region Cardinal Initialize/Prepare Callback Methods

    /**
     * Verification is associated with a transaction amount and your merchant account. To specify a
     * different merchant account (or, in turn, currency), you will need to specify the merchant
     * account id when <a href="https://developer.paypal.com/braintree/docs/start/hello-client#get-a-client-token">
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

                //TODO NEXT_MAJOR_VERSION
                //Change link to docs to https://developer.paypal.com/braintree/docs/guides/client-sdk/setup/android/v4#browser-switch-setup
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

                if (ThreeDSecureRequest.VERSION_1.equals(request.getVersionRequested())) {
                    api.performLookup(request, cardinalClient.getConsumerSessionId(), callback);
                    return;
                }

                cardinalClient.initialize(activity, configuration, request, new CardinalInitializeCallback() {
                    @Override
                    public void onResult(String consumerSessionId, Exception error) {
                        if (consumerSessionId != null) {
                            api.performLookup(request, cardinalClient.getConsumerSessionId(), callback);
                            braintreeClient.sendAnalyticsEvent("three-d-secure.cardinal-sdk.init.setup-completed");
                        } else {
                            api.performLookup(request, cardinalClient.getConsumerSessionId(), callback);
                            braintreeClient.sendAnalyticsEvent("three-d-secure.cardinal-sdk.init.setup-failed");
                        }
                    }
                });
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

    // endregion

    // region Launch 3DS With App/Browser Switch Encapsulation

    /**
     * Continues the 3DS verification. Should be called from {@link ThreeDSecureResultCallback#onResult(ThreeDSecureResult, Exception)}.
     * The result of this verification will be returned to your {@link ThreeDSecureListener}.
     *
     * @param activity an Android FragmentActivity
     * @param request the {@link ThreeDSecureRequest} with information used for authentication
     * @param result a {@link ThreeDSecureResult} that contains information about the 3DS verification request.
     */
    public void continuePerformVerification(@NonNull final FragmentActivity activity, @NonNull final ThreeDSecureRequest request, @NonNull final ThreeDSecureResult result) {
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

    /**
     * Initialize a challenge from a server side lookup call. The result of this challenge will be
     * returned to your {@link ThreeDSecureListener}.
     *
     * @param activity Android FragmentActivity
     * @param lookupResponse The lookup response String from the server side call to lookup the 3D Secure information.
     */
    public void initializeChallengeWithLookupResponse(@NonNull FragmentActivity activity, @NonNull String lookupResponse) {
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

    /**
     * Initialize a challenge from a server side lookup call. The result of this challenge will be
     * returned to your {@link ThreeDSecureListener}.
     *
     * @param activity Android FragmentActivity
     * @param request The {@link ThreeDSecureRequest} with optional UI customization.
     * @param lookupResponse The lookup response from the server side call to lookup the 3D Secure information.
     */
    public void initializeChallengeWithLookupResponse(@NonNull final FragmentActivity activity, @Nullable final ThreeDSecureRequest request, @NonNull final String lookupResponse) {
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

    // endregion

    // region Deprecated Launch 3DS

    /**
     * Continues the 3DS verification. Should be called from {@link ThreeDSecureResultCallback#onResult(ThreeDSecureResult, Exception)}
     *
     * Deprecated. Use {@link ThreeDSecureClient#continuePerformVerification(FragmentActivity, ThreeDSecureRequest, ThreeDSecureResult)} instead.
     *
     * @param activity Android FragmentActivity
     * @param request  the {@link ThreeDSecureRequest} with information used for authentication.
     * @param result   the {@link ThreeDSecureResult} returned for this request.
     *                 Contains information about the 3DS verification request that will
     *                 be invoked in this method.
     * @param callback {@link ThreeDSecureResultCallback}
     */
    @Deprecated
    public void continuePerformVerification(@NonNull final FragmentActivity activity, @NonNull final ThreeDSecureRequest request, @NonNull final ThreeDSecureResult result, @NonNull final ThreeDSecureResultCallback callback) {
        braintreeClient.getConfiguration(new ConfigurationCallback() {
            @Override
            public void onResult(@Nullable Configuration configuration, @Nullable Exception error) {
                startVerificationFlow(activity, configuration, request, result, callback);
            }
        });
    }

    /**
     * Initialize a challenge from a server side lookup call.
     *
     * Deprecated. Use {@link ThreeDSecureClient#initializeChallengeWithLookupResponse(FragmentActivity, String)} instead.
     *
     * @param activity Android FragmentActivity
     * @param lookupResponse The lookup response from the server side call to lookup the 3D Secure information.
     * @param callback {@link ThreeDSecureResultCallback}
     */
    @Deprecated
    public void initializeChallengeWithLookupResponse(@NonNull FragmentActivity activity, @NonNull String lookupResponse, @NonNull ThreeDSecureResultCallback callback) {
        initializeChallengeWithLookupResponse(activity, null, lookupResponse, callback);
    }

    /**
     * Initialize a challenge from a server side lookup call.
     *
     * Deprecated. Use {@link ThreeDSecureClient#initializeChallengeWithLookupResponse(FragmentActivity, ThreeDSecureRequest, String)} instead.
     *
     * @param activity Android FragmentActivity
     * @param request The {@link ThreeDSecureRequest} with optional UI customization.
     * @param lookupResponse The lookup response from the server side call to lookup the 3D Secure information.
     * @param callback {@link ThreeDSecureResultCallback}
     */
    @Deprecated
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

    // endregion

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
                    .launchAsNewTask(braintreeClient.useDefaultDeepLinkHandler())
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

    // region Deprecated Handle App/Browser Switch Results

    /**
     * Deprecated. Use {@link ThreeDSecureListener}. To handle results.
     *
     * @param browserSwitchResult a {@link BrowserSwitchResult} with a {@link BrowserSwitchStatus}
     * @param callback            {@link ThreeDSecureResultCallback}
     */
    @Deprecated
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
                            sendLiabilityShiftedAnalytics(result);
                            callback.onResult(result, null);                        }
                    } catch (JSONException e) {
                        callback.onResult(null, e);
                    }
                }
                break;
        }
    }

    /**
     * Deprecated. Use {@link ThreeDSecureListener}. To handle results.
     *
     * @param resultCode a code associated with the Activity result
     * @param data       Android Intent
     * @param callback   {@link ThreeDSecureResultCallback}
     */
    @Deprecated
    public void onActivityResult(int resultCode, @Nullable Intent data, @NonNull final ThreeDSecureResultCallback callback) {
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
                api.authenticateCardinalJWT(threeDSecureResult, jwt, new ThreeDSecureResultCallback() {
                    @Override
                    public void onResult(@Nullable ThreeDSecureResult threeDSecureResult, @Nullable Exception error) {
                        if (threeDSecureResult != null) {
                            if (threeDSecureResult.hasError()) {
                                braintreeClient.sendAnalyticsEvent("three-d-secure.verification-flow.upgrade-payment-method.failure.returned-lookup-nonce");
                            } else {
                                braintreeClient.sendAnalyticsEvent("three-d-secure.verification-flow.upgrade-payment-method.succeeded");
                                sendLiabilityShiftedAnalytics(threeDSecureResult);
                            }
                        } else if (error != null) {
                            braintreeClient.sendAnalyticsEvent("three-d-secure.verification-flow.upgrade-payment-method.errored");
                        }
                        callback.onResult(threeDSecureResult, error);
                    }
                });

                braintreeClient.sendAnalyticsEvent("three-d-secure.verification-flow.completed");
                break;
            case ERROR:
            case TIMEOUT:
                callback.onResult(null, new BraintreeException(validateResponse.getErrorDescription()));
                braintreeClient.sendAnalyticsEvent("three-d-secure.verification-flow.failed");
                break;
            case CANCEL:
                callback.onResult(null, new UserCanceledException("User canceled 3DS.", true));
                braintreeClient.sendAnalyticsEvent("three-d-secure.verification-flow.canceled");
                break;
        }
    }

    // endregion

    // region Internal Handle App/Browser Switch Results

    void onBrowserSwitchResult(FragmentActivity activity) {
        BrowserSwitchResult pendingResult = braintreeClient.getBrowserSwitchResult(activity);
        if (pendingResult != null && pendingResult.getRequestCode() == THREE_D_SECURE) {
            this.pendingBrowserSwitchResult = braintreeClient.deliverBrowserSwitchResult(activity);
        }

        BrowserSwitchResult pendingResultFromCache =
                braintreeClient.getBrowserSwitchResultFromCache(activity);
        if (pendingResultFromCache != null && pendingResultFromCache.getRequestCode() == THREE_D_SECURE) {
            this.pendingBrowserSwitchResult =
                    braintreeClient.deliverBrowserSwitchResultFromCache(activity);
        }

        if (pendingBrowserSwitchResult != null && listener != null) {
            deliverBrowserSwitchResultToListener(pendingBrowserSwitchResult);
        }
    }

    void onBrowserSwitchResult(@NonNull BrowserSwitchResult browserSwitchResult) {
        this.pendingBrowserSwitchResult = browserSwitchResult;
        if (listener != null) {
            // NEXT_MAJOR_VERSION: determine if browser switch logic can be further decoupled
            // from the client to allow more flexibility to merchants who rely heavily on view model.
            deliverBrowserSwitchResultToListener(pendingBrowserSwitchResult);
        }
    }

    private void deliverBrowserSwitchResultToListener(final BrowserSwitchResult browserSwitchResult) {
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
        this.pendingBrowserSwitchResult = null;
    }

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
                case NOACTION:
                case SUCCESS:
                    api.authenticateCardinalJWT(threeDSecureResult, jwt, new ThreeDSecureResultCallback() {
                        @Override
                        public void onResult(@Nullable ThreeDSecureResult threeDSecureResult, @Nullable Exception error) {
                            if (threeDSecureResult != null) {
                                if (threeDSecureResult.hasError()) {
                                    braintreeClient.sendAnalyticsEvent("three-d-secure.verification-flow.upgrade-payment-method.failure.returned-lookup-nonce");
                                } else {
                                    braintreeClient.sendAnalyticsEvent("three-d-secure.verification-flow.upgrade-payment-method.succeeded");
                                    sendLiabilityShiftedAnalytics(threeDSecureResult);
                                }
                                listener.onThreeDSecureSuccess(threeDSecureResult);
                            } else if (error != null) {
                                braintreeClient.sendAnalyticsEvent("three-d-secure.verification-flow.upgrade-payment-method.errored");
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
                    listener.onThreeDSecureFailure(new UserCanceledException("User canceled 3DS.", true));
                    braintreeClient.sendAnalyticsEvent("three-d-secure.verification-flow.canceled");
                    break;
            }
        }
    }

    private void sendLiabilityShiftedAnalytics(ThreeDSecureResult threeDSecureResult) {
        ThreeDSecureInfo info = threeDSecureResult.getTokenizedCard().getThreeDSecureInfo();

        braintreeClient.sendAnalyticsEvent(String.format("three-d-secure.verification-flow.liability-shifted.%b", info.isLiabilityShifted()));
        braintreeClient.sendAnalyticsEvent(String.format("three-d-secure.verification-flow.liability-shift-possible.%b", info.isLiabilityShiftPossible()));
    }

    // NEXT_MAJOR_VERSION: duplication here could be a sign that we need to decouple browser switching
    // logic into another component that also gives merchants more flexibility when using view models
    BrowserSwitchResult getBrowserSwitchResult(FragmentActivity activity) {
        return braintreeClient.getBrowserSwitchResult(activity);
    }

    BrowserSwitchResult deliverBrowserSwitchResult(FragmentActivity activity) {
        return braintreeClient.deliverBrowserSwitchResult(activity);
    }

    BrowserSwitchResult getBrowserSwitchResultFromCache(FragmentActivity activity) {
        return braintreeClient.deliverBrowserSwitchResultFromCache(activity);
    }

    BrowserSwitchResult deliverBrowserSwitchResultFromCache(FragmentActivity activity) {
        return braintreeClient.deliverBrowserSwitchResultFromCache(activity);
    }

    // endregion
}
