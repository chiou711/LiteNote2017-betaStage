package com.cw.litenote.main;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.RadioGroup.OnCheckedChangeListener;

import com.cw.litenote.R;
import com.cw.litenote.db.DB_folder;
import com.cw.litenote.db.DB_page;
import com.cw.litenote.page.TabsHost;
import com.cw.litenote.note.Note_addAudio;
import com.cw.litenote.note.Note_addCameraImage;
import com.cw.litenote.note.Note_addCameraVideo;
import com.cw.litenote.note.Note_addText;
import com.cw.litenote.note.Note_addNew_option;
import com.cw.litenote.note.Note_addReadyImage;
import com.cw.litenote.note.Note_addReadyVideo;
import com.cw.litenote.preference.Define;
import com.cw.litenote.util.image.UtilImage;
import com.cw.litenote.util.Util;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;


public class MainUi 
{
	MainUi(){}

	static int mMenuUiState;

	public static void setMenuUiState(int mMenuState) {
		MainUi.mMenuUiState = mMenuState;
	}

	/**
	 *
	 * 	Add new note
	 *
	 */
    static List<NewNote> mAddNote;

    private final static int ID_NEW_TEXT = 1;
    private final static int ID_NEW_AUDIO = 2;
    private final static int ID_NEW_CAMERA_IMAGE = 3;
    private final static int ID_NEW_READY_IMAGE = 4;
    private final static int ID_NEW_CAMERA_VIDEO = 5;
    private final static int ID_NEW_READY_VIDEO = 6;
    private final static int ID_NEW_YOUTUBE_LINK = 7;
	private final static int ID_NEW_WEB_LINK = 8;
	private final static int ID_NEW_BACK = 9;
    private final static int ID_NEW_SETTING = 10;

    static void addNewNote(final FragmentActivity act)
	{
		AbsListView gridView;

		// get layout inflater
		View rootView = act.getLayoutInflater().inflate(R.layout.add_note_grid, null);

		// check camera feature
		PackageManager packageManager = act.getPackageManager();

        mAddNote = new ArrayList<>();

        // text
        mAddNote.add(new NewNote(ID_NEW_TEXT,
                     android.R.drawable.ic_menu_edit,
                     R.string.note_text));

        // audio
        mAddNote.add(new NewNote(ID_NEW_AUDIO,
                     R.drawable.ic_audio_unselected,
                     R.string.note_audio));

		// camera image
		if(packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA))
        {
            mAddNote.add(new NewNote(ID_NEW_CAMERA_IMAGE,
                                     android.R.drawable.ic_menu_camera,
                                     R.string.note_camera_image));
		}

		// ready image
        mAddNote.add(new NewNote(ID_NEW_READY_IMAGE,
                                 android.R.drawable.ic_menu_gallery,
                                 R.string.note_local_image));

