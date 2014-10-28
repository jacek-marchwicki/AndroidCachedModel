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

package com.appunite.cache;

import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkNotNull;

public class SyncScheduler implements Scheduler {
    private boolean mLoadedData = false;

    public SyncScheduler() {
    }

    @Override
    public void schedule(@Nonnull final Runnable runnable) {
        checkNotNull(runnable);
        new Thread(new Runnable() {
            @Override
            public void run() {
                runnable.run();
                synchronized (SyncScheduler.this) {
                    mLoadedData = true;
                    SyncScheduler.this.notifyAll();
                }
            }
        }).start();
    }

    public void clear() {
        synchronized (this) {
            mLoadedData = false;
        }
    }

    public void waitForData() throws InterruptedException {
        synchronized (this) {
            while (!mLoadedData) {
                this.wait();
            }
        }
    }

    public void waitForData(long timeout) throws InterruptedException {
        synchronized (this) {
            while (!mLoadedData) {
                this.wait(timeout);
            }
        }
    }
}
