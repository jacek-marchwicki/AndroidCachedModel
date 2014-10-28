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
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

@SuppressWarnings("UnusedDeclaration")
public class CacheKey<T> {

    @Nonnull
    private final String mCacheKey;

    CacheKey(@Nonnull String cacheKey) {
        mCacheKey = checkNotNull(cacheKey);
    }

    public static <T> CacheKey<T> of(@Nonnull String cacheKey) {
        checkNotNull(cacheKey);
        return new CacheKey<>(cacheKey);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (!(o instanceof CacheKey)) return false;

        final CacheKey cacheKey = (CacheKey) o;

        return mCacheKey.equals(cacheKey.mCacheKey);

    }

    @Override
    public int hashCode() {
        return mCacheKey.hashCode();
    }

    @Override
    public String toString() {
        return "Key: " + mCacheKey;
    }
}
