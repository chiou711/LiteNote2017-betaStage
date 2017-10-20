package com.cw.litenote.operation.audio;

import com.cw.litenote.folder.FolderUi;
import com.cw.litenote.main.MainAct;
import com.cw.litenote.note.NoteUi;
import com.cw.litenote.page.Page;
import com.cw.litenote.R;
import com.cw.litenote.page.PageUi;
import com.cw.litenote.tabs.TabsHost;
import com.cw.litenote.note.Note;
import com.cw.litenote.util.Util;
import com.cw.litenote.util.preferences.Pref;

import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class AudioPlayer 
{
	private static final String TAG = "AUDIO_PLAYER"; // error logging tag
	private static final int DURATION_1S = 1000; // 1 seconds per slide
	static AudioInfo mAudioInfo; // slide show being played
	public static Handler mAudioHandler; // used to update the slide show
	public static int mAudioPos; // index of current media to play
	private static int mPlaybackTime; // time in miniSeconds from which media should play
	public static MediaPlayer mMediaPlayer; // plays the background music, if any

	private static int mAudioPlayMode;
	private static int mAudio_tryTimes; // use to avoid useless looping in Continue mode
	public final static int ONE_TIME_MODE = 0;
	public final static int CONTINUE_MODE = 1;

	private static int mPlayerState;
	public static int PLAYER_AT_STOP = 0;
	public static int PLAYER_AT_PLAY = 1;
	public static int PLAYER_AT_PAUSE = 2;
    public static boolean willPlayNext;
    public static boolean isRunnableOn;

    FragmentActivity act;
	private static ViewPager mNotePager;
    public Async_audioUrlVerify mAudioUrlVerifyTask;


    public AudioPlayer(FragmentActivity act){
        this.act = act;
        isRunnableOn = true;
    }

    /**
     * Setters and Getters
     *
     */
	// play state
	public static int getPlayState() {
		return mPlayerState;
	}

	public static void setAudioState(int mPlayerState) {
		AudioPlayer.mPlayerState = mPlayerState;
	}

	// play mode
	public static int getAudioMode() {
		return mAudioPlayMode;
	}

	public static void setPlayMode(int mAudioPlayMode) {
		AudioPlayer.mAudioPlayMode = mAudioPlayMode;
	}

	// view pager
    public static void setViewPager(ViewPager _pager)
    {
        mNotePager = _pager;
    }

    private static ViewPager getViewPager()
    {
        return mNotePager;
    }

    /**
     * prepare audio info
     */
    public static void prepareAudioInfo()
    {
        mAudioInfo = new AudioInfo();
        mAudioInfo.updateAudioInfo();
    }

	/**
     *  Run audio state
     */
    public void runAudioState()
	{
        mNotePager = getViewPager();
	   	System.out.println("AudioPlayer / _manageAudioState ");
	   	// if media player is null, set new fragment
		if(mMediaPlayer == null)
		{
		 	// show toast if Audio file is not found or No selection of audio file
			if( (AudioInfo.getAudioFilesCount() == 0) &&
				(getAudioMode() == AudioPlayer.CONTINUE_MODE)        )
			{
				Toast.makeText(act,R.string.audio_file_not_found,Toast.LENGTH_SHORT).show();
			}
			else
			{
				mPlaybackTime = 0;
				setAudioState(PLAYER_AT_PLAY);
				mAudio_tryTimes = 0;
				startNewAudio();
			}
		}
		else
		{
			// from play to pause
			if(mMediaPlayer.isPlaying())
			{
				System.out.println("AudioPlayer / _manageAudioState / play -> pause");
				mMediaPlayer.pause();
				mAudioHandler.removeCallbacks(mRunOneTimeMode); 
				mAudioHandler.removeCallbacks(mRunContinueMode);
				setAudioState(PLAYER_AT_PAUSE);
			}
			else // from pause to play
			{
				System.out.println("AudioPlayer / _manageAudioState / pause -> play");

				mMediaPlayer.start();

				if(getAudioMode() == ONE_TIME_MODE)
					mAudioHandler.post(mRunOneTimeMode);
				else if(getAudioMode() == CONTINUE_MODE)
					mAudioHandler.post(mRunContinueMode);

				setAudioState(PLAYER_AT_PLAY);
			}
		}
	}

    /**
     * One time mode runnable
     */
	public Runnable mRunOneTimeMode = new Runnable()
	{   @Override
		public void run()
		{
            if(!isRunnableOn)
            {
                if(mAudioHandler != null) {
                    mAudioHandler.removeCallbacks(mRunOneTimeMode);
                    mAudioHandler.removeCallbacks(mRunContinueMode);
                    mAudioHandler = null;
                }
                return;
            }

	   		if(mMediaPlayer == null)
	   		{
	   			String audioStr = mAudioInfo.getAudioStringAt(mAudioPos);
	   			if(Async_audioUrlVerify.mIsOkUrl)
	   			{
				    System.out.println("Runnable updateMediaPlay / play mode: OneTime");
	   				
				    //create a MediaPlayer
				    mMediaPlayer = new MediaPlayer();
	   				mMediaPlayer.reset();

	   				//set audio player listeners
	   				setMediaPlayerListeners(getViewPager());
	   				
	   				try
	   				{
//						mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
	   					mMediaPlayer.setDataSource(act, Uri.parse(audioStr));
	   					
					    // prepare the MediaPlayer to play, this will delay system response 
   						mMediaPlayer.prepare();
   						
	   					//Note: below
	   					//Set 1 second will cause Media player abnormal on Power key short click
	   					mAudioHandler.postDelayed(mRunOneTimeMode,DURATION_1S * 2);
	   				}
	   				catch(Exception e)
	   				{
	   					Toast.makeText(act,R.string.audio_message_could_not_open_file,Toast.LENGTH_SHORT).show();
	   					stopAudio();
	   				}
	   			}
	   			else
	   			{
	   				Toast.makeText(act,R.string.audio_message_no_media_file_is_found,Toast.LENGTH_SHORT).show();
   					stopAudio();
	   			}
	   		}
	   		else//mMediaPlayer != null
	   		{
	   			Note.updateAudioProgress(act);
				mAudioHandler.postDelayed(mRunOneTimeMode,DURATION_1S);
	   		}		    		
		} 
	};

    /**
     * Continue mode runnable
     */
	public static String mAudioStrContinueMode;
	public Runnable mRunContinueMode = new Runnable()
	{   @Override
		public void run()
		{
            if(!isRunnableOn)
            {
                if(mAudioHandler != null) {
                    mAudioHandler.removeCallbacks(mRunOneTimeMode);
                    mAudioHandler.removeCallbacks(mRunContinueMode);
                    mAudioHandler = null;
                }
                return;
            }

	   		if( AudioInfo.getCheckedAudio(mAudioPos) == 1 )
	   		{ 
	   			if(mMediaPlayer == null)
	   			{
		    		// check if audio file exists or not
   					mAudioStrContinueMode = mAudioInfo.getAudioStringAt(mAudioPos);

					if(!Async_audioUrlVerify.mIsOkUrl)
					{
						mAudio_tryTimes++;
						playNextAudio();
					}
					else
   					{
   						System.out.println("* Runnable updateMediaPlay / play mode: continue");
	   					
   						//create a MediaPlayer
   						mMediaPlayer = new MediaPlayer(); 
	   					mMediaPlayer.reset();
	   					willPlayNext = true; // default: play next
	   					Page.mProgress = 0;


						// for network stream buffer change
	   					mMediaPlayer.setOnBufferingUpdateListener(new OnBufferingUpdateListener()
	   					{
	   						@Override
	   						public void onBufferingUpdate(MediaPlayer mp, int percent) {
								if(Page.seekBarProgress != null)
	   								Page.seekBarProgress.setSecondaryProgress(percent);
	   						}
	   					});
   						
	   					// set listeners
   						setMediaPlayerListeners(getViewPager());
   						
   						try
   						{
   							// set data source
//							mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
							mMediaPlayer.setDataSource(act, Uri.parse(mAudioStrContinueMode));
   							
   							// prepare the MediaPlayer to play, could delay system response
   							mMediaPlayer.prepare();
   						}
   						catch(Exception e)
   						{
   							System.out.println("AudioPlayer on Exception");
   							Log.e(TAG, e.toString());
							mAudio_tryTimes++;
   							playNextAudio();
   						}
   					}
	   			}
	   			else//mMediaPlayer != null
	   			{
	   				// keep looping, do not set post() here, it will affect slide show timing
	   				if(mAudio_tryTimes < AudioInfo.getAudioFilesCount())
	   				{

						// update seek bar
	   					Page.update_audioPanel_progress(AudioPlayer.this); //todo static

						if(mAudio_tryTimes == 0)
							mAudioHandler.postDelayed(mRunContinueMode,DURATION_1S);
						else
							mAudioHandler.postDelayed(mRunContinueMode,DURATION_1S/10);
	   				}
	   			}
	   		}
	   		else if( AudioInfo.getCheckedAudio(mAudioPos) == 0 )// for non-marking item
	   		{
	   			System.out.println("--- for non-marking item");
	   			// get next index
	   			if(willPlayNext)
	   				mAudioPos++;
	   			else
	   				mAudioPos--;
	   			
	   			if( mAudioPos >= AudioInfo.getAudioList().size())
	   				mAudioPos = 0; //back to first index
	   			else if( mAudioPos < 0)
	   			{
	   				mAudioPos++;
	   				willPlayNext = true;
	   			}
	   			
	   			startNewAudio();
	   		}
		} 
	};	
	
	static boolean mIsPrepared;
    public static int media_file_length;
    /**
     * Set audio player listeners
     */
	private void setMediaPlayerListeners(final ViewPager pager)
	{
			// On Completion listener
			mMediaPlayer.setOnCompletionListener(new OnCompletionListener()
			{	@Override
				public void onCompletion(MediaPlayer mp) 
				{
					System.out.println("AudioPlayer / _setAudioPlayerListeners / onCompletion");
					
					if(mMediaPlayer != null)
						mMediaPlayer.release();
	
					mMediaPlayer = null;
					mPlaybackTime = 0;

					// get next index
					if(getAudioMode() == CONTINUE_MODE)
					{
                        playNextAudio();
						Page.mItemAdapter.notifyDataSetChanged();
					}
					else // one time mode
					{
						if(Pref.getPref_is_autoPlay_YouTubeApi(act))
						{
                            //TODO need review and improve the flexibility
                            int nextPos;
                            if(NoteUi.getFocus_notePos()+1 >= NoteUi.getNotesCnt() )
                                nextPos = 0;
                            else
                                nextPos = NoteUi.getFocus_notePos()+1;

							NoteUi.setFocus_notePos(nextPos);
                            pager.setCurrentItem(nextPos);
                            playNextAudio();
						}
						else
					    {
							stopAudio();
							Note.initAudioProgress(act, Note.mAudioUriInDB,pager);
							Note.updateAudioPlayState(act);
						}
					}
				}
			});
			
			// - on prepared listener
			mMediaPlayer.setOnPreparedListener(new OnPreparedListener()
			{	@Override
				public void onPrepared(MediaPlayer mp)
				{
					System.out.println("AudioPlayer / _setAudioPlayerListeners / _onPrepared");

					if (getAudioMode() == CONTINUE_MODE)
					{
						// media file length
						media_file_length = mMediaPlayer.getDuration(); // gets the song length in milliseconds from URL
                        System.out.println("AudioPlayer / _setAudioPlayerListeners / media_file_length = " + media_file_length);

						// set footer message: media name
						if (!Util.isEmptyString(mAudioStrContinueMode) &&
                            Page.mDndListView.isShown()                )
						{
							TextView audioPanel_file_length = (TextView) act.findViewById(R.id.audioPanel_file_length);
							// show audio file length of playing
							int fileHour = Math.round((float)(media_file_length / 1000 / 60 / 60));
							int fileMin = Math.round((float)((media_file_length - fileHour * 60 * 60 * 1000) / 1000 / 60));
							int fileSec = Math.round((float)((media_file_length - fileHour * 60 * 60 * 1000 - fileMin * 1000 * 60 )/ 1000));
                            if(audioPanel_file_length != null) {
                                audioPanel_file_length.setText(String.format(Locale.US, "%2d", fileHour) + ":" +
                                        String.format(Locale.US, "%02d", fileMin) + ":" +
                                        String.format(Locale.US, "%02d", fileSec));
                            }

                            Page.initAudioBlock(AudioPlayer.this);//todo static , this line must be here?

							// set seek bar progress
							Page.update_audioPanel_progress(AudioPlayer.this);//todo static

                            scrollHighlightAudioItemToVisible();
						}

						if (mMediaPlayer != null)
						{
							mIsPrepared = true;
							mMediaPlayer.start();
                            mMediaPlayer.seekTo(mPlaybackTime);

							// set highlight of playing tab
							if ((getAudioMode() == CONTINUE_MODE) &&
								(MainAct.mPlaying_folderPos == FolderUi.getFocus_folderPos())  )
								TabsHost.setAudioPlayingTab_WithHighlight(true);
							else
								TabsHost.setAudioPlayingTab_WithHighlight(false);

							Page.mItemAdapter.notifyDataSetChanged();

                            Page.isOnAudioClick = false;

							// add for calling runnable
							if (getAudioMode() == CONTINUE_MODE)
								mAudioHandler.postDelayed(mRunContinueMode, Util.oneSecond / 4);
						}
					}
					else if (getAudioMode() == ONE_TIME_MODE)
					{
                        if (mMediaPlayer != null)
                        {
                            mIsPrepared = true;
                            if (!Note.isPausedAtSeekerAnchor)
                            {
                                mMediaPlayer.start();
                                mMediaPlayer.getDuration();
                                mMediaPlayer.seekTo(mPlaybackTime);
                            }
                            else
                            {
                                mMediaPlayer.seekTo(Note.mAnchorPosition);
                                Note.mPager_audio_play_button.setImageResource(R.drawable.ic_media_play);
								TextView audio_title_text_view = (TextView) act.findViewById(R.id.pager_audio_title);
                                audio_title_text_view.setSelected(false);
                            }
                        }
					}
				}
			});
			
			// - on error listener
			mMediaPlayer.setOnErrorListener(new OnErrorListener()
			{	@Override
				public boolean onError(MediaPlayer mp,int what,int extra) 
				{
					// more than one error when playing an index 
					System.out.println("AudioPlayer / _setAudioPlayerListeners / on Error: what = " + what + " , extra = " + extra);
					return false;
				}
			});
	}

	/**
	* Scroll highlight audio item to visible position
	*
	* At the following conditions
	* 	1) click audio item of list view (this highlight is not good for user expectation, so cancel this condition now)
	* 	2) click previous/next item in audio controller
	* 	3) change tab to playing tab
	* 	4) back from key protect off
	* 	5) if seeker bar reaches the end
	* In order to view audio highlight item, playing(highlighted) audio item can be auto scrolled to top,
	* unless it is at the end page of list view, there is no need to scroll.
	*/
	public static void scrollHighlightAudioItemToVisible()
	{
		System.out.println("AudioPlayer / _scrollHighlightAudioItemToVisible");
		// version limitation: _scrollListBy
		// NoteFragment.mDndListView.scrollListBy(firstVisibleIndex_top);
		if(Build.VERSION.SDK_INT < 19)
			return;

		// check playing drawer and playing tab
		if( (PageUi.getFocus_pagePos() == MainAct.mPlaying_pagePos) &&
			(MainAct.mPlaying_folderPos == FolderUi.getFocus_folderPos()) &&
			(Page.mDndListView.getChildAt(0) != null)                   )
		{
			int itemHeight = Page.mDndListView.getChildAt(0).getHeight();
			int dividerHeight = Page.mDndListView.getDividerHeight();

			int firstVisible_noteId = Page.mDndListView.getFirstVisiblePosition();
			View v = Page.mDndListView.getChildAt(0);
			int firstVisibleNote_top = (v == null) ? 0 : v.getTop();

//			System.out.println("---------------- itemHeight = " + itemHeight);
//			System.out.println("---------------- dividerHeight = " + dividerHeight);
//			System.out.println("---------------- firstVisible_noteId = " + firstVisible_noteId);
//			System.out.println("---------------- firstVisibleNote_top = " + firstVisibleNote_top);
//			System.out.println("---------------- AudioPlayer.mAudioPos = " + AudioPlayer.mAudioPos);

			if(firstVisibleNote_top < 0)
			{
				Page.mDndListView.scrollListBy(firstVisibleNote_top);
//				System.out.println("-----scroll backwards by firstVisibleNote_top " + firstVisibleNote_top);
				firstVisibleNote_top = 0; // update top after scrolling
			}

			boolean noScroll = false;
			// base on AudioPlayer.mAudioPos to scroll
			if(firstVisible_noteId != AudioPlayer.mAudioPos)
			{
				while ((firstVisible_noteId != AudioPlayer.mAudioPos) && (!noScroll))
				{
					int offset = itemHeight + dividerHeight;
					// scroll forwards
					if (firstVisible_noteId > AudioPlayer.mAudioPos)
					{
						Page.mDndListView.scrollListBy(-offset);
//						System.out.println("-----scroll forwards " + (-offset));
					}
					// scroll backwards
					else if (firstVisible_noteId < AudioPlayer.mAudioPos)
					{
						Page.mDndListView.scrollListBy(offset);
//						System.out.println("-----scroll backwards " + offset);
					}

//					System.out.println("---------------- firstVisible_noteId 2 = " + firstVisible_noteId);
//					System.out.println("---------------- NoteFragment.mDndListView.getFirstVisiblePosition() = " + NoteFragment.mDndListView.getFirstVisiblePosition());
					if(firstVisible_noteId == Page.mDndListView.getFirstVisiblePosition())
						noScroll = true;
					else {
						// update first visible index
						firstVisible_noteId = Page.mDndListView.getFirstVisiblePosition();
					}
				}
			}
			// backup scroll Y
			Pref.setPref_focusView_list_view_first_visible_index(Page.mAct,firstVisible_noteId);
			Pref.setPref_focusView_list_view_first_visible_index_top(Page.mAct,firstVisibleNote_top);

			Page.mItemAdapter.notifyDataSetChanged();
		}
	}

    /**
     * Start new audio
     */
	private void startNewAudio()
	{
		// remove call backs to make sure next toast will appear soon
		if(mAudioHandler != null)
		{
			mAudioHandler.removeCallbacks(mRunOneTimeMode); 
			mAudioHandler.removeCallbacks(mRunContinueMode);
		}

		// start a new handler
		mAudioHandler = new Handler();
		
		if( (getAudioMode() == CONTINUE_MODE) && (AudioInfo.getCheckedAudio(mAudioPos) == 0))
		{
			mAudioHandler.postDelayed(mRunContinueMode,Util.oneSecond/4);		}
		else
		{
			mAudioUrlVerifyTask = new Async_audioUrlVerify(act);
			mAudioUrlVerifyTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,"Searching media ...");
		}

		while(!Async_audioUrlVerify.mIsOkUrl)
        {
            //wait for Url verification
        }


        if(Async_audioUrlVerify.mIsOkUrl)
        {
            // launch handler
            if(AudioPlayer.getPlayState() != AudioPlayer.PLAYER_AT_STOP) {
                //Todo static issue
                if(AudioPlayer.getAudioMode() == AudioPlayer.ONE_TIME_MODE)
                    AudioPlayer.mAudioHandler.postDelayed(mRunOneTimeMode,Util.oneSecond/4);
                else if(AudioPlayer.getAudioMode() == AudioPlayer.CONTINUE_MODE)
                    AudioPlayer.mAudioHandler.postDelayed(mRunContinueMode, Util.oneSecond / 4);
            }

            // during audio Preparing
            Async_audioPrepare mAsyncTaskAudioPrepare = new Async_audioPrepare(act);
            mAsyncTaskAudioPrepare.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,"Preparing to play ...");
        }

    }
	

    /**
     * Stop audio
     */
	private void stopAudio()
	{
		System.out.println("AudioPlayer / _stopAudio");

        // stop media player
        if(mMediaPlayer != null)
            mMediaPlayer.release();

        // set flag to remove runnable
        if(mAudioHandler != null)
            isRunnableOn = false;

        // stop handler
		if(mAudioHandler != null) {
			mAudioHandler.removeCallbacks(mRunOneTimeMode);
			mAudioHandler.removeCallbacks(mRunContinueMode);
			mAudioHandler = null;
		}

		// stop async task
        // make sure progress dialog will disappear
	 	if( (mAudioUrlVerifyTask!= null) && (!mAudioUrlVerifyTask.isCancelled()) )
	 	{
	 		mAudioUrlVerifyTask.cancel(true);

	 		if( (mAudioUrlVerifyTask.mUrlVerifyDialog != null) &&
	 		 	mAudioUrlVerifyTask.mUrlVerifyDialog.isShowing()	)
	 		{
	 			mAudioUrlVerifyTask.mUrlVerifyDialog.dismiss();
	 		}

	 		if( (mAudioUrlVerifyTask.mAsyncTaskAudioPrepare != null) &&
	 			(mAudioUrlVerifyTask.mAsyncTaskAudioPrepare.mPrepareDialog != null) &&
	 			mAudioUrlVerifyTask.mAsyncTaskAudioPrepare.mPrepareDialog.isShowing()	)
	 		{
	 			mAudioUrlVerifyTask.mAsyncTaskAudioPrepare.mPrepareDialog.dismiss();
	 		}
	 	}

		setAudioState(PLAYER_AT_STOP);
	}

    /**
     * Play next audio
     */
    private void playNextAudio()
    {
//		Toast.makeText(act,"Can not open file, try next one.",Toast.LENGTH_SHORT).show();
        System.out.println("AudioPlayer / _playNextAudio");
        if(mMediaPlayer != null)
        {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        mPlaybackTime = 0;

        // new audio index
        mAudioPos++;

        if(mAudioPos >= AudioInfo.getAudioList().size())
            mAudioPos = 0; //back to first index

        // check try times,had tried or not tried yet, anyway the audio file is found
        System.out.println("check mTryTimes = " + mAudio_tryTimes);
        if(mAudio_tryTimes < AudioInfo.getAudioFilesCount() )
        {
            startNewAudio();
        }
        else // try enough times: still no audio file is found
        {
            Toast.makeText(act,R.string.audio_message_no_media_file_is_found,Toast.LENGTH_SHORT).show();

            // do not show highlight
            if(MainAct.mSubMenuItemAudio != null)
                MainAct.mSubMenuItemAudio.setIcon(R.drawable.ic_menu_slideshow);
            TabsHost.setAudioPlayingTab_WithHighlight(false);
            Page.mItemAdapter.notifyDataSetChanged();

            // stop media player
            stopAudio();
        }
        System.out.println("Next mAudioPos = " + mAudioPos);
    }

}