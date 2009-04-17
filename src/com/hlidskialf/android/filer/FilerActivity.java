package com.hlidskialf.android.filer;


import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import android.widget.ListView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;

public class FilerActivity extends ListActivity
{
  private File mRootFile,mCurDir,mStartFile;
  private boolean mBrowseRoot,mHideDot,mCreatingShortcut;
  private String mRootPath,mHomePath;
  private ArrayList<String> mCurFiles;
  private FileListAdapter mFileAdapter;
  private SharedPreferences mPrefs;
  private ArrayList<String> mYanked;
  private boolean mIgnoreNextClick = false; // hack for long click ..

  private IntentFilter mMountFilter;
  private BroadcastReceiver mMountReceiver = new BroadcastReceiver() {
    public void onReceive(Context context, Intent intent) {
      String state = Environment.getExternalStorageState();
      if (Environment.MEDIA_MOUNTED.equals(state)) {
        fillData(mCurDir);
      }
      else {
        emptyData(R.string.no_storage);
      }
    }
  };
  private Comparator mFileComparator = new Comparator() {
    public int compare(Object a, Object b) {
      File fa = new File(mCurDir, (String)a);
      File fb = new File(mCurDir, (String)b);
      if (fa == null || fb == null) return 0;
      if (fa.isDirectory()) {
        if (fb.isDirectory()) return fa.getName().compareTo( fb.getName() );
        return -1;
      }
      if (fb.isDirectory()) return 1;
      return 0;
    }
  };
  private class FileListAdapter extends ArrayAdapter {
    private Context mContext ;
    public FileListAdapter(Context context) 
    {
      super(context, R.layout.file_list_item, mCurFiles); 
      mContext = context;
    }
    public View getView(int pos, View v, ViewGroup parent) 
    {
      if (v == null) {
        LayoutInflater li = getLayoutInflater();
        v = li.inflate(R.layout.file_list_item, parent, false); 
      }
      String filename = mCurFiles.get(pos);
      File f = new File(mCurDir, filename);

      TextView name = (TextView)v.findViewById(R.id.row_name);
      TextView size = (TextView)v.findViewById(R.id.row_size);
      TextView mtime = (TextView)v.findViewById(R.id.row_mtime);
      ImageView icon = (ImageView)v.findViewById(R.id.row_icon);
      ImageView mime = (ImageView)v.findViewById(R.id.row_mimetype);

      if (name != null) name.setText(filename);

      if (f.isDirectory()) { // directory
        if (icon != null) icon.setImageResource(filename.equals("..") ? android.R.drawable.ic_menu_revert : android.R.drawable.ic_menu_more);
        if (mime != null) mime.setImageResource(R.drawable.mimetype_folder);
        if (size != null) size.setText( "" );
        if (mtime != null) mtime.setText( "" );
      }
      else { // regular file
        if (icon != null) icon.setImageResource(0);
        if (mime != null) mime.setImageResource(R.drawable.mimetype_ascii);
        if (size != null) size.setText( Filer.format_size(f.length()) );
        if (mtime != null) mtime.setText( Filer.format_date(f.lastModified()) );
      }

      return v;
    }
  };

  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    setContentView(R.layout.filer);

    mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
    mBrowseRoot = mPrefs.getBoolean(Filer.PREF_BROWSE_ROOT, true);
    mRootPath = mBrowseRoot ? "/" : Environment.getExternalStorageDirectory().toString();
    mHideDot = mPrefs.getBoolean(Filer.PREF_HIDE_DOT, true);
    mHomePath = mPrefs.getString(Filer.PREF_HOME_PATH, Environment.getExternalStorageDirectory().toString());

    Intent trigger = getIntent();
    Uri uri = trigger.getData();
    mStartFile = new File(uri != null ? uri.getPath() : mHomePath);
    if (!mStartFile.isDirectory()) 
      mStartFile = mStartFile.getParentFile();
    mCurDir = mStartFile;

