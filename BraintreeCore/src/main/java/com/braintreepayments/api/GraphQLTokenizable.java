package com.braintreepayments.api;

interface GraphQLTokenizable {
   String buildGraphQL(Authorization authorization) throws BraintreeException;
}
