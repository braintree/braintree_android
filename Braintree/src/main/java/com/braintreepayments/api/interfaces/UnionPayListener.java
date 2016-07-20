package com.braintreepayments.api.interfaces;

import com.braintreepayments.api.BraintreeFragment;
import com.braintreepayments.api.models.UnionPayCapabilities;
import com.braintreepayments.api.models.UnionPayCardBuilder;

/**
 * Interface that defines callbacks for UnionPay.
 */
public interface UnionPayListener extends BraintreeListener {

    /**
     * Will be called when
     * {@link com.braintreepayments.api.models.UnionPayCapabilities} has been successfully fetched.
     */
    void onCapabilitiesFetched(UnionPayCapabilities capabilities);

    /**
     * Will be called when the customer has been enrolled.
     * </p>
     * If smsCodeRequired is {@code true}, a SMS code will be sent to the phone number provided during
     * {@link com.braintreepayments.api.UnionPay#enroll(BraintreeFragment, UnionPayCardBuilder)}.
     * This SMS code will need to be supplied in the {@link UnionPayCardBuilder} when calling
     * {@link com.braintreepayments.api.UnionPay#tokenize(BraintreeFragment, UnionPayCardBuilder)}.
     * If smsCodeRequired is false, enrollment is not required, and tokenization can be done immediately
     * without an SMS code.
     * </p>
     * The enrollment id and SMS code collected from the customer will need to be set on the
     * {@link UnionPayCardBuilder} before invoking
     * {@link com.braintreepayments.api.UnionPay#tokenize(BraintreeFragment, UnionPayCardBuilder)}
     *
     * @param enrollmentId returned from
     * {@link com.braintreepayments.api.UnionPay#enroll(BraintreeFragment, UnionPayCardBuilder)}.
     * @param smsCodeRequired return from
     * {@link com.braintreepayments.api.UnionPay#enroll(BraintreeFragment, UnionPayCardBuilder)}.
     */
    void onSmsCodeSent(String enrollmentId, boolean smsCodeRequired);
}
