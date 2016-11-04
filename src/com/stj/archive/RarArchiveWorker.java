
package com.stj.archive;

import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import com.github.junrar.Archive;
import com.github.junrar.exception.RarException;
import com.github.junrar.rarfile.FileHeader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RarArchiveWorker extends ArchiveWorker {
    private static final String TAG = RarArchiveWorker.class.getSimpleName();

    @Override
    protected ArrayList<String> readArchiveEntry(File zipFilePath) {
        ArrayList<String> rawEntry = new ArrayList<String>();
        Archive rarFile = null;
        String fileName = null;
        try {
            rarFile = new Archive(zipFilePath);

            List<FileHeader> fileHeaders = rarFile.getFileHeaders();
            for (FileHeader fileHeader : fileHeaders) {

                if (fileHeader.isUnicode()) {
                    fileName = fileHeader.getFileNameW();
                } else {
                    fileName = fileHeader.getFileNameString();
                }
                fileName = fileName.replaceAll("\\\\", "/");

                if (!TextUtils.isEmpty(fileName)) {
                    String rootDir = zipFilePath.getName();
                    String newPath = rootDir + File.separator + fileName;
                    rawEntry.add(newPath);
                }
            }

        } catch (RarException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (rarFile != null) {
                    rarFile.close();
                    rarFile = null;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return rawEntry;
    }

    @Override
    protected void doExtractFile(String zipFilePath, String header, String destPath) {
        ExtractFileTask task = new ExtractFileTask();
        task.execute(new String[] {
                zipFilePath, header, destPath
        });
    }

    private class ExtractFileTask extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... params) {
            String archiveFilePath = params[0];
            String header = params[1];
            String destPath = params[2];
            extractFile(archiveFilePath, header, destPath);

            String fileName = new File(header).getName();
            String cacheDir = destPath + File.separator + fileName;
            return cacheDir;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            viewFile(result);
        }
    }

    private void extractFile(String archiveFilePath, String header, String destPath) {
        if (archiveFilePath != null) {
            FileOutputStream fos = null;
            Archive rarFile = null;
            try {
                String fName = new File(header).getName();
                File out = new File(destPath + File.separator + fName);
                fos = new FileOutputStream(out);
                rarFile = new Archive(new File(archiveFilePath));

                List<FileHeader> fileHeaders = rarFile.getFileHeaders();
                String fileName = null;
                for (FileHeader fileHeader : fileHeaders) {
                    if (fileHeader.isUnicode()) {
                        fileName = fileHeader.getFileNameW().trim();
                    } else {
                        fileName = fileHeader.getFileNameString().trim();
                    }
                    fileName = fileName.replaceAll("\\\\", "/");

                    if (!TextUtils.isEmpty(fileName)) {
                        if (header.equals(fileName)) {
                            rarFile.extractFile(fileHeader, fos);
                            fos.flush();
                            break;
                        }

                    }
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (RarException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (rarFile != null) {
                        rarFile.close();
                        rarFile = null;
                    }
                    if (fos != null) {
                        fos.close();
                        fos = null;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

        } else {
            Log.i(TAG, TAG + "  doExtract Fail,because archiveFilePath is null");
        }
    }

}
