package com.cw.litenote.note;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.cw.litenote.R;
import com.cw.litenote.main.MainAct;
import com.cw.litenote.operation.audio.AudioManager;
import com.cw.litenote.operation.audio.AudioPlayer_note;
import com.cw.litenote.tabs.TabsHost;
import com.cw.litenote.util.ColorSet;
import com.cw.litenote.util.Util;
import com.cw.litenote.util.audio.UtilAudio;
import com.cw.litenote.util.preferences.Pref;

import java.util.Locale;

import static android.os.Build.VERSION_CODES.M;

/**
 * Created by cw on 2017/10/26.
 */

public class Note_audio
{
    private FragmentActivity act;
    private TextView audio_title;
    private ViewGroup audioBlock;
    private static String mAudioUriInDB;
    private ViewPager mPager;

    // constructor
    Note_audio(FragmentActivity act,String audioUriInDB)
    {
        this.act = act;
        mAudioUriInDB = audioUriInDB;
    }

    // initialize audio block
    void init_audio_block()
    {
        mPager = (ViewPager) act.findViewById(R.id.pager);

        // audio block
        TextView tag = (TextView) act.findViewById(R.id.text_view_audio);
        tag.setTextColor(ColorSet.color_white);

        audio_title = (TextView) act.findViewById(R.id.pager_audio_title); // first setting
        audio_title.setTextColor(ColorSet.color_white);
        if (Util.isLandscapeOrientation(act))
        {
            audio_title.setMovementMethod(new ScrollingMovementMethod());
            audio_title.scrollTo(0,0);
        }
        else
        {
            audio_title.setSingleLine(true);
            audio_title.setSelected(true);
        }

        audioBlock = (ViewGroup) act.findViewById(R.id.audioGroup);
        audioBlock.setBackgroundColor(ColorSet.color_black);

        mPager_audio_play_button = (ImageView) act.findViewById(R.id.pager_btn_audio_play);
    }

    // show audio block
    void showAudioBlock()
    {
        if(UtilAudio.hasAudioExtension(mAudioUriInDB))
        {
            audioBlock.setVisibility(View.VISIBLE);
            initAudioProgress(act,mAudioUriInDB,mPager);
        }
        else
            audioBlock.setVisibility(View.GONE);
    }

    // initialize audio progress
    public static void initAudioProgress(FragmentActivity act,String audioUriInDB,ViewPager _pager)
    {
        SeekBar seekBar = (SeekBar) act.findViewById(R.id.pager_img_audio_seek_bar);
        ImageView mPager_audio_play_button = (ImageView) act.findViewById(R.id.pager_btn_audio_play);
        setAudioBlockListener(act,audioUriInDB,_pager);

        mProgress = 0;

        mAudioUriInDB = audioUriInDB;
        showAudioName(act);

        TextView audioTitle = (TextView) act.findViewById(R.id.pager_audio_title);
        audioTitle.setSelected(false);
        mPager_audio_play_button.setImageResource(R.drawable.ic_media_play);
        audioTitle.setTextColor(ColorSet.getPauseColor(act));
        audioTitle.setSelected(false);

        // current position
        int curHour = Math.round((float)(mProgress / 1000 / 60 / 60));
        int curMin = Math.round((float)((mProgress - curHour * 60 * 60 * 1000) / 1000 / 60));
        int curSec = Math.round((float)((mProgress - curHour * 60 * 60 * 1000 - curMin * 60 * 1000)/ 1000));
        String curr_pos_str = String.format(Locale.ENGLISH,"%2d", curHour)+":" +
            String.format(Locale.ENGLISH,"%02d", curMin)+":" +
            String.format(Locale.ENGLISH,"%02d", curSec);

        TextView audio_curr_pos = (TextView) act.findViewById(R.id.pager_audio_current_pos);
        audio_curr_pos.setText(curr_pos_str);
        audio_curr_pos.setTextColor(ColorSet.color_white);

        // audio seek bar
        seekBar.setProgress(mProgress); // This math construction give a percentage of "was playing"/"song length"
        seekBar.setMax(99); // It means 100% .0-99
        seekBar.setVisibility(View.VISIBLE);

        // get audio file length
        try
        {
            MediaPlayer mp = MediaPlayer.create(act, Uri.parse(mAudioUriInDB));
            mediaFileLength = mp.getDuration();
            mp.release();
        }
        catch(Exception e)
        {
            System.out.println("Note_audio / _initAudioProgress / exception");
        }
        // set audio file length
        int fileHour = Math.round((float)(mediaFileLength / 1000 / 60 / 60));
        int fileMin = Math.round((float)((mediaFileLength - fileHour * 60 * 60 * 1000) / 1000 / 60));
        int fileSec = Math.round((float)((mediaFileLength - fileHour * 60 * 60 * 1000 - fileMin * 1000 * 60 )/ 1000));

        String strHour = String.format(Locale.ENGLISH,"%2d", fileHour);
        String strMinute = String.format(Locale.ENGLISH,"%02d", fileMin);
        String strSecond = String.format(Locale.ENGLISH,"%02d", fileSec);
        String strLength = strHour + ":" + strMinute+ ":" + strSecond;

        TextView audio_length = (TextView) act.findViewById(R.id.pager_audio_file_length);
        audio_length.setText(strLength);
        audio_length.setTextColor(ColorSet.color_white);
    }

