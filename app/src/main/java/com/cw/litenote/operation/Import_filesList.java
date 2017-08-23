package com.cw.litenote.operation;

import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.cw.litenote.main.MainAct;
import com.cw.litenote.R;
import com.cw.litenote.util.BaseBackPressedListener;
import com.cw.litenote.util.ColorSet;
import com.cw.litenote.util.Util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Import_filesList extends ListFragment
{
    private List<String> filePathArray = null;
    List<String> fileNames = null;
    public View rootView;
    ListView listView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.sd_files_list, container, false);

        View view = rootView.findViewById(R.id.view_back_btn_bg);
        view.setBackgroundColor(ColorSet.getBarColor(getActivity()));

        // back button
        Button backButton = (Button) rootView.findViewById(R.id.view_back);
        backButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_back, 0, 0, 0);

        // do cancel
        backButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        ((MainAct)getActivity()).setOnBackPressedListener(new BaseBackPressedListener(getActivity()));

        return rootView;
    }

    @Override
    public void onCreate(Bundle bundle) 
    {
        super.onCreate(bundle);
    }

    @Override
    public void onResume() {
        super.onResume();
        listView = getListView();
        String dirString = Environment.getExternalStorageDirectory().toString() +
                "/" +
                Util.getStorageDirName(getActivity());
        getFiles(new File(dirString).listFiles());
    }

    int selectedRow;
    String currFilePath;
    // on list item click
    @Override
    public void onListItemClick(ListView l, View v, int position, long rowId)
    {
        selectedRow = (int)rowId;
        if(selectedRow == 0)
        {
        	//root
            getFiles(new File("/").listFiles());
        }
        else
        {
            currFilePath = filePathArray.get(selectedRow);
            final File file = new File(currFilePath);
            if(file.isDirectory())
            {
            	//directory
                getFiles(file.listFiles());
            }
            else
            {
            	// view the selected file's content
            	if( file.isFile() &&
                   (file.getName().contains("XML") ||
                    file.getName().contains("xml")     ))
            	{
                    View view1 = getActivity().findViewById(R.id.view_back_btn_bg);
                    view1.setVisibility(View.GONE);
                    View view2 = getActivity().findViewById(R.id.file_list_title);
                    view2.setVisibility(View.GONE);

                    Import_fileView fragment = new Import_fileView();
                    final Bundle args = new Bundle();
                    args.putString("KEY_FILE_PATH", currFilePath);
                    fragment.setArguments(args);
                    FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                    transaction.setCustomAnimations(R.anim.fragment_slide_in_left, R.anim.fragment_slide_out_left, R.anim.fragment_slide_in_right, R.anim.fragment_slide_out_right);
                    transaction.replace(R.id.file_list_linear, fragment,"import_view").addToBackStack(null).commit();
            	}
            	else
            	{
            		Toast.makeText(getActivity(),R.string.file_not_found,Toast.LENGTH_SHORT).show();
                    String dirString = new File(currFilePath).getParent();
                    File dir = new File(dirString);
                    getFiles(dir.listFiles());
            	}
            }
        }
    }

    static ArrayAdapter<String> fileListAdapter;
    void getFiles(File[] files)
    {
        if(files == null)
        {
        	Toast.makeText(getActivity(),R.string.toast_import_SDCard_no_file,Toast.LENGTH_SHORT).show();
        	getActivity().finish();
        }
        else
        {
//        	System.out.println("files length = " + files.length);
            filePathArray = new ArrayList<>();
            fileNames = new ArrayList<>();
            filePathArray.add("");
            fileNames.add("ROOT");
            
	        for(File file : files)
	        {
                filePathArray.add(file.getPath());
                fileNames.add(file.getName());
	        }
	        fileListAdapter = new ArrayAdapter<>(getActivity(),
	        														R.layout.sd_files_list_row,
	        														fileNames);
	        setListAdapter(fileListAdapter);
            fileListAdapter.setNotifyOnChange(false);
        }
    }
}