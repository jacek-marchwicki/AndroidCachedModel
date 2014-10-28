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

import javax.annotation.Nullable;

public class Comment {
    private final long mId;
    @Nullable
    private final String mBody;

    public Comment(long id,
                   @Nullable String body) {
        mId = id;
        mBody = body;
    }

    public long getId() {
        return mId;
    }

    @Nullable
    public String getBody() {
        return mBody;
    }
}