		// camera video
		if(packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA))
        {
            mAddNote.add(new NewNote(ID_NEW_CAMERA_VIDEO,
                                     android.R.drawable.presence_video_online,
                                     R.string.note_camera_video));
		}

		// ready video
        mAddNote.add(new NewNote(ID_NEW_READY_VIDEO,
                                 R.drawable.ic_ready_video,
                                 R.string.note_local_video));

		// YouTube link
        mAddNote.add(new NewNote(ID_NEW_YOUTUBE_LINK,
                                 android.R.drawable.ic_menu_share,
                                 R.string.note_youtube_link));


		// Web link
		mAddNote.add(new NewNote(ID_NEW_WEB_LINK,
				android.R.drawable.ic_menu_share,
				R.string.note_web_link));

		// Back
		mAddNote.add(new NewNote(ID_NEW_BACK,
				R.drawable.ic_menu_back,
				R.string.btn_Cancel));

		// Setting
		mAddNote.add(new NewNote(ID_NEW_SETTING,
				android.R.drawable.ic_menu_preferences,
				R.string.settings));

		gridView = (GridView) rootView.findViewById(R.id.add_note_grid_view);

		// check if directory is created AND not empty
        if( (mAddNote != null  ) && (mAddNote.size() > 0))
		{
            GridIconAdapter mGridIconAdapter = new GridIconAdapter(act);
			gridView.setAdapter(mGridIconAdapter);
		}
		else
		{
			Toast.makeText(act,R.string.gallery_toast_no_file,Toast.LENGTH_SHORT).show();
			act.finish();
		}

		gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				System.out.println("MainUi / _addNewNote / _OnItemClickListener / position = " + position +" id = " + id);
                startAddNoteActivity(act,mAddNote.get(position).option_id);
			}
		});

		// set view to dialog
		AlertDialog.Builder builder1 = new AlertDialog.Builder(act);
		builder1.setView(rootView);
		dlgAddNew = builder1.create();
		dlgAddNew.show();
	}
	private static AlertDialog dlgAddNew;

	private static void startAddNoteActivity(FragmentActivity act,int option)
	{
		System.out.println("MainUi / _startAddNoteActivity / option = " + option);

		SharedPreferences mPref_add_new_note_location = act.getSharedPreferences("add_new_note_option", 0);
		boolean bTop = mPref_add_new_note_location.getString("KEY_ADD_NEW_NOTE_TO","bottom").equalsIgnoreCase("top");
		boolean bDirectory = mPref_add_new_note_location.getString("KEY_ADD_DIRECTORY","no").equalsIgnoreCase("yes");

		switch (option) {
			case ID_NEW_TEXT:
			{
				Intent intent = new Intent(act, Note_addText.class);
				if(bTop)
					intent.putExtra("extra_ADD_NEW_TO_TOP", "true");
				else
					intent.putExtra("extra_ADD_NEW_TO_TOP", "false");

				act.startActivity(intent);
			}
			break;

            case ID_NEW_AUDIO:
            {
                Intent intent = new Intent(act, Note_addAudio.class);
				if( bTop && !bDirectory )
					intent.putExtra("EXTRA_ADD_EXIST", "single_to_top");
				else if(!bTop && !bDirectory)
					intent.putExtra("EXTRA_ADD_EXIST", "single_to_bottom");
				else if(bTop && bDirectory)
					intent.putExtra("EXTRA_ADD_EXIST", "directory_to_top");
				else if(!bTop && bDirectory)
					intent.putExtra("EXTRA_ADD_EXIST", "directory_to_bottom");

				act.startActivity(intent);
            }
            break;

			case ID_NEW_CAMERA_IMAGE:
			{
				Intent intent = new Intent(act, Note_addCameraImage.class);
				if(bTop)
					intent.putExtra("extra_ADD_NEW_TO_TOP", "true");
				else
					intent.putExtra("extra_ADD_NEW_TO_TOP", "false");

				act.startActivity(intent);
			}
			break;

			case ID_NEW_READY_IMAGE:
			{
				Intent intent = new Intent(act, Note_addReadyImage.class);
				if( bTop && !bDirectory )
					intent.putExtra("EXTRA_ADD_EXIST", "single_to_top");
				else if(!bTop && !bDirectory)
					intent.putExtra("EXTRA_ADD_EXIST", "single_to_bottom");
				else if(bTop && bDirectory)
					intent.putExtra("EXTRA_ADD_EXIST", "directory_to_top");
				else if(!bTop && bDirectory)
					intent.putExtra("EXTRA_ADD_EXIST", "directory_to_bottom");

				act.startActivity(intent);
			}
			break;

			case ID_NEW_CAMERA_VIDEO:
			{
				Intent intent = new Intent(act, Note_addCameraVideo.class);
				if(bTop)
					intent.putExtra("extra_ADD_NEW_TO_TOP", "true");
				else
					intent.putExtra("extra_ADD_NEW_TO_TOP", "false");

				act.startActivity(intent);
			}
			break;

			case ID_NEW_READY_VIDEO:
			{
				Intent intent = new Intent(act, Note_addReadyVideo.class);
				if( bTop && !bDirectory )
					intent.putExtra("EXTRA_ADD_EXIST", "single_to_top");
				else if(!bTop && !bDirectory)
					intent.putExtra("EXTRA_ADD_EXIST", "single_to_bottom");
				else if(bTop && bDirectory)
					intent.putExtra("EXTRA_ADD_EXIST", "directory_to_top");
				else if(!bTop && bDirectory)
					intent.putExtra("EXTRA_ADD_EXIST", "directory_to_bottom");

				act.startActivity(intent);
			}
			break;

			case ID_NEW_YOUTUBE_LINK:
			{
				Intent	intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.youtube.com"));
                act.startActivityForResult(intent,Util.YOUTUBE_ADD_NEW_LINK_INTENT);
			}
			break;

			case ID_NEW_WEB_LINK:
			{
				Intent intent = new Intent(Intent.ACTION_VIEW,Uri.parse("http://www.google.com"));
				act.startActivity(intent);
			}
			break;

			case ID_NEW_BACK:
			{
				dlgAddNew.dismiss();
			}
			break;

			case ID_NEW_SETTING:
			{
				new Note_addNew_option(act);
			}
			break;

			// default
			default:
				break;
		}

	}
}


