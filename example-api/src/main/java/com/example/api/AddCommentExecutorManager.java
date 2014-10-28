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

import com.appunite.cache.SyncExecutor;
import com.example.api.internal.ExampleDescription;
import com.example.api.internal.SimpleExecutorManager;
import com.example.api.model.Comment;
import com.example.api.model.ResponseComment;

import java.io.IOException;
import java.util.Random;

import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public class AddCommentExecutorManager extends SimpleExecutorManager<ResponseComment> {

    private String mBody;
    private String mPostGuid;

    private final ExampleDescription mExampleDescription;

    public AddCommentExecutorManager(@Nonnull SyncExecutor syncExecutor,
                                     @Nonnull ExampleDescription exampleDescription) {
        super(syncExecutor);
        mExampleDescription = checkNotNull(exampleDescription);
    }

    public AddCommentExecutorManager withPostGuid(@Nonnull String postGuid) {
        mPostGuid = checkNotNull(postGuid);
        return this;
    }

    public AddCommentExecutorManager withBody(@Nonnull String body) {
        mBody = checkNotNull(body);
        return this;
    }

    @Nonnull
    @Override
    protected ResponseComment execute() throws Exception {
        checkState(mPostGuid != null);
        checkState(mBody != null);
        Thread.sleep(1000);
        if ("post does not exists".equals(mPostGuid)) {
            throw new IOException("simulate api error");
        }
        final long commentIdFromApi = new Random().nextLong();
        return new ResponseComment(mPostGuid, new Comment(commentIdFromApi, mBody));
    }

    @Override
    protected void afterExecute(@Nonnull ResponseComment data) {
        super.afterExecute(data);
        mExampleDescription.invalidate(ExampleDescription.COMMENT_ADDED, data);
    }
}
