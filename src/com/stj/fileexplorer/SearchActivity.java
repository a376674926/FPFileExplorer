/**
 * Copyright (c) 2009, Google Inc.
 * Copyright (c) 2013, The Linux Foundation. All Rights Reserved.
 * Not a Contribution.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.stj.fileexplorer;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ListActivity;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.AsyncQueryHandler;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.SearchRecentSuggestions;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.TextView;

import com.stj.fileexplorer.FileViewInteractionHub.Mode;

import com.stj.fileexplorer.R;



import java.io.File;
import java.util.HashMap;


public class SearchActivity extends ListActivity
{
    public static final String TAG = "SearchActivity";

    private AsyncQueryHandler mQueryHandler;
    private String mSearchString;
    private SearchView mSearchView;
    private ListView listView;
    private FileIconHelper mFileIconHelper = new FileIconHelper(this);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_activity);
        getActionBar().hide();
        setupSearchView();

        //mSearchString = getIntent().getStringExtra(SearchManager.QUERY);
        //Log.d(TAG, "mSearchString =" + mSearchString);
        listView = getListView();
        listView.setOnItemClickListener(new OnItemClickListener()
        {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // TODO Auto-generated method stub
                FileInfo info = (FileInfo)view.getTag();
                if (info.IsDir)
                {
                    Intent intent = new Intent(SearchActivity.this, FileExplorerTabActivity.class);
                    intent.putExtra(FileViewActivity.ROOT_DIRECTORY, info.filePath);
                    intent.putExtra("goto_dir", info.filePath);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    SearchActivity.this.startActivity(intent);
                }
                else
                {
                    try {
                        IntentBuilder.viewFile(SearchActivity.this, info.filePath);
                    } catch (ActivityNotFoundException e) {
                        Util.showPrompt(SearchActivity.this, R.string.prompt_activity_not_found);
                        Log.e(TAG, "fail to view file: " + e.toString());
                    }
                }
            }

        });

        // When the query completes cons up a new adapter and set our list adapter to that.
        mQueryHandler = new AsyncQueryHandler(getContentResolver()) {
            protected void onQueryComplete(int token, Object cookie, Cursor c) {
                Log.d(TAG, "query " + mSearchString + " " + c );
                if (c == null) {

                    return;
                }

                setListAdapter(new CursorAdapter(SearchActivity.this,
                        c, false /* no auto-requery */) {
                    @Override
                    public void bindView(View view, Context context, Cursor cursor) {
                        FileInfo fileInfo = new FileInfo();
                        fileInfo.fileName = cursor.getString(cursor.getColumnIndex("title"));
                        fileInfo.filePath = cursor.getString(cursor.getColumnIndex("_data"));
                        fileInfo.fileSize = cursor.getLong(cursor.getColumnIndex("_size"));
                        fileInfo.dbId = cursor.getLong(cursor.getColumnIndex("_id"));
                        File file = new File(fileInfo.filePath);
                        fileInfo.IsDir = file.isDirectory();
                        fileInfo.isHidden = file.isHidden();
                        fileInfo.ModifiedDate = file.lastModified();
                        ImageView checkbox = (ImageView) view.findViewById(R.id.file_checkbox);
                        checkbox.setVisibility(View.GONE);

                        Util.setText(view, R.id.file_name, fileInfo.fileName);
                        //                        Util.setText(view, R.id.file_count, fileInfo.IsDir ? "(" + fileInfo.Count + ")" : "");
                        Util.setText(view, R.id.modified_time, Util.formatDateString(context, fileInfo.ModifiedDate));
                        if (!fileInfo.IsDir)
                        {
                            Util.setText(view, R.id.file_size, (fileInfo.IsDir ? "" : Util.convertStorage(fileInfo.fileSize)));
                        }

                        ImageView lFileImage = (ImageView) view.findViewById(R.id.file_image);
                        ImageView lFileImageFrame = (ImageView) view.findViewById(R.id.file_image_frame);

                        if (fileInfo.IsDir) {
                            mFileIconHelper.pauseLoadingIcon();
                            lFileImageFrame.setVisibility(View.GONE);
                            lFileImage.setImageResource(R.drawable.folder);
                        } else {
                            mFileIconHelper.startLoadingIcon();
                            mFileIconHelper.setIcon(fileInfo, lFileImage, lFileImageFrame);
                        }
                        view.setTag(fileInfo);
                    }

                    @Override
                    public View newView(Context context, Cursor cursor, ViewGroup parent) {
                        LayoutInflater inflater = LayoutInflater.from(context);
                        View v = inflater.inflate(R.layout.file_browser_item, parent, false);
                        return v;
                    }

                });
            }
        };
        //startQuery();

    }
    private void startQuery() {
        if (TextUtils.isEmpty(mSearchString))
        {
            setListAdapter(null);
            return;
        }
        // don't pass a projection since the search uri ignores it
        Uri uri = MediaStore.Files.getContentUri("external").buildUpon().appendQueryParameter("limit", "20").build(); ;

        // kick off a query for the threads which match the search string
        mQueryHandler.startQuery(0, null, uri, new String[]{"_id","_data","_size","title"}, "title like '%" + mSearchString + "%' ", null, null);
    }
    public void setupSearchView() {

        mSearchView = (SearchView)findViewById(R.id.search_view);

        mSearchView.setIconifiedByDefault(false); // Do not iconify the widget; expand it by default
        mSearchView.setQueryHint(getString(R.string.search_hint));

        mSearchView.setOnQueryTextListener(new OnQueryTextListener()
        {

            @Override
            public boolean onQueryTextSubmit(String query) {

                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                mSearchString = newText == null ?null:newText.trim();
                startQuery();
                return false;
            }
        });
    }

}
