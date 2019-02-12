package com.braintreepayments.api;

import android.os.Parcel;

import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.models.Authorization;
import com.braintreepayments.api.models.Configuration;
import com.paypal.android.sdk.onetouch.core.PayPalLineItem;
import com.braintreepayments.api.models.PayPalRequest;
import com.braintreepayments.api.models.PostalAddress;
import com.paypal.android.sdk.onetouch.core.AuthorizationRequest;
import com.paypal.android.sdk.onetouch.core.BillingAgreementRequest;
import com.paypal.android.sdk.onetouch.core.CheckoutRequest;
import com.paypal.android.sdk.onetouch.core.Request;
import com.paypal.android.sdk.onetouch.core.network.EnvironmentManager;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.Arrays;

import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static com.braintreepayments.testutils.TestTokenizationKey.TOKENIZATION_KEY;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class PayPalRequestUnitTest {

    private MockFragmentBuilder mMockFragmentBuilder;

    @Before
    public void setup() {
        mMockFragmentBuilder = new MockFragmentBuilder()
                .context(RuntimeEnvironment.application);
    }

    @Test
    public void getCheckoutRequest_containsCorrectValues() throws JSONException {
        Configuration configuration = Configuration.fromJson(stringFromFixture("configuration/with_offline_paypal.json"));
        BraintreeFragment fragment = mMockFragmentBuilder.configuration(configuration).build();

        CheckoutRequest request = PayPal.getCheckoutRequest(fragment, "https://paypal.com/?token=pairingId");

        assertEquals(EnvironmentManager.MOCK, request.getEnvironment());
        assertEquals("com.braintreepayments.api.braintree://onetouch/v1/cancel", request.getCancelUrl());
        assertEquals("com.braintreepayments.api.braintree://onetouch/v1/success", request.getSuccessUrl());
        assertEquals("paypal_client_id", request.getClientId());
        assertEquals("pairingId", request.getPairingId());
    }

    @Test
    public void getCheckoutRequest_buildsWithLiveStageUrl() throws JSONException {
        Configuration configuration = Configuration.fromJson(stringFromFixture("configuration/with_live_paypal.json"));
        BraintreeFragment fragment = mMockFragmentBuilder.configuration(configuration).build();

        Request request = PayPal.getCheckoutRequest(fragment, null);

        assertEquals(EnvironmentManager.LIVE, request.getEnvironment());
    }

    @Test
    public void getCheckoutRequest_buildsWithOfflineStageUrl() throws JSONException {
        Configuration configuration = Configuration.fromJson(stringFromFixture("configuration/with_offline_paypal.json"));
        BraintreeFragment fragment = mMockFragmentBuilder.configuration(configuration).build();

        Request request = PayPal.getCheckoutRequest(fragment, null);

        assertEquals(EnvironmentManager.MOCK, request.getEnvironment());
    }

    @Test
    public void getCheckoutRequest_buildsWithCustomStageUrl() throws JSONException {
        Configuration configuration = Configuration.fromJson(stringFromFixture("configuration/with_custom_paypal.json"));
        BraintreeFragment fragment = mMockFragmentBuilder.configuration(configuration).build();

        Request request = PayPal.getCheckoutRequest(fragment, null);

        assertEquals("custom", request.getEnvironment());
    }

    @Test
    public void getBillingAgreement_containsCorrectValues() throws JSONException {
        Configuration configuration = Configuration.fromJson(stringFromFixture("configuration/with_offline_paypal.json"));
        BraintreeFragment fragment = mMockFragmentBuilder.configuration(configuration).build();

        BillingAgreementRequest request = PayPal.getBillingAgreementRequest(fragment,
                "https://paypal.com/?ba_token=pairingId");

        assertEquals(EnvironmentManager.MOCK, request.getEnvironment());
        assertEquals("com.braintreepayments.api.braintree://onetouch/v1/cancel", request.getCancelUrl());
        assertEquals("com.braintreepayments.api.braintree://onetouch/v1/success", request.getSuccessUrl());
        assertEquals("paypal_client_id", request.getClientId());
        assertEquals("pairingId", request.getPairingId());
    }

    @Test
    public void getBillingAgreementRequest_buildsWithLiveStageUrl() throws JSONException {
        Configuration configuration = Configuration.fromJson(stringFromFixture("configuration/with_live_paypal.json"));
        BraintreeFragment fragment = mMockFragmentBuilder.configuration(configuration).build();

        Request request = PayPal.getBillingAgreementRequest(fragment, null);

        assertEquals(EnvironmentManager.LIVE, request.getEnvironment());
    }

    @Test
    public void getBillingAgreementRequest_buildsWithOfflineStageUrl() throws JSONException {
        Configuration configuration = Configuration.fromJson(stringFromFixture("configuration/with_offline_paypal.json"));
        BraintreeFragment fragment = mMockFragmentBuilder.configuration(configuration).build();

        Request request = PayPal.getBillingAgreementRequest(fragment, null);

        assertEquals(EnvironmentManager.MOCK, request.getEnvironment());
    }

    @Test
    public void getBillingAgreementRequest_buildsWithCustomStageUrl() throws JSONException {
        Configuration configuration = Configuration.fromJson(stringFromFixture("configuration/with_custom_paypal.json"));
        BraintreeFragment fragment = mMockFragmentBuilder.configuration(configuration).build();

        Request request = PayPal.getBillingAgreementRequest(fragment, null);

        assertEquals("custom", request.getEnvironment());
    }

    @Test
    public void getAuthorizationRequest_containsCorrectValues() throws JSONException, InvalidArgumentException {
        Configuration configuration = Configuration.fromJson(stringFromFixture("configuration/with_offline_paypal.json"));
        BraintreeFragment fragment = mMockFragmentBuilder
                .authorization(Authorization.fromString(TOKENIZATION_KEY))
                .configuration(configuration)
                .build();

        AuthorizationRequest request = PayPal.getAuthorizationRequest(fragment);

        assertEquals(EnvironmentManager.MOCK, request.getEnvironment());
        assertEquals("com.braintreepayments.api.braintree://onetouch/v1/cancel", request.getCancelUrl());
        assertEquals("com.braintreepayments.api.braintree://onetouch/v1/success", request.getSuccessUrl());
        assertEquals("paypal_client_id", request.getClientId());
        assertEquals(configuration.getPayPal().getPrivacyUrl(), request.getPrivacyUrl());
        assertEquals(configuration.getPayPal().getUserAgreementUrl(), request.getUserAgreementUrl());

        String[] scopes = request.getScopeString().split(" ");
        Arrays.sort(scopes);
        assertEquals(2, scopes.length);
        assertEquals("email", scopes[0]);
        assertEquals("https://uri.paypal.com/services/payments/futurepayments", scopes[1]);
    }

    @Test
    public void getAuthorizationRequest_buildsWithLiveStageUrl() throws JSONException, InvalidArgumentException {
        Configuration configuration = Configuration.fromJson(stringFromFixture("configuration/with_live_paypal.json"));
        BraintreeFragment fragment = mMockFragmentBuilder
                .authorization(Authorization.fromString(TOKENIZATION_KEY))
                .configuration(configuration)
                .build();

        Request request = PayPal.getAuthorizationRequest(fragment);

        assertEquals(EnvironmentManager.LIVE, request.getEnvironment());
    }

    @Test
    public void getAuthorizationRequest_buildsWithOfflineStageUrl() throws JSONException, InvalidArgumentException {
        Configuration configuration = Configuration.fromJson(stringFromFixture("configuration/with_offline_paypal.json"));
        BraintreeFragment fragment = mMockFragmentBuilder
                .authorization(Authorization.fromString(TOKENIZATION_KEY))
                .configuration(configuration)
                .build();

        Request request = PayPal.getAuthorizationRequest(fragment);

        assertEquals(EnvironmentManager.MOCK, request.getEnvironment());
    }

    @Test
    public void getAuthorizationRequest_buildsWithCustomStageUrl() throws JSONException, InvalidArgumentException {
        Configuration configuration = Configuration.fromJson(stringFromFixture("configuration/with_custom_paypal.json"));
        BraintreeFragment fragment = mMockFragmentBuilder
                .authorization(Authorization.fromString(TOKENIZATION_KEY))
                .configuration(configuration)
                .build();

        Request request = PayPal.getAuthorizationRequest(fragment);

        assertEquals("custom", request.getEnvironment());
    }

    @Test
    public void parcelsCorrectly() {
        PayPalRequest request = new PayPalRequest("12.34")
                .currencyCode("USD")
                .localeCode("en-US")
                .billingAgreementDescription("Billing Agreement Description")
                .shippingAddressRequired(true)
                .shippingAddressEditable(true)
                .shippingAddressOverride(new PostalAddress()
                        .recipientName("Postal Address"))
                .intent(PayPalRequest.INTENT_SALE)
                .landingPageType(PayPalRequest.LANDING_PAGE_TYPE_LOGIN)
                .userAction(PayPalRequest.USER_ACTION_COMMIT)
                .displayName("Display Name")
                .offerCredit(true)
                .merchantAccountId("merchant_account_id");

        ArrayList<PayPalLineItem> lineItems = new ArrayList<PayPalLineItem>();
        lineItems.add(new PayPalLineItem(PayPalLineItem.KIND_DEBIT, "An Item", "1", "1"));
        request.lineItems(lineItems);

        Parcel parcel = Parcel.obtain();
        request.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        PayPalRequest result = PayPalRequest.CREATOR.createFromParcel(parcel);

        assertEquals("12.34", result.getAmount());
        assertEquals("USD", result.getCurrencyCode());
        assertEquals("en-US", result.getLocaleCode());
        assertEquals("Billing Agreement Description",
                result.getBillingAgreementDescription());
        assertEquals(true, result.isShippingAddressRequired());
        assertEquals(true, result.isShippingAddressEditable());
        assertEquals("Postal Address", result.getShippingAddressOverride()
                .getRecipientName());
        assertEquals(PayPalRequest.INTENT_SALE, result.getIntent());
        assertEquals(PayPalRequest.LANDING_PAGE_TYPE_LOGIN, result.getLandingPageType());
        assertEquals(PayPalRequest.USER_ACTION_COMMIT, result.getUserAction());
        assertEquals("Display Name", result.getDisplayName());
        assertTrue(result.shouldOfferCredit());
        assertEquals("merchant_account_id", result.getMerchantAccountId());
        assertEquals(1, result.getLineItems().size());
        assertEquals("An Item", result.getLineItems().get(0).getName());
    }
}

