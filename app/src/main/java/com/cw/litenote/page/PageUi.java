package com.cw.litenote.page;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.cw.litenote.R;
import com.cw.litenote.db.DB_folder;
import com.cw.litenote.db.DB_page;
import com.cw.litenote.main.MainAct;
import com.cw.litenote.note.Note_addAudio;
import com.cw.litenote.note.Note_addCameraImage;
import com.cw.litenote.note.Note_addCameraVideo;
import com.cw.litenote.note.Note_addNew_option;
import com.cw.litenote.note.Note_addReadyImage;
import com.cw.litenote.note.Note_addReadyVideo;
import com.cw.litenote.note.Note_addText;
import com.cw.litenote.preference.Define;
import com.cw.litenote.util.Util;
import com.cw.litenote.util.image.UtilImage;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;


public class PageUi
{
	public PageUi(){}

	private static AlertDialog dlgAddNew;


    /*
	 * Change Page Color
	 *
	 */
	public static void changePageColor(final Activity act)
	{
		// set color
		final Builder builder = new Builder(act);
		builder.setTitle(R.string.edit_page_color_title)
	    	   .setPositiveButton(R.string.edit_page_button_ignore, new OnClickListener(){
	            	@Override
	                public void onClick(DialogInterface dialog, int which)
	                {/*cancel*/}
	            	});
		// inflate select style layout
		LayoutInflater mInflater= (LayoutInflater) act.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = mInflater.inflate(R.layout.select_style, null);//??? how to set group view?
		RadioGroup RG_view = (RadioGroup)view.findViewById(R.id.radioGroup1);

		Util.setButtonColor((RadioButton)RG_view.findViewById(R.id.radio0),0);
		Util.setButtonColor((RadioButton)RG_view.findViewById(R.id.radio1),1);
		Util.setButtonColor((RadioButton)RG_view.findViewById(R.id.radio2),2);
		Util.setButtonColor((RadioButton)RG_view.findViewById(R.id.radio3),3);
		Util.setButtonColor((RadioButton)RG_view.findViewById(R.id.radio4),4);
		Util.setButtonColor((RadioButton)RG_view.findViewById(R.id.radio5),5);
		Util.setButtonColor((RadioButton)RG_view.findViewById(R.id.radio6),6);
		Util.setButtonColor((RadioButton)RG_view.findViewById(R.id.radio7),7);
		Util.setButtonColor((RadioButton)RG_view.findViewById(R.id.radio8),8);
		Util.setButtonColor((RadioButton)RG_view.findViewById(R.id.radio9),9);

		// set current selection
		for(int i=0;i< Util.getStyleCount();i++)
		{
			if(Util.getCurrentPageStyle(act) == i)
			{
				RadioButton buttton = (RadioButton) RG_view.getChildAt(i);
		    	if(i%2 == 0)
		    		buttton.setButtonDrawable(R.drawable.btn_radio_on_holo_dark);
		    	else
		    		buttton.setButtonDrawable(R.drawable.btn_radio_on_holo_light);
			}
		}

		builder.setView(view);

		RadioGroup radioGroup = (RadioGroup) RG_view.findViewById(R.id.radioGroup1);

		final AlertDialog dlg = builder.create();
	    dlg.show();

		radioGroup.setOnCheckedChangeListener(new OnCheckedChangeListener()
		{
			@Override
			public void onCheckedChanged(RadioGroup RG, int id) {
				DB_folder db = TabsHost.mDbFolder;
				TabsHost.mStyle = RG.indexOfChild(RG.findViewById(id));
				db.updatePage(db.getPageId(TabsHost.mNow_pageId, true),
							  db.getPageTitle(TabsHost.mNow_pageId, true),
							  db.getPageTableId(TabsHost.mNow_pageId, true),
							  TabsHost.mStyle,
                              true);
	 			dlg.dismiss();
	 			TabsHost.updateTabChange(act);
		}});
	}

