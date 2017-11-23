package com.cw.litenote.tabs;

import java.util.ArrayList;

import com.cw.litenote.R;
import com.cw.litenote.db.DB_drawer;
import com.cw.litenote.db.DB_folder;
import com.cw.litenote.db.DB_page;
import com.cw.litenote.folder.FolderUi;
import com.cw.litenote.main.MainAct;
import com.cw.litenote.operation.audio.AudioManager;
import com.cw.litenote.page.Page;
import com.cw.litenote.page.PageUi;
import com.cw.litenote.util.image.UtilImage;
import com.cw.litenote.util.ColorSet;
import com.cw.litenote.util.Util;
import com.cw.litenote.util.preferences.Pref;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTabHost;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnLongClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TabWidget;
import android.widget.TextView;
import android.widget.TabHost.OnTabChangeListener;

public class TabsHost extends Fragment
{
    public static FragmentTabHost mTabsHost;
    public static int mPagesCount;
	static String TAB_SPEC_PREFIX = "tab";
	static String TAB_SPEC;
	static String mClassName;
	// for DB
	public static DB_folder mDbFolder;
	private static Cursor mPageCursor;
	
	static SharedPreferences mPref_FinalPageViewed;
	private static SharedPreferences mPref_delete_warn;
	public static int mNow_pageTableId;
	static ArrayList<String> mTabIndicator_ArrayList = new ArrayList<>();
	public static int mFirstPos_PageId =0;
	static int mLastPos_pageId =0;
	public static int mLastPos_pageTableId;
	public static HorizontalScrollView mHorScrollView;
    public static FragmentActivity mAct;

    public TabsHost(){}

    @Override
	public void onCreate(final Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        // get final viewed table Id
		mAct = getActivity();
		int tableId = Pref.getPref_focusView_page_tableId(mAct);
		mClassName = getClass().getSimpleName();
		//System.out.println("TabsHost / onCreate / strFinalPageViewed_tableId = " + tableId);
        System.out.println(mClassName + " / onCreate / strFinalPageViewed_tableId = " + tableId);
    }

	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
    	System.out.println("TabsHost / _onCreateView");
		View rootView;

        if(FolderUi.getFolder_pagesCount(mAct,FolderUi.getFocus_folderPos()) == 0) {
            rootView = inflater.inflate(R.layout.page_view_blank, container, false);
            System.out.println("TabsHost / _onCreateView / rootView is empty TextView");
        }
        else {
            // set layout by orientation
            if (Util.isLandscapeOrientation(mAct))
                rootView = inflater.inflate(R.layout.page_view_landscape, container, false);
            else
                rootView = inflater.inflate(R.layout.page_view_portrait, container, false);
        }

        setRootView(rootView);

		if(mDbFolder != null)
			mDbFolder.close();

		DB_drawer dB_drawer = new DB_drawer(mAct);
		int folderTableId = dB_drawer.getFolderTableId(FolderUi.getFocus_folderPos(),true);
		mDbFolder = new DB_folder(mAct,folderTableId);

        mDbFolder.open();
        // check if only one page left
        int pagesCount = mDbFolder.getPagesCount(false);
        System.out.println("TabsHost / _onCreateView / pagesCount = " + pagesCount);
        mDbFolder.close();

