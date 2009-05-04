package com.hlidskialf.android.filer;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class AlertInput {
  private AlertDialog mDialog;
  private OnCompleteListener mListener;
  private EditText mText;

  public interface OnCompleteListener {
    public void onComplete(String value);
    public void onCancel();
  };

  public AlertInput(Context context, int title_res, String splash, String default_value)
  {
    LayoutInflater li = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    View layout = li.inflate(R.layout.alertinput, null);

    TextView tv = (TextView)layout.findViewById(android.R.id.text1);
    tv.setText(splash);

    mText = (EditText)layout.findViewById(android.R.id.text2);
    mText.setHint(default_value);
    mText.setText(default_value);
    mText.setSelectAllOnFocus(true);

    mDialog = new AlertDialog.Builder(context)
      .setTitle(title_res)
      .setView(layout)
      .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) { 
          if (mListener != null) {
            mListener.onComplete(mText.getText().toString());
          }
          dialog.dismiss();
        }
      })
      .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) { 
          if (mListener != null) {
            mListener.onCancel();
          }
          dialog.dismiss(); 
        }
      })
      .show();
  }
  public void setOnCompleteListener(OnCompleteListener listener) { mListener = listener; }
}
