package com.novelverse.app.utils;

import androidx.annotation.MainThread;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A LiveData that fires its event exactly once per observer registration.
 * Survives configuration changes but won't re-deliver to a new observer
 * that attaches after the value was set.
 *
 * Use this for navigation events, one-shot toasts, dialogs, etc.
 */
public final class SingleLiveEvent<T> extends MutableLiveData<T> {

    private final AtomicBoolean pending = new AtomicBoolean(false);

    @MainThread
    @Override
    public void observe(@Nullable LifecycleOwner owner, @Nullable Observer<? super T> observer) {
        super.observe(owner, value -> {
            if (pending.compareAndSet(true, false) && observer != null) {
                observer.onChanged(value);
            }
        });
    }

    @MainThread
    @Override
    public void setValue(@Nullable T value) {
        pending.set(true);
        super.setValue(value);
    }

    @Override
    public void postValue(@Nullable T value) {
        pending.set(true);
        super.postValue(value);
    }
}