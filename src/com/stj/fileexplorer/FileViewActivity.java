/*
 * Copyright (c) 2010-2011, The MiCode Open Source Community (www.micode.net)
 *
 * This file is part of FileExplorer.
 *
 * FileExplorer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FileExplorer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SwiFTP.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.stj.fileexplorer;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.storage.StorageManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import android.os.storage.VolumeInfo;

import com.stj.fileexplorer.FileExplorerTabActivity.IBackPressedListener;
import com.stj.fileexplorer.FileExplorerTabActivity.IMenuPressedListener;
import com.stj.fileexplorer.FileViewInteractionHub.Mode;
import com.stj.fileexplorer.Util.MENU_OPERATIONS;
import com.stj.fileexplorer.R;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class FileViewActivity extends Fragment implements
        IFileInteractionListener, IBackPressedListener, IMenuPressedListener {

    public static final String EXT_FILTER_KEY = "ext_filter";

    private static final String LOG_TAG = "FileViewActivity";

    public static final String EXT_FILE_FIRST_KEY = "ext_file_first";

    public static final String ROOT_DIRECTORY = "root_directory";

    public static final String PICK_FOLDER = "pick_folder";

    private ListView mFileListView;

    // private TextView mCurrentPathTextView;
    private ArrayAdapter<FileInfo> mAdapter;

    private FileViewInteractionHub mFileViewInteractionHub;

    private FileCategoryHelper mFileCagetoryHelper;
    private FileIconHelper mFileIconHelper;

    private ArrayList<FileInfo> mFileNameList = new ArrayList<FileInfo>();
    private Activity mActivity;

    private FileCategoryActivity mFileCategoryActivity;

    private View mRootView;
    private String sdDir = Util.getSdDirectory();

    // memorize the scroll positions of previous paths
    private ArrayList<PathScrollPositionItem> mScrollPositionList = new ArrayList<PathScrollPositionItem>();
    private String mPreviousPath;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            Log.v(LOG_TAG, "received broadcast:" + intent.toString());
            if (action.equals(Intent.ACTION_MEDIA_MOUNTED)
                    || action.equals(Intent.ACTION_MEDIA_UNMOUNTED)
                    || action.equals(Intent.ACTION_MEDIA_BAD_REMOVAL)) {
                if (action.equals(Intent.ACTION_MEDIA_UNMOUNTED)
                        || action.equals(Intent.ACTION_MEDIA_BAD_REMOVAL)) {
                    final String externalVolume = Util.getExternalStoragePath(context);
                    final String currentPath = mFileViewInteractionHub.getCurrentPath();
                    if (currentPath.contains(externalVolume)) {
                        final String limitedRootPath = Util.getLimitedRootPath(mActivity);
                        mFileViewInteractionHub.setCurrentPath(limitedRootPath);
                    }
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateUI();
                    }
                });
            }
        }
    };

    private boolean mBackspaceExit;

    private ArrayList<String> mHideFolderList = new ArrayList<String>();
    //add begin by zhongrenzhan@20140417,Get files info use AsyncTask.
    private ArrayList<FileInfo> mTmpFileNameList = new ArrayList<FileInfo>();
    private ProgressDialog mProgressDialog;
    private static final int NOTIFY_UPDATE_LIST = 0;
    private static final int SHOW_LOAD_PROGRESS_DIALOG = 1;
    private FileSortHelper mSortHelper;
    private GetFileInfoAsyncTask mGetFileInfoAsyncTask;
    private boolean mIsCanBack = true;
    private Button mLeftBtn;
    private Button mMiddleBtn;
    private Button mRightBtn;
    private IBottomButtonPressListener mBottomBtnPressListener;

    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case NOTIFY_UPDATE_LIST: {
                    mFileNameList.clear();
                    synchronized (mTmpFileNameList) {
                        mFileNameList.addAll(mTmpFileNameList);
                    }
                    sortCurrentList(mSortHelper);
                    View noSdView = mRootView.findViewById(R.id.sd_not_available_page_list);
                    if (noSdView.getVisibility() == View.VISIBLE) {
                        showEmptyView(false);
                    }else {
                        showEmptyView(mFileNameList.size() == 0);
                    }
                    mFileListView.post(new Runnable() {
                        @Override
                        public void run() {
                            int pos = computeScrollPosition(mFileViewInteractionHub
                                    .getCurrentPath());
                            mFileListView.setSelection(pos);
                        }
                    });
                    if (isProgressDialogShowing()) {
                        dismissProgressDialog();
                    }
                }
                    break;
                case SHOW_LOAD_PROGRESS_DIALOG: {
                    mIsCanBack = false;
                    showProgressDialog(R.string.progress_dialog_loading_warning);
                }
                    break;
                default:
                    break;
            }
        }
    };
    
    // add end
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mActivity = getActivity();
//        mFileCategoryActivity = (FileCategoryActivity) ((FileExplorerTabActivity) mActivity)
//                .getFragment(Util.CATEGORY_TAB_INDEX);
        String[] hideFolders = mActivity.getResources().getStringArray(R.array.hide_path_list);
        mHideFolderList.clear();
        for (int i = 0; hideFolders != null && i < hideFolders.length; i++) {
            mHideFolderList.add(hideFolders[i]);
        }
        // getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);

        mRootView = inflater.inflate(R.layout.file_explorer_list, container, false);

        initBottomBtn(mRootView);

        ActivitiesManager.getInstance().registerActivity(ActivitiesManager.ACTIVITY_FILE_VIEW, mActivity);

        mFileCagetoryHelper = new FileCategoryHelper(mActivity);
        mFileViewInteractionHub = new FileViewInteractionHub(this);
        Intent intent = mActivity.getIntent();
        String action = intent.getAction();
        Log.d("debug", " FileViewActivity-------------->>action:" + action);
        if (!TextUtils.isEmpty(action)
                && (action.equals(Intent.ACTION_PICK) || action.equals(Intent.ACTION_GET_CONTENT))) {
            mFileViewInteractionHub.setMode(Mode.Pick);

            boolean pickFolder = intent.getBooleanExtra(PICK_FOLDER, false);
            if (!pickFolder) {
                String[] exts = intent.getStringArrayExtra(EXT_FILTER_KEY);
                if (exts != null) {
                    mFileCagetoryHelper.setCustomCategory(exts);
                }
            } else {
                mFileCagetoryHelper.setCustomCategory(new String[]{} /*folder only*/);
                mRootView.findViewById(R.id.pick_operation_bar).setVisibility(View.VISIBLE);

                mRootView.findViewById(R.id.button_pick_confirm).setOnClickListener(
                        new OnClickListener() {
                            public void onClick(View v) {
                                try {
                                    Intent intent = Intent.parseUri(
                                            mFileViewInteractionHub.getCurrentPath(), 0);
                                    mActivity.setResult(Activity.RESULT_OK, intent);
                                    mActivity.finish();
                                } catch (URISyntaxException e) {
                                    e.printStackTrace();
                                }
                            }
                        });

                mRootView.findViewById(R.id.button_pick_cancel).setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        mActivity.finish();
                    }
                });
            }
        } else {
            mFileViewInteractionHub.setMode(Mode.View);
        }

        mFileListView = (ListView) mRootView.findViewById(R.id.file_path_list);
        mFileIconHelper = new FileIconHelper(mActivity);
        mAdapter = new FileListAdapter(mActivity, R.layout.file_browser_item, mFileNameList, mFileViewInteractionHub,
                mFileIconHelper);

        boolean baseSd = intent.getBooleanExtra(GlobalConsts.KEY_BASE_SD, !FileExplorerPreferenceActivity.isReadRoot(mActivity));
        Log.i(LOG_TAG, "baseSd = " + baseSd);

        boolean isExternalStorageDefault = getResources().getBoolean(
                R.bool.enable_external_storage_default);
        if (isExternalStorageDefault) {
            sdDir = Util.getDefaultExternalStorage(mActivity);
        }

        String rootDir = intent.getStringExtra(ROOT_DIRECTORY);
        Log.d("debug", " FileViewActivity-------------->>rootDir:" + rootDir + " baseSd:" + baseSd);
        if (!TextUtils.isEmpty(rootDir)) {
            if (baseSd && this.sdDir.startsWith(rootDir)) {
                rootDir = this.sdDir;
            }
        } else {
            rootDir = baseSd ? this.sdDir : GlobalConsts.ROOT_PATH;
        }
        mFileViewInteractionHub.setDefaultPath(rootDir);

        String currentDir = FileExplorerPreferenceActivity.getPrimaryFolder(mActivity);
        Uri uri = intent.getData();
        if (uri != null) {
            if (baseSd && this.sdDir.startsWith(uri.getPath())) {
                currentDir = this.sdDir;
            } else {
                currentDir = uri.getPath();
            }
        } else if (isExternalStorageDefault) {
            //currentDir = this.sdDir;
            currentDir = Util.getLimitedRootPath(mActivity);
        }

        //add begin by xugaoming@20140517, add function to locate
        //to a directory
        String gotoDir = intent.getStringExtra("goto_dir");
        if (gotoDir != null)
        {
            currentDir = gotoDir;
        }
        //add end

        mFileViewInteractionHub.setCurrentPath(currentDir);
        Log.i(LOG_TAG, "CurrentDir = " + currentDir);

        mBackspaceExit = (uri != null)
                && (TextUtils.isEmpty(action)
                || (!action.equals(Intent.ACTION_PICK) && !action.equals(Intent.ACTION_GET_CONTENT)));

        mFileListView.setAdapter(mAdapter);
        mFileListView.setOnItemSelectedListener(mItemSelectedListener);

        mFileViewInteractionHub.refreshFileList();

        updateUI();
        setHasOptionsMenu(false);
        return mRootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        registerStorageReceiver();
        refresh();
        if(umsSuccess() ){
        	Toast.makeText(mActivity, mActivity.getString(R.string.storage_warning), Toast.LENGTH_LONG).show();
        }
    }
    private String getSdcardState() {
        return Environment.getExternalStoragePathState();
    }
    private boolean isAllPublicVolumesShared() {
        int []states = new int[1];
        states[0] = VolumeInfo.STATE_SHARED;
        return isAllPublicVolumesStateOK(states);
    }

    private boolean isAllPublicVolumesUnshared() {
        int []states = new int[2];
        states[0] = VolumeInfo.STATE_MOUNTED;
        states[1] = VolumeInfo.STATE_UNMOUNTED;
        return isAllPublicVolumesStateOK(states);
    }
    private boolean stateOK(int state, int []stateArray) {
        for (int st:stateArray) {
            if (state == st) {
                return true;
            }
        }
        return false;
    }
    private boolean isAllPublicVolumesStateOK(int []states) {
        boolean stateOK = true;
        int publicCount = 0;
        StorageManager mStorageManager = (StorageManager) mActivity.getSystemService(Context.STORAGE_SERVICE);
        List<VolumeInfo> allVolumes = mStorageManager.getVolumes();
        for(VolumeInfo vol:allVolumes) {
            if (vol.type == VolumeInfo.TYPE_PUBLIC) {
                publicCount ++;
                if (!stateOK(vol.state, states)) {
                    stateOK = false;
                    break;
                }
            }
        }
        if (publicCount == 0) {
            return false;
        }
        return stateOK;
    }
    private String getInternalSdcardState() {
        return Environment.getInternalStoragePathState();
    }
    private boolean isInternalSdcardAvailable() {
        String interSdcardState = getInternalSdcardState();

        if (!Environment.MEDIA_REMOVED.equals(interSdcardState)
                && !Environment.MEDIA_BAD_REMOVAL.equals(interSdcardState)
                && !Environment.MEDIA_NOFS.equals(interSdcardState)
                && !Environment.MEDIA_UNMOUNTABLE.equals(interSdcardState)
                && !Environment.internalIsEmulated()
                && !Environment.MEDIA_UNKNOWN.equals(interSdcardState)) {
            return true;
        }

        return false;
    }
    private boolean umsSuccess() {
        if (isAllPublicVolumesShared()) {
            return true;
        }
        String mSdcardState = getSdcardState();
        if (!Environment.MEDIA_REMOVED.equals(mSdcardState)
                && !Environment.MEDIA_BAD_REMOVAL.equals(mSdcardState)
                && !Environment.MEDIA_NOFS.equals(mSdcardState)
                && !Environment.MEDIA_SHARED.equals(mSdcardState)
                /*&& !Environment.MEDIA_UNKNOWN.equals(mSdcardState)*/) {
            return false;
        } else if (isInternalSdcardAvailable()
                && !Environment.MEDIA_SHARED.equals(getInternalSdcardState())) {
            return false;
        }
        return true;
    }
    private void registerStorageReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_REMOVED);
        intentFilter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);
        intentFilter.addDataScheme("file");
        mActivity.registerReceiver(mReceiver, intentFilter);
    }

    @Override
    public void onPause() {
        if (mActivity != null) {
            mActivity.unregisterReceiver(mReceiver);
        }
        super.onPause();
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        mFileViewInteractionHub.onPrepareOptionsMenu(menu);
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        mFileViewInteractionHub.onCreateOptionsMenu(menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onBack() {
        if(!mIsCanBack){
           return true;
        }
        if (mBackspaceExit || mFileViewInteractionHub == null) {
            return false;
        }

        boolean result = mFileViewInteractionHub.onBackPressed();
        return result;
    }

    private class PathScrollPositionItem {
        String path;
        int pos;
        PathScrollPositionItem(String s, int p) {
            path = s;
            pos = p;
        }
    }

    // execute before change, return the memorized scroll position
    private int computeScrollPosition(String path) {
        int pos = 0;
        if(mPreviousPath!=null) {
            if (path.startsWith(mPreviousPath)) {
                int firstVisiblePosition = mFileListView.getFirstVisiblePosition();
                if (mScrollPositionList.size() != 0
                        && mPreviousPath.equals(mScrollPositionList.get(mScrollPositionList.size() - 1).path)) {
                    mScrollPositionList.get(mScrollPositionList.size() - 1).pos = firstVisiblePosition;
                    Log.i(LOG_TAG, "computeScrollPosition: update item: " + mPreviousPath + " " + firstVisiblePosition
                            + " stack count:" + mScrollPositionList.size());
                    pos = firstVisiblePosition;
                } else {
                    mScrollPositionList.add(new PathScrollPositionItem(mPreviousPath, firstVisiblePosition));
                    Log.i(LOG_TAG, "computeScrollPosition: add item: " + mPreviousPath + " " + firstVisiblePosition
                            + " stack count:" + mScrollPositionList.size());
                }
            } else {
                int i;
                boolean isLast = false;
                for (i = 0; i < mScrollPositionList.size(); i++) {
                    if (!path.startsWith(mScrollPositionList.get(i).path)) {
                        break;
                    }
                }
                // navigate to a totally new branch, not in current stack
                if (i > 0) {
                    pos = mScrollPositionList.get(i - 1).pos;
                }

                for (int j = mScrollPositionList.size() - 1; j >= i-1 && j>=0; j--) {
                    mScrollPositionList.remove(j);
                }
            }
        }

        Log.i(LOG_TAG, "computeScrollPosition: result pos: " + path + " " + pos + " stack count:" + mScrollPositionList.size());
        mPreviousPath = path;
        return pos;
    }

    public boolean onRefreshFileList(String path, FileSortHelper sort) {
        mSortHelper = sort;
        ActionBar bar = ((FileExplorerTabActivity) mActivity).getActionBar();
        if (bar != null) {
            int selectedNavigationIndex = bar.getSelectedNavigationIndex();
            if ((mFileViewInteractionHub != null)
                    && ((mFileViewInteractionHub.isFileOperationFinished())
                    && (selectedNavigationIndex == Util.SDCARD_TAB_INDEX))) {
                mFileViewInteractionHub.refreshFileInteraction();
            }
        }

        mLeftBtn.setVisibility(isRootPath(path) ? View.GONE : View.VISIBLE);

        File file = new File(path);
        if (!file.exists() || !file.isDirectory()) {
            return false;
        }
        //modify begin by zhongrenzhan@20140414,bug:538 Get files info use AsyncTask.
        final File[] listFiles = file.listFiles(mFileCagetoryHelper.getFilter());
        if (listFiles == null) {
            return true;
        }
        mIsCanBack = false;
        mGetFileInfoAsyncTask = new GetFileInfoAsyncTask(mHandler);
        mGetFileInfoAsyncTask.execute(listFiles);
        // modify end
        return true;
    }
    //add begin by zhongrenzhan@20140414,bug:538 Get files info use AsyncTask.
    private class GetFileInfoAsyncTask extends
            AsyncTask<File[], Void, ArrayList<FileInfo>> {
        private Handler mLocalHandler;
        private Timer mTimer;
        private TimerTask mTimerTask;

        public GetFileInfoAsyncTask(Handler handler) {
            this.mLocalHandler = handler;
        }

        @Override
        protected void onPreExecute() {
            if (mTimer == null) {
                if (mTimerTask == null) {
                    mTimerTask = new TimerTask() {
                        int i = 0;
                        boolean needUpdateView = true;
                        boolean needShowLoadingDialog = true;

                        @Override
                        public void run() {
                            if (isCancelled()
                                    && !(mGetFileInfoAsyncTask.getStatus() == AsyncTask.Status.RUNNING)) {
                                return;
                            }
                            i++;
                            // Loading file info more than 200ms,notify ui
                            // update and show what had been loaded,
                            // Enhance the user experience.
                            if (i > 2 && needUpdateView) {
                                needUpdateView = false;
                                mLocalHandler.sendEmptyMessage(NOTIFY_UPDATE_LIST);
                            }
                            // Loading file info more than 1 second,notifying
                            // show loading dialog.
                            String action = mActivity.getIntent().getAction();
                            if (Intent.ACTION_GET_CONTENT.equals(action)
                                    || (i > 10 && needShowLoadingDialog)) {
                                needShowLoadingDialog = false;
                                mHandler.sendEmptyMessage(SHOW_LOAD_PROGRESS_DIALOG);
                            }
                        }
                    };
                }
            }
            mTimer = new Timer();
            mTimer.schedule(mTimerTask, 100, 100);
        }

        @Override
        protected ArrayList<FileInfo> doInBackground(File[]... params) {
            return getFileInfoList(params[0]);
        }

        @Override
        protected void onCancelled() {
            // Calling cancel AsyncTask successful,onPostExecute(Object) is
            // never invoked,so release Timer and TimerTask here.
            if (null != mTimer) {
                mTimerTask.cancel();
                mTimerTask = null;
                mTimer.cancel();
                mTimer.purge();
                mTimer = null;
            }
            mIsCanBack = true;
            mLocalHandler.sendEmptyMessage(NOTIFY_UPDATE_LIST);
        }

        @Override
        protected void onPostExecute(ArrayList<FileInfo> result) {
            if (null != mTimer) {
                mTimerTask.cancel();
                mTimerTask = null;
                mTimer.cancel();
                mTimer.purge();
                mTimer = null;
            }
            mIsCanBack = true;
            mLocalHandler.removeMessages(NOTIFY_UPDATE_LIST);
            mLocalHandler.removeMessages(SHOW_LOAD_PROGRESS_DIALOG);
            mLocalHandler.sendEmptyMessage(NOTIFY_UPDATE_LIST);
        }
    }

    private ArrayList<FileInfo> getFileInfoList(File[] listFiles) {
        if (listFiles == null) {
            return null;
        }
        synchronized (mTmpFileNameList) {
            mTmpFileNameList.clear();
        }
        for (File child : listFiles) {
            // when user cancel loading task,break here,finish the task as early
            // as possible.
            if (null != mGetFileInfoAsyncTask && mGetFileInfoAsyncTask.isCancelled()
                    && mGetFileInfoAsyncTask.getStatus() == AsyncTask.Status.RUNNING) {
                break;
            }
            // do not show selected file if in move state
            if (mFileViewInteractionHub.isMoveState()
                    && mFileViewInteractionHub.isFileSelected(child.getPath()))
                continue;

            String absolutePath = child.getAbsolutePath();
            // do not show path if is hide folder or not mount.
            if (mHideFolderList.contains(absolutePath) || !Util.isVolumeMounted(absolutePath)) {
                continue;
            }
            if (Util.isNormalFile(absolutePath) && Util.shouldShowFile(absolutePath)) {
                FileInfo lFileInfo = Util.GetFileInfo(child,
                        mFileCagetoryHelper.getFilter(), Settings.instance()
                                .getShowDotAndHiddenFiles());
                if (lFileInfo != null) {
                    mTmpFileNameList.add(lFileInfo);
                }
            }
        }
        return mTmpFileNameList;
    }

    private void showProgressDialog(final Integer strId) {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(getActivity());
        }
        mProgressDialog.setCancelable(true);
        mProgressDialog.setMessage(getString(strId));
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.setOnCancelListener(new OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
                mIsCanBack = true;
                if (mGetFileInfoAsyncTask.getStatus() == AsyncTask.Status.RUNNING) {
                    mGetFileInfoAsyncTask.cancel(true);
                }
            }
        });
        mProgressDialog.show();
    }

    private void dismissProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    private boolean isProgressDialogShowing() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            return true;
        } else {
            return false;
        }
    }
    //add end
    private void updateUI() {
        boolean sdCardReady = Util.isSDCardReady(mActivity);
        boolean isExternalStorageDefault = getResources().getBoolean(
                R.bool.enable_external_storage_default);
        if (isExternalStorageDefault) {
            sdDir = Util.getDefaultExternalStorage(mActivity);
            sdCardReady = Util.isVolumeMounted(sdDir);
        }
        View noSdView = mRootView.findViewById(R.id.sd_not_available_page_list);
        noSdView.setVisibility(sdCardReady ? View.GONE : View.VISIBLE);

        // add by lzy@20140313
        if (noSdView.getVisibility() == View.VISIBLE) {
            showEmptyView(false);
        }
        // add end.

        View navigationBar = mRootView.findViewById(R.id.navigation_bar);
        navigationBar.setVisibility(View.GONE);
        mFileListView.setVisibility(sdCardReady ? View.VISIBLE : View.GONE);

        if (sdCardReady) {
            mFileViewInteractionHub.refreshFileList();
        }
    }

    private void showEmptyView(boolean show) {
        View emptyView = mRootView.findViewById(R.id.empty_view_list);
        if (emptyView != null)
            emptyView.setVisibility(show ? View.VISIBLE : View.GONE);
        mLeftBtn.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    @Override
    public View getViewById(int id) {
        return mRootView.findViewById(id);
    }

    @Override
    public Context getContext() {
        return mActivity;
    }

    @Override
    public void onDataChanged() {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                mAdapter.notifyDataSetChanged();
            }

        });
    }

    @Override
    public void onPick(FileInfo f) {
        try {
            Intent intent = Intent.parseUri(Uri.fromFile(new File(f.filePath)).toString(), 0);
            mActivity.setResult(Activity.RESULT_OK, intent);
            mActivity.finish();
            return;
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean shouldShowOperationPane() {
        return true;
    }

    @Override
    public boolean onOperation(int id) {
        return !mIsCanBack;
    }

    //支持显示真实路径
    @Override
    public String getDisplayPath(String path) {
        final boolean isLimitViewPath = Util.isLimitViewPath(mActivity);
        final boolean isFuseLimitedPath = Util.isFuseLimitedRootPath(mActivity);
        final boolean showRealPath = FileExplorerPreferenceActivity.showRealPath(mActivity);
        if (isLimitViewPath) {
            if (showRealPath) {
                if (isFuseLimitedPath) {
                    final String limitedPath = Util.getLimitedRootPath(mActivity);
                    if (path.startsWith(limitedPath)) {
                        path = path.replaceFirst(limitedPath, "");
                        if ("".equals(path)) {
                            path = Util.FUSE_ROOT_PATH;
                        }
                    }
                }
                return path;
            } else {
                boolean isStorageVolumePath = false;
                String volumePath = null;
                String volumeDescription = null;

                HashMap<String, String> volumeMap = Util.getVolumeMap(mActivity);
                for (String key : volumeMap.keySet()) {
                    if (path.startsWith(key)) {
                        volumePath = key;
                        volumeDescription = volumeMap.get(key);
                        isStorageVolumePath = true;
                        break;
                    }
                }
                if (isStorageVolumePath
                        && volumeDescription != null && volumePath != null) {
                    path = (isFuseLimitedPath ? Util.FUSE_ROOT_PATH : "") +
                            volumeDescription + path.substring(volumePath.length());
                } else {
                    if (isFuseLimitedPath) {
                        final String limitedPath = Util.getLimitedRootPath(mActivity);
                        if (path.startsWith(limitedPath)) {
                            path = path.replaceFirst(limitedPath, "");
                            if ("".equals(path)) {
                                path = Util.FUSE_ROOT_PATH;
                            }
                        }
                    }
                }
                return path;
            }
        } else if (path.startsWith(this.sdDir) && !showRealPath) {
            return getString(R.string.sd_folder) + path.substring(this.sdDir.length());
        } else {
            return path;
        }
    }

    @Override
    public String getRealPath(String displayPath) {
        final String perfixName = getString(R.string.sd_folder);
        if (Util.isLimitViewPath(mActivity)) {
            HashMap<String, String> volumeMap = Util.getVolumeMap(mActivity);
            for (String path : volumeMap.keySet()) {
                String description = volumeMap.get(path);
                String descriptionPath = File.separator + description;
                if (displayPath.startsWith(descriptionPath)) {
                    return path + displayPath.substring(descriptionPath.length());
                }
            }
            return displayPath;
        }else if (displayPath.startsWith(perfixName)) {
            return this.sdDir + displayPath.substring(perfixName.length());
        } else {
            return displayPath;
        }
    }

    @Override
    public boolean onNavigation(String path) {
        return false;
    }

    @Override
    public boolean shouldHideMenu(int menu) {
        return false;
    }

    public void copyFile(ArrayList<FileInfo> files) {
        mFileViewInteractionHub.onOperationCopy(files);
    }

    public void refresh() {
        if (mFileViewInteractionHub != null) {
            mFileViewInteractionHub.refreshFileList();
        }
    }

    public void moveToFile(ArrayList<FileInfo> files) {
        mFileViewInteractionHub.moveFileFrom(files);
    }

    public interface SelectFilesCallback {
        // files equals null indicates canceled
        void selected(ArrayList<FileInfo> files);
    }

    public void startSelectFiles(SelectFilesCallback callback) {
        mFileViewInteractionHub.startSelectFiles(callback);
    }

    @Override
    public FileIconHelper getFileIconHelper() {
        return mFileIconHelper;
    }

    public boolean setPath(String location) {
        if (!isStartWithDefaultPaths(mActivity, location)) {
            return false;
        }
        mFileViewInteractionHub.setCurrentPath(location);
        mFileViewInteractionHub.refreshFileList();
        return true;
    }

    @Override
    public FileInfo getItem(int pos) {
        if (pos < 0 || pos > mFileNameList.size() - 1)
            return null;

        return mFileNameList.get(pos);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void sortCurrentList(FileSortHelper sort) {
        Collections.sort(mFileNameList, sort.getComparator());
        onDataChanged();
    }

    @Override
    public ArrayList<FileInfo> getAllFiles() {
        return mFileNameList;
    }

    @Override
    public void addSingleFile(FileInfo file) {
        mFileNameList.add(file);
        onDataChanged();
        if (mFileViewInteractionHub != null) {
            mFileViewInteractionHub.refreshFileList();
        }
    }

    @Override
    public int getItemCount() {
        return mFileNameList.size();
    }

    @Override
    public void runOnUiThread(Runnable r) {
        mActivity.runOnUiThread(r);
    }

    // add begin by lzy@20140319,for refresh file list view in another tab.
    @Override
    public void onRefreshFileInteraction() {
        Log.i(LOG_TAG, "auto refresh FileCategoryActivity");
//        mFileCategoryActivity.refresh();
        if (mFileViewInteractionHub != null) {
            mFileViewInteractionHub.setOperationFinished(false);
        }
    }

    private boolean isStartWithDefaultPaths(Context context, String location) {
        if (location == null) {
            return false;
        }
        StorageManager storageManager = (StorageManager) context
                .getSystemService(Context.STORAGE_SERVICE);
        try {
            Method method = StorageManager.class.getMethod("getVolumePaths", new Class[] {});
            method.setAccessible(true);
            Object volumePaths = method.invoke(storageManager, new Object[] {});
            if (volumePaths != null) {
                String[] defaultPaths = ((String[]) volumePaths);
                for (int index = 0; index < defaultPaths.length; index++) {
                    String path = defaultPaths[index];
                    if (path != null && location.startsWith(path)) {
                        return true;
                    }
                }
            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return false;
    }
    // add end.

    private class BottomBtnPressListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if (v.getId() == R.id.bottom_left_button) {
                mBottomBtnPressListener.onLeftKeyPress();
            } else if (v.getId() == R.id.bottom_middle_button) {
                mBottomBtnPressListener.onMidKeyPress();
            } else if (v.getId() == R.id.bottom_right_button) {
                mBottomBtnPressListener.onRightKeyPress();
            }
        }
    }

    public void registBottomBtnPressListener(IBottomButtonPressListener l) {
        mBottomBtnPressListener = l;
    }

    public interface IBottomButtonPressListener {
        void onLeftKeyPress();

        void onMidKeyPress();

        void onRightKeyPress();
    }

    private void initBottomBtn(View view) {
        mLeftBtn = (Button) view.findViewById(R.id.bottom_left_button);
        mMiddleBtn = (Button) view.findViewById(R.id.bottom_middle_button);
        mRightBtn = (Button) view.findViewById(R.id.bottom_right_button);
        if (mLeftBtn.getVisibility() == View.VISIBLE) {
            mLeftBtn.setOnClickListener(new BottomBtnPressListener());
        }
        if (mMiddleBtn.getVisibility() == View.VISIBLE) {
            mMiddleBtn.setOnClickListener(new BottomBtnPressListener());
        }
        if (mRightBtn.getVisibility() == View.VISIBLE) {
            mRightBtn.setOnClickListener(new BottomBtnPressListener());
        }
    }

    public void onOptionMenuBtnClick() {
        Intent intent = new Intent(mActivity, FileOptionsMenuActivity.class);
        startActivityForResult(intent, Util.MENU_OPTIONS_REQUEST_CODE);
    }

    private boolean isRootPath(String path) {
        String rootPath = Util.getLimitedRootPath(mActivity);
        if (rootPath.equals(path)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean onMenuPressed() {
        if (mLeftBtn.getVisibility() == View.VISIBLE) {
            mBottomBtnPressListener.onLeftKeyPress();
            return true;
        }
        return false;
    }

    public FileViewInteractionHub getFileViewInteractionHub() {
        return mFileViewInteractionHub;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Util.MENU_OPTIONS_REQUEST_CODE && resultCode == Util.RESULT_OK) {
            mFileViewInteractionHub.addContextMenuSelectedItem();

            int optionId = data.getIntExtra(Util.KEY_MENU_OPTIONS, -1);
            if (optionId == MENU_OPERATIONS.COPY_OPTION.ordinal()) {
                mFileViewInteractionHub.onOperationCopy();
            } else if (optionId == MENU_OPERATIONS.DELETE_OPTION.ordinal()) {
                mFileViewInteractionHub.onOperationDelete();
            } else if (optionId == MENU_OPERATIONS.DETAIL_OPTION.ordinal()) {
                mFileViewInteractionHub.onOperationInfo();
            } else if (optionId == MENU_OPERATIONS.MOVE_OPTION.ordinal()) {
                mFileViewInteractionHub.onOperationMove();
            } else if (optionId == MENU_OPERATIONS.RENAME_OPTION.ordinal()) {
                mFileViewInteractionHub.onOperationRename();
            } else if (optionId == MENU_OPERATIONS.SEND_OPTION.ordinal()) {
                mFileViewInteractionHub.onOperationSend();
            } else if (optionId == MENU_OPERATIONS.CREATE_FOLDER_OPTION.ordinal()) {
                mFileViewInteractionHub.onOperationCreateFolder();
            }
        }
    }

    private OnItemSelectedListener mItemSelectedListener = new OnItemSelectedListener() {

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            mFileViewInteractionHub.updateListViewSelectedItemPosition(position);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    };
}
