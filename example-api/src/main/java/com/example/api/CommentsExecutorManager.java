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

package com.example.api;

import com.appunite.cache.CacheKey;
import com.appunite.cache.CommandName;
import com.appunite.cache.SyncExecutor;
import com.example.api.internal.ExampleDescription;
import com.example.api.internal.ExecutorManager;
import com.example.api.model.Comment;
import com.example.api.model.ResponseComments;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.util.List;


import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public class CommentsExecutorManager extends ExecutorManager<ResponseComments> {

    private String mPostGuid;

    public CommentsExecutorManager(@Nonnull SyncExecutor syncExecutor,
                                   @Nonnull ExampleDescription exampleDescription) {
        super(syncExecutor, exampleDescription);
    }


    public CommentsExecutorManager withPostGuid(@Nonnull String postGuid) {
        mPostGuid = checkNotNull(postGuid);
        return this;
    }

    @Nonnull
    @Override
    protected ResponseComments execute() throws Exception {
        return execute(null);
    }

    @Nonnull
    @Override
    protected ResponseComments execute(@Nullable String nextToken) throws Exception {
        checkState(mPostGuid != null);

        // This is normally done on server ;)
        Thread.sleep(1000);

        int pos;
        if (nextToken != null) {
            pos = Integer.parseInt(nextToken);
        } else {
            pos = 0;
        }

        final ImmutableList.Builder<Comment> posts = ImmutableList.builder();
        for (int i = 0; i < 10; ++i) {
            posts.add(new Comment(pos, "post: " + mPostGuid + ", comment: " + pos));
            pos++;
        }

        return new ResponseComments(mPostGuid, posts.build(), String.valueOf(pos));
    }

    @Override
    protected boolean hasMore(@Nonnull ResponseComments cache) {
        return getNextToken(cache) != null;
    }

    @Nullable
    @Override
    protected String getNextToken(@Nonnull ResponseComments cache) {
        return cache.getNextToken();
    }

    @Nonnull
    @Override
    protected ResponseComments mergeData(@Nonnull ResponseComments previous,
                                         @Nonnull ResponseComments moreData) {
        return ResponseComments.newWithAppended(previous, moreData);
    }

    @Nonnull
    @Override
    protected CacheKey<ResponseComments> getCacheKey() {
        return ExampleDescription.forPostComments(mPostGuid);
    }

    @Override
    protected List<CommandName<ResponseComments, ?>> getCommandsNames() {
        return ImmutableList.<CommandName<ResponseComments, ?>>of(
                ExampleDescription.COMMENT_ADDED);
    }
}
