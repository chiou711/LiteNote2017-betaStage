package com.cw.litenote.note;

import android.os.Bundle;

import com.cw.litenote.R;
import com.cw.litenote.util.Util;
import com.google.android.youtube.player.YouTubePlayerView;
import com.google.android.youtube.player.YouTubePlayer;

public class YouTubePlayerAct extends YouTubeFailureRecoveryActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.youtube_player);

        // set full screen
        Util.setFullScreen(this);
        if(getActionBar() != null)
           getActionBar().hide();

        // initialize YouTubeView
        YouTubePlayerView youTubeView = (YouTubePlayerView) findViewById(R.id.youtube_view);
        youTubeView.initialize(DeveloperKey.DEVELOPER_KEY, this);
    }

    @Override
    public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer youTubePlayer, boolean wasRestored) {
        if (!wasRestored) {
            String linkUri = getIntent().getExtras().getString("EXTRA_LINK_URI");

            // check Id string first
            String idStr = Util.getYoutubeId(linkUri);
            String listIdStr = Util.getYoutubeListId(linkUri);
            String playListIdStr = Util.getYoutubePlaylistId(linkUri);

            // only one Id, no list
            if(!Util.isEmptyString(idStr) &&
                Util.isEmptyString(listIdStr) &&
                Util.isEmptyString(playListIdStr) )
            {
                // manual start playing
//            youTubePlayer.cueVideo(idStr));

                // auto start playing
                youTubePlayer.loadVideo(idStr);
            }
            // random playlist (YouTube list)
            else if(!Util.isEmptyString(idStr) &&
                    !Util.isEmptyString(listIdStr) &&
                     Util.isEmptyString(playListIdStr) )
            {
                // manual start playing
//            youTubePlayer.cuePlaylist(listIdStr);

                // auto start playing
                youTubePlayer.loadPlaylist(listIdStr);
            }
            // ordered playlist (personal list)
            else if( Util.isEmptyString(idStr) &&
                     Util.isEmptyString(listIdStr) &&
                    !Util.isEmptyString(playListIdStr) )
            {
                // manual start playing
//            youTubePlayer.cuePlaylist(playListIdStr,0,0);

                // auto start playing
                youTubePlayer.loadPlaylist(playListIdStr,0,0);
            }
        }
    }

    @Override
    protected YouTubePlayer.Provider getYouTubePlayerProvider() {
        return (YouTubePlayerView) findViewById(R.id.youtube_view);
    }

}