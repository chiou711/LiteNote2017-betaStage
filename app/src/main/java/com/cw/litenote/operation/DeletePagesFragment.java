package com.cw.litenote.operation;

import android.content.Context;
import android.content.Intent;
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
import android.database.Cursor;

import com.cw.litenote.R;
import com.cw.litenote.db.DB_folder;
import com.cw.litenote.folder.FolderUi;
import com.cw.litenote.main.MainAct;
import com.cw.litenote.util.BaseBackPressedListener;
import com.cw.litenote.util.ColorSet;
import com.cw.litenote.util.Util;
import com.cw.litenote.util.audio.AudioPlayer;
import com.cw.litenote.util.audio.UtilAudio;

import static com.cw.litenote.tabs.TabsHost.mDbFolder;

public class DeletePagesFragment extends Fragment{
	Context mContext;
	CheckedTextView mCheckTvSelAll;
	Button btnSelPageOK;
    ListView mListView;
	SelectPageList selPageList;
	public static View rootView;
    FragmentActivity act;

	public DeletePagesFragment(){}

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

        // checked Text View: select all
        mCheckTvSelAll = (CheckedTextView) rootView.findViewById(R.id.chkSelectAllPages);
        mCheckTvSelAll.setOnClickListener(new OnClickListener()
        {	@Override
            public void onClick(View checkSelAll)
            {
                boolean currentCheck = ((CheckedTextView)checkSelAll).isChecked();
                ((CheckedTextView)checkSelAll).setChecked(!currentCheck);

                if(((CheckedTextView)checkSelAll).isChecked())
                    selPageList.selectAllPages(true);
                else
                    selPageList.selectAllPages(false);
            }
        });

        // list view: selecting which pages to send
        mListView = (ListView)rootView.findViewById(R.id.listView1);

        // OK button: click to do next
        btnSelPageOK = (Button) rootView.findViewById(R.id.btnSelPageOK);
        btnSelPageOK.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(selPageList.mChkNum > 0)
                {
					mDbFolder.open();
                    for(int i=0;i<selPageList.COUNT;i++)
                    {
                        if (selPageList.mCheckedArr.get(i))
                        {
							int pageTableId = mDbFolder.getPageTableId(i, false);
							mDbFolder.dropPageTable(pageTableId,false);

							int pageId = mDbFolder.getPageId(i,false);

							// delete page row
							mDbFolder.deletePage(DB_folder.getFocusFolder_tableName(),pageId,false);
                        }
                    }
					mDbFolder.close();

                    mDbFolder.open();
                    // check if only one page left
                    int pgsCnt = mDbFolder.getPagesCount(false);
                    if(pgsCnt > 0)
                    {
                        int newFirstPageTblId=0;
                        int i=0;
                        Cursor mPageCursor = mDbFolder.getPageCursor();
                        while(i < pgsCnt)
                        {
                            mPageCursor.moveToPosition(i);
                            if(mPageCursor.isFirst())
                                newFirstPageTblId = mDbFolder.getPageTableId(i,false);
                            i++;
                        }
                        System.out.println("TabsHost / _postDeletePage / newFirstPageTblId = " + newFirstPageTblId);
                        Util.setPref_focusView_page_tableId(act, newFirstPageTblId);
                    }
                    else if(pgsCnt ==0)
                        Util.setPref_focusView_page_tableId(act, 0);

                    mDbFolder.close();

                    // set scroll X
                    int scrollX = 0; //over the last scroll X
                    Util.setPref_focusView_scrollX_byFolderTableId(act, scrollX );

                    if(AudioPlayer.mMediaPlayer != null)
                    {
                        UtilAudio.stopAudioPlayer();
                        AudioPlayer.mAudioPos = 0;
                        AudioPlayer.setPlayState(AudioPlayer.PLAYER_AT_STOP);
                    }

                    selPageList = new SelectPageList(act,rootView , mListView);
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
            if(FolderUi.getFolder_pagesCount(FolderUi.getFocus_folderPos()) == 0)
            {
                getActivity().finish();
                Intent intent  = new Intent(act,MainAct.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                getActivity().startActivity(intent);
            }
            else
                act.getSupportFragmentManager().popBackStack(); //TODO ??? exception
                // for pages count = 0 case
                // java.lang.IllegalArgumentException: No view found for id 0x1020011 (android:id/tabcontent) for fragment Page{8ac28af #0 id=0x1020011 tab1}
            }
        });

        //show list for selection
        selPageList = new SelectPageList(act,rootView , mListView);

		((MainAct)act).setOnBackPressedListener(new BaseBackPressedListener(act));

		return rootView;
	}


	@Override
	public void onPause() {
		super.onPause();
	}

}