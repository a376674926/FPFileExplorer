
package com.stj.fileexplorer;

import android.content.Context;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.util.Log;

import com.stj.fileexplorer.R;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

public class StorageHelper {
    private static final String TAG = "StorageHelper";
    private static StorageHelper instance;
    private String INTERNAL_STORAGE = null;
    private String EXTERNAL_STORAGE = null;
    private boolean DEBUG = false;
    private Context mContext = null;
    private ArrayList<Storage> mStorages = new ArrayList<Storage>();
    private String mTempExternPath = null;

    public StorageHelper(Context mContext) {
        this.mContext = mContext;
    }

    class Storage {
        String mountPoint;
        boolean isPrimary;
        boolean isRemovable;
        boolean isAllowMassStorage;
        boolean isEmulated;
        int descriptionId;
    }

    public static StorageHelper getInstance(Context context) {
        if (instance == null) {
            instance = new StorageHelper(context);
            instance.initStoragePaths();
        }
        return instance;
    }

    private String getMainBlockName() {
        String mainBlockName = "mmcblk0";

        final String MOUNTS_PATH = "/proc/mounts";
        File file = new File(MOUNTS_PATH);
        if (file == null || !file.exists() || !file.canRead()) {
            return mainBlockName;
        }

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String line = null;
            while ((line = reader.readLine()) != null) {
                if (line.contains(" /system ")) {
                    String[] strs = line.split("\\s");
                    final String SYSTEM_PATH = strs[0];
                    final String SYSTEM_REALLY_PATH = new File(SYSTEM_PATH).getCanonicalPath();
                    strs = SYSTEM_REALLY_PATH.split(File.separator);
                    final String blockName = strs[strs.length - 1].trim();
                    if (blockName != null && blockName.matches("^[a-zA-Z]+[0-9]+[a-zA-Z]+")) {
                        int index = blockName.indexOf("[0-9]+");
                        mainBlockName = blockName.substring(0, index).trim();
                        break;
                    }
                }
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
            return mainBlockName;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return mainBlockName;
    }

    private int getInernalStoragePartitionNum() {
        int num = -1;
        final String PARTITIONS_PATH = "/proc/partitions";
        File file = new File(PARTITIONS_PATH);
        if (file == null || !file.exists() || !file.canRead()) {
            return num;
        }

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String line = null;
            String mainBlockName = getMainBlockName();
            while ((line = reader.readLine()) != null) {
                if (line.contains(mainBlockName)) {
                    String[] strs = line.split("[\\s]+");
                    if (strs != null && strs.length > 4) {
                        String str = strs[strs.length - 3].trim();
                        if (str.matches("[0-9]+")) {
                            int index = Integer.valueOf(str);
                            if (num < index) {
                                num = index;
                            }
                        }
                    }
                }
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
            num = -1;
        } catch (NumberFormatException e) {
            e.printStackTrace();
            num = -1;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return num;
    }

    private void initStoragePaths() {
        String internalStorage = "/mnt/storage/sdcard0";
        String externalStorage = "/mnt/storage/sdcard1";
        createInternalStorage(internalStorage);
        createExternalStorage(externalStorage);
        // String[] paths = getVolumePaths();
        // mTempExternPath = findExternalStoragePath();
        // String internalPath = findInternalStoragePath();
        // if ((paths == null) || (internalPath == null)) {
        // String internalStorage = "/storage/sdcard0";
        // String externalStorage = "/storage/sdcard1";
        // createInternalStorage(internalStorage);
        // createExternalStorage(externalStorage);
        // Log.d(TAG, "No storage mounted, set Internal storage:" +
        // INTERNAL_STORAGE
        // + ";set External storage:" + EXTERNAL_STORAGE);
        // } else {
        // String prefix = internalPath.substring(0, internalPath.length() - 1);
        // for (String path : paths) {
        // if (path.equals(internalPath)) {
        // createInternalStorage(path);
        // }
        // if ((!path.equals(internalPath)) && (path.startsWith(prefix))) {
        // createExternalStorage(path);
        // }
        // }
        // }
    }

    private String findInternalStoragePath() {
        final String FILE_MOUNTS = "/proc/mounts";
        final String FUSE_DEVICE_PREFIX = "/dev/fuse";
        final String STORAGE_DEVICE_PREFIX = "/dev/block/vold/";
        final String FAKE_STORAGE_DEVICE_INDICATION = "secure/asec";

        int partitionNum = getInernalStoragePartitionNum();
        if (partitionNum < 0) {
            Log.e(TAG, "no partitions on internal storage");
            return null;
        }
        if (DEBUG) {
            Log.d(TAG, "" + partitionNum + " partitions on internal storage");
        }
        BufferedReader reader = null;
        try {
            File file = new File(FILE_MOUNTS);
            if (file == null || !file.exists() || !file.canRead()) {
                return null;
            }

            reader = new BufferedReader(new FileReader(file));
            String line = null;
            while ((line = reader.readLine()) != null) {
                if (line.contains(STORAGE_DEVICE_PREFIX)) {
                    String[] strs = line.split("\\s");
                    if (strs.length < 2 || strs[1].contains(FAKE_STORAGE_DEVICE_INDICATION)) {
                        if (DEBUG) {
                            Log.d(TAG, "fake storage" + strs[1] + "");
                        }
                        continue;
                    }
                    final String dev = strs[0];
                    if ((dev != null) && dev.contains(":")) {
                        String[] devPaths = dev.split(":");
                        if ((devPaths != null) && (devPaths.length >= 2)) {
                            String minorStr = devPaths[1];
                            int minorNum = -1;
                            if (minorStr.matches("[0-9]+")) {
                                minorNum = Integer.valueOf(minorStr);
                                if (minorNum < 0) {
                                    Log.w(TAG, "invalid device");
                                    continue;
                                }

                                if (minorNum <= partitionNum) {
                                    Log.d(TAG, "vold storage:" + strs[1] + " for internal storage");
                                    return strs[1];
                                }
                            }
                        }
                    }
                } else if (line.contains(FUSE_DEVICE_PREFIX)) {
                    String[] strs = line.split("\\s");
                    if (strs.length > 2 && (strs[1].trim().startsWith("/storage/sdcard"))) {
                        if (!strs[1].trim().equals(mTempExternPath)
                                && Util.isVolumeMounted(strs[1])) {
                            Log.d(TAG, "fuse storage:" + strs[1] + " for internal storage");
                            return strs[1];
                        }

                    }
                }
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    private String findExternalStoragePath() {
        final String FILE_MOUNTS = "/proc/mounts";
        final String STORAGE_DEVICE_PREFIX = "/dev/block/vold/";
        final String FAKE_STORAGE_DEVICE_INDICATION = "secure/asec";

        int partitionNum = getInernalStoragePartitionNum();
        if (partitionNum < 0) {
            Log.e(TAG, "no partitions on internal storage");
            return null;
        }
        if (DEBUG) {
            Log.d(TAG, "" + partitionNum + " partitions on external storage");
        }
        BufferedReader reader = null;
        try {
            File file = new File(FILE_MOUNTS);
            if (file == null || !file.exists() || !file.canRead()) {
                return null;
            }

            reader = new BufferedReader(new FileReader(file));
            String line = null;
            while ((line = reader.readLine()) != null) {
                if (line.contains(STORAGE_DEVICE_PREFIX)) {
                    String[] strs = line.split("\\s");
                    if (strs.length < 2 || strs[1].contains(FAKE_STORAGE_DEVICE_INDICATION)) {
                        if (DEBUG) {
                            Log.d(TAG, "fake storage" + strs[1] + "");
                        }
                        continue;
                    }
                    final String dev = strs[0];
                    if ((dev != null) && dev.contains(":")) {
                        String[] devPaths = dev.split(":");
                        if ((devPaths != null) && (devPaths.length >= 2)) {
                            String minorStr = devPaths[1];
                            int minorNum = -1;
                            if (minorStr.matches("[0-9]+")) {
                                minorNum = Integer.valueOf(minorStr);
                                if (minorNum < 0) {
                                    Log.w(TAG, "invalid device");
                                    continue;
                                }
                                if (minorNum > partitionNum) {
                                    String path = buildExternalPath(strs[1]);
                                    Log.d(TAG, "vold storage:" + path + " for external storage");
                                    return (path != null) ? path : null;
                                }
                            }
                        }
                    }
                }
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    public ArrayList<Storage> getStorageList() {
        return mStorages;
    }

    public String getInternalSdcardPath() {
        return INTERNAL_STORAGE;
    }

    public String getExternalSdcardPath() {
        return EXTERNAL_STORAGE;
    }

    private void createInternalStorage(String path) {
        INTERNAL_STORAGE = path;
        Storage storage = new Storage();
        storage.mountPoint = path;
        storage.isAllowMassStorage = false;
        storage.isPrimary = false;
        storage.isRemovable = false;
        storage.isEmulated = false;
        storage.descriptionId = R.string.storage_internal;
        mStorages.add(storage);
    }

    private void createExternalStorage(String path) {
        EXTERNAL_STORAGE = path;
        Storage storage = new Storage();
        storage.mountPoint = path;
        storage.isAllowMassStorage = true;
        storage.isPrimary = true;
        storage.isRemovable = true;
        storage.isEmulated = false;
        storage.descriptionId = R.string.storage_sd_card;
        mStorages.add(storage);
    }

    private String[] getVolumePaths() {
        StorageManager storageManager = (StorageManager) mContext
                .getSystemService(Context.STORAGE_SERVICE);
        try {
            Method method = StorageManager.class.getMethod("getVolumePaths", new Class[] {});
            method.setAccessible(true);
            Object volumePaths = method.invoke(storageManager, new Object[] {});
            if (volumePaths != null) {
                String[] paths = ((String[]) volumePaths);
                return paths;
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
        return null;
    }

    private String buildExternalPath(String path) {
        // android4.4 /mnt/media_rw/sdcard1/0 --> /storage/sdcard1/0
        if (!path.isEmpty()) {
            int start = path.lastIndexOf(File.separator);
            int len = path.length();
            if (start < len) {
                // childPath: /sdcard1
                String childPath = path.substring(start, len);
                // parentPath: /storage
                String parentPath = Environment.getExternalStorageDirectory().getParent();
                if (parentPath != null) {
                    return parentPath + childPath;
                } else {
                    return "/storage" + childPath;
                }
            }
        }
        return null;
    }
}
