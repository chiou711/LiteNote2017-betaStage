/*
 * Copyright (C) 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cw.litenote.operation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import com.cw.litenote.R;
import com.cw.litenote.util.ColorSet;
import com.cw.litenote.util.Util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


public class Import_fileView extends Fragment
{

    private TextView mTitleViewText;
    private TextView mBodyViewText;
    String filePath;
    static File mFile;
    FileInputStream fileInputStream = null;
    View mViewFile,mViewFileProgressBar;
    View rootView;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		rootView = inflater.inflate(R.layout.sd_file_view,container, false);
		System.out.println("Import_fileView / onCreate");

		mViewFile = rootView.findViewById(R.id.view_file);
		mViewFileProgressBar = rootView.findViewById(R.id.view_file_progress_bar);

		mTitleViewText = (TextView) rootView.findViewById(R.id.view_title);
		mBodyViewText = (TextView) rootView.findViewById(R.id.view_body);

		getActivity().getActionBar().setDisplayShowHomeEnabled(false);

		ProgressBar progressBar = (ProgressBar) rootView.findViewById(R.id.import_progress);
		if(savedInstanceState == null) {
			ImportAsyncTask task = new ImportAsyncTask();
			task.setProgressBar(progressBar);
			task.enableSaveDB(false);
			task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);//mFile is created inside ImportAsyncTask / _insertSelectedFileContentToDB
		}
		else
		{
			mFile = new File(filePath);
			mTitleViewText.setText(mFile.getName());
			mBodyViewText.setText(importObject.fileBody);
		}

		int style = 2;
		//set title color
		mTitleViewText.setTextColor(ColorSet.mText_ColorArray[style]);
		mTitleViewText.setBackgroundColor(ColorSet.mBG_ColorArray[style]);
		//set body color
		mBodyViewText.setTextColor(ColorSet.mText_ColorArray[style]);
		mBodyViewText.setBackgroundColor(ColorSet.mBG_ColorArray[style]);

		// back button
		Button backButton = (Button) rootView.findViewById(R.id.view_back);
		backButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_back, 0, 0, 0);

		// confirm button
		Button confirmButton = (Button) rootView.findViewById(R.id.view_confirm);

		// delete button
		Button deleteButton = (Button) rootView.findViewById(R.id.view_delete);
		deleteButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_delete , 0, 0, 0);

		// do cancel
		backButton.setOnClickListener(new View.OnClickListener() {

			public void onClick(View view) {
                backToListFragment();
			}
		});

		// delete the file whose content is showing
		deleteButton.setOnClickListener(new View.OnClickListener() {

			public void onClick(View view)
			{
				Util util = new Util(getActivity());
				util.vibrate();

				AlertDialog.Builder builder1 = new AlertDialog.Builder(getActivity());
				builder1.setTitle(R.string.confirm_dialog_title)
						.setMessage(getResources().getString(R.string.confirm_dialog_message_file) +
								" (" + mFile.getName() +")" )
						.setNegativeButton(R.string.confirm_dialog_button_no, new DialogInterface.OnClickListener()
						{
							@Override
							public void onClick(DialogInterface dialog1, int which1) {/*nothing to do*/}
						})
						.setPositiveButton(R.string.confirm_dialog_button_yes, new DialogInterface.OnClickListener()
						{
							@Override
							public void onClick(DialogInterface dialog1, int which1)
							{
								mFile.delete();

                                // refresh directory list
                                backToListFragment();
								String dirString = new File(filePath).getParent();
								File dir = new File(dirString);
                                Import_filesList fragment = ((Import_filesList)getActivity().getSupportFragmentManager().findFragmentByTag("import"));
                                fragment.getFiles(dir.listFiles());
							}
						})
						.show();
			}
		});

		// confirm to import view to DB
		confirmButton.setOnClickListener(new View.OnClickListener()
		{

			public void onClick(View view)
			{
				ProgressBar progressBar = (ProgressBar) rootView.findViewById(R.id.import_progress);
				ImportAsyncTask task = new ImportAsyncTask();
				task.setProgressBar(progressBar);
				task.enableSaveDB(true);
				task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			}
		});


		return rootView;
	}

	void backToListFragment()
    {
        getActivity().getSupportFragmentManager().popBackStack();
        View view1 = getActivity().findViewById(R.id.view_back_btn_bg);
        view1.setVisibility(View.VISIBLE);
        View view2 = getActivity().findViewById(R.id.file_list_title);
        view2.setVisibility(View.VISIBLE);
    }

	@Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        Bundle arguments = getArguments();
        filePath = arguments.getString("KEY_FILE_PATH");
    }

    static ParseXmlToDB importObject;
    private void insertSelectedFileContentToDB(boolean enableInsertDB) 
    {
    	mFile = new File(filePath);
    	
    	try 
    	{
    		fileInputStream = new FileInputStream(mFile);
    	} 
    	catch (FileNotFoundException e) 
    	{
    		e.printStackTrace();
    	}
		 
    	// import data by HandleXmlByFile class
    	importObject = new ParseXmlToDB(fileInputStream,getActivity());
    	importObject.enableInsertDB(enableInsertDB);
    	importObject.handleXML();
    	while(importObject.parsingComplete);
    }
    
    public static void createDefaultTables(Activity act,String fileName)
    {
		System.out.println("Import_fileView / _createDefaultTables / fileName = " + fileName);

        FileInputStream fileInputStream = null;
        File assetsFile = Util.createAssetsFile(act,fileName);
        try
        {
            fileInputStream = new FileInputStream(assetsFile);
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }

        // import data by HandleXmlByFile class
        importObject = new ParseXmlToDB(fileInputStream,act);
        importObject.enableInsertDB(true);
        importObject.handleXML();
        while(importObject.parsingComplete);
	}

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }
    
	// Show progress bar
	public class ImportAsyncTask extends AsyncTask<Void, Integer, Void> {

		ProgressBar bar;
		boolean enableSaveDB;
		public void setProgressBar(ProgressBar bar) {
//			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
			Util.lockOrientation(getActivity());
			this.bar = bar;
		    mViewFile.setVisibility(View.GONE);
		    mViewFileProgressBar.setVisibility(View.VISIBLE);
		    bar.setVisibility(View.VISIBLE);
		}
		
		public void enableSaveDB(boolean enable)
		{
			enableSaveDB = enable;
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
		    super.onProgressUpdate(values);
		    if (this.bar != null) {
		        bar.setProgress(values[0]);
		    }
		}
		
		@Override
		protected Void doInBackground(Void... params) 
		{
			insertSelectedFileContentToDB(enableSaveDB);
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			bar.setVisibility(View.GONE);
			mViewFile.setVisibility(View.VISIBLE);
			
			if(enableSaveDB)
			{
                backToListFragment();
//				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
				Util.unlockOrientation(getActivity());
				Toast.makeText(getActivity(),R.string.toast_import_finished,Toast.LENGTH_SHORT).show();
			}
			else
			{
			    // show Import content
		    	mTitleViewText.setText(mFile.getName());
		    	mBodyViewText.setText(importObject.fileBody);
			}
		}
	}    
}
