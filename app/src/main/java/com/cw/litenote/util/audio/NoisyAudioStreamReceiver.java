package com.cw.litenote.util.audio;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;

import com.cw.litenote.R;
import com.cw.litenote.note.Note_audio;
import com.cw.litenote.operation.audio.AudioInfo;
import com.cw.litenote.operation.audio.AudioPlayer_page;
import com.cw.litenote.page.Page_audio;

// for earphone jack connection on/off
public class NoisyAudioStreamReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent)
	{
        if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction()))
		{
			if((AudioInfo.mMediaPlayer != null) && AudioInfo.mMediaPlayer.isPlaying() )
			{
				System.out.println("NoisyAudioStreamReceiver / play -> pause");
                AudioInfo.mMediaPlayer.pause();
                AudioInfo.isRunnableOn_note = false;
                AudioInfo.isRunnableOn_page = false;
                AudioInfo.setPlayerState(AudioInfo.PLAYER_AT_PAUSE);

                if(AudioInfo.getAudioPlayMode() == AudioInfo.CONTINUE_MODE) {
                    UtilAudio.updateAudioPanel(Page_audio.audioPanel_play_button, Page_audio.audio_panel_title_textView);

                    if (AudioInfo.getPlayerState() != AudioInfo.PLAYER_AT_STOP)
                        AudioPlayer_page.scrollHighlightAudioItemToVisible();
                }

				//update audio play button in pager
				if( (Note_audio.mPager_audio_play_button != null) &&
					Note_audio.mPager_audio_play_button.isShown()    )
				{
					Note_audio.mPager_audio_play_button.setImageResource(R.drawable.ic_media_play);
				}
			}
        }
    }
}