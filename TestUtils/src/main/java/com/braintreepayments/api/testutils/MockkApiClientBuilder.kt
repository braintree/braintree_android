package com.braintreepayments.api.testutils

import com.braintreepayments.api.core.ApiClient
import com.braintreepayments.api.core.TokenizeCallback
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
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
            mockk<JSONObject>(relaxed = true)
        }

        coEvery { apiClient.tokenizeGraphQL(any()) } answers {
            mockk<JSONObject>(relaxed = true)
        }
        return apiClient
    }
}
