package com.braintreepayments.api;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Base64;

import com.braintreepayments.api.annotations.Beta;
import com.braintreepayments.api.data.BraintreeData;
import com.braintreepayments.api.data.BraintreeEnvironment;
import com.braintreepayments.api.exceptions.AppSwitchNotAvailableException;
import com.braintreepayments.api.exceptions.BraintreeException;
import com.braintreepayments.api.exceptions.ConfigurationException;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.exceptions.ServerException;
import com.braintreepayments.api.exceptions.UnexpectedException;
import com.braintreepayments.api.internal.HttpRequest;
import com.braintreepayments.api.internal.HttpResponse;
import com.braintreepayments.api.models.AnalyticsRequest;
import com.braintreepayments.api.models.AndroidPayCard;
import com.braintreepayments.api.models.ClientToken;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.PayPalAccount;
import com.braintreepayments.api.models.PayPalAccountBuilder;
import com.braintreepayments.api.models.PayPalPaymentResource;
import com.braintreepayments.api.models.PaymentMethod;
import com.braintreepayments.api.models.ThreeDSecureLookup;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.wallet.Cart;
import com.google.android.gms.wallet.FullWallet;
import com.google.android.gms.wallet.PaymentMethodTokenizationParameters;
import com.google.android.gms.wallet.WalletConstants;
import com.google.gson.Gson;
import com.paypal.android.sdk.onetouch.core.PayPalOneTouchCore;
import com.paypal.android.sdk.onetouch.core.PerformRequestStatus;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Synchronous communication with the Braintree gateway. Consumed by {@link Braintree}.
 * Unless synchronous behavior is needed, we recommend using {@link Braintree}.
 *
 * @see Braintree
 */
public class BraintreeApi {

    private static final String PAYMENT_METHOD_ENDPOINT = "payment_methods";

    private Context mContext;
    private ClientToken mClientToken;
    private Configuration mConfiguration;
    private HttpRequest mHttpRequest;

    private VenmoAppSwitch mVenmoAppSwitch;
    private AndroidPay mAndroidPay;
    private Object mBraintreeData;

    /**
     * @deprecated Interactions should be done using {@link com.braintreepayments.api.Braintree}
     * instead.
     *
     * Initialize a BraintreeApi instance to communicate with Braintree.
     *
     * @param context
     * @param clientTokenString A client token obtained from a Braintree server side SDK.
     */
    @Deprecated
    public BraintreeApi(Context context, String clientTokenString) {
        Pattern pattern = Pattern.compile("([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{4}|[A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)");
        if (pattern.matcher(clientTokenString).matches()) {
            clientTokenString = new String(Base64.decode(clientTokenString, Base64.DEFAULT));
        }

        mClientToken = ClientToken.fromString(clientTokenString);

        mContext = context.getApplicationContext();
        mConfiguration = Configuration.fromJson(clientTokenString);
        mHttpRequest = new HttpRequest(mClientToken.getAuthorizationFingerprint());
        mHttpRequest.setBaseUrl(mConfiguration.getClientApiUrl());

        mBraintreeData = null;
        mVenmoAppSwitch = new VenmoAppSwitch(context, mConfiguration);
    }

    protected BraintreeApi(Context context, ClientToken clientToken) {
        mContext = context.getApplicationContext();
        mClientToken = clientToken;
        mHttpRequest = new HttpRequest(mClientToken.getAuthorizationFingerprint());
    }

    protected BraintreeApi(Context context, String clientTokenString, String configurationString) {
        this(context, ClientToken.fromString(clientTokenString),
                Configuration.fromJson(configurationString),
                new HttpRequest(ClientToken.fromString(clientTokenString).getAuthorizationFingerprint()));
    }

    protected BraintreeApi(Context context, ClientToken clientToken, Configuration configuration,
            HttpRequest requestor) {
        mContext = context.getApplicationContext();
        mClientToken = clientToken;
        mConfiguration = configuration;
        mHttpRequest = requestor;

        mBraintreeData = null;
        mVenmoAppSwitch = new VenmoAppSwitch(mContext, mConfiguration);
    }

