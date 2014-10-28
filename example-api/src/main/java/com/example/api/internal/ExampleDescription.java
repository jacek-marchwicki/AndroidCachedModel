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
import com.appunite.cache.Command;
import com.appunite.cache.CommandName;
import com.appunite.cache.CommandsDescription;
import com.example.api.model.ResponseComment;
import com.example.api.model.ResponseComments;
import com.google.common.base.Objects;
import com.google.common.base.Optional;

import java.util.List;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ExampleDescription {

    public static final CommandName<ResponseComments, ResponseComment> COMMENT_ADDED = CommandName.of("comment_added");

    public static CacheKey<ResponseComments> forPostComments(String postGuid) {
        return CacheKey.of("posts/" + postGuid + "/comments");
    }

    private CommandsDescription mCommandsDescription = new CommandsDescription();

    @Inject
    public ExampleDescription() {
        mCommandsDescription.addCommand(COMMENT_ADDED, new Command<ResponseComments, ResponseComment>() {
            @Nonnull
            @Override
            public Command.UpdateResult<ResponseComments> apply(ResponseComment parameter, @Nonnull ResponseComments object) {
                if (!Objects.equal(object.getPostGuid(), parameter.getPostGuid())) {
                    return UpdateResult.forUpdateNotRequired();
                }
                return UpdateResult.forUpdate(ResponseComments.newWithaddedComment(object, parameter.getComment()));
            }
        });
    }

    public <T, K> void invalidate(CommandName<T, K> commandName, K parameter) {
        mCommandsDescription.invalidate(commandName, parameter);
    }

    public <T> void register(CacheKey<T> cacheKey,
                             CacheInvalidationListener<T> listener) {
        mCommandsDescription.register(cacheKey, listener);
    }

    public <T> void unregister(CacheKey<T> cacheKey, CacheInvalidationListener<T> listener) {
        mCommandsDescription.unregister(cacheKey, listener);
    }

    public <T> void putCache(CacheKey<T> cacheKey, T object,
                             List<CommandName<T, ?>> commands) {
        mCommandsDescription.putCache(cacheKey, object, commands);
    }

    public <T> Optional<T> getCacheIfPresent(CacheKey<T> cacheKey) {
        return mCommandsDescription.getCacheIfPresent(cacheKey);
    }

    public void clearAll() {
        mCommandsDescription.clearAll();
    }
}
