package com.pinggusoft.zigbee_client;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Calendar;

import com.pinggusoft.zigbee_client.BuildConfig;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

public class LogUtil {
    
    private static String m_strLogFileFolderPath = "";
    private static String m_strLogFileName = "logs.txt";
    private static boolean m_boolWrite = false;

    public static void initialize(Context context)
    {
        if (BuildConfig.DEBUG) {
            m_strLogFileFolderPath = Environment.getExternalStorageDirectory () + "/PebbleNoti";
            File AppFolder = new File(Environment.getExternalStorageDirectory () + "/PebbleNoti");
            if(!AppFolder.exists()){
                if (AppFolder.mkdirs()){
                }
                else{
                }
            } else {
            }
        }
    }
    
    public static void reset()
    {
        if (BuildConfig.DEBUG) {
            File file = new File(m_strLogFileFolderPath + "/" + m_strLogFileName);
            file.delete();
            
            write("[E]", "LogUtil", "SnowFileLogUtil.reset()");
        }
    }
    
    private static void write(String strLevel, String strTag, String strMessage, Object ... args)
    {
        if (m_boolWrite) {
            String _strMessage = strMessage;
            if ( (strMessage == null) || (strMessage.length() == 0) )
                return;
            
            if (args.length != 0)
            {
                _strMessage = String.format(strMessage, args);
            }
            
            _strMessage = strLevel + " " + getCurrentTime() + "\t" + strTag + "\t" + _strMessage + "\n";
            
            File file = new File(m_strLogFileFolderPath + "/" + m_strLogFileName);
            FileOutputStream fos = null;
            
            try
            {
                fos = new FileOutputStream(file, true);
                if (fos != null)
                {
                    fos.write(_strMessage.getBytes());
                }
                
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
            finally
            {
                try
                {
                    if (fos != null)
                    {
                        fos.close();
                    }
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                }
            }
        }
    }
    
    public static void d(String strFormat, Object ... args) {
        if (BuildConfig.DEBUG) {
            if ( (strFormat == null) || (strFormat.length() == 0) )
                return;

            String strMessage = strFormat;
            if (args.length != 0)
                strMessage = String.format(strFormat, args);
            
            int nLine = Thread.currentThread().getStackTrace()[3].getLineNumber();
            String strClass = Thread.currentThread().getStackTrace()[3].getClassName();
            String strMethod = Thread.currentThread().getStackTrace()[3].getMethodName();
            strClass = strClass.substring(strClass.lastIndexOf(".") + 1);
            
            String str = String.format("[%5d] %-20s %s", nLine, strMethod, strMessage);
            Log.d(strClass, str);
            write("[D]", strClass, str);
        }
    }

    public static void i(String strFormat, Object ... args) {
        if (BuildConfig.DEBUG) {
            if ( (strFormat == null) || (strFormat.length() == 0) )
                return;

            String strMessage = strFormat;
            if (args.length != 0)
                strMessage = String.format(strFormat, args);

            int nLine = Thread.currentThread().getStackTrace()[3].getLineNumber();
            String strClass = Thread.currentThread().getStackTrace()[3].getClassName();
            String strMethod = Thread.currentThread().getStackTrace()[3].getMethodName();
            strClass = strClass.substring(strClass.lastIndexOf(".") + 1);
            
            String str = String.format("[%5d] %-20s %s", nLine, strMethod, strMessage);
            Log.i(strClass, str);
            write("[I]", strClass, str);
        }
    }            
    
    public static void w(String strFormat, Object ... args) {
        if (BuildConfig.DEBUG) {
            if ( (strFormat == null) || (strFormat.length() == 0) )
                return;

            String strMessage = strFormat;
            if (args.length != 0)
                strMessage = String.format(strFormat, args);
            
            int nLine = Thread.currentThread().getStackTrace()[3].getLineNumber();
            String strClass = Thread.currentThread().getStackTrace()[3].getClassName();
            String strMethod = Thread.currentThread().getStackTrace()[3].getMethodName();
            strClass = strClass.substring(strClass.lastIndexOf(".") + 1);
            
            String str = String.format("[%5d] %-20s %s", nLine, strMethod, strMessage);
            Log.w(strClass, str);
            write("[W]", strClass, str);
        }
    }            
    
    public static void e(String strFormat, Object ... args) {
        if ( (strFormat == null) || (strFormat.length() == 0) )
            return;

        String strMessage = strFormat;
        if (args.length != 0)
            strMessage = String.format(strFormat, args);
        
        int nLine = Thread.currentThread().getStackTrace()[3].getLineNumber();
        String strClass = Thread.currentThread().getStackTrace()[3].getClassName();
        String strMethod = Thread.currentThread().getStackTrace()[3].getMethodName();
        strClass = strClass.substring(strClass.lastIndexOf(".") + 1);
        
        String str = String.format("[%5d] %-20s %s", nLine, strMethod, strMessage);
        Log.e(strClass, str);
        
        if (BuildConfig.DEBUG) {
            write("[E]", strClass, str);
        }
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
}
