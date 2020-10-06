package com.braintreepayments.api.models;

import com.braintreepayments.api.internal.GraphQLConstants.Features;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class GraphQLConfigurationUnitTest {

    @Test
    public void parsesGraphQLConfiguration() throws JSONException {
        Configuration configuration = Configuration.fromJson(stringFromFixture("configuration/with_graphql.json"));
        GraphQLConfiguration graphQLConfiguration = configuration.getGraphQL();

        assertTrue(graphQLConfiguration.isEnabled());
        assertTrue(graphQLConfiguration.isFeatureEnabled(Features.TOKENIZE_CREDIT_CARDS));
        assertEquals("/graphql", graphQLConfiguration.getUrl());
    }
}
