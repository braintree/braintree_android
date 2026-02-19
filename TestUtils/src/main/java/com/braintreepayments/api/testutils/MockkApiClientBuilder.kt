package com.braintreepayments.api.testutils

import com.braintreepayments.api.core.ApiClient
import io.mockk.coEvery
import io.mockk.mockk
import org.json.JSONObject

class MockkApiClientBuilder {

    private var tokenizeRESTError: Exception? = null
    private var tokenizeRESTSuccess: JSONObject? = null

    private var tokenizeGraphQLError: Exception? = null
    private var tokenizeGraphQLSuccess: JSONObject? = null

    fun tokenizeRESTError(tokenizeRestError: Exception): MockkApiClientBuilder {
        tokenizeRESTError = tokenizeRestError
        return this
    }

    fun tokenizeRESTSuccess(tokenizeRestSuccess: JSONObject): MockkApiClientBuilder {
        tokenizeRESTSuccess = tokenizeRestSuccess
        return this
    }

    fun tokenizeGraphQLError(tokenizeGraphQLError: Exception): MockkApiClientBuilder {
        this.tokenizeGraphQLError = tokenizeGraphQLError
        return this
    }

    fun tokenizeGraphQLSuccess(tokenizeGraphQLSuccess: JSONObject): MockkApiClientBuilder {
        this.tokenizeGraphQLSuccess = tokenizeGraphQLSuccess
        return this
    }

    fun build(): ApiClient {
        val apiClient = mockk<ApiClient>(relaxed = true)

        coEvery { apiClient.tokenizeREST(any()) } answers {
            tokenizeRESTSuccess ?: throw tokenizeRESTError
                ?: Exception("No response configured for tokenizeREST")
        }

        coEvery { apiClient.tokenizeGraphQL(any()) } answers {
            tokenizeGraphQLSuccess ?: throw tokenizeGraphQLError
                ?: Exception("No response configured for tokenizeGraphQL")
        }
        return apiClient
    }
}
