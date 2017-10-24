package com.cw.litenote.util.audio;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;

import com.cw.litenote.operation.audio.AudioPlayer;
import com.cw.litenote.R;
import com.cw.litenote.note.Note;
import com.cw.litenote.page.Page_audio;

// for earphone jack connection on/off
public class NoisyAudioStreamReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent)
	{
        if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction()))
		{
			if((AudioPlayer.mMediaPlayer != null) && AudioPlayer.mMediaPlayer.isPlaying() )
			{
				System.out.println("NoisyAudioStreamReceiver / play -> pause");
				AudioPlayer.mMediaPlayer.pause();
				AudioPlayer.isRunnableOn = false;
				AudioPlayer.setPlayerState(AudioPlayer.PLAYER_AT_PAUSE);

				UtilAudio.updateAudioPanel(Page_audio.audioPanel_play_button, Page_audio.audio_panel_title_textView);

				if(AudioPlayer.getPlayerState() != AudioPlayer.PLAYER_AT_STOP)
					AudioPlayer.scrollHighlightAudioItemToVisible();

				//update audio play button in pager
				if( (Note.mPager_audio_play_button != null) &&
					Note.mPager_audio_play_button.isShown()    )
				{
					Note.mPager_audio_play_button.setImageResource(R.drawable.ic_media_play);
				}
			}        	
        }
    }
}