	/**
	 * shift page right or left
	 *
	 */
	public static void shiftPage(final Activity act)
	{
	    Builder builder = new Builder(act);
	    builder.setTitle(R.string.rearrange_page_title)
	      	   .setMessage(null)
	           .setNegativeButton(R.string.rearrange_page_left, null)
	           .setNeutralButton(R.string.edit_note_button_back, null)
	           .setPositiveButton(R.string.rearrange_page_right,null)
	           .setIcon(R.drawable.ic_dragger_horizontal);
	    final AlertDialog dlg = builder.create();

	    // disable dim background
		dlg.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
		dlg.show();


		final int dividerWidth = act.getResources().getDrawable(R.drawable.ic_tab_divider).getMinimumWidth();

		// Shift to left
	    dlg.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener()
	    {  @Override
	       public void onClick(View v)
	       {
	    		//change to OK
	    		Button mButton=(Button)dlg.findViewById(android.R.id.button3);
		        mButton.setText(R.string.btn_Finish);
		        mButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_finish , 0, 0, 0);

		        int[] leftMargin = {0,0};
		        if(TabsHost.mNow_pageId == 0)
		        	TabsHost.mTabsHost.getTabWidget().getChildAt(0).getLocationInWindow(leftMargin);
		        else
		        	TabsHost.mTabsHost.getTabWidget().getChildAt(TabsHost.mNow_pageId -1).getLocationInWindow(leftMargin);

				int curTabWidth,nextTabWidth;
				curTabWidth = TabsHost.mTabsHost.getTabWidget().getChildAt(TabsHost.mNow_pageId).getWidth();
				if(TabsHost.mNow_pageId == 0)
					nextTabWidth = curTabWidth;
				else
					nextTabWidth = TabsHost.mTabsHost.getTabWidget().getChildAt(TabsHost.mNow_pageId -1).getWidth();

				// when leftmost tab margin over window border
	       		if(leftMargin[0] < 0)
	       			TabsHost.mHorScrollView.scrollBy(- (nextTabWidth + dividerWidth) , 0);

	    		dlg.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
	    	    if(TabsHost.mNow_pageId == 0)
	    	    {
	    	    	Toast.makeText(TabsHost.mTabsHost.getContext(), R.string.toast_leftmost ,Toast.LENGTH_SHORT).show();
	    	    	dlg.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(false);//avoid long time toast
	    	    }
	    	    else
	    	    {
	    	    	Util.setPref_lastTimeView_page_tableId(act, TabsHost.mDbFolder.getPageTableId(TabsHost.mNow_pageId, true));
	    	    	swapPage(TabsHost.mNow_pageId,
	    	    			    TabsHost.mNow_pageId -1);

                    // shift left when audio playing
                    if(MainAct.mPlaying_folderPos == MainAct.mFocus_folderPos) {
                        // target is playing index
                        if (TabsHost.mNow_pageId == MainAct.mPlaying_pageId)
                            MainAct.mPlaying_pageId--;
                        // target is at right side of playing index
                        else if ((TabsHost.mNow_pageId - MainAct.mPlaying_pageId) == 1)
                            MainAct.mPlaying_pageId++;
                    }
					TabsHost.updateTabChange(act);
	    	    }
	       }
	    });

	    // done
	    dlg.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(new View.OnClickListener()
	    {   @Override
	       public void onClick(View v)
	       {
	           dlg.dismiss();
	       }
	    });

	    // Shift to right
	    dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener()
	    {  @Override
	       public void onClick(View v)
	       {
	    		dlg.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(false);

	    		// middle button text: change to OK
	    		Button mButton=(Button)dlg.findViewById(android.R.id.button3);
		        mButton.setText(R.string.btn_Finish);
		        mButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_finish , 0, 0, 0);

		        DB_folder db = TabsHost.mDbFolder;
	    		int count = db.getPagesCount(true);

				int[] rightMargin = {0,0};
				if(TabsHost.mNow_pageId == (count-1))
					TabsHost.mTabsHost.getTabWidget().getChildAt(count-1).getLocationInWindow(rightMargin);
				else
					TabsHost.mTabsHost.getTabWidget().getChildAt(TabsHost.mNow_pageId +1).getLocationInWindow(rightMargin);

				int curTabWidth, nextTabWidth;
				curTabWidth = TabsHost.mTabsHost.getTabWidget().getChildAt(TabsHost.mNow_pageId).getWidth();
				if(TabsHost.mNow_pageId == (count-1))
					nextTabWidth = curTabWidth;
				else
					nextTabWidth = TabsHost.mTabsHost.getTabWidget().getChildAt(TabsHost.mNow_pageId +1).getWidth();

	    		// when rightmost tab margin plus its tab width over screen border
				int screenWidth = UtilImage.getScreenWidth(act);
	    		if( screenWidth <= rightMargin[0] + nextTabWidth )
	    			TabsHost.mHorScrollView.scrollBy(nextTabWidth + dividerWidth, 0);

	    		dlg.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(true);

	   	    	if(TabsHost.mNow_pageId == (count-1))
	   	    	{
	   	    		// end of the right side
	   	    		Toast.makeText(TabsHost.mTabsHost.getContext(),R.string.toast_rightmost,Toast.LENGTH_SHORT).show();
	   	    		dlg.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);//avoid long time toast
	   	    	}
	   	    	else
	   	    	{
	    	    	Util.setPref_lastTimeView_page_tableId(act, db.getPageTableId(TabsHost.mNow_pageId, true));
					swapPage(TabsHost.mNow_pageId, TabsHost.mNow_pageId +1);

                    // shift right when audio playing
                    if(MainAct.mPlaying_folderPos == MainAct.mFocus_folderPos) {
                        // target is playing index
                        if (TabsHost.mNow_pageId == MainAct.mPlaying_pageId)
                            MainAct.mPlaying_pageId++;
                        // target is at left side of plying index
                        else if ((MainAct.mPlaying_pageId - TabsHost.mNow_pageId) == 1)
                            MainAct.mPlaying_pageId--;
                    }
					TabsHost.updateTabChange(act);
	   	    	}
	       }
	    });

	    // android.R.id.button1 for positive: next
	    ((Button)dlg.findViewById(android.R.id.button1))
	              .setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_forward, 0, 0, 0);
	    // android.R.id.button2 for negative: previous
	    ((Button)dlg.findViewById(android.R.id.button2))
	              .setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_back, 0, 0, 0);
	    // android.R.id.button3 for neutral: cancel
	    ((Button)dlg.findViewById(android.R.id.button3))
	              .setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_close_clear_cancel, 0, 0, 0);
	}

	/**
	 * swap page
	 *
	 */
	public static void swapPage(int start, int end)
	{
		System.out.println("MainUi / _swapPage / start = " + start + " , end = " + end);
		DB_folder db = TabsHost.mDbFolder;

        db.open();

        // start
        int startPageId = db.getPageId(start,false);
        String startPageTitle = db.getPageTitle(start,false);
        int startPageTableId = db.getPageTableId(start,false);
        int startPageStyle = db.getPageStyle(start, false);

        // end
        int endPageId = db.getPageId(end,false);
		String endPageTitle = db.getPageTitle(end,false);
		int endPageTableId = db.getPageTableId(end,false);
		int endPageStyle = db.getPageStyle(end, false);

        // swap
		db.updatePage(endPageId,
                      startPageTitle,
			          startPageTableId,
				      startPageStyle,
                      false);

		db.updatePage(startPageId,
					  endPageTitle,
					  endPageTableId,
					  endPageStyle,
                      false);
        db.close();
	}


	/**
	 * Add new page: 1 dialog
	 *
	 */
    static int mAddAt;
    static SharedPreferences mPref_add_new_page_location;
	public static  void addNewPage(final FragmentActivity act, final int newTabId) {
        // get tab name
        String pageName = Define.getTabTitle(act, newTabId);

        // check if name is duplicated
        DB_folder dbFolder = TabsHost.mDbFolder;
        dbFolder.open();
        int pagesCount = dbFolder.getPagesCount(false);

        for (int i = 0; i < pagesCount; i++) {
            String tabTitle = dbFolder.getPageTitle(i, false);
            // new name for differentiation
            if (pageName.equalsIgnoreCase(tabTitle)) {
                pageName = tabTitle.concat("b");
            }
        }
        dbFolder.close();

        // get layout inflater
        View rootView = act.getLayoutInflater().inflate(R.layout.add_new_page, null);
        final EditText editPageName = (EditText) rootView.findViewById(R.id.new_page_name);

        // set cursor
        try {
            Field f = TextView.class.getDeclaredField("mCursorDrawableRes");
            f.setAccessible(true);
            f.set(editPageName, R.drawable.cursor);
        } catch (Exception ignored) {
        }

        // set hint
        editPageName.setHint(pageName);
        editPageName.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    ((EditText) v).setText("");
                    ((EditText) v).setSelection(0);
                }
            }
        });

        // radio buttons
        final RadioGroup mRadioGroup = (RadioGroup) rootView.findViewById(R.id.radioGroup_new_page_at);

        // get new page location option
        mPref_add_new_page_location = act.getSharedPreferences("add_new_page_option", 0);
        if (mPref_add_new_page_location.getString("KEY_ADD_NEW_PAGE_TO", "right").equalsIgnoreCase("left")) {
            mRadioGroup.check(mRadioGroup.getChildAt(0).getId());
            mAddAt = 0;
        } else if (mPref_add_new_page_location.getString("KEY_ADD_NEW_PAGE_TO", "right").equalsIgnoreCase("right")) {
            mRadioGroup.check(mRadioGroup.getChildAt(1).getId());
            mAddAt = 1;
        }

        // update new page location option
        mRadioGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup RG, int id) {
                mAddAt = mRadioGroup.indexOfChild(mRadioGroup.findViewById(id));
                if (mAddAt == 0) {
                    mPref_add_new_page_location.edit().putString("KEY_ADD_NEW_PAGE_TO", "left").apply();
                } else if (mAddAt == 1) {
                    mPref_add_new_page_location.edit().putString("KEY_ADD_NEW_PAGE_TO", "right").apply();
                }
            }
        });

        // set view to dialog
        Builder builder1 = new Builder(act);
        builder1.setView(rootView);
        final AlertDialog dialog1 = builder1.create();
        dialog1.show();

        // cancel button
        Button btnCancel = (Button) rootView.findViewById(R.id.new_page_cancel);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog1.dismiss();
            }
        });

        // add button
        Button btnAdd = (Button) rootView.findViewById(R.id.new_page_add);
        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String pageName;
                if (!Util.isEmptyString(editPageName.getText().toString()))
                    pageName = editPageName.getText().toString();
                else
                    pageName = Define.getTabTitle(act, newTabId);


                if (mAddAt == 0)
                    insertPage_leftmost(act, newTabId, pageName);
                else
                    insertPage_rightmost(act, newTabId, pageName);

                dialog1.dismiss();
            }
        });
    }

	/*
	 * Insert Page to Rightmost
	 * 
	 */
	public static void insertPage_rightmost(final FragmentActivity act, int newTblId, String tabName)
	{
		DB_folder dbFolder = TabsHost.mDbFolder;
	    // insert tab name
		int style = Util.getNewPageStyle(act);
		dbFolder.insertPage(DB_folder.getFocusFolder_tableName(),tabName,newTblId,style );
		
		// insert table for new tab
		dbFolder.insertPageTable(dbFolder,DB_folder.getFocusFolder_tableId(),newTblId, false);
		TabsHost.mPagesCount++;
		
		// commit: final page viewed
		Util.setPref_lastTimeView_page_tableId(act, newTblId);
		
	    // set scroll X
		final int scrollX = (TabsHost.mPagesCount) * 60 * 5; //over the last scroll X
		
		TabsHost.updateTabChange(act);
		
		TabsHost.mHorScrollView.post(new Runnable() {
	        @Override
	        public void run() {
	        	TabsHost.mHorScrollView.scrollTo(scrollX, 0);
	        	Util.setPref_lastTimeView_scrollX_byFolderTableId(act, scrollX );
	        } 
	    });
	}

	/* 
	 * Insert Page to Leftmost
	 * 
	 */
	public static void insertPage_leftmost(final FragmentActivity act, int newTabId, String tabName)//??? why exception
	{
		DB_folder dbFolder = TabsHost.mDbFolder;
		
		
	    // insert tab name
		int style = Util.getNewPageStyle(act);
		dbFolder.insertPage(DB_folder.getFocusFolder_tableName(),tabName, newTabId, style );
		
		// insert table for new tab
		dbFolder.insertPageTable(dbFolder,DB_folder.getFocusFolder_tableId(),newTabId, false);
		TabsHost.mPagesCount++;
		
		//change to leftmost tab Id
		int tabTotalCount = dbFolder.getPagesCount(true);
		for(int i=0;i <(tabTotalCount-1);i++)
		{
			int tabIndex = tabTotalCount -1 -i ;
			swapPage(tabIndex,tabIndex-1);
			updateFinalPageViewed(act);
		}
		
	    // set scroll X
		final int scrollX = 0; // leftmost
		
		// commit: scroll X
		TabsHost.updateTabChange(act);
		
		TabsHost.mHorScrollView.post(new Runnable() {
	        @Override
	        public void run() {
	        	TabsHost.mHorScrollView.scrollTo(scrollX, 0);
	        	Util.setPref_lastTimeView_scrollX_byFolderTableId(act, scrollX );
	        } 
	    });
		
		// update highlight tab
		if(MainAct.mPlaying_folderPos == MainAct.mFocus_folderPos)
			MainAct.mPlaying_pageId++;
	}
	
	
	/*
	 * Update Final page which was viewed last time
	 * 
	 */
	protected static void updateFinalPageViewed(FragmentActivity act)
	{
	    // get final viewed table Id
	    int tableId = Util.getPref_lastTimeView_page_tableId(act);
		DB_page.setFocusPage_tableId(tableId);
	
		DB_folder dbFolder = TabsHost.mDbFolder;
		dbFolder.open();

		// get final view tab index of last time
		for(int i = 0; i<dbFolder.getPagesCount(false); i++)
		{
			if(Integer.valueOf(tableId) == dbFolder.getPageTableId(i, false))
				TabsHost.mFinalPageViewed_pageId = i;	// starts from 0
			
	    	if(	dbFolder.getPageId(i, false)== TabsHost.mFirstExist_PageId)
	    		Util.setPref_lastTimeView_page_tableId(act, dbFolder.getPageTableId(i, false) );
		}
		dbFolder.close();
	}

    public static boolean isSamePageTable()
    {
	    return ( (MainAct.mPlaying_pageTableId == TabsHost.mNow_pageTableId) &&
			     (MainAct.mPlaying_pageId == TabsHost.mNow_pageId) &&
	     	     (MainAct.mPlaying_folderPos == MainAct.mFocus_folderPos)        );
    }
    
}




