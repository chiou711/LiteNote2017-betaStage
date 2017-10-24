package com.cw.litenote.util.audio;

import java.io.File;
import java.util.Locale;

import com.cw.litenote.folder.FolderUi;
import com.cw.litenote.main.MainAct;
import com.cw.litenote.operation.audio.AudioPlayer;
import com.cw.litenote.page.Page;
import com.cw.litenote.R;
import com.cw.litenote.page.PageUi;
import com.cw.litenote.util.ColorSet;
import com.cw.litenote.util.Util;

import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.widget.ImageView;
import android.widget.TextView;

public class UtilAudio {
	
	// Stop audio media player and audio handler
    public static void stopAudioPlayer()
    {
    	System.out.println("UtilAudio / _stopAudioPlayer");
        if(AudioPlayer.mMediaPlayer != null)
    	{
			if(AudioPlayer.mMediaPlayer.isPlaying())
				AudioPlayer.mMediaPlayer.pause();
    		AudioPlayer.mMediaPlayer.release();
    		AudioPlayer.mMediaPlayer = null;

            AudioPlayer.isRunnableOn = false;

    		AudioPlayer.setPlayerState(AudioPlayer.PLAYER_AT_STOP);
    	}
    }
    
    public static void stopAudioIfNeeded()
    {
		if( (AudioPlayer.mMediaPlayer != null)    &&
			(MainAct.mPlaying_folderPos == FolderUi.getFocus_folderPos())&&
			(PageUi.getFocus_pagePos() == MainAct.mPlaying_pagePos)&&
			(AudioPlayer.getPlayerState() != AudioPlayer.PLAYER_AT_STOP)      )
		{
			UtilAudio.stopAudioPlayer();
			AudioPlayer.mAudioPos = 0;
			AudioPlayer.setPlayerState(AudioPlayer.PLAYER_AT_STOP);
			if(MainAct.mSubMenuItemAudio != null)
				MainAct.mSubMenuItemAudio.setIcon(R.drawable.ic_menu_slideshow);
			Page.mItemAdapter.notifyDataSetChanged(); // disable focus
		}     	
    }
    
    // update audio panel
    public static void updateAudioPanel(ImageView playBtn, TextView titleTextView)
    {
    	System.out.println("UtilAudio/ _updateAudioPanel / AudioPlayer.getPlayerState() = " + AudioPlayer.getPlayerState());
		titleTextView.setBackgroundColor(ColorSet.color_black);
		if(AudioPlayer.getPlayerState() == AudioPlayer.PLAYER_AT_PLAY)
		{
			titleTextView.setTextColor(ColorSet.getHighlightColor(MainAct.mAct));
			titleTextView.setSelected(true);
			playBtn.setImageResource(R.drawable.ic_media_pause);
		}
		else if( (AudioPlayer.getPlayerState() == AudioPlayer.PLAYER_AT_PAUSE) ||
				 (AudioPlayer.getPlayerState() == AudioPlayer.PLAYER_AT_STOP)    )
		{
			titleTextView.setSelected(false);
			titleTextView.setTextColor(ColorSet.getPauseColor(MainAct.mAct));
			playBtn.setImageResource(R.drawable.ic_media_play);
		}

    }

    // check if file has audio extension
    // refer to http://developer.android.com/intl/zh-tw/guide/appendix/media-formats.html
    public static boolean hasAudioExtension(File file)
    {
    	boolean hasAudio = false;
    	String fn = file.getName().toLowerCase(Locale.getDefault());
    	if(	fn.endsWith("3gp") || fn.endsWith("mp4") ||	fn.endsWith("m4a") || fn.endsWith("aac") ||
       		fn.endsWith("ts") || fn.endsWith("flac") ||	fn.endsWith("mp3") || fn.endsWith("mid") ||  
       		fn.endsWith("xmf") || fn.endsWith("mxmf")|| fn.endsWith("rtttl") || fn.endsWith("rtx") ||  
       		fn.endsWith("ota") || fn.endsWith("imy")|| fn.endsWith("ogg") || fn.endsWith("mkv") ||
       		fn.endsWith("wav") || fn.endsWith("wma")
    		) 
	    	hasAudio = true;
	    
    	return hasAudio;
    }
    
    // check if string has audio extension
    public static boolean hasAudioExtension(String string)
    {
    	boolean hasAudio = false;
    	if(!Util.isEmptyString(string))
    	{
	    	String fn = string.toLowerCase(Locale.getDefault());
	    	if(	fn.endsWith("3gp") || fn.endsWith("mp4") ||	fn.endsWith("m4a") || fn.endsWith("aac") ||
	           		fn.endsWith("ts") || fn.endsWith("flac") ||	fn.endsWith("mp3") || fn.endsWith("mid") ||  
	           		fn.endsWith("xmf") || fn.endsWith("mxmf")|| fn.endsWith("rtttl") || fn.endsWith("rtx") ||  
	           		fn.endsWith("ota") || fn.endsWith("imy")|| fn.endsWith("ogg") || fn.endsWith("mkv") ||
	           		fn.endsWith("wav") || fn.endsWith("wma")
	        		) 
	    		hasAudio = true;
    	}
    	return hasAudio;
    }     
    
    public static boolean mIsCalledWhilePlayingAudio;
    // for Pause audio player when incoming phone call
    // http://stackoverflow.com/questions/5610464/stopping-starting-music-on-incoming-calls
    public static PhoneStateListener phoneStateListener = new PhoneStateListener() 
    {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) 
        {
			System.out.print("UtilAudio / _onCallStateChanged");
            if ( (state == TelephonyManager.CALL_STATE_RINGING) ||
                 (state == TelephonyManager.CALL_STATE_OFFHOOK )   ) 
            {
            	System.out.println(" -> Incoming phone call:");
                //from Play to Pause
            	if(AudioPlayer.getPlayerState() == AudioPlayer.PLAYER_AT_PLAY)
            	{
                    if( (AudioPlayer.mMediaPlayer != null) &&
                        AudioPlayer.mMediaPlayer.isPlaying() ) {
                        AudioPlayer.setPlayerState(AudioPlayer.PLAYER_AT_PAUSE);
                        AudioPlayer.mMediaPlayer.pause();
                    }
            		mIsCalledWhilePlayingAudio = true;
            	}
            } 
            else if(state == TelephonyManager.CALL_STATE_IDLE) 
            {
            	System.out.println(" -> Not in phone call:");
                // from Pause to Play
            	if( (AudioPlayer.getPlayerState() == AudioPlayer.PLAYER_AT_PAUSE) &&
            		mIsCalledWhilePlayingAudio )	
            	{
                    if( (AudioPlayer.mMediaPlayer != null) &&
                        !AudioPlayer.mMediaPlayer.isPlaying() ) {
                        AudioPlayer.setPlayerState(AudioPlayer.PLAYER_AT_PLAY);
                        AudioPlayer.mMediaPlayer.start();
                    }
                    mIsCalledWhilePlayingAudio = false;
            	}
            }
            super.onCallStateChanged(state, incomingNumber);
        }
    };
    
    
}
