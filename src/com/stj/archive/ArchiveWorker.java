
package com.stj.archive;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Log;

import com.stj.fileexplorer.IntentBuilder;
import com.stj.fileexplorer.Util;

import com.stj.fileexplorer.R;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

abstract class ArchiveWorker {
    private Map<String, ArrayList<ArchiveNode>> mArchiveFrame = new HashMap<String, ArrayList<ArchiveNode>>();
    private static final String TAG = ArchiveWorker.class.getSimpleName();
    protected IArchiveFileOperatorListener mArchiveFileOperatorListener;
    private String mAbsoluteZipFilePath = null;
    private static final String sTempDir = Environment.getExternalStorageDirectory()
            .getAbsolutePath() + File.separator + ".FPFileArchiveTemp";

    private Context mContext;

    protected abstract ArrayList<String> readArchiveEntry(File zipFilePath);

    protected abstract void doExtractFile(String zipFilePath, String header, String destPath);

    public void registArchiveOperatorListener(IArchiveFileOperatorListener listener) {
        mArchiveFileOperatorListener = listener;
        mContext = listener.getContext();
    }

    protected void initArchiveEntry(File f) {
        mAbsoluteZipFilePath = f.getAbsolutePath();
        LoadArchiveFileTask task = new LoadArchiveFileTask();
        task.execute(f);
    }

    protected void listFile(String path) {
        ArrayList<ArchiveNode> nodes = listChilds(path);
        mArchiveFileOperatorListener.notifyUpdateUI(nodes);
    }

    protected void previewFile(String path) {
        File cacheDir = new File(sTempDir);
        if (!cacheDir.exists()) {
            cacheDir.mkdir();
        }
        String zipFileRoot = new File(mAbsoluteZipFilePath).getName() + File.separator;
        String header = path.substring(zipFileRoot.length(), path.length());
        doExtractFile(mAbsoluteZipFilePath, header, sTempDir);
    }

    protected void viewFile(String path) {
        try {
            IntentBuilder.viewFile(mContext, path);
        } catch (ActivityNotFoundException e) {
            Util.showPrompt(mContext, R.string.prompt_activity_not_found);
            Log.e(TAG, "fail to view file: " + e.toString());
        }
    }

    protected void deleteTempFile() {
        final File tempFile = new File(sTempDir);
        if (tempFile.exists()) {
            asnycExecute(new Runnable() {
                @Override
                public void run() {
                    deleteFile(tempFile);
                }
            });
        }
    }

    private void asnycExecute(Runnable r) {
        final Runnable _r = r;
        new AsyncTask<Object, Void, Object>() {
            @Override
            protected Object doInBackground(Object... params) {
                PowerManager.WakeLock wl = null;
                if (mContext != null) {
                    PowerManager pm = (PowerManager) mContext
                            .getSystemService(Context.POWER_SERVICE);
                    wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, mContext.getPackageName());
                    wl.acquire();

                }
                _r.run();
                if (wl != null) {
                    wl.release();
                }
                return null;
            }
        }.execute();
    }

    private void deleteFile(File f) {
        if (f == null) {
            Log.i(TAG, "DeleteFile: null parameter");
            return;
        }
        File file = new File(f.getAbsolutePath());
        boolean directory = file.isDirectory();
        if (directory) {
            for (File child : file.listFiles()) {
                deleteFile(child);
            }
        }

        String path = file.getAbsolutePath();
        if (!sTempDir.equals(path)) {
            file.delete();
        }
    }

    private ArrayList<ArchiveNode> listChilds(String path) {

        if (path == null) {
            return null;
        }

        int length = path.length();

        // Compatible for the path end with '/' or not.
        if ((path.charAt(length - 1)) != File.separatorChar) {
            path = path + File.separatorChar;
        }
        return mArchiveFrame.get(path);
    }

    protected boolean isDirectory(String path) {

        if (TextUtils.isEmpty(path)) {
            Log.i(TAG, TAG + " isDirectory path is null,return error!");
            return true;
        }

        String parent = new File(path).getParent();
        if (TextUtils.isEmpty(parent)) {
            Log.i(TAG, TAG + " isDirectory parent is null,return error!");
            return true;
        }

        ArrayList<ArchiveNode> nodes = listChilds(parent);
        int start = path.lastIndexOf(File.separator) + 1;
        String newNode = path.substring(start, path.length());
        for (ArchiveNode node : nodes) {
            if (newNode.equals(node.getName())) {
                return node.isDirectory();
            }
        }
        return false;
    }

    private void buildDirTree(ArrayList<String> rawEntry) {
        Set<String> parents = getAllParentPaths(rawEntry);
        Iterator<String> iterator = parents.iterator();
        while (iterator.hasNext()) {
            String parent = iterator.next();
            buildArchiveFrame(parent, rawEntry);
        }
    }

    private Set<String> getAllParentPaths(ArrayList<String> rawEntry) {
        Set<String> parents = new HashSet<String>();

        for (String path : rawEntry) {
            if (!TextUtils.isEmpty(path)) {
                buildPathParents(new File(path), parents);
            }
        }

        return parents;
    }

    private void buildPathParents(File file, Set<String> parents) {
        String parent = file.getParent();
        if (parent != null) {
            String path = parent + File.separator;
            if (!parents.contains(path)) {
                parents.add(path);
            }
            buildPathParents(new File(parent), parents);
        }
    }

    private void buildArchiveFrame(String parent, ArrayList<String> rawEntry) {
        ArrayList<ArchiveNode> mArchiveNodes = new ArrayList<ArchiveNode>();

        for (String entry : rawEntry) {
            if (entry.startsWith(parent)) {
                int start = parent.length();
                int end = entry.indexOf(File.separator, start);
                boolean isDirectory = true;
                if (end == -1) {
                    end = entry.length();
                    isDirectory = false;
                }
                // end = (end == -1) ? entry.length() : end;
                String child = entry.substring(start, end);
                if (!TextUtils.isEmpty(child)) {
                    if (!hasSameChildInParentDir(parent, child)) {
                        ArchiveNode node = new ArchiveNode();
                        node.setParent(parent);
                        node.setName(child);
                        node.setPath(parent + child);
                        node.setDirectory(isDirectory);
                        mArchiveNodes.add(node);
                        mArchiveFrame.put(parent, mArchiveNodes);
                    }
                }
            }
        }
    }

    private boolean hasSameChildInParentDir(String parent, String child) {
        ArrayList<ArchiveNode> archiveNodes = mArchiveFrame.get(parent);
        if (archiveNodes != null) {
            for (ArchiveNode node : archiveNodes) {
                if (node.getName().equals(child)) {
                    return true;
                }
            }
        }

        return false;
    }

    private class LoadArchiveFileTask extends AsyncTask<File, Void, ArrayList<ArchiveNode>> {

        @Override
        protected void onPreExecute() {
            mArchiveFileOperatorListener.onArchiveFileLoadStart();
            super.onPreExecute();
        }

        @Override
        protected ArrayList<ArchiveNode> doInBackground(File... files) {
            File file = files[0];
            ArrayList<ArchiveNode> nodes = null;
            if (file != null) {
                ArrayList<String> rawEntry = readArchiveEntry(file);
                buildDirTree(rawEntry);
                nodes = listChilds(file.getName());
            }
            return nodes;
        }

        @Override
        protected void onPostExecute(ArrayList<ArchiveNode> result) {
            super.onPostExecute(result);
            mArchiveFileOperatorListener.onArchiveFileLoadComplete(result);
        }

    }
}
