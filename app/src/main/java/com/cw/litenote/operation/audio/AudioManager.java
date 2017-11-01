package com.cw.litenote.operation.audio;

import android.media.MediaPlayer;
import java.util.ArrayList;
import java.util.List;

import com.cw.litenote.page.Page;
import com.cw.litenote.db.DB_page;
import com.cw.litenote.util.Util;

public class AudioManager
{
	private static List<String> audioList;
	private static List<Integer> audioList_checked;
    static boolean mIsPrepared;

    private static int mAudioPlayMode;
    public final static int NOTE_PLAY_MODE = 0;
    public final static int PAGE_PLAY_MODE = 1;

    private static int mPlayerState;
    public static int PLAYER_AT_STOP = 0;
    public static int PLAYER_AT_PLAY = 1;
    public static int PLAYER_AT_PAUSE = 2;
    public static boolean isRunnableOn_note;
    public static boolean isRunnableOn_page;
    public static MediaPlayer mMediaPlayer; // plays the background music, if any
    public static int mAudioPos; // index of current media to play
//    public static Handler mAudioHandler; // used to update the slide show


    // constructor
   AudioManager()
   {
      audioList = new ArrayList<>();
      audioList_checked = new ArrayList<>();
   }

    /**
     * Setters and Getters
     *
     */
    // player state
    public static int getPlayerState() {
        return mPlayerState;
    }

    public static void setPlayerState(int playerState) {
        mPlayerState = playerState;
    }

    // Audio play mode
    public static int getAudioPlayMode() {
        return mAudioPlayMode;
    }

    public static void setAudioPlayMode(int audioPlayMode) {
        mAudioPlayMode = audioPlayMode;
    }

    public static List<String> getAudioList()
   {
      return audioList;
   }

    /**
     * Stop audio
     */
    public static void stopAudioPlayer()
    {
        System.out.println("AudioManager / _stopAudio");

        // stop media player
        if(AudioManager.mMediaPlayer != null) {
            if (AudioManager.mMediaPlayer.isPlaying()) {
                AudioManager.mMediaPlayer.pause();
                AudioManager.mMediaPlayer.stop();
            }
            AudioManager.mMediaPlayer.release();
            AudioManager.mMediaPlayer = null;
        }

        // stop handler and set flag to remove runnable
        if( AudioPlayer_page.mAudioHandler != null)
            AudioManager.isRunnableOn_page = false;
        else if(AudioPlayer_note.mAudioHandler != null)
            AudioManager.isRunnableOn_note = false;

        AudioManager.setPlayerState(AudioManager.PLAYER_AT_STOP);
    }


   // Get audio files count
   static int getAudioFilesCount()
   {
	   int size = 0; 
	   if(getAudioList()!= null)
	   {
		  for(int i=0;i< getAudioList().size();i++)
		  {
			  if( !Util.isEmptyString(audioList.get(i)) && (getCheckedAudio(i) == 1) )
				  size++;
		  }
	   }
//	   System.out.println( " AudioManager / getAudioFilesCount = " + size);
	   return size;
   }

   // Add audio to list
   private static void addAudio(String path)
   {
      audioList.add(path);
   }
   
//   public void setAudio(int i, String path)
//   {
//      audioList.set(i,path);
//   }

   // Add audio with marking to list
   private static void addCheckedAudio(int i)
   {
	   audioList_checked.add(i);
   }   
   
   private static void setCheckedAudio(int index, int marking)
   {
	   audioList_checked.set(index,marking);
   }

   public static int getCheckedAudio(int index)
   {
	   return  audioList_checked.get(index);
   }
   
//   public int getFirstAudioMarking()
//   {
//	   int first = 0;
//	   for(int i = 0;i < audioList_checked.size() ; i++ )
//	   {
//		   if( audioList_checked.get(i) == 1)
//			   first = Math.min(first,i);
//	   }
//	   return first;
//   }
   
   // return String at position index
   public static String getAudioStringAt(int index)
   {
      if (index >= 0 && index < audioList.size())
         return audioList.get(index);
      else
         return null;
   }
   
	// Update audio info
	void updateAudioInfo()
	{
		DB_page db_page = Page.mDb_page;
		
		db_page.open();
	 	// update media info 
	 	for(int i = 0; i< db_page.getNotesCount(false); i++)
	 	{
	 		String audioUri = db_page.getNoteAudioUri(i,false);
	 		
	 		// initialize
	 		addAudio(audioUri);
	 		addCheckedAudio(i);

	 		// set playable
	 		if( !Util.isEmptyString(audioUri)  && (db_page.getNoteMarking(i,false) == 1))
		 		setCheckedAudio(i,1);
	 		else
	 			setCheckedAudio(i,0);
	 	}
	 	db_page.close();
	}
	
}