    mMountFilter = new IntentFilter();
    mMountFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED); 
    mMountFilter.addAction(Intent.ACTION_MEDIA_MOUNTED); 
    mMountFilter.addDataScheme("file");

    mYanked = new ArrayList<String>();
    mCurFiles = new ArrayList<String>();
    mFileAdapter = new FileListAdapter(this);
    setListAdapter(mFileAdapter);

    if (mCreatingShortcut = Intent.ACTION_CREATE_SHORTCUT.equals(trigger.getAction())) {
      Toast t = Toast.makeText(this, R.string.toast_shortcut_hint, Toast.LENGTH_LONG);
      t.show();
    }
    registerForContextMenu(getListView());
  }
  @Override
  public void onResume()
  {
    super.onResume();
    registerReceiver(mMountReceiver, mMountFilter);
    fillData(mCurDir);
  }
  @Override
  public void onPause()
  {
    unregisterReceiver(mMountReceiver);
    super.onPause();
  }
  @Override
  public void onSaveInstanceState(Bundle icicle)
  {
    super.onSaveInstanceState(icicle);

    icicle.putStringArrayList("mYanked",mYanked);
    icicle.putString("mCurDir", mCurDir.getPath());
  }
  @Override 
  public void onRestoreInstanceState(Bundle icicle)
  {
    super.onRestoreInstanceState(icicle);
    mYanked = icicle.getStringArrayList("mYanked");
    String cur_dir = icicle.getString("mCurDir");
    if (cur_dir != null) fillData(new File(cur_dir));
  }

  @Override
  public void onListItemClick(ListView lv, View v, int pos, long itemid)
  {
    if (mIgnoreNextClick) {
      mIgnoreNextClick = false;
      return;
    }

    super.onListItemClick(lv,v,pos,itemid);
    if (pos == 0) { // cd ..
      fillData(mCurDir.getParentFile());
      return;
    }
    String filename = mCurFiles.get(pos);
    File f = new File(mCurDir, filename);
    if (f.isDirectory()) {
      fillData(f);
      return;
    }

    if (mCreatingShortcut) {
      //create_shortcut(f);
      return;
    }

    //TODO
    //start intent based on mimetype
  }
  @Override 
  public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo)
  {
    AdapterContextMenuInfo info = (AdapterContextMenuInfo)menuInfo;


    if (info.position == 0) { // long click on .. goes to home
      fillData(mStartFile);
      mIgnoreNextClick = true;
      return;
    }

    if (mCreatingShortcut) {
      //TODO
      //create_shortcut();
      return;
    }

    super.onCreateContextMenu(menu,v,menuInfo);
    getMenuInflater().inflate(R.menu.files_context, menu);

    MenuItem yank = menu.findItem(R.id.context_menu_yank);
    MenuItem unyank = menu.findItem(R.id.context_menu_unyank);
    
    if (yank != null && unyank != null) {
      File f = new File(mCurDir, mCurFiles.get(info.position));
      boolean vis = mYanked.contains(f.getPath());
      unyank.setVisible(vis);
      yank.setVisible(!vis);
    }
  }
  @Override 
  public boolean onContextItemSelected(MenuItem item)
  {
    AdapterContextMenuInfo info = (AdapterContextMenuInfo)item.getMenuInfo();

    switch (item.getItemId()) {
      case R.id.context_menu_yank:
        return true;
      case R.id.context_menu_unyank:
        return true;
      case R.id.context_menu_open:
        return true;
      case R.id.context_menu_rename:
        return true;
      case R.id.context_menu_delete:
        return true;
    }
    return true;
  }
  @Override
  public boolean onCreateOptionsMenu(Menu menu)
  {
    super.onCreateOptionsMenu(menu);
    getMenuInflater().inflate(R.menu.options, menu);

    return true;
  }
  @Override
  public boolean onPrepareOptionsMenu(Menu menu)
  {
    MenuItem unyank_all = menu.findItem(R.id.options_menu_unyank_all);
    MenuItem copy = menu.findItem(R.id.options_menu_copy);
    MenuItem move = menu.findItem(R.id.options_menu_move);

    boolean vis = (mYanked != null) && (mYanked.size() > 0);
    unyank_all.setVisible(vis);
    copy.setVisible(vis);
    move.setVisible(vis);
    return true;
  }
  @Override
  public boolean onOptionsItemSelected(MenuItem item)
  {
    switch (item.getItemId()) {
      case R.id.options_menu_unyank_all:
        return true;
      case R.id.options_menu_copy:
        return true;
      case R.id.options_menu_move:
        return true;
      case R.id.options_menu_mkdir:
        return true;
      case R.id.options_menu_prefs:
        return true;
      case R.id.options_menu_help:
        return true;
    }
    return false;
  }



  @Override
  public boolean onKeyDown(int code, KeyEvent event) {
    if (code == KeyEvent.KEYCODE_BACK) {
      fillData(mCurDir.getParentFile());
      return true;
    }
    return super.onKeyDown(code, event);
  }




  public void fillData(File new_dir)
  {
    try {
      mCurDir = new File(new_dir.getCanonicalPath());
      setTitle(mCurDir.getPath());
    } catch (Exception e) {
      return;
    }

    mCurFiles.clear();
    String[] ls = mCurDir.list();
    if (ls != null) {
      int i;
      for (i=0; i < ls.length; i++) {
        if (mHideDot && ls[i].startsWith(".")) continue;
        mCurFiles.add(ls[i]);
      }
    }
    Collections.sort(mCurFiles, mFileComparator);

    if (!mCurDir.getPath().equals(mRootPath)) {
      mCurFiles.add(0,"..");
    }

    TextView tv = (TextView)findViewById(R.id.empty_text);
    if (tv != null) tv.setVisibility(View.GONE);
    
    getListView().invalidateViews();
  }
  public void emptyData(int reason_res_id)
  {
    mCurFiles.clear();
    TextView tv = (TextView)findViewById(R.id.empty_text);
    if (tv != null) {
      tv.setVisibility(View.VISIBLE);
      tv.setText(reason_res_id);
    }
  }
}
