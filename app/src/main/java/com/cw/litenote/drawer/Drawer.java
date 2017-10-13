package com.cw.litenote.drawer;

import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.View;

import com.cw.litenote.R;
import com.cw.litenote.db.DB_drawer;
import com.cw.litenote.folder.FolderUi;
import com.cw.litenote.main.MainAct;
import com.cw.litenote.tabs.TabsHost;
import com.mobeta.android.dslv.DragSortListView;

/**
 * Created by CW on 2016/8/24.
 */
public class Drawer {


    public DrawerLayout drawerLayout;
    private FragmentActivity act;
    public ActionBarDrawerToggle drawerToggle;
    DragSortListView listView;

    public Drawer(FragmentActivity activity)
    {
        drawerLayout = (DrawerLayout) activity.findViewById(R.id.drawer_layout);
        act = activity;
        listView = (DragSortListView) act.findViewById(R.id.left_drawer);
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

                        act.findViewById(R.id.content_frame).setVisibility(View.INVISIBLE);
                        act.invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()

                        if(listView.getCount() >0) {
                            act.getActionBar().setTitle(MainAct.mAppTitle);
                        }
                    }

                    public void onDrawerClosed(View view)
                    {
                        System.out.println("Drawer / _onDrawerClosed / FolderUi.getFocus_folderPos() = " + FolderUi.getFocus_folderPos());

                        act.findViewById(R.id.content_frame).setVisibility(View.VISIBLE);

                        FragmentManager fragmentManager = act.getSupportFragmentManager();
                        if(fragmentManager.getBackStackEntryCount() ==0 )
                        {
                            act.invalidateOptionsMenu(); // creates a call to onPrepareOptionsMenu()

                            DB_drawer dB_drawer = new DB_drawer(act);
                            if (dB_drawer.getFoldersCount(true) > 0)
                            {
                                int pos = listView.getCheckedItemPosition();
                                MainAct.mFolderTitle = dB_drawer.getFolderTitle(pos,true);
                                act.getActionBar().setTitle(MainAct.mFolderTitle);

                                // add for deleting folder condition
                                if (TabsHost.mTabsHost == null)
                                    FolderUi.selectFolder(act,FolderUi.getFocus_folderPos());
                            }
                            else
                                act.findViewById(R.id.content_frame).setVisibility(View.INVISIBLE);
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
        drawerLayout.closeDrawer(listView);
    }


    public boolean isDrawerOpen()
    {
        return drawerLayout.isDrawerOpen(listView);
    }
}
