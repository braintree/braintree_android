package com.braintreepayments.api.localpayment;

import androidx.annotation.NonNull;

import com.braintreepayments.api.BrowserSwitchOptions;

/**
 * Local payment result information.
 */
public class LocalPaymentAuthRequestParams {

    private final LocalPaymentRequest request;
    private final String approvalUrl;
    private final String paymentId;

    private BrowserSwitchOptions browserSwitchOptions;

    LocalPaymentAuthRequestParams(LocalPaymentRequest request, String approvalUrl, String paymentId) {
        this.request = request;
        this.approvalUrl = approvalUrl;
        this.paymentId = paymentId;
    }

    /**
     * @return The original request used to create the local payment transaction.
     */
    @NonNull
    public LocalPaymentRequest getRequest() {
        return request;
    }

    /**
     * @return The URL used for payment approval.
     */
    @NonNull
    public String getApprovalUrl() {
        return approvalUrl;
    }

    /**
     * @return The ID of the local payment after creation.
     */
    @NonNull
    public String getPaymentId() {
        return paymentId;
    }

    BrowserSwitchOptions getBrowserSwitchOptions() {
        return browserSwitchOptions;
    }

    void setBrowserSwitchOptions(BrowserSwitchOptions browserSwitchOptions) {
        this.browserSwitchOptions = browserSwitchOptions;
    }
}
