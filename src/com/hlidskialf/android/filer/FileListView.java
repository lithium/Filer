package com.hlidskialf.android.filer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class FileListView extends ListView
{
  private Context mContext;
  private File mRootFile,mCurFile;
  private boolean mBrowseRoot,mHideDot;
  private ArrayList<String> mCurFiles;

  private BroadcastReceiver mReceiver = new BroadcastReceiver() {
    public void onReceive(Context context, Intent intent) { refresh(); }
  }; 
  private ArrayAdapter<String> mAdapter;

  private class FileListAdapter extends ArrayAdapter {
    public FileListAdapter(Context context) 
    {
      super(context, android.R.layout.simple_list_item_2, mCurFiles);
    }
    public View getView(int pos, View v, ViewGroup parent) 
    {
      TextView tv;
      if (v == null) {
        v = new TextView(mContext);
      }
      tv = (TextView)v;
      tv.setText(mCurFiles.get(pos));

      return v;
    }
  };

  public FileListView(Context context) { super(context); init(context,null,null,false,true); }
  public FileListView(Context context, AttributeSet attrs) { super(context,attrs); init(context,null,null,false,true); }
  public FileListView(Context context, String root_path, String start_path, boolean browse_root, boolean hide_dot)
  {
    super(context);
    init(context,root_path,start_path,browse_root,hide_dot);
  }
  private Comparator mComparator = new Comparator() {
    public int compare(Object a, Object b) {
      File fa = new File(mCurFile, (String)a);
      File fb = new File(mCurFile, (String)b);
      if (fa == null || fb == null) return 0;
      if (fa.isDirectory()) {
        if (fb.isDirectory()) return fa.getName().compareTo( fb.getName() );
        return -1;
      }
      if (fb.isDirectory()) return 1;
      return 0;
    }
  };

  private void init(Context context, String root_path, String start_path, boolean browse_root, boolean hide_dot)
  {
    mContext = context;
    if(root_path == null) root_path = Environment.getExternalStorageDirectory().toString();
    mRootFile = new File(root_path);
    mCurFile = new File(start_path == null ? root_path : start_path);
    mBrowseRoot = browse_root;
    mHideDot = hide_dot;

    IntentFilter filt = new IntentFilter();
    filt.addDataScheme("file");
    filt.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
    filt.addAction(Intent.ACTION_MEDIA_MOUNTED);
    mContext.registerReceiver(mReceiver, filt);

    mCurFiles = new ArrayList<String>();
    mAdapter = new FileListAdapter(mContext);
    setAdapter(mAdapter);

    refresh();
  }

  protected void finalize() throws Throwable
  {
    mContext.unregisterReceiver(mReceiver);
    super.finalize();
  }

  public void refresh()
  {
    String state = Environment.getExternalStorageState();

    if (Environment.MEDIA_MOUNTED.equals(state)) {
      refreshFiles();
    }
    else {
      // show unmounted string
      mCurFiles.clear();
    }
  }

  private void refreshFiles()
  {
    String ls[] = mCurFile.list();
    if (ls == null) return;
    int i;
    for (i=0; i < ls.length; i++) {
      if (mHideDot && ls[i].startsWith(".")) continue;
      mCurFiles.add(ls[i]);
    }
    Collections.sort(mCurFiles, mComparator);
    if (!mCurFile.getPath().equals("/") || mBrowseRoot || !mCurFile.getPath().equals(mRootFile.getPath())) {
      mCurFiles.add(0,"..");
    }
  }

}
