package com.cw.litenote.note;

import android.content.Context;
import android.widget.Toast;

/**
 * Created by cw on 2017/9/15.
 */

public class JavaScriptInterface {

        Context mContext;

        JavaScriptInterface(Context c) {
            mContext = c;
        }

        @android.webkit.JavascriptInterface
        public void showToast(String toast) {
            Toast.makeText(mContext, toast, Toast.LENGTH_LONG).show();
        }
}
