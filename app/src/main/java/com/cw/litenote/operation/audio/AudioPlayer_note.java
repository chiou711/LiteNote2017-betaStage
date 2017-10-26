package com.cw.litenote.operation.audio;

import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.widget.Toast;

import com.cw.litenote.R;
import com.cw.litenote.note.Note;
import com.cw.litenote.note.NoteUi;
import com.cw.litenote.note.Note_audio;
import com.cw.litenote.util.Util;
import com.cw.litenote.util.preferences.Pref;

public class AudioPlayer_note
{
	private static final int DURATION_1S = 1000; // 1 seconds per slide
    private static AudioInfo mAudioInfo; // slide show being played
	public static int mAudioPos; // index of current media to play
	private static int mPlaybackTime; // time in miniSeconds from which media should play
    private FragmentActivity act;
	private ViewPager notePager;
    private Async_audioUrlVerify mAudioUrlVerifyTask;
    public static Handler mAudioHandler; // used to update the slide show

    public AudioPlayer_note(FragmentActivity act, ViewPager pager){
        this.act = act;
        this.notePager = pager;

		// start a new handler
		mAudioHandler = new Handler();
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
	   	System.out.println("AudioPlayer_note / _runAudioState ");
	   	// if media player is null, set new fragment
		if(AudioInfo.mMediaPlayer == null)
		{
            mPlaybackTime = 0;
            AudioInfo.setPlayerState(AudioInfo.PLAYER_AT_PLAY);
            startNewAudio();
		}
		else
		{
			// from play to pause
			if(AudioInfo.mMediaPlayer.isPlaying())
			{
				System.out.println("AudioPlayer_note / _runAudioState / play -> pause");
				AudioInfo.mMediaPlayer.pause();
				mAudioHandler.removeCallbacks(mRunOneTimeMode);
                AudioInfo.setPlayerState(AudioInfo.PLAYER_AT_PAUSE);
			}
			else // from pause to play
			{
				System.out.println("AudioPlayer_note / _runAudioState / pause -> play");
				AudioInfo.mMediaPlayer.start();

				if(AudioInfo.getAudioPlayMode() == AudioInfo.ONE_TIME_MODE)
					mAudioHandler.post(mRunOneTimeMode);

                AudioInfo.setPlayerState(AudioInfo.PLAYER_AT_PLAY);
			}
		}
	}


    /**
     * One time mode runnable
     */
	private Runnable mRunOneTimeMode = new Runnable()
	{   @Override
		public void run()
		{
            if(!AudioInfo.isRunnableOn_note)
            {
                System.out.println("AudioPlayer_note / mRunOneTimeMode / AudioInfo.isRunnableOn_note = " + AudioInfo.isRunnableOn_note);
                stopHandler();
                stopAsyncTask();
                return;
            }

	   		if(AudioInfo.mMediaPlayer == null)
	   		{
	   			String audioStr = AudioInfo.getAudioStringAt(mAudioPos);
	   			if(Async_audioUrlVerify.mIsOkUrl)
	   			{
                    System.out.println("AudioPlayer_note / mRunOneTimeMode / AudioInfo.isRunnableOn_note = " + AudioInfo.isRunnableOn_note);

				    //create a MediaPlayer
				    AudioInfo.mMediaPlayer = new MediaPlayer();
	   				AudioInfo.mMediaPlayer.reset();

	   				//set audio player listeners
                    setMediaPlayerListeners(notePager);
	   				
	   				try
	   				{
//						AudioInfo.mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
	   					AudioInfo.mMediaPlayer.setDataSource(act, Uri.parse(audioStr));
	   					
					    // prepare the MediaPlayer to play, this will delay system response 
   						AudioInfo.mMediaPlayer.prepare();
   						
	   					//Note: below
	   					//Set 1 second will cause Media player abnormal on Power key short click
	   					mAudioHandler.postDelayed(mRunOneTimeMode,DURATION_1S * 2);
	   				}
	   				catch(Exception e)
	   				{
	   					Toast.makeText(act,R.string.audio_message_could_not_open_file,Toast.LENGTH_SHORT).show();
	   					AudioInfo.stopAudioPlayer();
	   				}
	   			}
	   			else
	   			{
	   				Toast.makeText(act,R.string.audio_message_no_media_file_is_found,Toast.LENGTH_SHORT).show();
   					AudioInfo.stopAudioPlayer();
	   			}
	   		}
	   		else//AudioInfo.mMediaPlayer != null
	   		{
	   			Note_audio.updateAudioProgress(act);
				mAudioHandler.postDelayed(mRunOneTimeMode,DURATION_1S);
	   		}		    		
		} 
	};

	private void stopHandler()
    {
        if(mAudioHandler != null) {
            mAudioHandler.removeCallbacks(mRunOneTimeMode);
            mAudioHandler = null;
        }
    }

