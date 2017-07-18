package com.cw.litenote.operation;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.cw.litenote.db.DB_drawer;
import com.cw.litenote.db.DB_folder;
import com.cw.litenote.db.DB_page;
import com.cw.litenote.main.MainAct;
import com.cw.litenote.R;
import com.cw.litenote.util.BaseBackPressedListener;
import com.cw.litenote.util.ColorSet;
import com.cw.litenote.util.Util;

import java.util.ArrayList;
import java.util.List;

public class MailPagesFragment extends Fragment{
	Context mContext;
	Intent mEMailIntent;
	CheckedTextView mCheckTvSelAll;
	Button btnSelPageOK;
    ListView mListView;
	String mSentString;
	SelectPageList selPageList;
	public static View rootView;

	public MailPagesFragment(){}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		rootView = inflater.inflate(R.layout.select_page_list, container, false);
		getActivity().getActionBar().setBackgroundDrawable(new ColorDrawable(ColorSet.getBarColor(getActivity())));

        // checked Text View: select all
        mCheckTvSelAll = (CheckedTextView) rootView.findViewById(R.id.chkSelectAllPages);
        mCheckTvSelAll.setOnClickListener(new OnClickListener()
        {	@Override
        public void onClick(View checkSelAll)
        {
            boolean currentCheck = ((CheckedTextView)checkSelAll).isChecked();
            ((CheckedTextView)checkSelAll).setChecked(!currentCheck);

            if(((CheckedTextView)checkSelAll).isChecked())
                selPageList.selectAllPages(true);
            else
                selPageList.selectAllPages(false);
        }
        });

        // list view: selecting which pages to send
        mListView = (ListView)rootView.findViewById(R.id.listView1);

