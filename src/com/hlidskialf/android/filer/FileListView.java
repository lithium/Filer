
public class FileListView extends ListView
{
  File mRootFile,mCurFile;


  private BroadcastReceiver mReceiver = new BroadcastReceiver() {
    public void onReceive(Context context, Intent intent) { refresh(); }
  }; 

  public FileListView(String root_path, String start_path, boolean browse_root, boolean hide_dot)
  {
    if(root_path == null) root_path = Environment.getExternalStorageDirectory().toString();
    mRootFile = new File(root_path);
    mCurFile = new File(start_path == null ? root_path : start_path);
    mBrowseRoot = browse_root;
    mHideDot = hide_dot;

    registerReceiver(mReceiver, new IntentFilter()
        .addDataScheme("file")
        .addAction(Intent.ACTION_MEDIA_UNMOUNTED)
        .addAction(Intent.ACTION_MEDIA_MOUNTED));
  }

  protected void finalize() 
  {
    unregisterReceiver(mReceiver);
    super.finalize();
  }

  public void refresh()
  {

    new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) { fillData(mCurDir); }
    };


  }

}
