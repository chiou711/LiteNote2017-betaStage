package com.cw.litenote.operation;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.cw.litenote.R;
import com.cw.litenote.db.DB_page;
import com.cw.litenote.note.Note;
import com.cw.litenote.util.Util;
import com.google.android.youtube.player.YouTubePlayerView;
import com.google.android.youtube.player.YouTubePlayer;

public class YouTubePlayerAct extends YouTubeFailureRecoveryActivity
{
    YouTubeFailureRecoveryActivity act;
    Button previous_btn,next_btn;
    View btn_group;
    boolean bShow_landscape_prev_next_control;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.youtube_player);
        act = this;

        // initial: control is seen
        bShow_landscape_prev_next_control = true;

        if(getActionBar() != null)
           getActionBar().hide();

        // initialize YouTubeView
        YouTubePlayerView youTubeView = (YouTubePlayerView) findViewById(R.id.youtube_view);
        youTubeView.initialize(YouTubeDeveloperKey.DEVELOPER_KEY, this);

        btn_group = findViewById(R.id.youtube_control);
        // image: previous button
        previous_btn = (Button) findViewById(R.id.btn_previous);
        previous_btn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_media_previous, 0, 0, 0);

        // click to previous
        previous_btn.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View view) {
                Note.mCurrentPosition--;
                // leftmost boundary check
                if(Note.mCurrentPosition <0) {
                    Note.mCurrentPosition++;
                    Toast.makeText(act,R.string.toast_leftmost,Toast.LENGTH_SHORT).show();
                }
                else
                    prepare_play_YouTube(youTube_player);
            }
        });

        // image: next button
        next_btn = (Button) findViewById(R.id.btn_next);
        next_btn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_media_next, 0, 0, 0);
        // click to next
        next_btn.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View view) {
                Note.mCurrentPosition++;
                // rightmost boundary check
                if(Note.mCurrentPosition >= Note.mPagerAdapter.getCount()) {
                    Note.mCurrentPosition--;
                    Toast.makeText(act,R.string.toast_rightmost,Toast.LENGTH_SHORT).show();
                }
                else
                    prepare_play_YouTube(youTube_player);
            }
        });
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        bShow_landscape_prev_next_control = false;
        setLayout();
    }

    YouTubePlayer youTube_player;
    @Override
    public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer youTubePlayer, boolean wasRestored) {
        youTube_player = youTubePlayer;

        youTube_player.setOnFullscreenListener(new YouTubePlayer.OnFullscreenListener() {
            @Override
            public void onFullscreen(boolean isFullScreen) {
                System.out.println("YouTubePlayerAct / _onInitializationSuccess / setOnFullscreenListener / isFullScreen =" + isFullScreen);

                bShow_landscape_prev_next_control = !isFullScreen;

                if (!isFullScreen && Util.isLandscapeOrientation(act)) {
                    Util.setNotFullScreen(act);
                    youTube_player.setFullscreen(false);
                    btn_group.setVisibility(View.VISIBLE);

                    previous_btn.setVisibility(View.VISIBLE);
                    previous_btn.setAlpha(Note.mCurrentPosition == 0?0.3f:1.0f);

                    next_btn.setVisibility(View.VISIBLE);
                    next_btn.setAlpha(Note.mCurrentPosition == (Note.mPagerAdapter.getCount() - 1)?0.3f:1.0f);

                    bShow_landscape_prev_next_control = true;
                }
            }
        });

        if (!wasRestored) {
            prepare_play_YouTube(youTube_player);
        }
    }

    @Override
    protected YouTubePlayer.Provider getYouTubePlayerProvider() {
        return (YouTubePlayerView) findViewById(R.id.youtube_view);
    }


    /**
     *  Prepare to play YouTube
     * @param youTubePlayer
     */
    void prepare_play_YouTube(YouTubePlayer youTubePlayer)
    {
        // set interface, cf. .CHROMELESS: no interface, .MINIMAL: only play/pause button
        YouTubePlayer.PlayerStyle style = YouTubePlayer.PlayerStyle.DEFAULT;
        youTubePlayer.setPlayerStyle(style);

        setLayout();

        youTubePlayer.setShowFullscreenButton(true);

        DB_page db_page = new DB_page(act,Util.getPref_lastTimeView_page_tableId(act));
        String linkUri = db_page.getNoteLinkUri(Note.mCurrentPosition,true);
        System.out.println("YouTubePlayerAct / _prepare_play_YouTube / linkUri = " + linkUri);

        // check Id string first
        String idStr = Util.getYoutubeId(linkUri);
        String listIdStr = Util.getYoutubeListId(linkUri);
        String playListIdStr = Util.getYoutubePlaylistId(linkUri);

        // only v
        if(!Util.isEmptyString(idStr) &&
           Util.isEmptyString(listIdStr) &&
           Util.isEmptyString(playListIdStr) )
        {
            // auto start playing
            youTubePlayer.loadVideo(idStr);// cf. _cueVideo for manual start
        }
        // v and list
        else if(!Util.isEmptyString(idStr) &&
                !Util.isEmptyString(listIdStr) &&
                Util.isEmptyString(playListIdStr) )
        {
            // auto start playing
            youTubePlayer.loadPlaylist(listIdStr); // cf. _cuePlaylist for manual start
        }
        // playlist
        else if(Util.isEmptyString(idStr) &&
                Util.isEmptyString(listIdStr) &&
                !Util.isEmptyString(playListIdStr) )
        {
            // auto start playing
            youTubePlayer.loadPlaylist(playListIdStr,0,0); // cf. _cuePlaylist for manual start
        }
        else
            Toast.makeText(act,R.string.toast_no_link_found,Toast.LENGTH_SHORT).show();

    }

    /**
     *  set layout
     */
    void setLayout()
    {
        // full screen
        if(Util.isLandscapeOrientation(act)) {
            if(!bShow_landscape_prev_next_control) {

                Util.setFullScreen(this);
                youTube_player.setFullscreen(true);

                btn_group.setVisibility(View.GONE);
                previous_btn.setVisibility(View.GONE);
                next_btn.setVisibility(View.GONE);
            }
            else
            {
                Util.setNotFullScreen(this);
                youTube_player.setFullscreen(false);
                btn_group.setVisibility(View.VISIBLE);

                previous_btn.setVisibility(View.VISIBLE);
                previous_btn.setAlpha(Note.mCurrentPosition == 0?0.3f:1.0f);

                next_btn.setVisibility(View.VISIBLE);
                next_btn.setAlpha(Note.mCurrentPosition == (Note.mPagerAdapter.getCount() - 1)?0.3f:1.0f);
            }
        }
        // not full screen
        else {
            Util.setNotFullScreen(this);
            youTube_player.setFullscreen(false);
            btn_group.setVisibility(View.VISIBLE);

            previous_btn.setVisibility(View.VISIBLE);
            previous_btn.setAlpha(Note.mCurrentPosition == 0?0.3f:1.0f);

            next_btn.setVisibility(View.VISIBLE);
            next_btn.setAlpha(Note.mCurrentPosition == (Note.mPagerAdapter.getCount() - 1)?0.3f:1.0f);
        }
    }
}