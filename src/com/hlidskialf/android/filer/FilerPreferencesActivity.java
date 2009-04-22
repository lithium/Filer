package com.hlidskialf.android.filer;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.PreferenceActivity;

public class FilerPreferencesActivity extends PreferenceActivity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    addPreferencesFromResource(R.xml.preferences);
  }
  @Override
  public boolean onPreferenceTreeClick(PreferenceScreen screen, Preference preference)
  {
    String key = preference.getKey();
    if (key != null && key.equals("mime_types")) {
      IconPicker ip = new IconPicker(this, "pick icon");
      ip.show();
    }
    return super.onPreferenceTreeClick(screen,preference);
  }
  
}
