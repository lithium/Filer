package com.hlidskialf.android.filer;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Spinner;
import android.widget.Toast;
import android.webkit.WebView;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Stack;



public class FilerActivity extends ListActivity
{
  static private final int REQUEST_PREFERENCES=1;
  static private final int REQUEST_FILE_INTENT=2;

  private File mRootFile,mCurDir,mStartFile;
  private boolean mBrowseRoot,mHideDot,mRecursiveDelete,mCreatingShortcut;
  private String mRootPath,mHomePath;
  private ArrayList<String> mCurFiles;
  private FileListAdapter mFileAdapter;
  private SharedPreferences mPrefs;
  private ArrayList<String> mYanked;
  private boolean mIgnoreNextClick = false; // hack for long click ..
  private Stack<String> mPathHistory;
  private LayoutInflater mFactory;
  private Dialog mDialog;

  private List<String> mActionStrings;

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
        v = mFactory.inflate(R.layout.file_list_item, parent, false); 
      }
      if (pos >= mCurFiles.size()) return v; // Only monkey seems to trigger this


      final String filename = mCurFiles.get(pos);
      File f = new File(mCurDir, filename);

      if (mYanked.contains(f.getAbsolutePath())) 
        v.setBackgroundResource(R.drawable.yanked);
      else 
        v.setBackgroundResource(R.drawable.unyanked);

      TextView name = (TextView)v.findViewById(R.id.row_name);
      TextView size = (TextView)v.findViewById(R.id.row_size);
      TextView mtime = (TextView)v.findViewById(R.id.row_mtime);
      ImageView icon = (ImageView)v.findViewById(R.id.row_icon);
      ImageView mime = (ImageView)v.findViewById(R.id.row_mimetype);

      final View row = v;
      View.OnClickListener yank_listener = new View.OnClickListener() {
        public void onClick(View mimev) {
          if (is_file_yanked(filename)) {
            unyank_file(filename);
            row.setBackgroundResource(R.drawable.unyanked);
          } else {
            yank_file(filename);
            row.setBackgroundResource(R.drawable.yanked);
          }
          update_yankbar_visibility();
        }
      };
      mime.setOnClickListener(yank_listener);
      icon.setOnClickListener(yank_listener);

      if (name != null) name.setText(filename);

      if (f.isDirectory() || filename.equals("..")) { // directory
        if (icon != null) icon.setImageResource(filename.equals("..") ? android.R.drawable.ic_menu_revert : android.R.drawable.ic_menu_more);
        if (mime != null) mime.setImageResource(R.drawable.mimetype_folder);
        if (size != null) size.setText( "" );
        if (mtime != null) mtime.setText( "" );
      }
      else { // regular file

        if (icon != null) icon.setImageResource(0);
        if (mime != null) {
          String ext = Filer.getExtension(filename);
          String icon_url = Filer.getIconFromExtension(FilerActivity.this, ext);
          if (ext == null || icon_url == null || !Filer.setImageFromUri(mime, Uri.parse(icon_url)))
            mime.setImageResource(R.drawable.mimetype_ascii);
        }
        if (size != null) size.setText( Filer.format_size(f.length()) );
        if (mtime != null) mtime.setText( Filer.format_date(f.lastModified()) );
      }

      return v;
    }
  };

  private void load_preferences()
  {
    if (mPrefs == null) mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

    mBrowseRoot = mPrefs.getBoolean(Filer.PREF_BROWSE_ROOT, false);
    mRootPath = mBrowseRoot ? "/" : Environment.getExternalStorageDirectory().toString();
    mHideDot = mPrefs.getBoolean(Filer.PREF_HIDE_DOT, true);
    mHomePath = mPrefs.getString(Filer.PREF_HOME_PATH, "");
    
    if (mHomePath == null || mHomePath.length() < 1) 
      mHomePath = Environment.getExternalStorageDirectory().toString();

    mRecursiveDelete = mPrefs.getBoolean(Filer.PREF_RECURSIVE_DELETE, true);
  }
  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    setContentView(R.layout.filer);

    mFactory = LayoutInflater.from(this);

    Intent trigger = getIntent();

    load_preferences();

    /* determine starting directory */
    Uri uri = trigger.getData();
    mStartFile = new File(uri != null ? uri.getPath() : mHomePath);
    if (!mStartFile.isDirectory()) 
      mStartFile = mStartFile.getParentFile();
    mCurDir = mStartFile;

    mMountFilter = new IntentFilter();
    mMountFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED); 
    mMountFilter.addAction(Intent.ACTION_MEDIA_MOUNTED); 
    mMountFilter.addDataScheme("file");

    
    mPathHistory = new Stack<String>();
    mYanked = new ArrayList<String>();
    mCurFiles = new ArrayList<String>();
    mFileAdapter = new FileListAdapter(this);
    setListAdapter(mFileAdapter);


    if (mCreatingShortcut = Intent.ACTION_CREATE_SHORTCUT.equals(trigger.getAction())) {
      Toast t = Toast.makeText(this, R.string.toast_shortcut_hint, Toast.LENGTH_LONG);
      t.show();
    }


    mActionStrings = Arrays.asList(getResources().getStringArray(R.array.actions));

    init_yankbar();


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
    if (mCurDir != null)
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
  protected void onActivityResult(int requestCode, int resultCode, Intent data) 
  {
    if (requestCode == REQUEST_PREFERENCES) {
      load_preferences();
      fillData(mCurDir);
    }
  }
  @Override
  public boolean onKeyDown(int code, KeyEvent event) {
    if (code == KeyEvent.KEYCODE_BACK) {
      if (mPathHistory.size() > 1) {
        mPathHistory.pop();
        fillData(new File( mPathHistory.peek() ));
        return true;
      }
    }
    return super.onKeyDown(code, event);
  }




  @Override
  public void onListItemClick(ListView lv, View v, int pos, long itemid)
  {
    if (mIgnoreNextClick) {
      mIgnoreNextClick = false;
      fillData(mStartFile);
      return;
    }
    String filename = mCurFiles.get(pos);

    super.onListItemClick(lv,v,pos,itemid);
    if (filename.equals("..")) { // cd ..
      fillData(mCurDir.getParentFile());
      return;
    }
    File f = new File(mCurDir, filename);
    if (f.isDirectory()) {
      fillData(f);
      return;
    }

    if (mCreatingShortcut) {
      create_shortcut(f);
      return;
    }

    open_file(Filer.getIntentFromFile(this, f));
  }
  @Override 
  public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo)
  {
    AdapterContextMenuInfo info = (AdapterContextMenuInfo)menuInfo;
    String filename = mCurFiles.get(info.position);

    if (filename.equals("..")) { // long click on .. goes to home
      mIgnoreNextClick = true;
      return;
    }

    if (mCreatingShortcut) {
      create_shortcut(new File(mCurDir, filename));
      return;
    }

    super.onCreateContextMenu(menu,v,menuInfo);
    getMenuInflater().inflate(R.menu.files_context, menu);

    MenuItem yank = menu.findItem(R.id.context_menu_yank);
    MenuItem unyank = menu.findItem(R.id.context_menu_unyank);
    
    if (yank != null && unyank != null) {
      File f = new File(mCurDir, filename);
      boolean vis = mYanked.contains(f.getPath());
      unyank.setVisible(vis);
      yank.setVisible(!vis);
    }
  }
  @Override 
  public boolean onContextItemSelected(MenuItem item)
  {
    AdapterContextMenuInfo info = (AdapterContextMenuInfo)item.getMenuInfo();
    final String filename = mCurFiles.get(info.position);
    final File f = new File(mCurDir, filename); 

    switch (item.getItemId()) {
      case R.id.context_menu_open:
        open_as(f);
        return true;
      case R.id.context_menu_yank:
        yank_file(filename);
        info.targetView.setBackgroundResource(R.drawable.yanked);
        return true;
      case R.id.context_menu_unyank:
        unyank_file(filename);
        info.targetView.setBackgroundResource(R.drawable.unyanked);
        return true;
      case R.id.context_menu_rename:
        AlertInput ai = new AlertInput(this, R.string.dialog_rename_title, getString(R.string.dialog_rename_splash, filename), filename);
        ai.setOnCompleteListener(new AlertInput.OnCompleteListener() {
          public void onCancel() {}
          public void onComplete(String value) {
            File fnew = new File(mCurDir, value);
            String msg;
            if (!f.exists()) {
              msg = getString(R.string.file_not_found, filename);
            }
            else if (fnew.exists()) {
              msg = getString(R.string.file_exists, value);
            }
            else {
              msg = getString(R.string.file_renamed, filename, value);
              Filer.MediaProviderBatch batch = new Filer.MediaProviderBatch(FilerActivity.this);
              batch.remove(f);
              f.renameTo(fnew);
              batch.add(fnew);
              batch.commit();
            }
            fillData(mCurDir);

            Toast t = Toast.makeText(FilerActivity.this, msg, Toast.LENGTH_LONG);
            t.show();
          }
        });
        return true;
      case R.id.context_menu_delete:
        final String path = f.getAbsolutePath();
        build_yank_buffer_dialog(R.string.dialog_delete_buffer_title, path) 
          .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) { 
              final DialogInterface dia = dialog;
              AlertLog log = new AlertLog(FilerActivity.this, R.string.deleting_files);
              log.setDoneListener(new AlertLog.DoneListener() {
                public void done() {
                  fillData(mCurDir);
                }
              });
              
              FileSystem.delete(FilerActivity.this, log, new String[] {path}, mRecursiveDelete);
            }
          })
          .show();
        return true;
      case R.id.context_menu_info: {
        View layout = mFactory.inflate(R.layout.file_info, null);
        TextView tv;
        tv = (TextView)layout.findViewById(R.id.file_info_path);
        tv.setText(f.getAbsolutePath());

        tv = (TextView)layout.findViewById(R.id.file_info_type);
        tv.setText(Filer.getMimeFromFile(this, f));

        tv = (TextView)layout.findViewById(R.id.file_info_mtime);
        tv.setText(Filer.format_date(f.lastModified()));

        tv = (TextView)layout.findViewById(R.id.file_info_size);
        tv.setText(Filer.format_size(Filer.disk_usage(f)));

        new AlertDialog.Builder(this)
          .setTitle(getString(R.string.file_info_title, f.getName()))
          .setView( layout )
          .setPositiveButton(android.R.string.ok, null)
          .show();
        return true;
      }
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
    boolean vis = (mYanked != null) && (mYanked.size() > 0);
    unyank_all.setVisible(vis);
    /*
    MenuItem copy = menu.findItem(R.id.options_menu_copy);
    MenuItem move = menu.findItem(R.id.options_menu_move);

    copy.setVisible(vis);
    move.setVisible(vis);
    */
    return true;
  }
  @Override
  public boolean onOptionsItemSelected(MenuItem item)
  {
    switch (item.getItemId()) {
        /*
      case R.id.options_menu_copy:
        return true;
      case R.id.options_menu_move:
        return true;
        */
      case R.id.options_menu_yank_all:
        yank_all();
        return true;
      case R.id.options_menu_unyank_all:
        unyank_all();
        return true;
      case R.id.options_menu_mkdir:
        AlertInput ai = new AlertInput(this, R.string.dialog_mkdir_title, getString(R.string.dialog_mkdir_splash), getString(R.string.dialog_mkdir_hint));
        ai.setOnCompleteListener(new AlertInput.OnCompleteListener() {
          public void onComplete(String value) {
            File f = new File(mCurDir, value);
            String msg;
            if (f.exists()) {
              msg = getString(R.string.file_exists, value);
            }
            else {
              f.mkdir();
              msg = getString(R.string.directory_created, value);
            }
            fillData(mCurDir);

            Toast t = Toast.makeText(FilerActivity.this, msg, Toast.LENGTH_LONG);
            t.show();
          }
          public void onCancel() {} 
        });
        return true;
      case R.id.options_menu_prefs:
        startActivityForResult( new Intent(this, FilerPreferencesActivity.class), REQUEST_PREFERENCES );
        return true;
      case R.id.options_menu_help:
        show_help();
        return true;
    }
    return false;
  }










  public void fillData(File new_dir)
  {
    try {
      mCurDir = new File(new_dir.getCanonicalPath());
      setTitle(mCurDir.getPath());
    } catch (Exception e) {
      return;
    }

    if (mPathHistory.size() < 1 || !mCurDir.getPath().equals( mPathHistory.peek() ))
      mPathHistory.push(mCurDir.getPath());

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




  private void yank_file(String filename) {
    File f = new File(mCurDir, filename);
    String path = f.getAbsolutePath();
    if (!mYanked.contains(path)) {
      mYanked.add(path);
    }
    update_yankbar_visibility();
  }
  private void unyank_file(String filename) {
    File f = new File(mCurDir, filename);
    String path = f.getAbsolutePath();
    if (mYanked.contains(path)) {
      mYanked.remove(path);
    }
    update_yankbar_visibility();
  }
  private boolean is_file_yanked(String filename) {
    File f = new File(mCurDir, filename);
    return mYanked.contains(f.getAbsolutePath());
  }
  private void unyank_all()
  {
    mYanked.clear();
    fillData(mCurDir);
    update_yankbar_visibility();
  }
  private void yank_all()
  {
    String[] ls = mCurDir.list();
    int i;
    for (i=0; i < ls.length; i++) 
      yank_file(ls[i]);
    fillData(mCurDir);
    update_yankbar_visibility();
  }
  private void update_yankbar_visibility()
  {
    View yank_bar = findViewById(R.id.yank_bar);
    if (mYanked == null) return;
    boolean vis = (mYanked.size() > 0);
    if (yank_bar != null) yank_bar.setVisibility(vis ? View.VISIBLE : View.GONE);
  }

  private ArrayList<File> yank_buffer_contents()
  {
    if (mYanked == null) return null;
    ArrayList<File> ret = new ArrayList<File>();
    int len = mYanked.size();
    int i;
    for (i=0; i < len; i++) {
      String path = mYanked.get(i);
      File f = new File(path);
      if (f == null || ! f.exists()) continue;
      ret.add(f);
      if (f.isDirectory()) {
        yank_buffer_contents_append_directory(ret, f);
      }
    }
    return ret;
  }
  private void yank_buffer_contents_append_directory(ArrayList<File> ret, File dir)
  {
    String[] files = dir.list();
    int i;
    for (i=0; i < files.length; i++) {
      File f = new File(dir, files[i]);
      ret.add(f);
      if (f.isDirectory())
        yank_buffer_contents_append_directory(ret, f);
    }
  }
  private AlertDialog.Builder build_yank_buffer_dialog(int title_res, String path)
  {
    ArrayList<File> files;
    ListView lv = new ListView(this);
    lv.setCacheColorHint(0);
    if (path != null) {
      files = new ArrayList<File>(1);
      files.add(new File(path));
    }
    else {
      files = yank_buffer_contents();
    }
    lv.setAdapter(new ArrayAdapter(this, R.layout.dialog_list_item, files));


    AlertDialog.Builder builder = new AlertDialog.Builder(this)
      .setTitle(title_res)
      .setView(lv)
      .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) { dialog.dismiss(); }
      })
      ;
    return builder;
  }

  private void init_yankbar() 
  {
    View buffer = findViewById(R.id.yank_bar_buffer);
    buffer.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) { 
        build_yank_buffer_dialog(R.string.dialog_yank_buffer_title, null)
          .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) { dialog.dismiss(); }
          })
          .setNeutralButton(R.string.unyank_all, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) { 
              dialog.dismiss();
              unyank_all(); 
            }
          })
          .show();
      }
    });
    View copy = findViewById(R.id.yank_bar_copy);
    copy.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) { 
        build_yank_buffer_dialog(R.string.dialog_copy_buffer_title, null) 
          .setPositiveButton(R.string.copy_here, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) { 
              final DialogInterface dia = dialog;
              AlertLog log = new AlertLog(FilerActivity.this, R.string.copying_files);
              log.setDoneListener(new AlertLog.DoneListener() {
                public void done() {
                  dia.dismiss();
                  unyank_all();
                }
              });
              FileSystem.copy(FilerActivity.this, log, mYanked.toArray(new String[0]), mCurDir);
            }
          })
          .show();
      }
    });
    View move = findViewById(R.id.yank_bar_move);
    move.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) { 
        build_yank_buffer_dialog(R.string.dialog_move_buffer_title, null) 
          .setPositiveButton(R.string.move_here, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) { 
              final DialogInterface dia = dialog;
              AlertLog log = new AlertLog(FilerActivity.this, R.string.moving_files);
              log.setDoneListener(new AlertLog.DoneListener() {
                public void done() {
                  dia.dismiss();
                  unyank_all();
                }
              });
              FileSystem.move(FilerActivity.this, log, mYanked.toArray(new String[0]), mCurDir);
            }
          })
          .show();
      }
    });
    View rm = findViewById(R.id.yank_bar_delete);
    rm.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) { 
        build_yank_buffer_dialog(R.string.dialog_delete_buffer_title, null) 
          .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) { 
              final DialogInterface dia = dialog;
              AlertLog log = new AlertLog(FilerActivity.this, R.string.deleting_files);
              log.setDoneListener(new AlertLog.DoneListener() {
                public void done() {
                  dia.dismiss();
                  unyank_all();
                }
              });
              FileSystem.delete(FilerActivity.this, log, mYanked.toArray(new String[0]), mRecursiveDelete);
            }
          })
          .show();
      }
    });
  }

  public void create_shortcut(File f)
  {
    final Intent short_intent = Filer.shortcutIntent(this, f);
    AlertInput ai = new AlertInput(this, R.string.create_shortcut_title, getString(R.string.create_shortcut_splash), f.getName());
    ai.setOnCompleteListener(new AlertInput.OnCompleteListener() {
      public void onCancel() {}
      public void onComplete(String value) {
        if (value.length() > 0)
          short_intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, value);
        FilerActivity.this.setResult(RESULT_OK, short_intent);
        FilerActivity.this.finish();
      }
    });
  }

  private void open_as(File f) 
  {
    final Intent intent = Filer.getIntentFromFile(this, f);
    
    View layout = mFactory.inflate(R.layout.open_as, null);
    TextView tv;
    tv = (TextView)layout.findViewById(R.id.file_info_path);
    if (tv != null) tv.setText(f.getAbsolutePath());

    final EditText mimetype = (EditText)layout.findViewById(R.id.edit_mime_mimetype);
    mimetype.setText(intent.getType());
    mimetype.setHint(intent.getType());


    final Spinner spinner = (Spinner)layout.findViewById(R.id.edit_mime_action);
    ArrayAdapter spinadapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, mActionStrings);
    spinadapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    spinner.setAdapter(spinadapter);
    int spinidx = mActionStrings.indexOf(intent.getAction().replaceFirst("android.intent.action.","ACTION_"));
    spinner.setSelection(spinidx, false);

    mDialog = new AlertDialog.Builder(this)
      .setTitle(R.string.open_as)
      .setView(layout)
      .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dia, int which) { 
          String action = (String)spinner.getSelectedItem();
          String type = mimetype.getText().toString();
          intent.setDataAndType(intent.getData(), type);
          intent.setAction(action);
          open_file(intent);
          dia.dismiss();
        }
      })
      .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dia, int which) { dia.dismiss(); }
      })
      .show();
  }

  private void open_file(Intent intent)
  {
    if (intent == null) return;

    try {
      startActivityForResult(intent,REQUEST_FILE_INTENT);
    } catch (android.content.ActivityNotFoundException ex) {
      Toast t = Toast.makeText(FilerActivity.this, R.string.activity_not_found, Toast.LENGTH_SHORT);
      t.show();
    }
  }

  private void show_help()
  {
    View layout = mFactory.inflate(R.layout.help,null);
    WebView webv = (WebView)layout.findViewById(R.id.webhelp);
    String data = getString(R.string.help_body);
    webv.loadDataWithBaseURL( "filer://help/home", data, "text/html", "UTF-8" , "filer://help/fail");

    webv.setScrollBarStyle(WebView.SCROLLBARS_INSIDE_INSET);
    new AlertDialog.Builder(this)
      .setTitle(R.string.help_title)
      .setView( layout )
      .setPositiveButton(android.R.string.ok, null)
      .show();
  }
}