    private void stopAsyncTask()
    {
        // stop async task
        // make sure progress dialog will disappear
        if( (mAudioUrlVerifyTask!= null) &&
                (!mAudioUrlVerifyTask.isCancelled()) )
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

    }

    /**
     * Set audio player listeners
     */
	private void setMediaPlayerListeners(final ViewPager pager)
	{
        // - on prepared listener
        AudioInfo.mMediaPlayer.setOnPreparedListener(new OnPreparedListener()
        {	@Override
            public void onPrepared(MediaPlayer mp)
            {
                System.out.println("AudioPlayer_note / _setAudioPlayerListeners / _onPrepared");

                if (AudioInfo.getAudioPlayMode() == AudioInfo.ONE_TIME_MODE)
                {
                    if (AudioInfo.mMediaPlayer != null)
                    {
                        AudioInfo.mIsPrepared = true;
                        if (!Note_audio.isPausedAtSeekerAnchor)
                        {
                            AudioInfo.mMediaPlayer.start();
                            AudioInfo.mMediaPlayer.getDuration();
                            AudioInfo.mMediaPlayer.seekTo(mPlaybackTime);
                        }
                        else
                            AudioInfo.mMediaPlayer.seekTo(Note_audio.mAnchorPosition);

                        Note_audio.updateAudioPlayState(act);
                    }
                }
            }
        });

        // On Completion listener
        AudioInfo.mMediaPlayer.setOnCompletionListener(new OnCompletionListener()
        {	@Override
        public void onCompletion(MediaPlayer mp)
        {
            System.out.println("AudioPlayer_note / _setAudioPlayerListeners / _onCompletion");

            if(AudioInfo.mMediaPlayer != null)
                AudioInfo.mMediaPlayer.release();

            AudioInfo.mMediaPlayer = null;
            mPlaybackTime = 0;

            if(AudioInfo.getAudioPlayMode() == AudioInfo.ONE_TIME_MODE) // one time mode
            {
                if(Pref.getPref_is_autoPlay_YouTubeApi(act))
                {
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
                    AudioInfo.stopAudioPlayer();
                    Note_audio.initAudioProgress(act, Note.mAudioUriInDB,pager);
                    Note_audio.updateAudioPlayState(act);
                }
            }
        }
        });

        // - on error listener
        AudioInfo.mMediaPlayer.setOnErrorListener(new OnErrorListener()
        {	@Override
            public boolean onError(MediaPlayer mp,int what,int extra)
            {
                // more than one error when playing an index
                System.out.println("AudioPlayer_note / _setAudioPlayerListeners / _onError / what = " + what + " , extra = " + extra);
                return false;
            }
        });
	}


    /**
     * Start new audio
     */
	private void startNewAudio()
	{
		// remove call backs to make sure next toast will appear soon
		if(mAudioHandler != null)
			mAudioHandler.removeCallbacks(mRunOneTimeMode);
        mAudioHandler = null;
        mAudioHandler = new Handler();

        AudioInfo.isRunnableOn_page = false;
        AudioInfo.isRunnableOn_note = true;
        AudioInfo.mMediaPlayer = null;

		mAudioUrlVerifyTask = new Async_audioUrlVerify(act, mAudioInfo.getAudioStringAt(mAudioPos));
		mAudioUrlVerifyTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,"Searching media ...");

		while(!Async_audioUrlVerify.mIsOkUrl)
        {
            //wait for Url verification
        }

        if(Async_audioUrlVerify.mIsOkUrl)
        {
            // launch handler
            if(AudioInfo.getPlayerState() != AudioInfo.PLAYER_AT_STOP)
            {
                if(AudioInfo.getAudioPlayMode() == AudioInfo.ONE_TIME_MODE) {
                    mAudioHandler.postDelayed(mRunOneTimeMode, Util.oneSecond / 4);
                }
            }

            // during audio Preparing
            Async_audioPrepare mAsyncTaskAudioPrepare = new Async_audioPrepare(act);
            mAsyncTaskAudioPrepare.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,"Preparing to play ...");
        }

    }
	

    /**
     * Play next audio
     */
    private void playNextAudio()
    {
//		Toast.makeText(act,"Can not open file, try next one.",Toast.LENGTH_SHORT).show();
        System.out.println("AudioPlayer_note / _playNextAudio");
        AudioInfo.stopAudioPlayer();

        // new audio index
        mAudioPos++;

        if(mAudioPos >= AudioInfo.getAudioList().size())
            mAudioPos = 0; //back to first index

        mPlaybackTime = 0;
        AudioInfo.setPlayerState(AudioInfo.PLAYER_AT_PLAY);
        startNewAudio();
    }

}