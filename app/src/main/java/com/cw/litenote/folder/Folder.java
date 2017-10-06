package com.cw.litenote.folder;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.AdapterView;

import com.cw.litenote.R;
import com.cw.litenote.db.DB_drawer;
import com.cw.litenote.main.MainAct;
import com.cw.litenote.page.PageUi;
import com.cw.litenote.util.Util;
import com.mobeta.android.dslv.DragSortController;
import com.mobeta.android.dslv.DragSortListView;
import com.mobeta.android.dslv.SimpleDragSortCursorAdapter;

import static com.cw.litenote.folder.FolderUi.addFolderListeners;

/**
 * Created by CW on 2016/8/23.
 */
public class Folder
{
    public DragSortListView listView;
    public SimpleDragSortCursorAdapter adapter;
    DragSortController controller;
    FragmentActivity act;

    public Folder(FragmentActivity act)
    {
        this.act = act;
        listView = (DragSortListView) act.findViewById(R.id.left_drawer);
    }

    // initialize folder list view
    public SimpleDragSortCursorAdapter initFolder()
    {
        // set Folder title
        if(MainAct.mDb_drawer.getFoldersCount(true) == 0)
        {
            // default: add 2 new folders
//            for(int i = 0; i< Define.ORIGIN_FOLDERS_COUNT; i++)
//            {
//                // insert folder
//                System.out.println("Folder/ _initFolder / insert folder "+ i) ;
//                String folderTitle = Define.getFolderTitle(act,i);
//                MainAct.mFolderTitles.add(folderTitle);
//                MainAct.mDb_drawer.insertFolder(i+1, folderTitle );
//            }
        }
        else
        {
            for(int i = 0; i< MainAct.mDb_drawer.getFoldersCount(true); i++)
            {
                MainAct.mFolderTitles.add(""); // init only
                MainAct.mFolderTitles.set(i, MainAct.mDb_drawer.getFolderTitle(i,true));
            }
        }

        // check DB
//        DB_drawer.listFolders();

        // set adapter
        MainAct.mDb_drawer.open();
        Cursor cursor = DB_drawer.mCursor_folder;

        String[] from = new String[] { DB_drawer.KEY_FOLDER_TITLE};
        int[] to = new int[] { R.id.folderText};

        adapter = new Folder_adapter(
                act,
                R.layout.folder_row,
                cursor,
                from,
                to,
                0
        );

        MainAct.mDb_drawer.close();

        listView.setAdapter(adapter);

        // set up click listener
        addFolderListeners();//??? move to resume?
        listView.setOnItemClickListener(FolderUi.folderClick);
        // set up long click listener
        listView.setOnItemLongClickListener(FolderUi.folderLongClick);

        controller = buildController(listView);
        listView.setFloatViewManager(controller);
        listView.setOnTouchListener(controller);

        // init folder dragger
        SharedPreferences pref = act.getSharedPreferences("show_note_attribute", 0);
        if(pref.getString("KEY_ENABLE_FOLDER_DRAGGABLE", "no")
                .equalsIgnoreCase("yes"))
            listView.setDragEnabled(true);
        else
            listView.setDragEnabled(false);

        listView.setDragListener(onDrag);
        listView.setDropListener(onDrop);

        return adapter;
    }

    // list view listener: on drag
    DragSortListView.DragListener onDrag = new DragSortListView.DragListener()
    {
        @Override
        public void drag(int startPosition, int endPosition) {
        }
    };

    // list view listener: on drop
    DragSortListView.DropListener onDrop = new DragSortListView.DropListener()
    {
        @Override
        public void drop(int startPosition, int endPosition) {
            //reorder data base storage
            int loop = Math.abs(startPosition-endPosition);
            for(int i=0;i< loop;i++)
            {
                FolderUi.swapFolderRows(startPosition,endPosition);
                if((startPosition-endPosition) >0)
                    endPosition++;
                else
                    endPosition--;
            }

            DB_drawer db_drawer = new DB_drawer(act);
            // update audio playing drawer index
            int drawerCount = db_drawer.getFoldersCount(true);
            for(int i=0;i<drawerCount;i++)
            {
                if(db_drawer.getFolderTableId(i,true) == MainAct.mPlaying_folderTableId)
                    MainAct.mPlaying_folderPos = i;
            }
            adapter.notifyDataSetChanged();
            FolderUi.updateFocus_folderPosition();
        }
    };



    /**
     * Called in onCreateView. Override this to provide a custom
     * DragSortController.
     */
    private static DragSortController buildController(DragSortListView dslv)
    {
        // defaults are
        DragSortController controller = new DragSortController(dslv);
        controller.setSortEnabled(true);
        controller.setDragInitMode(DragSortController.ON_DOWN); // click
        controller.setDragHandleId(R.id.folder_dragger);// handler
        controller.setBackgroundColor(Color.argb(128,128,64,0));// background color when dragging

        return controller;
    }

    /**
     * Listeners for folder ListView
     *
     */
    // click
    public static class FolderListener_click implements AdapterView.OnItemClickListener
    {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id)
        {
            System.out.println("Folder / _onItemClickListener / position = " + position);
            FolderUi.setFocus_folderPos(position);

            DB_drawer db_drawer = new DB_drawer(MainAct.mAct);
            Util.setPref_focusView_folder_tableId(MainAct.mAct,db_drawer.getFolderTableId(position,true) );

            FolderUi.selectFolder(position);
            MainAct.setFolderTitle(MainAct.mFolderTitle);
        }
    }

    // long click
    public static class FolderListener_longClick implements DragSortListView.OnItemLongClickListener
    {
        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id)
        {
            FolderUi.editFolder(position);
            return true;
        }
    }
}