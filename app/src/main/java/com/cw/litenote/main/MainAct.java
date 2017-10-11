package com.cw.litenote.main;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import com.cw.litenote.R;
import com.cw.litenote.config.Config;
import com.cw.litenote.db.DB_folder;
import com.cw.litenote.db.DB_page;
import com.cw.litenote.drawer.Drawer;
import com.cw.litenote.folder.Folder;
import com.cw.litenote.folder.FolderUi;
import com.cw.litenote.note_add.New_noteOption;
import com.cw.litenote.operation.DeleteFoldersFragment;
import com.cw.litenote.operation.DeletePagesFragment;
import com.cw.litenote.operation.Import_webAct;
import com.cw.litenote.page.PageUi;
import com.cw.litenote.tabs.TabsHost;
import com.cw.litenote.page.Page;
import com.cw.litenote.util.ColorSet;
import com.cw.litenote.util.DeleteFileAlarmReceiver;
import com.cw.litenote.operation.Export_toSDCardFragment;
import com.cw.litenote.operation.Import_filesList;
import com.cw.litenote.db.DB_drawer;
import com.cw.litenote.util.audio.AudioPlayer;
import com.cw.litenote.util.audio.NoisyAudioStreamReceiver;
import com.cw.litenote.util.audio.UtilAudio;
import com.cw.litenote.util.image.GalleryGridAct;
import com.cw.litenote.util.image.SlideshowInfo;
import com.cw.litenote.util.image.SlideshowPlayer;
import com.cw.litenote.util.image.UtilImage;
import com.cw.litenote.preference.Define;
import com.cw.litenote.util.EULA_dlg;
import com.cw.litenote.operation.MailNotes;
import com.cw.litenote.util.OnBackPressedListener;
import com.cw.litenote.operation.MailPagesFragment;
import com.cw.litenote.util.Util;
import com.mobeta.android.dslv.SimpleDragSortCursorAdapter;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentManager.OnBackStackChangedListener;
import android.support.v4.app.FragmentTransaction;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

public class MainAct extends FragmentActivity implements OnBackStackChangedListener
{
    public static CharSequence mFolderTitle;
    public static CharSequence mAppTitle;
    public Context mContext;
    public Config mConfigFragment;
    public static Menu mMenu;
    public static DB_drawer mDb_drawer;
    public static DB_folder mDb_folder;
    public DB_page mDb_page;
    public static List<String> mFolderTitles;
	static NoisyAudioStreamReceiver noisyAudioStreamReceiver;
	static IntentFilter intentFilter;
    public static FragmentActivity mAct;
	public static FragmentManager fragmentManager;
	public static FragmentManager.OnBackStackChangedListener mOnBackStackChangedListener;
	public static int mLastOkTabId = 1;
	static SharedPreferences mPref_show_note_attribute;
	OnBackPressedListener onBackPressedListener;
    public static Drawer mDrawer;
	public static Folder mFolder;
    public static SimpleDragSortCursorAdapter folderAdapter;
    public static MainUi mMainUi;

	// Main Act onCreate
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
    	///
//    	 StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
//    	   .detectDiskReads()
//    	   .detectDiskWrites()
//    	   .detectNetwork() 
//    	   .penaltyLog()
//    	   .build());
//
//    	    StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
////    	   .detectLeakedSqlLiteObjects() //??? unmark this line will cause strict mode error
//    	   .penaltyLog() 
//    	   .penaltyDeath()
//    	   .build());     	
    	///
        super.onCreate(savedInstanceState);

        mAct = this;
        setContentView(R.layout.drawer);
		mAppTitle = getTitle();
        mMainUi = new MainUi();

