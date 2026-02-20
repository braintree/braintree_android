package com.braintreepayments.api.testutils

import org.json.JSONException
import org.json.JSONObject

abstract class JSONBuilder protected constructor(json: JSONObject = JSONObject()) {

    protected var jsonBody: JSONObject = json

    fun build(): String = jsonBody.toString()

    fun put(value: Any?) {
        var stackIndex = STACK_INDEX_CALLER
        val stack = Thread.currentThread().stackTrace
        if (!stack[0].isNativeMethod) {
            stackIndex--
        }
        val current = stack[stackIndex]
        put(current.methodName, value)
    }

    fun put(key: String, value: Any?) {
        try {
            jsonBody.put(key, value)
        } catch (_: JSONException) {
        }
    }

    companion object {
        private const val STACK_INDEX_CALLER = 3
    }
}
