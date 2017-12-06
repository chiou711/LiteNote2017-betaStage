package com.cw.litenote.operation.import_export;

import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.cw.litenote.R;
import com.cw.litenote.util.Util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

/**
 * Created by cw on 2017/9/16.
 */
// Show progress progressBar
class Import_webAct_asyncTask extends AsyncTask<Void, Integer, Void> {

    private ProgressBar progressBar;
    private boolean enableSaveDB;
    private FragmentActivity act;
    private File file;
    private View contentBlock;

    Import_webAct_asyncTask(FragmentActivity _act, String _filePath)
    {
        act = _act;
        Util.lockOrientation(act);

        contentBlock = act.findViewById(R.id.contentBlock);
        contentBlock.setVisibility(View.GONE);

        progressBar = (ProgressBar) act.findViewById(R.id.import_progress);
        progressBar.setVisibility(View.VISIBLE);

        file = new File(_filePath);
    }

    void enableSaveDB(boolean enable)
    {
        enableSaveDB = enable;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        if (this.progressBar != null ){
            progressBar.setProgress(values[0]);
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

        if(enableSaveDB)
        {
            contentBlock.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);

//				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
            Util.unlockOrientation(act);
            Toast.makeText(act, R.string.toast_import_finished,Toast.LENGTH_SHORT).show();
        }
    }

    ParseXmlToDB importObject;
    private void insertSelectedFileContentToDB(boolean enableInsertDB)
    {
        FileInputStream fileInputStream = null;
        try
        {
            fileInputStream = new FileInputStream(file);
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }

        // import data by HandleXmlByFile class
        if(fileInputStream != null) {
            importObject = new ParseXmlToDB(fileInputStream, act);
            importObject.enableInsertDB(enableInsertDB);
            importObject.handleXML();
            while (importObject.isParsing) ;
        }
    }
}

