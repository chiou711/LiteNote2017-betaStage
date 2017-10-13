package com.cw.litenote.operation.import_export;

import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
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
    private WebView webView;
    private File file;
    private Button btn_cancel;
    private Button btn_import;
    private View contentBlcok;

    Import_webAct_asyncTask(FragmentActivity _act, String _filePath)
    {
        act = _act;
        Util.lockOrientation(act);

        contentBlcok = act.findViewById(R.id.contentBlock);
        contentBlcok.setVisibility(View.GONE);

//        webView = (WebView) act.findViewById(R.id.webView);
//        webView.setVisibility(View.GONE);
//
//        btn_cancel = (Button) act.findViewById(R.id.import_web_cancel);
//        btn_cancel.setVisibility(View.GONE);
//
//        btn_import = (Button) act.findViewById(R.id.import_web_import);
//        btn_import.setVisibility(View.GONE);

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
//            webView.setVisibility(View.VISIBLE);
//            btn_cancel.setVisibility(View.VISIBLE);
//            btn_import.setVisibility(View.VISIBLE);
            contentBlcok.setVisibility(View.VISIBLE);
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