		// Show Api version
        if(Define.CODE_MODE == Define.DEBUG_MODE)
		    Toast.makeText(mAct, mAppTitle + " " + "API_" + Build.VERSION.SDK_INT , Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(mAct, mAppTitle , Toast.LENGTH_SHORT).show();

		// Release mode: no debug message
        if(Define.CODE_MODE == Define.RELEASE_MODE)
        {
        	OutputStream nullDev = new OutputStream()
            {
                public  void    close() {}
                public  void    flush() {}
                public  void    write(byte[] b) {}
                public  void    write(byte[] b, int off, int len) {}
                public  void    write(int b) {}
            };
            System.setOut( new PrintStream(nullDev));
        }

        //Log.d below can be disabled by applying proguard
        //1. enable proguard-android-optimize.txt in project.properties
        //2. be sure to use newest version to avoid build error
        //3. add the following in proguard-project.txt
        /*-assumenosideeffects class android.util.Log {
        public static boolean isLoggable(java.lang.String, int);
        public static int v(...);
        public static int i(...);
        public static int w(...);
        public static int d(...);
        public static int e(...);
    	}
        */
        Log.d("test log tag","start app");

        System.out.println("================start application ==================");
        System.out.println("MainAct / _onCreate");

        UtilImage.getDefaultScaleInPercent(MainAct.this);

        mFolderTitles = new ArrayList<>();

		Context context = getApplicationContext();

        // init Drawer DB
        mDb_drawer = new DB_drawer(context);

        // init Folder DB
        int focusFolder_tableId = Util.getPref_focusView_folder_tableId(context);
        System.out.println("MainAct / _onCreate / focusFolder_tableId = " + focusFolder_tableId);
        mDb_folder = new DB_folder(context, focusFolder_tableId);

        // init Page DB
        int focusPage_tableId = Util.getPref_focusView_page_tableId(context);
        System.out.println("MainAct / _onCreate / focusPage_tableId = " + focusPage_tableId);
        mDb_page = new DB_page(context,focusPage_tableId);

		//Add note with the link which got from other App
		String intentLink = mMainUi.addNote_IntentLink(getIntent(),mAct);
        if(!Util.isEmptyString(intentLink))//TODO　??? handle each case
		{
			finish(); // LiteNote not running at first, keep closing
		}
		else
		{
			// check DB
			final boolean ENABLE_DB_CHECK = true;//true;//false
			if(ENABLE_DB_CHECK)
			{
		        // list all folder tables
                FolderUi.listAllFolderTables(mAct);

				// recover focus
				focusFolder_tableId = Util.getPref_focusView_folder_tableId(this);
	    		DB_folder.setFocusFolder_tableId(focusFolder_tableId);
				focusPage_tableId = Util.getPref_focusView_page_tableId(this);
				DB_page.setFocusPage_tableId(focusPage_tableId);
			}//if(ENABLE_DB_CHECK)

	        // get focus folder table Id, default folder table Id: 1
	        if (savedInstanceState == null)
	        {
	        	for(int i = 0; i< mDb_drawer.getFoldersCount(true); i++)
	        	{
		        	if(mDb_drawer.getFolderTableId(i,true)==Util.getPref_focusView_folder_tableId(this))
		        	{
                        FolderUi.setFocus_folderPos(i);
		    			System.out.println("MainAct / _onCreate /  FolderUi.getFocus_folderPos() = " + FolderUi.getFocus_folderPos());
		        	}
	        	}
	        	AudioPlayer.setPlayState(AudioPlayer.PLAYER_AT_STOP);
	        	UtilAudio.mIsCalledWhilePlayingAudio = false;
	        }

            // new drawer
            mDrawer = new Drawer(mAct);
            mDrawer.initDrawer();

            // new folder
            mFolder = new Folder(mAct);
            folderAdapter = mFolder.initFolder();

	        // enable ActionBar app icon to behave as action to toggle nav drawer
	        getActionBar().setDisplayHomeAsUpEnabled(true);
	        getActionBar().setHomeButtonEnabled(true);
			getActionBar().setBackgroundDrawable(new ColorDrawable(ColorSet.getBarColor(mAct)));

	        mContext = getBaseContext();

			// add on back stack changed listener
	        fragmentManager = getSupportFragmentManager();
			mOnBackStackChangedListener = MainAct.this;//??? = this?
	        fragmentManager.addOnBackStackChangedListener(mOnBackStackChangedListener);

			// register an audio stream receiver
			if(noisyAudioStreamReceiver == null)
			{
				noisyAudioStreamReceiver = new NoisyAudioStreamReceiver();
				intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
				registerReceiver(noisyAudioStreamReceiver, intentFilter);
			}
		}

		// Show license dialog
		new EULA_dlg(this).show();
    }

    @Override
    public void setTitle(CharSequence title) {
        super.setTitle(title);
        mMainUi.setFolderTitle(mAct,title,mMenu,mDrawer,mFolderTitle);
    }


    /*********************************************************************************
     *
     *                                      Life cycle
     *
     *********************************************************************************/

    // if one LiteNote Intent is already running, call it again in YouTube or Browser will run into this
    @Override
    protected void onNewIntent(Intent intent)
    {
        super.onNewIntent(intent);
		System.out.println("MainAct / _onNewIntent");
        String intentLink = mMainUi.addNote_IntentLink(intent,mAct);
        if(!Util.isEmptyString(intentLink) && intentLink.startsWith("http")) {
            Page.mItemAdapter.notifyDataSetChanged();
        }
    }

