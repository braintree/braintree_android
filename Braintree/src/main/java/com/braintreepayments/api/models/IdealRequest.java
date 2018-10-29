package com.braintreepayments.api.models;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * @deprecated Use {@link LocalPaymentRequest}
 *
 * Builder used to construct an iDEAL payment request.
 */
@Deprecated
public class IdealRequest {

    private static final String ROUTE_ID_KEY = "route_id";
    private static final String ORDER_ID_KEY = "order_id";
    private static final String ISSUER_KEY = "issuer";
    private static final String AMOUNT_KEY = "amount";
    private static final String CURRENCY_KEY = "currency";
    private static final String REDIRECT_URL_KEY = "redirect_url";

    private String mOrderId;
    private String mIssuerId;
    private String mAmount;
    private String mCurrency;

    /**
     * @param orderId Required - An id used to identify ideal payments when the consumer closes the bank confirmation
     * page after approving the payment.
     * @return {@link IdealRequest}
     */
    public IdealRequest orderId(String orderId) {
        mOrderId = orderId;
        return this;
    }

    /**
     * @param issuerId Required - {@link IdealBank} that the customer will use to complete this payment.
     * @return {@link IdealRequest}
     */
    public IdealRequest issuerId(String issuerId) {
        mIssuerId = issuerId;
        return this;
    }

    /**
     * @param amount Required - The purchase price (with a period (.) used as decimal separator).
     * @return {@link IdealRequest}
     */
    public IdealRequest amount(String amount) {
        mAmount = amount;
        return this;
    }

    /**
     * @param currency Required - Since iDEAL currently only supports Euro payments, value should always be ‘EUR’
     * @return {@link IdealRequest}
     */
    public IdealRequest currency(String currency) {
        mCurrency = currency;
        return this;
    }

    public String build(String redirectUrl, String routeId) {
        try {
            return new JSONObject()
                    .put(ROUTE_ID_KEY, routeId)
                    .put(ORDER_ID_KEY, mOrderId)
                    .put(ISSUER_KEY, mIssuerId)
                    .put(AMOUNT_KEY, mAmount)
                    .put(CURRENCY_KEY, mCurrency)
                    .put(REDIRECT_URL_KEY, redirectUrl)
                    .toString();
        } catch (JSONException ignored) {}

        return new JSONObject().toString();
    }
}
