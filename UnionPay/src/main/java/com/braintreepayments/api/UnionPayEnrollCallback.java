package com.braintreepayments.api;

import androidx.annotation.Nullable;

/**
 * Callback for receiving result of
 * {@link UnionPayClient#enroll(UnionPayCard, UnionPayEnrollCallback)}.
 */
public interface UnionPayEnrollCallback {

    /**
     * @param enrollment {@link UnionPayEnrollment}
     * @param error an exception that occurred while enrolling a Union Pay card
     */
    void onResult(@Nullable UnionPayEnrollment enrollment, @Nullable Exception error);
}
