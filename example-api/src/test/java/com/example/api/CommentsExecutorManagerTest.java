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
import com.appunite.cache.ListenerCallbackAdapter;
import com.appunite.cache.SameThreadScheduler;
import com.appunite.cache.SyncExecutor;
import com.example.api.internal.ExampleDescription;
import com.example.api.model.ResponseComment;
import com.example.api.model.ResponseComments;
import com.google.common.util.concurrent.MoreExecutors;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@SuppressWarnings("unchecked")
public class CommentsExecutorManagerTest {

    private CommentsExecutorManager mCommentsExecutorManager;
    private AddCommentExecutorManager mAddCommentExecutorManager;

    @Before
    public void setUpExecutorManager() throws Exception {
        final ExampleDescription exampleDescription = new ExampleDescription();
        final SyncExecutor syncExecutor = new SyncExecutor(MoreExecutors.newDirectExecutorService(),
                new SameThreadScheduler());
        mCommentsExecutorManager = new CommentsExecutorManager(syncExecutor, exampleDescription);
        mAddCommentExecutorManager = new AddCommentExecutorManager(syncExecutor, exampleDescription);
    }

    @Test
    public void testExecutorManager() throws Exception {
        final ListenerCallback<ResponseComments> listener = mock(ListenerCallback.class);
        final CommentsExecutorManager executorManager = mCommentsExecutorManager.withPostGuid("123");
        executorManager.register(listener);

        final ArgumentCaptor<ResponseComments> captor = ArgumentCaptor.forClass(ResponseComments.class);
        verify(listener).onNewData(captor.capture());

        assertThat(captor.getValue(), is(notNullValue()));
        assertThat(captor.getValue().getNextToken(), is(equalTo("10")));
        assertThat(captor.getValue().getCommentList(), hasSize(10));
        assertThat(captor.getValue().getCommentList().get(0).getBody(), is(equalTo("post: 123, comment: 0")));
        assertThat(captor.getValue().getCommentList().get(0).getId(), is(equalTo(0L)));
        assertThat(captor.getValue().getPostGuid(), is(equalTo("123")));

        executorManager.unregister();
    }

    @Test
    public void testLoadMore() throws Exception {
        final ListenerCallback<ResponseComments> listener = mock(ListenerCallback.class);
        final CommentsExecutorManager executorManager = mCommentsExecutorManager.withPostGuid("123");
        executorManager.register(listener);

        verify(listener).onNewData(any(ResponseComments.class));
        reset(listener);
        assertThat(executorManager.hasMore(), is(equalTo(true)));

        executorManager.loadMore();

        final ArgumentCaptor<ResponseComments> captor = ArgumentCaptor.forClass(ResponseComments.class);
        verify(listener).onNewData(captor.capture());

        assertThat(captor.getValue(), is(notNullValue()));
        assertThat(captor.getValue().getNextToken(), is(equalTo("20")));
        assertThat(captor.getValue().getCommentList(), hasSize(20));
        assertThat(captor.getValue().getCommentList().get(10).getBody(), is(equalTo("post: 123, comment: 10")));

        executorManager.unregister();
    }

    @Test
    public void testInvalidation() throws Exception {
        final ListenerCallback<ResponseComments> listener = mock(ListenerCallback.class);
        final CommentsExecutorManager executorManager = mCommentsExecutorManager.withPostGuid("123");
        executorManager.register(listener);

        verify(listener).onNewData(any(ResponseComments.class));
        reset(listener);

        {
            mAddCommentExecutorManager
                    .withPostGuid("123")
                    .withBody("some")
                    .register(new ListenerCallbackAdapter<ResponseComment>());
        }

        final ArgumentCaptor<ResponseComments> captor = ArgumentCaptor.forClass(ResponseComments.class);
        verify(listener).onNewData(captor.capture());

        assertThat(captor.getValue(), is(notNullValue()));
        assertThat(captor.getValue().getNextToken(), is(equalTo("10")));
        assertThat(captor.getValue().getCommentList(), hasSize(11));
        assertThat(captor.getValue().getCommentList().get(0).getBody(), is(equalTo("some")));

        executorManager.unregister();
    }

    @Test
    public void testDoesNotInvalidate() throws Exception {
        final ListenerCallback<ResponseComments> listener = mock(ListenerCallback.class);
        final CommentsExecutorManager executorManager = mCommentsExecutorManager.withPostGuid("123");
        executorManager.register(listener);

        verify(listener).onNewData(any(ResponseComments.class));
        reset(listener);

        {
            mAddCommentExecutorManager
                    .withPostGuid("other_post_id")
                    .withBody("some")
                    .register(new ListenerCallbackAdapter<ResponseComment>());
        }

        verifyNoMoreInteractions(listener);
        executorManager.unregister();
    }
}
