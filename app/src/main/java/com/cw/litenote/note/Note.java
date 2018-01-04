package com.cw.litenote.note;

import com.cw.litenote.R;
import com.cw.litenote.db.DB_folder;
import com.cw.litenote.db.DB_page;
import com.cw.litenote.operation.audio.AudioManager;
import com.cw.litenote.operation.audio.AudioPlayer_note;
import com.cw.litenote.page.Page;
import com.cw.litenote.page.PageUi;
import com.cw.litenote.tabs.TabsHost;
import com.cw.litenote.util.CustomWebView;
import com.cw.litenote.util.DeleteFileAlarmReceiver;
import com.cw.litenote.util.audio.UtilAudio;
import com.cw.litenote.util.image.UtilImage;
import com.cw.litenote.util.preferences.Pref;
import com.cw.litenote.util.video.AsyncTaskVideoBitmapPager;
import com.cw.litenote.util.video.UtilVideo;
import com.cw.litenote.util.video.VideoPlayer;
import com.cw.litenote.util.ColorSet;
import com.cw.litenote.operation.mail.MailNotes;
import com.cw.litenote.util.uil.UilCommon;
import com.cw.litenote.util.Util;

import android.R.color;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class Note extends FragmentActivity
{
    /**
     * The pager widget, which handles animation and allows swiping horizontally to access previous
     * and next wizard steps.
     */
    public ViewPager mPager;
    public static boolean isPagerActive;

    /**
     * The pager adapter, which provides the pages to the view pager widget.
     */
    public static PagerAdapter mPagerAdapter;

    // DB
    public DB_page mDb_page;
    public static Long mNoteId;
    int mEntryPosition;
    int EDIT_CURRENT_VIEW = 5;
    int MAIL_CURRENT_VIEW = 6;
    static int mStyle;
    
    static SharedPreferences mPref_show_note_attribute;

    Button editButton;
    Button optionButton;
    Button backButton;

	public static String mAudioUriInDB;

    public FragmentActivity act;
    public static int mPlayVideoPositionOfInstance;
    public Note_audio note_audio;

    @Override
    protected void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        System.out.println("Note / _onCreate");

		// set current selection
		mEntryPosition = getIntent().getExtras().getInt("POSITION");
		NoteUi.setFocus_notePos(mEntryPosition);

		// init video
		UtilVideo.mPlayVideoPosition = 0;   // not played yet
		mPlayVideoPositionOfInstance = 0;
		AsyncTaskVideoBitmapPager.mRotationStr = null;

		act = this;

	} //onCreate end

	// callback of granted permission
	@Override
	public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
	{
		System.out.println("Note / _onRequestPermissionsResult / grantResults.length =" + grantResults.length);
		switch (requestCode)
		{
			case Util.PERMISSIONS_REQUEST_PHONE:
			{
				// If request is cancelled, the result arrays are empty.
				if ( (grantResults.length > 0) && ( (grantResults[0] == PackageManager.PERMISSION_GRANTED) ))
					UtilAudio.setPhoneListener(this);
			}
			break;
		}
	}

	// Add to prevent resizing full screen picture,
	// when popup menu shows up at picture mode
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		System.out.println("Note / _onWindowFocusChanged");
		if (hasFocus && isPictureMode() )
			Util.setFullScreen(act);
	}

	void setLayoutView()
	{
        System.out.println("Note / _setLayoutView");

		if( UtilVideo.mVideoView != null)
			UtilVideo.mPlayVideoPosition = UtilVideo.mVideoView.getCurrentPosition();

		// video view will be reset after _setContentView
		if(Util.isLandscapeOrientation(this))
			setContentView(R.layout.note_view_landscape);
		else
			setContentView(R.layout.note_view_portrait);

		ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.view_note_title);
            actionBar.setBackgroundDrawable(new ColorDrawable(ColorSet.getBarColor(this)));
        }

		mPref_show_note_attribute = getSharedPreferences("show_note_attribute", 0);

		UilCommon.init();

		// DB
		mDb_page = new DB_page(act, Pref.getPref_focusView_page_tableId(act));

		// Instantiate a ViewPager and a PagerAdapter.
		mPager = (ViewPager) findViewById(R.id.pager);
		mPagerAdapter = new Note_adapter(mPager,this);
		mPager.setAdapter(mPagerAdapter);
		mPager.setCurrentItem(NoteUi.getFocus_notePos());

		// tab style
		if(TabsHost.mDbFolder != null)
			TabsHost.mDbFolder.close();

		TabsHost.mDbFolder = new DB_folder(act,Pref.getPref_focusView_folder_tableId(act));

		mStyle = TabsHost.mDbFolder.getPageStyle(PageUi.getFocus_pagePos(), true);

		if(mDb_page != null) {
			mNoteId = mDb_page.getNoteId(NoteUi.getFocus_notePos(), true);
			mAudioUriInDB = mDb_page.getNoteAudioUri_byId(mNoteId);
		}

        if(UtilAudio.hasAudioExtension(mAudioUriInDB) ||
		   UtilAudio.hasAudioExtension(Util.getDisplayNameByUriString(mAudioUriInDB, act))) {
            note_audio = new Note_audio(this, mAudioUriInDB);
            note_audio.init_audio_block();
        }


		// Note: if mPager.getCurrentItem() is not equal to mEntryPosition, _onPageSelected will
		//       be called again after rotation
		mPager.setOnPageChangeListener(onPageChangeListener);

		// edit note button
		editButton = (Button) findViewById(R.id.view_edit);
		editButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_edit, 0, 0, 0);
		editButton.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View view)
			{
				Intent intent = new Intent(Note.this, Note_edit.class);
				intent.putExtra(DB_page.KEY_NOTE_ID, mNoteId);
				intent.putExtra(DB_page.KEY_NOTE_TITLE, mDb_page.getNoteTitle_byId(mNoteId));
				intent.putExtra(DB_page.KEY_NOTE_AUDIO_URI , mDb_page.getNoteAudioUri_byId(mNoteId));
				intent.putExtra(DB_page.KEY_NOTE_PICTURE_URI , mDb_page.getNotePictureUri_byId(mNoteId));
				intent.putExtra(DB_page.KEY_NOTE_LINK_URI , mDb_page.getNoteLinkUri_byId(mNoteId));
				intent.putExtra(DB_page.KEY_NOTE_BODY, mDb_page.getNoteBody_byId(mNoteId));
				intent.putExtra(DB_page.KEY_NOTE_CREATED, mDb_page.getNoteCreatedTime_byId(mNoteId));
				startActivityForResult(intent, EDIT_CURRENT_VIEW);
			}
		});

		// send note button
		optionButton = (Button) findViewById(R.id.view_option);
		optionButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_more, 0, 0, 0);
		optionButton.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View view)
			{
				View_note_option.note_option(act,mNoteId);
			}
		});

		// back button
		backButton = (Button) findViewById(R.id.view_back);
		backButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_back, 0, 0, 0);
		backButton.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View view) {
				if(isTextMode())
				{
					// back to view all mode
					setViewAllMode();
					setOutline(act);
				}
				else //view all mode
				{
					stopAV();
					finish();
				}
			}
		});

	}

	// on page change listener
	ViewPager.SimpleOnPageChangeListener onPageChangeListener = new ViewPager.SimpleOnPageChangeListener()
	{
		@Override
		public void onPageSelected(int nextPosition)
		{
			if(AudioManager.getAudioPlayMode()  == AudioManager.NOTE_PLAY_MODE)
                AudioManager.stopAudioPlayer();

			NoteUi.setFocus_notePos(mPager.getCurrentItem());
			System.out.println("Note / _onPageSelected");
			System.out.println("    NoteUi.getFocus_notePos() = " + NoteUi.getFocus_notePos());
			System.out.println("    nextPosition = " + nextPosition);

			mIsViewModeChanged = false;

			// show audio name
			mNoteId = mDb_page.getNoteId(nextPosition,true);
			System.out.println("Note / _onPageSelected / mNoteId = " + mNoteId);
			mAudioUriInDB = mDb_page.getNoteAudioUri_byId(mNoteId);
			System.out.println("Note / _onPageSelected / mAudioUriInDB = " + mAudioUriInDB);

			if(UtilAudio.hasAudioExtension(mAudioUriInDB)) {
                note_audio = new Note_audio(Note.this, mAudioUriInDB);
                note_audio.init_audio_block();
                note_audio.showAudioBlock();
            }

			// stop video when changing note
			String pictureUriInDB = mDb_page.getNotePictureUri_byId(mNoteId);
			if(UtilVideo.hasVideoExtension(pictureUriInDB,act)) {
				VideoPlayer.stopVideo();
				NoteUi.cancel_UI_callbacks();
			}

            setOutline(act);
		}
	};

	public static int getStyle() {
		return mStyle;
	}

	public void setStyle(int style) {
		mStyle = style;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		System.out.println("Note / _onActivityResult ");
        if((requestCode==EDIT_CURRENT_VIEW) || (requestCode==MAIL_CURRENT_VIEW))
        {
			stopAV();
        }
		else if(requestCode == MailNotes.EMAIL)
		{
			Toast.makeText(act,R.string.mail_exit,Toast.LENGTH_SHORT).show();
			// note: result code is always 0 (cancel), so it is not used
			new DeleteFileAlarmReceiver(act,
					                    System.currentTimeMillis() + 1000 * 60 * 5, // formal: 300 seconds
//						    		    System.currentTimeMillis() + 1000 * 10, // test: 10 seconds
					                    MailNotes.mAttachmentFileName);
		}

		// show current item
		if(requestCode == Util.YOUTUBE_LINK_INTENT)
        	mPager.setCurrentItem(mPager.getCurrentItem());

	    // check if there is one note at least in the pager
		if( mPager.getAdapter().getCount() > 0 )
			setOutline(act);
		else
			finish();
	}

    /** Set outline for selected view mode
    *
    *   Determined by view mode: all, picture, text
    *
    *   Controlled factor:
    *   - action bar: hide, show
    *   - full screen: full, not full
    */
	public static void setOutline(Activity act)
	{
        // Set full screen or not, and action bar
		if(isViewAllMode() || isTextMode())
		{
			Util.setFullScreen_noImmersive(act);
            if(act.getActionBar() != null)
			    act.getActionBar().show();
		}
		else if(isPictureMode())
		{
			Util.setFullScreen(act);
            if(act.getActionBar() != null)
    			act.getActionBar().hide();
		}

        // renew pager
        showSelectedView();

		LinearLayout buttonGroup = (LinearLayout) act.findViewById(R.id.view_button_group);
        // button group
        if(Note.isPictureMode() )
            buttonGroup.setVisibility(View.GONE);
        else
            buttonGroup.setVisibility(View.VISIBLE);

		TextView audioTitle = (TextView) act.findViewById(R.id.pager_audio_title);
        // audio title
        if(!Note.isPictureMode())
        {
            if(!Util.isEmptyString(audioTitle.getText().toString()) )
                audioTitle.setVisibility(View.VISIBLE);
            else
                audioTitle.setVisibility(View.GONE);
        }

        // renew options menu
        act.invalidateOptionsMenu();
	}


    //Refer to http://stackoverflow.com/questions/4434027/android-videoview-orientation-change-with-buffered-video
	/***************************************************************
	video play spec of Pause and Rotate:
	1. Rotate: keep pause state
	 pause -> rotate -> pause -> play -> continue

	2. Rotate: keep play state
	 play -> rotate -> continue play

	3. Key guard: enable pause
	 play -> key guard on/off -> pause -> play -> continue

	4. Key guard and Rotate: keep pause
	 play -> key guard on/off -> pause -> rotate -> pause
	 ****************************************************************/	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
	    super.onConfigurationChanged(newConfig);
	    System.out.println("Note / _onConfigurationChanged");

		// dismiss popup menu we
		if(NoteUi.popup != null)
		{
			NoteUi.popup.dismiss();
			NoteUi.popup = null;
		}

		NoteUi.cancel_UI_callbacks();

        setLayoutView();

        if(canShowFullScreenPicture())
            Note.setPictureMode();
        else
            Note.setViewAllMode();

        // Set outline of view mode
        setOutline(act);
	}

	@Override
	protected void onStart() {
		super.onStart();
		System.out.println("Note / _onStart");
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		System.out.println("Note / _onResume");

		setLayoutView();

		isPagerActive = true;

        if(canShowFullScreenPicture())
            Note.setPictureMode();
        else
            Note.setViewAllMode();

		setOutline(act);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		System.out.println("Note / _onPause");

		isPagerActive = false;

		// set pause when key guard is ON
		if( UtilVideo.mVideoView != null)
		{
			UtilVideo.mPlayVideoPosition = UtilVideo.mVideoView.getCurrentPosition();

			// keep play video position
			mPlayVideoPositionOfInstance = UtilVideo.mPlayVideoPosition;
			System.out.println("Note / _onPause / mPlayVideoPositionOfInstance = " + mPlayVideoPositionOfInstance);

			if(UtilVideo.mVideoPlayer != null)
				VideoPlayer.stopVideo();
		}

		// to stop YouTube web view running
    	String tagStr = "current"+ mPager.getCurrentItem()+"webView";
    	CustomWebView webView = (CustomWebView) mPager.findViewWithTag(tagStr);
    	CustomWebView.pauseWebView(webView);
    	CustomWebView.blankWebView(webView);

		// to stop Link web view running
    	tagStr = "current"+ mPager.getCurrentItem()+"linkWebView";
    	CustomWebView linkWebView = (CustomWebView) mPager.findViewWithTag(tagStr);
    	CustomWebView.pauseWebView(linkWebView);
    	CustomWebView.blankWebView(linkWebView);

		NoteUi.cancel_UI_callbacks();
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		System.out.println("Note / _onStop");
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		System.out.println("Note / _onDestroy");
	}

	// avoid exception: has leaked window android.widget.ZoomButtonsController
	@Override
	public void finish() {
		
		if(mPagerHandler != null)
			mPagerHandler.removeCallbacks(mOnBackPressedRun);		
	    
		ViewGroup view = (ViewGroup) getWindow().getDecorView();
	    view.setBackgroundColor(getResources().getColor(color.background_dark)); // avoid white flash
	    view.removeAllViews();
	    
	    super.finish();
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		System.out.println("Note / _onSaveInstanceState");
	}

	Menu mMenu;
	// On Create Options Menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) 
    {
        super.onCreateOptionsMenu(menu);
//		System.out.println("Note / _onCreateOptionsMenu");

		// inflate menu
		getMenuInflater().inflate(R.menu.pager_menu, menu);
		mMenu = menu;

		// menu item: checked status
		// get checked or not
		int isChecked = mDb_page.getNoteMarking(NoteUi.getFocus_notePos(),true);
		if( isChecked == 0)
			menu.findItem(R.id.VIEW_NOTE_CHECK).setIcon(R.drawable.btn_check_off_holo_dark);
		else
			menu.findItem(R.id.VIEW_NOTE_CHECK).setIcon(R.drawable.btn_check_on_holo_dark);

		// menu item: view mode
   		markCurrentSelected(menu.findItem(R.id.VIEW_ALL),"ALL");
		markCurrentSelected(menu.findItem(R.id.VIEW_PICTURE),"PICTURE_ONLY");
		markCurrentSelected(menu.findItem(R.id.VIEW_TEXT),"TEXT_ONLY");

	    // menu item: previous
		MenuItem itemPrev = menu.findItem(R.id.ACTION_PREVIOUS);
		itemPrev.setEnabled(mPager.getCurrentItem() > 0);
		itemPrev.getIcon().setAlpha(mPager.getCurrentItem() > 0?255:30);

		// menu item: Next or Finish
		MenuItem itemNext = menu.findItem(R.id.ACTION_NEXT);
		itemNext.setTitle((mPager.getCurrentItem() == mPagerAdapter.getCount() - 1)	?
									R.string.view_note_slide_action_finish :
									R.string.view_note_slide_action_next                  );

        // set Disable and Gray for Last item
		boolean isLastOne = (mPager.getCurrentItem() == (mPagerAdapter.getCount() - 1));
        if(isLastOne)
        	itemNext.setEnabled(false);

        itemNext.getIcon().setAlpha(isLastOne?30:255);

        return true;
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	// called after _onCreateOptionsMenu
        return true;
    }  
    
    // for menu buttons
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
            	if(isTextMode())
            	{
        			// back to view all mode
            		setViewAllMode();
					setOutline(act);
            	}
            	else if(isViewAllMode())
            	{
					stopAV();
	            	finish();
            	}
                return true;

            case R.id.VIEW_NOTE_MODE:
            	return true;

			case R.id.VIEW_NOTE_CHECK:
				int markingNow = Page.toggleNoteMarking(NoteUi.getFocus_notePos());

				// update marking
				if(markingNow == 1)
					mMenu.findItem(R.id.VIEW_NOTE_CHECK).setIcon(R.drawable.btn_check_on_holo_dark);
				else
					mMenu.findItem(R.id.VIEW_NOTE_CHECK).setIcon(R.drawable.btn_check_off_holo_dark);

				return true;

            case R.id.VIEW_ALL:
        		setViewAllMode();
				setOutline(act);
            	return true;
            	
            case R.id.VIEW_PICTURE:
        		setPictureMode();
				setOutline(act);
            	return true;

            case R.id.VIEW_TEXT:
        		setTextMode();
				setOutline(act);
            	return true;
            	
            case R.id.ACTION_PREVIOUS:
                // Go to the previous step in the wizard. If there is no previous step,
                // setCurrentItem will do nothing.
            	NoteUi.setFocus_notePos(NoteUi.getFocus_notePos()-1);
            	mPager.setCurrentItem(mPager.getCurrentItem() - 1);
                return true;

            case R.id.ACTION_NEXT:
                // Advance to the next step in the wizard. If there is no next step, setCurrentItem
                // will do nothing.
				NoteUi.setFocus_notePos(NoteUi.getFocus_notePos()+1);
            	mPager.setCurrentItem(mPager.getCurrentItem() + 1);
            	
            	//TO
