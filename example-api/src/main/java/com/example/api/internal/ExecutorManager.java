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

import com.appunite.cache.CacheInvalidationListener;
import com.appunite.cache.CacheKey;
import com.appunite.cache.CommandName;
import com.appunite.cache.ListenerCallback;
import com.appunite.cache.ObservableExecutor;
import com.appunite.cache.SyncExecutor;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.concurrent.Callable;

import javax.inject.Inject;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public abstract class ExecutorManager<T> implements ObservableExecutor<T>,
        CacheInvalidationListener<T> {

    private final SyncExecutor mSyncExecutor;
    private final ExampleDescription mExampleDescription;

    @Inject
    public ExecutorManager(SyncExecutor syncExecutor, ExampleDescription exampleDescription) {
        mSyncExecutor = syncExecutor;
        mExampleDescription = exampleDescription;
    }

    private boolean mExecuting = false;

    protected ListenerCallback<T> mListener;

    @Override
    public void register(ListenerCallback<T> listener) {
        checkNotNull(listener);
        checkState(mListener == null);
        mListener = listener;

        mExampleDescription.register(getCacheKey(), this);
        final Optional<T> cacheIfPresent = getCacheIfPresent();
        if (cacheIfPresent.isPresent()) {
            listener.onNewData(cacheIfPresent.get());
        } else {
            loadData();
        }
    }

    public void refresh() {
        loadData();
    }

    private void loadData() {
        mExecuting = true;
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
                        mExampleDescription.putCache(getCacheKey(), data, getCommandsNames());
                        mExecuting = false;
                    }
                },
                new SyncExecutor.OnError() {
                    @Override
                    public void except(Exception e) {
                        mExecuting = false;
                        if (mListener != null) {
                            mListener.onError(e);
                        }
                    }
                });
    }

    protected List<CommandName<T,?>> getCommandsNames() {
        return ImmutableList.of();
    }

    @Override
    public void onChanged(T object) {
        if (mListener != null) {
            mListener.onNewData(object);
        }
    }

    @Override
    public void onInvalidated() {
        loadData();
    }

    protected abstract T execute() throws Exception;

    protected abstract CacheKey<T> getCacheKey();

    protected T execute(String nextToken) throws Exception {
        throw new UnsupportedOperationException("Not supported operation");
    }

    protected String getNextToken(T cache) {
        return null;
    }

    protected boolean hasMore(T cache) {
        return false;
    }

    protected T mergeData(T previous, T moreData) {
        throw new UnsupportedOperationException("Not supported operation");
    }

    protected void afterExecute(T data) {

    }

    @Override
    public void unregister() {
        mExampleDescription.unregister(getCacheKey(), this);

        checkState(mListener != null);
        mListener = null;
    }

    @Override
    public void unregisterIfRegistered() {
        mListener = null;
    }

    public boolean hasMore() {
        final Optional<T> cache = getCacheIfPresent();
        checkState(cache.isPresent(), "You can not call hasMore before success download data");
        return hasMore(cache.get());
    }

    public void loadMoreIfCan() {
        final Optional<T> cache = getCacheIfPresent();
        if (!cache.isPresent()) {
            return;
        }

        final String nextToken = getNextToken(cache.get());
        if (nextToken == null) {
            return;
        }
        if (mExecuting) {
            return;
        }
        loadMore(nextToken);
    }

    public void loadMore() {
        final Optional<T> cache = getCacheIfPresent();
        checkState(cache.isPresent(), "You can not call loadMore before success download data");

        final String nextToken = getNextToken(cache.get());
        checkState(nextToken != null, "You can not call loadMore if there is no more data");

        checkState(!mExecuting, "You can not call loadMore if executing");

        loadMore(nextToken);
    }

    private void loadMore(final String nextToken) {
        mExecuting = true;
        mSyncExecutor.executeAndReturn(
                new Callable<T>() {
                    @Override
                    public T call() throws Exception {
                        return execute(nextToken);
                    }
                },
                new SyncExecutor.OnSuccess<T>() {
                    @Override
                    public void run(T moreData) {
                        mExecuting = false;
                        final Optional<T> previousOptional = getCacheIfPresent();
                        if (!previousOptional.isPresent()) {
                            return;
                        }
                        final T previous = previousOptional.get();
                        if (!Objects.equal(getNextToken(previous), nextToken)) {
                            return;
                        }
                        final T newData = mergeData(previous, moreData);
                        mExampleDescription.putCache(getCacheKey(), newData, getCommandsNames());
                    }
                },
                new SyncExecutor.OnError() {
                    @Override
                    public void except(Exception e) {
                        mExecuting = false;
                        if (mListener != null) {
                            mListener.onError(e);
                        }
                    }
                });
    }

    public Optional<T> getCacheIfPresent() {
        return mExampleDescription.getCacheIfPresent(getCacheKey());
    }

}
