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

package com.example.api.model;

import com.google.common.collect.ImmutableList;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public class ResponseComments {

    @Nonnull
    private final String mPostGuid;

    @Nonnull
    private ImmutableList<Comment> mCommentList;

    @Nullable
    private String mNextToken;

    public ResponseComments(@Nonnull String postGuid,
                            @Nonnull ImmutableList<Comment> commentList,
                            @Nullable String nextToken) {
        mPostGuid = checkNotNull(postGuid);
        mCommentList = checkNotNull(commentList);
        mNextToken = nextToken;
    }

    public static ResponseComments newWithAppended(@Nonnull ResponseComments oldData,
                                                   @Nonnull ResponseComments moreData) {
        checkNotNull(moreData);
        checkState(oldData.mPostGuid.equals(moreData.mPostGuid));

        final ImmutableList<Comment> build = ImmutableList.<Comment>builder()
                .addAll(oldData.mCommentList)
                .addAll(moreData.mCommentList)
                .build();
        return new ResponseComments(oldData.mPostGuid, build, moreData.mNextToken);
    }

    public static ResponseComments newWithaddedComment(@Nonnull ResponseComments oldData,
                                                       @Nonnull Comment comment) {
        checkNotNull(comment);
        final ImmutableList<Comment> build = ImmutableList.<Comment>builder()
                .add(comment)
                .addAll(oldData.mCommentList)
                .build();
        return new ResponseComments(oldData.mPostGuid, build, oldData.mNextToken);
    }

    @Nullable
    public String getNextToken() {
        return mNextToken;
    }

    @Nonnull
    public String getPostGuid() {
        return mPostGuid;
    }

    @Nonnull
    public List<Comment> getCommentList() {
        return mCommentList;
    }
}
