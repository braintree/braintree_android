package com.braintreepayments.api;

import static junit.framework.TestCase.assertEquals;

import com.google.android.gms.wallet.Wallet;
import com.google.android.gms.wallet.WalletConstants;

import org.json.JSONException;
import org.junit.Test;

public class WalletOptionsWrapperUnitTest {

    @Test
    public void buildWalletOptions_whenConfigurationEnvironmentIsSandbox_returnsWalletOptionsWithEnvironmentTest() throws JSONException {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_GOOGLE_PAY);
        WalletOptionsWrapper sut = new WalletOptionsWrapper();

        Wallet.WalletOptions walletOptions = sut.buildWalletOptions(configuration);

        assertEquals(WalletConstants.ENVIRONMENT_TEST, walletOptions.environment);
    }

    @Test
    public void buildWalletOptions_whenConfigurationEnvironmentIsProduction_returnsWalletOptionsWithEnvironmentProduction() throws JSONException {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_GOOGLE_PAY_PRODUCTION);
        WalletOptionsWrapper sut = new WalletOptionsWrapper();

        Wallet.WalletOptions walletOptions = sut.buildWalletOptions(configuration);

        assertEquals(WalletConstants.ENVIRONMENT_PRODUCTION, walletOptions.environment);
    }
}
