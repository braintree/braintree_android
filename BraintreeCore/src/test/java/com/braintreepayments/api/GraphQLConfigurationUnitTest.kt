package com.braintreepayments.api

import org.robolectric.RobolectricTestRunner
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(RobolectricTestRunner::class)
class GraphQLConfigurationUnitTest {
    @Test
    fun fromJson_parsesFullInput() {
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
    fun fromJson_whenInputNull_returnsConfigWithDefaultValues() {
        val sut = GraphQLConfiguration(null)
        assertFalse(sut.isEnabled)
        assertFalse(sut.isFeatureEnabled(GraphQLConstants.Features.TOKENIZE_CREDIT_CARDS))
        assertEquals("", sut.url)
    }

    @Test
    fun fromJson_whenInputEmpty_returnsConfigWithDefaultValues() {
        val sut = GraphQLConfiguration(JSONObject())
        assertFalse(sut.isEnabled)
        assertFalse(sut.isFeatureEnabled(GraphQLConstants.Features.TOKENIZE_CREDIT_CARDS))
        assertEquals("", sut.url)
    }
}
