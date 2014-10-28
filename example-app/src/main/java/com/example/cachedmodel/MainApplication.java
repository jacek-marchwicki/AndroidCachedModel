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

package com.example.cachedmodel;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import com.appunite.cache.Scheduler;
import com.appunite.cache.SyncExecutor;
import com.example.api.internal.ExampleDescription;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

public class MainApplication extends Application {

    private SyncExecutor mSyncExecutor;
    private ExampleDescription mExampleDescription;

    @Override
    public void onCreate() {
        super.onCreate();

        final Handler handler = new Handler(Looper.getMainLooper());
        final Scheduler scheduler = new Scheduler() {

            @Override
            public void schedule(@Nonnull Runnable runnable) {
                handler.post(runnable);
            }
        };

        final ThreadPoolExecutor executor = new ThreadPoolExecutor(3, 3,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>());
        mSyncExecutor = new SyncExecutor(MoreExecutors.listeningDecorator(executor), scheduler);
        mExampleDescription = new ExampleDescription();
    }

    public static MainApplication fromApplication(Application application) {
        return (MainApplication) application;
    }

    public ExampleDescription getExampleDescription() {
        return mExampleDescription;
    }

    public SyncExecutor getSyncExecutor() {
        return mSyncExecutor;
    }
}
