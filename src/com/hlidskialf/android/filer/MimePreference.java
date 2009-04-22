package com.hlidskialf.android.filer;

import android.content.Context;
import android.util.AttributeSet;
import android.database.Cursor;
import android.preference.DialogPreference;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ListView;
import android.net.Uri;

public class MimePreference extends DialogPreference
{
  private Context mContext;
  private ListView mListView;
  private Cursor mCursor;
  private MimeAdapter mAdapter;
  private LayoutInflater mFactory;

  private class MimeAdapter extends CursorAdapter {
    public MimeAdapter(Context context, Cursor cursor) {
      super(context,cursor);
    }
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
      View ret = mFactory.inflate(R.layout.mime_list_item, parent, false);
      return ret;
    }
    @Override
    public void bindView(View v, Context context, Cursor cursor) {
      final int id = cursor.getInt(Filer.MimeColumns.MIME_ID_INDEX);
      final String ext = cursor.getString(Filer.MimeColumns.MIME_EXTENSION_INDEX);
      final String mime = cursor.getString(Filer.MimeColumns.MIME_MIMETYPE_INDEX);
      final String icon_uri = cursor.getString(Filer.MimeColumns.MIME_ICON_INDEX);
      final String action = cursor.getString(Filer.MimeColumns.MIME_ACTION_INDEX);

      ImageView iv;
      TextView tv;
      iv = (ImageView)v.findViewById(R.id.row_icon);
      if (iv != null) {
        Filer.setImageFromUri(iv, Uri.parse(icon_uri));
      }

      tv = (TextView)v.findViewById(R.id.row_ext);
      if (tv != null) tv.setText(ext);

      tv = (TextView)v.findViewById(R.id.row_mime);
      if (tv != null) tv.setText(mime);

      tv = (TextView)v.findViewById(R.id.row_action);
      if (tv != null) tv.setText(action);
    }
  }

  public MimePreference(Context context, AttributeSet attrs)
  {
    super(context,attrs);
    mContext = context;
    mFactory = LayoutInflater.from(context);
  }

  @Override
  protected void onBindDialogView(View v) 
  {
    mListView = (ListView)v.findViewById(android.R.id.list);

    mCursor = Filer.getMimeCursor(mContext);
    if (mCursor != null) {
      mAdapter = new MimeAdapter(mContext, mCursor);
      mListView.setAdapter(mAdapter);
    }
    else {
      android.util.Log.v("MimePreference", "COULDNT GET CURSOR");
    }
  }

  @Override 
  protected void onDialogClosed(boolean positiveResult) 
  {
    super.onDialogClosed(positiveResult);

    if (mCursor != null)
      mCursor.deactivate();
  }
}
