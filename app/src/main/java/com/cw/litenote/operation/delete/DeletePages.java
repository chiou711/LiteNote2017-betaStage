package com.cw.litenote.operation.delete;

import android.app.AlertDialog;
import android.content.DialogInterface;
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
import android.widget.TextView;
import android.widget.Toast;
import android.database.Cursor;

import com.cw.litenote.R;
import com.cw.litenote.db.DB_folder;
import com.cw.litenote.folder.FolderUi;
import com.cw.litenote.main.MainAct;
import com.cw.litenote.operation.List_selectPage;
import com.cw.litenote.operation.audio.AudioManager;
import com.cw.litenote.util.BaseBackPressedListener;
import com.cw.litenote.util.ColorSet;
import com.cw.litenote.util.Util;
import com.cw.litenote.util.preferences.Pref;

import static com.cw.litenote.tabs.TabsHost.mDbFolder;

public class DeletePages extends Fragment{
    TextView title;
	CheckedTextView mCheckTvSelAll;
	Button btnSelPageOK;
    ListView mListView;
	List_selectPage list_selPage;
	public static View rootView;
    FragmentActivity act;

	public DeletePages(){}

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    }

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		rootView = inflater.inflate(R.layout.select_page_list, container, false);
        act = getActivity();
		act.getActionBar().setBackgroundDrawable(new ColorDrawable(ColorSet.getBarColor(act)));

        // title
        title = (TextView) rootView.findViewById(R.id.select_list_title);
        title.setText(R.string.config_select_pages_delete_title);

        // checked Text View: select all
        mCheckTvSelAll = (CheckedTextView) rootView.findViewById(R.id.chkSelectAllPages);
        mCheckTvSelAll.setOnClickListener(new OnClickListener()
        {	@Override
            public void onClick(View checkSelAll)
            {
                boolean currentCheck = ((CheckedTextView)checkSelAll).isChecked();
                ((CheckedTextView)checkSelAll).setChecked(!currentCheck);

                if(((CheckedTextView)checkSelAll).isChecked())
                    list_selPage.selectAllPages(true);
                else
                    list_selPage.selectAllPages(false);
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
                if(list_selPage.mChkNum > 0)
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
                                    doDeletePages();
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
            if(FolderUi.getFolder_pagesCount(act,FolderUi.getFocus_folderPos()) == 0)
            {
                getActivity().finish();
                Intent intent  = new Intent(act,MainAct.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                getActivity().startActivity(intent);
            }
            else
                act.getSupportFragmentManager().popBackStack();
            }
        });

        //show list for selection
        list_selPage = new List_selectPage(act,rootView , mListView);

		((MainAct)act).setOnBackPressedListener(new BaseBackPressedListener(act));

		return rootView;
	}


	@Override
	public void onPause() {
		super.onPause();
	}


	void doDeletePages()
    {
        mDbFolder.open();
        for(int i = 0; i< list_selPage.count; i++)
        {
            if (list_selPage.mCheckedArr.get(i))
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
            Pref.setPref_focusView_page_tableId(act, newFirstPageTblId);
        }
        else if(pgsCnt ==0)
            Pref.setPref_focusView_page_tableId(act, 0);

        mDbFolder.close();

        // set scroll X
        int scrollX = 0; //over the last scroll X
        Pref.setPref_focusView_scrollX_byFolderTableId(act, scrollX );

        if(AudioManager.mMediaPlayer != null)
        {
            AudioManager.stopAudioPlayer();
            AudioManager.mAudioPos = 0;
            AudioManager.setPlayerState(AudioManager.PLAYER_AT_STOP);
        }

        list_selPage = new List_selectPage(act,rootView , mListView);
    }

}