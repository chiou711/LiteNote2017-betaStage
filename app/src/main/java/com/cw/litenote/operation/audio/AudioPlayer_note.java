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
    private static AudioManager mAudioManager; // slide show being played
	public static int mAudioPos; // index of current media to play
	private static int mPlaybackTime; // time in miniSeconds from which media should play
    private FragmentActivity act;
	private ViewPager notePager;
    private Async_audioUrlVerify mAudioUrlVerifyTask;
    static Handler mAudioHandler; // used to update the slide show

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
        mAudioManager = new AudioManager();
        mAudioManager.updateAudioInfo();
    }

	/**
     *  Run audio state
     */
    public void runAudioState()
	{
	   	System.out.println("AudioPlayer_note / _runAudioState ");
	   	// if media player is null, set new fragment
		if(AudioManager.mMediaPlayer == null)
		{
            mPlaybackTime = 0;
            if(!Note_audio.isPausedAtSeekerAnchor)
                AudioManager.setPlayerState(AudioManager.PLAYER_AT_PLAY);
            else
                AudioManager.setPlayerState(AudioManager.PLAYER_AT_PAUSE);//just slide the progress bar

            startNewAudio();
		}
		else
		{
			// from play to pause
			if(AudioManager.mMediaPlayer.isPlaying())
			{
				System.out.println("AudioPlayer_note / _runAudioState / play -> pause");
				AudioManager.mMediaPlayer.pause();
				mAudioHandler.removeCallbacks(mRunOneTimeMode);
                AudioManager.setPlayerState(AudioManager.PLAYER_AT_PAUSE);
			}
			else // from pause to play
			{
				System.out.println("AudioPlayer_note / _runAudioState / pause -> play");
				AudioManager.mMediaPlayer.start();

				if(AudioManager.getAudioPlayMode() == AudioManager.NOTE_PLAY_MODE)
					mAudioHandler.post(mRunOneTimeMode);

                AudioManager.setPlayerState(AudioManager.PLAYER_AT_PLAY);
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
            if(!AudioManager.isRunnableOn_note)
            {
                System.out.println("AudioPlayer_note / mRunOneTimeMode / AudioManager.isRunnableOn_note = " + AudioManager.isRunnableOn_note);
                stopHandler();
                stopAsyncTask();
                return;
            }

	   		if(AudioManager.mMediaPlayer == null)
	   		{
	   			String audioStr = AudioManager.getAudioStringAt(mAudioPos);
	   			if(Async_audioUrlVerify.mIsOkUrl)
	   			{
                    System.out.println("AudioPlayer_note / mRunOneTimeMode / AudioManager.isRunnableOn_note = " + AudioManager.isRunnableOn_note);

				    //create a MediaPlayer
				    AudioManager.mMediaPlayer = new MediaPlayer();
	   				AudioManager.mMediaPlayer.reset();

	   				//set audio player listeners
                    setMediaPlayerListeners(notePager);
	   				
	   				try
	   				{
//						AudioManager.mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
	   					AudioManager.mMediaPlayer.setDataSource(act, Uri.parse(audioStr));
	   					
					    // prepare the MediaPlayer to play, this will delay system response 
   						AudioManager.mMediaPlayer.prepare();
   						
	   					//Note: below
	   					//Set 1 second will cause Media player abnormal on Power key short click
	   					mAudioHandler.postDelayed(mRunOneTimeMode,DURATION_1S * 2);
	   				}
	   				catch(Exception e)
	   				{
	   					Toast.makeText(act,R.string.audio_message_could_not_open_file,Toast.LENGTH_SHORT).show();
	   					AudioManager.stopAudioPlayer();
	   				}
	   			}
	   			else
	   			{
	   				Toast.makeText(act,R.string.audio_message_no_media_file_is_found,Toast.LENGTH_SHORT).show();
   					AudioManager.stopAudioPlayer();
	   			}
	   		}
	   		else//AudioManager.mMediaPlayer != null
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
        AudioManager.mMediaPlayer.setOnPreparedListener(new OnPreparedListener()
        {	@Override
            public void onPrepared(MediaPlayer mp)
            {
                System.out.println("AudioPlayer_note / _setAudioPlayerListeners / _onPrepared");

                if (AudioManager.getAudioPlayMode() == AudioManager.NOTE_PLAY_MODE)
                {
                    if (AudioManager.mMediaPlayer != null)
                    {
                        AudioManager.mIsPrepared = true;
                        if (!Note_audio.isPausedAtSeekerAnchor)
                        {
                            AudioManager.mMediaPlayer.start();
                            AudioManager.mMediaPlayer.getDuration();
                            AudioManager.mMediaPlayer.seekTo(mPlaybackTime);
                        }
                        else
                            AudioManager.mMediaPlayer.seekTo(Note_audio.mAnchorPosition);

                        Note_audio.updateAudioPlayState(act);
                    }
                }
            }
        });

        // On Completion listener
        AudioManager.mMediaPlayer.setOnCompletionListener(new OnCompletionListener()
        {	@Override
        public void onCompletion(MediaPlayer mp)
        {
            System.out.println("AudioPlayer_note / _setAudioPlayerListeners / _onCompletion");

            if(AudioManager.mMediaPlayer != null)
                AudioManager.mMediaPlayer.release();

            AudioManager.mMediaPlayer = null;
            mPlaybackTime = 0;

            if(AudioManager.getAudioPlayMode() == AudioManager.NOTE_PLAY_MODE) // one time mode
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
                    AudioManager.stopAudioPlayer();
                    Note_audio.initAudioProgress(act, Note.mAudioUriInDB,pager);
                    Note_audio.updateAudioPlayState(act);
                }
            }
        }
        });

        // - on error listener
        AudioManager.mMediaPlayer.setOnErrorListener(new OnErrorListener()
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

        AudioManager.isRunnableOn_page = false;
        AudioManager.isRunnableOn_note = true;
        AudioManager.mMediaPlayer = null;
        Async_audioUrlVerify.mIsOkUrl = false;

		mAudioUrlVerifyTask = new Async_audioUrlVerify(act, mAudioManager.getAudioStringAt(mAudioPos));
		mAudioUrlVerifyTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,"Searching media ...");

		while(!Async_audioUrlVerify.mIsOkUrl)
        {
            //wait for Url verification
        }

        if(Async_audioUrlVerify.mIsOkUrl)
        {
            // launch handler
            if(AudioManager.getPlayerState() != AudioManager.PLAYER_AT_STOP)
            {
                if(AudioManager.getAudioPlayMode() == AudioManager.NOTE_PLAY_MODE) {
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
        AudioManager.stopAudioPlayer();

        // new audio index
        mAudioPos++;

        if(mAudioPos >= AudioManager.getAudioList().size())
            mAudioPos = 0; //back to first index

        mPlaybackTime = 0;
        AudioManager.setPlayerState(AudioManager.PLAYER_AT_PLAY);
        startNewAudio();
    }

}