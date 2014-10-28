package com.appunite.cache;

import com.google.common.util.concurrent.ListeningExecutorService;

import java.util.concurrent.Callable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

public class SyncExecutor {
    @Nonnull
    private ListeningExecutorService mExecutor;
    @Nonnull
    private Scheduler mScheduler;

    public SyncExecutor(@Nonnull ListeningExecutorService executor,
                        @Nonnull Scheduler scheduler) {
        mExecutor = checkNotNull(executor);
        mScheduler = checkNotNull(scheduler);
    }

    public static interface OnSuccess<T> {
        public void run(T data);
    }

    public static interface OnError {
        public void except(@Nonnull Exception e);
    }

    public static interface Method<T> extends OnSuccess<T>, OnError {
    }

    public <X> void executeAndReturn(@Nonnull final Callable<X> call,
                                     @Nullable final Method<X> method) {
        executeAndReturn(call, method, method);
    }

    public <X> void executeAndReturn(@Nonnull final Callable<X> call,
                                     @Nullable final OnSuccess<X> success,
                                     @Nullable final OnError error) {
        checkNotNull(call, "call could not be null");
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final X ret = call.call();
                    if (success != null) {
                        mScheduler.schedule(new Runnable() {
                            @Override
                            public void run() {
                                success.run(ret);
                            }
                        });
                    }
                } catch (final Exception e) {
                    if (error != null) {
                        mScheduler.schedule(new Runnable() {
                            @Override
                            public void run() {
                                error.except(e);
                            }
                        });
                    }
                }
            }
        });
    }

}
