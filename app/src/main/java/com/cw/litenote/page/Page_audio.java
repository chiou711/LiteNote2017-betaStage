package com.cw.litenote.page;

import android.support.v4.app.FragmentActivity;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.cw.litenote.R;
import com.cw.litenote.operation.audio.AudioInfo;
import com.cw.litenote.operation.audio.AudioPlayer;
import com.cw.litenote.util.Util;
import com.cw.litenote.util.audio.UtilAudio;

import java.util.Locale;

/**
 * Created by cw on 2017/10/21.
 */

public class Page_audio {

    FragmentActivity mAct;
    View audio_panel;
    public TextView audioPanel_curr_pos;
    public static TextView audio_panel_title_textView;
    public static ImageView audioPanel_play_button;
    public SeekBar seekBarProgress;
    public static int mProgress;

    public Page_audio(FragmentActivity act)
    {
        this.mAct = act;
    }

    /**
     * init audio block
     */
    public void initAudioBlock()
    {
        System.out.println("Page_audio / _initAudioBlock");

        audio_panel = mAct.findViewById(R.id.audio_panel);
        audio_panel_title_textView = (TextView) audio_panel.findViewById(R.id.audio_panel_title);

        // scroll audio title to start position at landscape orientation
        // marquee of audio title is enabled for Portrait, not Landscape
        if (Util.isLandscapeOrientation(mAct))
        {
            audio_panel_title_textView.setMovementMethod(new ScrollingMovementMethod());
            audio_panel_title_textView.scrollTo(0,0);
        }
        else {
            // set marquee
            audio_panel_title_textView.setSingleLine(true);
            audio_panel_title_textView.setSelected(true);
        }

        // update play button status
        audioPanel_play_button = (ImageView) mAct.findViewById(R.id.audioPanel_play);
        UtilAudio.updateAudioPanel(audioPanel_play_button, audio_panel_title_textView);

        ImageView audioPanel_previous_btn = (ImageView) mAct.findViewById(R.id.audioPanel_previous);
        audioPanel_previous_btn.setImageResource(R.drawable.ic_media_previous);

        ImageView audioPanel_next_btn = (ImageView) mAct.findViewById(R.id.audioPanel_next);
        audioPanel_next_btn.setImageResource(R.drawable.ic_media_next);

        audioPanel_curr_pos = (TextView) mAct.findViewById(R.id.audioPanel_current_pos);
        TextView audioPanel_file_length = (TextView) mAct.findViewById(R.id.audioPanel_file_length);
        TextView audioPanel_audio_number = (TextView) mAct.findViewById(R.id.audioPanel_audio_number);

        // init audio seek bar
        seekBarProgress = (SeekBar)mAct.findViewById(R.id.audioPanel_seek_bar);
        seekBarProgress.setMax(99); // It means 100% .0-99
        seekBarProgress.setProgress(mProgress);

        // seek bar behavior is not like other control item
        //, it is seen when changing drawer, so set invisible at xml
        seekBarProgress.setVisibility(View.VISIBLE);

        int media_length = AudioPlayer.media_file_length;
        System.out.println("Page_audio / _initAudioBlock / audioLen = " + media_length);
        // show audio file audioLen of playing
        int fileHour = Math.round((float)(media_length / 1000 / 60 / 60));
        int fileMin = Math.round((float)((media_length - fileHour * 60 * 60 * 1000) / 1000 / 60));
        int fileSec = Math.round((float)((media_length - fileHour * 60 * 60 * 1000 - fileMin * 1000 * 60 )/ 1000));
        audioPanel_file_length.setText( String.format(Locale.US,"%2d", fileHour)+":" +
                String.format(Locale.US,"%02d", fileMin)+":" +
                String.format(Locale.US,"%02d", fileSec)         );

        // show playing audio item message
        String message = mAct.getResources().getString(R.string.menu_button_play) +
                "#" +
                (AudioPlayer.mAudioPos +1);
        audioPanel_audio_number.setText(message);

        //
        // Set up listeners
        //

        // Seek bar listener
        seekBarProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar)
            {
                if( AudioPlayer.mMediaPlayer != null  )
                {
                    int mPlayAudioPosition = (int) (((float)(AudioPlayer.media_file_length / 100)) * seekBar.getProgress());
                    AudioPlayer.mMediaPlayer.seekTo(mPlayAudioPosition);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                if(fromUser)
                {
                    // show progress change
                    int currentPos = AudioPlayer.media_file_length *progress/(seekBar.getMax()+1);
                    int curHour = Math.round((float)(currentPos / 1000 / 60 / 60));
                    int curMin = Math.round((float)((currentPos - curHour * 60 * 60 * 1000) / 1000 / 60));
                    int curSec = Math.round((float)((currentPos - curHour * 60 * 60 * 1000 - curMin * 60 * 1000)/ 1000));

                    // set current play time
                    audioPanel_curr_pos.setText(String.format(Locale.US,"%2d", curHour)+":" +
                            String.format(Locale.US,"%02d", curMin)+":" +
                            String.format(Locale.US,"%02d", curSec) );
                }
            }
        });

        // Audio play and pause button on click listener
        audioPanel_play_button.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {

                initAudioBlock();

                AudioPlayer audioPlayer = new AudioPlayer(mAct,Page_audio.this);
                AudioPlayer.prepareAudioInfo();
                audioPlayer.runAudioState();

                // update status
                UtilAudio.updateAudioPanel((ImageView)v, audio_panel_title_textView); // here v is audio play button
                if(AudioPlayer.getPlayerState() != AudioPlayer.PLAYER_AT_STOP)
                    AudioPlayer.scrollHighlightAudioItemToVisible();
            }
        });

        // Audio play previous on click button listener
        audioPanel_previous_btn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                AudioPlayer.willPlayNext = false;

                AudioPlayer.mAudioPos--;
                if( AudioPlayer.mAudioPos < 0)
                    AudioPlayer.mAudioPos++; //back to first index

                while (AudioInfo.getCheckedAudio(AudioPlayer.mAudioPos) == 0)
                {
                    AudioPlayer.mAudioPos--;
                    if( AudioPlayer.mAudioPos < 0)
                        AudioPlayer.mAudioPos++; //back to first index
                }

                playNextAudio();
            }
        });

        // Audio play next on click button listener
        audioPanel_next_btn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                AudioPlayer.willPlayNext = true;

                AudioPlayer.mAudioPos++;
                if( AudioPlayer.mAudioPos >= AudioInfo.getAudioList().size())
                    AudioPlayer.mAudioPos = 0; //back to first index

                while (AudioInfo.getCheckedAudio(AudioPlayer.mAudioPos) == 0)
                {
                    AudioPlayer.mAudioPos++;
                }

                playNextAudio();
            }
        });
    }


    private void playNextAudio()
    {
        // cancel playing
        if(AudioPlayer.mMediaPlayer != null)
        {
            if(AudioPlayer.mMediaPlayer.isPlaying())
            {
                AudioPlayer.mMediaPlayer.pause();
            }

            AudioPlayer.mMediaPlayer.release();
            AudioPlayer.mMediaPlayer = null;
        }

        AudioPlayer audioPlayer = new AudioPlayer(mAct,this);
        initAudioBlock();

        // update status
        UtilAudio.updateAudioPanel(audioPanel_play_button, audio_panel_title_textView);

        // new audio player instance
        audioPlayer.runAudioState();

        if(AudioPlayer.getPlayerState() != AudioPlayer.PLAYER_AT_STOP)
            AudioPlayer.scrollHighlightAudioItemToVisible();
    }

}