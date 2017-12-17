package com.cw.litenote.page;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.cw.litenote.R;
import com.cw.litenote.operation.audio.AudioManager;
import com.cw.litenote.operation.audio.AudioPlayer_page;
import com.cw.litenote.util.Util;
import com.cw.litenote.util.audio.UtilAudio;

import java.util.Locale;

import static android.os.Build.VERSION_CODES.M;

/**
 * Created by cw on 2017/10/21.
 */

public class Page_audio {

    FragmentActivity mAct;
    View audio_panel;
    public TextView audioPanel_curr_pos;
    public TextView audio_panel_title_textView;
    public ImageView audioPanel_play_button;
    public SeekBar seekBarProgress;
    public static int mProgress;

    public Page_audio(FragmentActivity act)
    {
        this.mAct = act;

        // check permission first time, request phone permission
        if(Build.VERSION.SDK_INT >= M)//API23
        {
            int permissionPhone = ActivityCompat.checkSelfPermission(mAct, Manifest.permission.READ_PHONE_STATE);
            if(permissionPhone != PackageManager.PERMISSION_GRANTED)
            {
                ActivityCompat.requestPermissions(mAct,
                                                  new String[]{Manifest.permission.READ_PHONE_STATE},
                                                  Util.PERMISSIONS_REQUEST_PHONE);
            }
            else
                UtilAudio.setPhoneListener(mAct);
        }
        else
            UtilAudio.setPhoneListener(mAct);
    }


    /**
     * init audio block
     */
    public void initAudioBlock()
    {
        System.out.println("Page_audio / _initAudioBlock");

        audio_panel = mAct.findViewById(R.id.audio_panel);

        if(audio_panel == null)
            return;

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

        ImageView audioPanel_previous_btn = (ImageView) mAct.findViewById(R.id.audioPanel_previous);
        audioPanel_previous_btn.setImageResource(R.drawable.ic_media_previous);

        ImageView audioPanel_next_btn = (ImageView) mAct.findViewById(R.id.audioPanel_next);
        audioPanel_next_btn.setImageResource(R.drawable.ic_media_next);

        // text view for audio info
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

        // show audio file audio length of playing
        int media_length = AudioPlayer_page.media_file_length;
        System.out.println("Page_audio / _initAudioBlock / audioLen = " + media_length);
        int fileHour = Math.round((float)(media_length / 1000 / 60 / 60));
        int fileMin = Math.round((float)((media_length - fileHour * 60 * 60 * 1000) / 1000 / 60));
        int fileSec = Math.round((float)((media_length - fileHour * 60 * 60 * 1000 - fileMin * 1000 * 60 )/ 1000));
        String file_len_str =  String.format(Locale.US,"%2d", fileHour)+":" +
                String.format(Locale.US,"%02d", fileMin)+":" +
                String.format(Locale.US,"%02d", fileSec);
        audioPanel_file_length.setText(file_len_str);

        // show playing audio item message
        String message = mAct.getResources().getString(R.string.menu_button_play) +
                "#" +
                (AudioManager.mAudioPos +1);
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
                if( AudioManager.mMediaPlayer != null  )
                {
                    int mPlayAudioPosition = (int) (((float)(AudioPlayer_page.media_file_length / 100)) * seekBar.getProgress());
                    AudioManager.mMediaPlayer.seekTo(mPlayAudioPosition);
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
                    int currentPos = AudioPlayer_page.media_file_length *progress/(seekBar.getMax()+1);
                    int curHour = Math.round((float)(currentPos / 1000 / 60 / 60));
                    int curMin = Math.round((float)((currentPos - curHour * 60 * 60 * 1000) / 1000 / 60));
                    int curSec = Math.round((float)((currentPos - curHour * 60 * 60 * 1000 - curMin * 60 * 1000)/ 1000));
                    String curr_time_str = String.format(Locale.US,"%2d", curHour)+":" +
                        String.format(Locale.US,"%02d", curMin)+":" +
                        String.format(Locale.US,"%02d", curSec);
                    // set current play time
                    audioPanel_curr_pos.setText(curr_time_str);
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

                AudioPlayer_page audioPlayer_page = new AudioPlayer_page(mAct,Page_audio.this);
//                AudioPlayer_page.prepareAudioInfo();
                audioPlayer_page.runAudioState();

                // update status
                UtilAudio.updateAudioPanel((ImageView)v, audio_panel_title_textView); // here v is audio play button

                if(AudioManager.getPlayerState() != AudioManager.PLAYER_AT_STOP)
                    AudioPlayer_page.scrollHighlightAudioItemToVisible();
            }
        });

        // Audio play previous on click button listener
        audioPanel_previous_btn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                AudioPlayer_page.willPlayNext = false;

                AudioManager.mAudioPos--;
                if( AudioManager.mAudioPos < 0)
                    AudioManager.mAudioPos++; //back to first index

                while (AudioManager.getCheckedAudio(AudioManager.mAudioPos) == 0)
                {
                    AudioManager.mAudioPos--;
                    if( AudioManager.mAudioPos < 0)
                        AudioManager.mAudioPos++; //back to first index
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
                AudioPlayer_page.willPlayNext = true;

                AudioManager.mAudioPos++;
                if( AudioManager.mAudioPos >= AudioManager.getAudioList().size())
                    AudioManager.mAudioPos = 0; //back to first index

                while (AudioManager.getCheckedAudio(AudioManager.mAudioPos) == 0)
                {
                    AudioManager.mAudioPos++;
                }

                playNextAudio();
            }
        });
    }


    private void playNextAudio()
    {
        // cancel playing
        if(AudioManager.mMediaPlayer != null)
        {
            if(AudioManager.mMediaPlayer.isPlaying())
            {
                AudioManager.mMediaPlayer.pause();
            }

            AudioManager.mMediaPlayer.release();
            AudioManager.mMediaPlayer = null;
        }

        AudioPlayer_page audioPlayer_page = new AudioPlayer_page(mAct,this);
        initAudioBlock();

        // new audio player instance
        audioPlayer_page.runAudioState();

        // update status
        UtilAudio.updateAudioPanel(audioPanel_play_button, audio_panel_title_textView);

        if(AudioManager.getPlayerState() != AudioManager.PLAYER_AT_STOP)
            AudioPlayer_page.scrollHighlightAudioItemToVisible();
    }

}
