package com.braintreepayments.api;

/**
 A VenmoRequest specifies options that contribute to the Venmo flow
 */
public class VenmoRequest {

    private boolean shouldVault;
    private String profileId;

    /**
     * @param shouldVault Optional - Whether or not to automatically vault the Venmo Account.
     *                    Vaulting will only occur if a client token with a customer ID is being used.
     *                    Defaults to false.
     */
    public void shouldVault(boolean shouldVault) {
        this.shouldVault = shouldVault;
    }

    /**
     * @param profileId Optional - The Venmo profile ID to be used during payment authorization.
     *                  Customers will see the business name and logo associated with this Venmo
     *                  profile, and it will show up in the Venmo app as a "Connected Merchant".
     *                  Venmo profile IDs can be found in the Braintree Control Panel.
     *                  Leaving this `null` will use the default Venmo profile.
     */
    public void profileId(String profileId) {
        this.profileId = profileId;
    }

    /**
     * @return Whether or not to automatically vault the Venmo Account.
     */
    public boolean shouldVault() {
        return shouldVault;
    }

    /**
     * @return The Venmo profile ID to be used during payment authorization.
     */
    public String getProfileId() {
        return profileId;
    }
}

