package com.cw.litenote.define;

import android.app.Activity;
import android.content.Context;

import com.cw.litenote.R;

/**
 * Created by CW on 2016/6/16.
 *
 * build apk file size:
 * 1) prefer w/ assets files: 15,483 KB
 *
 * 2) default w/ assets files: 15,483 KB
 *
 * 3) default w/o assets files: 1,173 KB
 *
 * 4) release: 706 KB
 */
public class Define {

    public static boolean DEBUG_MODE = false;
    public static boolean RELEASE_MODE = !DEBUG_MODE;

    /**
     * Set release/debug mode
     * - RELEASE_MODE
     * - DEBUG_MODE
     */
    public static boolean CODE_MODE = DEBUG_MODE; //DEBUG_MODE; //RELEASE_MODE;


    /****************************************************************************
     *
     * Flags for Default tables after App installation:
     * - HAS_PREFERRED_TABLES
     * - HAS_ORIGINAL_TABLES
     * Note of flag setting: exclusive
     *
     ****************************************************************************/

    /**
     * Has preferred tables
     * - true : un-mark preferred/assets/ line in build.gradle file
     * - false:    mark preferred/assets/ line in build.gradle file
     *
     *
     * android {
     * ...
     *    sourceSets {
     *        main {
     *      // mark: Has original tables
     *      // un-mark: Has preferred tables
     *      // Apk file size will increase if assets directory is set at default location (src/main/assets)
     *           assets.srcDirs = ['preferred/assets/']
     *      }
     *    }
     * }
     *
     */
    public static boolean HAS_PREFERRED_TABLES = false; //true; //false;

    /**
     * Has original tables
     * - folder tables: 2
     * - page tables: 1
     *
     */
    public static boolean HAS_ORIGINAL_TABLES = !HAS_PREFERRED_TABLES;

    // Apply system default for picture path
    public static boolean PICTURE_PATH_BY_SYSTEM_DEFAULT = true;

    // default table count
    public static int ORIGIN_PAGES_COUNT = 1;//5; // Page1_1, Page1_2, Page1_3, Page1_4, Page1_5
    public static int ORIGIN_FOLDERS_COUNT = 2;  // Folder1, Folder2, Folder3

    // default style
    public static int STYLE_DEFAULT = 1;
    public static int STYLE_PREFER = 2;

    public static String getFolderTitle(Activity act, Integer i)
    {
        String title = null;
        if(Define.HAS_PREFERRED_TABLES) {
            if (i == 0)
                title = act.getResources().getString(R.string.prefer_folder_name_local);
            else if (i == 1)
                title = act.getResources().getString(R.string.prefer_folder_name_web);
        }
        else {
            title = act.getResources().getString(R.string.default_folder_name).concat(String.valueOf(i+1));
        }
        return title;
    }

    public static String getTabTitle(Context context, Integer Id)
    {
        String title;

        if(Define.HAS_PREFERRED_TABLES) {
            title = context.getResources().getString(R.string.prefer_page_name).concat(String.valueOf(Id));
        }
        else {
            title = context.getResources().getString(R.string.default_page_name).concat(String.valueOf(Id));
        }
        return title;
    }
}
