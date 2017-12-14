package com.cw.litenote.util.audio;

import java.io.File;
import java.util.Locale;

import com.cw.litenote.folder.FolderUi;
import com.cw.litenote.main.MainAct;
import com.cw.litenote.operation.audio.AudioManager;
import com.cw.litenote.page.Page;
import com.cw.litenote.R;
import com.cw.litenote.page.PageUi;
import com.cw.litenote.util.ColorSet;
import com.cw.litenote.util.Util;

import android.support.v4.app.FragmentActivity;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.widget.ImageView;
import android.widget.TextView;

import static android.content.Context.TELEPHONY_SERVICE;

public class UtilAudio {

	public static void setPhoneListener(FragmentActivity act)
	{
		// To Registers a listener object to receive notification when incoming call
		TelephonyManager telMgr = (TelephonyManager) act.getSystemService(TELEPHONY_SERVICE);
		if (telMgr != null) {
			telMgr.listen(UtilAudio.phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
		}
	}

    public static void stopAudioIfNeeded()
    {
		if( ( (AudioManager.mMediaPlayer != null) &&
              (AudioManager.getPlayerState() != AudioManager.PLAYER_AT_STOP) ) &&
			(MainAct.mPlaying_folderPos == FolderUi.getFocus_folderPos()) &&
			(PageUi.getFocus_pagePos() == MainAct.mPlaying_pagePos)                           )
		{
            if(AudioManager.mMediaPlayer != null){
                AudioManager.stopAudioPlayer();
                AudioManager.mAudioPos = 0;
            }

			if(MainAct.mSubMenuItemAudio != null)
				MainAct.mSubMenuItemAudio.setIcon(R.drawable.ic_menu_slideshow);

			Page.mItemAdapter.notifyDataSetChanged(); // disable focus
		}     	
    }
    
    // update audio panel
    public static void updateAudioPanel(ImageView playBtn, TextView titleTextView)
    {
    	System.out.println("UtilAudio/ _updateAudioPanel / AudioManager.getPlayerState() = " + AudioManager.getPlayerState());
		titleTextView.setBackgroundColor(ColorSet.color_black);
		if(AudioManager.getPlayerState() == AudioManager.PLAYER_AT_PLAY)
		{
			titleTextView.setTextColor(ColorSet.getHighlightColor(MainAct.mAct));
			titleTextView.setSelected(true);
			playBtn.setImageResource(R.drawable.ic_media_pause);
		}
		else if( (AudioManager.getPlayerState() == AudioManager.PLAYER_AT_PAUSE) ||
				 (AudioManager.getPlayerState() == AudioManager.PLAYER_AT_STOP)    )
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
            	if(AudioManager.getPlayerState() == AudioManager.PLAYER_AT_PLAY)
            	{
                    if( (AudioManager.mMediaPlayer != null) &&
                            AudioManager.mMediaPlayer.isPlaying() ) {
                        AudioManager.setPlayerState(AudioManager.PLAYER_AT_PAUSE);
                        AudioManager.mMediaPlayer.pause();
                    }
            		mIsCalledWhilePlayingAudio = true;
            	}
            }
            else if(state == TelephonyManager.CALL_STATE_IDLE) 
            {
            	System.out.println(" -> Not in phone call:");
                // from Pause to Play
            	if( (AudioManager.getPlayerState() == AudioManager.PLAYER_AT_PAUSE) &&
            		mIsCalledWhilePlayingAudio )	
            	{
                    if( (AudioManager.mMediaPlayer != null) &&
                        !AudioManager.mMediaPlayer.isPlaying() ) {
                        AudioManager.setPlayerState(AudioManager.PLAYER_AT_PLAY);
                        AudioManager.mMediaPlayer.start();
                    }
                    mIsCalledWhilePlayingAudio = false;
            	}
            }
            super.onCallStateChanged(state, incomingNumber);
        }
    };
    
    
}
