
package com.hlidskialf.android.filer;

import android.net.Uri;
import android.content.ContentProvider;
import android.content.UriMatcher;
import android.content.ContentValues;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.database.Cursor;
import android.database.SQLException;

public class MimeProvider extends ContentProvider {
  private SQLiteOpenHelper mOpenHelper;

  public static final int URI_MATCH_MIMETYPE=1;
  public static final int URI_MATCH_MIMETYPE_ID=2;
  public static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

  static {
    sUriMatcher.addURI("com.hlidskialf.android.filer", "mimetype", URI_MATCH_MIMETYPE);
    sUriMatcher.addURI("com.hlidskialf.android.filer", "mimetype/#", URI_MATCH_MIMETYPE_ID);
  }

  private static class MimeDatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "mimetypes.db";
    private static final int DATABASE_VERSION = 1;

    public MimeDatabaseHelper(Context context) {
      super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
      db.execSQL("CREATE TABLE "+ Filer.MimeColumns.TABLE_NAME +" (" +
        "extension TEXT, " +
        "mimetype TEXT, " +
        "icon TEXT, " +
        "action TEXT, " +
        "_id INTEGER PRIMARY KEY);");

      //insert defaults;
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int curVersion) {
      db.execSQL("DROP TABLE IF EXISTS "+Filer.MimeColumns.TABLE_NAME);
      onCreate(db);
    }
  };

  public MimeProvider() { }

  @Override
  public boolean onCreate() {
    mOpenHelper = new MimeDatabaseHelper(getContext());
    return true;
  }

  @Override
  public String getType(Uri uri)  { 
    switch(sUriMatcher.match(uri)) {
      case URI_MATCH_MIMETYPE:
        return "vnd.android.cursor.dir/"+Filer.MimeColumns.TABLE_NAME;
      case URI_MATCH_MIMETYPE_ID:
        return "vnd.android.cursor.item/"+Filer.MimeColumns.TABLE_NAME;
      default:
        throw new IllegalArgumentException("Unknown URI");
    }
  }

  @Override
  public Uri insert(Uri uri, ContentValues values) { 
    if (sUriMatcher.match(uri) != URI_MATCH_MIMETYPE) {
      throw new IllegalArgumentException("Cannot insert into URI: " + uri);
    }

    if ((values == null) || (!values.containsKey(Filer.MimeColumns.EXTENSION)) || (!values.containsKey(Filer.MimeColumns.MIMETYPE))) {
      throw new IllegalArgumentException("Missing required fields: " + uri);
    }

    if (!values.containsKey(Filer.MimeColumns.ACTION))
      values.put(Filer.MimeColumns.ACTION, Intent.ACTION_VIEW);

    SQLiteDatabase db = mOpenHelper.getWritableDatabase();
    long id = db.insert(Filer.MimeColumns.TABLE_NAME, Filer.MimeColumns.EXTENSION, values);
    if (id < 0)
      throw new SQLException("Failed to insert row into " + uri);

    Uri new_uri = ContentUris.withAppendedId(Filer.MimeColumns.CONTENT_URI, id);
    getContext().getContentResolver().notifyChange(new_uri, null);
    return new_uri;
  }

  @Override
  public int update(Uri uri, ContentValues values, String where, String[] where_args) { 
    if (sUriMatcher.match(uri) != URI_MATCH_MIMETYPE_ID)
      throw new UnsupportedOperationException("Cannot update URI: " + uri);

    SQLiteDatabase db = mOpenHelper.getWritableDatabase();
    String segment = uri.getPathSegments().get(1);
    long id = Long.parseLong(segment);
    int count = db.update(Filer.MimeColumns.TABLE_NAME, values, "_id="+id, null);

    getContext().getContentResolver().notifyChange(uri, null);
    return count;

  }

  @Override
  public int delete(Uri uri, String where, String[] where_args) { 
    SQLiteDatabase db = mOpenHelper.getWritableDatabase();
    int count;
    switch (sUriMatcher.match(uri)) {
      case URI_MATCH_MIMETYPE:
        count = db.delete(Filer.MimeColumns.TABLE_NAME, where, where_args);
        break;
      case URI_MATCH_MIMETYPE_ID:
        String segment = uri.getPathSegments().get(1);
        long id = Long.parseLong(segment);
        if (where == null || where.length() < 1)
          where = "_id=" + segment;
        else
          where = "_id=" + segment + " AND (" + where + ")";
        count = db.delete(Filer.MimeColumns.TABLE_NAME, where, where_args);
        break;
      default:
        throw new IllegalArgumentException("Cannot delete from URI: " + uri);
    }

    getContext().getContentResolver().notifyChange(uri, null);
    return count;
  }

  @Override
  public Cursor query(Uri uri, String[] projection, String selection, String[] selection_args, String sort) { 
    SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

    switch(sUriMatcher.match(uri)) {
      case URI_MATCH_MIMETYPE:
        qb.setTables(Filer.MimeColumns.TABLE_NAME);
        break;
      case URI_MATCH_MIMETYPE_ID:
        qb.setTables(Filer.MimeColumns.TABLE_NAME);
        qb.appendWhere("_id=");
        qb.appendWhere(uri.getPathSegments().get(1));
        break;
      default:
        throw new IllegalArgumentException("Unknown URI: " + uri);
    }
    SQLiteDatabase db = mOpenHelper.getReadableDatabase();
    Cursor ret = qb.query(db, projection, selection, selection_args, null, null, sort);

    if (ret != null)
      ret.setNotificationUri(getContext().getContentResolver(), uri);
    
    return ret;
  }

}
