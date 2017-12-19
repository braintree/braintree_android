package com.braintreepayments.api.models;

import com.braintreepayments.testutils.TestConfigurationBuilder;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.List;

import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class IdealBankUnitTest {

    @Test
    public void fromJson_parsesSuccessfully() throws JSONException {
        Configuration configuration = new TestConfigurationBuilder()
                .assetsUrl("https://assets.braintreegateway.com")
                .buildConfiguration();

        String idealBankFixture = stringFromFixture("payment_methods/ideal_issuing_banks.json");
        List<IdealBank> banks = IdealBank.fromJson(configuration, idealBankFixture);

        assertEquals(10, banks.size());
        IdealBank bank = banks.get(0);

        assertEquals("ABNANL2A", bank.getId());
        assertEquals("ABN AMRO", bank.getName());
        assertEquals(configuration.getAssetsUrl() + "/web/static/images/ideal_issuer-logo_ABNANL2A.png", bank.getImageUri().toString());
        assertEquals("NL", bank.getCountryCode());
    }

    @Test
    public void fromJson_whenNull_retunsEmptyList() throws JSONException {
        Configuration configuration = new TestConfigurationBuilder()
                .assetsUrl("https://assets.braintreegateway.com")
                .buildConfiguration();

        List<IdealBank> banks = IdealBank.fromJson(configuration, null);
        assertEquals(0, banks.size());
    }
}
