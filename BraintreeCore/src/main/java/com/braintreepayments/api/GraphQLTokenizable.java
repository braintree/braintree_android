package com.braintreepayments.api;

import org.json.JSONObject;

interface GraphQLTokenizable {
   JSONObject buildGraphQLJSON() throws BraintreeException;
}
