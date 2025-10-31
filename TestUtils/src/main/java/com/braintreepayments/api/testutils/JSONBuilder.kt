package com.braintreepayments.api.testutils

import org.json.JSONException
import org.json.JSONObject

abstract class JSONBuilder protected constructor(json: JSONObject) {

    protected var jsonBody: JSONObject = json

    constructor() : this(JSONObject())

    fun build(): String = jsonBody.toString()

    fun put(value: Any?) {
        var stackIndex = 3
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
}