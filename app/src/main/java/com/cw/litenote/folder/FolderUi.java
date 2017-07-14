package com.cw.litenote.folder;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.cw.litenote.R;
import com.cw.litenote.operation.Import_selectedFileAct;
import com.cw.litenote.db.DB_drawer;
import com.cw.litenote.db.DB_folder;
import com.cw.litenote.main.MainAct;
import com.cw.litenote.page.TabsHost;
import com.cw.litenote.preference.Define;
import com.cw.litenote.util.Util;
import com.cw.litenote.util.audio.AudioPlayer;
import com.cw.litenote.util.audio.UtilAudio;

import java.lang.reflect.Field;


public class FolderUi
{
	FolderUi(){}


	public static Folder.FolderListener_click folderClick;
	public static Folder.FolderListener_longClick folderLongClick;


	public static void addFolderListeners()
	{
		folderClick = new Folder.FolderListener_click();
		folderLongClick = new Folder.FolderListener_longClick();
	}


	/**
     * Add new folder
     *
     */
    static private int mAddFolderAt;
    static private SharedPreferences mPref_add_new_folder_location;
    public static void addNewFolder(final FragmentActivity act, final int newTableId)
    {
        // get folder name
        String folderName = act.getResources()
                               .getString(R.string.default_folder_name)
                               .concat(String.valueOf(newTableId));

        // get layout inflater
        View rootView = act.getLayoutInflater().inflate(R.layout.add_new_folder, null);
        final EditText editFolderName = (EditText) rootView.findViewById(R.id.new_folder_name);

        // set cursor
        try {
            Field f = TextView.class.getDeclaredField("mCursorDrawableRes");
            f.setAccessible(true);
            f.set(editFolderName, R.drawable.cursor);
        } catch (Exception ignored) {
        }

        // set hint
        editFolderName.setHint(folderName);
        editFolderName.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    ((EditText) v).setText("");
                    ((EditText) v).setSelection(0);
                }
            }
        });

        // radio buttons
        final RadioGroup mRadioGroup = (RadioGroup) rootView.findViewById(R.id.radioGroup_new_folder_at);

        // get new folder location option
        mPref_add_new_folder_location = act.getSharedPreferences("add_new_folder_option", 0);
        if (mPref_add_new_folder_location.getString("KEY_ADD_NEW_FOLDER_TO", "bottom").equalsIgnoreCase("top")) {
            mRadioGroup.check(mRadioGroup.getChildAt(0).getId());
            mAddFolderAt = 0;
        } else if (mPref_add_new_folder_location.getString("KEY_ADD_NEW_FOLDER_TO", "bottom").equalsIgnoreCase("bottom")) {
            mRadioGroup.check(mRadioGroup.getChildAt(1).getId());
            mAddFolderAt = 1;
        }

        // update new folder location option
        mRadioGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup RG, int id) {
                mAddFolderAt = mRadioGroup.indexOfChild(mRadioGroup.findViewById(id));
                if (mAddFolderAt == 0) {
                    mPref_add_new_folder_location.edit().putString("KEY_ADD_NEW_FOLDER_TO", "top").apply();
                } else if (mAddFolderAt == 1) {
                    mPref_add_new_folder_location.edit().putString("KEY_ADD_NEW_FOLDER_TO", "bottom").apply();
                }
            }
        });

        // set view to dialog
        Builder builder1 = new Builder(act);
        builder1.setView(rootView);
        final AlertDialog dialog1 = builder1.create();
        dialog1.show();

        // cancel button
        Button btnCancel = (Button) rootView.findViewById(R.id.new_folder_cancel);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog1.dismiss();
            }
        });

        // add button
        Button btnAdd = (Button) rootView.findViewById(R.id.new_folder_add);
        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
				DB_drawer db_drawer = new DB_drawer(act);
				DB_folder db_folder = new DB_folder(act,newTableId);

				String folderTitle;
                if (!Util.isEmptyString(editFolderName.getText().toString()))
                    folderTitle = editFolderName.getText().toString();
                else
                    folderTitle = act.getResources().getString(R.string.default_folder_name)
                                                    .concat(String.valueOf(newTableId));

                MainAct.mFolderTitles.add(folderTitle);
                // insert new drawer Id and Title
                db_drawer.insertFolder(newTableId, folderTitle );

                // insert folder table
                db_drawer.insertFolderTable(db_drawer,newTableId, false);

                // insert page table
                for(int i = 1; i<= Define.ORIGIN_PAGES_COUNT; i++)
                {
                    db_folder.insertPageTable(db_folder,newTableId, i, false);
                }

                // add new folder to the top
                if(mAddFolderAt == 0)
                {
                    int startCursor = db_drawer.getFoldersCount()-1;
                    int endCursor = 0;

                    //reorder data base storage for ADD_NEW_TO_TOP option
                    int loop = Math.abs(startCursor-endCursor);
                    for(int i=0;i< loop;i++)
                    {
                        swapFolderRows(startCursor,endCursor);
                        if((startCursor-endCursor) >0)
                            endCursor++;
                        else
                            endCursor--;
                    }

                    // focus position is 0 for Add to top
                    MainAct.mFocus_folderPos = 0;
                    Util.setPref_lastTimeView_folder_tableId(act,db_drawer.getFolderTableId(MainAct.mFocus_folderPos) );

                    // update playing highlight if needed
                    if(AudioPlayer.mMediaPlayer != null)
                        MainAct.mPlaying_folderPos++;
                }

                MainAct.folderAdapter.notifyDataSetChanged();

                //end
                dialog1.dismiss();

                updateFocus_folderPosition();
            }
        });
    }


	/**
	 * delete selected folder
	 * 
	 */
	private static int mFirstExist_folderId = 0;
	public static int mLastExist_folderTableId;
	private static void deleteFolder(int position, final Activity act)
	{

		System.out.println("MainUi / _deleteFolder");
		// Before delete: renew first FolderId and last FolderId
		renewFirstAndLast_folderId();
		
		// keep one folder at least
        DB_drawer db_drawer = new DB_drawer(act);
		int foldersCount = db_drawer.getFoldersCount();
		if(foldersCount == 1)
		{
			 // show toast for only one folder
             Toast.makeText(act, R.string.toast_keep_one_drawer , Toast.LENGTH_SHORT).show();
             return;
		}

		// get folder table Id
		int folderTableId = db_drawer.getFolderTableId(position);

		// get folder Id
		int folderId =  (int) db_drawer.getFolderId(position);

		// 1) delete related page table
        DB_folder dbFolder = new DB_folder(TabsHost.mAct, folderTableId);
		for(int i = 0; i< dbFolder.getPagesCount(true); i++)
		{
			int pageTableId = dbFolder.getPageTableId(i, true);
			dbFolder.dropPageTable(folderTableId, pageTableId);
		}

		// 2) delete folder table Id
		db_drawer.dropFolderTable(folderTableId);
		
		// 3) delete folder Id in drawer table
        db_drawer.deleteFolderId(folderId);

		renewFirstAndLast_folderId();

		// After Delete
        // - update mFocus_folderPos
        // - select first existing drawer item
		foldersCount = db_drawer.getFoldersCount();

		// get new focus position
		// if focus item is deleted, set focus to new first existing folder
        if(MainAct.mFocus_folderPos == position)
        {
	        for(int item = 0; item < foldersCount; item++)
	        {
	        	if(	db_drawer.getFolderId(item)== mFirstExist_folderId)
	        		MainAct.mFocus_folderPos = item;
	        }
        }
        else if(position < MainAct.mFocus_folderPos)
        	MainAct.mFocus_folderPos--;

//		System.out.println("MainUi / MainAct.mFocus_folderPos = " + MainAct.mFocus_folderPos);

        // set new focus position
        MainAct.mFolder.listView.setItemChecked(MainAct.mFocus_folderPos, true);

		int focusFolderTableId =  db_drawer.getFolderTableId(MainAct.mFocus_folderPos);
		// update folder table Id of last time view
        Util.setPref_lastTimeView_folder_tableId(act, focusFolderTableId );
		// update folder table Id of new focus (error will cause first folder been deleted)
		DB_folder.setFocusFolder_tableId(focusFolderTableId);

        // update audio playing highlight if needed
        if(AudioPlayer.mMediaPlayer != null)
        {
           if( MainAct.mPlaying_folderPos > position)
        	   MainAct.mPlaying_folderPos--;
           else if(MainAct.mPlaying_folderPos == position)
           {
			   // stop audio since the folder is deleted
        	   UtilAudio.stopAudioPlayer();
			   // update
        	   selectFolder(MainAct.mFocus_folderPos); // select folder to clear old playing view
			   MainAct.setFolderTitle(MainAct.mFolderTitle);
           }
        }

        // refresh drawer list view
		MainAct.folderAdapter.notifyDataSetChanged();

        // clear folder
        if(TabsHost.mTabsHost != null)
        	TabsHost.mTabsHost.clearAllTabs();
        TabsHost.mTabsHost = null;

        // remove last time view Key
        Util.removePref_lastTimeView_key(act,folderTableId);
	}


	// Renew first and last folder Id
	private static Cursor mFolderCursor;
	public static void renewFirstAndLast_folderId()
	{
        Activity act = MainAct.mAct;
		DB_drawer db_drawer = new DB_drawer(act);
		int i = 0;
		int foldersCount = db_drawer.getFoldersCount();
		mLastExist_folderTableId = 0;
		while(i < foldersCount)
    	{
			boolean isFirst;
			db_drawer.open();
			mFolderCursor = DB_drawer.mCursor_folder;
			mFolderCursor.moveToPosition(i);
			isFirst = mFolderCursor.isFirst();
			db_drawer.close();

			if(isFirst)
				mFirstExist_folderId = (int) db_drawer.getFolderId(i) ;
			
			if(db_drawer.getFolderTableId(i) >= mLastExist_folderTableId)
				mLastExist_folderTableId = db_drawer.getFolderTableId(i);
			
			i++;
    	} 
	}

    private static SharedPreferences mPref_delete_warn;
	public static void editFolder(final int position)
	{
        final Activity act = MainAct.mAct;
		DB_drawer db = new DB_drawer(act);

		// insert when table is empty, activated only for the first time 
		final String folderTitle = db.getFolderTitle(position);
	
		final EditText editText = new EditText(act);
	    editText.setText(folderTitle);
	    editText.setSelection(folderTitle.length()); // set edit text start position
	    //update tab info
	    Builder builder = new Builder(act);
	    builder.setTitle(R.string.edit_folder_title)
	    	.setMessage(R.string.edit_folder_message)
	    	.setView(editText)   
	    	.setNegativeButton(R.string.btn_Cancel, new OnClickListener()
	    	{   @Override
	    		public void onClick(DialogInterface dialog, int which)
	    		{/*cancel*/}
	    	})
	        .setNeutralButton(R.string.edit_page_button_delete, new OnClickListener()
	        {   @Override
	            public void onClick(DialogInterface dialog, int which)
	        	{
	        		// delete
					//warning:start
	            	mPref_delete_warn = act.getSharedPreferences("delete_warn", 0);
	            	if(mPref_delete_warn.getString("KEY_DELETE_WARN_MAIN","enable").equalsIgnoreCase("enable") &&
	                   mPref_delete_warn.getString("KEY_DELETE_FOLDER_WARN","yes").equalsIgnoreCase("yes"))
	            	{
	        			Util util = new Util(act);
	    				util.vibrate();
	    				
	            		Builder builder1 = new Builder(act); 
	            		builder1.setTitle(R.string.confirm_dialog_title)
	                    .setMessage(R.string.confirm_dialog_message_drawer)
	                    .setNegativeButton(R.string.confirm_dialog_button_no, new OnClickListener(){
	                    	@Override
	                        public void onClick(DialogInterface dialog1, int which1){
	                    		/*nothing to do*/}})
	                    .setPositiveButton(R.string.confirm_dialog_button_yes, new OnClickListener(){
	                    	@Override
	                        public void onClick(DialogInterface dialog1, int which1){
	                    		deleteFolder(position, act);
	                    	}})
	                    .show();
	            	} //warning:end
	            	else
	            	{
	            		deleteFolder(position, act);
	            	}
	            	
	            }
	        })		    	
	    	.setPositiveButton(R.string.edit_page_button_update, new OnClickListener()
	    	{   @Override
	    		public void onClick(DialogInterface dialog, int which)
	    		{
	    			DB_drawer db_drawer = new DB_drawer(act);
	    			// save
	    			int drawerId =  (int) db_drawer.getFolderId(position);
	    			int drawerTabInfoTableId =  db_drawer.getFolderTableId(position);
					db_drawer.updateFolder(drawerId,
									drawerTabInfoTableId,
									editText.getText().toString());
					// update
					MainAct.folderAdapter.notifyDataSetChanged();
	                MainAct.setFolderTitle(editText.getText().toString());
	            }
	        })	
	        .setIcon(android.R.drawable.ic_menu_edit);
	        
	    AlertDialog d1 = builder.create();
	    d1.show();
	    // android.R.id.button1 for positive: save
	    ((Button)d1.findViewById(android.R.id.button1))
	    .setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_save, 0, 0, 0);
	    
	    // android.R.id.button2 for negative: cancel 
	    ((Button)d1.findViewById(android.R.id.button2))
	    .setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_close_clear_cancel, 0, 0, 0);
	    
	    // android.R.id.button3 for neutral: delete
	    ((Button)d1.findViewById(android.R.id.button3))
	    .setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_delete, 0, 0, 0);
	}

	// swap rows
	private static Long mFolderId1 = (long) 1;
	private static Long mFolderId2 = (long) 1;
	private static int mFolderTableId1;
	private static int mFolderTableId2;
	private static String mFolderTitle1;
	private static String mFolderTitle2;
	public static void swapFolderRows(int startPosition, int endPosition)
	{
        Activity act = MainAct.mAct;
		DB_drawer db_drawer = new DB_drawer(act);

		mFolderId1 = db_drawer.getFolderId(startPosition);
		mFolderTableId1 = db_drawer.getFolderTableId(startPosition);
		mFolderTitle1 = db_drawer.getFolderTitle(startPosition);

		mFolderId2 = db_drawer.getFolderId(endPosition);
		mFolderTableId2 = db_drawer.getFolderTableId(endPosition);
		mFolderTitle2 = db_drawer.getFolderTitle(endPosition);

	    db_drawer.updateFolder(mFolderId1,
				mFolderTableId2,
				mFolderTitle2);

		db_drawer.updateFolder(mFolderId2,
				mFolderTableId1,
				mFolderTitle1);
	}

    // Update focus position
    public static void updateFocus_folderPosition()
    {
    	Activity act = MainAct.mAct;
        DB_drawer db_drawer = new DB_drawer(act);

		//update focus position
		int iLastView_folderTableId = Util.getPref_lastTimeView_folder_tableId(act);
		int count = db_drawer.getFoldersCount();
    	for(int i=0;i<count;i++)
    	{
        	if(	db_drawer.getFolderTableId(i)== iLastView_folderTableId)
        	{
        		MainAct.mFocus_folderPos =  i;
				MainAct.mFolder.listView.setItemChecked(MainAct.mFocus_folderPos, true);
        	}
    	}
    	
    }	
    
    // select folder
    public static void selectFolder(final int position)
    {
    	System.out.println("MainUi / _selectFolder / position = " + position);
    	MainAct.mFolderTitle = MainAct.mDb_drawer.getFolderTitle(position);

		// update selected item and title, then close the drawer
		MainAct.mFolder.listView.setItemChecked(position, true);

        // will call Drawer / _onDrawerClosed
		MainAct.mDrawer.drawerLayout.closeDrawer(MainAct.mFolder.listView);
        
		if(Define.HAS_PREFERENCE)
		{
			// Create default tables
			if( (position < Define.ORIGIN_FOLDERS_COUNT) &&
				!Util.getPref_has_default_import(MainAct.mAct,position) )
			{
				String fileName = "default"+ (position+1) + ".xml";
				Activity act = MainAct.mAct;

				// set focus folder table Id
				int folderTableId = Util.getPref_lastTimeView_folder_tableId(act);
				System.out.println("MainUi / _selectFolder / folderTableId = " + folderTableId);
				DB_folder.setFocusFolder_tableId(folderTableId);

				// set tab Id
				TabsHost.setLastExist_tabId(0);

				// check DB: before importing
//				DB_drawer.listFolders();

				// import default tables
				Import_selectedFileAct.createDefaultTables(act,fileName);

				// check DB: after importing
//				DB_drawer.listFolders();

				Util.setPref_has_default_import(act,true,position);

				// add default image
				String imageFileName = "local"+ (position+1) + ".jpg";
				Util.createAssetsFile(act,imageFileName);

				// add default video
				String videoFileName = "local"+ (position+1) + ".mp4";
				Util.createAssetsFile(act,videoFileName);

				// add default audio
				String audioFileName = "local"+ (position+1) + ".mp3";
				Util.createAssetsFile(act,audioFileName);
			}
		}

        // use Runnable to make sure only one folder background is seen
        mHandler = new Handler();
       	mHandler.post(mTabsHostRun);
    }
    
    public static Handler mHandler;
    // runnable to launch folder host
    public static Runnable mTabsHostRun =  new Runnable()
    {
        @Override
        public void run() 
        {
        	System.out.println("MainUi / mTabsHostRun");
            Fragment fragment = new TabsHost();
        	FragmentTransaction fragmentTransaction = MainAct.fragmentManager.beginTransaction();
        	fragmentTransaction.replace(R.id.content_frame, fragment).commit();//???  Can not perform this action after onSaveInstanceState
        	MainAct.fragmentManager.executePendingTransactions();
        } 
    };    
    

    
}






