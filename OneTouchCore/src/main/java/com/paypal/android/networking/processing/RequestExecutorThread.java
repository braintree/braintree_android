package com.paypal.android.networking.processing;

import com.paypal.android.networking.events.LibraryError;
import com.paypal.android.networking.events.RequestError;
import com.paypal.android.networking.request.ServerRequest;
import com.paypal.android.networking.request.ServerRequestEnvironment;
import com.paypal.android.sdk.onetouch.core.base.Constants;

import android.util.Log;

import org.json.JSONException;

import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * This is a thread so that requests to send off are released from the main UI
 * thread as soon as possible.
 */
public class RequestExecutorThread extends Thread {
    private static final String TAG = RequestExecutorThread.class.getSimpleName();

    private final ServerRequestEnvironment mServerRequestEnvironment;

    // List of submitted pending requests
    private final List<ServerRequest> mQueuedRequests =
            Collections.synchronizedList(new LinkedList<ServerRequest>());

    // Used to signal this thread that it needs to stop
    private boolean mStopThread;

    private final RequestProcessor mRequestProcessor;

    public RequestExecutorThread(ServerRequestEnvironment env,
                          RequestProcessor requestProcessor) {
        this.mServerRequestEnvironment = env;
        this.mRequestProcessor = requestProcessor;

        start();
    }

    public void queue(ServerRequest request) {
        synchronized (mQueuedRequests) {
            // Add the new request to the list of pending requests
            mQueuedRequests.add(request);

            Log.d(TAG, "Queued " + request.toLogString());

            // Wake up the thread so it'll pull one off the list
            mQueuedRequests.notifyAll();
        }
    }

    public void stopThread() {
        // Already stopped?
        if (mStopThread) {
            return;
        }

        mRequestProcessor.cancelRequests();

        // It will be now!
        mStopThread = true;

        // Wake up all threads waiting on pending requests. Hold a lock on
        // the list to avoid race conditions until all have been notified.
        synchronized (mQueuedRequests) {
            mQueuedRequests.notifyAll();
        }

        // Wake up our thread
        this.interrupt();

        // Wait for this thread to die
        while (this.isAlive()) {
            try {
                Thread.sleep(10);
                Log.d(TAG, "Waiting for " + getClass().getSimpleName() + " to die");
            } catch (InterruptedException e) {
            }
        }
    }

    @Override
    public void run() {
        Log.d(TAG, "Starting " + getClass().getSimpleName());


        // Run until stopped
        while (!mStopThread) {
            ServerRequest serverRequest = null;

            synchronized (mQueuedRequests) {

                // If there are no pending requests, just go to sleep
                // until there are some
                if (mQueuedRequests.isEmpty()) {
                    try {
                        mQueuedRequests.wait();
                    } catch (InterruptedException e) {
                    }
                }

                // We have some requests...get one to process
                else {
                    serverRequest = mQueuedRequests.remove(0);
                }
            }// end synchronized

            // will be null iff the app is shutting down
            if(null != serverRequest) {
                /*
                    execute the request outside outside the synchronized block to allow the main
                    thread to add more mQueuedRequests.  Made this change because getRiskPayload
                    now does a lot of processing and thread weirdness with advertiserId, which was
                    blocking subsequent tracking requests from getting added to the queue.
                    See https://github.com/paypal/PayPal-Android-SDK/issues/83
                */
                execute(serverRequest);
            }

        } // end while
        Log.d(TAG, getClass().getSimpleName() + " exiting");

    }

    private void execute(ServerRequest serverRequest) {
        try {
            serverRequest.setComputedRequest(serverRequest.computeRequest());
        } catch (JSONException e) {
            Log.e(Constants.PUBLIC_TAG, "Exception computing request", e);
            serverRequest.setError(new RequestError(LibraryError.PARSE_RESPONSE_ERROR.toString(),
                    "JSON Exception in computeRequest", e.getMessage()));
        } catch (UnsupportedEncodingException e) {
            Log.e(Constants.PUBLIC_TAG, "Exception computing request", e);
            serverRequest.setError(new RequestError(LibraryError.PARSE_RESPONSE_ERROR.toString(),
                    "Unsupported encoding", e.getMessage()));
        }

        boolean isStartedSuccessfully = mRequestProcessor.execute(serverRequest);

        if (!isStartedSuccessfully) {
            // put failed response
            mServerRequestEnvironment.completeServerRequest(serverRequest);
        }
    }
}
