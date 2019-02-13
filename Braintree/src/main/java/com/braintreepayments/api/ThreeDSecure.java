package com.braintreepayments.api;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.braintreepayments.api.exceptions.BraintreeException;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.ConfigurationListener;
import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCallback;
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
import com.braintreepayments.api.models.ThreeDSecurePostalAddress;
import com.braintreepayments.api.models.ThreeDSecureRequest;
import com.cardinalcommerce.cardinalmobilesdk.Cardinal;
import com.cardinalcommerce.cardinalmobilesdk.models.response.InitResponse;
import com.cardinalcommerce.cardinalmobilesdk.models.response.ValidateResponse;
import com.cardinalcommerce.cardinalmobilesdk.services.CardinalInitService;
import com.cardinalcommerce.cardinalmobilesdk.services.CardinalReceiver;
import com.cardinalcommerce.cardinalmobilesdk.services.CardinalValidateReceiver;
import com.cardinalcommerce.shared.models.challenge.StepUpData;
import com.cardinalcommerce.shared.models.enums.DirectoryServerID;
import com.cardinalcommerce.shared.models.parameters.CardinalConfigurationParameters;
import com.cardinalcommerce.shared.models.parameters.CardinalEnvironment;
import com.cardinalcommerce.shared.models.parameters.CardinalRenderType;
import com.cardinalcommerce.shared.models.parameters.CardinalUiType;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import androidx.appcompat.app.AppCompatActivity;

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
    private static String mDFReferenceId;

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

                fragment.getHttpClient().post(TokenizationClient.versionedPath(
                        TokenizationClient.PAYMENT_METHOD_ENDPOINT + "/" + request.getNonce() +
                                "/three_d_secure/lookup"), request.build(mDFReferenceId), new HttpResponseCallback() {
                    @Override
                    public void success(String responseBody) {
                        try {
                            final ThreeDSecureLookup threeDSecureLookup = ThreeDSecureLookup.fromJson(responseBody);
                            if (threeDSecureLookup.getAcsUrl() != null) {
                                if (threeDSecureLookup.isThreeDSecureVersion2()) {
                                    performCardinalAuthentication(fragment, threeDSecureLookup);
                                } else {
                                    launchBrowserSwitch(fragment, threeDSecureLookup);
                                }
                            } else {
                                fragment.postCallback(threeDSecureLookup.getCardNonce());
                            }
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

    protected static void performCardinalAuthentication(final BraintreeFragment fragment, final ThreeDSecureLookup threeDSecureLookup) {
        Cardinal.getInstance().cca_continue(threeDSecureLookup.getTransactionId(),
                threeDSecureLookup.getPareq(),
                threeDSecureLookup.getAcsUrl(),
                DirectoryServerID.VISA01, // TODO: Does this matter?
                fragment.getActivity(),
                new CardinalValidateReceiver() {
                    @Override
                    public void onValidated(Context currentContext, ValidateResponse validateResponse, String serverJWT) {
                        switch (validateResponse.getActionCode()) {
                            case FAILURE:
                            case SUCCESS:
                            case NOACTION:
                                authenticateCardinalJWT(fragment, threeDSecureLookup, serverJWT);
                                break;

                            case ERROR:
                                fragment.postCallback(new BraintreeException(validateResponse.errorDescription));
                                break;
                            case CANCEL:
                                fragment.postCancelCallback(BraintreeRequestCodes.THREE_D_SECURE);
                                break;

                        }
                    }
                });
    }

    protected static void authenticateCardinalJWT(final BraintreeFragment fragment, final ThreeDSecureLookup threeDSecureLookup, final String cardinalJWT) {
        final String nonce = threeDSecureLookup.getCardNonce().getNonce();
        JSONObject body = new JSONObject();
        try {
            body.put("jwt", cardinalJWT);
            body.put("paymentMethodNonce", nonce);
        } catch (JSONException exception) {
            fragment.postCallback(exception);
        }

        fragment.getHttpClient().post(TokenizationClient.versionedPath(
                TokenizationClient.PAYMENT_METHOD_ENDPOINT + "/" + nonce +
                        "/three_d_secure/authenticate_from_jwt"), body.toString(), new HttpResponseCallback() {
            @Override
            public void success(String responseBody) {
                Log.d("Response: ", responseBody);
                    ThreeDSecureAuthenticationResponse authenticationResponse = ThreeDSecureAuthenticationResponse.fromJson(responseBody);
                    if (authenticationResponse.getErrors() != null) {
                        // TODO: This isn't a GraphQL request, but the response uses GraphQL style errors. How do we want to parse them?
                        fragment.postCallback(ErrorWithResponse.fromGraphQLJson(authenticationResponse.getErrors()));
                    }
                    else {
                        fragment.postCallback(authenticationResponse.getCardNonce());
                    }
            }

            @Override
            public void failure(Exception exception) {
                fragment.postCallback(exception);
            }
        });
    }

    protected static void onActivityResult(BraintreeFragment fragment, int resultCode, Intent data) {
        if (resultCode == AppCompatActivity.RESULT_OK) {
            Uri resultUri = data.getData();
            ThreeDSecureAuthenticationResponse authenticationResponse = ThreeDSecureAuthenticationResponse
                    .fromJson(resultUri.getQueryParameter("auth_response"));

            if (authenticationResponse.isSuccess()) {
                fragment.postCallback(authenticationResponse.getCardNonce());
            } else if (authenticationResponse.getException() != null) {
                fragment.postCallback(new BraintreeException(authenticationResponse.getException()));
            } else {
                fragment.postCallback(new ErrorWithResponse(422, authenticationResponse.getErrors()));
            }
        }
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
                    CardinalEnvironment cardinalEnvironment = CardinalEnvironment.PRODUCTION;
                    switch (configuration.getEnvironment().toLowerCase()) {
                        case "production":
                            cardinalEnvironment = CardinalEnvironment.PRODUCTION;
                            break;
                        default:
                            cardinalEnvironment = CardinalEnvironment.STAGING;
                            break;
                    }

                    CardinalConfigurationParameters cardinalConfigurationParameters = new CardinalConfigurationParameters();
                    cardinalConfigurationParameters.setEnvironment(cardinalEnvironment);
                    // TODO what should timeout and "quick auth" be
                    cardinalConfigurationParameters.setTimeout(8000);
                    cardinalConfigurationParameters.setEnableQuickAuth(false);

                    // TODO what is an rType
                    JSONArray rType = new JSONArray();
                    rType.put(CardinalRenderType.OTP);
                    rType.put(CardinalRenderType.SINGLE_SELECT);
                    rType.put(CardinalRenderType.MULTI_SELECT);
                    rType.put(CardinalRenderType.OOB);
                    rType.put(CardinalRenderType.HTML);
                    cardinalConfigurationParameters.setRenderType(rType);

                    // TODO what UI type should we use
                    cardinalConfigurationParameters.setUiType(CardinalUiType.BOTH);

                    Cardinal cardinal = Cardinal.getInstance();
                    cardinal.configure(fragment.getApplicationContext(), cardinalConfigurationParameters);
                    cardinal.init(configuration.getCardinalAuthenticationJwt(), new CardinalInitService() {
                        @Override
                        public void onSetupCompleted(String consumerSessionId) {
                            mDFReferenceId = consumerSessionId;
                        }

                        @Override
                        public void onValidated(ValidateResponse validateResponse, String serverJwt) {
                            Log.d("ERROR", "Cardinal could not be initialized");
                        }
                    });
                }
            });
        }
    }
}
