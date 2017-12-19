package com.braintreepayments.api;

import android.app.Activity;

import com.braintreepayments.api.exceptions.ConfigurationException;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.BraintreeResponseListener;
import com.braintreepayments.api.interfaces.ConfigurationListener;
import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.internal.BraintreeSharedPreferences;
import com.braintreepayments.api.models.BraintreeRequestCodes;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.IdealBank;
import com.braintreepayments.api.models.IdealRequest;
import com.braintreepayments.api.models.IdealResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Used to integrate with iDEAL. For more information see the <a href="https://developers.braintreepayments.com/guides/ideal/overview">documentation</a>
 */
public class Ideal {

    private static final String ASSET_SERVER_REDIRECT_PATH = "/mobile/ideal-redirect/0.0.0/index.html?redirect_url=";

    protected static final String IDEAL_RESULT_ID = "com.braintreepayments.api.Ideal.IDEAL_RESULT_ID";
    protected static final int MIN_POLLING_RETRIES = 0;
    protected static final int MAX_POLLING_RETRIES = 10;
    protected static final int MIN_POLLING_DELAY = 1000;
    protected static final int MAX_POLLING_DELAY = 10000;

    /**
     * Makes a call to fetch the list of potential issuing banks with which a customer can pay.
     *
     * @param fragment {@link BraintreeFragment}
     * @param listener {@link BraintreeResponseListener} the callback to which a list of issuing banks will be provided.
     */
    public static void fetchIssuingBanks(final BraintreeFragment fragment,
            final BraintreeResponseListener<List<IdealBank>> listener) {
        fragment.waitForConfiguration(new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(final Configuration configuration) {
                Exception configException = checkIdealEnabled(configuration);
                if (configException != null) {
                    fragment.postCallback(configException);
                    return;
                }

                fragment.getBraintreeApiHttpClient().get("/issuers/ideal", new HttpResponseCallback() {
                    @Override
                    public void success(String responseBody) {
                        fragment.sendAnalyticsEvent("ideal.load.succeeded");
                        try {
                            List<IdealBank> banks = IdealBank.fromJson(configuration, responseBody);
                            if (listener != null) {
                                listener.onResponse(banks);
                            }
                        } catch (JSONException jsonException) {
                            failure(jsonException);
                        }
                    }

                    @Override
                    public void failure(Exception exception) {
                        fragment.sendAnalyticsEvent("ideal.load.failed");
                        fragment.postCallback(exception);
                    }
                });
            }
        });
    }

    /**
     * Initiates the payment flow by opening a browser where the customer can authenticate with their bank.
     *
     * @param fragment {@link BraintreeFragment}
     * @param builder {@link IdealRequest} with the payment details.
     * @param listener {@link BraintreeResponseListener} the callback to which the {@link IdealResult} will be sent
     * with a status of `PENDING` before the flow starts. This result contains the iDEAL payment ID.
     */
    public static void startPayment(final BraintreeFragment fragment, final IdealRequest builder,
            final BraintreeResponseListener<IdealResult> listener) {
        fragment.waitForConfiguration(new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(Configuration configuration) {
                fragment.sendAnalyticsEvent("ideal.start-payment.selected");

                Exception configException = checkIdealEnabled(configuration);
                if (configException != null) {
                    fragment.postCallback(configException);
                    fragment.sendAnalyticsEvent("ideal.start-payment.invalid-configuration");
                    return;
                }

                String redirectUrl = URI.create(configuration.getIdealConfiguration().getAssetsUrl() +
                        ASSET_SERVER_REDIRECT_PATH + fragment.getReturnUrlScheme() + "://").toString();

                fragment.getBraintreeApiHttpClient().post("/ideal-payments", builder.build(redirectUrl, configuration.getIdealConfiguration().getRouteId()),
                        new HttpResponseCallback() {
                            @Override
                            public void success(String responseBody) {
                                try {
                                    IdealResult idealResult = IdealResult.fromJson(responseBody);
                                    BraintreeSharedPreferences.putString(fragment.getApplicationContext(),
                                            IDEAL_RESULT_ID, idealResult.getId());
                                    if (listener != null) {
                                        listener.onResponse(idealResult);
                                    }

                                    JSONObject responseJson = new JSONObject(responseBody);
                                    String approvalUrl = responseJson.getJSONObject("data").getString("approval_url");

                                    fragment.browserSwitch(BraintreeRequestCodes.IDEAL, approvalUrl);
                                    fragment.sendAnalyticsEvent("ideal.webswitch.initiate.succeeded");
                                } catch (JSONException jsonException) {
                                    failure(jsonException);
                                }
                            }

                            @Override
                            public void failure(Exception exception) {
                                fragment.sendAnalyticsEvent("ideal.webswitch.initiate.failed");
                                fragment.postCallback(exception);
                            }
                        });
            }
        });
    }

    /**
     * Poll at a regular interval until the status of this payment has changed, or we exceed a specific number of retries.
     *
     * @param fragment {@link BraintreeFragment}.
     * @param idealId the ID of the iDEAL payment for which you'd like to check the status.
     * @param maxRetries the number of polling attempts. Must be between 0 and 10.
     * @param delay the number of milliseconds between polling attempts. Must be between 1000 and 10000.
     * @throws InvalidArgumentException If the `maxRetries` or `delay` are invalid.
     */
    public static void pollForCompletion(BraintreeFragment fragment, String idealId, int maxRetries, long delay) throws
            InvalidArgumentException {
        if (delay < MIN_POLLING_DELAY || delay > MAX_POLLING_DELAY ||
                maxRetries < MIN_POLLING_RETRIES || maxRetries > MAX_POLLING_RETRIES) {
            throw new InvalidArgumentException("Failed to begin polling: " +
                    "retries must be between" + MIN_POLLING_RETRIES + " and " + MAX_POLLING_RETRIES +
                    ", delay must be between" + MIN_POLLING_DELAY + " and " + MAX_POLLING_DELAY + ".\n");
        }
        pollForCompletion(fragment, idealId, maxRetries, delay, 0);
    }

    private static void pollForCompletion(final BraintreeFragment fragment, final String id, final int maxRetries, final long delay, final int retryCount) {
        checkTransactionStatus(fragment, id, new IdealStatusListener() {
            @Override
            public void onSuccess(IdealResult result) {
                String status = result.getStatus();
                if ("COMPLETE".equals(status)) {
                    fragment.postCallback(result);
                } else if ("PENDING".equals(status) && retryCount < maxRetries) {
                    Executors.newSingleThreadScheduledExecutor().schedule(new Runnable() {
                        @Override
                        public void run() {
                            pollForCompletion(fragment, id, maxRetries, delay, retryCount + 1);
                        }
                    }, delay, TimeUnit.MILLISECONDS);
                } else {
                    fragment.postCallback(result);
                }
            }

            @Override
            public void onFailure(Exception error) {
                fragment.postCallback(error);
            }
        });
    }

    static void onActivityResult(final BraintreeFragment fragment, int resultCode) {
        if (resultCode == Activity.RESULT_OK) {
            fragment.sendAnalyticsEvent("ideal.webswitch.succeeded");

            String idealResultId = BraintreeSharedPreferences.getString(fragment.getApplicationContext(),
                    IDEAL_RESULT_ID);
            BraintreeSharedPreferences.remove(fragment.getApplicationContext(), IDEAL_RESULT_ID);

            checkTransactionStatus(fragment, idealResultId, new IdealStatusListener() {
                @Override
                public void onSuccess(IdealResult result) {
                    fragment.postCallback(result);
                }

                @Override
                public void onFailure(Exception throwable) {
                    fragment.postCallback(throwable);
                }
            });
        } else if (resultCode == Activity.RESULT_CANCELED) {
            fragment.sendAnalyticsEvent("ideal.webswitch.canceled");
        }
    }

    private static void checkTransactionStatus(final BraintreeFragment fragment, String resultId, final IdealStatusListener listener) {
        fragment.getBraintreeApiHttpClient().get(String.format("/ideal-payments/%s/status", resultId), new HttpResponseCallback() {
            @Override
            public void success(String responseBody) {
                try {
                    final IdealResult fetchedNonce = IdealResult.fromJson(responseBody);
                    listener.onSuccess(fetchedNonce);
                } catch (JSONException e) {
                    listener.onFailure(e);
                }
            }

            @Override
            public void failure(Exception exception) {
                listener.onFailure(exception);
            }
        });
    }

    private static ConfigurationException checkIdealEnabled(Configuration configuration) {
        if (!configuration.getBraintreeApiConfiguration().isEnabled()) {
            return new ConfigurationException("Your access is restricted and cannot use this part of the Braintree API.");
        } else if (!configuration.getIdealConfiguration().isEnabled()) {
            return new ConfigurationException("iDEAL is not enabled for this merchant.");
        }

        return null;
    }

    private interface IdealStatusListener {
        void onSuccess(IdealResult result);
        void onFailure(Exception throwable);
    }
}
