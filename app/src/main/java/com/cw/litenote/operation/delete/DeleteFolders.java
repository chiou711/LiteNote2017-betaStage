package com.cw.litenote.operation.delete;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.cw.litenote.R;
import com.cw.litenote.db.DB_drawer;
import com.cw.litenote.db.DB_folder;
import com.cw.litenote.folder.FolderUi;
import com.cw.litenote.main.MainAct;
import com.cw.litenote.operation.List_selectFolder;
import com.cw.litenote.operation.audio.AudioManager;
import com.cw.litenote.util.BaseBackPressedListener;
import com.cw.litenote.util.ColorSet;
import com.cw.litenote.util.Util;
import com.cw.litenote.util.preferences.Pref;


public class DeleteFolders extends Fragment{
	Context mContext;
	TextView title;
	CheckedTextView mCheckTvSelAll;
	Button btnSelPageOK;
    ListView mListView;
	List_selectFolder list_selFolder;
	View rootView;
    FragmentActivity act;

	public DeleteFolders(){}

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        mContext = act;
    }

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		rootView = inflater.inflate(R.layout.select_page_list, container, false);
        act = getActivity();
		act.getActionBar().setBackgroundDrawable(new ColorDrawable(ColorSet.getBarColor(act)));

		// title
		title = (TextView) rootView.findViewById(R.id.select_list_title);
        title.setText(R.string.config_select_folders_title);

        // checked Text View: select all
        mCheckTvSelAll = (CheckedTextView) rootView.findViewById(R.id.chkSelectAllPages);
        mCheckTvSelAll.setOnClickListener(new OnClickListener()
        {	@Override
            public void onClick(View checkSelAll)
            {
                boolean currentCheck = ((CheckedTextView)checkSelAll).isChecked();
                ((CheckedTextView)checkSelAll).setChecked(!currentCheck);

                if(((CheckedTextView)checkSelAll).isChecked())
                    list_selFolder.selectAllPages(true);
                else
                    list_selFolder.selectAllPages(false);
            }
        });

        // list view: selecting which pages to send
        mListView = (ListView)rootView.findViewById(R.id.listView1);

        // OK button: click to do next
        btnSelPageOK = (Button) rootView.findViewById(R.id.btnSelPageOK);
        btnSelPageOK.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_delete, 0, 0, 0);
        btnSelPageOK.setText(R.string.config_delete_DB_btn);
        btnSelPageOK.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(list_selFolder.mChkNum > 0)
                {
                    Util util = new Util(act);
                    util.vibrate();

                    AlertDialog.Builder builder1 = new AlertDialog.Builder(act);
                    builder1.setTitle(R.string.confirm_dialog_title)
                            .setMessage(R.string.confirm_dialog_message_selection)
                            .setNegativeButton(R.string.confirm_dialog_button_no, new DialogInterface.OnClickListener()
                            {   @Override
                            public void onClick(DialogInterface dialog1, int which1)
                            {
                                    /*nothing to do*/
                            }
                            })
                            .setPositiveButton(R.string.confirm_dialog_button_yes, new DialogInterface.OnClickListener()
                            {   @Override
                            public void onClick(DialogInterface dialog1, int which1)
                            {
                                doDeleteFolders();
                            }
                            })
                            .show();//warning:end
                }
                else
                    Toast.makeText(act,
                            R.string.delete_checked_no_checked_items,
                            Toast.LENGTH_SHORT).show();
            }
        });

        // cancel button
        Button btnSelPageCancel = (Button) rootView.findViewById(R.id.btnSelPageCancel);
		btnSelPageCancel.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_close_clear_cancel, 0, 0, 0);

        btnSelPageCancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                System.out.println("DeleteFolders / _btnSelPageCancel");
                DB_drawer db_drawer = new DB_drawer(act);

                int focusFolder_tableId = Pref.getPref_focusView_folder_tableId(act);
                DB_folder db_folder = new DB_folder(act,focusFolder_tableId);
                if((db_drawer.getFoldersCount(true) == 0) || (db_folder.getPagesCount(true) == 0))
                {
                    System.out.println("DeleteFolders / _btnSelPageCancel / will call MainAct");
                    getActivity().finish();
                    Intent intent  = new Intent(act,MainAct.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    getActivity().startActivity(intent);
                }
                else {
                    System.out.println("DeleteFolders / _btnSelPageCancel / will do _popBackStack");
                    act.getSupportFragmentManager().popBackStack();
                }
                // for pages count = 0 case
                // java.lang.IllegalArgumentException: No view found for id 0x1020011 (android:id/tabcontent) for fragment Page{8ac28af #0 id=0x1020011 tab1}
            }
        });

        //show list for selection
        list_selFolder = new List_selectFolder(act,rootView , mListView);

		((MainAct)act).setOnBackPressedListener(new BaseBackPressedListener(act));

		return rootView;
	}

	@Override
	public void onPause() {
		super.onPause();
	}

    /**
     * Do delete folders
     */
	void doDeleteFolders()
    {
        DB_drawer dbDrawer = new DB_drawer(act);

        // drawer DB check
        boolean doDB_check = false;
        if(doDB_check) {
            dbDrawer.open();
            for (int i = 0; i < list_selFolder.count; i++) {
                int folderTableId = dbDrawer.getFolderTableId(i, false);
                System.out.println("DeleteFolders / _setOnClickListener / drawer DB check / folderTableId = " + folderTableId);

                int folderId = (int) dbDrawer.getFolderId(i, false);
                System.out.println("DeleteFolders / _setOnClickListener / drawer DB check / folderId = " + folderId);
            }
            dbDrawer.close();
        }

        dbDrawer.open();
        for(int i = 0; i< list_selFolder.count; i++)
        {
            if(list_selFolder.mCheckedArr.get(i))
            {
                int folderTableId = dbDrawer.getFolderTableId(i,false);
                dbDrawer.dropFolderTable(folderTableId,false);

                int folderId = (int)dbDrawer.getFolderId(i,false);
                // delete folder row
                dbDrawer.deleteFolderId(folderId,false);

                // change focus
                FolderUi.setFocus_folderPos(0);
            }
        }

        // check if only one folder left
        int foldersCnt = dbDrawer.getFoldersCount(false);

        if(foldersCnt > 0)
        {
            int newFirstFolderTblId=0;
            int i=0;
            dbDrawer.open();
            Cursor folderCursor = dbDrawer.getFolderCursor();
            while(i < foldersCnt)
            {
                folderCursor.moveToPosition(i);
                if(folderCursor.isFirst())
                    newFirstFolderTblId = dbDrawer.getFolderTableId(i,false);
                i++;
            }
            Pref.setPref_focusView_folder_tableId(act, newFirstFolderTblId);
        }
        else if(foldersCnt ==0)
            Pref.setPref_focusView_folder_tableId(act, 1);

        dbDrawer.close();

        // set scroll X
        int scrollX = 0; //over the last scroll X
        Pref.setPref_focusView_scrollX_byFolderTableId(act, scrollX );

        if(AudioManager.mMediaPlayer != null)
        {
            AudioManager.stopAudioPlayer();
            AudioManager.mAudioPos = 0;
            AudioManager.setPlayerState(AudioManager.PLAYER_AT_STOP);
        }

        list_selFolder = new List_selectFolder(act,rootView , mListView);
    }

}