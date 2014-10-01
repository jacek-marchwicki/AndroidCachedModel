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

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.google.common.base.Charsets;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public abstract class BaseArrayAdapter<T> extends BaseAdapter {

    @Nullable
    protected List<T> mItems;
    @Nonnull
    private final HashFunction mHashFunction;

    public BaseArrayAdapter() {
        mHashFunction = Hashing.goodFastHash(Long.SIZE);
    }

    protected long getIdFromGuid(@Nonnull CharSequence guid) {
        return mHashFunction.hashString(checkNotNull(guid), Charsets.UTF_8).asLong();
    }

    @Override
    public final int getCount() {
        return mItems == null ? 0 : mItems.size();
    }

    @Override
    @Nonnull
    public final T getItem(int position) {
        checkState(mItems != null);
        return mItems.get(position);
    }

    @Override
    public final long getItemId(int position) {
        // We need to allow for querying for id after notifyDataSetInvalidated
        if (mItems == null || position >= mItems.size()) {
            return 0;
        }
        return getItemId(position, getItem(position));
    }

    @Override
    @Nonnull
    public final View getView(int position,
                              @Nonnull View convertView,
                              @Nonnull ViewGroup parent) {
        return getView(position, getItem(position), convertView, parent);
    }

    @Override
    public int getItemViewType(int position) {
        if (getViewTypeCount() != 1) {
            final T item = getItem(position);
            return getItemViewType(position, item);
        } else {
            return super.getItemViewType(position);
        }
    }

    protected int getItemViewType(int position,
                                  @Nonnull T item) {
        return 0;
    }

    public void swapData(@Nullable List<T> items) {
        mItems = items;
        notifyChange();
    }

    @Nullable
    public List<T> getData() {
        return mItems;
    }

    protected void notifyChange() {
        if (mItems == null) {
            notifyDataSetInvalidated();
        } else {
            notifyDataSetChanged();
        }
    }

    protected abstract long getItemId(int position,
                                      @Nonnull T item);

    protected abstract View getView(int position,
                                    @Nonnull T item,
                                    @Nullable View convertView,
                                    @Nonnull ViewGroup parent);

    @Override
    public boolean hasStableIds() {
        return true;
    }

}
