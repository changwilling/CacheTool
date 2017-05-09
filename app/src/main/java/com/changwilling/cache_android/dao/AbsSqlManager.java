package com.changwilling.cache_android.dao;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import com.changwilling.cache_android.app.TApplication;

/**
 * Created by changwilling on 17/1/9.
 * sql控制父类
 */

public class AbsSqlManager {
    private static DatabaseHelper databaseHelper;
    private static SQLiteDatabase sqliteDB;
    private static String database_name;

    public AbsSqlManager() {
        String dbName = getDataBaseName();
        if (TextUtils.isEmpty(dbName)) {
            return;
        }
        openDatabase(TApplication.getInstance().getApplicationContext(), dbName, 1);
    }

    /**
     * 获取私有用户的唯一数据库名字
     *
     * @return
     */
    private synchronized static String getDataBaseName() {
        if (database_name != null) {
            return database_name;
        }
        try {
            //根据用户的缓存，返回用户的私有数据库链接,目前测试使用下面的数据库名称
            database_name = "HLYManager.db";
            return database_name;
        } catch (Exception e) {
//            return Syste+".db";
            return null;
        }
    }

    /**
     * 打开对数据库的连接
     *
     * @param context
     * @param dbName
     * @param databaseVersion
     */
    private void openDatabase(Context context, String dbName, int databaseVersion) {
        if (databaseHelper == null) {
            synchronized (AbsSqlManager.class) {
                if (databaseHelper == null) {
                    databaseHelper = new DatabaseHelper(context, dbName, this, databaseVersion);
                }
            }
        }
        if (sqliteDB == null) {
            synchronized (AbsSqlManager.class) {
                if (sqliteDB == null) {
                    sqliteDB = databaseHelper.getWritableDatabase();
                }
            }
        }
    }

    public void destroy() {
        try {
            if (databaseHelper != null) {
                databaseHelper.close();
                databaseHelper = null;
            }
            closeDB();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void open(boolean isReadonly) {
        if (sqliteDB == null) {
            if (isReadonly) {
                sqliteDB = databaseHelper.getReadableDatabase();
            } else {
                sqliteDB = databaseHelper.getWritableDatabase();
            }
        }
    }

    public final void reopen() {
        closeDB();
        open(false);
    }

    private void closeDB() {
        if (sqliteDB != null) {
            sqliteDB.close();
            sqliteDB = null;
        }
    }

    protected final SQLiteDatabase sqliteDB() {
        open(false);
        return sqliteDB;
    }

    static class DatabaseHelper extends SQLiteOpenHelper {
        private AbsSqlManager mAbsSqlManager;

        //需要动态的根据不同的user，加载不同的database_name
        private static String DATABASE_NAME = "";

        /**
         * 以下是数据库表的名称
         */
        static final String TABLES_NAME_EXPIRECACHE = "table_expireCache";


        public DatabaseHelper(Context context, String dbName, AbsSqlManager mAbsSqlManager, int version) {
            this(context, mAbsSqlManager, dbName, null, version);
            DATABASE_NAME = dbName;
        }

        public DatabaseHelper(Context context, AbsSqlManager mAbsSqlManager, String name, SQLiteDatabase.CursorFactory factory, int version) {
            super(context, name, factory, version);
            this.mAbsSqlManager = mAbsSqlManager;
        }

        /**
         * oncreate方法只有在数据库第一次创建时才执行，因此在这里进行表的创建工作，只要数据库一创建，所有表创建完成
         *
         * @param db
         */
        @Override
        public void onCreate(SQLiteDatabase db) {
            createTables(db);//创建表
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            //数据库的版本更新
        }

        private void createTables(SQLiteDatabase db) {
            createTableForExpireCache(db);//创建管理缓存有效期的数据表

        }

        private void createTableForExpireCache(SQLiteDatabase db) {
            String sql = "CREATE TABLE IF NOT EXISTS "
                    + TABLES_NAME_EXPIRECACHE
                    + " ("
                    + ExpireCacheColum._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + ExpireCacheColum.EXPIRECACHEKEY_ID + " TEXT UNIQUE ON CONFLICT ABORT, "
                    + ExpireCacheColum.EXPIRE_ENDTIME + " INTEGER"
                    + ")";
            db.execSQL(sql);
        }
    }

    /**
     * 字段基类
     */
    class BaseColum {
        //为了区别（因为有的实体类也有id字段，这里对于autoIncrement的id加下划线）
        public static final String _ID = "_ID";
    }

    public class ExpireCacheColum extends BaseColum {
        public static final String EXPIRECACHEKEY_ID = "expireCacheKeyId";
        public static final String EXPIRE_ENDTIME = "expireEndTime";
    }

    protected void release() {
        destroy();
        closeDB();
        databaseHelper = null;
    }

    public synchronized static void closeDataBase() {
        if (databaseHelper != null) {
            databaseHelper.close();
            databaseHelper = null;
        }
        if (sqliteDB != null) {
            sqliteDB.close();
            sqliteDB = null;
        }
        //便于用户切换
        DatabaseHelper.DATABASE_NAME = null;
        database_name = null;
    }
}
