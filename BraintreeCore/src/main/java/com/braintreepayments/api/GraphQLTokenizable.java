package com.braintreepayments.api;

import org.json.JSONObject;

interface GraphQLTokenizable {
   String buildGraphQL(Authorization authorization) throws BraintreeException;
   JSONObject buildGraphQLTokenizationJSON() throws BraintreeException;
}
