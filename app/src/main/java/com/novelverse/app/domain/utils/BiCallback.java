package com.novelverse.app.domain.utils;

/** Two-argument callback used for (result, error) patterns. */
public interface BiCallback<T> {
    void onResult(T result, String error);
}
