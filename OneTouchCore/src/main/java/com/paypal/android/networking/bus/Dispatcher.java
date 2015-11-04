package com.paypal.android.networking.bus;

import com.paypal.android.networking.request.ServerRequest;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages the receivers, and dispatching to those receivers.
 */
public class Dispatcher {
    private static final String TAG = Dispatcher.class.getSimpleName();

    private class RegisteredReceiver {
        public RequestRouter mReceiver;

        public RegisteredReceiver(RequestRouter receiver) {
            mReceiver = receiver;
        }
    }

    // A list of all registered interfaces
    private final List<RegisteredReceiver> mRegisteredReceivers =
            new ArrayList<>();

    public void register(RequestRouter receiver) {
        synchronized (mRegisteredReceivers) {
            for (RegisteredReceiver registeredReceiver : mRegisteredReceivers) {
                if (registeredReceiver.mReceiver == receiver) {
                    Log.d(TAG, "Ignoring attempt to re-register listener " + receiver);
                    return;
                }
            }

            mRegisteredReceivers.add(new RegisteredReceiver(receiver));
        }
    }

    public void unregisterAll() {
        synchronized (mRegisteredReceivers) {
            for (RegisteredReceiver registeredReceiver : mRegisteredReceivers) {
                mRegisteredReceivers.remove(registeredReceiver);
            }
        }
    }

    public void dispatch(ServerRequest request, long minimum_serial_number) {
        Log.d(TAG, "dispatching " + request.toLogString());

        if (request.getSerialNumber() < minimum_serial_number) {
            Log.d(TAG, "discarding " + request.toLogString());

            return;
        }

        // copy the registered listener list, so if a handler
        // unregisters it doesn't mess with the list we're using,
        // those changes will take effect on the next event

        List<RegisteredReceiver> registeredReceivers = new ArrayList<>();
        synchronized (mRegisteredReceivers) {
            // we dispatch in reverse order of registration, this can
            // be thought of as nested registrations, the most recently
            // registered is the most interested in the event

            for (RegisteredReceiver registeredReceiver : mRegisteredReceivers) {
                registeredReceivers.add(0, registeredReceiver);
            }
        }

        // dispatch the event
        //
        for (RegisteredReceiver registeredReceiver : registeredReceivers) {
            registeredReceiver.mReceiver.route(request);
        }
    }
}
