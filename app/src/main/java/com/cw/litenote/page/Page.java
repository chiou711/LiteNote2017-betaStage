package com.cw.litenote.page;

import java.util.ArrayList;
import java.util.List;

import com.cw.litenote.R;
import com.cw.litenote.db.DB_drawer;
import com.cw.litenote.db.DB_folder;
import com.cw.litenote.db.DB_page;
import com.cw.litenote.folder.FolderUi;
import com.cw.litenote.operation.audio.AudioInfo;
import com.cw.litenote.operation.audio.AudioPlayer_page;
import com.cw.litenote.tabs.TabsHost;
import com.cw.litenote.main.MainAct;
import com.cw.litenote.note.Note;
import com.cw.litenote.util.audio.UtilAudio;
import com.cw.litenote.note.Note_edit;
import com.cw.litenote.util.ColorSet;
import com.cw.litenote.operation.mail.MailNotes;
import com.cw.litenote.util.uil.UilCommon;
import com.cw.litenote.util.uil.UilListViewBaseFragment;
import com.cw.litenote.util.Util;
import com.cw.litenote.util.preferences.Pref;
import com.mobeta.android.dslv.DragSortController;
import com.mobeta.android.dslv.DragSortListView;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnShowListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class Page extends UilListViewBaseFragment
						  implements LoaderManager.LoaderCallbacks<List<String>> 
{
	private static Cursor mCursor_note;
	public static DB_page mDb_page;
    SharedPreferences mPref_delete_warn;
	public static SharedPreferences mPref_show_note_attribute;
	private List<Boolean> mSelectedList = new ArrayList<>();
	
	// This is the Adapter being used to display the list's data.
	NoteListAdapter mAdapter;
	public static DragSortListView mDndListView;
	private DragSortController mController;
	public static int MOVE_TO = 0;
	public static int COPY_TO = 1;
    public static int mStyle = 0;
	public static FragmentActivity mAct;
	String mClassName;
    public static int mHighlightPosition;
	public static SeekBar seekBarProgress;
	static ProgressBar mSpinner;
    public static int currPlayPosition;

    public Page(){}

	// page
	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);
		System.out.println("Page / _onActivityCreated");
		mAct = getActivity();
		mClassName = getClass().getSimpleName();

		listView = (DragSortListView)getActivity().findViewById(R.id.list1);
		mDndListView = listView;

		if(Build.VERSION.SDK_INT >= 21)
			mDndListView.setSelector(R.drawable.ripple);

	    mFooterMessage = (TextView) mAct.findViewById(R.id.footerText);
		mSpinner = (ProgressBar) getActivity().findViewById(R.id.list1_progress);
		new SpinnerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		//new ProgressBarTask().execute();
		//refer to
		// http://stackoverflow.com/questions/9119627/android-sdk-asynctask-doinbackground-not-running-subclass
		//Behavior of AsyncTask().execute(); has changed through Android versions.
		// -Before Donut (Android:1.6 API:4) tasks were executed serially,
		// -from Donut to Gingerbread (Android:2.3 API:9) tasks executed paralleled;
		// -since Honeycomb (Android:3.0 API:11) execution was switched back to sequential;
		// a new method AsyncTask().executeOnExecutor(Executor) however, was added for parallel execution.

		// show scroll thumb
//		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
//			mDndListView.setFastScrollAlwaysVisible(true);

		mDndListView.setScrollbarFadingEnabled(true);
		mDndListView.setScrollBarStyle(ListView.SCROLLBARS_OUTSIDE_OVERLAY);
		Util.setScrollThumb(getActivity(),mDndListView);

    	mStyle = Util.getCurrentPageStyle();
