package com.paypal.android.networking.processing;

import com.paypal.android.networking.request.ServerRequest;

public interface RequestProcessor {

    boolean execute(ServerRequest serverRequest);
    void cancelRequests();

}
