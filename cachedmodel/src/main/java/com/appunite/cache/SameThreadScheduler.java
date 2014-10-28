package com.appunite.cache;

import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkNotNull;

public class SameThreadScheduler implements Scheduler {
    @Override
    public void schedule(@Nonnull Runnable runnable) {
        checkNotNull(runnable);
        runnable.run();
    }
}
