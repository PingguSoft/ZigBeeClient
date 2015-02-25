package com.pinggusoft.zigbee_client;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

public class LogUtil {
    private static String   m_strTag = null;
    private static String   m_strLogFilePath = "";
    private static String   m_strLogFileName = "logs.txt";
    private static boolean  m_boolWrite = false;
    private static boolean  m_boolInit  = false;

    public static void initialize(Context context) {
        if (m_boolInit)
            return;
        
        int nLastDot = context.getPackageName().lastIndexOf('.');
        if (nLastDot > 0)
            m_strTag = context.getPackageName().substring(nLastDot + 1);
        else
            m_strTag = "TAG";

        if (!BuildConfig.DEBUG)
            return;

        m_strLogFilePath = Environment.getExternalStorageDirectory () + "/" + context.getPackageName();
        File AppFolder = new File(m_strLogFilePath);
        if (!AppFolder.exists())
            AppFolder.mkdirs();
        
        m_boolInit = true;
    }
    
    public static void reset() {
        if (!BuildConfig.DEBUG)
            return;

        File file = new File(m_strLogFilePath + "/" + m_strLogFileName);
        file.delete();
        write("[E]", "LogUtil", "--------------- log reset ---------------");
    }
    
    private static void write(String strLevel, String strTag, String strMessage, Object ... args) {
        if (!m_boolWrite)
            return;

        String _strMessage = strMessage;
        if ( (strMessage == null) || (strMessage.length() == 0) )
            return;
        
        if (args.length != 0)
            _strMessage = String.format(strMessage, args);
        
        _strMessage = strLevel + " " + getCurrentTime() + "\t" + strTag + "\t" + _strMessage + "\n";
        
        File file = new File(m_strLogFilePath + "/" + m_strLogFileName);
        FileOutputStream fos = null;
        
        try {
            fos = new FileOutputStream(file, true);
            if (fos != null)
                fos.write(_strMessage.getBytes());
        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (fos != null)
                    fos.close();
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    public static void d(String strFormat, Object ... args) {
        if (!BuildConfig.DEBUG)
            return;

        if ( (strFormat == null) || (strFormat.length() == 0) )
            return;

        String strMessage = strFormat;
        if (args.length != 0)
            strMessage = String.format(strFormat, args);
        
        int     nLine     = Thread.currentThread().getStackTrace()[3].getLineNumber();
        String  strClass  = Thread.currentThread().getStackTrace()[3].getClassName();
        String  strMethod = Thread.currentThread().getStackTrace()[3].getMethodName();
        strClass = strClass.substring(strClass.lastIndexOf(".") + 1);
        
        String str = String.format("[%5d] %-50s %s", nLine, strClass + ":" + strMethod, strMessage);
        Log.d(m_strTag, str);
        write("[D]", m_strTag, str);
    }

    public static void i(String strFormat, Object ... args) {
        if (!BuildConfig.DEBUG)
            return;
        
        if ( (strFormat == null) || (strFormat.length() == 0) )
            return;

        String strMessage = strFormat;
        if (args.length != 0)
            strMessage = String.format(strFormat, args);

        int     nLine     = Thread.currentThread().getStackTrace()[3].getLineNumber();
        String  strClass  = Thread.currentThread().getStackTrace()[3].getClassName();
        String  strMethod = Thread.currentThread().getStackTrace()[3].getMethodName();
        strClass = strClass.substring(strClass.lastIndexOf(".") + 1);
        
        String str = String.format("[%5d] %-50s %s", nLine, strClass + ":" + strMethod, strMessage);
        Log.i(m_strTag, str);
        write("[I]", m_strTag, str);
    }            
    
    public static void w(String strFormat, Object ... args) {
        if (!BuildConfig.DEBUG)
            return;
        
        if ( (strFormat == null) || (strFormat.length() == 0) )
            return;

        String strMessage = strFormat;
        if (args.length != 0)
            strMessage = String.format(strFormat, args);
        
        int     nLine     = Thread.currentThread().getStackTrace()[3].getLineNumber();
        String  strClass  = Thread.currentThread().getStackTrace()[3].getClassName();
        String  strMethod = Thread.currentThread().getStackTrace()[3].getMethodName();
        strClass = strClass.substring(strClass.lastIndexOf(".") + 1);
        
        String str = String.format("[%5d] %-50s %s", nLine, strClass + ":" + strMethod, strMessage);
        Log.w(m_strTag, str);
        write("[W]", m_strTag, str);
    }            
    
    public static void e(String strFormat, Object ... args) {
        if ( (strFormat == null) || (strFormat.length() == 0) )
            return;

        String strMessage = strFormat;
        if (args.length != 0)
            strMessage = String.format(strFormat, args);
        
        int     nLine     = Thread.currentThread().getStackTrace()[3].getLineNumber();
        String  strClass  = Thread.currentThread().getStackTrace()[3].getClassName();
        String  strMethod = Thread.currentThread().getStackTrace()[3].getMethodName();
        strClass = strClass.substring(strClass.lastIndexOf(".") + 1);
        
        String str = String.format("[%5d] %-50s %s", nLine, strClass + ":" + strMethod, strMessage);
        Log.e(m_strTag, str);
        write("[E]", m_strTag, str);
    }

    private static String getCurrentTime()
    {
        Calendar calendar = Calendar.getInstance();
        String strTime = String.format("%4d-%02d-%02d %02d:%02d:%02d", calendar.get(Calendar.YEAR), 
                                                            calendar.get(Calendar.MONTH) + 1,
                                                            calendar.get(Calendar.DAY_OF_MONTH),
                                                            calendar.get(Calendar.HOUR_OF_DAY),
                                                            calendar.get(Calendar.MINUTE),
                                                            calendar.get(Calendar.SECOND));
        return strTime;
    }
    
    public static String getFormattedTime(Context ctx, String format, long milliSeconds)
    {
        Date date = new Date(milliSeconds);
        Locale locale = Locale.getDefault();
        if (format == null)
            format = "yyyy-MM-dd HH:mm:ss";
        SimpleDateFormat sdf = new SimpleDateFormat(format, locale);
        return sdf.format(date);
    }
}
