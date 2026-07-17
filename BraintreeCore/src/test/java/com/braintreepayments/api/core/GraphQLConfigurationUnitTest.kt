package com.braintreepayments.api.core

import org.robolectric.RobolectricTestRunner
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(RobolectricTestRunner::class)
class GraphQLConfigurationUnitTest {
    @Test
    fun `when json contains url and features, GraphQLConfiguration is enabled with feature and url parsed`() {
        val input = JSONObject()
            .put("url", "https://example.com/graphql")
            .put(
                "features", JSONArray()
                    .put("tokenize_credit_cards")
            )
        val sut = GraphQLConfiguration(input)
        assertTrue(sut.isEnabled)
        assertTrue(sut.isFeatureEnabled(GraphQLConstants.Features.TOKENIZE_CREDIT_CARDS))
        assertEquals("https://example.com/graphql", sut.url)
    }

    @Test
    fun `when input json is null, GraphQLConfiguration is disabled with default values`() {
        val sut = GraphQLConfiguration(null)
        assertFalse(sut.isEnabled)
        assertFalse(sut.isFeatureEnabled(GraphQLConstants.Features.TOKENIZE_CREDIT_CARDS))
        assertEquals("", sut.url)
    }

    @Test
    fun `when input json is empty, GraphQLConfiguration is disabled with default values`() {
        val sut = GraphQLConfiguration(JSONObject())
        assertFalse(sut.isEnabled)
        assertFalse(sut.isFeatureEnabled(GraphQLConstants.Features.TOKENIZE_CREDIT_CARDS))
        assertEquals("", sut.url)
    }
}
