package com.hlidskialf.android.filer;

import java.util.GregorianCalendar;
import java.util.Calendar;
import java.util.Date;
import java.io.File;
import java.text.SimpleDateFormat;
import java.text.DecimalFormat;
import android.provider.BaseColumns;
import android.net.Uri;
import android.content.Intent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ContentValues;
import android.content.ContentUris;
import android.database.Cursor;
import android.widget.ImageView;


public class Filer 
{
  public static final String PREF_BROWSE_ROOT = "browse_root";
  public static final String PREF_HIDE_DOT = "hide_dot";
  public static final String PREF_HOME_PATH = "home_path";
  public static final String PREF_RECURSIVE_DELETE = "recursive_delete";


  public static final String FORMAT_DECIMAL = "0.#";
  public static final String FORMAT_DATE_TIME = "MMM dd HH:mm";
  public static final String FORMAT_DATE_YEAR = "MMM dd yyyy";
  private final SimpleDateFormat pDateFmt_time = new SimpleDateFormat("MMM dd HH:mm");
  private final SimpleDateFormat pDateFmt_year  = new SimpleDateFormat("MMM dd yyyy");

  public static String format_size(long size)
  {
    DecimalFormat fmt = new DecimalFormat(FORMAT_DECIMAL);
    String ret;
    if (size > 1024*1024*1024) ret = fmt.format((double)size / (double)(1024*1024*1024)) + "G";
    else if (size > 1024*1024) ret = fmt.format((double)size / (double)(1024*1024)) + "M";
    else if (size > 1024) ret = fmt.format((double)size / (double)1024) + "k";
    else ret = fmt.format(size) + "b";
    return ret;
  }
  public static String format_date(long when)
  {
    Date last = new Date(when);
    GregorianCalendar now = new GregorianCalendar();
    GregorianCalendar then = new GregorianCalendar();
    then.setTime(last);

    SimpleDateFormat fmt = new SimpleDateFormat(now.get(Calendar.YEAR) == then.get(Calendar.YEAR) ? FORMAT_DATE_TIME : FORMAT_DATE_YEAR);
    return fmt.format(last);
  }


  public static class MimeColumns implements BaseColumns {
    public static final Uri CONTENT_URI = Uri.parse("content://com.hlidskialf.android.filer/mimetype");
    public static final String _ID = "_id";
    public static final String DEFAULT_SORT_ORDER = "mimetype ASC";
    public static final String TABLE_NAME = "mimetypes";

    public static final String EXTENSION="extension";
    public static final String MIMETYPE="mimetype";
    public static final String ICON="icon";
    public static final String ACTION="action";

    static final String[] MIME_QUERY_COLUMNS = { _ID, EXTENSION, MIMETYPE, ICON, ACTION };

    public static final int MIME_ID_INDEX = 0;
    public static final int MIME_EXTENSION_INDEX = 1;
    public static final int MIME_MIMETYPE_INDEX = 2;
    public static final int MIME_ICON_INDEX = 3;
    public static final int MIME_ACTION_INDEX = 4;
  }


  public interface MimetypeReporter {
    public void reportMime(int id, String ext, String mimetype, String icon, String action);
  }
  public synchronized static void getMimetypes(MimetypeReporter reporter, Cursor cur) 
  {
    if (!cur.moveToFirst()) return;
    do {
      int id = cur.getInt(MimeColumns.MIME_ID_INDEX);
      String ext = cur.getString(MimeColumns.MIME_EXTENSION_INDEX);
      String mime = cur.getString(MimeColumns.MIME_MIMETYPE_INDEX);
      String icon = cur.getString(MimeColumns.MIME_ICON_INDEX);
      String action = cur.getString(MimeColumns.MIME_ACTION_INDEX);
      reporter.reportMime(id, ext, mime, icon, action);
    } while (cur.moveToNext());
  }
  public synchronized static void getMimetype(Context context, int mime_id, MimetypeReporter reporter)
  {
    Cursor cursor = context.getContentResolver().query(
        ContentUris.withAppendedId(MimeColumns.CONTENT_URI, mime_id), 
        MimeColumns.MIME_QUERY_COLUMNS, null, null, MimeColumns.DEFAULT_SORT_ORDER);
    Filer.getMimetypes(reporter, cursor);
    cursor.close();
  }

