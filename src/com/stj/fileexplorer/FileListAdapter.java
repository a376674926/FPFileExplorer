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

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.stj.fileexplorer.FileViewInteractionHub.Mode;

import com.stj.fileexplorer.R;


import java.util.List;

public class FileListAdapter extends ArrayAdapter<FileInfo> {
    private LayoutInflater mInflater;

    private FileViewInteractionHub mFileViewInteractionHub;

    private FileIconHelper mFileIcon;

    private Context mContext;

    public FileListAdapter(Context context, int resource,
            List<FileInfo> objects, FileViewInteractionHub f,
            FileIconHelper fileIcon) {
        super(context, resource, objects);
        mInflater = LayoutInflater.from(context);
        mFileViewInteractionHub = f;
        mFileIcon = fileIcon;
        mContext = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = null;
        if (convertView != null) {
            view = convertView;
        } else {
            view = mInflater.inflate(R.layout.file_browser_item, parent, false);
        }
        FileInfo lFileInfo = mFileViewInteractionHub.getItem(position);
        FileListItem.setupFileListItemInfo(mContext, view, lFileInfo,
                mFileIcon, mFileViewInteractionHub);
        // Modify begin by lzy@20140422,for don't set checkbox listener at pick mode.
        if (mFileViewInteractionHub.getMode() != Mode.Pick) {
            view.findViewById(R.id.file_checkbox_area).setOnClickListener(
                    new FileListItem.FileItemOnClickListener(mContext,
                            mFileViewInteractionHub));
        }
        // Modify end.
        return view;
    }
}
