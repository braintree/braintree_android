package com.braintreepayments.api

import io.mockk.every
import io.mockk.mockk

internal class MockkAuthorizationLoaderBuilder {

    private var authorization: Authorization? = null
    private var authorizationError: Exception? = null

    fun authorization(authorization: Authorization): MockkAuthorizationLoaderBuilder {
        this.authorization = authorization
        return this
    }

    fun authorizationError(authorizationError: Exception): MockkAuthorizationLoaderBuilder {
        this.authorizationError = authorizationError
        return this
    }

    fun build(): AuthorizationLoader {
        val authorizationLoader = mockk<AuthorizationLoader>(relaxed = true)

        every { authorizationLoader.loadAuthorization(any()) } answers {
            val callback = firstArg() as AuthorizationCallback
            if (authorization != null) {
                callback.onAuthorizationResult(authorization, null)
            } else if (authorizationError != null) {
                callback.onAuthorizationResult(null, authorizationError)
            }
        }

        return authorizationLoader
    }
}
