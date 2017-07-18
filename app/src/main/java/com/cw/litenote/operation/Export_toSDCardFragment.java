package com.cw.litenote.operation;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.cw.litenote.util.BaseBackPressedListener;
import com.cw.litenote.main.MainAct;
import com.cw.litenote.R;
import com.cw.litenote.util.ColorSet;
import com.cw.litenote.util.Util;

public class Export_toSDCardFragment extends Fragment {
	Context mContext;
	CheckedTextView mCheckTvSelAll;
    ListView mListView;
    int mStyle;
	MailPagesFragment.SelectPageList mSelectPageList;
	public static View mSelPageDlg,mProgressBar;
	public Export_toSDCardFragment(){}
	public static View rootView;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		rootView = inflater.inflate(R.layout.select_page_list, container, false);
		getActivity().getActionBar().setBackgroundDrawable(new ColorDrawable(ColorSet.getBarColor(getActivity())));
		mSelPageDlg = rootView.findViewById(R.id.selectPageDlg);
		mProgressBar = rootView.findViewById(R.id.progressBar);

		// checked Text View: select all
		mCheckTvSelAll = (CheckedTextView) rootView.findViewById(R.id.chkSelectAllPages);
		mCheckTvSelAll.setOnClickListener(new OnClickListener()
		{	@Override
		public void onClick(View checkSelAll)
		{
			boolean currentCheck = ((CheckedTextView)checkSelAll).isChecked();
			((CheckedTextView)checkSelAll).setChecked(!currentCheck);

			if(((CheckedTextView)checkSelAll).isChecked())
				mSelectPageList.selectAllPages(true);
			else
				mSelectPageList.selectAllPages(false);
		}
		});
		mStyle = Util.getCurrentPageStyle(mContext);

		// list view: selecting which pages to send
		mListView = (ListView)rootView.findViewById(R.id.listView1);
		// OK button: click to do next
		Button btnSelPageOK = (Button) rootView.findViewById(R.id.btnSelPageOK);
		btnSelPageOK.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// input mail address: dialog
				if(mSelectPageList.mChkNum > 0)
					inputFileNameDialog(); // call next dialog
				else
					Toast.makeText(getActivity(),
							R.string.delete_checked_no_checked_items,
							Toast.LENGTH_SHORT).show();
			}
		});

		// cancel button
		Button btnSelPageCancel = (Button) rootView.findViewById(R.id.btnSelPageCancel);
		btnSelPageCancel.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_close_clear_cancel, 0, 0, 0);

		btnSelPageCancel.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v)
			{
				System.out.println("Export_toSDCardFragment / cancel button");
				getActivity().getSupportFragmentManager().popBackStack();
				mCheckTvSelAll.setVisibility(View.INVISIBLE);
			}
		});

		// step 1: show list for Select
		mSelectPageList = new MailPagesFragment.SelectPageList(getActivity(),rootView,mListView);

		((MainAct)getActivity()).setOnBackPressedListener(new BaseBackPressedListener(getActivity()));

		return rootView;
	}


	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		mContext = getActivity();
	}

	@Override
	public void onResume() {
		super.onResume();
		System.out.println("--- onResume");
	}

	// step 2: input file name
    String mDefaultFileName;
    SharedPreferences mPref_email;
	EditText editSDCardFileNameText;
	AlertDialog mDialog;

	void inputFileNameDialog()
	{
		AlertDialog.Builder builder1;

		mPref_email = getActivity().getSharedPreferences("sd_card_file_name", 0);
	    editSDCardFileNameText = (EditText)getActivity().getLayoutInflater()
	    							.inflate(R.layout.edit_text_dlg, null);
		builder1 = new AlertDialog.Builder(getActivity());

		// default file name: with tab title
		mDefaultFileName = mSelectPageList.mXML_default_filename + ".xml";

		editSDCardFileNameText.setText(mDefaultFileName);

		builder1.setTitle(R.string.config_export_SDCard_edit_filename)
				.setMessage(R.string.config_SDCard_filename)
				.setView(editSDCardFileNameText)
				.setNegativeButton(R.string.edit_note_button_back,
						new DialogInterface.OnClickListener()
				{   @Override
					public void onClick(DialogInterface dialog, int which)
					{/*cancel*/dialog.dismiss(); }
				})
				.setPositiveButton(R.string.btn_OK, null); //call override

		mDialog = builder1.create();
		mDialog.show();

		// override positive button
		Button enterButton = mDialog.getButton(DialogInterface.BUTTON_POSITIVE);
		enterButton.setOnClickListener(new CustomListener(mDialog));
	}

	String mStrSDCardFileName;
	//for keeping dialog if no input
	class CustomListener implements OnClickListener
	{
		public CustomListener(Dialog dialog){
	    }
	    
	    @Override
	    public void onClick(View v){
	        mStrSDCardFileName = editSDCardFileNameText.getText().toString();
	        if(mStrSDCardFileName.length() > 0)
	        {
				ProgressBar progressBar = (ProgressBar) getActivity().findViewById(R.id.export_progress);
				ShowProgressBarAsyncTask task = new ShowProgressBarAsyncTask();
		        task.setProgressBar(progressBar);
		        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	        }
	        else
	        {
    			Toast.makeText(getActivity(),
						R.string.toast_input_filename,
						Toast.LENGTH_SHORT).show();
	        }
	    }
	}
	
	// Show progress bar
	public class ShowProgressBarAsyncTask extends AsyncTask<Void, Integer, Void> {

		ProgressBar bar;
		public void setProgressBar(ProgressBar bar) {
		    this.bar = bar;
		    mDialog.dismiss();
			mSelPageDlg.setVisibility(View.GONE);
			mProgressBar.setVisibility(View.VISIBLE);
		    bar.setVisibility(View.VISIBLE);
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
		    super.onProgressUpdate(values);
		    if (this.bar != null) {
		        bar.setProgress(values[0]);
		    }
		}
		
		@Override
		protected Void doInBackground(Void... params) {
			Util util = new Util(getActivity());
			util.exportToSdCard(mStrSDCardFileName, // attachment name
								mSelectPageList.mCheckedArr); // checked page array
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			bar.setVisibility(View.GONE);
			Toast.makeText(getActivity(),
					   R.string.btn_Finish, 
					   Toast.LENGTH_SHORT).show();
			getActivity().getSupportFragmentManager().popBackStack();
		}
	}
}