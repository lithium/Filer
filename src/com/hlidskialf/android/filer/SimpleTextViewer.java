
package com.hlidskialf.android.filer;
import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.widget.TextView;
import java.io.File;
import java.io.FileInputStream;
import android.net.Uri;

public class SimpleTextViewer extends Activity
{
  private TextView mText;
  @Override
  public void onCreate(Bundle icicle)
  {
    mText = new TextView(this); 
    super.onCreate(icicle);
    setContentView(mText);

    Intent i = getIntent();
    Uri u = i.getData();
    if (u != null && u.getScheme().equals("file")) {
      try {
        File f = new File(u.getPath());
        FileInputStream fis = new FileInputStream(f);
        int size = fis.available();
        byte[] buf = new byte[size];
        fis.read(buf);
        mText.setText(new String(buf));
      } catch (java.io.IOException ex) {
        finish();
      }
    }
    else {
      finish();
    }
  }
}
