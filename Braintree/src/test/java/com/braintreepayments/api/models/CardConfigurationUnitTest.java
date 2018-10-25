package com.braintreepayments.api.models;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class CardConfigurationUnitTest {

    @Test
    public void parsesCardConfiguration() throws JSONException {
        Configuration configuration = Configuration.fromJson(stringFromFixture("configuration/with_supported_card_types.json"));
        CardConfiguration cardConfiguration = configuration.getCardConfiguration();

        assertEquals(5, cardConfiguration.getSupportedCardTypes().size());
        assertTrue(cardConfiguration.getSupportedCardTypes().contains("American Express"));
        assertTrue(cardConfiguration.getSupportedCardTypes().contains("Discover"));
        assertTrue(cardConfiguration.getSupportedCardTypes().contains("JCB"));
        assertTrue(cardConfiguration.getSupportedCardTypes().contains("MasterCard"));
        assertTrue(cardConfiguration.getSupportedCardTypes().contains("Visa"));
        assertFalse(cardConfiguration.isFraudDataCollectionEnabled());
    }

    @Test
    public void handlesNull() {
        CardConfiguration cardConfiguration = CardConfiguration.fromJson(null);

        assertEquals(0, cardConfiguration.getSupportedCardTypes().size());
    }

    @Test
    public void isFraudDataCollectionEnabled_isTrueWhenEnabled() throws JSONException {
        Configuration configuration = Configuration.fromJson(stringFromFixture("configuration/with_card_collect_device_data.json"));
        CardConfiguration cardConfiguration = configuration.getCardConfiguration();

        assertTrue(cardConfiguration.isFraudDataCollectionEnabled());
    }

}
