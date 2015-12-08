package com.paypal.android.networking;

import android.os.Handler;
import android.os.Message;

import com.paypal.android.networking.bus.Dispatcher;
import com.paypal.android.networking.bus.RequestRouter;
import com.paypal.android.networking.processing.RequestExecutorThread;
import com.paypal.android.networking.request.ApiInfo;
import com.paypal.android.networking.request.ServerRequest;
import com.paypal.android.networking.request.ServerRequestEnvironment;
import com.paypal.android.sdk.onetouch.core.base.ContextInspector;
import com.paypal.android.sdk.onetouch.core.base.CoreEnvironment;

import java.lang.ref.WeakReference;

public class ServerInterface implements ServerRequestEnvironment {

    private static final String TAG = ServerInterface.class.getSimpleName();

    private static final int MSG_COMPLETED_SERVER_REQUEST = 2;

    private final ContextInspector mContextInspector;
    private final PayPalEnvironment mEnvironment;
    private final CoreEnvironment mCoreEnvironment;
    private final Dispatcher mDispatcher;

    /**
     * Handler is on the main thread, because ServerInterface is created on the main thread, so all
     * invocations get posted back to main thread.
     */
    private final UiHandler mUiHandler;

    /**
     * Static class because of the following lint message:
     * <p>
     * Messages enqueued on the application thread's MessageQueue also retain their target Handler.
     * If the Handler is an inner class, its outer class will be retained as well. To void leaking
     * the outer class, declass the Handler as a static nested class with a WeakReference to its
     * outer class.
     */
    private static class UiHandler extends Handler {
        WeakReference<ServerInterface> mServerInterfaceRef;

        public UiHandler(ServerInterface serverInterface) {
            mServerInterfaceRef = new WeakReference<>(serverInterface);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_COMPLETED_SERVER_REQUEST:
                    ServerInterface serverInterface = mServerInterfaceRef.get();
                    if (null != serverInterface) {
                        serverInterface.dispatchReply((ServerRequest) msg.obj);
                    }
                    break;
            }
        }
    }

    public ServerInterface(ContextInspector contextInspector, PayPalEnvironment environment,
            CoreEnvironment coreEnv) {
        mContextInspector = contextInspector;
        mEnvironment = environment;
        mCoreEnvironment = coreEnv;
        mDispatcher = new Dispatcher();
        mUiHandler = new UiHandler(this);
    }

    public void setExecutor(RequestExecutorThread executor) {
        if (mExecutor != null) {
            // due to weird dependency issues, we must do this outside of constructor.  should never be called twice.
            throw new IllegalStateException();
        }
        mExecutor = executor;
    }

    @Override
    public void completeServerRequest(ServerRequest serverRequest) {
        // run postExecute on non-UI thread, before replying to UI thread via mUiHandler
        serverRequest.postExecute();

        // Don't dispatch tracking requests
        if (!serverRequest.isTrackingRequest()) {
            Message msg = new Message();
            msg.what = MSG_COMPLETED_SERVER_REQUEST;
            msg.obj = serverRequest;

            mUiHandler.sendMessage(msg);
        }
    }

    /**
     * Dispatches reply on main thread!
     *
     * @param dispatchable
     */
    private void dispatchReply(ServerRequest dispatchable) {
        dispatch(dispatchable);
    }

    public void killExecutorThread() {
        mExecutor.stopThread();
    }

    public void register(RequestRouter requestRouter) {
        mDispatcher.register(requestRouter);
    }

    public void unregisterAllListeners() {
        mDispatcher.unregisterAll();
    }

    private void dispatch(ServerRequest serverRequest) {
        mDispatcher.dispatch(serverRequest, MINIMUM_DISPATCHABLE);
    }

    private RequestExecutorThread mExecutor;

    private static final long MINIMUM_DISPATCHABLE = 0;

    public void submit(ServerRequest serverRequest) {
        mExecutor.queue(serverRequest);
    }

    @Override
    public String getUrl(ApiInfo api) {
        if (mEnvironment != null && mEnvironment.getEndpoints() != null) {
            String url = mEnvironment.getEndpoints().get(api.getName());
            return url;
        }

        return null;
    }

    @Override
    public String environmentName() {
        return mEnvironment.getServerName();
    }

    @Override
    public ContextInspector getContextInspector() {
        return mContextInspector;
    }

    public String environmentBaseUrl() {
        return mEnvironment.getBaseUrl();
    }

    public CoreEnvironment getCoreEnvironment() {
        return mCoreEnvironment;
    }
}
