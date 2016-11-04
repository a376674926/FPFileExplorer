
package com.stj.archive;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;

public class ArchiveController {
    private static final String TAG = ArchiveController.class.getSimpleName();
    private ArchiveWorker mArchiveWorker = null;
    private File mFile = null;
    private IArchiveFileOperatorListener mArchiveListener;
    private Context mContext;
    private String mCurrentDir;

    public ArchiveController(IArchiveFileOperatorListener listener, File file) {
        mFile = file;
        mArchiveListener = listener;
        mContext = mArchiveListener.getContext();
    }

    private ArchiveWorker getWorkerByExt(File file) {
        if ((file == null) || TextUtils.isEmpty(file.getAbsolutePath())) {
            printLog("file is null,getArchiveWorker fail return null!");
            return null;
        }
        String ext = getExt(file.getAbsolutePath());

        if (!TextUtils.isEmpty(ext) && ext.equalsIgnoreCase("zip")) {
            printLog("getArchiveWorker return ZipAchiveWorker.");
            return new ZipArchiveWorker();
        } else if (!TextUtils.isEmpty(ext) && ext.equalsIgnoreCase("rar")) {
            printLog("getArchiveWorker return RarAchiveWorker.");
            return new RarArchiveWorker();
        }
        printLog("no match,getArchiveWorker return ZipAchiveWorker by default.");
        return new ZipArchiveWorker();
    }

    public void initArchiveEntry() {
        mCurrentDir = mFile.getName();
        mArchiveWorker = getWorkerByExt(mFile);
        mArchiveWorker.registArchiveOperatorListener(mArchiveListener);
        mArchiveWorker.initArchiveEntry(mFile);
    }

    public void previewArchive(String path) {
        if (mArchiveWorker.isDirectory(path)) {
            mCurrentDir = path;
            mArchiveWorker.listFile(path);
        } else {
            mArchiveWorker.previewFile(path);
        }
    }

    public void deleteTempFiles() {
        mArchiveWorker.deleteTempFile();
    }

    public boolean onBackPress() {
        if (mCurrentDir == null) {
            return true;
        }

        String curDir = new File(mCurrentDir).getParent();
        if (TextUtils.isEmpty(curDir)) {
            return true;
        } else {
            previewArchive(curDir);
            mCurrentDir = curDir;
            return false;
        }
    }

    private static String getExt(String filePath) {
        int dotPosition = filePath.lastIndexOf('.');
        if (dotPosition == -1) {
            return null;
        }

        String ext = filePath.substring(dotPosition + 1, filePath.length()).toLowerCase();
        return ext != null ? ext : null;
    }

    public String getCurrentDir() {
        return mCurrentDir;
    }

    private static void printLog(String msg) {
        Log.i(TAG, msg);
    }
}
