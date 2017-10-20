package com.cw.litenote.operation.audio;

import com.cw.litenote.R;
import com.cw.litenote.note.Note;
import com.cw.litenote.util.Util;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;

/**
 * Audio Url verification task
 * - a class that will show progress bar in the main GUI context
 */

public class Async_audioUrlVerify extends AsyncTask<String,Integer,String>
{
	public ProgressDialog mUrlVerifyDialog;
	private FragmentActivity act;
	public Async_audioPrepare mAsyncTaskAudioPrepare;
    static boolean mIsOkUrl;

	Async_audioUrlVerify(FragmentActivity act)
	{
	    this.act = act;
	}
	 
	@Override
	protected void onPreExecute()
	{
	    super.onPreExecute();
	 	 // lock orientation
	 	 Util.lockOrientation(act);

	 	 // disable rotation
//	 	mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
	 	
		System.out.println("AudioUrlVerifyTask / onPreExecute" );

        mUrlVerifyDialog = new ProgressDialog(act);
        if (!Note.isPausedAtSeekerAnchor)
        {
            mUrlVerifyDialog.setMessage(act.getResources().getText(R.string.audio_message_searching_media));
            mUrlVerifyDialog.setCancelable(true); // set true for enabling Back button
            mUrlVerifyDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER); //ProgressDialog.STYLE_HORIZONTAL
            mUrlVerifyDialog.show();
        }

		AudioPlayer.mIsPrepared = false;
	}
	 
	@Override
	protected String doInBackground(String... params)
	{
	    int mProgress;
	    System.out.println("AudioUrlVerifyTask / doInBackground / params[0] = " + params[0] );
	    mProgress =0;
 	    // check if audio file exists or not
		String audioStr = AudioPlayer.mAudioInfo.getAudioStringAt(AudioPlayer.mAudioPos);
 		mIsOkUrl = false;
 		String scheme  = Util.getUriScheme(audioStr);
 		System.out.println("scheme = " + scheme + " / path = " + audioStr);
 		
 		// if scheme is https or http
 		boolean isUriExisted;
 		 
 		if(scheme == null)
 		    return  "ng";
 		 
 		if(scheme.equalsIgnoreCase("http")|| scheme.equalsIgnoreCase("https") )
 		{
		    if(Util.isNetworkConnected(act))
			{
		 	    isUriExisted = Util.isUriExisted(audioStr, act);
		 		System.out.println("AudioUrlVerifyTask / isUriExisted  = " + isUriExisted);
		 		if(isUriExisted)
		 		{
		 		    try
		 			{
		 			    boolean isEnd = false;
		 				int i = 0;
		 				while(!isEnd)
		 				{
		 				    // check if network connection is OK
		 					publishProgress(Integer.valueOf(mProgress));
		 					mProgress =+ 20;
		 					if(mProgress >= 100)
		 					    mProgress = 0;
 				         
		 					Util.tryUrlConnection(audioStr, act);
		 					// wait for response
		 					Thread.sleep(Util.oneSecond); //??? better idea?
 						
		 					// check response
		 					if(200 <= Util.mResponseCode && Util.mResponseCode <= 399)
		 					    mIsOkUrl =  true;
		 					else
		 					    mIsOkUrl =  false;
 						
		 					System.out.println("mIsOkUrl = " + mIsOkUrl +
		 					                   " / count = " + i);
		 					if(mIsOkUrl)
		 					    isEnd = true;
		 					else
		 					{
		 					    i++;
		 						if(i==5) //??? better idea?
		 						    isEnd = true; // no more try
		 					}
		 				}
		 			}
		 			catch (Exception e1)
		 			{
		 			    e1.printStackTrace();
		 			}
		 		}
			}
 		}
 		// if scheme is content or file
 		else if(scheme.equalsIgnoreCase("content") ||
 		    scheme.equalsIgnoreCase("file")    )
 		{
 		    String strName = null;
 			isUriExisted = Util.isUriExisted(audioStr, act);
	 		 
 			if(isUriExisted)
	 		    strName = Util.getDisplayNameByUriString(audioStr, act);
 			 
	 		if(!Util.isEmptyString(strName))
 			    mIsOkUrl = true;
 			else
 			    mIsOkUrl = false;
 		}
 		
 		System.out.println("Url mIsOkUrl = " + mIsOkUrl);

 		if(mIsOkUrl)
 		    return "ok";
 		else
 		    return "ng";
	}
	
	@Override
	protected void onProgressUpdate(Integer... progress)
	{
	    System.out.println("AudioUrlVerifyTask / OnProgressUpdate / progress[0] " + progress[0] );
	    super.onProgressUpdate(progress);
	    if(mUrlVerifyDialog != null)
	        mUrlVerifyDialog.setProgress(progress[0]);
	}
	 
	// This is executed in the context of the main GUI thread
    @Override
	protected void onPostExecute(String result)
	{
	    System.out.println("AudioUrlVerifyTask / onPostExecute / result = " + result);
		
	 	// dialog off
		if((mUrlVerifyDialog != null) && mUrlVerifyDialog.isShowing() )
			mUrlVerifyDialog.dismiss();

 		mUrlVerifyDialog = null;
	 }
}