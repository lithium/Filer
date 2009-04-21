package com.hlidskialf.android.filer;

import android.os.Handler;
import android.os.Message;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.widget.TextView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;

public class AlertLog extends Handler {
  private AlertDialog mDialog = null;
  private Context mContext = null;
  private TextView mText = null;
  private DoneListener mDoneListener;
  private ScrollView mScroll = null;

  private static final int MSG_DISMISS=0;
  private static final int MSG_APPEND=1;
  private static final int MSG_WAITFORIT=2;

  public interface DoneListener {
    public void done();
  };

  public AlertLog(Context context, int title_res)
  {
    mContext = context;
    LayoutInflater li = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    View v = li.inflate(R.layout.alertlog,null);
    mDialog = new AlertDialog.Builder(context)
      .setTitle(title_res)
      .setView(v)
      .show();
    mText = (TextView)mDialog.findViewById(android.R.id.text1);
    mScroll = (ScrollView)mDialog.findViewById(android.R.id.list);
  }
  public void setDoneListener(DoneListener listener) {
    mDoneListener = listener;
  }

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
        break;
      }
    }
  }
}
