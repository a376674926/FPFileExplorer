
package com.stj.fileexplorer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

import com.stj.fileexplorer.Util.MENU_OPERATIONS;

import java.util.Arrays;

public class FileOptionsMenuActivity extends BaseActivity implements
        BaseActivity.BottomKeyClickListener {

    private BaseMenuItemAdapter mAdapter;
    private int mSelectedPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAboveViewStub.setLayoutResource(R.layout.activity_base_listview);
        setBottomKeyClickListener(this);

        View listView = mAboveViewStub.inflate();
        TextView emptyTextView = (TextView) listView.findViewById(R.id.empty);
        emptyTextView.setVisibility(View.GONE);

        String[] menuItems = getResources().getStringArray(R.array.file_option_menu);
        mAdapter = new BaseMenuItemAdapter(this, Arrays.asList(menuItems));
        final ListView lv = (ListView) findViewById(R.id.base_list_view);
        lv.setAdapter(mAdapter);
        lv.setOnItemClickListener(new OptionMenuItemClickListener());
        lv.setOnItemSelectedListener(new OptionMenuItemSelectedListener());
    }

    @Override
    public Button BuildLeftBtn(Button v) {
        v.setText(R.string.select);
        return v;
    }

    @Override
    public Button BuildMiddleBtn(Button v) {
        return null;
    }

    @Override
    public Button BuildRightBtn(Button v) {
        v.setText(R.string.back);
        return v;
    }

    @Override
    public TextView BuildTopTitle(TextView v) {
        v.setText(R.string.option);
        return v;
    }

    @Override
    public void onLeftKeyPress() {
        onMenuItemClick(mSelectedPosition);
    }

    @Override
    public void onMiddleKeyPress() {

    }

    @Override
    public void onRightKeyPress() {
        finish();
    }

    private class OptionMenuItemClickListener implements OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            onMenuItemClick(position);
        }
    }

    private void onMenuItemClick(int position) {
        Intent intent = new Intent();

        if (position == MENU_OPERATIONS.COPY_OPTION.ordinal()) {
            intent.putExtra(Util.KEY_MENU_OPTIONS, MENU_OPERATIONS.COPY_OPTION.ordinal());
            setResult(Util.RESULT_OK, intent);

        } else if (position == MENU_OPERATIONS.DELETE_OPTION.ordinal()) {
            intent.putExtra(Util.KEY_MENU_OPTIONS, MENU_OPERATIONS.DELETE_OPTION.ordinal());
            setResult(Util.RESULT_OK, intent);

        } else if (position == MENU_OPERATIONS.DETAIL_OPTION.ordinal()) {
            intent.putExtra(Util.KEY_MENU_OPTIONS, MENU_OPERATIONS.DETAIL_OPTION.ordinal());
            setResult(Util.RESULT_OK, intent);

        } else if (position == MENU_OPERATIONS.MOVE_OPTION.ordinal()) {
            intent.putExtra(Util.KEY_MENU_OPTIONS, MENU_OPERATIONS.MOVE_OPTION.ordinal());
            setResult(Util.RESULT_OK, intent);

        } else if (position == MENU_OPERATIONS.RENAME_OPTION.ordinal()) {
            intent.putExtra(Util.KEY_MENU_OPTIONS, MENU_OPERATIONS.RENAME_OPTION.ordinal());
            setResult(Util.RESULT_OK, intent);

        } else if (position == MENU_OPERATIONS.SEND_OPTION.ordinal()) {
            intent.putExtra(Util.KEY_MENU_OPTIONS, MENU_OPERATIONS.SEND_OPTION.ordinal());
            setResult(Util.RESULT_OK, intent);

        } else if (position == MENU_OPERATIONS.CREATE_FOLDER_OPTION.ordinal()) {
            intent.putExtra(Util.KEY_MENU_OPTIONS, MENU_OPERATIONS.CREATE_FOLDER_OPTION.ordinal());
            setResult(Util.RESULT_OK, intent);
        }
        finish();
    }
    
    private class OptionMenuItemSelectedListener implements OnItemSelectedListener{

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            mSelectedPosition = position;
        }

        @Override
        public void onNothingSelected(AdapterView<?> arg0) {
            // TODO Auto-generated method stub
            
        }
        
    }

}
