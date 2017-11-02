package com.cw.litenote.main;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.Toast;

import com.cw.litenote.R;
import com.cw.litenote.db.DB_drawer;
import com.cw.litenote.db.DB_folder;
import com.cw.litenote.db.DB_page;
import com.cw.litenote.page.Page;
import com.cw.litenote.util.CustomWebView;
import com.cw.litenote.util.Util;
import com.cw.litenote.util.preferences.Pref;

import java.util.Date;

/**
 * Created by cw on 2017/10/7.
 */

public class MainUi {

    MainUi(){}

    /**
     * Add note with Intent link
     */
    String title;
    String addNote_IntentLink(Intent intent,final FragmentActivity act)
    {
        Bundle extras = intent.getExtras();
        String pathOri = null;
        String path;
        if(extras != null)
            pathOri = extras.getString(Intent.EXTRA_TEXT);
        else
            System.out.println("MainUi / _addNote_IntentLink / extras == null");

        path = pathOri;

        if(!Util.isEmptyString(pathOri))
        {
            System.out.println("MainUi / _addNote_IntentLink / pathOri = " + pathOri);
            // for SoundCloud case, path could contain other strings before URI path
            if(pathOri.contains("http"))
            {
                String[] str = pathOri.split("http");

                for(int i=0;i< str.length;i++)
                {
                    if(str[i].contains("://"))
                        path = "http".concat(str[i]);
                }
            }

            DB_drawer db_drawer = new DB_drawer(act);
            DB_folder db_folder = new DB_folder(act, Pref.getPref_focusView_folder_tableId(MainAct.mAct));
            if((db_drawer.getFoldersCount(true) == 0) || (db_folder.getPagesCount(true) == 0))
            {
                Toast.makeText(act,"No folder or no page yet, please add a new one in advance.",Toast.LENGTH_LONG).show();
                return null;
            }

            System.out.println("MainUi / _addNote_IntentLink / path = " + path);
            final DB_page dB_page = new DB_page(act,Pref.getPref_focusView_page_tableId(MainAct.mAct));
            dB_page.open();
            final long rowId = dB_page.insertNote("", "", "", "", path, "", 0, (long) 0);// add new note, get return row Id
            dB_page.close();

            // save to top or to bottom
            final String link =path;
            int count = dB_page.getNotesCount(true);
            SharedPreferences pref_show_note_attribute = act.getSharedPreferences("add_new_note_option", 0);

            // YouTube
            if( Util.isYouTubeLink(path))
            {
                title = Util.getYouTubeTitle(path);

                if(pref_show_note_attribute
                        .getString("KEY_ENABLE_LINK_TITLE_SAVE", "yes")
                        .equalsIgnoreCase("yes"))
                {
                    Date now = new Date();
                    dB_page.updateNote(rowId, title, "", "", "", path, "", 0, now.getTime(), true); // update note
                }

                if( pref_show_note_attribute.getString("KEY_ADD_NEW_NOTE_TO","bottom").equalsIgnoreCase("top") &&
                        (count > 1)        )
                {
                    Page.swap(dB_page);
                }

                Toast.makeText(act,
                        act.getResources().getText(R.string.add_new_note_option_title) + title,
                        Toast.LENGTH_SHORT)
                        .show();
            }
            // Web page
            else if(!Util.isEmptyString(path) &&
                    path.startsWith("http")   &&
                    !Util.isYouTubeLink(path)   )
            {
                title = path; //set default
                final CustomWebView web = new CustomWebView(act);
                web.loadUrl(path);
                web.setVisibility(View.INVISIBLE);
                web.setWebChromeClient(new WebChromeClient() {
                    @Override
                    public void onReceivedTitle(WebView view, String titleReceived) {
                        super.onReceivedTitle(view, titleReceived);
                        if (!TextUtils.isEmpty(titleReceived) &&
                                !titleReceived.equalsIgnoreCase("about:blank"))
                        {
                            SharedPreferences pref_show_note_attribute = act.getSharedPreferences("add_new_note_option", 0);
                            if(pref_show_note_attribute
                                    .getString("KEY_ENABLE_LINK_TITLE_SAVE", "yes")
                                    .equalsIgnoreCase("yes"))
                            {
                                Date now = new Date();
                                dB_page.updateNote(rowId, titleReceived, "", "", "", link, "", 0, now.getTime(), true); // update note
                            }

                            int count = dB_page.getNotesCount(true);
                            if( pref_show_note_attribute.getString("KEY_ADD_NEW_NOTE_TO","bottom").equalsIgnoreCase("top") &&
                                    (count > 1)        )
                            {
                                Page.swap(dB_page);
                            }

                            Toast.makeText(act,
                                    act.getResources().getText(R.string.add_new_note_option_title) + titleReceived,
                                    Toast.LENGTH_SHORT)
                                    .show();
                            CustomWebView.pauseWebView(web);
                            CustomWebView.blankWebView(web);
                            if(Page.mItemAdapter != null)
                                Page.mItemAdapter.notifyDataSetChanged();
                            title = titleReceived;
                        }
                    }
                });
            }
            else // other
            {
                title = pathOri;
                if (pref_show_note_attribute.getString("KEY_ADD_NEW_NOTE_TO", "bottom").equalsIgnoreCase("top") &&
                        (count > 1)) {
                    Page.swap(dB_page);
                }

                Toast.makeText(act,
                        act.getResources().getText(R.string.add_new_note_option_title) + title,
                        Toast.LENGTH_SHORT)
                        .show();
            }

            return title;
        }
        else
            return null;
    }


    /****************************
     *          YouTube
     *
     ****************************/
    /**
     *  get YouTube link
     */
    String getYouTubeLink(FragmentActivity act,int pos)
    {
        DB_page dB_page = new DB_page(act, Pref.getPref_focusView_page_tableId(act));

        dB_page.open();
        int count = dB_page.getNotesCount(false);
        dB_page.close();

        if(pos >= count)
        {
            pos = 0;
            Page.currPlayPosition = 0;
        }

        String linkStr="";
        if(pos < count)
            linkStr =dB_page.getNoteLinkUri(pos,true);

        return linkStr;
    }

    /**
     *  launch next YouTube intent
     */
    void launchNextYouTubeIntent(FragmentActivity act,Handler handler,Runnable runCountDown)
    {
        SharedPreferences pref_open_youtube;
        pref_open_youtube = act.getSharedPreferences("show_note_attribute", 0);

        String link = getYouTubeLink(act,Page.currPlayPosition);
        if( Util.isYouTubeLink(link) &&
                pref_open_youtube.getString("KEY_VIEW_NOTE_LAUNCH_YOUTUBE", "no").equalsIgnoreCase("yes") )
        {
            Util.openLink_YouTube(act, link);
            cancelYouTubeHandler(handler,runCountDown);
        }
    }

    /**
     *  cancel YouTube Handler
     */
    void cancelYouTubeHandler(Handler handler,Runnable runCountDown)
    {
        if(handler != null) {
            handler.removeCallbacks(runCountDown);
//            handler = null;
        }
    }

}
