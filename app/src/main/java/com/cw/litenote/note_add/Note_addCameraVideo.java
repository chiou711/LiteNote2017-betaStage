package com.cw.litenote.note_add;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import com.cw.litenote.note_common.Note_common;
import com.cw.litenote.page.Page;
import com.cw.litenote.R;
import com.cw.litenote.db.DB_page;
import com.cw.litenote.util.Util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Toast;

/*
 * Note: 
 * 	cameraVideoUri: used to show in confirmation Continue dialog
 *  	Two conditions:
 *  	1. is got after taking picture
 *  	2. is kept during rotation
 * 
 *  UtilImage.bShowExpandedImage: used to control DB saving state
 * 
 *  Note_common: used to do DB operation
 */
public class Note_addCameraVideo extends Activity {

    Long noteId;
    String cameraVideoUri;
    Note_common note_common;
    boolean enSaveDb;
	String videoUriInDB;
	private DB_page dB;
	final int TAKE_VIDEO_ACT = 1;
	private Uri videoUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        System.out.println("Note_addCameraVideo / onCreate");
        
        note_common = new Note_common(this);
        videoUriInDB = "";
        cameraVideoUri = "";
        enSaveDb = true;
        
        // get row Id from saved instance
        noteId = (savedInstanceState == null) ? null :
            (Long) savedInstanceState.getSerializable(DB_page.KEY_NOTE_ID);
        
        // get picture Uri in DB if instance is not null
        dB = Page.mDb_page;
        if(savedInstanceState != null)
        {
	        System.out.println("Note_addCameraVideo / onCreate / noteId =  " + noteId);
	        if(noteId != null)
	        	videoUriInDB = dB.getNotePictureUri_byId(noteId);
        }
        
