
package com.hlidskialf.android.filer;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Spinner;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Button;
import android.widget.AdapterView;

import android.text.Editable;
import android.text.TextWatcher;

import android.widget.ArrayAdapter;

public class EditMimeDialog
{
  private Context mContext;
  private AlertDialog mDialog;

  private ImageButton mImageButton;
  private Spinner mSpinner;
  private ArrayAdapter mSpinAdapter;
  private Button mOkButton;
  private EditText mEditExt,mEditMime;

  private int mMimeId;
  private String mIcon;
  private String mExtension;
  private String mMimetype;
  private String mAction;



  public EditMimeDialog(Context context, int title_res, int mime_id)
  {
    mMimeId = mime_id;
    mContext = context;
    LayoutInflater mFactory = LayoutInflater.from(mContext);
    View v = mFactory.inflate(R.layout.edit_mime, null);

    mSpinner = (Spinner)v.findViewById(R.id.edit_mime_action);
    mSpinAdapter = ArrayAdapter.createFromResource(context, R.array.actions, android.R.layout.simple_spinner_item);
    mSpinAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    mSpinner.setAdapter(mSpinAdapter);

    mImageButton = (ImageButton)v.findViewById(R.id.edit_mime_icon);
    mImageButton.setOnClickListener(new Button.OnClickListener() {
      public void onClick(View v) {
        IconPicker picker = new IconPicker(mContext, mContext.getString(R.string.choose_icon));
        picker.setOnIconPickedListener(new IconPicker.OnIconPickedListener() {
          public void onIconPicked(String url) {
            setIcon(url);
          }
        });
        picker.show();
      }
    });

    mEditExt = (EditText)v.findViewById(R.id.edit_mime_ext);
    mEditExt.addTextChangedListener(new TextWatcher() {
      public void beforeTextChanged(CharSequence s, int start, int count, int after){}
      public void onTextChanged(CharSequence s, int start, int before, int count){}
      public void afterTextChanged(Editable s) {
        mExtension = s.toString();
        update_ok();
      }
    });

    mEditMime = (EditText)v.findViewById(R.id.edit_mime_mimetype);
    mEditMime.addTextChangedListener(new TextWatcher() {
      public void beforeTextChanged(CharSequence s, int start, int count, int after){}
      public void onTextChanged(CharSequence s, int start, int before, int count){}
      public void afterTextChanged(Editable s) {
        mMimetype = s.toString();
        update_ok();
      }
    });


    if (mime_id > 0) {
      load_mime(mime_id);
    }

    mDialog = new AlertDialog.Builder(context)
      .setTitle(title_res)
      .setView(v)
      .setNegativeButton(android.R.string.cancel, null)
      .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dia, int which) {
          save_mime();
        }
      })
      .show();

    mOkButton = (Button)mDialog.findViewById(android.R.id.button1);

    update_ok();
  }

  public void setExtension(String new_ext)
  {
    mExtension = new_ext;
    mEditExt.setText(mExtension);
  }
  public void setMimetype(String new_mime)
  {
    mMimetype = new_mime;
    mEditMime.setText(mMimetype);
  }
  public void setAction(String new_action)
  {
    mAction = new_action;
  }
  public void setIcon(String new_url)
  {
    mIcon = new_url;
    Filer.setImageFromUri(mImageButton, Uri.parse(mIcon));
  }

  private boolean is_complete()
  {
    mAction = (String)mSpinner.getSelectedItem();
    return (mMimetype != null && mMimetype.length() > 0 && mExtension != null && mExtension.length() > 0);
  }

  private void update_ok()
  {
    mOkButton.setEnabled(is_complete());
  }
  private void save_mime()
  {
    if (!is_complete()) return;
    if (!mExtension.startsWith(".")) mExtension = "."+mExtension;
    if (mAction.startsWith("ACTION_")) mAction = mAction.replaceFirst("ACTION_","android.intent.action.");

    if (mMimeId == 0) {
      Filer.insertMimetype(mContext, mExtension, mMimetype, mIcon, mAction);
    }
    else {
      Filer.updateMimetype(mContext, mMimeId, mExtension, mMimetype, mIcon, mAction);
    }
  }
  private void load_mime(int mime_id)
  {
    Filer.getMimetype(mContext, mime_id, new Filer.MimetypeReporter() {
      public void reportMime(int id, String ext, String mime, String icon, String action) {
        setExtension(ext);
        setMimetype(mime);
        setAction(action);
        setIcon(icon);
      }
    });
  }
}
