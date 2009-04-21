package com.hlidskialf.android.filer;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.TextView;
import android.widget.Button;
import android.view.LayoutInflater;
import android.view.View;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

class AlertLog {
  private AlertDialog mDialog = null;
  private Context mContext = null;
  private TextView mText = null;

  public AlertLog(Context context, int title_res)
  {
    mContext = context;
    if (mContext != null) {
      LayoutInflater li = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      View v = li.inflate(R.layout.alertlog,null);
      mDialog = new AlertDialog.Builder(context)
        .setTitle(title_res)
        .setView(v)
        .show();
      mText = (TextView)mDialog.findViewById(android.R.id.text1);
    }
  }
  public void append(String text) { if (mText != null) mText.append(text); }
  public void appendln(String text) { append(text+"\n"); }
  public void waitForIt() { 
    if (mDialog == null) dismiss();

    Button ok = (Button)mDialog.findViewById(android.R.id.button1);
    ok.setVisibility(View.VISIBLE);
    ok.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) { dismiss(); }
    });
  }
  public void dismiss() { if (mDialog != null) mDialog.dismiss(); }
}

public class FileSystem
{
  public static void file_deepcopy(File src, File dest) throws java.io.IOException {
    if (src.isDirectory()) {
      if (!dest.exists())
        dest.mkdirs();
      String ls[] = src.list();
      int i;
      for (i = 0; i < ls.length; i++) {
        file_deepcopy(new File(src, ls[i]), new File(dest, ls[i]));
      }
    }
    else {
      file_copy(src, dest);
    }
  }
  public static void file_copy(File src, File dest) throws java.io.IOException {
    FileInputStream in = new FileInputStream(src);
    FileOutputStream out = new FileOutputStream(dest);

    byte[] buf = new byte[1024];
    int len;
    while ((len = in.read(buf)) > 0) {
      out.write(buf, 0, len);
    }
    in.close();
    out.close();
  }

  public static void copy(String[] src, File dest, Context context)
  {
    AlertLog log = new AlertLog(context, R.string.copying_files);
    if (!(dest.exists() && dest.isDirectory())) 
      return;
    int i;
    for (i=0; i < src.length; i++) {
      File fsrc = new File(src[i]);
      if (!fsrc.exists()) {
        log.appendln(context.getString(R.string.file_not_found, fsrc.getAbsolutePath()));
        continue;
      }
      File fnew = new File(dest, fsrc.getName());
      if (fnew.exists()) {
        log.appendln(context.getString(R.string.file_exists, fnew.getAbsolutePath()));
        continue; 
      }
      try {
        log.appendln(fsrc.getAbsolutePath() + " -> " + fnew.getAbsolutePath());
        file_deepcopy(fsrc, fnew);
      } catch (java.io.IOException ex) {
      }
    }
    log.waitForIt();
  }

  public static void move(String[] src, File dest, Context context)
  {
    AlertLog log = new AlertLog(context, R.string.moving_files);
    if (!(dest.exists() && dest.isDirectory())) 
      return;
    int i;
    for (i=0; i < src.length; i++) {
      File fsrc = new File(src[i]);
      if (!fsrc.exists()) {
        log.appendln(context.getString(R.string.file_not_found, fsrc.getAbsolutePath()));
        continue;
      }
      File fnew = new File(dest, fsrc.getName());
      if (fnew.exists()) {
        log.appendln(context.getString(R.string.file_exists, fnew.getAbsolutePath()));
        continue; 
      }
      log.appendln(fsrc.getAbsolutePath() + " -> " + fnew.getAbsolutePath());
      fsrc.renameTo(fnew);
    }
    log.waitForIt();
  }

  public static void delete(String[] src, boolean recursive, Context context)
  {
  }

}
