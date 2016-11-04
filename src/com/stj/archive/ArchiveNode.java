
package com.stj.archive;

import com.stj.fileexplorer.FileInfo;

import java.io.File;


public class ArchiveNode extends FileInfo {
    private String parent;

    public boolean isDirectory() {
        return IsDir;
    }

    public void setDirectory(boolean isDirectory) {
        this.IsDir = isDirectory;
    }

    public String getName() {
        return fileName;
    }

    public void setName(String name) {
        this.fileName = name;
    }

    public String getParent() {
        return new File(filePath).getParent();
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public String getPath() {
        return filePath;
    }

    public void setPath(String path) {
        this.filePath = path;
    }

    public long getSize() {
        return fileSize;
    }

    public void setSize(long size) {
        this.fileSize = size;
    }
}
