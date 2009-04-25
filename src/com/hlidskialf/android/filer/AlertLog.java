package com.hlidskialf.android.filer;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

public class AlertLog extends Handler {
  private AlertDialog mDialog = null;
  private Context mContext = null;
  private TextView mText = null;
  private DoneListener mDoneListener;
  private CancelListener mCancelListener;
  private ScrollView mScroll = null;
  private ProgressDialog mProgress = null;
  private boolean mCanceled = false;

  private static final int MSG_DISMISS=0;
  private static final int MSG_APPEND=1;
  private static final int MSG_WAITFORIT=2;
  private static final int MSG_PROGRESS_START=20;
  private static final int MSG_PROGRESS_UPDATE=21;
  private static final int MSG_PROGRESS_FINISH=22;

  public interface DoneListener {
    public void done();
  };
  public interface CancelListener {
    public void cancel();
  };

  public AlertLog(Context context, int title_res)
  {
    mContext = context;
    LayoutInflater li = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    View v = li.inflate(R.layout.alertlog,null);
    mDialog = new AlertDialog.Builder(context)
      .setTitle(title_res)
      .setView(v)
      .setCancelable(false)
      .setOnCancelListener(new DialogInterface.OnCancelListener() {
        public void onCancel(DialogInterface dia) { 
          mCanceled = true;
          AlertLog.this.dismiss(); 
        }
      })
      .show();
    mText = (TextView)mDialog.findViewById(android.R.id.text1);
    mScroll = (ScrollView)mDialog.findViewById(android.R.id.list);

    mProgress = new ProgressDialog(context);
    mProgress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
    mProgress.setOnCancelListener(new DialogInterface.OnCancelListener() {
      public void onCancel(DialogInterface dia) { 
        android.util.Log.v("progress","cancel");
        if (mCancelListener != null)
          mCancelListener.cancel();
        mCanceled = true;
      }
    });
  }
  public void setDoneListener(DoneListener listener) { mDoneListener = listener; }
  public void setCancelListener(CancelListener listener) { mCancelListener = listener; }
  public boolean isCancelled() { return mCanceled; }

  public void appendln(String text) { append(text+"\n"); }
  public void append(String text) { 
    if (mText == null) return;
    Message msg = Message.obtain();
    msg.obj = text;
    msg.what = MSG_APPEND;
    sendMessage(msg);
  }
  public void waitForIt() { 
    Message msg = Message.obtain();
    msg.what = MSG_WAITFORIT;
    sendMessage(msg);
  }
  public void dismiss() { 
    Message msg = Message.obtain();
    msg.what = MSG_DISMISS;
    sendMessage(msg);
  }

  public void progress_start(String title, String message, int max) { 
    Message msg = Message.obtain();
    msg.what = MSG_PROGRESS_START;
    Bundle b = new Bundle();
    b.putString("title", title);
    b.putString("message", message);
    b.putInt("max", max);
    msg.obj = b;
    sendMessage(msg);
  }
  public void progress_update(int progress) { 
    Message msg = Message.obtain();
    msg.what = MSG_PROGRESS_UPDATE;
    msg.obj = new Integer(progress);
    sendMessage(msg);
  }
  public void progress_finish() { 
    Message msg = Message.obtain();
    msg.what = MSG_PROGRESS_FINISH;
    sendMessage(msg);
  }


  @Override
  public void handleMessage(Message msg) {
    switch (msg.what) {
      case MSG_DISMISS:
        mDialog.dismiss(); 
        if (mDoneListener != null) mDoneListener.done();
        break;
      case MSG_APPEND:
        mText.append((String)msg.obj);
        mScroll.fullScroll(View.FOCUS_DOWN);
        break;
      case MSG_WAITFORIT: {
        mScroll.fullScroll(View.FOCUS_DOWN);
        Button ok = (Button)mDialog.findViewById(android.R.id.button1);
        ok.setVisibility(View.VISIBLE);
        ok.setOnClickListener(new View.OnClickListener() {
          public void onClick(View v) { dismiss(); }
        });
        mDialog.setCancelable(true);
        break;
      }
      case MSG_PROGRESS_START: {
        Bundle b = (Bundle)msg.obj;
        mProgress.setTitle( b.getString("title") );
        mProgress.setMessage( b.getString("message") );
        mProgress.setMax( b.getInt("max") );
        mProgress.show();
        break;
      }
      case MSG_PROGRESS_UPDATE:
        mProgress.setProgress((Integer)msg.obj);
        break;
      case MSG_PROGRESS_FINISH:
        mProgress.hide();
        break;
    }
  }
}