    protected boolean isSetup() {
        return mConfiguration != null;
    }

    protected void setup() throws ErrorWithResponse, BraintreeException {
        mConfiguration = getConfiguration();
        mHttpRequest.setBaseUrl(mConfiguration.getClientApiUrl());

        mBraintreeData = null;
        mVenmoAppSwitch = new VenmoAppSwitch(mContext, mConfiguration);
    }

    private Configuration getConfiguration() throws ErrorWithResponse, BraintreeException {
        String configUrl = Uri.parse(mClientToken.getConfigUrl())
                .buildUpon()
                .appendQueryParameter("configVersion", "3")
                .build()
                .toString();
        HttpResponse response = mHttpRequest.get(configUrl);
        return Configuration.fromJson(response.getResponseBody());
    }

    protected String getConfigurationString() {
        if (mConfiguration != null) {
            return new Gson().toJson(mConfiguration);
        } else {
            return null;
        }
    }

    /**
     * @deprecated See {@link Braintree#isPayPalEnabled()}
     *
     * @return If PayPal is supported and enabled in the current environment.
     */
    @Deprecated
    public boolean isPayPalEnabled() {
        return mConfiguration.isPayPalEnabled();
    }

    /**
     * @deprecated See {@link Braintree#isVenmoEnabled()}
     *
     * @return If Venmo app switch is supported and enabled in the current environment.
     */
    @Deprecated
    public boolean isVenmoEnabled() {
        return mVenmoAppSwitch.isAvailable();
    }

