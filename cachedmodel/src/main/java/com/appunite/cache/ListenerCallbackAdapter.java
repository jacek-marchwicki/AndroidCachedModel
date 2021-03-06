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

public class ListenerCallbackAdapter<T> implements ListenerCallback<T> {
    private final static ListenerCallback<?> EMPTY = new ListenerCallbackAdapter<Object>() {
    };

    @Nonnull
    @SuppressWarnings("unchecked")
    public static <X> ListenerCallback<X> getEmpty() {
        return (ListenerCallbackAdapter<X>) EMPTY;
    }

    @Override
    public void onError(@Nonnull Throwable e) {
    }

    @Override
    public void onNewData(@Nonnull T data) {
    }
}