        if(pagesCount >0)
        {
            setTabHost();
            setTab(mAct);
        }
        return rootView;
    }

    @Override
	public void onResume() {
		super.onResume();
		System.out.println("TabsHost / _onResume");
	}
	
	
	@Override
	public void onPause() {
		super.onPause();
//		System.out.println("TabsHost / _onPause");
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		System.out.println("TabsHost / _onSaveInstanceState");
//		super.onSaveInstanceState(outState);
	}
	
	@Override
	public void onStop() {
//		System.out.println("TabsHost / _onStop");
		super.onStop();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
//		System.out.println("TabsHost / _onDestroy");
		if(mTabsHost != null)
			mTabsHost.clearAllTabs(); // clear for changing drawer
	}
    
    static View mRootView;
	private void setRootView(View rootView) {
		mRootView = rootView;
	}
	
	private static View getRootView()
	{
		return mRootView;
	}

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        System.out.println("TabsHost / _onConfigurationChanged");

		//for audio layout configuration change
		if( (AudioManager.mMediaPlayer != null) &&
			(AudioManager.getPlayerState() != AudioManager.PLAYER_AT_STOP)) {
			FolderUi.selectFolder(mAct,FolderUi.getFocus_folderPos());
		}
    }

    /**
	 * set tab host
	 * 
	 */
	protected void setTabHost()
	{
		// declare tab widget
        TabWidget tabWidget = (TabWidget) getRootView().findViewById(android.R.id.tabs);
        
        // declare linear layout
        LinearLayout linearLayout = (LinearLayout) tabWidget.getParent();
        
        // set horizontal scroll view
        HorizontalScrollView horScrollView = new HorizontalScrollView(mAct);
        horScrollView.setLayoutParams(new FrameLayout.LayoutParams(
								            FrameLayout.LayoutParams.MATCH_PARENT,
								            FrameLayout.LayoutParams.WRAP_CONTENT));
        linearLayout.addView(horScrollView, 0);
        linearLayout.removeView(tabWidget);
        
        horScrollView.addView(tabWidget);
        horScrollView.setHorizontalScrollBarEnabled(true); //set scroll bar
        horScrollView.setHorizontalFadingEdgeEnabled(true); // set fading edge
        mHorScrollView = horScrollView;

		// tab host
        mTabsHost = (FragmentTabHost)getRootView().findViewById(android.R.id.tabhost);

        //for android-support-v4.jar
        //mTabsHost.setup(this, getSupportFragmentManager(), R.id.realtabcontent);

        //add frame layout for android-support-v13.jar
        //Note: must use getChildFragmentManager() for nested fragment
        mTabsHost.setup(mAct, getChildFragmentManager(), android.R.id.tabcontent);
	}
	
	static public void setTab(FragmentActivity act)
	{
		System.out.println("TabsHost/ _setTab");
        //set tab indicator
    	setTabIndicator(act);
    	
    	// set tab listener
    	setTabChangeListener(act);
    	setTabEditListener(act);
	}
	
	/**
	 * set tab indicator
	 * 
	 */
	protected static void setTabIndicator(final Activity act)
	{
//		int folderTableId = DB_folder.getFocusFolder_tableId();
//		System.out.println("TabsHost / _setTabIndicator / folderTableId = " + folderTableId);
		
		// get final viewed table Id
        int tableId = Pref.getPref_focusView_page_tableId(act);
        System.out.println("TabsHost / _setTabIndicator / final viewed tableId = " + tableId);

        DB_drawer dB_drawer = new DB_drawer(act);
        int folderTableId = dB_drawer.getFolderTableId(FolderUi.getFocus_folderPos(),true);

        if(mDbFolder != null)
            mDbFolder = null;

        mDbFolder = new DB_folder(mAct,folderTableId);

		mDbFolder.open();
		mPagesCount = mDbFolder.getPagesCount(false);
		System.out.println("TabsHost / _setTabIndicator / mPagesCount = " + mPagesCount);

		// get first tab id and last tab id
		int i = 0;
		while(i < mPagesCount)
    	{
    		mTabIndicator_ArrayList.add(i, mDbFolder.getPageTitle(i, false));
    		
    		int pageId = mDbFolder.getPageId(i, false);
    		
    		mPageCursor = mDbFolder.getPageCursor();
    		mPageCursor.moveToPosition(i);
    		
			if(mPageCursor.isFirst())
			{
				mFirstPos_PageId = pageId ;
			}
			
			if(mPageCursor.isLast())
			{
                setLastPos_pageId(pageId);
			}
			i++;
    	}
    	
		mLastPos_pageTableId = 0;
		// get focus view table id
		for(int iPosition = 0; iPosition< mPagesCount; iPosition++)
		{
			int pageTableId = mDbFolder.getPageTableId(iPosition,false);
			if(tableId == pageTableId)
                PageUi.setFocus_pagePos(iPosition);	// starts from 0

			if( pageTableId >= mLastPos_pageTableId)
				mLastPos_pageTableId = pageTableId;
		}
		mDbFolder.close();
		
		System.out.println("TabsHost / mLastPos_pageTableId = " + mLastPos_pageTableId);
		
    	//add tab
//        mTabsHost.getTabWidget().setStripEnabled(true); // enable strip
        i = 0;
        while(i < mPagesCount)
        {
            TAB_SPEC = TAB_SPEC_PREFIX.concat(String.valueOf(mDbFolder.getPageId(i,true)));
//        	System.out.println(mClassName + " / addTab / " + i);
            mTabsHost.addTab(mTabsHost.newTabSpec(TAB_SPEC).setIndicator(mTabIndicator_ArrayList.get(i)),
							 Page.class, //interconnection //KP call page class here
							 null);
            
            //set round corner and background color
            int style = mDbFolder.getPageStyle(i, true);
    		switch(style)
    		{
    			case 0:
    				mTabsHost.getTabWidget().getChildAt(i).setBackgroundResource(R.drawable.bg_0);
    				break;
    			case 1:
    				mTabsHost.getTabWidget().getChildAt(i).setBackgroundResource(R.drawable.bg_1);
    				break;
    			case 2:
    				mTabsHost.getTabWidget().getChildAt(i).setBackgroundResource(R.drawable.bg_2);
    				break;
    			case 3:
    				mTabsHost.getTabWidget().getChildAt(i).setBackgroundResource(R.drawable.bg_3);
    				break;
    			case 4:
    				mTabsHost.getTabWidget().getChildAt(i).setBackgroundResource(R.drawable.bg_4);
    				break;	
    			case 5:
    				mTabsHost.getTabWidget().getChildAt(i).setBackgroundResource(R.drawable.bg_5);
    				break;	
    			case 6:
    				mTabsHost.getTabWidget().getChildAt(i).setBackgroundResource(R.drawable.bg_6);
    				break;	
    			case 7:
    				mTabsHost.getTabWidget().getChildAt(i).setBackgroundResource(R.drawable.bg_7);
    				break;	
    			case 8:
    				mTabsHost.getTabWidget().getChildAt(i).setBackgroundResource(R.drawable.bg_8);
    				break;		
    			case 9:
    				mTabsHost.getTabWidget().getChildAt(i).setBackgroundResource(R.drawable.bg_9);
    				break;		
    			default:
    				break;
    		}
    		
            //set text color
	        TextView tv = (TextView) mTabsHost.getTabWidget().getChildAt(i).findViewById(android.R.id.title);
		    if((style%2) == 1)
    		{	
		        tv.setTextColor(Color.argb(255,0,0,0));
    		}
           	else
           	{
		        tv.setTextColor(Color.argb(255,255,255,255));
           	}
            // set tab text center
	    	int tabCount = mTabsHost.getTabWidget().getTabCount();
	    	for (int j = 0; j < tabCount; j++) {
	    	    final View view = mTabsHost.getTabWidget().getChildTabViewAt(j);
	    	    if ( view != null ) {
	    	        //  get title text view
	    	        final View textView = view.findViewById(android.R.id.title);
	    	        if ( textView instanceof TextView ) {
	    	            ((TextView) textView).setGravity(Gravity.CENTER);
	    	            ((TextView) textView).setSingleLine(true);
	    	            textView.setPadding(6, 0, 6, 0);
	    	            textView.setMinimumWidth(96);
	    	            ((TextView) textView).setMaxWidth(UtilImage.getScreenWidth(act)/2);
	    	            textView.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
	    	        }
	    	    }
	    	}
	    	i++;
        }
        
        setTabMargin(act);

		System.out.println("TabsHost / setTabIndicator / PageUi.mFocus_pagePos = " + PageUi.getFocus_pagePos());
		
		//set background color to selected tab 
		mTabsHost.setCurrentTab(PageUi.getFocus_pagePos());
        
		// scroll to last view
        mHorScrollView.post(new Runnable() {
	        @Override
	        public void run() {
		        mPref_FinalPageViewed = act.getSharedPreferences("focus_view", 0);
		        int scrollX = Pref.getPref_focusView_scrollX_byFolderTableId(act);
	        	mHorScrollView.scrollTo(scrollX, 0);
	            updateTabSpec(mTabsHost.getCurrentTabTag(),act);
	        } 
	    });
        
	}
	
	public static void setAudioPlayingTab_WithHighlight(boolean highlightIsOn)
	{
		// get first tab id and last tab id
		int tabCount = mTabsHost.getTabWidget().getTabCount();
		for (int i = 0; i < tabCount; i++)	
		{
	        TextView textView= (TextView) mTabsHost.getTabWidget().getChildAt(i).findViewById(android.R.id.title);
			if(highlightIsOn && (MainAct.mPlaying_pagePos == i))
			    textView.setTextColor(ColorSet.getHighlightColor(mAct));
			else
			{
		        int style = mDbFolder.getPageStyle(i, true);
			    if((style%2) == 1)
				{	
			        textView.setTextColor(Color.argb(255,0,0,0));
				}
		       	else
		       	{
			        textView.setTextColor(Color.argb(255,255,255,255));
		       	}
			}
		}
	}

	static void setTabMargin(Activity activity)
	{
    	mTabsHost.getTabWidget().setShowDividers(TabWidget.SHOW_DIVIDER_MIDDLE);
        mTabsHost.getTabWidget().setDividerDrawable(R.drawable.ic_tab_divider);
    	
        TabWidget tabWidget = (TabWidget) getRootView().findViewById(android.R.id.tabs);
        
        LinearLayout.LayoutParams tabWidgetLayout;
        for (int j = 0; j < mPagesCount; j++)
        {
        	tabWidgetLayout = (LinearLayout.LayoutParams) tabWidget.getChildAt(j).getLayoutParams();
        	int oriLeftMargin = tabWidgetLayout.leftMargin;
        	int oriRightMargin = tabWidgetLayout.rightMargin;
        	
        	// fix right edge been cut issue when single one note
        	if(mPagesCount == 1)
        		oriRightMargin = 0;
        	
        	if (j == 0) {
        		tabWidgetLayout.setMargins(0, 2, oriRightMargin, 5);
        	} else if (j == (mPagesCount - 1)) {
        		tabWidgetLayout.setMargins(oriLeftMargin, 2, 0, 5);
        	} else {
        		tabWidgetLayout.setMargins(oriLeftMargin, 2, oriRightMargin, 5);
        	}
        }
        tabWidget.requestLayout();
	}
	
	
	/**
	 * set tab change listener
	 * 
	 */
	static String mTabSpec;
	protected static void setTabChangeListener(final Activity activity)
	{
        // set on tab changed listener
	    mTabsHost.setOnTabChangedListener(new OnTabChangeListener()
	    {
			@Override
			public void onTabChanged(String tabSpec)
			{
				System.out.println(mClassName + " / onTabChanged");
				mTabSpec = tabSpec;
				updateTabSpec(tabSpec,activity);
			}
		}
	    );    
	}
	
	static void updateTabSpec(String tabSpec,Activity activity)
	{
//		System.out.println("TabsHost / _updateTabSpec");
		// get scroll X
		int scrollX = mHorScrollView.getScrollX();
		
		//update final page currently viewed: scroll x
        mPref_FinalPageViewed = activity.getSharedPreferences("focus_view", 0);
		Pref.setPref_focusView_scrollX_byFolderTableId(activity, scrollX );
		
    	mDbFolder.open();
		int pagesCount = mDbFolder.getPagesCount(false);
		for(int i=0;i<pagesCount;i++)
		{
			int iTabId = mDbFolder.getPageId(i, false);
			int pageTableId = mDbFolder.getPageTableId(i, false);
			TAB_SPEC = TAB_SPEC_PREFIX.concat(String.valueOf(iTabId)); // TAB_SPEC starts from 1
	    	
			if(TAB_SPEC.equals(tabSpec) )
	    	{
	    		PageUi.setFocus_pagePos(i);
	    		//update final page currently viewed: tab Id
				Pref.setPref_focusView_page_tableId(activity,pageTableId);

				// get current playing page table Id
				mNow_pageTableId = Pref.getPref_focusView_page_tableId(activity);
	    		DB_page.setFocusPage_tableId(pageTableId);
	    		System.out.println(mClassName + " / _updateTabSpec / tabSpec = " + tabSpec);
	    	} 
		}
		mDbFolder.close();
		
    	// set current audio playing tab with highlight
		if( (AudioManager.mMediaPlayer != null) &&
			(AudioManager.getPlayerState() != AudioManager.PLAYER_AT_STOP)&&
		    (MainAct.mPlaying_folderPos == FolderUi.getFocus_folderPos()))
			setAudioPlayingTab_WithHighlight(true);
		else
			setAudioPlayingTab_WithHighlight(false);
	}
	
	/**
	 * set tab Edit listener
	 *
	 */
	protected static void setTabEditListener(final FragmentActivity activity)
	{
	    // set listener for editing tab info
	    int i = 0;
	    while(i < mPagesCount)
		{
			final int tabCursor = i;
			View tabView= mTabsHost.getTabWidget().getChildAt(i);
			
			// on long click listener
			tabView.setOnLongClickListener(new OnLongClickListener() 
	    	{	
				@Override
				public boolean onLongClick(View v) 
				{
					editPageTitle(tabCursor, activity);
					return true;
				}
			});
			i++;
		}
	}
	
	/**
	 * delete page
	 * 
	 */
	public static  void deletePage(int TabId, final FragmentActivity activity)
	{
		mDbFolder.open();
		// check if only one page left
		int pagesCount = mDbFolder.getPagesCount(false);
		if(pagesCount > 0)
		{
			final int tabId =  mDbFolder.getPageId(PageUi.getFocus_pagePos(), false);
			//if current page is the first page and will be delete,
			//try to get next existence of note page
			System.out.println("deletePage / mCurrentTabIndex = " + PageUi.getFocus_pagePos());
			System.out.println("deletePage / mFirstPos_PageId = " + mFirstPos_PageId);
	        if(tabId == mFirstPos_PageId)
	        {
	        	int cGetNextExistIndex = PageUi.getFocus_pagePos() +1;
	        	boolean bGotNext = false;
				while(!bGotNext){
		        	try{
		        	   	mFirstPos_PageId =  mDbFolder.getPageId(cGetNextExistIndex, false);
		        		bGotNext = true;
		        	}catch(Exception e){
    		        	 bGotNext = false;
    		        	 cGetNextExistIndex++;}}		            		        	
	        }
            
	        //change to first existing page
	        int newFirstPageTblId = 0;
	        for(int i=0 ; i<pagesCount; i++)
	        {
	        	if(	mDbFolder.getPageId(i, false)== mFirstPos_PageId)
	        	{
	        		newFirstPageTblId =  mDbFolder.getPageTableId(i, false);
	    			System.out.println("deletePage / newFirstPageTblId = " + newFirstPageTblId);
	        	}
	        }
	        System.out.println("--- after delete / newFirstPageTblId = " + newFirstPageTblId);
			Pref.setPref_focusView_page_tableId(activity, newFirstPageTblId);
		}
//		else
//		{
//             Toast.makeText(activity, R.string.toast_keep_one_page , Toast.LENGTH_SHORT).show();
//             return;
//		}
		mDbFolder.close();
		
		// set scroll X
		int scrollX = 0; //over the last scroll X
        mPref_FinalPageViewed = activity.getSharedPreferences("focus_view", 0);
		Pref.setPref_focusView_scrollX_byFolderTableId(activity, scrollX );
	 	  
		
		// get page table Id for dropping
		int pageTableId = mDbFolder.getPageTableId(PageUi.getFocus_pagePos(), true);
		System.out.println("TabsHost / _deletePage / pageTableId =  " + pageTableId);
		
 	    // delete tab name
		mDbFolder.dropPageTable(pageTableId,true);
		mDbFolder.deletePage(DB_folder.getFocusFolder_tableName(),TabId,true);
		mPagesCount--;
		
		// After Delete page, update highlight tab
    	if(PageUi.getFocus_pagePos() < MainAct.mPlaying_pagePos)
    	{
    		MainAct.mPlaying_pagePos--;
    	}
        else if((PageUi.getFocus_pagePos() == MainAct.mPlaying_pagePos) &&
                (MainAct.mPlaying_folderPos == FolderUi.getFocus_folderPos()))
        {
    		if(AudioManager.mMediaPlayer != null)
    		{
				AudioManager.stopAudioPlayer();
				AudioManager.mAudioPos = 0;
				AudioManager.setPlayerState(AudioManager.PLAYER_AT_STOP);
    		}    		
    	}
    	
    	// update change after deleting tab
		FolderUi.startTabsHostRun();
    	
    	// Note: _onTabChanged will reset scroll X to another value,
    	// so we need to add the following to set scroll X again
        mHorScrollView.post(new Runnable() 
        {
	        @Override
	        public void run() {
	        	mHorScrollView.scrollTo(0, 0);
				Pref.setPref_focusView_scrollX_byFolderTableId(activity, 0 );
	        }
	    });
	}

	/**
	 * edit page title
	 * 
	 */
	public static int mStyle = 0;
	static void editPageTitle(int pageCursor, final FragmentActivity act)
	{
		final int pageId = mDbFolder.getPageId(pageCursor, true);
		mDbFolder.open();
		mPageCursor = mDbFolder.getPageCursor();
		if(mPageCursor.isFirst())
			mFirstPos_PageId = pageId;
		mDbFolder.close();

		// get tab name
		String title = mDbFolder.getPageTitle(pageCursor, true);
		
		if(pageCursor == PageUi.getFocus_pagePos())
		{
	        final EditText editText1 = new EditText(act.getBaseContext());
	        editText1.setText(title);
	        editText1.setSelection(title.length()); // set edit text start position
	        //update tab info
	        Builder builder = new Builder(mTabsHost.getContext());
	        builder.setTitle(R.string.edit_page_tab_title)
	                .setMessage(R.string.edit_page_tab_message)
	                .setView(editText1)   
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
                            Util util = new Util(act);
                            util.vibrate();

                            Builder builder1 = new Builder(mTabsHost.getContext());
                            builder1.setTitle(R.string.confirm_dialog_title)
                            .setMessage(R.string.confirm_dialog_message_page)
                            .setNegativeButton(R.string.confirm_dialog_button_no, new OnClickListener(){
                                @Override
                                public void onClick(DialogInterface dialog1, int which1){
                                    /*nothing to do*/}})
                            .setPositiveButton(R.string.confirm_dialog_button_yes, new OnClickListener(){
                                @Override
                                public void onClick(DialogInterface dialog1, int which1){
                                    deletePage(pageId, act);
                                }})
                            .show();
	                    }
	                })	
	                .setPositiveButton(R.string.edit_page_button_update, new OnClickListener()
	                {   @Override
	                    public void onClick(DialogInterface dialog, int which)
	                    {
	                		// save
        					final int pageId =  mDbFolder.getPageId(PageUi.getFocus_pagePos(), true);
        					final int pageTableId =  mDbFolder.getPageTableId(PageUi.getFocus_pagePos(), true);
        					
	                        int tabStyle = mDbFolder.getPageStyle(PageUi.getFocus_pagePos(), true);
							mDbFolder.updatePage(pageId,
                                                 editText1.getText().toString(),
                                                 pageTableId,
                                                 tabStyle,
                                                 true);
	                        
							// Before _recreate, store latest page number currently viewed
							Pref.setPref_focusView_page_tableId(act, pageTableId);
	                        
							FolderUi.startTabsHostRun();
	                    }
	                })	
	                .setIcon(android.R.drawable.ic_menu_edit);
	        
			        AlertDialog d1 = builder.create();
			        d1.show();
			        // android.R.id.button1 for positive: save
			        ((Button)d1.findViewById(android.R.id.button1))
			        .setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_save, 0, 0, 0);
			        
			        // android.R.id.button2 for negative: color 
			        ((Button)d1.findViewById(android.R.id.button2))
  			        .setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_close_clear_cancel, 0, 0, 0);
			        
			        // android.R.id.button3 for neutral: delete
			        ((Button)d1.findViewById(android.R.id.button3))
			        .setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_delete, 0, 0, 0);
			}
	}
	
    
	static public int getLastPos_pageId()
	{
		return mLastPos_pageId;
	}
	
	static public void setLastPos_pageId(int lastPosPageId)
	{
		mLastPos_pageId = lastPosPageId;
	}
}