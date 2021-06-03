package com.braintreepayments.api;

import androidx.annotation.Nullable;

/**
 A VenmoRequest specifies options that contribute to the Venmo flow
 */
public class VenmoRequest {

    private boolean shouldVault;
    private String profileId;
    private String displayName;

    private final @VenmoPaymentMethodUsage int paymentMethodUsage;

    /**
     * Request to tokenize a Venmo account.
     *
     * @param paymentMethodUsage {@link VenmoPaymentMethodUsage} for the tokenized Venmo account: either multi-use or single use.
     */
    public VenmoRequest(@VenmoPaymentMethodUsage int paymentMethodUsage) {
        this.paymentMethodUsage = paymentMethodUsage;
    }

    /**
     * @param shouldVault Optional - Whether or not to automatically vault the Venmo Account.
     *                    Vaulting will only occur if a client token with a customer ID is being used.
     *                    Defaults to false.
     */
    public void setShouldVault(boolean shouldVault) {
        this.shouldVault = shouldVault;
    }

    /**
     * @param profileId Optional - The Venmo profile ID to be used during payment authorization.
     *                  Customers will see the business name and logo associated with this Venmo
     *                  profile, and it will show up in the Venmo app as a "Connected Merchant".
     *                  Venmo profile IDs can be found in the Braintree Control Panel.
     *                  Leaving this `null` will use the default Venmo profile.
     */
    public void setProfileId(String profileId) {
        this.profileId = profileId;
    }

    /**
     * @return Whether or not to automatically vault the Venmo Account.
     */
    public boolean getShouldVault() {
        return shouldVault;
    }

    /**
     * @return The Venmo profile ID to be used during payment authorization.
     */
    public String getProfileId() {
        return profileId;
    }

    /**
     * @return {@link VenmoPaymentMethodUsage} for the tokenized Venmo account: either multi-use or single use.
     */
    public @VenmoPaymentMethodUsage int getPaymentMethodUsage() {
        return paymentMethodUsage;
    }

    /**
     * @param displayName Optional. The business name that will be displayed in the Venmo app payment approval screen. Only used by merchants onboarded as PayFast channel partners.
     */
    public void setDisplayName(@Nullable String displayName) {
        this.displayName = displayName;
    }

    /**
     * @return The business name that will be displayed in the Venmo app payment approval screen. Only used by merchants onboarded as PayFast channel partners.
     */
    @Nullable
    public String getDisplayName() {
        return displayName;
    }

    String getPaymentMethodUsageAsString() {
        switch (paymentMethodUsage) {
            case VenmoPaymentMethodUsage.SINGLE_USE:
                return "SINGLE_USE";
            case VenmoPaymentMethodUsage.MULTI_USE:
                return "MULTI_USE";
            default:
                return null;
        }
    }
}