    // show audio name
    static void showAudioName(FragmentActivity act)
    {
        TextView audio_title_text_view = (TextView) act.findViewById(R.id.pager_audio_title);
        // title: set marquee
        if(Util.isUriExisted(mAudioUriInDB, act)) {
            String audio_name = Util.getDisplayNameByUriString(mAudioUriInDB, act);
            audio_title_text_view.setText(audio_name);
        }
        else
            audio_title_text_view.setText(R.string.file_not_found);

        audio_title_text_view.setSelected(false);
    }

    // Set audio block
    public static ImageView mPager_audio_play_button;
    private static int mProgress;
    private static int mediaFileLength; // this value contains the song duration in milliseconds. Look at getDuration() method in MediaPlayer class

    public static boolean isPausedAtSeekerAnchor;
    public static int mAnchorPosition;

    // set audio block listener
    private static void setAudioBlockListener(final FragmentActivity act, final String audioStr, final ViewPager _pager)
    {
        SeekBar seekBarProgress = (SeekBar) act.findViewById(R.id.pager_img_audio_seek_bar);
        ImageView mPager_audio_play_button = (ImageView) act.findViewById(R.id.pager_btn_audio_play);

        // set audio play and pause control image
        mPager_audio_play_button.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {

                // check permission first time, request phone permission
                if(Build.VERSION.SDK_INT >= M)//API23
                {
                    int permissionPhone = ActivityCompat.checkSelfPermission(act, Manifest.permission.READ_PHONE_STATE);
                    if(permissionPhone != PackageManager.PERMISSION_GRANTED)
                    {
                        ActivityCompat.requestPermissions(act,
                                new String[]{Manifest.permission.READ_PHONE_STATE},
                                Util.PERMISSIONS_REQUEST_PHONE);
                    }
                    else
                        UtilAudio.setPhoneListener(act);
                }
                else
                    UtilAudio.setPhoneListener(act);

                isPausedAtSeekerAnchor = false;
                TabsHost.setAudioPlayingTab_WithHighlight(false);// in case playing audio in pager
                playAudioInPager(act,audioStr,_pager);
            }
        });

        // set seek bar listener
        seekBarProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar)
            {
                if( AudioManager.mMediaPlayer != null  )
                {
                    int mPlayAudioPosition = (int) (((float)(mediaFileLength / 100)) * seekBar.getProgress());
                    AudioManager.mMediaPlayer.seekTo(mPlayAudioPosition);
                }
                else
                {
                    // pause at seek bar anchor
                    isPausedAtSeekerAnchor = true;
                    mAnchorPosition = (int) (((float)(mediaFileLength / 100)) * seekBar.getProgress());
                    playAudioInPager(act,audioStr,_pager);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // audio player is one time mode in pager
                if(AudioManager.getAudioPlayMode() == AudioManager.PAGE_PLAY_MODE)
                    AudioManager.stopAudioPlayer();
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                if(fromUser)
                {
                    // show progress change
                    int currentPos = mediaFileLength *progress/(seekBar.getMax()+1);
                    int curHour = Math.round((float)(currentPos / 1000 / 60 / 60));
                    int curMin = Math.round((float)((currentPos - curHour * 60 * 60 * 1000) / 1000 / 60));
                    int curSec = Math.round((float)((currentPos - curHour * 60 * 60 * 1000 - curMin * 60 * 1000)/ 1000));
                    String curr_play_time_str = String.format(Locale.ENGLISH,"%2d", curHour)+":" +
                        String.format(Locale.ENGLISH,"%02d", curMin)+":" +
                        String.format(Locale.ENGLISH,"%02d", curSec);
                    // set current play time
                    TextView audio_curr_pos = (TextView) act.findViewById(R.id.pager_audio_current_pos);
                    audio_curr_pos.setText(curr_play_time_str);
                }
            }
        });

    }

    // play audio in pager
    private static void playAudioInPager(FragmentActivity act, String audioStr, ViewPager pager)
    {
        if(AudioManager.getAudioPlayMode()  == AudioManager.PAGE_PLAY_MODE)
            AudioManager.stopAudioPlayer();

        if(UtilAudio.hasAudioExtension(audioStr) ||
           UtilAudio.hasAudioExtension(Util.getDisplayNameByUriString(audioStr, act)))
        {
            AudioPlayer_note.mAudioPos = NoteUi.getFocus_notePos();

            // new instance
            if(AudioManager.mMediaPlayer == null)
                MainAct.mPlaying_pageTableId = Pref.getPref_focusView_page_tableId(act);
            else if((AudioManager.mMediaPlayer != null) &&
                    (AudioManager.getAudioPlayMode() == AudioManager.PAGE_PLAY_MODE))// If Audio player is NOT at One time mode and media exists
                AudioManager.stopAudioPlayer();

            AudioManager.setAudioPlayMode(AudioManager.NOTE_PLAY_MODE);

            AudioPlayer_note audioPlayer_note = new AudioPlayer_note(act,pager);
            AudioPlayer_note.prepareAudioInfo();
            audioPlayer_note.runAudioState();

            updateAudioPlayState(act);
        }
    }

    // update audio progress
    public static void updateAudioProgress(FragmentActivity act)
    {
        SeekBar seekBar = (SeekBar) act.findViewById(R.id.pager_img_audio_seek_bar);
        int currentPos=0;

        if(AudioManager.mMediaPlayer != null)
            currentPos = AudioManager.mMediaPlayer.getCurrentPosition();

        int curHour = Math.round((float)(currentPos / 1000 / 60 / 60));
        int curMin = Math.round((float)((currentPos - curHour * 60 * 60 * 1000) / 1000 / 60));
        int curSec = Math.round((float)((currentPos - curHour * 60 * 60 * 1000 - curMin * 60 * 1000)/ 1000));
        String curr_time_str = String.format(Locale.ENGLISH,"%2d", curHour)+":" +
            String.format(Locale.ENGLISH,"%02d", curMin)+":" +
            String.format(Locale.ENGLISH,"%02d", curSec);
        TextView audio_curr_pos = (TextView) act.findViewById(R.id.pager_audio_current_pos);
        // set current play time and the play length of audio file
        if(audio_curr_pos != null)
        {
            audio_curr_pos.setText(curr_time_str);
        }

        mProgress = (int)(((float)currentPos/ mediaFileLength)*100);

        if(seekBar != null)
            seekBar.setProgress(mProgress); // This math construction give a percentage of "was playing"/"song length"
    }

    // update audio play state
    public static void updateAudioPlayState(FragmentActivity act)
    {
        ImageView audio_play_btn = (ImageView) act.findViewById(R.id.pager_btn_audio_play);

        if(AudioManager.getAudioPlayMode() != AudioManager.NOTE_PLAY_MODE)
            return;

        TextView audioTitle = (TextView) act.findViewById(R.id.pager_audio_title);
        // update playing state
        if(AudioManager.getPlayerState() == AudioManager.PLAYER_AT_PLAY)
        {
            audio_play_btn.setImageResource(R.drawable.ic_media_pause);
            showAudioName(act);
            audioTitle.setTextColor(ColorSet.getHighlightColor(act) );
            audioTitle.setSelected(true);
        }
        else if( (AudioManager.getPlayerState() == AudioManager.PLAYER_AT_PAUSE) ||
                (AudioManager.getPlayerState() == AudioManager.PLAYER_AT_STOP)    )
        {
            audio_play_btn.setImageResource(R.drawable.ic_media_play);
            showAudioName(act);
            audioTitle.setTextColor(ColorSet.getPauseColor(act));
            audioTitle.setSelected(false);
        }
    }

}
