package com.rojama.pianoshelf;

import java.util.Vector;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper {
	private static final String DATABASE_NAME = "dbForPianoShelf.db";
	private static final int DATABASE_VERSION = 3;
	private static final String TABLE_RECENT_NAME = "RECENT";
	private static final String TABLE_FAVORITE_NAME = "FAVORITE";

	public DatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		String sql = "CREATE TABLE " + TABLE_RECENT_NAME
				+ " (created_at TIMESTAMP NOT NULL PRIMARY KEY," + "filepath VARCHAR(512));";
		Log.i("haiyang:createDB=", sql);
		db.execSQL(sql);
		sql = "CREATE TABLE " + TABLE_FAVORITE_NAME
				+ " (created_at TIMESTAMP NOT NULL PRIMARY KEY," + "filepath VARCHAR(512));";
		Log.i("haiyang:createDB=", sql);
		db.execSQL(sql);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		String sql = "DROP TABLE " + TABLE_RECENT_NAME + ";";
		Log.i("haiyang:createDB=", sql);
		db.execSQL(sql);
		sql = "DROP TABLE " + TABLE_FAVORITE_NAME + ";";
		Log.i("haiyang:createDB=", sql);
		db.execSQL(sql);
		sql = "CREATE TABLE " + TABLE_RECENT_NAME + " (created_at TIMESTAMP NOT NULL PRIMARY KEY,"
				+ "filepath VARCHAR(512));";
		Log.i("haiyang:createDB=", sql);
		db.execSQL(sql);
		sql = "CREATE TABLE " + TABLE_FAVORITE_NAME
				+ " (created_at TIMESTAMP NOT NULL PRIMARY KEY," + "filepath VARCHAR(512));";
		Log.i("haiyang:createDB=", sql);
		db.execSQL(sql);
	}

	public int getTableCount(String tableName, SQLiteDatabase db) {
		int count = 0;
		Cursor cursor = null;
		try {
			cursor = db.rawQuery("select count(*) from " + tableName, null);
			if (cursor.moveToFirst()) {
				count = cursor.getInt(0);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return count;
	}

	public void insertRecentItem(String filepath) {
		SQLiteDatabase db = this.getWritableDatabase();
		String sql1 = "delete from " + TABLE_RECENT_NAME + " where filepath = '" + filepath + "'; ";
		String sql2 = "insert into " + TABLE_RECENT_NAME
				+ " (created_at,filepath) values(strftime('%s','now'), '" + filepath + "');";
		String sql3 = "delete from " + TABLE_RECENT_NAME
				+ " where created_at = (select min(created_at) from " + TABLE_RECENT_NAME + ");";
		try {
			Log.i("haiyang:sql1=", sql1);
			db.execSQL(sql1);
			Log.i("haiyang:sql2=", sql2);
			db.execSQL(sql2);
			if (getTableCount(TABLE_RECENT_NAME, db) > 1000) {
				Log.i("haiyang:sql3=", sql3);
				db.execSQL(sql3);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			db.close();
		}
	}

	public void insertFavoriteItem(String filepath) {
		SQLiteDatabase db = this.getWritableDatabase();
		String sql1 = "delete from " + TABLE_FAVORITE_NAME + " where filepath = '" + filepath
				+ "'; ";
		String sql2 = "insert into " + TABLE_FAVORITE_NAME
				+ " (created_at,filepath) values(strftime('%s','now'), '" + filepath + "');";
		try {
			Log.i("haiyang:sql1=", sql1);
			db.execSQL(sql1);
			Log.i("haiyang:sql2=", sql2);
			db.execSQL(sql2);
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			db.close();
		}
	}

	public void deleteFavoriteItem(String filepath) {
		SQLiteDatabase db = this.getWritableDatabase();
		String sql2 = "delete from " + TABLE_FAVORITE_NAME + " where filepath = '" + filepath
				+ "';";
		try {
			Log.i("haiyang:sql2=", sql2);
			db.execSQL(sql2);
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			db.close();
		}
	}

	public Vector<String> selectRecentItem(String limit) {
		Vector<String> returnItem = new Vector<String>();
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor result = null;
		try {
			result = db.query(TABLE_RECENT_NAME, new String[] { "filepath" }, null, null, null,
					null, "created_at desc", limit);
			result.moveToFirst();
			while (!result.isAfterLast()) {
				returnItem.add(result.getString(0));
				result.moveToNext();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			if (result != null)
				result.close();
			db.close();
		}
		return returnItem;
	}

	public Vector<String> selectFavoriteItem() {
		Vector<String> returnItem = new Vector<String>();
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor result = null;
		try {
			result = db.query(TABLE_FAVORITE_NAME, new String[] { "filepath" }, null, null, null,
					null, "created_at desc");
			result.moveToFirst();
			while (!result.isAfterLast()) {
				returnItem.add(result.getString(0));
				result.moveToNext();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			if (result != null)
				result.close();
			db.close();
		}
		return returnItem;
	}
}