    // for Rotate screen
    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
       super.onSaveInstanceState(outState);
  	   System.out.println("MainAct / onSaveInstanceState / getFocus_folderPos() = " + FolderUi.getFocus_folderPos());
       outState.putInt("NowFolderPosition", FolderUi.getFocus_folderPos());
       outState.putInt("Playing_pageId", mPlaying_pagePos);
       outState.putInt("Playing_folderPos", mPlaying_folderPos);
       outState.putInt("SeekBarProgress", Page.mProgress);
       outState.putInt("AudioPlayerState",AudioPlayer.getPlayState());
       outState.putBoolean("CalledWhilePlayingAudio", UtilAudio.mIsCalledWhilePlayingAudio);
       if(FolderUi.mHandler != null)
    	   FolderUi.mHandler.removeCallbacks(FolderUi.mTabsHostRun);
       FolderUi.mHandler = null;
    }

    // for After Rotate
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState)
    {
    	super.onRestoreInstanceState(savedInstanceState);
		System.out.println("MainAct / _onRestoreInstanceState ");
    	if(savedInstanceState != null)
    	{
            FolderUi.setFocus_folderPos(savedInstanceState.getInt("NowFolderPosition"));
    		mPlaying_pagePos = savedInstanceState.getInt("Playing_pageId");
    		mPlaying_folderPos = savedInstanceState.getInt("Playing_folderPos");
    		AudioPlayer.setPlayState(savedInstanceState.getInt("AudioPlayerState"));
    		Page.mProgress = savedInstanceState.getInt("SeekBarProgress");
    		UtilAudio.mIsCalledWhilePlayingAudio = savedInstanceState.getBoolean("CalledWhilePlayingAudio");
    	}
    }

    @Override
    protected void onPause() {
    	super.onPause();
    	System.out.println("MainAct / _onPause");
    }

	@Override
    protected void onResume()
    {
    	System.out.println("MainAct / _onResume");

      	// To Registers a listener object to receive notification when incoming call
     	TelephonyManager telMgr = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
     	if(telMgr != null)
     	{
     		telMgr.listen(UtilAudio.phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
     	}
        super.onResume();
    }

    @Override
    protected void onResumeFragments() {
    	System.out.println("MainAct / _onResumeFragments ");
    	super.onResumeFragments();

		// fix: home button failed after power off/on in Config fragment
		fragmentManager.popBackStack();

		if(mDb_drawer.getFoldersCount(true)>0) {
			System.out.println("MainAct / _onResumeFragments / getFocus_folderPos() = " + FolderUi.getFocus_folderPos());
			FolderUi.selectFolder(FolderUi.getFocus_folderPos());
			setTitle(mFolderTitle);
		}
    }

    @Override
    protected void onDestroy()
    {
    	System.out.println("MainAct / onDestroy");

    	//unregister TelephonyManager listener
        TelephonyManager mgr = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        if(mgr != null) {
            mgr.listen(UtilAudio.phoneStateListener, PhoneStateListener.LISTEN_NONE);
        }

		// unregister an audio stream receiver
		if(noisyAudioStreamReceiver != null)
		{
			try
			{
				unregisterReceiver(noisyAudioStreamReceiver);//??? unregister here?
			}
			catch (Exception e)
			{
			}
			noisyAudioStreamReceiver = null;
		}

        // stop audio player
        if(AudioPlayer.mMediaPlayer != null)
            UtilAudio.stopAudioPlayer();

		super.onDestroy();
    }

    /**
     * When using the ActionBarDrawerToggle, you must call it during
     * onPostCreate() and onConfigurationChanged()...
     */
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
//        System.out.println("MainAct / onPostCreate");
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawer.drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        System.out.println("MainAct / _onConfigurationChanged");
        // Pass any configuration change to the drawer toggles
        mDrawer.drawerToggle.onConfigurationChanged(newConfig);

		mDrawer.drawerToggle.syncState();
    }

    /**
     *  on Back button pressed
     */
    @Override
    public void onBackPressed()
    {
        System.out.println("MainAct / _onBackPressed");

        if (onBackPressedListener != null)
        {
            DB_drawer dbDrawer = new DB_drawer(this);
            int foldersCnt = dbDrawer.getFoldersCount(true);

            if(foldersCnt == 0)
            {
                finish();
                Intent intent  = new Intent(this,MainAct.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
            else
                onBackPressedListener.doBack();
        }
        else
            super.onBackPressed();
    }

    @Override
    public void onBackStackChanged() {
        int backStackEntryCount = fragmentManager.getBackStackEntryCount();
        System.out.println("MainAct / _onBackStackChanged / backStackEntryCount = " + backStackEntryCount);

        if(backStackEntryCount == 1) // Config fragment
        {
            System.out.println("MainAct / _onBackStackChanged / Config");
            getActionBar().setDisplayShowHomeEnabled(false);
            getActionBar().setDisplayHomeAsUpEnabled(true);
            mDrawer.drawerToggle.setDrawerIndicatorEnabled(false);
        }
        else if(backStackEntryCount == 0) // Folder
        {
            onBackPressedListener = null;

            if(mFolder.adapter!=null)
                mFolder.adapter.notifyDataSetChanged();

            System.out.println("MainAct / _onBackStackChanged / Folder");
            mMainUi.initActionBar(mAct,mMenu,mDrawer);
            setTitle(mFolderTitle);
            invalidateOptionsMenu();
        }
    }

    public void setOnBackPressedListener(OnBackPressedListener onBackPressedListener) {
        this.onBackPressedListener = onBackPressedListener;
    }

    /**
     * on Activity Result
     */
    AlertDialog.Builder builder;
    AlertDialog alertDlg;
    Handler handler;
    int count;
    String countStr;
    String nextLinkTitle;
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        System.out.println("MainAct / _onActivityResult ");
        String stringFileName = null;

        // mail
        if((requestCode== MailNotes.EMAIL) ||(requestCode== MailPagesFragment.EMAIL_PAGES)) {
            if (requestCode == MailNotes.EMAIL)
                stringFileName = MailNotes.mAttachmentFileName;
            else if (requestCode == MailPagesFragment.EMAIL_PAGES)
                stringFileName = MailPagesFragment.mAttachmentFileName;

            Toast.makeText(mAct, R.string.mail_exit, Toast.LENGTH_SHORT).show();

            // note: result code is always 0 (cancel), so it is not used
            new DeleteFileAlarmReceiver(mAct,
                    System.currentTimeMillis() + 1000 * 60 * 5, // formal: 300 seconds
//					System.currentTimeMillis() + 1000 * 10, // test: 10 seconds
                    stringFileName);
        }

        // YouTube
        if(requestCode == Util.YOUTUBE_LINK_INTENT)
        {
            // preference of delay
            SharedPreferences pref_delay = getSharedPreferences("youtube_launch_delay", 0);
            count = Integer.valueOf(pref_delay.getString("KEY_YOUTUBE_LAUNCH_DELAY","10"));

            builder = new AlertDialog.Builder(this);

            Page.currPlayPosition++;

            String link = mMainUi.getYouTubeLink(this,Page.currPlayPosition);
            nextLinkTitle =  Util.getYouTubeTitle(link);

            countStr = getResources().getString(R.string.message_continue_or_stop_YouTube_message);
            countStr = countStr.replaceFirst("[0-9]",String.valueOf(count));

            builder.setTitle(R.string.message_continue_or_stop_YouTube_title)
                    .setMessage(nextLinkTitle +"\n\n" + countStr)
                    .setNegativeButton(R.string.confirm_dialog_button_no, new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog1, int which1)
                        {
                            alertDlg.dismiss();
                            mMainUi.cancelYouTubeHandler(handler,runCountDown);
                        }
                    })
                    .setPositiveButton(R.string.confirm_dialog_button_yes, new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog1, int which1) {
                            alertDlg.dismiss();
                            mMainUi.cancelYouTubeHandler(handler,runCountDown);
                            mMainUi.launchNextYouTubeIntent(mAct,handler,runCountDown);
                        }
                    });

            alertDlg = builder.create();

            // set listener for selection
            alertDlg.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dlgInterface) {
                    handler = new Handler();
                    handler.postDelayed(runCountDown,1000);
                }
            });
            alertDlg.show();
        }

        // make sure main activity is still executing
        if(requestCode == Util.YOUTUBE_ADD_NEW_LINK_INTENT)
            recreate();
    }

    /**
     * runnable for counting down
     */
    Runnable runCountDown = new Runnable() {
        public void run() {
            // show count down
            TextView messageView = (TextView) alertDlg.findViewById(android.R.id.message);
            count--;
            countStr = getResources().getString(R.string.message_continue_or_stop_YouTube_message);
            countStr = countStr.replaceFirst("[0-9]",String.valueOf(count));
            messageView.setText(nextLinkTitle + "\n\n" +countStr);

            if(count>0)
                handler.postDelayed(runCountDown,1000);
            else
            {
                // launch next intent
                alertDlg.dismiss();
                mMainUi.cancelYouTubeHandler(handler,runCountDown);
                mMainUi.launchNextYouTubeIntent(mAct,handler,runCountDown);
            }
        }
    };



    /***********************************************************************************
     *
     *                                          Menu
     *
     ***********************************************************************************/

    /****************************************************
     *  On Prepare Option menu :
     *  Called whenever we call invalidateOptionsMenu()
     ****************************************************/
    @Override
    public boolean onPrepareOptionsMenu(android.view.Menu menu) {
        System.out.println("MainAct / _onPrepareOptionsMenu");

        //??? why still got here even already call finish()after having got YouTube link
        if((mDrawer == null) || (mDrawer.drawerLayout == null))
            return false;

        DB_drawer db_drawer = new DB_drawer(this);
        int foldersCnt = db_drawer.getFoldersCount(true);

        /**
         * Folder group
         */
        // If the navigation drawer is open, hide action items related to the content view
        if(mDrawer.isDrawerOpen())
        {
            mMenu.setGroupVisible(R.id.group_folders, true);

            mMenu.findItem(R.id.DELETE_FOLDERS).setVisible(foldersCnt >0);
            mMenu.findItem(R.id.ENABLE_FOLDER_DRAG_AND_DROP).setVisible(foldersCnt >1);

            mMenu.setGroupVisible(R.id.group_pages_and_more, false);
            mMenu.setGroupVisible(R.id.group_notes, false);
        }
        else if(!mDrawer.isDrawerOpen())
        {
            mMenu.setGroupVisible(R.id.group_folders, false);

            /**
             * Page group and more
             */
            mMenu.setGroupVisible(R.id.group_pages_and_more, foldersCnt >0);

            if(foldersCnt>0)
            {
                setTitle(mFolderTitle);

                int pgsCnt = FolderUi.getFolder_pagesCount(FolderUi.getFocus_folderPos());
                String preStr = "MainAct / _onPrepareOptionsMenu / ";
                System.out.println(preStr + "FolderUi.getFocus_folderPos() = " + FolderUi.getFocus_folderPos());

                int notesCnt = 0;
                System.out.println(preStr + "DB_page.getFocusPage_tableId() = " + DB_page.getFocusPage_tableId());
                if(Page.mDb_page != null){
                    try {
                        notesCnt = Page.mDb_page.getNotesCount(true);
                    }
                    catch (Exception e)
                    {
                        System.out.println(preStr + "Page.mDb_page.getNotesCount(false) error / 0 page ");
                        notesCnt = 0;
                    }
                }

                // change page color
                mMenu.findItem(R.id.CHANGE_PAGE_COLOR).setVisible(pgsCnt >0);

                // pages order
                mMenu.findItem(R.id.SHIFT_PAGE).setVisible(pgsCnt >1);

                // delete pages
                mMenu.findItem(R.id.DELETE_PAGES).setVisible(pgsCnt >0);

                // note operation
                mMenu.findItem(R.id.note_operation).setVisible( (pgsCnt >0) && (notesCnt>0) );

                // EXPORT TO SD CARD
                mMenu.findItem(R.id.EXPORT_TO_SD_CARD).setVisible(pgsCnt >0);

                // SEND PAGES
                mMenu.findItem(R.id.SEND_PAGES).setVisible(pgsCnt >0);

                /**
                 *  Note group
                 */
                // group of notes
                mMenu.setGroupVisible(R.id.group_notes, pgsCnt > 0);

                // play
                mMenu.findItem(R.id.PLAY).setVisible( (pgsCnt >0) && (notesCnt>0) );

                // HANDLE CHECKED NOTES
                mMenu.findItem(R.id.HANDLE_CHECKED_NOTES).setVisible( (pgsCnt >0) && (notesCnt>0) );
            }
            else if(foldersCnt==0)
            {
                /**
                 *  Note group
                 */
                mMenu.setGroupVisible(R.id.group_notes, false);
            }
        }
        return super.onPrepareOptionsMenu(menu);
    }

	/*************************
	 * onCreate Options Menu
     *
	 *************************/
	public static MenuItem mSubMenuItemAudio;
	MenuItem playOrStopMusicButton;
	@Override
	public boolean onCreateOptionsMenu(android.view.Menu menu)
	{
//		System.out.println("MainAct / _onCreateOptionsMenu");
		mMenu = menu;

		// inflate menu
		getMenuInflater().inflate(R.menu.main_menu, menu);

		playOrStopMusicButton = menu.findItem(R.id.PLAY_OR_STOP_MUSIC);

		// enable drag note
		mPref_show_note_attribute = getSharedPreferences("show_note_attribute", 0);
		if(mPref_show_note_attribute.getString("KEY_ENABLE_DRAGGABLE", "no").equalsIgnoreCase("yes"))
			menu.findItem(R.id.ENABLE_NOTE_DRAG_AND_DROP)
					.setIcon(R.drawable.btn_check_on_holo_light)
					.setTitle(R.string.drag_note) ;
		else
			menu.findItem(R.id.ENABLE_NOTE_DRAG_AND_DROP)
					.setIcon(R.drawable.btn_check_off_holo_light)
					.setTitle(R.string.drag_note) ;

	    // enable show body
	    mPref_show_note_attribute = getSharedPreferences("show_note_attribute", 0);
    	if(mPref_show_note_attribute.getString("KEY_SHOW_BODY", "yes").equalsIgnoreCase("yes"))
			menu.findItem(R.id.SHOW_BODY)
					.setIcon(R.drawable.btn_check_on_holo_light)
					.setTitle(R.string.preview_note_body) ;
    	else
			menu.findItem(R.id.SHOW_BODY)
				.setIcon(R.drawable.btn_check_off_holo_light)
				.setTitle(R.string.preview_note_body) ;


		// enable click launch YouTube
		mPref_show_note_attribute = getSharedPreferences("show_note_attribute", 0);
		if(mPref_show_note_attribute.getString("KEY_VIEW_NOTE_LAUNCH_YOUTUBE", "no").equalsIgnoreCase("yes"))
			menu.findItem(R.id.CLICK_LAUNCH_YOUTUBE)
					.setIcon(R.drawable.btn_check_on_holo_light)
					.setTitle(R.string.click_launch_youtube);
		else
			menu.findItem(R.id.CLICK_LAUNCH_YOUTUBE)
					.setIcon(R.drawable.btn_check_off_holo_light)
					.setTitle(R.string.click_launch_youtube) ;

		//
	    // Group 1 sub_menu for drawer operation
		//

	    // add sub_menu item: add folder drag setting
    	if(mPref_show_note_attribute.getString("KEY_ENABLE_FOLDER_DRAGGABLE", "no")
    								.equalsIgnoreCase("yes"))
			menu.findItem(R.id.ENABLE_FOLDER_DRAG_AND_DROP)
				.setIcon(R.drawable.btn_check_on_holo_light)
				.setTitle(R.string.drag_folder) ;
    	else
			menu.findItem(R.id.ENABLE_FOLDER_DRAG_AND_DROP)
				.setIcon(R.drawable.btn_check_off_holo_light)
				.setTitle(R.string.drag_folder) ;

		return super.onCreateOptionsMenu(menu);
	}

	/******************************
	 * on options item selected
     *
	 ******************************/
	public static SlideshowInfo slideshowInfo;
	static FragmentTransaction mFragmentTransaction;
	public static int mPlaying_pageTableId;
	public static int mPlaying_pagePos;
	public static int mPlaying_folderPos;
	public static int mPlaying_folderTableId;

    static int mMenuUiState;

    public static void setMenuUiState(int mMenuState) {
        mMenuUiState = mMenuState;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) //??? java.lang.IllegalStateException: Can not perform this action after onSaveInstanceState
    {
		setMenuUiState(item.getItemId());

		// Go back: check if Configure fragment now
		if( (item.getItemId() == android.R.id.home ))
    	{
    		System.out.println("MainAct / _onOptionsItemSelected / Home key of Config is pressed / fragmentManager.getBackStackEntryCount() =" +
            fragmentManager.getBackStackEntryCount());

            if(fragmentManager.getBackStackEntryCount() > 0 )
			{
                DB_drawer dbDrawer = new DB_drawer(this);
                int foldersCnt = dbDrawer.getFoldersCount(true);
                System.out.println("MainAct / _onOptionsItemSelected / Home key of Config is pressed / foldersCnt = " + foldersCnt);

                if(foldersCnt == 0)
                {
                    finish();
                    Intent intent  = new Intent(this,MainAct.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                }
                else {
                    fragmentManager.popBackStack();//??? folders count = 1 : exception
                    {
                        mMainUi.initActionBar(mAct,mMenu,mDrawer);
                        mFolderTitle = mDb_drawer.getFolderTitle(FolderUi.getFocus_folderPos(),true);
                        setTitle(mFolderTitle);
                        mDrawer.closeDrawer();
                    }
                }
				return true;
			}
    	}


    	// The action bar home/up action should open or close the drawer.
    	// ActionBarDrawerToggle will take care of this.
    	if (mDrawer.drawerToggle.onOptionsItemSelected(item))
    	{
    		System.out.println("MainAct / _onOptionsItemSelected / drawerToggle.onOptionsItemSelected(item) == true ");
    		return true;
    	}

        switch (item.getItemId())
        {
	    	case MenuId.ADD_NEW_FOLDER:
	    		FolderUi.renewFirstAndLast_folderId();
                FolderUi.addNewFolder(mAct, FolderUi.mLastExist_folderTableId +1);
				return true;

	    	case MenuId.ENABLE_FOLDER_DRAG_AND_DROP:
            	if(mPref_show_note_attribute.getString("KEY_ENABLE_FOLDER_DRAGGABLE", "no")
            			                    .equalsIgnoreCase("yes"))
            	{
            		mPref_show_note_attribute.edit().putString("KEY_ENABLE_FOLDER_DRAGGABLE","no")
            								 .apply();
					mFolder.listView.setDragEnabled(false);
                    Toast.makeText(mAct,getResources().getString(R.string.drag_folder)+
                                        ": " +
                                        getResources().getString(R.string.set_disable),
                                   Toast.LENGTH_SHORT).show();
            	}
            	else
            	{
            		mPref_show_note_attribute.edit().putString("KEY_ENABLE_FOLDER_DRAGGABLE","yes")
            								 .apply();
					mFolder.listView.setDragEnabled(true);
                    Toast.makeText(mAct,getResources().getString(R.string.drag_folder) +
                                        ": " +
                                        getResources().getString(R.string.set_enable),
                                   Toast.LENGTH_SHORT).show();
            	}
                folderAdapter.notifyDataSetChanged();
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
                return true;

            case MenuId.DELETE_FOLDERS:
//                mDb_folder = new DB_folder(this,DB_folder.getFocusFolder_tableId());
                mDb_drawer = new DB_drawer(this);
                if(mDb_drawer.getFoldersCount(true)>0)
                {
                    mDrawer.closeDrawer();
                    mMenu.setGroupVisible(R.id.group_notes, false); //hide the menu
                    DeleteFoldersFragment delFoldersFragment = new DeleteFoldersFragment();
                    mFragmentTransaction = fragmentManager.beginTransaction();
                    mFragmentTransaction.setCustomAnimations(R.anim.fragment_slide_in_left, R.anim.fragment_slide_out_left, R.anim.fragment_slide_in_right, R.anim.fragment_slide_out_right);
                    mFragmentTransaction.replace(R.id.content_frame, delFoldersFragment).addToBackStack("delete_folders").commit();
                }
                else
                {
                    Toast.makeText(this, R.string.config_export_none_toast, Toast.LENGTH_SHORT).show();
                }
                return true;

			case MenuId.ADD_NEW_NOTE:
				New_noteOption.addNewNote(this);
				return true;

        	case MenuId.OPEN_PLAY_SUBMENU:
        		// new play instance: stop button is off
        	    if( (AudioPlayer.mMediaPlayer != null) &&
        	    	(AudioPlayer.getPlayState() != AudioPlayer.PLAYER_AT_STOP))
        		{
       		    	// show Stop
           			playOrStopMusicButton.setTitle(R.string.menu_button_stop_audio);
           			playOrStopMusicButton.setIcon(R.drawable.ic_media_stop);
        	    }
        	    else
        	    {
       		    	// show Play
           			playOrStopMusicButton.setTitle(R.string.menu_button_play_audio);
           			playOrStopMusicButton.setIcon(R.drawable.ic_media_play);
        	    }
        		return true;

        	case MenuId.PLAY_OR_STOP_AUDIO:
        		if( (AudioPlayer.mMediaPlayer != null) &&
        			(AudioPlayer.getPlayState() != AudioPlayer.PLAYER_AT_STOP))
        		{
					UtilAudio.stopAudioPlayer();
					TabsHost.setAudioPlayingTab_WithHighlight(false);
					Page.mItemAdapter.notifyDataSetChanged();
					Page.showFooter();
					return true; // just stop playing, wait for user action
        		}
        		else
        		{
        			AudioPlayer.setPlayMode(AudioPlayer.CONTINUE_MODE);
        			AudioPlayer.mAudioPos = 0;
       				AudioPlayer.prepareAudioInfo();

        			AudioPlayer.runAudioState(this);

					Page.mItemAdapter.notifyDataSetChanged();
	        		Page.showFooter();

					// update page table Id
					mPlaying_pageTableId = TabsHost.mNow_pageTableId;
					// update playing tab index
					mPlaying_pagePos = PageUi.getFocus_pagePos();
					// update playing drawer position
				    mPlaying_folderPos = FolderUi.getFocus_folderPos();
        		}
        		return true;

        	case MenuId.SLIDE_SHOW:
        		slideshowInfo = new SlideshowInfo();

        		int pageTableId = Util.getPref_focusView_page_tableId(this);
    			DB_page.setFocusPage_tableId(pageTableId);

        		// add images for slide show
    			mDb_page.open();
                int count = mDb_page.getNotesCount(false);
        		for(int position = 0; position < mDb_page.getNotesCount(false) ; position++)
        		{
        			if(mDb_page.getNoteMarking(position,false) == 1)
        			{
						String pictureUri = mDb_page.getNotePictureUri(position,false);
						String linkUri = mDb_page.getNoteLinkUri(position,false);

                        // replace picture path
						if(Util.isEmptyString(pictureUri) && UtilImage.hasImageExtension(linkUri,this))
                            pictureUri = linkUri;

                        String title = mDb_folder.getCurrentPageTitle();
                        title = title.concat(" " + "(" + (position+1) + "/" + count + ")");
						String text = mDb_page.getNoteTitle(position,false);

						if(!Util.isEmptyString(mDb_page.getNoteBody(position,false)))
							text += " : " + mDb_page.getNoteBody(position,false);

						if( (!Util.isEmptyString(pictureUri) && UtilImage.hasImageExtension(pictureUri,this)) ||
                            !(Util.isEmptyString(text)) 														) // skip empty
						{
							slideshowInfo.addShowItem(title,pictureUri,text,position);
						}
        			}
        		}
        		mDb_page.close();

        		if(slideshowInfo.showItemsSize() > 0)
        		{
					// create new Intent to launch the slideShow player Activity
					Intent playSlideshow = new Intent(this, SlideshowPlayer.class);
					startActivity(playSlideshow);
        		}
        		else
        			Toast.makeText(mContext,R.string.file_not_found,Toast.LENGTH_SHORT).show();
        		return true;

            case MenuId.ADD_NEW_PAGE:
            	int pgCnt = FolderUi.getFolder_pagesCount(FolderUi.getFocus_folderPos());
//				PageUi.addNewPage(mAct, TabsHost.mLastPos_pageTableId + 1);
				PageUi.addNewPage(mAct, pgCnt + 1);

                return true;

            case MenuId.CHANGE_PAGE_COLOR:
            	PageUi.changePageColor(mAct);
                return true;

            case MenuId.SHIFT_PAGE:
			PageUi.shiftPage(mAct);
			return true;

			case MenuId.DELETE_PAGES:
                mDb_folder = new DB_folder(this,DB_folder.getFocusFolder_tableId());
				if(mDb_folder.getPagesCount(true)>0)
				{
                    mMenu.setGroupVisible(R.id.group_notes, false); //hide the menu
					DeletePagesFragment delPgsFragment = new DeletePagesFragment();
					mFragmentTransaction = fragmentManager.beginTransaction();
					mFragmentTransaction.setCustomAnimations(R.anim.fragment_slide_in_left, R.anim.fragment_slide_out_left, R.anim.fragment_slide_in_right, R.anim.fragment_slide_out_right);
					mFragmentTransaction.replace(R.id.content_frame, delPgsFragment).addToBackStack("delete_pages").commit();
				}
				else
				{
					Toast.makeText(this, R.string.no_page_yet, Toast.LENGTH_SHORT).show();
				}
			return true;

			case MenuId.ENABLE_NOTE_DRAG_AND_DROP:
				mPref_show_note_attribute = mContext.getSharedPreferences("show_note_attribute", 0);
				if(mPref_show_note_attribute.getString("KEY_ENABLE_DRAGGABLE", "no").equalsIgnoreCase("yes")) {
                    mPref_show_note_attribute.edit().putString("KEY_ENABLE_DRAGGABLE", "no").apply();
                    Toast.makeText(mAct,getResources().getString(R.string.drag_note)+
                                        ": " +
                                        getResources().getString(R.string.set_disable),
                                   Toast.LENGTH_SHORT).show();
                }
				else {
                    mPref_show_note_attribute.edit().putString("KEY_ENABLE_DRAGGABLE", "yes").apply();
                    Toast.makeText(mAct,getResources().getString(R.string.drag_note) +
                                        ": " +
                                        getResources().getString(R.string.set_enable),
                                   Toast.LENGTH_SHORT).show();
                }
				TabsHost.updateTabChange(this);
				return true;

			case MenuId.SHOW_BODY:
            	mPref_show_note_attribute = mContext.getSharedPreferences("show_note_attribute", 0);
            	if(mPref_show_note_attribute.getString("KEY_SHOW_BODY", "yes").equalsIgnoreCase("yes")) {
                    mPref_show_note_attribute.edit().putString("KEY_SHOW_BODY", "no").apply();
                    Toast.makeText(mAct,getResources().getString(R.string.preview_note_body) +
										": " +
										getResources().getString(R.string.set_disable),
									Toast.LENGTH_SHORT).show();
                }
            	else {
                    mPref_show_note_attribute.edit().putString("KEY_SHOW_BODY", "yes").apply();
                    Toast.makeText(mAct,getResources().getString(R.string.preview_note_body) +
										": " +
										getResources().getString(R.string.set_enable),
								   Toast.LENGTH_SHORT).show();
                }
            	TabsHost.updateTabChange(this);
                return true;

			case MenuId.CLICK_LAUNCH_YOUTUBE:
				mPref_show_note_attribute = mContext.getSharedPreferences("show_note_attribute", 0);
				if(mPref_show_note_attribute.getString("KEY_VIEW_NOTE_LAUNCH_YOUTUBE", "no").equalsIgnoreCase("yes")) {
					mPref_show_note_attribute.edit().putString("KEY_VIEW_NOTE_LAUNCH_YOUTUBE", "no").apply();
					Toast.makeText(mAct,getResources().getString(R.string.click_launch_youtube) +
                                        ": " +
                                        getResources().getString(R.string.set_disable),
                                   Toast.LENGTH_SHORT).show();
				}
				else {
					mPref_show_note_attribute.edit().putString("KEY_VIEW_NOTE_LAUNCH_YOUTUBE", "yes").apply();
					Toast.makeText(mAct,getResources().getString(R.string.click_launch_youtube) +
                                        ": " +
                                        getResources().getString(R.string.set_enable),
                                   Toast.LENGTH_SHORT).show();
				}
				invalidateOptionsMenu();
				return true;

			case MenuId.EXPORT_TO_SD_CARD:
				mMenu.setGroupVisible(R.id.group_notes, false); //hide the menu
                mDb_folder = new DB_folder(this,DB_folder.getFocusFolder_tableId());
				if(mDb_folder.getPagesCount(true)>0)
				{
					Export_toSDCardFragment exportFragment = new Export_toSDCardFragment();
					FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
					transaction.setCustomAnimations(R.anim.fragment_slide_in_left, R.anim.fragment_slide_out_left, R.anim.fragment_slide_in_right, R.anim.fragment_slide_out_right);
					transaction.replace(R.id.content_frame, exportFragment,"export").addToBackStack(null).commit();
				}
				else
				{
					Toast.makeText(this, R.string.no_page_yet, Toast.LENGTH_SHORT).show();
				}
				return true;

			case MenuId.IMPORT_FROM_SD_CARD:
				mMenu.setGroupVisible(R.id.group_notes, false); //hide the menu
				Import_filesList importFragment = new Import_filesList();
				FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
				transaction.setCustomAnimations(R.anim.fragment_slide_in_left, R.anim.fragment_slide_out_left, R.anim.fragment_slide_in_right, R.anim.fragment_slide_out_right);
				transaction.replace(R.id.content_frame, importFragment,"import").addToBackStack(null).commit();
				return true;

			case MenuId.IMPORT_FROM_WEB:
                Intent import_web = new Intent(this,Import_webAct.class);
                startActivityForResult(import_web,8000);

				return true;

			case MenuId.SEND_PAGES:
				mMenu.setGroupVisible(R.id.group_notes, false); //hide the menu

				DB_folder dbFolderMail = new DB_folder(this,DB_folder.getFocusFolder_tableId());
				if(dbFolderMail.getPagesCount(true)>0)
				{
					MailPagesFragment mailFragment = new MailPagesFragment();
					FragmentTransaction transactionMail = getSupportFragmentManager().beginTransaction();
					transactionMail.setCustomAnimations(R.anim.fragment_slide_in_left, R.anim.fragment_slide_out_left, R.anim.fragment_slide_in_right, R.anim.fragment_slide_out_right);
					transactionMail.replace(R.id.content_frame, mailFragment,"mail").addToBackStack(null).commit();
				}
				else
				{
					Toast.makeText(this, R.string.no_page_yet, Toast.LENGTH_SHORT).show();
				}
            	return true;

            case MenuId.GALLERY:
				Intent i_browsePic = new Intent(this, GalleryGridAct.class);
				startActivity(i_browsePic);
            	return true;

            case MenuId.CONFIG:
            	mMenu.setGroupVisible(R.id.group_notes, false); //hide the menu
        		setTitle(R.string.settings);

            	mConfigFragment = new Config();
            	mFragmentTransaction = fragmentManager.beginTransaction();
				mFragmentTransaction.setCustomAnimations(R.anim.fragment_slide_in_left, R.anim.fragment_slide_out_left, R.anim.fragment_slide_in_right, R.anim.fragment_slide_out_right);
                mFragmentTransaction.replace(R.id.content_frame, mConfigFragment).addToBackStack("config").commit();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }
}