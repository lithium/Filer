package com.hlidskialf.android.filer;

import android.os.Handler;
import android.os.Message;
import java.lang.Thread;
import android.app.ProgressDialog;

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


public class FileSystem
{
  public static void copy(Context _context, AlertLog _log, String[] _src, File _dest)
  {
    final Context context = _context;
    final AlertLog log = _log; 
    final String[] src = _src;
    final File dest = _dest;

    if (!(dest.exists() && dest.isDirectory())) 
      return;

    class CopyThread extends Thread {
      public boolean running = true;
      public void run() {
        int i;
        for (i=0; running && (i < src.length); i++) {
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
            file_deepcopy(context, log, fsrc, fnew);
          } catch (java.io.IOException ex) {
          }
        }
        log.waitForIt();
      }
    };
    final CopyThread thread = new CopyThread();
    log.setCancelListener(new AlertLog.CancelListener() {
      public void cancel() {
        thread.running = false;
        thread.interrupt();
      }
    });
    thread.start();
  }
  private static void file_deepcopy(Context context, AlertLog log, File src, File dest) 
    throws java.io.IOException 
  {
    if (src.isDirectory()) {
      if (!dest.exists())
        dest.mkdirs();
      String ls[] = src.list();
      int i;
      for (i = 0; i < ls.length; i++) {
        file_deepcopy(context, log, new File(src, ls[i]), new File(dest, ls[i]));
      }
    }
    else {
      file_copy(context, log, src, dest);
    }
  }
  private static void file_copy(Context context, AlertLog log, File src, File dest) 
    throws java.io.IOException 
  {
    FileInputStream in = new FileInputStream(src);
    FileOutputStream out = new FileOutputStream(dest);
    byte[] buf = new byte[1024];
    int len,red=0;
    int i=0;

    String msg = context.getString(R.string.copying_file, src.getAbsolutePath(), dest.getAbsolutePath());
    log.appendln(msg);

    log.progress_start(context.getString(R.string.copy_here), msg, (int)src.length());

    while (!log.isCancelled() && ((len = in.read(buf)) > 0)) {
      red += len;
      out.write(buf, 0, len);
      if (++i % 5 == 0)
        log.progress_update(red);
    }
    in.close();
    out.close();
    log.progress_finish();
  }

  /*
  private static void file_copy(Context context, File src, File dest) throws java.io.IOException {
    final FileInputStream in = new FileInputStream(src);
    final FileOutputStream out = new FileOutputStream(dest);

    final ProgressDialog pd = new ProgressDialog(context);
    pd.setTitle(context.getString(R.string.copy_here));
    pd.setMessage(context.getString(R.string.copying_file, src.getAbsolutePath()));
    pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
    //final ProgressDialog pd = ProgressDialog.show(context, context.getString(R.string.copy_here), context.getString(R.string.copying_file, src.getAbsolutePath()));
    pd.setMax( (int)src.length() );
    pd.show();

    final Handler handler = new Handler() {
      @Override
      public void handleMessage(Message msg) {
        if (msg.what == 0) 
          pd.dismiss();
        else
          pd.incrementProgressBy(msg.what);
      }
    };

    Thread thread = new Thread(new Runnable() {
      public void run() {
        try  {
          byte[] buf = new byte[1024];
          int len;
          int i=0;
          while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
            if (++i % 5 == 0)
              handler.sendEmptyMessage(len*5);
          }
          in.close();
          out.close();
        } catch (java.io.IOException ex) {
        }
        handler.sendEmptyMessage(0);
      }
    });
    thread.start();

  }

  

  /* move */
  public static void move(Context _context, AlertLog _log, String[] _src, File _dest)
  {
    final Context context = _context;
    final AlertLog log = _log; 
    final String[] src = _src;
    final File dest = _dest;

    if (!(dest.exists() && dest.isDirectory())) 
      return;

    Thread thread = new Thread() {
      public void run() {
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
    };
    thread.start();
  }


  /* delete */
  public static void delete(Context _context, AlertLog _log, String[] _src, boolean _recursive)
  {
    final Context context = _context;
    final AlertLog log = _log; 
    final String[] src = _src;
    final boolean recursive = _recursive;

    Thread thread = new Thread() {
      public void run() {
        int i;
        for (i=0; i < src.length; i++) {
          File fsrc = new File(src[i]);
          if (!fsrc.exists()) {
            log.appendln(context.getString(R.string.file_not_found, fsrc.getAbsolutePath()));
            continue;
          }
          if (recursive) {
            file_deepdelete(context, log, fsrc);
          } else {
            if (fsrc.isDirectory() && fsrc.list().length > 0) 
              log.appendln(context.getString(R.string.directory_not_empty, fsrc.getAbsolutePath()));
            else
              file_delete(context, log, fsrc);
          }
        }
        log.waitForIt();
      }
    };
    thread.start();
  }
  private static void file_deepdelete(Context context, AlertLog log, File src)
  {
    if (src.isDirectory()) {
      String ls[] = src.list();
      int i;
      for (i = 0; i < ls.length; i++) {
        file_deepdelete(context, log, new File(src, ls[i]));
      }
    } 
    file_delete(context, log, src);
  }
  private static void file_delete(Context context, AlertLog log, File src)
  {
    src.delete();
    log.appendln(context.getString(R.string.file_deleted, src.getAbsolutePath()));
  }


}
