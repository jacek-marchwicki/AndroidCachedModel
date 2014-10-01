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

import com.appunite.cache.ListenerCallback;
import com.appunite.cache.SameThreadScheduler;
import com.appunite.cache.SyncExecutor;
import com.example.api.internal.ExampleDescription;
import com.example.api.model.ResponseComment;
import com.google.common.util.concurrent.MoreExecutors;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@SuppressWarnings("unchecked")
public class AddCommentExecutorManagerTest {

    private AddCommentExecutorManager mAddCommentExecutorManager;

    @Before
    public void setUpExecutorManager() throws Exception {
        final ExampleDescription exampleDescription = new ExampleDescription();
        final SyncExecutor syncExecutor = new SyncExecutor(MoreExecutors.newDirectExecutorService(),
                new SameThreadScheduler());
        mAddCommentExecutorManager = new AddCommentExecutorManager(syncExecutor, exampleDescription);
    }

    @Test
    public void testAddAComment() throws Exception {
        final ListenerCallback<ResponseComment> listener = mock(ListenerCallback.class);

        final AddCommentExecutorManager executorManager = mAddCommentExecutorManager.withPostGuid("123").withBody("some body");
        executorManager.register(listener);

        final ArgumentCaptor<ResponseComment> captor = ArgumentCaptor.forClass(ResponseComment.class);
        verify(listener).onNewData(captor.capture());
        executorManager.unregister();

        final ResponseComment newData = captor.getValue();
        assertThat(newData, is(notNullValue()));
        assertThat(newData.getPostGuid(), is(equalTo("123")));
        assertThat(newData.getComment(), is(notNullValue()));
        assertThat(newData.getComment().getBody(), is(equalTo("some body")));
    }

    @Test
    public void testError() throws Exception {
        final ListenerCallback<ResponseComment> listener = mock(ListenerCallback.class);

        final AddCommentExecutorManager executorManager = mAddCommentExecutorManager.withPostGuid("post does not exists").withBody("some body");
        executorManager.register(listener);

        verify(listener).onError(notNull(IOException.class));

        executorManager.unregister();
    }
}
