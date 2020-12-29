package com.braintreepayments.api.models;

import com.braintreepayments.api.internal.GraphQLConstants.Features;
import com.braintreepayments.testutils.Fixtures;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class GraphQLConfigurationUnitTest {

    @Test
    public void parsesGraphQLConfiguration() throws JSONException {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_GRAPHQL);
        GraphQLConfiguration graphQLConfiguration = configuration.getGraphQL();

        assertTrue(graphQLConfiguration.isEnabled());
        assertTrue(graphQLConfiguration.isFeatureEnabled(Features.TOKENIZE_CREDIT_CARDS));
        assertEquals("https://example-graphql.com/graphql", graphQLConfiguration.getUrl());
    }
}
