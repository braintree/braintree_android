package com.paypal.android.networking.bus;

import com.paypal.android.networking.request.ServerRequest;

public interface RequestRouter {
    void route(ServerRequest serverRequest);
}
