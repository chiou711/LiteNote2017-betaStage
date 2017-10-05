package com.cw.litenote.operation;

import android.content.Context;
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
import android.widget.Toast;

import com.cw.litenote.R;
import com.cw.litenote.db.DB_drawer;
import com.cw.litenote.main.MainAct;
import com.cw.litenote.util.BaseBackPressedListener;
import com.cw.litenote.util.ColorSet;
import com.cw.litenote.util.Util;
import com.cw.litenote.util.audio.AudioPlayer;
import com.cw.litenote.util.audio.UtilAudio;


public class DeleteFoldersFragment extends Fragment{
	Context mContext;
	CheckedTextView mCheckTvSelAll;
	Button btnSelPageOK;
    ListView mListView;
	SelectFolderList selFolderList;
	View rootView;
    FragmentActivity act;
    DB_drawer mDbDrawer;

	public DeleteFoldersFragment(){}

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        mContext = act;
        mDbDrawer = new DB_drawer(act);
    }

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		rootView = inflater.inflate(R.layout.select_page_list, container, false);
        act = getActivity();
		act.getActionBar().setBackgroundDrawable(new ColorDrawable(ColorSet.getBarColor(act)));

        // checked Text View: select all
        mCheckTvSelAll = (CheckedTextView) rootView.findViewById(R.id.chkSelectAllPages);
        mCheckTvSelAll.setOnClickListener(new OnClickListener()
        {	@Override
            public void onClick(View checkSelAll)
            {
                boolean currentCheck = ((CheckedTextView)checkSelAll).isChecked();
                ((CheckedTextView)checkSelAll).setChecked(!currentCheck);

                if(((CheckedTextView)checkSelAll).isChecked())
                    selFolderList.selectAllPages(true);
                else
                    selFolderList.selectAllPages(false);
            }
        });

        // list view: selecting which pages to send
        mListView = (ListView)rootView.findViewById(R.id.listView1);

        // OK button: click to do next
        btnSelPageOK = (Button) rootView.findViewById(R.id.btnSelPageOK);
        btnSelPageOK.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(selFolderList.mChkNum > 0)
                {
					DB_drawer dbDrawer = new DB_drawer(act);

                    // drawer DB check
                    boolean doDB_check = false;
                    if(doDB_check) {
                        dbDrawer.open();
                        for (int i = 0; i < selFolderList.COUNT; i++) {
                            int folderTableId = dbDrawer.getFolderTableId(i, false);
                            System.out.println("DeleteFoldersFragment / _setOnClickListener / drawer DB check / folderTableId = " + folderTableId);

                            int folderId = (int) dbDrawer.getFolderId(i, false);
                            System.out.println("DeleteFoldersFragment / _setOnClickListener / drawer DB check / folderId = " + folderId);
                        }
                        dbDrawer.close();
                    }

                    dbDrawer.open();
                    for(int i = 0; i< selFolderList.COUNT; i++)
                    {
                        if(selFolderList.mCheckedArr.get(i))
                        {
							int folderTableId = dbDrawer.getFolderTableId(i,false);
                            dbDrawer.dropFolderTable(folderTableId,false);

							int folderId = (int)dbDrawer.getFolderId(i,false);
							// delete folder row
                            dbDrawer.deleteFolderId(folderId,false);

                            // change focus //TODO
                            MainAct.mFocus_folderPos=0;
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
                        Util.setPref_focusView_folder_tableId(act, newFirstFolderTblId);
                    }
                    else if(foldersCnt ==0)
                        Util.setPref_focusView_folder_tableId(act, 1);

                    dbDrawer.close();

                    // set scroll X //TODO　??? need this?
                    int scrollX = 0; //over the last scroll X
                    Util.setPref_focusView_scrollX_byFolderTableId(act, scrollX );

                    if(AudioPlayer.mMediaPlayer != null)
                    {
                        UtilAudio.stopAudioPlayer();
                        AudioPlayer.mAudioPos = 0;
                        AudioPlayer.setPlayState(AudioPlayer.PLAYER_AT_STOP);
                    }
                    selFolderList = new SelectFolderList(act,rootView , mListView);
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
            //TODO ??? need this?
            DB_drawer dbDrawer = new DB_drawer(act);
            int foldersCnt = dbDrawer.getFoldersCount(true);

            if(foldersCnt == 0)
            {
                getActivity().finish();
                Intent intent  = new Intent(act,MainAct.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                getActivity().startActivity(intent);
            }
            else {
                act.getSupportFragmentManager().popBackStack(); //TODO ??? exception
            }
                // for pages count = 0 case
                // java.lang.IllegalArgumentException: No view found for id 0x1020011 (android:id/tabcontent) for fragment Page{8ac28af #0 id=0x1020011 tab1}
            }
        });

        //show list for selection
        selFolderList = new SelectFolderList(act,rootView , mListView);

		((MainAct)act).setOnBackPressedListener(new BaseBackPressedListener(act));

		return rootView;
	}

	@Override
	public void onPause() {
		super.onPause();
	}

}