package com.braintreepayments.api.models;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;

import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;

@RunWith(RobolectricGradleTestRunner.class)
public class CardConfigurationUnitTest {

    @Test
    public void parsesCardConfiguration() throws JSONException {
        Configuration configuration = Configuration.fromJson(stringFromFixture("configuration_with_supported_card_types.json"));
        CardConfiguration cardConfiguration = configuration.getCardConfiguration();

        assertEquals("American Express", cardConfiguration.getSupportedCardTypes()[0]);
        assertEquals("Discover", cardConfiguration.getSupportedCardTypes()[1]);
        assertEquals("JCB", cardConfiguration.getSupportedCardTypes()[2]);
        assertEquals("MasterCard", cardConfiguration.getSupportedCardTypes()[3]);
        assertEquals("Visa", cardConfiguration.getSupportedCardTypes()[4]);
    }

    @Test
    public void handlesNull() {
        CardConfiguration cardConfiguration = CardConfiguration.fromJson(null);

        assertEquals(0, cardConfiguration.getSupportedCardTypes().length);
    }
}