        // OK button: click to do next
        btnSelPageOK = (Button) rootView.findViewById(R.id.btnSelPageOK);
        btnSelPageOK.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // input mail address: dialog
                System.out.println("OK");
                if(selPageList.mChkNum > 0)
                {
                    inputEMailDialog(); // call next dialog
                }
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
            public void onClick(View v) {
                System.out.println("SendMailFragment / Cancel button");
                getActivity().getSupportFragmentManager().popBackStack();
                mCheckTvSelAll.setVisibility(View.INVISIBLE);
            }
        });

        //Send e-Mail 1: show list for selection
        selPageList = new SelectPageList(getActivity(),rootView , mListView);

		((MainAct)getActivity()).setOnBackPressedListener(new BaseBackPressedListener(getActivity()));

		return rootView;
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		mContext = getActivity();
	}

	// Send e-Mail 2
	// case A: input mail address from current activity
	// case B: input mail address from ViewNote activity
    String mDefaultEmailAddr;
    SharedPreferences mPref_email;
	EditText editEMailAddrText;
	String mEMailBodyString;
	AlertDialog mDialog;

	void inputEMailDialog()
	{
		AlertDialog.Builder builder1;

		mPref_email = getActivity().getSharedPreferences("email_addr", 0);
	    editEMailAddrText = (EditText)getActivity().getLayoutInflater()
	    							.inflate(R.layout.edit_text_dlg, null);
		builder1 = new AlertDialog.Builder(getActivity());

		// get default email address
		mDefaultEmailAddr = mPref_email.getString("KEY_DEFAULT_EMAIL_ADDR","@");
		editEMailAddrText.setText(mDefaultEmailAddr);

		builder1.setTitle(R.string.mail_notes_dlg_title)
				.setMessage(R.string.mail_notes_dlg_message)
				.setView(editEMailAddrText)
				.setNegativeButton(R.string.edit_note_button_back,
						new DialogInterface.OnClickListener()
				{   @Override
					public void onClick(DialogInterface dialog, int which)
					{/*cancel*/
                        dialog.dismiss();
                    }

				})
				.setPositiveButton(R.string.mail_notes_btn, null); //call override

		mDialog = builder1.create();
		mDialog.show();

		// override positive button
		Button enterButton = mDialog.getButton(DialogInterface.BUTTON_POSITIVE);
		enterButton.setOnClickListener(new CustomListener(mDialog));


		// back
		mDialog.setOnKeyListener(new Dialog.OnKeyListener() {

            @Override
            public boolean onKey(DialogInterface arg0, int keyCode,
                    KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    mDialog.dismiss();
                    return true;
                }
                return false;
            }
        });
	}

	//for keeping dialog when eMail address is empty
	class CustomListener implements OnClickListener
	{
		private final Dialog dialog;
	    public CustomListener(Dialog dialog){
	    	this.dialog = dialog;
	    }

	    @Override
	    public void onClick(View v){
	    	String attachmentFileName;
	        String strEMailAddr = editEMailAddrText.getText().toString();
	        if(strEMailAddr.length() > 0)
	        {
	    	    Bundle extras = getActivity().getIntent().getExtras();

				// save to SD card
				attachmentFileName = Util.getStorageDirName(getActivity()) + "_SEND_" + // file name
		        							Util.getCurrentTimeString() + // time
		        							".xml"; // extension name
				Util util = new Util(getActivity());

				// null: for page selection
				String[] picFileNameArr = null;
		        if(extras == null)
		        {
					mEMailBodyString = util.exportToSdCard(attachmentFileName, // attachment name
														   selPageList.mCheckedArr);// checked page array
					mEMailBodyString = util.trimXMLtag(mEMailBodyString);
	        	}
	        	else //other: for Note_view or selected Check notes
	        	{
		    	    mSentString = extras.getString("SentString");
					mEMailBodyString = util.exportStringToSdCard(attachmentFileName, // attachment name
															   	 mSentString); // sent string
					mEMailBodyString = util.trimXMLtag(mEMailBodyString);
		        	picFileNameArr = extras.getStringArray("SentPictureFileNameArray");
	        	}

	        	mPref_email.edit().putString("KEY_DEFAULT_EMAIL_ADDR", strEMailAddr).apply();

	        	// call next dialog
				sendEMail(strEMailAddr,  // eMail address
					      attachmentFileName, // attachment file name
						  picFileNameArr ); // picture file name array. For page selection, this is null
				dialog.dismiss();
	        }
	        else
	        {
    			Toast.makeText(getActivity(),
						R.string.toast_no_email_address,
						Toast.LENGTH_SHORT).show();
	        }
	    }
	}
	
	// Send e-Mail : send file by e-Mail
    public final static int EMAIL_PAGES = 102;
	public static String mAttachmentFileName;
	void sendEMail(String strEMailAddr,  // eMail address
			       String attachmentFileName, // attachment name
			       String[] picFileNameArray) // attachment picture file name
	{
		mAttachmentFileName = attachmentFileName;
		// new ACTION_SEND intent
		mEMailIntent = new Intent(Intent.ACTION_SEND_MULTIPLE); // for multiple attachments

		// set type
		mEMailIntent.setType("text/plain");//can select which APP will be used to send mail

    	// open issue: cause warning for Key android.intent.extra.TEXT expected ArrayList
    	String text_body = mContext.getResources().getString(R.string.eMail_body)// eMail text (body)
			 	 			+ " " + Util.getStorageDirName(mContext) + " (UTF-8)" + Util.NEW_LINE
			 	 			+ mEMailBodyString;
    	
    	// attachment: message
    	List<String> filePaths = new ArrayList<String>();
    	String messagePath = "file:///" + Environment.getExternalStorageDirectory().getPath() + 
                			 "/" + Util.getStorageDirName(mContext) + "/" + 
                			 attachmentFileName;// message file name
    	filePaths.add(messagePath);
    	
    	// attachment: pictures
    	if(picFileNameArray != null)
    	{
	    	for(int i=0;i<picFileNameArray.length;i++)
	    	{
	        	filePaths.add(picFileNameArray[i]);
	    	}
    	}
    	
        ArrayList<Uri> uris = new ArrayList<Uri>();
        for (String file : filePaths)
        {
            Uri uri = Uri.parse(file);
            uris.add(uri);
        }
    	
    	mEMailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{strEMailAddr}) // eMail address
	    			.putExtra(Intent.EXTRA_SUBJECT,
	    			          Util.getStorageDirName(mContext) + // eMail subject
	    					  " " + mContext.getResources().getString(R.string.eMail_subject ))// eMail subject
	    			.putExtra(Intent.EXTRA_TEXT,text_body) // eMail body (open issue)
	    			.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris); // multiple eMail attachment
    	
	    Log.v(getClass().getSimpleName(),
			  "attachment " + Uri.parse("file name is:"+ attachmentFileName));
	    
	    getActivity().startActivityForResult(Intent.createChooser(mEMailIntent,
	    											getResources().getText(R.string.mail_chooser_title)) ,
                                             EMAIL_PAGES);
	} 
	
	@Override
	public void onPause() {
		super.onPause();

		if( null != mDialog)
			mDialog.dismiss();//fix leaked window
	}

	public static class SelectPageList {
        View mView;
        CheckedTextView mCheckTvSelAll;
        ListView mListView;
        List<String> mListStrArr; // list view string array
        public List<Boolean> mCheckedArr; // checked list view items array
        DB_drawer mDb_drawer;
        DB_folder mDb_folder;
        int COUNT;
        Activity mAct;
        public String mXML_default_filename;//??? for only 2 cases (all and single)?
        public String mFolderTitle;

        public SelectPageList(Activity act, View rootView, View view)
        {
            mAct = act;

            mDb_drawer = new DB_drawer(act);
            int pos = MainAct.mFolder.listView.getCheckedItemPosition();
            mFolderTitle = mDb_drawer.getFolderTitle(pos);

            mDb_folder = new DB_folder(mAct,Util.getPref_lastTimeView_folder_tableId(mAct));

            // checked Text View: select all
            mCheckTvSelAll = (CheckedTextView) rootView.findViewById(R.id.chkSelectAllPages);
            mCheckTvSelAll.setOnClickListener(new OnClickListener()
            {	@Override
                public void onClick(View checkSelAll)
                {
                    boolean currentCheck = ((CheckedTextView)checkSelAll).isChecked();
                    ((CheckedTextView)checkSelAll).setChecked(!currentCheck);

                    if(((CheckedTextView)checkSelAll).isChecked())
                    {
                        selectAllPages(true);
                        mXML_default_filename = mFolderTitle;
                    }
                    else
                        selectAllPages(false);
                }
            });

            // list view: selecting which pages to send
            mListView = (ListView)view;
            listForSelect(rootView);
        }

        // select all pages
        public void selectAllPages(boolean enAll) {
            mChkNum = 0;

            mDb_folder.open();
            COUNT = mDb_folder.getPagesCount(false);
            for(int i=0;i<COUNT;i++)
            {
                 CheckedTextView chkTV = (CheckedTextView) mListView.findViewById(R.id.checkTV);
                 mCheckedArr.set(i, enAll);
                 mListStrArr.set(i, mDb_folder.getPageTitle(i,false));

                 int style = mDb_folder.getPageStyle(i, false);

                  if( enAll)
                      chkTV.setCompoundDrawablesWithIntrinsicBounds(style%2 == 1 ?
                             R.drawable.btn_check_on_holo_light:
                             R.drawable.btn_check_on_holo_dark,0,0,0);
                  else
                      chkTV.setCompoundDrawablesWithIntrinsicBounds(style%2 == 1 ?
                             R.drawable.btn_check_off_holo_light:
                             R.drawable.btn_check_off_holo_dark,0,0,0);
            }
            mDb_folder.close();

            mChkNum = (enAll == true)? COUNT : 0;

            // set list adapter
            ListAdapter listAdapter = new ListAdapter(mAct, mListStrArr);

            // list view: set adapter
            mListView.setAdapter(listAdapter);
        }

        // show list for Select
        public int mChkNum;
        void listForSelect(View root)
        {
            mChkNum = 0;
            // set list view
            mListView = (ListView) root.findViewById(R.id.listView1);
            mListView.setOnItemClickListener(new AdapterView.OnItemClickListener()
            {
                public void onItemClick(AdapterView<?> parent, View vw, int position, long id)
                {
                     CheckedTextView chkTV = (CheckedTextView) vw.findViewById(R.id.checkTV);
                     chkTV.setChecked(!chkTV.isChecked());
                     mCheckedArr.set(position, chkTV.isChecked());
                     if(mCheckedArr.get(position) == true)
                         mChkNum++;
                     else
                         mChkNum--;

                     if(!chkTV.isChecked())
                     {
                         mCheckTvSelAll.setChecked(false);
                     }

                     // set for contrast
                     int mStyle = mDb_folder.getPageStyle(position, true);
                      if( chkTV.isChecked())
                          chkTV.setCompoundDrawablesWithIntrinsicBounds(mStyle%2 == 1 ?
                                 R.drawable.btn_check_on_holo_light:
                                 R.drawable.btn_check_on_holo_dark,0,0,0);
                      else
                          chkTV.setCompoundDrawablesWithIntrinsicBounds(mStyle%2 == 1 ?
                                 R.drawable.btn_check_off_holo_light:
                                 R.drawable.btn_check_off_holo_dark,0,0,0);

                      // set default file name
                        mXML_default_filename = mDb_folder.getPageTitle(position, true);
                }
            });

            // set list string array
            mCheckedArr = new ArrayList<Boolean>();
            mListStrArr = new ArrayList<String>();

            // DB
            int pageTableId = Util.getPref_lastTimeView_page_tableId(mAct);
            DB_page.setFocusPage_tableId(pageTableId);

            mDb_folder.open();
            COUNT = mDb_folder.getPagesCount(false);
            for(int i=0;i<COUNT;i++)
            {
                 // list string array: init
                 mListStrArr.add(mDb_folder.getPageTitle(i,false));
                 // checked mark array: init
                 mCheckedArr.add(false);
            }
            mDb_folder.close();

            // set list adapter
            ListAdapter listAdapter = new ListAdapter(mAct, mListStrArr);

            // list view: set adapter
            mListView.setAdapter(listAdapter);
        }

        // list adapter
        public class ListAdapter extends BaseAdapter
        {
            private Activity activity;
            private List<String> mList;
            private LayoutInflater inflater = null;

            public ListAdapter(Activity a, List<String> list)
            {
                activity = a;
                mList = list;
                inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            }

            public int getCount()
            {
                return mList.size();
            }

            public Object getItem(int position)
            {
                return mCheckedArr.get(position);
            }

            public long getItemId(int position)
            {
                return position;
            }

            public View getView(int position, View convertView, ViewGroup parent)
            {
                mView = inflater.inflate(R.layout.select_page_list_row, null);

                // set checked text view
                CheckedTextView chkTV = (CheckedTextView) mView.findViewById(R.id.checkTV);
                // show style
                int style = mDb_folder.getPageStyle(position, true);
                chkTV.setBackgroundColor(ColorSet.mBG_ColorArray[style]);
                chkTV.setTextColor(ColorSet.mText_ColorArray[style]);

                // Show current page
                //??? how to set left padding of text view of a CheckedTextview
                // workaround: set single line to true and add one space in front of the text
                if(mDb_folder.getPageTableId(position,true) == Integer.valueOf(DB_page.getFocusPage_tableId()))
                {
                    chkTV.setTypeface(chkTV.getTypeface(), Typeface.BOLD_ITALIC);
                    chkTV.setText( " " + mList.get(position) + "*" );
                }
                else
                    chkTV.setText( " " + mList.get(position).toString());

                chkTV.setChecked(mCheckedArr.get(position));

                 // set for contrast
                 if( chkTV.isChecked())
                     // note: have to remove the following in XML file
                     // android:drawableLeft="?android:attr/listChoiceIndicatorMultiple"
                     // otherwise, setCompoundDrawablesWithIntrinsicBounds will not work on ICS
                     chkTV.setCompoundDrawablesWithIntrinsicBounds(style%2 == 1 ?
                            R.drawable.btn_check_on_holo_light:
                            R.drawable.btn_check_on_holo_dark,0,0,0);
                 else
                     chkTV.setCompoundDrawablesWithIntrinsicBounds(style%2 == 1 ?
                            R.drawable.btn_check_off_holo_light:
                            R.drawable.btn_check_off_holo_dark,0,0,0);

                 return mView;
            }
        }

    }
}