package com.rojama.pianoshelf;

import java.util.Vector;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper {
	private static final String TAG = "DatabaseHelper";
	private static final String DATABASE_NAME = "dbForPianoShelf.db";
	private static final int DATABASE_VERSION = 4; // bumped for safety fixes
	private static final String TABLE_RECENT_NAME = "RECENT";
	private static final String TABLE_FAVORITE_NAME = "FAVORITE";
	private static final String COL_CREATED_AT = "created_at";
	private static final String COL_FILEPATH = "filepath";
	private static final int MAX_RECENT_HARD_LIMIT = 1000;

	public DatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		createTables(db);
	}

	private void createTables(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_RECENT_NAME + " ("
				+ COL_CREATED_AT + " INTEGER NOT NULL PRIMARY KEY, "
				+ COL_FILEPATH + " TEXT NOT NULL);");
		db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_FAVORITE_NAME + " ("
				+ COL_CREATED_AT + " INTEGER NOT NULL PRIMARY KEY, "
				+ COL_FILEPATH + " TEXT NOT NULL);");
		// index for faster lookup by filepath (used in delete-by-path + dedupe)
		db.execSQL("CREATE INDEX IF NOT EXISTS idx_recent_filepath ON "
				+ TABLE_RECENT_NAME + "(" + COL_FILEPATH + ");");
		db.execSQL("CREATE INDEX IF NOT EXISTS idx_favorite_filepath ON "
				+ TABLE_FAVORITE_NAME + "(" + COL_FILEPATH + ");");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// Re-create safely; tables are small user metadata
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_RECENT_NAME);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_FAVORITE_NAME);
		db.execSQL("DROP INDEX IF EXISTS idx_recent_filepath");
		db.execSQL("DROP INDEX IF EXISTS idx_favorite_filepath");
		createTables(db);
	}

	/**
	 * Query count safely via parameterized SQLiteStatement equivalent.
	 *
	 * NOTE: tableName comes from code constants ONLY, never from user input.
	 * This is safe against injection here.
	 */
	public int getTableCount(String tableName, SQLiteDatabase db) {
		int count = 0;
		Cursor cursor = null;
		try {
			cursor = db.rawQuery("SELECT COUNT(*) FROM " + tableName, null);
			if (cursor != null && cursor.moveToFirst()) {
				count = cursor.getInt(0);
			}
		} catch (Exception e) {
			Log.e(TAG, "getTableCount failed for " + tableName, e);
		} finally {
			closeQuietly(cursor);
		}
		return count;
	}

	/**
	 * Insert (or refresh) a recent item.
	 *
	 * Fixes:
	 *  - SQL injection: uses ContentValues + ? args (never string-concat filepath)
	 *  - Atomicity: wraps in transaction (partial failure -> rollback)
	 *  - Dedup: removes existing entry for the same filepath first
	 *  - Bound: evicts oldest entries above MAX_RECENT_HARD_LIMIT
	 */
	public void insertRecentItem(String filepath) {
		if (filepath == null) return;
		SQLiteDatabase db = null;
		try {
			db = this.getWritableDatabase();
			db.beginTransaction();

			// 1) remove duplicates by filepath (safe with ? arg)
			db.delete(TABLE_RECENT_NAME, COL_FILEPATH + " = ?", new String[] { filepath });

			// 2) insert new row via ContentValues
			ContentValues cv = new ContentValues(2);
			cv.put(COL_CREATED_AT, System.currentTimeMillis() / 1000L); // unix seconds
			cv.put(COL_FILEPATH, filepath);
			db.insert(TABLE_RECENT_NAME, null, cv);

			// 3) trim if exceeded (parameterized bound)
			int count = getTableCount(TABLE_RECENT_NAME, db);
			if (count > MAX_RECENT_HARD_LIMIT) {
				int toRemove = count - MAX_RECENT_HARD_LIMIT;
				db.execSQL("DELETE FROM " + TABLE_RECENT_NAME
						+ " WHERE " + COL_CREATED_AT + " IN ("
						+ "SELECT " + COL_CREATED_AT + " FROM " + TABLE_RECENT_NAME
						+ " ORDER BY " + COL_CREATED_AT + " ASC LIMIT ?)",
						new Object[] { toRemove });
			}

			db.setTransactionSuccessful();
		} catch (SQLException e) {
			Log.e(TAG, "insertRecentItem failed", e);
		} finally {
			endTransactionQuietly(db);
			closeQuietly(db);
		}
	}

	/**
	 * Insert (or refresh) a favorite item.
	 */
	public void insertFavoriteItem(String filepath) {
		if (filepath == null) return;
		SQLiteDatabase db = null;
		try {
			db = this.getWritableDatabase();
			db.beginTransaction();

			db.delete(TABLE_FAVORITE_NAME, COL_FILEPATH + " = ?", new String[] { filepath });

			ContentValues cv = new ContentValues(2);
			cv.put(COL_CREATED_AT, System.currentTimeMillis() / 1000L);
			cv.put(COL_FILEPATH, filepath);
			db.insert(TABLE_FAVORITE_NAME, null, cv);

			db.setTransactionSuccessful();
		} catch (SQLException e) {
			Log.e(TAG, "insertFavoriteItem failed", e);
		} finally {
			endTransactionQuietly(db);
			closeQuietly(db);
		}
	}

	/**
	 * Delete a favorite item.
	 */
	public void deleteFavoriteItem(String filepath) {
		if (filepath == null) return;
		SQLiteDatabase db = null;
		try {
			db = this.getWritableDatabase();
			int rows = db.delete(TABLE_FAVORITE_NAME,
					COL_FILEPATH + " = ?", new String[] { filepath });
			Log.d(TAG, "deleteFavoriteItem removed rows=" + rows);
		} catch (SQLException e) {
			Log.e(TAG, "deleteFavoriteItem failed", e);
		} finally {
			closeQuietly(db);
		}
	}

	/**
	 * Read recent items up to `limit`.
	 *
	 * Fixes: handles empty cursor; safe Cursor/Database closure; safe limit parsing.
	 */
	public Vector<String> selectRecentItem(String limit) {
		Vector<String> returnItem = new Vector<String>();
		int parsedLimit;
		try {
			parsedLimit = Integer.parseInt(limit);
			if (parsedLimit <= 0) return returnItem;
		} catch (NumberFormatException e) {
			parsedLimit = 100; // default fallback
		}

		SQLiteDatabase db = null;
		Cursor result = null;
		try {
			db = this.getReadableDatabase();
			result = db.query(TABLE_RECENT_NAME,
					new String[] { COL_FILEPATH },
					null, null, null, null,
					COL_CREATED_AT + " DESC",
					String.valueOf(parsedLimit));
			if (result != null && result.moveToFirst()) {
				final int colIdx = result.getColumnIndexOrThrow(COL_FILEPATH);
				do {
					String path = result.getString(colIdx);
					if (path != null) returnItem.add(path);
				} while (result.moveToNext());
			}
		} catch (SQLException e) {
			Log.e(TAG, "selectRecentItem failed", e);
		} finally {
			closeQuietly(result);
			closeQuietly(db);
		}
		return returnItem;
	}

	/**
	 * Read all favorite items.
	 */
	public Vector<String> selectFavoriteItem() {
		Vector<String> returnItem = new Vector<String>();
		SQLiteDatabase db = null;
		Cursor result = null;
		try {
			db = this.getReadableDatabase();
			result = db.query(TABLE_FAVORITE_NAME,
					new String[] { COL_FILEPATH },
					null, null, null, null,
					COL_CREATED_AT + " DESC");
			if (result != null && result.moveToFirst()) {
				final int colIdx = result.getColumnIndexOrThrow(COL_FILEPATH);
				do {
					String path = result.getString(colIdx);
					if (path != null) returnItem.add(path);
				} while (result.moveToNext());
			}
		} catch (SQLException e) {
			Log.e(TAG, "selectFavoriteItem failed", e);
		} finally {
			closeQuietly(result);
			closeQuietly(db);
		}
		return returnItem;
	}

	// -------- small safe-close helpers --------

	private static void closeQuietly(Cursor c) {
		if (c != null && !c.isClosed()) {
			try { c.close(); } catch (Exception ignored) { /* no-op */ }
		}
	}

	private static void closeQuietly(SQLiteDatabase db) {
		if (db != null && db.isOpen()) {
			try { db.close(); } catch (Exception ignored) { /* no-op */ }
		}
	}

	private static void endTransactionQuietly(SQLiteDatabase db) {
		if (db != null && db.inTransaction()) {
			try { db.endTransaction(); } catch (Exception ignored) { /* no-op */ }
		}
	}
}
