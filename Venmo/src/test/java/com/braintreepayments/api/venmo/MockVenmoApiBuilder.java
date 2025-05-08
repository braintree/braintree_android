package com.braintreepayments.api.venmo;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import org.mockito.stubbing.Answer;

/**
 * Replaced by {@link MockkVenmoApiBuilder} as tests are being converted to kotlin
 */
@Deprecated
public class MockVenmoApiBuilder {

    private String venmoPaymentContextId;
    private VenmoAccountNonce createNonceFromPaymentContextSuccess;
    private VenmoAccountNonce vaultVenmoAccountNonceSuccess;
    private Exception createPaymentContextError;
    private Exception createNonceFromPaymentContextError;
    private Exception vaultVenmoAccountNonceError;

    public MockVenmoApiBuilder createPaymentContextSuccess(String venmoPaymentContextId) {
        this.venmoPaymentContextId = venmoPaymentContextId;
        return this;
    }

    public MockVenmoApiBuilder createPaymentContextError(Exception createPaymentContextError) {
        this.createPaymentContextError = createPaymentContextError;
        return this;
    }

    public MockVenmoApiBuilder createNonceFromPaymentContextSuccess(
            VenmoAccountNonce createNonceFromPaymentContextSuccess) {
        this.createNonceFromPaymentContextSuccess = createNonceFromPaymentContextSuccess;
        return this;
    }

    public MockVenmoApiBuilder createNonceFromPaymentContextError(
            Exception createNonceFromPaymentContextError) {
        this.createNonceFromPaymentContextError = createNonceFromPaymentContextError;
        return this;
    }

    public MockVenmoApiBuilder vaultVenmoAccountNonceSuccess(
            VenmoAccountNonce vaultVenmoAccountNonceSuccess) {
        this.vaultVenmoAccountNonceSuccess = vaultVenmoAccountNonceSuccess;
        return this;
    }

    public MockVenmoApiBuilder vaultVenmoAccountNonceError(Exception vaultVenmoAccountNonceError) {
        this.vaultVenmoAccountNonceError = vaultVenmoAccountNonceError;
        return this;
    }

    public VenmoApi build() {
        VenmoApi venmoApi = mock(VenmoApi.class);

        doAnswer((Answer<Void>) invocation -> {
            VenmoApiCallback callback = (VenmoApiCallback) invocation.getArguments()[2];
            if (venmoPaymentContextId != null) {
                callback.onResult(venmoPaymentContextId, null);
            } else if (createPaymentContextError != null) {
                callback.onResult(null, createPaymentContextError);
            }

            return null;
        }).when(venmoApi).createPaymentContext(any(VenmoRequest.class), anyString(),
                any(VenmoApiCallback.class));

        doAnswer((Answer<Void>) invocation -> {
            VenmoInternalCallback callback = (VenmoInternalCallback) invocation.getArguments()[1];
            if (createNonceFromPaymentContextSuccess != null) {
                callback.onResult(createNonceFromPaymentContextSuccess, null);
            } else if (createNonceFromPaymentContextError != null) {
                callback.onResult(null, createNonceFromPaymentContextError);
            }

            return null;
        }).when(venmoApi)
                .createNonceFromPaymentContext(anyString(), any(VenmoInternalCallback.class));

        doAnswer((Answer<Void>) invocation -> {
            VenmoInternalCallback callback = (VenmoInternalCallback) invocation.getArguments()[1];
            if (vaultVenmoAccountNonceSuccess != null) {
                callback.onResult(vaultVenmoAccountNonceSuccess, null);
            } else if (vaultVenmoAccountNonceError != null) {
                callback.onResult(null, vaultVenmoAccountNonceError);
            }

            return null;
        }).when(venmoApi).vaultVenmoAccountNonce(anyString(), any(VenmoInternalCallback.class));

        return venmoApi;
    }
}
