package com.cw.litenote.operation.import_export;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.Toast;

import com.cw.litenote.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class Import_webAct extends FragmentActivity
{
    String content=null;
    WebView webView;
    Button btn_import;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.import_web);

        // web view
        webView = (WebView) findViewById(R.id.webView);

        // cancel button
        Button btn_cancel = (Button) findViewById(R.id.import_web_cancel);
        btn_cancel.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
                setResult(RESULT_CANCELED);
                if (webView.canGoBack()) {
                    webView.goBack();
                    content = null;
                }
                else
                    finish();
            }
        });

        // import button
        btn_import = (Button) findViewById(R.id.import_web_import);
//        btn_import.setVisibility(View.INVISIBLE);
        btn_import.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View view)
            {
                setResult(RESULT_OK);

                // import
                // save text in a file
                String dirName = "Download";
                String fileName = "temp.xml";
                String dirPath = Environment.getExternalStorageDirectory().toString() +
                        "/" +
                        dirName;
                File file = new File(dirPath, fileName);

                try
                {
                    try {
                        file.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    FileOutputStream fOut = new FileOutputStream(file);
                    OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
                    content = content.replaceAll("(?m)^[ \t]*\r?\n", "");
                    myOutWriter.append(content);
                    myOutWriter.close();

                    fOut.flush();
                    fOut.close();
                }
                catch (IOException e)
                {
                    Log.e("Exception", "File write failed: " + e.toString());
                }

                // import file content to DB
                Import_webAct_asyncTask task = new Import_webAct_asyncTask(Import_webAct.this,file.getPath());
                task.enableSaveDB(true);// view
                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }

        });

        webView.getSettings().setJavaScriptEnabled(true);

        // create instance
        final ImportInterface import_interface = new ImportInterface(webView);

        // load web content
        webView.addJavascriptInterface(import_interface, "INTERFACE");
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url)
            {
                view.loadUrl("javascript:window.INTERFACE.processContent(document.getElementsByTagName('body')[0].innerText);");
            }
        });

        // show toast
        webView.addJavascriptInterface(import_interface, "LiteNote");
//        webView.addJavascriptInterface(import_interface, "INTERFACE");

        // load content to web view
        webView.loadUrl("http://litenoteapp.blogspot.tw/2017/09/xml-link.html");
    }

    @Override
    public void onBackPressed() {
        System.out.println("Import_webAct / _onBackPressed");
        // web view can go back
        if (webView.canGoBack()) {
            webView.goBack();
            content = null;
        }
        else
            super.onBackPressed();
    }

    /* An instance of this class will be registered as a JavaScript interface */
    class ImportInterface {

        WebView webView;
        public ImportInterface(WebView _webView)
        {
            webView = _webView;
        }

        @SuppressWarnings("unused")
        @android.webkit.JavascriptInterface
        public void processContent(final String _content)
        {
            webView.post(new Runnable()
            {
                public void run()
                {
                    content = _content;
                    System.out.println("Import_webAct.content = "+ content );
                }
            });
        }

        @android.webkit.JavascriptInterface
        public void showToast(String toastText) {
            Toast.makeText(Import_webAct.this, toastText, Toast.LENGTH_LONG).show();
        }


    }
}
