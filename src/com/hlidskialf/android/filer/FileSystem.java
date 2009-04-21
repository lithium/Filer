package com.hlidskialf.android.filer;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.TextView;
import android.widget.Button;
import android.view.LayoutInflater;
import android.view.View;

import java.io.File;

class AlertLog {
  private AlertDialog mDialog;
  private Context mContext;
  private TextView mText;

  public AlertLog(Context context, int title_res)
  {
    LayoutInflater li = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    View v = li.inflate(R.layout.log,null);
    mContext = context;
    mDialog = new AlertDialog.Builder(context)
      .setTitle(title_res)
      .setView(v)
      .create();
    mText = (TextView)mDialog.findViewById(android.R.id.text1);
  }
  public void show() { mDialog.show(); }
  public void append(String text) { mText.append(text); }
  public void waitForIt() { 
    Button ok = (Button)mDialog.findViewById(android.R.id.button1);
    ok.setVisibility(View.VISIBLE);
    ok.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) { dismiss(); }
    });
  }
  public void dismiss() { mDialog.dismiss(); }
}

public class FileSystem
{


  public static boolean copy(String[] src, File dest, Context context)
  {
    if (!(dest.exists() && dest.isDirectory())) 
      return false;
    int i;
    for (i=0; i < src.length; i++) {
      File fsrc = new File(src[i]);
      if (!fsrc.exists()) continue;
      File fnew = new File(dest, fsrc.getName());
      if (fnew.exists()) continue; 

    }

  }

  public static void move(String[] src, File dest, Context context)
  {
    AlertLog log = null;
    if (context != null) {
      log = new AlertLog(context, R.string.moving_files);
      log.show();
    }
    if (!(dest.exists() && dest.isDirectory())) 
      return;
    int i;
    for (i=0; i < src.length; i++) {
      File fsrc = new File(src[i]);
      if (!fsrc.exists()) continue;
      File fnew = new File(dest, fsrc.getName());
      if (fnew.exists()) continue; 
      if (log != null)
        log.append(fsrc.getAbsolutePath() + " -> " + fnew.getAbsolutePath() + "\n");
      fsrc.renameTo(fnew);
    }
    log.waitForIt();
  }

  public static void delete(String[] src, boolean recursive, Context context)
  {
  }

}
