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

package com.example.cachedmodel;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.TextView;

import com.appunite.cache.ListenerCallback;
import com.example.api.CommentsExecutorManager;
import com.example.api.model.Comment;
import com.example.api.model.ResponseComments;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;


public class MainActivity extends Activity {

    private CommentsAdapter mAdapter;

    public static class CommentsAdapter extends BaseArrayAdapter<Comment> {

        @Nonnull
        private final Context mContext;

        public CommentsAdapter(@Nonnull Context context) {
            mContext = checkNotNull(context);
        }

        @Override
        protected long getItemId(int position,
                                 @Nonnull Comment item) {
            return item.getId();
        }

        @Override
        protected View getView(int position,
                               @Nonnull Comment item,
                               @Nullable View convertView,
                               @Nonnull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater
                        .from(mContext)
                        .inflate(android.R.layout.simple_list_item_1, parent, false);
            }
            final TextView textView = (TextView) convertView.findViewById(android.R.id.text1);
            textView.setText(item.getBody());
            return convertView;
        }
    }

    private TextView mErrorView;
    private ListView mListView;
    private View mProgress;
    private CommentsExecutorManager mExecutorManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mErrorView = (TextView) findViewById(R.id.error);
        mListView = (ListView) findViewById(R.id.list);
        mProgress = findViewById(R.id.progress);

        mAdapter = new CommentsAdapter(this);
        mListView.setAdapter(mAdapter);

        mExecutorManager = new CommentsExecutorManager(
                MainApplication.fromApplication(getApplication()).getSyncExecutor(),
                MainApplication.fromApplication(getApplication()).getExampleDescription())
                .withPostGuid("123");
        mExecutorManager
                .register(new ListenerCallback<ResponseComments>() {
                    @Override
                    public void onError(Throwable e) {
                        mProgress.setVisibility(View.GONE);
                        mErrorView.setVisibility(View.VISIBLE);
                        mErrorView.setText(e.getMessage());
                        mAdapter.swapData(null);
                    }

                    @Override
                    public void onNewData(ResponseComments data) {
                        mProgress.setVisibility(View.GONE);
                        mErrorView.setVisibility(View.GONE);
                        mAdapter.swapData(data.getCommentList());
                        loadMore();
                    }
                });

        mErrorView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mErrorView.setVisibility(View.GONE);
                mProgress.setVisibility(View.VISIBLE);
                mExecutorManager.refresh();
            }
        });

        mListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                loadMore();
            }
        });
    }

    private void loadMore() {
        if (mListView.getFirstVisiblePosition() + mListView.getChildCount() + 5 > mAdapter.getCount()) {
            mExecutorManager.loadMoreIfCan();
        }
    }

    @Override
    protected void onDestroy() {
        mExecutorManager.unregister();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_add) {
            startActivity(new Intent(this.getApplicationContext(), AddCommentActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