    protected boolean isAndroidPayEnabled() {
        try {
            return (mConfiguration.getAndroidPay().isEnabled() &&
                    GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(mContext) == ConnectionResult.SUCCESS);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * @deprecated See {@link Braintree#isThreeDSecureEnabled()}
     *
     * @return If 3D Secure is supported and enabled for the current merchant account.
     */
    @Deprecated
    public boolean isThreeDSecureEnabled() {
        return mConfiguration.isThreeDSecureEnabled();
    }

    /**
     * @deprecated See {@link Braintree#isCvvChallenegePresent()}
     *
     * @return If cvv is required to add a card.
     */
    @Deprecated
    public boolean isCvvChallengePresent() {
        return mConfiguration.isCvvChallengePresent();
    }

    /**
     * @deprecated See {@link Braintree#isPostalCodeChallengePresent()}
     *
     * @return If postal code is required to add a card.
     */
    @Deprecated
    public boolean isPostalCodeChallengePresent() {
        return mConfiguration.isPostalCodeChallengePresent();
    }

    /**
     * Start the Pay With PayPal flow. This will launch a new activity for the PayPal mobile SDK.
     *
     * @param activity The {@link android.app.Activity} to receive {@link android.app.Activity#onActivityResult(int, int, android.content.Intent)}
     *   when {@link #startPayWithPayPal(android.app.Activity, int, java.util.List)} finishes.
     * @param requestCode The request code associated with this start request. Will be returned in
     * @param additionalScopes A {@link java.util.List} of additional scopes. Ex: 'address'
     * {@link android.app.Activity#onActivityResult(int, int, android.content.Intent)}
     */
    public PerformRequestStatus startPayWithPayPal(Activity activity, int requestCode, List<String> additionalScopes) throws ConfigurationException {
        return PayPal.launchPayPal(activity, requestCode, mConfiguration, mClientToken, additionalScopes);
    }

    /**
     * Start the Checkout With PayPal flow. This will create a payment resource with the specified
     * amount and launch a new activity for the PayPal mobile SDK.
     *
     * @param activity The {@link android.app.Activity} to receive {@link
     * android.app.Activity#onActivityResult(int, int, android.content.Intent)} when {@link
     * #startPayWithPayPal(android.app.Activity, int, java.util.List)} finishes.
     * @param requestCode The request code associated with this start request. Will be returned in
     * {@link android.app.Activity#onActivityResult(int, int, android.content.Intent)}
     * @param payPalPaymentResource The {@link PayPalPaymentResource} containing the URL for the app switch
     *
     */
    public PerformRequestStatus startCheckoutWithPayPal(final Activity activity,
            final int requestCode, final PayPalPaymentResource payPalPaymentResource)
            throws ConfigurationException {

        return PayPal.checkoutWithPayPal(payPalPaymentResource, activity, requestCode,
                mConfiguration);
    }

    /**
     * Create a PayPalPaymentResource on behalf of the merchant. To be used in the PayPal Checkout
     * flow.
     *
     * @param checkout A {@link PayPalCheckout} object containing an amount.
     * @param activity The {@link android.app.Activity} to receive {@link android.app.Activity#onActivityResult(int, int, android.content.Intent)}
     * when {@link #startPayWithVenmo(android.app.Activity, int)} finishes.x
     * @return A {@link com.braintreepayments.api.models.PayPalPaymentResource} containing the redurectUrl
     * which will be used by {@link com.paypal.android.sdk.onetouch.core.PayPalOneTouchCore} for
     * user authorization.
     */
    public PayPalPaymentResource createPayPalPaymentResource(PayPalCheckout checkout,
            Activity activity)
            throws JSONException, ErrorWithResponse, BraintreeException {

        String returnUri =
                PayPal.buildPayPalCheckoutConfiguration(null, activity, mConfiguration)
                        .getSuccessUrl();
        String cancelUri =
                PayPal.buildPayPalCheckoutConfiguration(null, activity, mConfiguration)
                        .getCancelUrl();

        String currencyCode = checkout.getCurrencyCode();
        if (currencyCode == null) {
            currencyCode = mConfiguration.getPayPal().getCurrencyIsoCode();
        }

        JSONObject experienceProfile = new JSONObject();
        experienceProfile.put("no_shipping", !checkout.getEnableShippingAddress());
        experienceProfile.put("address_override", checkout.getAddressOverride());

        if (checkout.getLocaleCode() != null) {
            experienceProfile.put("locale_code", checkout.getLocaleCode());
        }

        JSONObject parameters = new JSONObject()
                .put("authorization_fingerprint", mClientToken.getAuthorizationFingerprint())
                .put("amount", checkout.getAmount().toString())
                .put("currency_iso_code", currencyCode)
                .put("return_url", returnUri)
                .put("cancel_url", cancelUri)
                .put("experience_profile", experienceProfile)
                .put("correlation_id", ""); // don't send clientMetadataId when creating the payment resource

        if (checkout.getAddressOverride() && checkout.getShippingAddress() != null) {
            parameters.put("line1", checkout.getShippingAddress().getStreetAddress());
            parameters.put("line2", checkout.getShippingAddress().getExtendedAddress());
            parameters.put("city", checkout.getShippingAddress().getLocality());
            parameters.put("state", checkout.getShippingAddress().getRegion());
            parameters.put("postal_code", checkout.getShippingAddress().getPostalCode());
            parameters.put("country_code", checkout.getShippingAddress().getCountryCodeAlpha2());
            parameters.put("recipient_name", checkout.getShippingAddress().getRecipientName());
        }

        HttpResponse response = mHttpRequest.post(
                versionedPath("paypal_hermes/create_payment_resource"),
                parameters.toString());

        return PayPalPaymentResource.fromJson(response.getResponseBody());

    }


    /**
     * Start the Pay With Venmo flow. This will app switch to the Venmo app.
     * @param activity The {@link android.app.Activity} to receive {@link android.app.Activity#onActivityResult(int, int, android.content.Intent)}
     * when {@link #startPayWithVenmo(android.app.Activity, int)} finishes.x
     * @param requestCode The request code associated with this start request. Will be returned in
     * {@link android.app.Activity#onActivityResult(int, int, android.content.Intent)}
     * @throws com.braintreepayments.api.exceptions.AppSwitchNotAvailableException If the Venmo app is
     * not available.
     */
    public void startPayWithVenmo(Activity activity, int requestCode)
            throws AppSwitchNotAvailableException {
        mVenmoAppSwitch.launch(activity, requestCode);
    }

    protected PaymentMethodTokenizationParameters getAndroidPayTokenizationParameters() {
        if (mAndroidPay == null) {
            mAndroidPay = new AndroidPay(mConfiguration);
        }

        return mAndroidPay.getTokenizationParameters();
    }

    protected void performAndroidPayMaskedWalletRequest(Activity activity, int requestCode, Cart cart,
            boolean isBillingAgreement, boolean shippingAddressRequired,
            boolean phoneNumberRequired) throws InvalidArgumentException, UnexpectedException {
        if (isBillingAgreement && cart != null) {
            throw new InvalidArgumentException("The cart must be null when isBillingAgreement is true");
        } else if(!isBillingAgreement && cart == null) {
            throw new InvalidArgumentException("Cart cannot be null unless isBillingAgreement is true");
        }

        if (mAndroidPay == null) {
            mAndroidPay = new AndroidPay(mConfiguration);
        }

        mAndroidPay.setCart(cart);
        mAndroidPay.performMaskedWalletRequest(activity, requestCode, isBillingAgreement,
                shippingAddressRequired, phoneNumberRequired);
    }

    protected void performAndroidPayChangeMaskedWalletRequest(Activity activity, int requestCode,
            String googleTransactionId) throws UnexpectedException {
        if (mAndroidPay == null) {
            mAndroidPay = new AndroidPay(mConfiguration);
        }

        mAndroidPay.performChangeMaskedWalletRequest(activity, requestCode, googleTransactionId);
    }

    protected void performAndroidPayFullWalletRequest(Activity activity, int requestCode, Cart cart,
            String googleTransactionId) throws UnexpectedException {
        if (mAndroidPay == null) {
            mAndroidPay = new AndroidPay(mConfiguration);
        }

        if (cart != null) {
            mAndroidPay.setCart(cart);
        }

        mAndroidPay.performFullWalletRequest(activity, requestCode, googleTransactionId);
    }

    protected void disconnectGoogleApiClient() {
        if (mAndroidPay != null) {
            mAndroidPay.disconnect();
        }
    }

    /**
     * Handles response from PayPal and returns a PayPalAccountBuilder which must be then passed to
     * {@link #create(com.braintreepayments.api.models.PaymentMethod.Builder)}. {@link #finishPayWithPayPal(android.app.Activity, int, android.content.Intent)}
     * will call this and {@link #create(com.braintreepayments.api.models.PaymentMethod.Builder)} for you
     * and may be a better option.
     * @param activity The activity that received the result.
     * @param resultCode The result code provided in {@link android.app.Activity#onActivityResult(int, int, android.content.Intent)}
     * @param data The {@link android.content.Intent} provided in {@link android.app.Activity#onActivityResult(int, int, android.content.Intent)}
     * @return {@link com.braintreepayments.api.models.PayPalAccountBuilder} ready to be sent to {@link #create(com.braintreepayments.api.models.PaymentMethod.Builder)}
     * @throws ConfigurationException If PayPal credentials from the Braintree control panel are incorrect.
     */
    public PayPalAccountBuilder handlePayPalResponse(Activity activity, int resultCode, Intent data)
            throws ConfigurationException {
        return PayPal.getBuilderFromActivity(activity, resultCode, data);
    }

    /**
     * @deprecated Use {@link com.braintreepayments.api.BraintreeApi#finishPayWithPayPal(android.app.Activity, int, android.content.Intent)}
     * instead.
     *
     * This method should *not* be used, it does not include a Application Correlation ID.
     * PayPal uses the Application Correlation ID to verify that the payment is originating from
     * a valid, user-consented device+application. This helps reduce fraud and decrease declines.
     * PayPal does not provide any loss protection for transactions that do not correctly supply
     * an Application Correlation ID.
     *
     * @param resultCode The result code provided in {@link android.app.Activity#onActivityResult(int, int, android.content.Intent)}
     * @param data The {@link android.content.Intent} provided in {@link android.app.Activity#onActivityResult(int, int, android.content.Intent)}
     * @return The {@link com.braintreepayments.api.models.PaymentMethod} created from a PayPal account
     * @throws ErrorWithResponse If creation fails validation
     * @throws BraintreeException If an error not due to validation (server error, network issue, etc.) occurs
     * @throws ConfigurationException If PayPal credentials from the Braintree control panel are incorrect.
     *
     * @see BraintreeApi#create(com.braintreepayments.api.models.PaymentMethod.Builder)
     */
    @Deprecated
    public PayPalAccount finishPayWithPayPal(int resultCode, Intent data)
            throws BraintreeException, ErrorWithResponse, JSONException {
        PayPalAccountBuilder payPalAccountBuilder = handlePayPalResponse(null, resultCode, data);
        if (payPalAccountBuilder != null) {
            return create(payPalAccountBuilder);
        } else {
            return null;
        }
    }

    /**
     * @deprecated Use {@link Braintree#finishPayWithPayPal(Activity, int, Intent)} instead.
     *
     * @param activity The calling activity
     * @param resultCode The result code provided in {@link android.app.Activity#onActivityResult(int, int, android.content.Intent)}
     * @param data The {@link android.content.Intent} provided in {@link android.app.Activity#onActivityResult(int, int, android.content.Intent)}
     * @return The {@link com.braintreepayments.api.models.PaymentMethod} created from a PayPal account
     * @throws ErrorWithResponse If creation fails validation
     * @throws BraintreeException If an error not due to validation (server error, network issue, etc.) occurs
     * @throws ConfigurationException If PayPal credentials from the Braintree control panel are incorrect
     *
     * @see BraintreeApi#create(com.braintreepayments.api.models.PaymentMethod.Builder)
     */
    @Deprecated
    public PayPalAccount finishPayWithPayPal(Activity activity, int resultCode, Intent data)
            throws BraintreeException, ErrorWithResponse, JSONException {

        PayPalAccountBuilder payPalAccountBuilder = handlePayPalResponse(activity, resultCode, data);
        if (payPalAccountBuilder != null) {
            return create(payPalAccountBuilder);
        } else {
            return null;
        }
    }

    /**
     * Handles response from Venmo app after One Touch app switch.
     * @param resultCode The result code provided in {@link android.app.Activity#onActivityResult(int, int, android.content.Intent)}
     * @param data The {@link android.content.Intent} provided in {@link android.app.Activity#onActivityResult(int, int, android.content.Intent)}
     * @return The nonce representing the Venmo payment method.
     */
    public String finishPayWithVenmo(int resultCode, Intent data) {
        return mVenmoAppSwitch.handleAppSwitchResponse(resultCode, data);
    }

    protected AndroidPayCard getNonceFromAndroidPayFullWalletResponse(Intent data) throws JSONException {
        if (AndroidPay.isFullWalletResponse(data)) {
            FullWallet fullWallet = data.getParcelableExtra(WalletConstants.EXTRA_FULL_WALLET);
            JSONArray androidPayCards = new JSONObject(fullWallet.getPaymentMethodToken().getToken())
                    .getJSONArray("androidPayCards");
            if (androidPayCards.length() > 0) {
                return new Gson().fromJson(androidPayCards.getString(0), AndroidPayCard.class);
            }
        }

        return null;
    }

    /**
     * Create a {@link com.braintreepayments.api.models.PaymentMethod} in the Braintree Gateway.
     *
     * @param paymentMethodBuilder {@link com.braintreepayments.api.models.PaymentMethod.Builder} for the
     * {@link com.braintreepayments.api.models.PaymentMethod} to be created.
     * @param <T> {@link com.braintreepayments.api.models.PaymentMethod} or a subclass.
     * @return {@link com.braintreepayments.api.models.PaymentMethod}
     * @throws ErrorWithResponse If creation fails validation
     * @throws BraintreeException If an error not due to validation (server error, network issue, etc.) occurs
     *
     * @see BraintreeApi#tokenize(com.braintreepayments.api.models.PaymentMethod.Builder)
     */
    public <T extends PaymentMethod> T create(PaymentMethod.Builder<T> paymentMethodBuilder)
            throws ErrorWithResponse, BraintreeException {

        HttpResponse response = mHttpRequest.post(
                versionedPath(PAYMENT_METHOD_ENDPOINT + "/" + paymentMethodBuilder.getApiPath()),
                paymentMethodBuilder.toJsonString());

        return paymentMethodBuilder.fromJson(jsonForType(response.getResponseBody(),
                paymentMethodBuilder.getApiResource()));
    }

    /**
     * Tokenize a {@link com.braintreepayments.api.models.PaymentMethod} with the Braintree gateway.
     *
     * Tokenization functions like creating a {@link com.braintreepayments.api.models.PaymentMethod}, but
     * defers validation until a server library attempts to use the {@link com.braintreepayments.api.models.PaymentMethod}.
     * Use {@link #tokenize(com.braintreepayments.api.models.PaymentMethod.Builder)} to handle validation errors
     * on the server instead of on device.
     *
     * @param paymentMethodBuilder The {@link com.braintreepayments.api.models.PaymentMethod.Builder} to tokenize
     * @return A nonce that can be used by a server library to create a transaction with the Braintree gateway.
     * @throws BraintreeException
     * @throws ErrorWithResponse
     * @see #create(com.braintreepayments.api.models.PaymentMethod.Builder)
     */
    public String tokenize(PaymentMethod.Builder paymentMethodBuilder)
            throws BraintreeException, ErrorWithResponse, JSONException {
        PaymentMethod paymentMethod = create(paymentMethodBuilder.validate(false));
        return paymentMethod.getNonce();
    }

    /**
     * 3D Secure is a protocol that enables cardholders and issuers to add a layer of security
     * to e-commerce transactions via password entry at checkout.
     *
     * One of the primary reasons to use 3D Secure is to benefit from a shift in liability from the
     * merchant to the issuer, which may result in interchange savings. Please read our online
     * documentation (<a href="https://developers.braintreepayments.com">https://developers.braintreepayments.com</a>)
     * for a full explanation of 3D Secure.
     *
     * Verification is associated with a transaction amount and your merchant account. To specify a
     * different merchant account (or, in turn, currency), you will need to specify the merchant
     * account id when generating a client token
     * (See <a href="https://developers.braintreepayments.com/android/sdk/overview/generate-client-token">https://developers.braintreepayments.com/android/sdk/overview/generate-client-token</a>).
     *
     * During lookup the original payment method nonce is consumed and a new one is returned,
     * which points to the original payment method, as well as the 3D Secure verification.
     * Transactions created with this nonce will be 3D Secure, and benefit from the appropriate
     * liability shift if authentication is successful or fail with a 3D Secure failure.
     *
     * @param nonce The nonce that represents a card to perform a 3D Secure verification against
     * @param amount The amount of the transaction in the current merchant account's currency
     * @return {@code null} if user authentication is required and an {@link android.app.Activity}
     *         was launched to acquire authentication, or a {@link com.braintreepayments.api.models.Card}
     *         that can be used immediately and benefits from the appropriate liability shift of
     *         3D Secure.
     * @throws JSONException If building the request fails
     * @throws BraintreeException If the request to the Braintree Gateway fails
     * @throws ErrorWithResponse If there is an error with the request
     */
    @Beta
    public ThreeDSecureLookup threeDSecureLookup(String nonce, String amount)
            throws JSONException, BraintreeException, ErrorWithResponse {
        JSONObject params = new JSONObject()
                .put("merchantAccountId", mConfiguration.getMerchantAccountId())
                .put("amount", amount);

        HttpResponse response = mHttpRequest.post(
                versionedPath(PAYMENT_METHOD_ENDPOINT + "/" + nonce + "/three_d_secure/lookup"),
                params.toString());

        return ThreeDSecureLookup.fromJson(response.getResponseBody());
    }

    /**
     * @return A {@link java.util.List} of {@link com.braintreepayments.api.models.PaymentMethod}s for this
     *   client token.
     * @throws ErrorWithResponse When a recoverable validation error occurs.
     * @throws BraintreeException When a non-recoverable error (authentication, server error, network, etc.) occurs.
     */
    public List<PaymentMethod> getPaymentMethods() throws ErrorWithResponse, BraintreeException {
        HttpResponse response = mHttpRequest.get(versionedPath(PAYMENT_METHOD_ENDPOINT));
        return PaymentMethod.parsePaymentMethods(response.getResponseBody());
    }

    protected PaymentMethod getPaymentMethod(String nonce)
            throws ErrorWithResponse, BraintreeException, JSONException {
        HttpResponse response = mHttpRequest.get(versionedPath(
                PAYMENT_METHOD_ENDPOINT + "/" + nonce));

        List<PaymentMethod> paymentMethodsList = PaymentMethod.parsePaymentMethods(response.getResponseBody());
        if (paymentMethodsList.size() == 1) {
            return paymentMethodsList.get(0);
        } else if (paymentMethodsList.size() > 1) {
            throw new ServerException("Expected one payment method, got multiple payment methods");
        } else {
            throw new ServerException("No payment methods were found for nonce");
        }
    }

    /**
     * Sends analytics event to send to the Braintree analytics service. Used internally and by Drop-In.
     * @param event Name of event to be sent.
     * @param integrationType The type of integration used. Should be "custom" for those directly
     * using {@link Braintree} or {@link BraintreeApi} without Drop-In
     */
    public void sendAnalyticsEvent(String event, String integrationType) {
        if (mConfiguration.isAnalyticsEnabled()) {
            AnalyticsRequest analyticsRequest = new AnalyticsRequest(mContext, event, integrationType);

            try {
                mHttpRequest.post(mConfiguration.getAnalytics().getUrl(), analyticsRequest.toJson());
            } catch (BraintreeException ignored) {
                // Analytics failures should not interrupt normal application activity
            } catch (ErrorWithResponse ignored) {
                // Analytics failures should not interrupt normal application activity
            }
        }
    }

    /**
     * Collect device information for fraud identification purposes.
     *
     * @param activity The currently visible activity.
     * @param environment The Braintree environment to use.
     * @return device_id String to send to Braintree.
     * @see com.braintreepayments.api.data.BraintreeData
     */
    public String collectDeviceData(Activity activity, BraintreeEnvironment environment) {
        return collectDeviceData(activity, environment.getMerchantId(),
                environment.getCollectorUrl());
    }

    /**
     * Collect device information for fraud identification purposes. This should be used in conjunction
     * with a non-aggregate fraud id.
     *
     * @param activity The currently visible activity.
     * @param merchantId The fraud merchant id from Braintree.
     * @param collectorUrl The fraud collector url from Braintree.
     * @return device_id String to send to Braintree.
     * @see com.braintreepayments.api.data.BraintreeData
     */
    public String collectDeviceData(Activity activity, String merchantId, String collectorUrl) {
        String deviceData;
        try {
            mBraintreeData = new BraintreeData(activity, merchantId, collectorUrl);
            deviceData = ((BraintreeData) mBraintreeData).collectDeviceData();
        } catch (NoClassDefFoundError e) {
            deviceData = "{\"correlation_id\":\"" +
                    PayPalOneTouchCore.getClientMetadataId(activity) + "\"}";
        }

        return deviceData;
    }

    private String versionedPath(String path) {
        return "/v1/" + path;
    }

    private String jsonForType(String response, String type) throws ServerException {
        JSONObject responseJson;
        try {
            responseJson = new JSONObject(response);
            return responseJson.getJSONArray(type)
                    .get(0).toString();
        } catch (JSONException e) {
            throw new ServerException("Parsing server response failed");
        }
    }
}
