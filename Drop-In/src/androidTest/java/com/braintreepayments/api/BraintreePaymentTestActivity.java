package com.braintreepayments.api;

import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.internal.BraintreeHttpClient;
import com.braintreepayments.api.models.Authorization;
import com.braintreepayments.api.models.CardBuilder;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.test.BraintreeTestHttpClient;

import static org.mockito.Mockito.spy;

public class BraintreePaymentTestActivity extends BraintreePaymentActivity {

    public static final String EXTRA_DELAY = "delay";
    public static final String MOCK_CONFIGURATION = "mock_configuration";
    public static final String CONFIGURATION_ERROR = "configuration_error";
    public static final String GET_PAYMENT_METHODS = TokenizationClient
            .versionedPath(TokenizationClient.PAYMENT_METHOD_ENDPOINT);
    public static final String GET_PAYMENT_METHODS_ERROR = "get_payment_methods_error";
    public static final String TOKENIZE_CREDIT_CARD = TokenizationClient.versionedPath(
            TokenizationClient.PAYMENT_METHOD_ENDPOINT + "/" + new CardBuilder().getApiPath());
    public static final String TOKENIZE_CREDIT_CARD_ERROR = "tokenize_credit_card_error";

    @Override
    protected BraintreeFragment getBraintreeFragment() throws InvalidArgumentException {
        Authorization authorization = Authorization.fromString(getAuthorization());
        BraintreeHttpClient httpClient = spy(new BraintreeTestHttpClient(authorization, getIntent()));
        BraintreeFragment fragment = super.getBraintreeFragment();
        try {
            fragment.mConfiguration = Configuration.fromJson(getIntent().getStringExtra(MOCK_CONFIGURATION));
        } catch (Exception ignored) {}
        fragment.mHttpClient = httpClient;

        return fragment;
    }
}