//    	System.out.println("Page / _onActivityCreated / mStyle = " + mStyle);

    	UilCommon.init();

    	//listener: view note
    	mDndListView.setOnItemClickListener(new OnItemClickListener()
    	{   @Override
			public void onItemClick(AdapterView<?> arg0, View view, int position, long id)
			{
                openClickedItem(position);
			}
        });

    	// listener: edit note
    	mDndListView.setOnItemLongClickListener(new OnItemLongClickListener()
    	{
            public boolean onItemLongClick(AdapterView<?> parent, View v, int position, long id)
             {
                 openLongClickedItem(position);
            	return true;
             }
	    });

        mController = buildController(mDndListView);
        mDndListView.setFloatViewManager(mController);
        mDndListView.setOnTouchListener(mController);
        //called on it but does not override performClick
  		mDndListView.setDragEnabled(true);

		// We have a menu item to show in action bar.
		setHasOptionsMenu(true);

		// Create an empty adapter we will use to display the loaded data.
		mAdapter = new NoteListAdapter(getActivity());

		setListAdapter(mAdapter);

		// Start out with a progress indicator.
		setListShown(true); //set progress indicator

		// Prepare the loader. Either re-connect with an existing one or start a new one.
		getLoaderManager().initLoader(0, null, this);
	}

	// Open clicked item of list view
	static void openClickedItem(int position)
    {
		System.out.println("Page / _openClickedItem");

		currPlayPosition = position;
        mDb_page.open();
        int count = mDb_page.getNotesCount(false);
        String linkStr = mDb_page.getNoteLinkUri(position,false);
        mDb_page.close();

        if(position < count) {

            SharedPreferences pref_open_youtube;
            pref_open_youtube = mAct.getSharedPreferences("show_note_attribute", 0);

            if( Util.isYouTubeLink(linkStr) &&
                    pref_open_youtube.getString("KEY_VIEW_NOTE_LAUNCH_YOUTUBE", "no").equalsIgnoreCase("yes") )
            {
                AudioInfo.stopAudioPlayer();

                // apply native YouTube
                Util.openLink_YouTube(mAct, linkStr);
            }
            else
            {
                // apply Note class
                Intent intent;
                intent = new Intent(mAct, Note.class);
                intent.putExtra("POSITION", position);
                mAct.startActivity(intent);
            }
        }
    }

    // Open long clicked item of list view
    void openLongClickedItem(int position)
    {
        Intent i = new Intent(getActivity(), Note_edit.class);
        Long rowId = mDb_page.getNoteId(position,true);
        i.putExtra("list_view_position", position);
        i.putExtra(DB_page.KEY_NOTE_ID, rowId);
        i.putExtra(DB_page.KEY_NOTE_TITLE, mDb_page.getNoteTitle_byId(rowId));
        i.putExtra(DB_page.KEY_NOTE_PICTURE_URI , mDb_page.getNotePictureUri_byId(rowId));
        i.putExtra(DB_page.KEY_NOTE_AUDIO_URI , mDb_page.getNoteAudioUri_byId(rowId));
        i.putExtra(DB_page.KEY_NOTE_LINK_URI , mDb_page.getNoteLinkUri_byId(rowId));
        i.putExtra(DB_page.KEY_NOTE_BODY, mDb_page.getNoteBody_byId(rowId));
        i.putExtra(DB_page.KEY_NOTE_CREATED, mDb_page.getNoteCreatedTime_byId(rowId));
        startActivity(i);
    }

	private class SpinnerTask extends AsyncTask <Void,Void,Void>{
	    @Override
	    protected void onPreExecute(){
			mDndListView.setVisibility(View.GONE);
			mFooterMessage.setVisibility(View.GONE);
			mSpinner.setVisibility(View.VISIBLE);
	    }

	    @Override
	    protected Void doInBackground(Void... arg0) {
			return null;   
	    }

	    @Override
	    protected void onPostExecute(Void result) {
	    	mSpinner.setVisibility(View.GONE);
			mDndListView.setVisibility(View.VISIBLE);
			mFooterMessage.setVisibility(View.VISIBLE);
			if(!this.isCancelled())
			{
				this.cancel(true);
			}
	    }
	}
	
    // list view listener: on drag
    private DragSortListView.DragListener onDrag = new DragSortListView.DragListener()
    {
                @Override
                public void drag(int startPosition, int endPosition) {
                	//add highlight boarder
//                    View v = mDndListView.mFloatView;
//                    v.setBackgroundColor(Color.rgb(255,128,0));
//                	v.setBackgroundResource(R.drawable.listview_item_shape_dragging);
//                    v.setPadding(0, 4, 0,4);
                }
    };
	
    // list view listener: on drop
    private DragSortListView.DropListener onDrop = new DragSortListView.DropListener() 
    {
        @Override
        public void drop(int startPosition, int endPosition) {

        	int oriStartPos = startPosition;
        	int oriEndPos = endPosition;
        	
			if(startPosition >= mDb_page.getNotesCount(true)) // avoid footer error
				return;

			mSelectedList.set(startPosition, true);
			mSelectedList.set(endPosition, true);
			
			
			//reorder data base storage
			int loop = Math.abs(startPosition-endPosition);
			for(int i=0;i< loop;i++)
			{
				swapRows(Page.mDb_page, startPosition,endPosition);
				if((startPosition-endPosition) >0)
					endPosition++;
				else
					endPosition--;
			}
			
			if( PageUi.isSamePageTable() &&
	     		(AudioInfo.mMediaPlayer != null)				   )
			{
				if( (mHighlightPosition == oriEndPos)  && (oriStartPos > oriEndPos))      
				{
					mHighlightPosition = oriEndPos+1;
				}
				else if( (mHighlightPosition == oriEndPos) && (oriStartPos < oriEndPos))
				{
					mHighlightPosition = oriEndPos-1;
				}
				else if( (mHighlightPosition == oriStartPos)  && (oriStartPos > oriEndPos))      
				{
					mHighlightPosition = oriEndPos;
				}
				else if( (mHighlightPosition == oriStartPos) && (oriStartPos < oriEndPos))
				{
					mHighlightPosition = oriEndPos;
				}				
				else if(  (mHighlightPosition < oriEndPos) && 
						  (mHighlightPosition > oriStartPos)   )    
				{
					mHighlightPosition--;
				}
				else if( (mHighlightPosition > oriEndPos) && 
						 (mHighlightPosition < oriStartPos)  )
				{
					mHighlightPosition++;
				}

				AudioInfo.mAudioPos = mHighlightPosition;
				AudioPlayer_page.prepareAudioInfo();
			}
			mItemAdapter.notifyDataSetChanged();
			showFooter();
        }
    };
	
    /**
     * Called in onCreateView. Override this to provide a custom
     * DragSortController.
     */
    public DragSortController buildController(DragSortListView dslv)
    {
        // defaults are
        DragSortController controller = new DragSortController(dslv);
        controller.setSortEnabled(true);
        
        //drag
	  	mPref_show_note_attribute = getActivity().getSharedPreferences("show_note_attribute", 0);
	  	if(mPref_show_note_attribute.getString("KEY_ENABLE_DRAGGABLE", "no").equalsIgnoreCase("yes"))
	  		controller.setDragInitMode(DragSortController.ON_DOWN); // click
	  	else
	        controller.setDragInitMode(DragSortController.MISS); 

	  	controller.setDragHandleId(R.id.img_dragger);// handler
//        controller.setDragInitMode(DragSortController.ON_LONG_PRESS); //long click to drag
	  	controller.setBackgroundColor(Color.argb(128,128,64,0));// background color when dragging
//        controller.setBackgroundColor(Util.mBG_ColorArray[mStyle]);// background color when dragging
        
	  	// mark
        controller.setMarkEnabled(true);
        controller.setClickMarkId(R.id.img_check);
        controller.setMarkMode(DragSortController.ON_DOWN);
        // audio
        controller.setAudioEnabled(true);
//        controller.setClickAudioId(R.id.img_audio);
        controller.setClickAudioId(R.id.audio_block);
        controller.setAudioMode(DragSortController.ON_DOWN);

        return controller;
    }        

    @Override
    public void onResume() {
    	super.onResume();
		mDb_page = new DB_page(getActivity(), Pref.getPref_focusView_page_tableId(getActivity()));
    	System.out.println(mClassName + " / _onResume");

        // recover scroll Y
        mFirstVisibleIndex = Pref.getPref_focusView_list_view_first_visible_index(getActivity());
        mFirstVisibleIndexTop = Pref.getPref_focusView_list_view_first_visible_index_top(getActivity());

		// for incoming phone call case or key protection off to on
		if( (page_audio != null) &&
				(AudioInfo.getPlayerState() != AudioInfo.PLAYER_AT_STOP) &&
				(AudioInfo.getAudioPlayMode() == AudioInfo.CONTINUE_MODE)   )
		{
			page_audio.initAudioBlock();
		}
    }

    @Override
    public void onPause() {
    	super.onPause();
    	System.out.println("Page / _onPause");

		if( (AudioInfo.mMediaPlayer != null) &&
			(AudioInfo.getPlayerState() != AudioInfo.PLAYER_AT_STOP))
		{
            if((page_audio != null) && (page_audio.audio_panel!=null))
                page_audio.audio_panel.setVisibility(View.GONE);
		}
	 }
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
    	super.onSaveInstanceState(outState);
    	System.out.println(mClassName + " / onSaveInstanceState");
    }
    
	@Override
	public Loader<List<String>> onCreateLoader(int id, Bundle args) 
	{
		// This is called when a new Loader needs to be created. 
		return new NoteListLoader(getActivity());
	}

	@Override
	public void onLoadFinished(Loader<List<String>> loader,
							   List<String> data) 
	{
		System.out.println("Page / _onLoadFinished");
		// Set the new data in the adapter.
		mAdapter.setData(data);

		// The list should now be shown.
		if (isResumed()) 
			setListShown(true);
		else 
			setListShownNoAnimation(true);
		
		fillData();
		
		getLoaderManager().destroyLoader(0); // add for fixing callback twice
	}
	
	@Override
	public void onLoaderReset(Loader<List<String>> loader) {
		// Clear the data in the adapter.
		mAdapter.setData(null);
	}

    int mFirstVisibleIndex;
	int mFirstVisibleIndexTop;
	/**
	 * fill data
	 */
	public static Page_adapter mItemAdapter;
    public void fillData()
    {
    	System.out.println("Page / _fillData");
    	
    	// save index and top position
//    	int index = mDndListView.getFirstVisiblePosition();
//      View v = mDndListView.getChildAt(0);
//      int top = (v == null) ? 0 : v.getTop();

    	/*
        // set background color of list view
        mDndListView.setBackgroundColor(Util.mBG_ColorArray[mStyle]);

    	//show divider color
        if(mStyle%2 == 0)
	    	mDndListView.setDivider(new ColorDrawable(0xFFffffff));//for dark
        else
          mDndListView.setDivider(new ColorDrawable(0xff000000));//for light

        mDndListView.setDividerHeight(3);
        */

    	mDb_page.open();
		mCursor_note = DB_page.mCursor_note;
        int count = mDb_page.getNotesCount(false);
        mDb_page.close();
        
        // set adapter
        String[] from = new String[] { DB_page.KEY_NOTE_TITLE};
        int[] to = new int[] { R.id.row_whole};
        mItemAdapter = new Page_adapter(
				getActivity(),
				R.layout.page_view_row,
				mCursor_note,
				from,
				to,
				0
				);
        
        mDndListView.setAdapter(mItemAdapter);
        
		// selected list
		for(int i=0; i< count ; i++ )
		{
			mSelectedList.add(true);
			mSelectedList.set(i,true);
		}

        System.out.println("Page / _fillData / mFirstVisibleIndex = " + mFirstVisibleIndex +
                                           " , mFirstVisibleIndexTop = " + mFirstVisibleIndexTop);
        // restore index and top position
        mDndListView.setSelectionFromTop(mFirstVisibleIndex, mFirstVisibleIndexTop);
        
        mDndListView.setDropListener(onDrop);
        mDndListView.setDragListener(onDrag);
        mDndListView.setMarkListener(onMark);
        mDndListView.setAudioListener(onAudio);
		mDndListView.setOnScrollListener(onScroll);

        showFooter();

		// scroll highlight audio item to be visible
		if((AudioInfo.getPlayerState() != AudioInfo.PLAYER_AT_STOP) && (!Page.isOnAudioClick))
			AudioPlayer_page.scrollHighlightAudioItemToVisible();
    }

    OnScrollListener onScroll = new OnScrollListener() {
		
		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState) {
	        mFirstVisibleIndex = mDndListView.getFirstVisiblePosition();
	        View v = mDndListView.getChildAt(0);
	        mFirstVisibleIndexTop = (v == null) ? 0 : v.getTop();

			if( (PageUi.getFocus_pagePos() == MainAct.mPlaying_pagePos)&&
				(MainAct.mPlaying_folderPos == FolderUi.getFocus_folderPos()) &&
				(AudioInfo.getPlayerState() == AudioInfo.PLAYER_AT_PLAY) &&
				(Page.mDndListView.getChildAt(0) != null)                    )
			{
				// do nothing when playing audio
				System.out.println("_onScrollStateChanged / do nothing");
			}
			else
            {
				// keep index and top position
				Pref.setPref_focusView_list_view_first_visible_index(getActivity(), mFirstVisibleIndex);
				Pref.setPref_focusView_list_view_first_visible_index_top(getActivity(), mFirstVisibleIndexTop);
			}
		}
		
		@Override
		public void onScroll(AbsListView view, int firstVisibleItem,
				int visibleItemCount, int totalItemCount) {
			
//			System.out.println("_onScroll / firstVisibleItem " + firstVisibleItem);
//			System.out.println("_onScroll / visibleItemCount " + visibleItemCount);
//			System.out.println("_onScroll / totalItemCount " + totalItemCount);

		}
	};

	
	
    // swap rows
	protected static void swapRows(DB_page dB_page, int startPosition, int endPosition)
	{
		Long mNoteNumber1;
		String mNoteTitle1;
		String mNotePictureUri1;
		String mNoteAudioUri1;
		String mNoteLinkUri1;
		String mNoteBodyString1;
		int mMarkingIndex1;
		Long mCreateTime1;
		Long mNoteNumber2 ;
		String mNotePictureUri2;
		String mNoteAudioUri2;
		String mNoteLinkUri2;
		String mNoteTitle2;
		String mNoteBodyString2;
		int mMarkingIndex2;
		Long mCreateTime2;

		dB_page.open();
		mNoteNumber1 = dB_page.getNoteId(startPosition,false);
        mNoteTitle1 = dB_page.getNoteTitle(startPosition,false);
        mNotePictureUri1 = dB_page.getNotePictureUri(startPosition,false);
        mNoteAudioUri1 = dB_page.getNoteAudioUri(startPosition,false);
        mNoteLinkUri1 = dB_page.getNoteLinkUri(startPosition,false);
        mNoteBodyString1 = dB_page.getNoteBody(startPosition,false);
        mMarkingIndex1 = dB_page.getNoteMarking(startPosition,false);
    	mCreateTime1 = dB_page.getNoteCreatedTime(startPosition,false);

		mNoteNumber2 = dB_page.getNoteId(endPosition,false);
        mNoteTitle2 = dB_page.getNoteTitle(endPosition,false);
        mNotePictureUri2 = dB_page.getNotePictureUri(endPosition,false);
        mNoteAudioUri2 = dB_page.getNoteAudioUri(endPosition,false);
        mNoteLinkUri2 = dB_page.getNoteLinkUri(endPosition,false);
        mNoteBodyString2 = dB_page.getNoteBody(endPosition,false);
        mMarkingIndex2 = dB_page.getNoteMarking(endPosition,false);
    	mCreateTime2 = dB_page.getNoteCreatedTime(endPosition,false);

        dB_page.updateNote(mNoteNumber2,
				 mNoteTitle1,
				 mNotePictureUri1,
				 mNoteAudioUri1, 
				 "",
				 mNoteLinkUri1,
				 mNoteBodyString1,
				 mMarkingIndex1,
				 mCreateTime1,false);		        
		
		dB_page.updateNote(mNoteNumber1,
		 		 mNoteTitle2,
		 		 mNotePictureUri2,
		 		 mNoteAudioUri2, 
				 "",
				 mNoteLinkUri2,
		 		 mNoteBodyString2,
		 		 mMarkingIndex2,
		 		 mCreateTime2,false);

		dB_page.close();
	}

    // list view listener: on mark
    private DragSortListView.MarkListener onMark =
    new DragSortListView.MarkListener() 
	{   @Override
        public void mark(int position) 
		{
			System.out.println("Page / _onMark");

            // toggle marking
			int markingNow = toggleNoteMarking(position);

            // Stop if unmarked item is at playing state
            if(AudioInfo.mAudioPos == position) {
				UtilAudio.stopAudioIfNeeded();
				if(markingNow == 0)
					TabsHost.setAudioPlayingTab_WithHighlight(false);
			}

			// update list view
            mItemAdapter.notifyDataSetChanged(); //note: add this can avoid conflict of onMark and onItemClick

			// update footer
            showFooter();

			// update audio info
            if(PageUi.isSamePageTable())
            	AudioPlayer_page.prepareAudioInfo();
        }
    };    

	// toggle mark of note
	public static int toggleNoteMarking(int position)
	{
		int marking = 0;
		mDb_page.open();
		int count = mDb_page.getNotesCount(false);
		if(position >= count) //end of list
		{
			mDb_page.close();
			return marking;
		}

		String strNote = mDb_page.getNoteTitle(position,false);
		String strPictureUri = mDb_page.getNotePictureUri(position,false);
		String strAudioUri = mDb_page.getNoteAudioUri(position,false);
		String strLinkUri = mDb_page.getNoteLinkUri(position,false);
		String strNoteBody = mDb_page.getNoteBody(position,false);
		Long idNote =  mDb_page.getNoteId(position,false);

		// toggle the marking
		if(mDb_page.getNoteMarking(position,false) == 0) {
			mDb_page.updateNote(idNote, strNote, strPictureUri, strAudioUri, "", strLinkUri, strNoteBody, 1, 0, false);
			marking = 1;
		}
		else {
			mDb_page.updateNote(idNote, strNote, strPictureUri, strAudioUri, "", strLinkUri, strNoteBody, 0, 0, false);
			marking = 0;
		}

		mDb_page.close();
		return  marking;
	}


	public static boolean isOnAudioClick;
	AudioPlayer_page audioPlayer_page;
	public static Page_audio page_audio;//todo static issue
    // list view listener: on audio
    private DragSortListView.AudioListener onAudio = new DragSortListView.AudioListener() 
	{   @Override
        public void audio(int position) 
		{
			System.out.println("Page / _onAudio");
			AudioInfo.setAudioPlayMode(AudioInfo.CONTINUE_MODE);

			int notesCount = mDb_page.getNotesCount(true);
            if(position >= notesCount) //end of list
            	return ;

			int marking = mDb_page.getNoteMarking(position,true);
    		String uriString = mDb_page.getNoteAudioUri(position,true);

    		boolean isAudioUri = false;
    		if( !Util.isEmptyString(uriString) && (marking == 1))
    			isAudioUri = true;

    		System.out.println("Page / _onAudio / isAudioUri = " + isAudioUri);

            if(position < notesCount) // avoid footer error
			{
				if(isAudioUri)
				{
					// cancel playing
					if(AudioInfo.mMediaPlayer != null)
					{
						if(AudioInfo.mMediaPlayer.isPlaying())
							AudioInfo.mMediaPlayer.pause();

		   			   	if(audioPlayer_page != null) {
							AudioPlayer_page.mAudioHandler.removeCallbacks(audioPlayer_page.mRunContinueMode);
                        }
						AudioInfo.mMediaPlayer.release();
						AudioInfo.mMediaPlayer = null;
					}

					isOnAudioClick = true;
					AudioInfo.setPlayerState(AudioInfo.PLAYER_AT_PLAY);

					// create new Intent to play audio
					AudioInfo.mAudioPos = position;

                    page_audio = new Page_audio(mAct);
                    page_audio.initAudioBlock();

                    audioPlayer_page = new AudioPlayer_page(mAct,page_audio);
					AudioPlayer_page.prepareAudioInfo();
					audioPlayer_page.runAudioState();

                    // update playing page position
                    MainAct.mPlaying_pagePos = PageUi.getFocus_pagePos();
					// update playing page table Id
					MainAct.mPlaying_pageTableId = TabsHost.mNow_pageTableId;

					// update playing folder position
				    MainAct.mPlaying_folderPos = FolderUi.getFocus_folderPos();
				    // update playing folder table Id
					DB_drawer dB_drawer = new DB_drawer(mAct);
					MainAct.mPlaying_folderTableId = dB_drawer.getFolderTableId(MainAct.mPlaying_folderPos,true);
					
		            mItemAdapter.notifyDataSetChanged();
				}
			}
        }
	};            

    static TextView mFooterMessage;

	// set footer
    public void showFooter()
    {
    	System.out.println("Page / _showFooter ");

		// show footer
        mFooterMessage.setTextColor(ColorSet.color_white);
        if(mFooterMessage != null) //add this for avoiding null exception when after e-Mail action
        {
            mFooterMessage.setText(getFooterMessage());
            mFooterMessage.setBackgroundColor(ColorSet.getBarColor(mAct));
        }
    }

	// get footer message of list view
    static String getFooterMessage()
    {
        return mAct.getResources().getText(R.string.footer_checked).toString() +
               "/" +
               mAct.getResources().getText(R.string.footer_total).toString() +
                  ": " +
               mDb_page.getCheckedNotesCount() +
                  "/" +
               mDb_page.getNotesCount(true);
    }

	/*******************************************
	 * 					menu
	 *******************************************/
    // Menu identifiers
    static final int CHECK_ALL = R.id.CHECK_ALL;
    static final int UNCHECK_ALL = R.id.UNCHECK_ALL;
    static final int INVERT_SELECTED = R.id.INVERT_SELECTED;
    static final int MOVE_CHECKED_NOTE = R.id.MOVE_CHECKED_NOTE;
    static final int COPY_CHECKED_NOTE = R.id.COPY_CHECKED_NOTE;
    static final int MAIL_CHECKED_NOTE = R.id.MAIL_CHECKED_NOTE;
    static final int DELETE_CHECKED_NOTE = R.id.DELETE_CHECKED_NOTE;
    
    @Override public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) 
        {
	        case CHECK_ALL:
	        	checkAll(1); 
	            return true;
	        case UNCHECK_ALL:
	        	checkAll(0); 
	            return true;
	        case INVERT_SELECTED:
	        	invertSelected(); 
	            return true;
	        case MOVE_CHECKED_NOTE:
	        case COPY_CHECKED_NOTE:
	    		if(!noItemChecked())
	        	{
	    			int count = mDb_page.getCheckedNotesCount();
		    		String copyItems[] = new String[count];
		    		String copyItemsPicture[] = new String[count];
		    		String copyItemsLink[] = new String[count];
		    		String copyItemsAudio[] = new String[count];
		    		String copyItemsBody[] = new String[count];
		    		Long copyItemsTime[] = new Long[count];
		    		int cCopy = 0;
		    		
		    		mDb_page.open();
		    		int noteCount = mDb_page.getNotesCount(false);
		    		for(int i=0; i<noteCount; i++)
		    		{
		    			if(mDb_page.getNoteMarking(i,false) == 1)
		    			{
		    				copyItems[cCopy] = mDb_page.getNoteTitle(i,false);
		    				copyItemsPicture[cCopy] = mDb_page.getNotePictureUri(i,false);
		    				copyItemsLink[cCopy] = mDb_page.getNoteLinkUri(i,false);
		    				copyItemsAudio[cCopy] = mDb_page.getNoteAudioUri(i,false);
		    				copyItemsBody[cCopy] = mDb_page.getNoteBody(i,false);
		    				copyItemsTime[cCopy] = mDb_page.getNoteCreatedTime(i,false);
		    				cCopy++;
		    			}
		    		}
		    		mDb_page.close();
		           
		    		if(item.getItemId() == MOVE_CHECKED_NOTE)
		    			operateCheckedTo(mAct,copyItems, copyItemsPicture, copyItemsLink, copyItemsAudio, copyItemsBody, copyItemsTime, MOVE_TO); // move to
		    		else if(item.getItemId() == COPY_CHECKED_NOTE)
			    		operateCheckedTo(mAct,copyItems, copyItemsPicture, copyItemsLink, copyItemsAudio, copyItemsBody, copyItemsTime, COPY_TO);// copy to
		    			
	        	}
	        	else
	    			Toast.makeText(getActivity(),
							   R.string.delete_checked_no_checked_items,
							   Toast.LENGTH_SHORT)
					     .show();
	            return true;
	            
	        case MAIL_CHECKED_NOTE:
	    		if(!noItemChecked())
	        	{
		        	// set Sent string Id
					List<Long> noteIdArray = new ArrayList<>();
					List<String> pictureFileNameList = new ArrayList<>();
	            	int j=0;
	            	mDb_page.open();
	            	int count = mDb_page.getNotesCount(false);
		    		for(int i=0; i<count; i++)
		    		{
		    			if(mDb_page.getNoteMarking(i,false) == 1)
		    			{
		    				noteIdArray.add(j, mDb_page.getNoteId(i,false));
		    				j++;
		    				
		    				String picFile = mDb_page.getNotePictureUri_byId(mDb_page.getNoteId(i,false),false,false);
		    				if((picFile != null) && (picFile.length() > 0))
		    					pictureFileNameList.add(picFile);
		    			}
		    		}
		    		mDb_page.close();

					// message
					String sentString = Util.getStringWithXmlTag(noteIdArray);
					sentString = Util.addXmlTag(sentString);

		    		// picture array
		    		int cnt = pictureFileNameList.size();
		    		String pictureFileNameArr[] = new String[cnt];
		    		for(int i=0; i < cnt ; i++ )
		    		{
		    			pictureFileNameArr[i] = pictureFileNameList.get(i);
		    		}
					new MailNotes(mAct,sentString,pictureFileNameArr);
	        	}
	        	else
	    			Toast.makeText(getActivity(),
							   R.string.delete_checked_no_checked_items,
							   Toast.LENGTH_SHORT)
						 .show();
	        	return true;
	        	
	        case DELETE_CHECKED_NOTE:
	        	if(!noItemChecked())
	        		deleteCheckedNotes();
	        	else
	    			Toast.makeText(getActivity(),
	    						   R.string.delete_checked_no_checked_items,
	    						   Toast.LENGTH_SHORT)
	    				 .show();
	            return true;     

            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
	
	static public void swap(DB_page dB_page)
	{
        int startCursor = dB_page.getNotesCount(true)-1;
        int endCursor = 0;
		
		//reorder data base storage for ADD_NEW_TO_TOP option
		int loop = Math.abs(startCursor-endCursor);
		for(int i=0;i< loop;i++)
		{
			swapRows(dB_page, startCursor,endCursor);
			if((startCursor-endCursor) >0)
				endCursor++;
			else
				endCursor--;
		}
	}
    
	/**
	 *  check all or uncheck all
	 */
	public void checkAll(int action) 
	{
		boolean bStopAudio = false;
		mDb_page.open();
		int count = mDb_page.getNotesCount(false);
		for(int i=0; i<count; i++)
		{
			Long rowId = mDb_page.getNoteId(i,false);
			String noteTitle = mDb_page.getNoteTitle(i,false);
			String pictureUri = mDb_page.getNotePictureUri(i,false);
			String audioUri = mDb_page.getNoteAudioUri(i,false);
			String linkUri = mDb_page.getNoteLinkUri(i,false);
			String noteBody = mDb_page.getNoteBody(i,false);
			mDb_page.updateNote(rowId, noteTitle, pictureUri, audioUri, "", linkUri, noteBody , action, 0,false);// action 1:check all, 0:uncheck all
	        // Stop if unmarked item is at playing state
	        if((AudioInfo.mAudioPos == i) && (action == 0) )
	        	bStopAudio = true;		
		}
		mDb_page.close();
		
		if(bStopAudio)
			UtilAudio.stopAudioIfNeeded();	
		
		// update audio play list
        if(PageUi.isSamePageTable())
        	AudioPlayer_page.prepareAudioInfo();
        
		mItemAdapter.notifyDataSetChanged();
		showFooter();
	}
	
	/**
	 *  Invert Selected
	 */
	public void invertSelected() 
	{
		boolean bStopAudio = false;
		mDb_page.open();
		int count = mDb_page.getNotesCount(false);
		for(int i=0; i<count; i++)
		{
			Long rowId = mDb_page.getNoteId(i,false);
			String noteTitle = mDb_page.getNoteTitle(i,false);
			String pictureUri = mDb_page.getNotePictureUri(i,false);
			String audioUri = mDb_page.getNoteAudioUri(i,false);
			String linkUri = mDb_page.getNoteLinkUri(i,false);
			String noteBody = mDb_page.getNoteBody(i,false);
			long marking = (mDb_page.getNoteMarking(i,false)==1)?0:1;
			mDb_page.updateNote(rowId, noteTitle, pictureUri, audioUri, "", linkUri, noteBody , marking, 0,false);// action 1:check all, 0:uncheck all
	        // Stop if unmarked item is at playing state
	        if((AudioInfo.mAudioPos == i) && (marking == 0) )
	        	bStopAudio = true;			
		}
		mDb_page.close();
		
		if(bStopAudio)
			UtilAudio.stopAudioIfNeeded();	
		
		// update audio play list
        if(PageUi.isSamePageTable())
        	AudioPlayer_page.prepareAudioInfo();
		
		mItemAdapter.notifyDataSetChanged();
		showFooter();
	}	
	
	
    /**
     *   operate checked to: move to, copy to
     * 
     */
	void operateCheckedTo(FragmentActivity act,final String[] copyItems, final String[] copyItemsPicture, final String[] copyItemsLink,
						  final String[] copyItemsAudio, final String[] copyItemsBody,
						  final Long[] copyItemsTime, final int action)
	{
		//list all pages
		int focusFolder_tableId = Pref.getPref_focusView_folder_tableId(act);
		DB_folder db_folder = new DB_folder(act, focusFolder_tableId);
		db_folder.open();
		int tabCount = db_folder.getPagesCount(false);
		final String[] pageNames = new String[tabCount];
		final int[] pageTableIds = new int[tabCount];
		for(int i=0;i<tabCount;i++)
		{
			pageNames[i] = db_folder.getPageTitle(i,false);
			pageTableIds[i] = db_folder.getPageTableId(i,false);
		}
		db_folder.close();
		
		pageNames[PageUi.getFocus_pagePos()] = pageNames[PageUi.getFocus_pagePos()] + " *"; // add mark to current page
		   
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which) {
				
				//keep focus page table id
				int srcPageTableId = DB_page.getFocusPage_tableId();

				//copy checked item to destination page
				int destPageTableId = pageTableIds[which];
				DB_page.setFocusPage_tableId(destPageTableId);
				for(int i=0;i< copyItems.length;i++)
				{
					int marking = 0;
					// default marking of picture or audio is 1
					if( (!Util.isEmptyString(copyItemsPicture[i])) || (!Util.isEmptyString(copyItemsAudio[i])))
						marking = 1;

					// move to same page is not allowed
					if(!((action == MOVE_TO) && (srcPageTableId == destPageTableId)))
						mDb_page.insertNote(copyItems[i],copyItemsPicture[i], copyItemsAudio[i], "", copyItemsLink[i], copyItemsBody[i],marking, copyItemsTime[i]);
				}
				
				//recover table Id of original page
				if((action == MOVE_TO) && (srcPageTableId != destPageTableId))
				{
					DB_page.setFocusPage_tableId(srcPageTableId);
					mDb_page.open();
					int count = mDb_page.getNotesCount(false);

                    //delete checked items that were moved
					for(int i=0; i<count; i++)
					{
						if(mDb_page.getNoteMarking(i,false) == 1)
						{
							mDb_page.deleteNote(mDb_page.getNoteId(i,false),false);
							// update playing highlight
							UtilAudio.stopAudioIfNeeded();
						}
					}
					mDb_page.close();
					
					mItemAdapter.notifyDataSetChanged();
					showFooter();
				}
				else if(action == COPY_TO)
				{
					DB_page.setFocusPage_tableId(srcPageTableId);
					if(destPageTableId == srcPageTableId)
					{
						mItemAdapter.notifyDataSetChanged();
						showFooter();
					}
				}
				
				dialog.dismiss();
			}
		};
		
		if(action == MOVE_TO)
			builder.setTitle(R.string.checked_notes_move_to_dlg);
		else if(action == COPY_TO)
			builder.setTitle(R.string.checked_notes_copy_to_dlg);
		
		builder.setSingleChoiceItems(pageNames, -1, listener)
		  	.setNegativeButton(R.string.btn_Cancel, null);
		
		// override onShow to mark current page status
		AlertDialog alertDlg = builder.create();
		alertDlg.setOnShowListener(new OnShowListener() {
			@Override
			public void onShow(DialogInterface dlgInterface) {
				// add mark for current page
				Util util = new Util(getActivity());
				util.addMarkToCurrentPage(dlgInterface,action);
			}
		});
		alertDlg.show();
	}
	
	
	/**
	 * delete checked notes
	 */
	public void deleteCheckedNotes()
	{
		final Context context = getActivity();

		mPref_delete_warn = context.getSharedPreferences("delete_warn", 0);
    	if(mPref_delete_warn.getString("KEY_DELETE_WARN_MAIN","enable").equalsIgnoreCase("enable") &&
           mPref_delete_warn.getString("KEY_DELETE_CHECKED_WARN","yes").equalsIgnoreCase("yes"))
    	{
			Util util = new Util(getActivity());
			util.vibrate();
    		
    		// show warning dialog
			Builder builder = new Builder(context);
			builder.setTitle(R.string.delete_checked_note_title)
					.setMessage(R.string.delete_checked_message)
					.setNegativeButton(R.string.btn_Cancel, 
							new OnClickListener() 
					{	@Override
						public void onClick(DialogInterface dialog, int which) 
						{/*cancel*/} })
					.setPositiveButton(R.string.btn_OK, 
							new OnClickListener() 
					{	@Override
						public void onClick(DialogInterface dialog, int which) 
						{
							mDb_page.open();
							int count = mDb_page.getNotesCount(false);
							for(int i=0; i<count; i++)
							{
								if(mDb_page.getNoteMarking(i,false) == 1)
									mDb_page.deleteNote(mDb_page.getNoteId(i,false),false);
							}
							mDb_page.close();
							
							// Stop Play/Pause if current tab's item is played and is not at Stop state
							if(AudioInfo.mAudioPos == Page.mHighlightPosition)
								UtilAudio.stopAudioIfNeeded();
							
							mItemAdapter.notifyDataSetChanged();
							showFooter();
						}
					});
			
	        AlertDialog d = builder.create();
	        d.show();
    	}
    	else
    	{
    		// not show warning dialog
    		mDb_page.open();
    		int count = mDb_page.getNotesCount(false);
			for(int i=0; i<count; i++)
			{
				if(mDb_page.getNoteMarking(i,false) == 1)
					mDb_page.deleteNote(mDb_page.getNoteId(i,false),false);
			}
			mDb_page.close();
			
			mItemAdapter.notifyDataSetChanged();
			showFooter();
    	}
	}
    
	@Override
	public void onDestroy() {
		super.onDestroy();
	}
	
	boolean noItemChecked()
	{
		int checkedItemCount = mDb_page.getCheckedNotesCount();
		return (checkedItemCount == 0);
	}
	
	/*
	 * inner class for note list loader
	 */
	public static class NoteListLoader extends AsyncTaskLoader<List<String>> 
	{
		List<String> mApps;

		NoteListLoader(Context context) {
			super(context);

		}

		@Override
		public List<String> loadInBackground() {
			return new ArrayList<>();
		}

		@Override
		protected void onStartLoading() {
			forceLoad();
		}
	}

	/*
	 * 	inner class for note list adapter
	 */
	public static class NoteListAdapter extends ArrayAdapter<String> 
	{
		NoteListAdapter(Context context) {
			super(context, android.R.layout.simple_list_item_1);
		}
		public void setData(List<String> data) {
			clear();
			if (data != null) {		
					addAll(data);
			}
		}
	}
}