class MenuId {
    static final int ADD_NEW_NOTE = R.id.ADD_NEW_NOTE;

    static final int OPEN_PLAY_SUBMENU = R.id.PLAY;
    static final int PLAY_OR_STOP_AUDIO = R.id.PLAY_OR_STOP_MUSIC;
    static final int SLIDE_SHOW = R.id.SLIDE_SHOW;
    static final int GALLERY = R.id.GALLERY;

    static final int ADD_NEW_PAGE = R.id.ADD_NEW_PAGE;
    static final int CHANGE_PAGE_COLOR = R.id.CHANGE_PAGE_COLOR;
    static final int SHIFT_PAGE = R.id.SHIFT_PAGE;
	static final int SHOW_BODY = R.id.SHOW_BODY;
	static final int CLICK_LAUNCH_YOUTUBE = R.id.CLICK_LAUNCH_YOUTUBE;
    static final int ENABLE_NOTE_DRAG_AND_DROP = R.id.ENABLE_NOTE_DRAG_AND_DROP;
    static final int SEND_PAGES = R.id.SEND_PAGES;
    static final int EXPORT_TO_SD_CARD = R.id.EXPORT_TO_SD_CARD;
	static final int IMPORT_FROM_SD_CARD = R.id.IMPORT_FROM_SD_CARD;
	static final int IMPORT_FROM_WEB = R.id.IMPORT_FROM_WEB;
    static final int CONFIG_PREFERENCE = R.id.CONFIG_PREF;

    static final int ADD_NEW_FOLDER = R.id.ADD_NEW_FOLDER;
    static final int ENABLE_FOLDER_DRAG_AND_DROP = R.id.ENABLE_FOLDER_DRAG_AND_DROP;
}


class GridIconAdapter extends BaseAdapter {
	private FragmentActivity act;
	GridIconAdapter(FragmentActivity fragAct){act = fragAct;}

	@Override
	public int getCount() {
        return MainUi.mAddNote.size();
	}

	@Override
	public Object getItem(int position) {
		return null;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		final ViewHolder holder;
		View view = convertView;
		if (view == null) {
			view = act.getLayoutInflater().inflate(R.layout.add_note_grid_item, parent, false);
			holder = new ViewHolder();
			assert view != null;
			holder.imageView = (ImageView) view.findViewById(R.id.grid_item_image);
			holder.text = (TextView) view.findViewById(R.id.grid_item_text);
			view.setTag(holder);
		} else {
			holder = (ViewHolder) view.getTag();
		}

        Drawable drawable = act.getResources().getDrawable(MainUi.mAddNote.get(position).option_drawable_id);
		holder.imageView.setImageDrawable(drawable);
        holder.text.setText(MainUi.mAddNote.get(position).option_string_id);
		return view;
	}

	private class ViewHolder {
		ImageView imageView;
		TextView text;
	}
}

class NewNote {
    int option_id;
    int option_drawable_id;
    int option_string_id;

    NewNote(int id, int draw_id, int string_id)
    {
        this.option_id = id;
        this.option_drawable_id = draw_id;
        this.option_string_id = string_id;
    }
}