
package com.stj.fileexplorer;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.List;

public abstract class BaseActivity extends Activity implements View.OnClickListener {

    private View mRootView;
    protected ViewStub mAboveViewStub;
    protected ViewStub mMiddleViewStub;
    protected ViewStub mBelowViewStub;
    private Button mInitLeftBtn;
    private Button mInitMidBtn;
    private Button mInitRightBtn;

    private Button mLeftBtn;
    private Button mMidBtn;
    private Button mRightBtn;
    private TextView mTopTitle;

    private View mInitTopTitle;
    private BottomKeyClickListener mBottomKeyClickListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRootView = LayoutInflater.from(this).inflate(R.layout.activity_base, null);
        setContentView(mRootView);

        mAboveViewStub = (ViewStub) findViewById(R.id.middle_list_above_viewstub);
        mMiddleViewStub = (ViewStub) findViewById(R.id.middle_list_middle_viewstub);
        mBelowViewStub = (ViewStub) findViewById(R.id.middle_list_below_viewstub);
        buildButtons();
    }

    protected void setActivityBgDrawable(Drawable drawable) {
        mRootView.setBackgroundDrawable(drawable);
    }

    protected void setActivityBgResource(int resid) {
        mRootView.setBackgroundResource(resid);
    }

    protected void setTopTitleDrawable(Drawable drawable) {
        if (mInitTopTitle != null) {
            mInitTopTitle.setBackgroundDrawable(drawable);
        }
    }

    protected void setTopTitleBgResource(int resid) {
        if (mInitTopTitle != null) {
            mInitTopTitle.setBackgroundResource(resid);
        }
    }

    protected void setBottomButtonsDrawable(Drawable drawable) {
        RelativeLayout layout = (RelativeLayout) mRootView.findViewById(R.id.bottom_layout);
        layout.setBackgroundDrawable(drawable);
    }

    protected void setBottomButtonsResource(int resid) {
        RelativeLayout layout = (RelativeLayout) mRootView.findViewById(R.id.bottom_layout);
        layout.setBackgroundResource(resid);
    }

    private void buildButtons() {
        mLeftBtn = (Button) findViewById(R.id.bottom_left_button);
        mMidBtn = (Button) findViewById(R.id.bottom_middle_button);
        mRightBtn = (Button) findViewById(R.id.bottom_right_button);

        mLeftBtn.setOnClickListener(this);
        mMidBtn.setOnClickListener(this);
        mRightBtn.setOnClickListener(this);

        mTopTitle = (TextView) findViewById(R.id.top_title);

        mInitLeftBtn = BuildLeftBtn(mLeftBtn);
        mInitMidBtn = BuildMiddleBtn(mMidBtn);
        mInitRightBtn = BuildRightBtn(mRightBtn);
        mInitTopTitle = BuildTopTitle(mTopTitle);

        if (mInitLeftBtn == null) {
            mLeftBtn.setVisibility(View.GONE);
        }

        if (mInitMidBtn == null) {
            mMidBtn.setVisibility(View.GONE);
        }
        if (mInitRightBtn == null) {
            mRightBtn.setVisibility(View.GONE);
        }

        if (mInitTopTitle == null) {
            mTopTitle.setVisibility(View.GONE);
        }
    }

    protected void setLeftBtnText(String text) {
        mLeftBtn.setText(text);
    }

    protected void setMidBtnText(String text) {
        mMidBtn.setText(text);
    }

    protected void setRightText(String text) {
        mRightBtn.setText(text);
    }

    protected void setTopTitleText(String text) {
        mTopTitle.setText(text);
    }

    @Override
    public void onClick(View v) {

        if (mBottomKeyClickListener == null) {
            return;
        }

        switch (v.getId()) {
            case R.id.bottom_left_button:
                mBottomKeyClickListener.onLeftKeyPress();
                break;
            case R.id.bottom_middle_button:
                mBottomKeyClickListener.onMiddleKeyPress();
                break;
            case R.id.bottom_right_button:
                mBottomKeyClickListener.onRightKeyPress();
                break;
            default:
                break;
        }
    }

    public interface BottomKeyClickListener {

        public void onLeftKeyPress();

        public void onMiddleKeyPress();

        public void onRightKeyPress();
    }

    public void setBottomKeyClickListener(BottomKeyClickListener l) {
        if (l != null) {
            mBottomKeyClickListener = l;
        }
    }

    protected class BaseMenuItemAdapter extends BaseAdapter {
        private List<String> menus;
        private LayoutInflater inflater;

        public BaseMenuItemAdapter(Context context, List<String> list) {
            this.menus = list;
            this.inflater = ((LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE));
        }

        @Override
        public int getCount() {
            return menus.size();
        }

        @Override
        public Object getItem(int position) {
            return Integer.valueOf(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            ViewHolder holder;
            if (view == null) {
                holder = new ViewHolder();
                view = inflater.inflate(R.layout.list_item_mms_main, null);
                holder.itemIndex = ((TextView) view.findViewById(R.id.mms_main_list_index));
                holder.itemText = (TextView) view.findViewById(R.id.mms_main_list_item_text);
                view.setTag(holder);
            } else {
                holder = (ViewHolder) view.getTag();
            }
            holder.itemIndex.setText(String.valueOf(position + 1));
            holder.itemText.setText(menus.get(position));
            return view;
        }

        private class ViewHolder {
            TextView itemIndex;
            TextView itemText;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_MENU:
                mBottomKeyClickListener.onLeftKeyPress();
                break;
            case KeyEvent.KEYCODE_BACK:
                mBottomKeyClickListener.onRightKeyPress();
                break;
            default:
                break;
        }
        return super.onKeyDown(keyCode, event);
    }
    
    public abstract Button BuildLeftBtn(Button v);

    public abstract Button BuildMiddleBtn(Button v);

    public abstract Button BuildRightBtn(Button v);

    public abstract TextView BuildTopTitle(TextView v);

}
