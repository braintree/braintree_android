package com.paypal.android.sdk.onetouch.core.base;

/**
 * All users of the library must supply an implementation of this interface.
 */
public interface CoreEnvironment {
    /**
     * allow the library to obtain the application's version. obtain the application's version,
     * needed to send to the servers, has to be negotiated with the server groups what is to be
     * sent
     * <p>
     * typically X.y.z.b
     */
    String getVersion();

    /**
     * the app provides the name of a pref file for the library to use for it's stuff. deleting will
     * pretty much lobotomize the library and make it think it's a fresh installation
     */
    String getPrefsFile();

    /**
     * ask the application to supply a user agent string for us to use
     *
     * @return user agent or NULL if the app has no preference and desires no change.
     */
    String getUserAgent();

    /**
     * The application specified sha1
     *
     * @return
     */
    String getSha1();

    /**
     * Library projects' BuildConfig doesn't work right, so the parent project needs to specify
     * this.  Blargh.
     */
    boolean isDebug();
}
