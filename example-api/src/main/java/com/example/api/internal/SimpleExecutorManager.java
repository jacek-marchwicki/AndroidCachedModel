/*
 * Copyright (C) 2014 Jacek Marchwicki <jacek.marchwicki@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.api.internal;

import com.appunite.cache.ListenerCallback;
import com.appunite.cache.ObservableExecutor;
import com.appunite.cache.SyncExecutor;


import java.util.concurrent.Callable;

import javax.inject.Inject;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public abstract class SimpleExecutorManager<T> implements ObservableExecutor<T> {

    private final SyncExecutor mSyncExecutor;

    protected ListenerCallback<T> mListener;

    @Inject
    protected SimpleExecutorManager(SyncExecutor syncExecutor) {
        mSyncExecutor = syncExecutor;
    }

    @Override
    public void register(ListenerCallback<T> listener) {
        checkNotNull(listener);
        checkState(mListener == null);
        mListener = listener;

        loadData();
    }

    private void loadData() {
        beforeExecute();
        mSyncExecutor.executeAndReturn(
                new Callable<T>() {
                    @Override
                    public T call() throws Exception {
                        return execute();
                    }
                },
                new SyncExecutor.OnSuccess<T>() {
                    @Override
                    public void run(T data) {
                        afterExecute(data);
                        if (mListener != null) {
                            mListener.onNewData(data);
                        }
                    }
                },
                new SyncExecutor.OnError() {
                    @Override
                    public void except(Exception e) {
                        afterFail(e);
                        if (mListener != null) {
                            mListener.onError(e);
                        }
                    }
                });
    }

    protected void beforeExecute() {

    }

    protected abstract T execute() throws Exception;

    protected void afterExecute(T data) {
    }

    protected void afterFail(Exception e) {

    }

    @Override
    public void unregister() {
        checkState(mListener != null);
        mListener = null;
    }

    @Override
    public void unregisterIfRegistered() {
        mListener = null;
    }

    public static void unregisterIfRegistered(ObservableExecutor<?> executor) {
        if (executor != null) {
            executor.unregisterIfRegistered();
        }
    }
}
