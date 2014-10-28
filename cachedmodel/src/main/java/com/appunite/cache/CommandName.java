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

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

@SuppressWarnings("UnusedDeclaration")
public final class CommandName<T, K> {
    @Nonnull
    private final String mCommandName;

    CommandName(@Nonnull String commandName) {
        mCommandName = commandName;
    }
    public static <T, K> CommandName<T, K> of(@Nonnull String commandName) {
        return new CommandName<>(checkNotNull(commandName));
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (!(o instanceof CommandName)) return false;

        final CommandName that = (CommandName) o;

        return mCommandName.equals(that.mCommandName);

    }

    @Override
    public int hashCode() {
        return mCommandName.hashCode();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("commandName", mCommandName)
                .toString();
    }
}
