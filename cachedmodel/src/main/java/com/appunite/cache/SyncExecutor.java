package com.appunite.cache;

import com.google.common.util.concurrent.ListeningExecutorService;

import java.util.concurrent.Callable;

import static com.google.common.base.Preconditions.checkNotNull;

public class SyncExecutor {

    public SyncExecutor(ListeningExecutorService executor, Scheduler scheduler) {
        mExecutor = executor;
        mScheduler = scheduler;
    }

    private ListeningExecutorService mExecutor;
    private Scheduler mScheduler;

    public static interface OnSuccess<T> {
        public void run(T data);
    }

    public static interface OnError {
        public void except(Exception e);
    }

    public static interface Method<T> extends OnSuccess<T>, OnError {
    }

    public <X> void executeAndReturn(final Callable<X> call, final Method<X> method) {
        executeAndReturn(call, method, method);
    }

    public <X> void executeAndReturn(final Callable<X> call, final OnSuccess<X> success, final OnError error) {
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
