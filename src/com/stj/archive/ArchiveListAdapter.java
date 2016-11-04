
package com.stj.archive;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import com.stj.fileexplorer.FileIconHelper;
import com.stj.fileexplorer.Util;

import com.stj.fileexplorer.R;

import java.util.ArrayList;

public class ArchiveListAdapter extends ArrayAdapter<ArchiveNode> {

    private LayoutInflater mInflater;

    private ArrayList<ArchiveNode> mArchiveNodes = new ArrayList<ArchiveNode>();
    private FileIconHelper mFileIconHelper;

    public ArchiveListAdapter(Context context, int resource, ArrayList<ArchiveNode> data) {
        super(context, resource, data);

        mFileIconHelper = new FileIconHelper(context);
        mInflater = LayoutInflater.from(context);
        mArchiveNodes = data;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = null;
        if (convertView != null) {
            view = convertView;
        } else {
            view = mInflater.inflate(R.layout.archive_file_browser_item, parent, false);
        }

        ImageView lFileImage = (ImageView) view.findViewById(R.id.file_image);
        ImageView lFileImageFrame = (ImageView) view.findViewById(R.id.file_image_frame);

        ArchiveNode node = mArchiveNodes.get(position);

        if (node.isDirectory()) {
            mFileIconHelper.pauseLoadingIcon();
            lFileImageFrame.setVisibility(View.GONE);
            lFileImage.setImageResource(R.drawable.folder);
        } else {
            mFileIconHelper.startLoadingIcon();
            mFileIconHelper.setIcon(node, lFileImage, lFileImageFrame);
        }

        Util.setText(view, R.id.file_name, node.getName());
        return view;
    }

}
