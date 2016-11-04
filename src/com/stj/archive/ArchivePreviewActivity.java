
package com.stj.archive;

import com.stj.fileexplorer.R;

import android.app.ActionBar;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.stj.fileexplorer.FileSortHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

public class ArchivePreviewActivity extends ListActivity implements IArchiveFileOperatorListener {
    private final String TAG = ArchivePreviewActivity.class.getSimpleName();
    private final boolean DEBUG = true;
    private ListView mListView = null;
    private ArchiveListAdapter mAdapter = null;
    private FileSortHelper mSortHelper;
    private ProgressDialog mProgressDialog;
    private ArchiveController mController;

    private ArrayList<ArchiveNode> mFileList = new ArrayList<ArchiveNode>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        overridePendingTransition(0, 0);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.archive_preview_activity);
        mListView = getListView();
        mSortHelper = new FileSortHelper();

        String path = getPath(ArchivePreviewActivity.this, getIntent());

        if (path == null) {
            log("onCreate Path is null return!");
            return;
        }

        File file = new File(path);
        mController = new ArchiveController(this, file);
        mController.initArchiveEntry();

        updateTitle(mController.getCurrentDir());
        setActionBarIcon();
    }

    private void onListItemClick(AdapterView<?> parent, View view, int position, long id) {
        ArchiveNode node = mAdapter.getItem(position);
        String currentPath = node.getPath();
        mController.previewArchive(currentPath);
        updateTitle(mController.getCurrentDir());
    }

    private String getPath(Context context, Intent intent) {
        if (intent == null) {
            return null;
        }
        Uri data = intent.getData();
        if (data != null) {
            return data.getPath();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private void sortCurrentList(FileSortHelper sort) {
        Collections.sort(mFileList, sort.getComparator());
        onDataChanged();
    }

    private void onDataChanged() {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                mAdapter.notifyDataSetChanged();
            }
        });
    }

    private void showEmptyView(boolean show) {
        View emptyView = findViewById(R.id.empty_view_archive);
        if (emptyView != null) {
            emptyView.setVisibility(show ? View.VISIBLE : View.GONE);
        }

    }

    private void log(String msg) {
        if (DEBUG) {
            Log.i(TAG, msg);
        }
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, 0);
    }

    @Override
    public void onBackPressed() {
        if (mController.onBackPress()) {
            mController.deleteTempFiles();
            super.onBackPressed();
        }
        updateTitle(mController.getCurrentDir());
    }

    @Override
    public void onArchiveFileLoadStart() {
        showProgress(getString(R.string.progress_dialog_loading_warning));
    }

    @Override
    public void onArchiveFileLoadComplete(ArrayList<ArchiveNode> nodes) {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }

        if (nodes != null) {
            mFileList.addAll(nodes);
        }

        mAdapter = new ArchiveListAdapter(this, R.layout.file_browser_item, mFileList);
        mListView.setAdapter(mAdapter);
        sortCurrentList(mSortHelper);
        showEmptyView(mFileList.size() == 0);
        setListViewClickListener();
    }

    private void setListViewClickListener() {
        mListView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                onListItemClick(parent, view, position, id);
            }
        });
    }

    private void showProgress(String msg) {
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage(msg);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();
    }

    private void updateTitle(String title) {
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setTitle(title);
        }
    }

    private void setActionBarIcon() {
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setIcon(R.drawable.file_icon_zip);
        }
    }

    @Override
    public Context getContext() {
        return this;
    }

    @Override
    public void notifyUpdateUI(ArrayList<ArchiveNode> nodes) {
        mFileList.clear();
        if (nodes != null) {
            mFileList.addAll(nodes);
        }
        sortCurrentList(mSortHelper);
        showEmptyView(mFileList.size() == 0);
    }
}
