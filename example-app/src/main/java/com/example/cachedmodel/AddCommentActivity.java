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
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.appunite.cache.ListenerCallback;
import com.example.api.AddCommentExecutorManager;
import com.example.api.model.ResponseComment;

public class AddCommentActivity extends Activity {

    private TextView mCommentBody;
    private View mProgressLayout;


    private AddCommentExecutorManager mExecutorManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_add_comment);

        mCommentBody = (TextView) findViewById(R.id.comment_body);
        findViewById(R.id.send_button)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        sendComment();
                    }
                });
        mProgressLayout = findViewById(R.id.progress_layout);
    }

    @Override
    protected void onDestroy() {
        unregisterAddCommentExecutorManager();
        super.onDestroy();
    }

    private void unregisterAddCommentExecutorManager() {
        if (mExecutorManager != null) {
            mExecutorManager.unregister();
            mExecutorManager = null;
        }
    }

    private void sendComment() {
        final String comment = String.valueOf(mCommentBody.getText());

        unregisterAddCommentExecutorManager();
        mExecutorManager = new AddCommentExecutorManager(
                MainApplication.fromApplication(getApplication()).getSyncExecutor(),
                MainApplication.fromApplication(getApplication()).getExampleDescription())
                .withPostGuid("123")
                .withBody(comment);
        mProgressLayout.setVisibility(View.VISIBLE);
        mExecutorManager.register(new ListenerCallback<ResponseComment>() {
            @Override
            public void onError(Throwable e) {
                mProgressLayout.setVisibility(View.GONE);
                unregisterAddCommentExecutorManager();
                new AlertDialog.Builder(AddCommentActivity.this)
                        .setTitle("Error")
                        .setMessage(e.getMessage())
                        .show();
            }

            @Override
            public void onNewData(ResponseComment data) {
                mProgressLayout.setVisibility(View.GONE);
                unregisterAddCommentExecutorManager();
                mCommentBody.setText(null);
                Toast.makeText(AddCommentActivity.this, "Comment sent", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
