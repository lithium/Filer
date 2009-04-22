package com.hlidskialf.android.filer;

import android.app.Dialog;
import android.widget.BaseAdapter;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.GridView;
import android.view.View;
import android.view.ViewGroup;
import android.content.Context;
import android.graphics.drawable.Drawable;

public class IconPicker extends Dialog
{
  private static final int[] RESOURCEIDS = {
    R.drawable.mimetype_ascii,
    R.drawable.mimetype_binary,
    R.drawable.mimetype_document,
    R.drawable.mimetype_encrypted,
    R.drawable.mimetype_folder,
    R.drawable.mimetype_font_bitmap,
    R.drawable.mimetype_log,
    R.drawable.mimetype_make,
    R.drawable.mimetype_man,
    R.drawable.mimetype_message,
    R.drawable.mimetype_midi,
    R.drawable.mimetype_mime_txt,
    R.drawable.mimetype_quicktime,
    R.drawable.mimetype_sound,
    R.drawable.mimetype_spreadsheet,
    R.drawable.mimetype_tar,
    R.drawable.mimetype_txt,
    R.drawable.mimetype_vcalendar,
    R.drawable.mimetype_video,


    
  };

  private Context mContext;
  private GridView mGrid;
  private OnIconPickedListener mPickedListener;

  static public interface OnIconPickedListener {
    abstract public void onIconPicked(Drawable icon);
  }

  public IconPicker(Context ctx, String title) {
    super(ctx); 
    mContext = ctx;
    mGrid = new GridView(ctx);
    mGrid.setNumColumns(4);
    setContentView(mGrid);
    setTitle(title);
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
        i.setImageResource(IconPicker.RESOURCEIDS[pos]);
        return i;
      }
      public final int getCount() { return IconPicker.RESOURCEIDS.length; }
      public final Object getItem(int pos) { return IconPicker.RESOURCEIDS[pos]; }
      public final long getItemId(int pos) { return pos; }
    });
    mGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      public void onItemClick(AdapterView parent, View view, int pos, long id) {
        if (mPickedListener != null) {
          ImageView iv = (ImageView)view;
          mPickedListener.onIconPicked(iv.getDrawable());
        }
        dismiss();
      }
    });
  }
  public void setOnIconPickedListener(IconPicker.OnIconPickedListener listener) { mPickedListener = listener; }
}