  public synchronized static String getIconFromExtension(Context context, String extension)
  {
    if (extension == null) return null;
    Cursor cursor = context.getContentResolver().query(MimeColumns.CONTENT_URI, 
        new String[] { MimeColumns.ICON }, 
        MimeColumns.EXTENSION+"=?", new String[] {extension},
        null);
    String ret = null;
    if (cursor.moveToFirst()) {
      ret = cursor.getString(0);
    }
    cursor.close();
    return ret;
  }
  public synchronized static Intent getIntentFromFile(Context context, File file)
  {
    Intent ret = new Intent();
    String type = "text/*";
    String action = Intent.ACTION_VIEW;
    String extension = Filer.getExtension(file.getName());
    if (extension == null) return ret;

    Cursor cursor = context.getContentResolver().query(MimeColumns.CONTENT_URI, 
        new String[] { MimeColumns.MIMETYPE, MimeColumns.ACTION }, 
        MimeColumns.EXTENSION+"=?", new String[] {extension},
        null);
    if (cursor.moveToFirst()) {
      type = cursor.getString(0);
      action = cursor.getString(1);
    }
    ret.setDataAndType(Uri.fromFile(file), type);
    ret.setAction(action);
    cursor.close();
    return ret;
  }

  public synchronized static Cursor getMimeCursor(Context context)
  {
    return context.getContentResolver().query(MimeColumns.CONTENT_URI, MimeColumns.MIME_QUERY_COLUMNS, null, null, MimeColumns.DEFAULT_SORT_ORDER);
  }
  public synchronized static Uri insertMimetype(Context context, String ext, String mime, String icon, String action)
  {
    ContentValues values = new ContentValues(5);
    ContentResolver resolver = context.getContentResolver();
    values.put(MimeColumns.EXTENSION, ext);
    values.put(MimeColumns.MIMETYPE, mime);
    values.put(MimeColumns.ICON, icon);
    values.put(MimeColumns.ACTION, action);
    return resolver.insert(MimeColumns.CONTENT_URI, values);
  }

  public synchronized static void updateMimetype(Context context, int id, String extension, String mimetype, String icon, String action) 
  {
    ContentValues values = new ContentValues(5);
    ContentResolver resolver = context.getContentResolver();

    values.put(MimeColumns.EXTENSION, extension);
    values.put(MimeColumns.MIMETYPE, mimetype);
    values.put(MimeColumns.ICON, icon);
    values.put(MimeColumns.ACTION, action);

    resolver.update(ContentUris.withAppendedId(MimeColumns.CONTENT_URI, id), values, null, null);
  }

  private synchronized static void deleteMimetype(Context context, int id)
  {
    ContentResolver resolver = context.getContentResolver();
    resolver.delete(ContentUris.withAppendedId(MimeColumns.CONTENT_URI, id), "", null);
  }


  public static boolean setImageFromUri(ImageView iv, Uri uri)
  {
    if (iv == null || uri == null) return false;
    String scheme = uri.getScheme();
    if (scheme == null) return false;
    if (scheme.equals("drawable")) {
      int id = iv.getContext().getResources().getIdentifier(uri.getLastPathSegment(), uri.getScheme(), uri.getAuthority());
      iv.setImageResource(id);
    }
    else {
      iv.setImageURI(uri);
    }
    return true;
  }

  public static String getExtension(String s)
  {
    int idx = s.lastIndexOf('.');
    return idx == -1 ? null : s.substring(idx);
  }
}
