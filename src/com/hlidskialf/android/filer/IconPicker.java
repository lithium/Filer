package com.hlidskialf.android.filer;

import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import java.util.ArrayList;
import java.util.Arrays;

public class IconPicker extends Dialog
{
  private static final String[] RESOURCE_NAMES = {
    "drawable://com.hlidskialf.android.filer/mimetype_ascii",
    "drawable://com.hlidskialf.android.filer/mimetype_binary",
    "drawable://com.hlidskialf.android.filer/mimetype_document",
    "drawable://com.hlidskialf.android.filer/mimetype_encrypted",
    "drawable://com.hlidskialf.android.filer/mimetype_folder",
    "drawable://com.hlidskialf.android.filer/mimetype_font_bitmap",
    "drawable://com.hlidskialf.android.filer/mimetype_log",
    "drawable://com.hlidskialf.android.filer/mimetype_html",
    "drawable://com.hlidskialf.android.filer/mimetype_make",
    "drawable://com.hlidskialf.android.filer/mimetype_man",
    "drawable://com.hlidskialf.android.filer/mimetype_message",
    "drawable://com.hlidskialf.android.filer/mimetype_midi",
    "drawable://com.hlidskialf.android.filer/mimetype_mime_txt",
    "drawable://com.hlidskialf.android.filer/mimetype_quicktime",
    "drawable://com.hlidskialf.android.filer/mimetype_sound",
    "drawable://com.hlidskialf.android.filer/mimetype_spreadsheet",
    "drawable://com.hlidskialf.android.filer/mimetype_soffice",
    "drawable://com.hlidskialf.android.filer/mimetype_tar",
    "drawable://com.hlidskialf.android.filer/mimetype_txt",
    "drawable://com.hlidskialf.android.filer/mimetype_vcalendar",
    "drawable://com.hlidskialf.android.filer/mimetype_video",
  };

  private Context mContext;
  private GridView mGrid;
  private OnIconPickedListener mPickedListener;
  private Resources mResources;
  private ArrayList<String> mIcons;

  static public interface OnIconPickedListener {
    abstract public void onIconPicked(String icon);
  }


  public IconPicker(Context ctx, String title) {
    super(ctx); 
    mContext = ctx;
    mResources = mContext.getResources();
    mGrid = new GridView(ctx);
    mGrid.setNumColumns(4);
    setContentView(mGrid);
    setTitle(title);

    mIcons = new ArrayList<String>(Arrays.asList(RESOURCE_NAMES));

    mGrid.setAdapter( new BaseAdapter() {
      public View getView(int pos, View convert, ViewGroup parent) {
        ImageView i;
        if (convert == null) {
          i = new ImageView(mContext);
          i.setScaleType(ImageView.ScaleType.FIT_CENTER);
          i.setLayoutParams(new GridView.LayoutParams(48, 48));
        }
        else {
          i = (ImageView) convert;
        }
        String uri = mIcons.get(pos);
        Filer.setImageFromUri(i, Uri.parse(uri));

        return i;
      }
      public final int getCount() { return mIcons.size(); }
      public final Object getItem(int pos) { return mIcons.get(pos); }
      public final long getItemId(int pos) { return pos; }
    });
    mGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      public void onItemClick(AdapterView parent, View view, int pos, long id) {
        if (mPickedListener != null) {
          ImageView iv = (ImageView)view;
          mPickedListener.onIconPicked(mIcons.get(pos));
        }
        dismiss();
      }
    });
  }
  public void setOnIconPickedListener(IconPicker.OnIconPickedListener listener) { mPickedListener = listener; }

}