        // at the first beginning
        if(savedInstanceState == null)
    	    takeVideoWithName();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
    }
    
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState)
    {
    	super.onRestoreInstanceState(savedInstanceState);
    	cameraVideoUri = savedInstanceState.getString("showCameraPictureUri");
    }

    // for Add new picture (stage 1)
    // for Rotate screen (stage 2)
    @Override
    protected void onPause() {
        super.onPause();
       	System.out.println("Note_addCameraVideo / onPause / keep pictureUriInDB");
       	noteId = note_common.savePictureStateInDB(noteId, enSaveDb, videoUriInDB, "", "", "");
    }

    // for Add new picture (stage 2)
    // for Rotate screen (stage 2)
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
   	 	System.out.println("Note_addCameraVideo / onSaveInstanceState");
        outState.putSerializable(DB_page.KEY_NOTE_ID, noteId);
    }
    
    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
	    enSaveDb = false;
	    finish();
    }
    
    
    // Create temporary image file
    private File createTempVideoFile() throws IOException 
    {
		// First, create a sub-directory named App name under DCIM if needed 
        File videoDir = Util.getPicturesDir(this);
		if(!videoDir.isDirectory())
			videoDir.mkdir();        
		
		// note: createTempFile will generate random number and a 0 bit file size instance first
        // Create an video file name
//      String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
		String videoFileName = "VID_" + Util.getCurrentTimeString();
		File videoFile = new File(videoDir /* directory */,
        						  videoFileName  /* prefix */ +
        						  ".mp4" 		 /* suffix */);
        
        System.out.println("+++ _createTempVideoFile / videoFile path = " + videoFile.getPath());
        return videoFile;
    }
    
    private void takeVideoWithName() 
    {
        Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);

        // Ensure that there's a camera activity to handle the intent
        if (takeVideoIntent.resolveActivity(getPackageManager()) != null) 
        {
            // Create temporary image File where the photo will save in
            File tempFile = null;
            try 
            {
                tempFile = createTempVideoFile();
            } 
            catch (IOException ex)
            {
                // Error occurred while creating the File
            }
            
            // Continue only if the File was successfully created
            if (tempFile != null) 
            {
            	videoUri = Uri.fromFile(tempFile); // so far, file size is 0
                takeVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT, videoUri); // appoint Uri for captured image
                videoUriInDB = videoUri.toString();
                startActivityForResult(takeVideoIntent, TAKE_VIDEO_ACT);
            }
        }
    }   
    
    // On Activity Result
	protected void onActivityResult(int requestCode, int resultCode, Intent videoReturnedIntent) 
	{
		System.out.println("Note_addCameraVideo / onActivityResult");
		if (requestCode == TAKE_VIDEO_ACT)
		{
			if (resultCode == Activity.RESULT_OK)
			{
				// disable Rotate to avoid leak window
//				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
				
				// Note: 
				// for Google Camera App, imageReturnedIntent is null
				// default path of Google camera is /sdcard/DCIM/Camera 
				
				// check returned intent
				Uri intentVideoUri = null;
				if(videoReturnedIntent == null)
				{
					System.out.println("returned intent is null");
				}
				else
				{
					intentVideoUri = videoReturnedIntent.getData();
					
					if(intentVideoUri == null)
						System.out.println("-- videoUri = " + null);
					else
						System.out.println("-- videoUri = " + intentVideoUri.toString());
				}
				
				
				// set for Rotate any times
		        if(noteId != null)
		        {
		        	cameraVideoUri = dB.getNotePictureUri_byId(noteId);
		        }

		        // Add for Sony, the file size is 0 for given file name by putExtra 
				if(videoReturnedIntent != null )
				{
					Uri uri = Uri.parse(cameraVideoUri);
				    File file = new File(uri.getPath());
				    
				    // update file name by returned intent
				    if(file.length() == 0)
				    {
				    	System.out.println("--- file size = 0");
				    	String path = Util.getLocalRealPathByUri(Note_addCameraVideo.this,intentVideoUri);
				    	videoUriInDB = "file://" + path;
				    	enSaveDb = true;
				       	noteId = note_common.savePictureStateInDB(noteId, enSaveDb, videoUriInDB, "", "", "");
				    	enSaveDb = false;
				    }
				}
		        
    			if( getIntent().getExtras().getString("extra_ADD_NEW_TO_TOP", "false").equalsIgnoreCase("true") &&
    				(note_common.getCount() > 0) )
		               Page.swap(Page.mDb_page);
    			
    			Toast.makeText(this, R.string.toast_saved , Toast.LENGTH_SHORT).show();

				// check and delete duplicated image file in 100ANDRO (Sony) / 100MEDIA (hTC)
				int lastContentId = getLastCapturedVideoId(this);
				handleDuplicatedVideo(this, lastContentId);
    			
	  		    noteId = null; // set null for Insert
	  		    takeVideoWithName();
			} 
			else if (resultCode == RESULT_CANCELED)
			{
				// hide action bar
				getActionBar().hide();
				
				// set background to transparent
				getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
				
				Toast.makeText(this, R.string.note_cancel_add_new, Toast.LENGTH_LONG).show();
				
				// delete the temporary note in DB
                note_common.deleteNote(noteId);
                enSaveDb = false;
                
                // When auto time out of taking picture App happens, 
            	// Note_addCameraVideo activity will start from onCreate,
                // at this case, mImageUri is null
                if(videoUri != null)
                {
	           		File tempFile = new File(videoUri.getPath());
	        		if(tempFile.isFile())
	        		{
	                    // delete 0 bit temporary file
	        			tempFile.delete();
	        			System.out.println("temp 0 bit file is deleted");
	        		}
                }
                finish();
                return; // must add this
			}
			
		}
	}

	public void handleDuplicatedVideo(Context context, int lastContentId)
	{
	    /*
	     * Checking for duplicate images
	     * This is necessary because some camera implementation not only save where you want them to save but also in their default location.
	     */
	    if (lastContentId == 0)
	        return;
	    
	    final String[] projection = {MediaStore.Video.VideoColumns.DATA,
	    							 MediaStore.Video.VideoColumns.DATE_TAKEN,
	    							 MediaStore.Video.VideoColumns.SIZE,
	    							 MediaStore.Video.VideoColumns._ID};
	    final String videoWhere = MediaStore.Video.Media._ID + "=?";
	    final String[] videoArguments = {Integer.toString(lastContentId)};
	    final String videoOrderBy = MediaStore.Video.Media._ID + " DESC";
	    
	    Cursor videoCursor = context.getContentResolver()
	    							.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
	    								   projection,
	    								   videoWhere,
	    								   videoArguments,
	    								   videoOrderBy);
	    
	    // last file: file1
	    // new file: file2
		String path1 = null;
	    File file1 = null;
	    long dateTaken = 0;
	    if (videoCursor.getCount() > 0) 
	    {
	        videoCursor.moveToFirst(); // newest one
	        path1 = videoCursor.getString(videoCursor.getColumnIndex(MediaStore.Video.Media.DATA));
	        dateTaken = videoCursor.getLong(videoCursor.getColumnIndex(MediaStore.Video.Media.DATE_TAKEN));
	        System.out.println("date taken = " + Util.getTimeString(dateTaken) );
	        System.out.println("last Id point to file path: " + path1);
	        file1 = new File(path1);
	    }
	    else
	    	System.out.println("videoCursor.getCount() = " + videoCursor.getCount() ); 	

	    videoCursor.close();
	    
	    Uri uri = Uri.parse(videoUriInDB);
	    File file2 = new File(uri.getPath());

	    System.out.println("- file1 size = " + file1.length());
	    System.out.println("- file1 path = " + file1.getPath());
	    System.out.println("- file2 size = " + file2.length());
	    System.out.println("- file2 path = " + file2.getPath());
	    
	    boolean isSameSize = false;
	    if(file1.length() == file2.length())
	    {
	    	System.out.println("-- file lenghts are the same");
	    	isSameSize = true;
	    }
	    else
	    	System.out.println("-- files are different");
	    
	    boolean isSameFilePath = false;
	    if(file1.getPath().equalsIgnoreCase( file2.getPath()))
	    {
	    	System.out.println("-- file paths are the same");
	    	isSameFilePath = true;
	    }
	    else
	    	System.out.println("-- file paths are different");
	    
	    // Check time for avoiding Delete existing file, since lastContentId could points to 
	    // wrong file by experiment
    	Date now = new Date(); 
        System.out.println("current time = " + Util.getTimeString(now.getTime()) );
	    long elapsedTime = Math.abs(dateTaken - now.getTime() );

	    // check if there is a duplicated file
        if( isSameSize && !isSameFilePath && (file1 != null) && (elapsedTime < 10000)) // tolerance 10 seconds
	    {
    		// delete file
        	// for ext_sd file, it can not be deleted after Kitkat, so this will be false
	        boolean bDeleteFile1 = file1.delete(); 

	        // check if default image file is deleted
	        if (bDeleteFile1)
	        {
	        	System.out.println("deleted file path1 = " + path1);
	        	String repPath =  path1;
        	  
	        	// delete 
	        	int deletedRows = context.getContentResolver().delete(
        	            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        	            MediaStore.Video.VideoColumns.DATA
        	                    + "='"
        	                    + repPath + "'", null);	        	  
        	  
	        	System.out.println("deleted thumbnail deletedRows = " + deletedRows);	  
	       }
	       else
	       {
	    	   	boolean bDeleteFile2 = file2.delete(); 
	    	   	
	    	   	// check if self-naming file is deleted
	    	   	if (bDeleteFile2)
	    	   	{
	    	   		System.out.println("deleted file path1 = " + file2.getPath());
	    	   		String repPath =  file2.getPath();
	         	  
	    	   		// update new Uri to DB
	    	   		videoUriInDB = "file://" + Uri.parse(file1.getPath()).toString();
					
					// set for Rotate any times
			        if(noteId != null)
			        {
			        	cameraVideoUri = dB.getNotePictureUri_byId(noteId);
			        }
			        
	    	   		// delete
	    	   		int deletedRows = context.getContentResolver().delete(
	         	            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
	         	            MediaStore.Video.VideoColumns.DATA
	         	                    + "='"
	         	                    + repPath + "'", null);	        	  
	         	  
	    	   		System.out.println("deleted thumbnail deletedRows = " + deletedRows);	  	    	   
	    	   	}
	       }
	    }
	}

	public static int getLastCapturedVideoId(Context context)
	{
	    final String[] videoColumns = { MediaStore.Video.Media._ID };
	    final String videoOrderBy = MediaStore.Video.Media._ID+" DESC";
	    final String videoWhere = null;
	    final String[] videoArguments = null;
	    Cursor videoCursor = context.getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
	    														videoColumns,
	    														videoWhere,
	    														videoArguments,
	    														videoOrderBy);
	    if(videoCursor.moveToFirst())
	    {
	        int id = videoCursor.getInt(videoCursor.getColumnIndex(MediaStore.Video.Media._ID));
	        videoCursor.close();
	        System.out.println("last captured video Id = " + id);
	        return id;
	    }else
	    {
	        return 0;
	    }
	}	
	
}
