package com.cw.litenote.drawer;

import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.View;

import com.cw.litenote.R;
import com.cw.litenote.folder.FolderUi;
import com.cw.litenote.main.MainAct;
import com.cw.litenote.tabs.TabsHost;

/**
 * Created by CW on 2016/8/24.
 */
public class Drawer {


    public DrawerLayout drawerLayout;
    private FragmentActivity act;
    public ActionBarDrawerToggle drawerToggle;

    public Drawer(FragmentActivity activity)
    {
        drawerLayout = (DrawerLayout) activity.findViewById(R.id.drawer_layout);
        act = activity;

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the sliding drawer and the action bar app icon
        drawerToggle =new ActionBarDrawerToggle(act,                  /* host Activity */
                                                drawerLayout,         /* DrawerLayout object */
                                                R.drawable.ic_drawer,  /* navigation drawer image to replace 'Up' caret */
                                                R.string.drawer_open,  /* "open drawer" description for accessibility */
                                                R.string.drawer_close  /* "close drawer" description for accessibility */
                                                )
                {
                    public void onDrawerOpened(View drawerView)
                    {
                        System.out.println("Drawer / _onDrawerOpened ");

                        MainAct.mAct.findViewById(R.id.content_frame).setVisibility(View.INVISIBLE);
                        act.invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()

                        if(MainAct.mFolder.listView.getCount() >0) {
                            MainAct.setFolderTitle(MainAct.mAppTitle);
                        }
                    }

                    public void onDrawerClosed(View view)
                    {
                        System.out.println("Drawer / _onDrawerClosed / MainAct.mFocus_folderPos = " + MainAct.mFocus_folderPos);

                        MainAct.mAct.findViewById(R.id.content_frame).setVisibility(View.VISIBLE);

                        if( !MainAct.bEnableConfig ) {
                            act.invalidateOptionsMenu(); // creates a call to onPrepareOptionsMenu()

//                            if(MainAct.mFolder.listView.getCount() >0)
                            if (MainAct.mDb_drawer.getFoldersCount(true) > 0)
                            {
                                int pos = MainAct.mFolder.listView.getCheckedItemPosition();
                                MainAct.mFolderTitle = MainAct.mDb_drawer.getFolderTitle(pos,true);
                                MainAct.setFolderTitle(MainAct.mFolderTitle);

                                // add for deleting folder condition
                                if (TabsHost.mTabsHost == null)
                                    FolderUi.selectFolder(MainAct.mFocus_folderPos);
                            } else
                                MainAct.mAct.findViewById(R.id.content_frame).setVisibility(View.INVISIBLE);
                        }
                    }
               };
    }

    public void initDrawer()
    {
        // set a custom shadow that overlays the main content when the drawer opens
        drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        drawerLayout.setDrawerListener(drawerToggle);
    }

    public void closeDrawer()
    {
        drawerLayout.closeDrawer(MainAct.mFolder.listView);
    }


    public boolean isDrawerOpen()
    {
        return drawerLayout.isDrawerOpen(MainAct.mFolder.listView);
    }

    public static int getFocusFolderPosition()
    {
        return MainAct.mFocus_folderPos;
    }
}
