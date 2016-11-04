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

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.ActionMode;
import android.view.KeyEvent;

import com.stj.fileexplorer.FileViewActivity.IBottomButtonPressListener;
import com.stj.fileexplorer.R;


import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

public class FileExplorerTabActivity extends Activity implements IBottomButtonPressListener {
    private static final String INSTANCESTATE_TAB = "tab";
    private static final int DEFAULT_OFFSCREEN_PAGES = 1;//2;
    ViewPager mViewPager;
    TabsAdapter mTabsAdapter;
    ActionMode mActionMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // add begin by xugaoming@20140317, don't need to restore
        // fragments automaticaly if killed by system for memory,
        // it will mess up the logic
        savedInstanceState = null;
        // add end

        super.onCreate(savedInstanceState);
        StorageHelper.getInstance(this);
        setContentView(R.layout.fragment_pager);
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setOffscreenPageLimit(DEFAULT_OFFSCREEN_PAGES);

        final ActionBar bar = getActionBar();
        bar.hide();
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        bar.setDisplayOptions(0, ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_SHOW_HOME);

        mTabsAdapter = new TabsAdapter(this, mViewPager);
//        mTabsAdapter.addTab(bar.newTab().setText(R.string.tab_category),
//                FileCategoryActivity.class, null);
        mTabsAdapter.addTab(bar.newTab().setText(R.string.tab_sd),
                FileViewActivity.class, null);

        FileViewActivity fileViewActivity = ((FileViewActivity) getFragment(Util.SDCARD_TAB_INDEX));
        fileViewActivity.registBottomBtnPressListener(this);

