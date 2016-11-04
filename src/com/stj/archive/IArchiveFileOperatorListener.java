
package com.stj.archive;

import android.content.Context;

import java.util.ArrayList;

public interface IArchiveFileOperatorListener {
    public void onArchiveFileLoadStart();

    public void onArchiveFileLoadComplete(ArrayList<ArchiveNode> nodes);

    public void notifyUpdateUI(ArrayList<ArchiveNode> nodes);

    public Context getContext();
}
