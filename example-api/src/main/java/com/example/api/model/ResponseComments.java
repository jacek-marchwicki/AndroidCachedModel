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

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public class ResponseComments {

    @Nonnull
    private String mPostGuid;

    @Nonnull
    private List<Comment> mCommentList;

    @Nullable
    private String mNextToken;

    public ResponseComments(@Nonnull String postGuid,
                            @Nonnull List<Comment> commentList,
                            @Nullable String nextToken) {
        mPostGuid = checkNotNull(postGuid);
        mCommentList = checkNotNull(commentList);
        mNextToken = nextToken;
    }

    public void append(@Nonnull ResponseComments moreData) {
        checkNotNull(moreData);
        checkState(mPostGuid.equals(moreData.mPostGuid));
        mCommentList.addAll(moreData.mCommentList);
        mNextToken = moreData.mNextToken;
    }

    public void addComment(@Nonnull Comment comment) {
        checkNotNull(comment);
        mCommentList.add(0, comment);
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
