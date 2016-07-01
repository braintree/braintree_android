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

    // TODO - if smsCodeRequired is false, should we just tokenize?
    /**
     * Will be called when the customer has been enrolled. If smsCodeRequired is false, enrollment is not required
     * EnrollmentId, and the SMS code collected from the customer will need to be applied to the
     * {@link UnionPayCardBuilder} before invoking
     * {@link com.braintreepayments.api.UnionPay#tokenize(BraintreeFragment, UnionPayCardBuilder)}
     * @param enrollmentId returned from
     * {@link com.braintreepayments.api.UnionPay#enroll(BraintreeFragment, UnionPayCardBuilder)}.
     * @param smsCodeRequired return from
     * {@link com.braintreepayments.api.UnionPay#enroll(BraintreeFragment, UnionPayCardBuilder)}.
     */
    void onSmsCodeSent(String enrollmentId/*, boolean smsCodeRequired*/);
}
