package com.braintreepayments.api;

import org.json.JSONObject;

interface GraphQLTokenizable {
   JSONObject buildGraphQLTokenizationJSON() throws BraintreeException;
}
