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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public interface Command<T, K> {
    @Nonnull
    public UpdateResult<T> apply(@Nullable K parameter, @Nonnull T object);

    public static class UpdateResult<T> {
        public static enum Type {
            UPDATED,
            UPDATE_NOT_REQUIRED,
            INVALIDATE
        }

        @Nullable
        private final T mParam;
        @Nonnull
        private final Type mType;

        private UpdateResult(@Nullable T param, @Nonnull Type type) {
            mType = checkNotNull(type);
            if (Type.UPDATED.equals(type)) {
                checkArgument(param != null);
            } else {
                checkArgument(param == null);
            }
            mParam = param;
        }

        public boolean isInvalidate() {
            return Type.INVALIDATE.equals(mType);
        }

        public boolean isUpdate() {
            return Type.UPDATED.equals(mType);
        }

        @Nonnull
        public T getParam() {
            checkState(Type.UPDATED.equals(mType));
            assert mParam != null;
            return mParam;
        }

        public static <T> UpdateResult<T>forUpdate(@Nonnull T param) {
            checkNotNull(param);
            return new UpdateResult<>(param, Type.UPDATED);
        }

        public static <T> UpdateResult<T>forUpdateNotRequired() {
            return new UpdateResult<>(null, Type.UPDATE_NOT_REQUIRED);
        }

        public static <T> UpdateResult<T>forInvalidate() {
            return new UpdateResult<>(null, Type.INVALIDATE);
        }


    }
}