//            	mMP.setVolume(0,0);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

//    //
//    // Open link of YouTube
//    //
//    // Due to "AdWords or copyright" server limitation, for some URI,
//    // "video is not available" message could show up.
//    // At this case, one solution is to switch current mobile website to desktop website by browser setting.
//    // So, base on URI key words to decide "YouTube App" or "browser" launch.
//    public void openLink_YouTube(String linkUri)
//    {
//        // by YouTube App
//        if(linkUri.contains("youtu.be"))
//        {
//            // stop audio and video if playing
//            stopAV();
//
//            String id = Util.getYoutubeId(linkUri);
//            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube://" + id));
//            act.startActivity(intent);
//        }
//        // by Chrome browser
//        else if(linkUri.contains("youtube.com"))
//        {
//            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(linkUri));
//            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            i.setPackage("com.android.chrome");
//
//            try
//            {
//                act.startActivity(i);
//            }
//            catch (ActivityNotFoundException e)
//            {
//                // Chrome is probably not installed
//                // Try with the default browser
//                i.setPackage(null);
//                act.startActivity(i);
//            }
//        }
//    }

    // on back pressed
    @Override
    public void onBackPressed() {
		System.out.println("Note / _onBackPressed");
    	// web view can go back
    	String tagStr = "current"+ mPager.getCurrentItem()+"linkWebView";
    	CustomWebView linkWebView = (CustomWebView) mPager.findViewWithTag(tagStr);
        if (linkWebView.canGoBack()) 
        {
        	linkWebView.goBack();
        }
        else if(isPictureMode())
    	{
            // dispatch touch event to show buttons
            long downTime = SystemClock.uptimeMillis();
            long eventTime = SystemClock.uptimeMillis() + 100;
            float x = 0.0f;
            float y = 0.0f;
            // List of meta states found here: developer.android.com/reference/android/view/KeyEvent.html#getMetaState()
            int metaState = 0;
            MotionEvent event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_UP,
                                                    x, y,metaState);
            dispatchTouchEvent(event);
            event.recycle();

            // in order to make sure ImageViewBackButton is effective to be clicked
            mPagerHandler = new Handler();
            mPagerHandler.postDelayed(mOnBackPressedRun, 500);
        }
    	else if(isTextMode())
    	{
			// back to view all mode
    		setViewAllMode();
			setOutline(act);
    	}
    	else
    	{
    		System.out.println("Note / _onBackPressed / view all mode");
			stopAV();
        	finish();
    	}
    }
    
    static Handler mPagerHandler;
	Runnable mOnBackPressedRun = new Runnable()
	{   @Override
		public void run()
		{
            String tagStr = "current"+ NoteUi.getFocus_notePos() +"pictureView";
            ViewGroup pictureGroup = (ViewGroup) mPager.findViewWithTag(tagStr);
            System.out.println("Note / _showPictureViewUI / tagStr = " + tagStr);

            Button picView_back_button;
            if(pictureGroup != null)
            {
                picView_back_button = (Button) (pictureGroup.findViewById(R.id.image_view_back));
                picView_back_button.performClick();
            }

			if(Note_adapter.mIntentView != null)
				Note_adapter.mIntentView = null;
		}
	};
    
    // get current picture string
    public String getCurrentPictureString()
    {
		return mDb_page.getNotePictureUri(NoteUi.getFocus_notePos(),true);
    }

    // Mark current selected
    void markCurrentSelected(MenuItem subItem, String str)
    {
        if(mPref_show_note_attribute.getString("KEY_PAGER_VIEW_MODE", "ALL")
                .equalsIgnoreCase(str))
            subItem.setIcon(R.drawable.btn_radio_on_holo_dark);
        else
            subItem.setIcon(R.drawable.btn_radio_off_holo_dark);
    }

    // Show selected view
    static void showSelectedView()
    {
   		mIsViewModeChanged = false;

		if(!Note.isTextMode())
   		{
	   		if(UtilVideo.mVideoView != null)
	   		{
	   	   		// keep current video position for NOT text mode
				mPositionOfChangeView = UtilVideo.mPlayVideoPosition;
	   			mIsViewModeChanged = true;

	   			if(VideoPlayer.mVideoHandler != null)
	   			{
					System.out.println("Note / _showSelectedView / just remove callbacks");
	   				VideoPlayer.mVideoHandler.removeCallbacks(VideoPlayer.mRunPlayVideo);
	   				if(UtilVideo.hasMediaControlWidget)
	   					VideoPlayer.cancelMediaController();
	   			}
	   		}
   			Note_adapter.mLastPosition = -1;
   		}

    	if(mPagerAdapter != null)
    		mPagerAdapter.notifyDataSetChanged(); // will call Note_adapter / _setPrimaryItem
    }
    
    public static int mPositionOfChangeView;
    public static boolean mIsViewModeChanged;
    
    static void setViewAllMode()
    {
		 mPref_show_note_attribute.edit()
		   						  .putString("KEY_PAGER_VIEW_MODE","ALL")
		   						  .apply();
    }
    
    static void setPictureMode()
    {
		 mPref_show_note_attribute.edit()
		   						  .putString("KEY_PAGER_VIEW_MODE","PICTURE_ONLY")
		   						  .apply();
    }
    
    static void setTextMode()
    {
		 mPref_show_note_attribute.edit()
		   						  .putString("KEY_PAGER_VIEW_MODE","TEXT_ONLY")
		   						  .apply();
    }
    
    
    public static boolean isPictureMode()
    {
	  	return mPref_show_note_attribute.getString("KEY_PAGER_VIEW_MODE", "ALL")
										.equalsIgnoreCase("PICTURE_ONLY");
    }
    
    public static boolean isViewAllMode()
    {
	  	return mPref_show_note_attribute.getString("KEY_PAGER_VIEW_MODE", "ALL")
										.equalsIgnoreCase("ALL");
    }

    public static boolean isTextMode()
    {
	  	return mPref_show_note_attribute.getString("KEY_PAGER_VIEW_MODE", "ALL")
										.equalsIgnoreCase("TEXT_ONLY");
    }

	static NoteUi picUI_touch;
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        int maskedAction = event.getActionMasked();
        switch (maskedAction) {
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
    			 System.out.println("Note / _dispatchTouchEvent / MotionEvent.ACTION_UP / mPager.getCurrentItem() =" + mPager.getCurrentItem());
				 //1st touch to turn on UI
				 if(picUI_touch == null) {
				 	picUI_touch = new NoteUi(act,mPager, mPager.getCurrentItem());
				 	picUI_touch.tempShow_picViewUI(5000,getCurrentPictureString());
				 }
				 //2nd touch to turn off UI
				 else
					 setTransientPicViewUI();

				 //1st touch to turn off UI (primary)
				 if(Note_adapter.picUI_primary != null)
					 setTransientPicViewUI();
    	  	  	 break;

	        case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
	        case MotionEvent.ACTION_CANCEL: 
	        	 break;
        }

        return super.dispatchTouchEvent(event);
    }

	/**
	 * Set delay for transient picture view UI
	 *
	 */
    void setTransientPicViewUI()
    {
        NoteUi.cancel_UI_callbacks();
        picUI_touch = new NoteUi(act,mPager, mPager.getCurrentItem());

        // for video
        String pictureUriInDB = mDb_page.getNotePictureUri_byId(mNoteId);
        if(UtilVideo.hasVideoExtension(pictureUriInDB,act) &&
                (UtilVideo.mVideoView != null) &&
                (UtilVideo.getVideoState() != UtilVideo.VIDEO_AT_STOP) )
        {
            if (!NoteUi.showSeekBarProgress)
                picUI_touch.tempShow_picViewUI(110, getCurrentPictureString());
            else
                picUI_touch.tempShow_picViewUI(1110, getCurrentPictureString());
        }
        // for image
        else
            picUI_touch.tempShow_picViewUI(111,getCurrentPictureString());
    }

	public static void stopAV()
	{
		if(AudioManager.getAudioPlayMode() == AudioManager.NOTE_PLAY_MODE)
            AudioManager.stopAudioPlayer();

		VideoPlayer.stopVideo();
	}

	public static void changeToNext(ViewPager mPager)
	{
		mPager.setCurrentItem(mPager.getCurrentItem() + 1);
	}

	public static void changeToPrevious(ViewPager mPager)
	{
		mPager.setCurrentItem(mPager.getCurrentItem() + 1);
	}

    // Show full screen picture when device orientation and image orientation are the same
    boolean canShowFullScreenPicture()
    {
        String pictureStr = mDb_page.getNotePictureUri(NoteUi.getFocus_notePos(),true);
		System.out.println(" Note / _canShowFullPicture / pictureStr = " +pictureStr);
//		System.out.println(" Note / _canShowFullPicture / Util.isLandscapeOrientation(act) = " +Util.isLandscapeOrientation(act));
//		System.out.println(" Note / _canShowFullPicture / UtilImage.isLandscapePicture(pictureStr) = " +UtilImage.isLandscapePicture(pictureStr));
        if( !Util.isEmptyString(pictureStr) &&
            ( (Util.isLandscapeOrientation(act) && UtilImage.isLandscapePicture(pictureStr) ) ||
            (!Util.isLandscapeOrientation(act) && !UtilImage.isLandscapePicture(pictureStr))  ) )
            return true;
        else
            return false;
    }
}