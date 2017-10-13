package com.cw.litenote.db;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.cw.litenote.R;
import com.cw.litenote.main.MainAct;
import com.cw.litenote.define.Define;

// Data Base Helper 
class DatabaseHelper extends SQLiteOpenHelper
{  
    static final String DB_NAME = "litenote.db";
    private static int DB_VERSION = 1;
    
    DatabaseHelper(Context context)
    {  
        super(context, DB_NAME , null, DB_VERSION);
    }

    @Override
    //Called when the database is created ONLY for the first time.
    public void onCreate(SQLiteDatabase sqlDb)
    {   
    	String tableCreated;
    	String DB_CREATE;
    	
    	// WritableDatabase(i.e. sqlDb) is created
    	DB_drawer.mSqlDb = sqlDb;
		DB_folder.mSqlDb = sqlDb; // add for DB_drawer.insertFolderTable below
		DB_page.mSqlDb = sqlDb; // add for DB_folder.insertPageTable below

    	System.out.println("DatabaseHelper / _onCreate");

		// Create Drawer table
		tableCreated = DB_drawer.DB_DRAWER_TABLE_NAME;
		DB_CREATE = "CREATE TABLE IF NOT EXISTS " + tableCreated + "(" +
				DB_drawer.KEY_FOLDER_ID + " INTEGER PRIMARY KEY," +
				DB_drawer.KEY_FOLDER_TABLE_ID + " INTEGER," +
				DB_drawer.KEY_FOLDER_TITLE + " TEXT," +
				DB_drawer.KEY_FOLDER_CREATED + " INTEGER);";
		sqlDb.execSQL(DB_CREATE);

		// Create original folder tables and page tables
    	if(Define.HAS_ORIGINAL_TABLES)
    	{
	    	for(int i = 1; i<= Define.ORIGIN_FOLDERS_COUNT; i++)
	    	{
                // folder tables
	        	System.out.println("DatabaseHelper / _onCreate / will insert folder table " + i);
                DB_drawer db_drawer = new DB_drawer(MainAct.mAct);
                String folderTitle = MainAct.mAct.getResources().getString(R.string.default_folder_name).concat(String.valueOf(i));
                db_drawer.insertFolder(i, folderTitle,false ); // Note: must set false for DB creation stage
                db_drawer.insertFolderTable(db_drawer, i, false);

                // page tables
	        	for(int j = 1; j<= Define.ORIGIN_PAGES_COUNT; j++)
	        	{
	            	System.out.println("DatabaseHelper / _onCreate / will insert page table " + j);
					DB_folder db_folder = new DB_folder(MainAct.mAct,i);
					db_folder.insertPageTable(db_folder, i, j, false);

                    String DB_FOLDER_TABLE_PREFIX = "Folder";
                    String folder_table = DB_FOLDER_TABLE_PREFIX.concat(String.valueOf(i));
                    db_folder.insertPage(sqlDb,
                                         folder_table,
                                         Define.getTabTitle(MainAct.mAct,1),
                                         1,
                                         Define.STYLE_DEFAULT);//Define.STYLE_PREFER
                    //db_folder.insertPage(sqlDb,folder_table,"N2",2,1);
                    //db_folder.insertPage(sqlDb,folder_table,"N3",3,2);
                    //db_folder.insertPage(sqlDb,folder_table,"N4",4,3);
                    //db_folder.insertPage(sqlDb,folder_table,"N5",5,4);
	        	}
	    	}
    	}

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    { //??? how to upgrade?
//            db.execSQL("DROP DATABASE IF EXISTS "+DATABASE_TABLE); 
//            System.out.println("DB / _onUpgrade / drop DB / DATABASE_NAME = " + DB_NAME);
 	    onCreate(db);
    }
    
    @Override
    public void onDowngrade (SQLiteDatabase db, int oldVersion, int newVersion)
    { 
//            db.execSQL("DROP DATABASE IF EXISTS "+DATABASE_TABLE); 
//            System.out.println("DB / _onDowngrade / drop DB / DATABASE_NAME = " + DB_NAME);
 	    onCreate(db);
    }
}