       /*mTabsAdapter.addTab(bar.newTab().setText(R.string.tab_remote),
                ServerControlActivity.class, null);*/
        int selectedItem = PreferenceManager.getDefaultSharedPreferences(this)
                .getInt(INSTANCESTATE_TAB, Util.CATEGORY_TAB_INDEX);
        if (Intent.ACTION_PICK.equals(getIntent().getAction())) {
            selectedItem = Util.SDCARD_TAB_INDEX;
        }
        //add begin by xugaoming@20140517, add function to locate
        //to a directory
        Intent intent = getIntent();
        if (intent != null && intent.getStringExtra("goto_dir") != null)
        {
            selectedItem = Util.SDCARD_TAB_INDEX;
        }
        //add end
        bar.setSelectedNavigationItem(0);
    }

    @Override
    protected void onPause() {
        super.onPause();
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putInt(INSTANCESTATE_TAB, getActionBar().getSelectedNavigationIndex());
        editor.commit();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if ((getActionBar().getSelectedNavigationIndex() == Util.CATEGORY_TAB_INDEX)
                || (getActionBar().getSelectedNavigationIndex() == Util.SDCARD_TAB_INDEX)) {
            //modify begin. by hhj@20160929
            /*FileCategoryActivity categoryFragement = (FileCategoryActivity) mTabsAdapter
            if (categoryFragement.isHomePage()) {
                reInstantiateCategoryTab();
            } else {
                categoryFragement.setConfigurationChanged(true);
            }
            */
          //modify end.
        }
        super.onConfigurationChanged(newConfig);
    }

    public void reInstantiateCategoryTab() {
        mTabsAdapter.destroyItem(mViewPager, Util.CATEGORY_TAB_INDEX,
                mTabsAdapter.getItem(Util.CATEGORY_TAB_INDEX));
        mTabsAdapter.instantiateItem(mViewPager, Util.CATEGORY_TAB_INDEX);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mActionMode != null && isUiFocusable(mActionMode)) {
            return true;
        }
        invalidateOptionsMenu();

        if (keyCode == KeyEvent.KEYCODE_MENU) {
            IMenuPressedListener menuPressedListener = (IMenuPressedListener) mTabsAdapter
                    .getItem(mViewPager.getCurrentItem());
            if (menuPressedListener.onMenuPressed()) {
                return true;
            }
        }else if(keyCode == KeyEvent.KEYCODE_BACK){
            onRightKeyPress();
        }

        return super.onKeyDown(keyCode, event);
    }

    private boolean isUiFocusable(ActionMode actionMode) {
        try {
            if (actionMode != null) {
                Class<?> c = Class.forName("android.view.ActionMode");
                Method method = c.getMethod("isUiFocusable", new Class[] {});
                method.setAccessible(true);
                Object isFocusable = method.invoke(actionMode, new Object[] {});
                if (isFocusable != null) {
                    return ((Boolean) isFocusable);
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        IBackPressedListener backPressedListener = (IBackPressedListener) mTabsAdapter
                .getItem(mViewPager.getCurrentItem());
        if (!backPressedListener.onBack()) {
            super.onBackPressed();
        }
    }

    public interface IBackPressedListener {
        /**
         * 处理back事件。
         * @return True: 表示已经处理; False: 没有处理，让基类处理。
         */
        boolean onBack();
    }

    public interface IMenuPressedListener {
        boolean onMenuPressed();
    }

    public void setActionMode(ActionMode actionMode) {
        mActionMode = actionMode;
    }

    public ActionMode getActionMode() {
        return mActionMode;
    }

    public Fragment getFragment(int tabIndex) {
        return mTabsAdapter.getItem(tabIndex);
    }

    /**
     * This is a helper class that implements the management of tabs and all
     * details of connecting a ViewPager with associated TabHost.  It relies on a
     * trick.  Normally a tab host has a simple API for supplying a View or
     * Intent that each tab will show.  This is not sufficient for switching
     * between pages.  So instead we make the content part of the tab host
     * 0dp high (it is not shown) and the TabsAdapter supplies its own dummy
     * view to show as the tab content.  It listens to changes in tabs, and takes
     * care of switch to the correct paged in the ViewPager whenever the selected
     * tab changes.
     */
    public static class TabsAdapter extends FragmentPagerAdapter
            implements ActionBar.TabListener, ViewPager.OnPageChangeListener {
        private final Context mContext;
        private final ActionBar mActionBar;
        private final ViewPager mViewPager;
        private final ArrayList<TabInfo> mTabs = new ArrayList<TabInfo>();

        static final class TabInfo {
            private final Class<?> clss;
            private final Bundle args;
            private Fragment fragment;

            TabInfo(Class<?> _class, Bundle _args) {
                clss = _class;
                args = _args;
            }
        }

        public TabsAdapter(Activity activity, ViewPager pager) {
            super(activity.getFragmentManager());
            mContext = activity;
            mActionBar = activity.getActionBar();
            mViewPager = pager;
            mViewPager.setAdapter(this);
            mViewPager.setOnPageChangeListener(this);
        }

        public void addTab(ActionBar.Tab tab, Class<?> clss, Bundle args) {
            TabInfo info = new TabInfo(clss, args);
            tab.setTag(info);
            tab.setTabListener(this);
            mTabs.add(info);
            mActionBar.addTab(tab);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mTabs.size();
        }

        @Override
        public Fragment getItem(int position) {
            TabInfo info = mTabs.get(position);
            if (info.fragment == null) {
                info.fragment = Fragment.instantiate(mContext, info.clss.getName(), info.args);
            }
            return info.fragment;
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
            mActionBar.setSelectedNavigationItem(position);
        }

        @Override
        public void onPageScrollStateChanged(int state) {
        }

        @Override
        public void onTabSelected(Tab tab, FragmentTransaction ft) {
            Object tag = tab.getTag();
            for (int i=0; i<mTabs.size(); i++) {
                if (mTabs.get(i) == tag) {
                    mViewPager.setCurrentItem(i);
                }
            }
            // Modify begin by lzy@20140607,finish tab_category and tab_sd action mode.
            // if(!tab.getText().equals(mContext.getString(R.string.tab_sd))) {
            ActionMode actionMode = ((FileExplorerTabActivity) mContext).getActionMode();
            if (actionMode != null) {
                actionMode.finish();
            }
            // }
            // Modify end.
        }

        @Override
        public void onTabUnselected(Tab tab, FragmentTransaction ft) {
        }

        @Override
        public void onTabReselected(Tab tab, FragmentTransaction ft) {
        }
    }

    @Override
    public void onLeftKeyPress() {
        FileViewActivity fileViewActivity = ((FileViewActivity) getFragment(Util.SDCARD_TAB_INDEX));
        fileViewActivity.onOptionMenuBtnClick();
    }

    @Override
    public void onMidKeyPress() {

    }

    @Override
    public void onRightKeyPress() {
        onBackPressed();
    }
    
}
