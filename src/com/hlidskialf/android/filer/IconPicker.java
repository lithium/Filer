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
    "drawable://"+Filer.PACKAGE_NAME+"/mimetype_ascii",
    "drawable://"+Filer.PACKAGE_NAME+"/mimetype_binary",
    "drawable://"+Filer.PACKAGE_NAME+"/mimetype_document",
    "drawable://"+Filer.PACKAGE_NAME+"/mimetype_encrypted",
    "drawable://"+Filer.PACKAGE_NAME+"/mimetype_folder",
    "drawable://"+Filer.PACKAGE_NAME+"/mimetype_font_bitmap",
    "drawable://"+Filer.PACKAGE_NAME+"/mimetype_log",
    "drawable://"+Filer.PACKAGE_NAME+"/mimetype_html",
    "drawable://"+Filer.PACKAGE_NAME+"/mimetype_make",
    "drawable://"+Filer.PACKAGE_NAME+"/mimetype_man",
    "drawable://"+Filer.PACKAGE_NAME+"/mimetype_message",
    "drawable://"+Filer.PACKAGE_NAME+"/mimetype_midi",
    "drawable://"+Filer.PACKAGE_NAME+"/mimetype_mime_txt",
    "drawable://"+Filer.PACKAGE_NAME+"/mimetype_quicktime",
    "drawable://"+Filer.PACKAGE_NAME+"/mimetype_sound",
    "drawable://"+Filer.PACKAGE_NAME+"/mimetype_spreadsheet",
    "drawable://"+Filer.PACKAGE_NAME+"/mimetype_soffice",
    "drawable://"+Filer.PACKAGE_NAME+"/mimetype_tar",
    "drawable://"+Filer.PACKAGE_NAME+"/mimetype_txt",
    "drawable://"+Filer.PACKAGE_NAME+"/mimetype_vcalendar",
    "drawable://"+Filer.PACKAGE_NAME+"/mimetype_video",
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
