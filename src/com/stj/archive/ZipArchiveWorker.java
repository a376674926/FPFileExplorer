
package com.stj.archive;

import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ZipArchiveWorker extends ArchiveWorker {
    private static final String TAG = ZipArchiveWorker.class.getSimpleName();

    @Override
    protected ArrayList<String> readArchiveEntry(File zipFilePath) {

        ZipFile zipFile = null;
        ArrayList<String> rawEntry = new ArrayList<String>();
        try {
            zipFile = new ZipFile(zipFilePath);
            @SuppressWarnings("unchecked")
            List<FileHeader> fileHeaders = zipFile.getFileHeaders();
            for (FileHeader fileHeader : fileHeaders) {
                String fileName = fileHeader.getFileName();
                if (!TextUtils.isEmpty(fileName)) {
                    String rootDir = zipFilePath.getName();
                    String newPath = rootDir + File.separator + fileName;
                    rawEntry.add(newPath);
                }
            }
        } catch (ZipException e) {
            e.printStackTrace();
        }
        return rawEntry;
    }

    private class ExtractFileTask extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... params) {
            String zipFilePath = params[0];
            String header = params[1];
            String destPath = params[2];
            extractFile(zipFilePath, header, destPath);
            String cacheDir = destPath + File.separator + header;
            return cacheDir;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            viewFile(result);
        }
    }

    private void extractFile(String zipFilePath, String zipHeaderPath, String destPath) {
        if (zipFilePath != null) {
            try {
                ZipFile zipFile = new ZipFile(zipFilePath);
                zipFile.extractFile(zipHeaderPath, destPath);
            } catch (ZipException e) {
                e.printStackTrace();
            }
        } else {
            Log.i(TAG, TAG + "  doExtract Fail,because zipFilePath is null");
        }
    }

    @Override
    protected void doExtractFile(String zipFilePath, String header, String destPath) {
        ExtractFileTask task = new ExtractFileTask();
        task.execute(new String[] {
                zipFilePath, header, destPath
        });
    }
}
