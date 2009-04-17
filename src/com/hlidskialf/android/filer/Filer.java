package com.hlidskialf.android.filer;

import java.util.GregorianCalendar;
import java.util.Calendar;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.text.DecimalFormat;


public class Filer 
{
  public static final String PREF_BROWSE_ROOT = "browse_root";
  public static final String PREF_HIDE_DOT = "hide_dot";
  public static final String PREF_HOME_PATH = "home_path";


  public static final String FORMAT_DECIMAL = "0.#";
  public static final String FORMAT_DATE_TIME = "MMM dd HH:mm";
  public static final String FORMAT_DATE_YEAR = "MMM dd yyyy";
  private final SimpleDateFormat pDateFmt_time = new SimpleDateFormat("MMM dd HH:mm");
  private final SimpleDateFormat pDateFmt_year  = new SimpleDateFormat("MMM dd yyyy");

  public static String format_size(long size)
  {
    DecimalFormat fmt = new DecimalFormat(FORMAT_DECIMAL);
    String ret;
    if (size > 1024*1024*1024) ret = fmt.format((double)size / (double)(1024*1024*1024)) + "G";
    else if (size > 1024*1024) ret = fmt.format((double)size / (double)(1024*1024)) + "M";
    else if (size > 1024) ret = fmt.format((double)size / (double)1024) + "k";
    else ret = fmt.format(size) + "b";
    return ret;
  }
  public static String format_date(long when)
  {
    Date last = new Date(when);
    GregorianCalendar now = new GregorianCalendar();
    GregorianCalendar then = new GregorianCalendar();
    then.setTime(last);

    SimpleDateFormat fmt = new SimpleDateFormat(now.get(Calendar.YEAR) == then.get(Calendar.YEAR) ? FORMAT_DATE_TIME : FORMAT_DATE_YEAR);
    return fmt.format(last);
  }

}
