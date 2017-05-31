package com.cw.litenote.util.image;

import com.cw.litenote.main.MainAct;
import com.cw.litenote.R;
import com.cw.litenote.util.UilCommon;
import com.cw.litenote.util.Util;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class SlideshowPlayer extends FragmentActivity
{
	private String STATE_SLIDE_INDEX = "STATE_SLIDE_INDEX";
	private int slideIndex; // index of the next image to display
	private static int switch_time;
	private ImageView imageView; // displays the current image
	private TextView textView; // displays the current text

	private SlideshowInfo slideshow; // slide show being played
	private Handler slideHandler; // used to update the slide show
	private BroadcastReceiver mReceiver;
   
	// initializes the SlideshowPlayer Activity
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.slideshow_player);
      
		UilCommon.init();
		Util.setFullScreen(this);
		
		// disable screen saving
		getWindow().addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);

		// disable key guard
		getWindow().addFlags(LayoutParams.FLAG_DISMISS_KEYGUARD);
      
		System.out.println("SlideshowPlayer / _onCreate ");
		if (savedInstanceState == null) // Activity starting
		{
			System.out.println("_onCreate / savedInstanceState == null"); 
			slideIndex = 0; // start from first image
		}
		else // Activity resuming
		{
			slideIndex = savedInstanceState.getInt(STATE_SLIDE_INDEX);
		}       
      
		// get SlideshowInfo for slideshow to play
		slideshow = MainAct.slideshowInfo;
   	  	slideHandler = new Handler(); // create handler to control slideshow

   	  	IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
   	  	filter.addAction(Intent.ACTION_SCREEN_OFF);
   	  	mReceiver = new SlideshowScreenReceiver();
   	  	registerReceiver(mReceiver, filter);

		SharedPreferences pref_sw_time = MainAct.mAct.getSharedPreferences("slideshow_sw_time", 0);
		switch_time = Integer.valueOf(pref_sw_time.getString("KEY_SLIDESHOW_SW_TIME","5"));
	}
   
	@Override
	protected void onRestart()
	{
//		System.out.println("SlideshowPlayer / onRestart ");
		super.onRestart();
	}   

   
	// called after onCreate and sometimes onStop
	@Override
	protected void onStart()
	{
//		System.out.println("SlideshowPlayer / onStart ");
		super.onStart();
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState)
	{
//		System.out.println("SlideshowPlayer / onRestoreInstanceState ");
		super.onRestoreInstanceState(savedInstanceState);
		slideIndex = savedInstanceState.getInt(STATE_SLIDE_INDEX);
	}
   
	// called after onStart or onPause
	@Override
	protected void onResume()
	{
//		System.out.println("SlideshowPlayer / onResume ");
		super.onResume();
   	  	slideHandler.post(runSlideshow); // post updateSlideshow to execute
	}

	// called when the Activity is paused
	@Override
	protected void onPause()
	{
//		System.out.println("SlideshowPlayer / onPause ");
		super.onPause();
   	  	slideHandler.removeCallbacks(runSlideshow);
	}

	// save slide show state so it can be restored in onCreate
	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
//		System.out.println("SlideshowPlayer / onSaveInstanceState ");
		super.onSaveInstanceState(outState);
		// save nextItemIndex and slideshowName
		slideIndex--;
		if(slideIndex <0)
			slideIndex =0;
		outState.putInt(STATE_SLIDE_INDEX, slideIndex);
	}    
   
	// called when the Activity stops
	@Override
	protected void onStop()
	{
//		System.out.println("SlideshowPlayer / onStop ");
		super.onStop();
	}

	// called when the Activity is destroyed
	@Override
	protected void onDestroy()
	{
		System.out.println("SlideshowPlayer / onDestroy ");
		super.onDestroy();
		unregisterReceiver(mReceiver);
	}


	boolean bEnablePlay = true;
	boolean bShowPause = false;
	Toast toast;
	// Runnable: runSlideshow
	private Runnable runSlideshow = new Runnable()
	{
		@Override
		public void run()
		{
			if(slideIndex >= slideshow.imageSize())
				slideIndex = 0;

			String uriStr = slideshow.getImageAt(slideIndex);
			String text = slideshow.getTextAt(slideIndex);
			System.out.println(" Runnable updateSlideshow / slideIndex = " + slideIndex);

			// check if Uri exists
			boolean uriOK;
			if(UtilImage.hasImageExtension(uriStr,SlideshowPlayer.this))
				uriOK = Util.isUriExisted(Uri.parse(uriStr).toString(), SlideshowPlayer.this);
			else
				uriOK = false;

			if(uriOK)
			{
				// image
				imageView = (ImageView) findViewById(R.id.slideshow_image);
				imageView.setOnClickListener(new View.OnClickListener() {
					public void onClick(View view) {
						bEnablePlay = !bEnablePlay;

						if(!bEnablePlay) {
							if((toast != null) &&toast.getView().isShown())
								toast.cancel();

							toast = Toast.makeText(SlideshowPlayer.this, R.string.toast_pause, Toast.LENGTH_SHORT);
							toast.show();
						}
						else {
							if((toast != null) && toast.getView().isShown())
								toast.cancel();

							toast = Toast.makeText(SlideshowPlayer.this, R.string.toast_play, Toast.LENGTH_SHORT);
							toast.show();

							slideHandler.removeCallbacks(runSlideshow);
							slideHandler.post(runSlideshow);
						}
					}
				});

				if(bEnablePlay) {
					UilCommon.imageLoader.displayImage(Uri.parse(uriStr).toString(),
							imageView,
							UilCommon.optionsForFadeIn,
							UilCommon.animateFirstListener);

					// text
					textView = (TextView) findViewById(R.id.slideshow_text);
					if (!Util.isEmptyString(text)) {
						textView.setVisibility(View.VISIBLE);
						textView.setText(text);
					} else
						textView.setVisibility(View.GONE);

					slideIndex++;

					bShowPause = false;
				}
				slideHandler.postDelayed(runSlideshow, switch_time * 1000);
			}
			else
			{
				slideIndex++;
				slideHandler.post(runSlideshow); // go to display next instantly
			}
		}
	}; 
}