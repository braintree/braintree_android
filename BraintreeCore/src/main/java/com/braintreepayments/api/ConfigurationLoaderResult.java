package com.braintreepayments.api;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

class ConfigurationLoaderResult {

    private final Configuration configuration;
    private final Exception loadFromCacheError;
    private final Exception saveToCacheError;

    ConfigurationLoaderResult(@NonNull Configuration configuration) {
        this(configuration, null, null);
    }

    ConfigurationLoaderResult(@NonNull Configuration configuration, @Nullable Exception loadFromCacheError, @Nullable Exception saveToCacheError) {
        this.configuration = configuration;
        this.loadFromCacheError = loadFromCacheError;
        this.saveToCacheError = saveToCacheError;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public Exception getLoadFromCacheError() {
        return loadFromCacheError;
    }

    public Exception getSaveToCacheError() {
        return saveToCacheError;
    }
}
