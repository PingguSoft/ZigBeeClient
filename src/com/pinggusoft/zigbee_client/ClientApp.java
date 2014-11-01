package com.pinggusoft.zigbee_client;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.Vector;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;

public class ClientApp extends Application {
    private final static String KEY_BTDEVICE           = "KEY_BTDEVICE";
    private final static String KEY_INSTALL_TIME       = "KEY_INSTALL_TIME";
    private final static String KEY_INSTALL_VER        = "KEY_INSTALL_VER";

    public String   m_strBTDevice = null;
    public String   m_strBTAddr = null;
    private long    m_lFirstInstallTime;
    private String  m_strVer;
    private SharedPreferences m_spBTCon;
    private Editor  m_editorBTCon;
    
    @Override
    public void onCreate() {
        super.onCreate();
       
        LogUtil.initialize(this);
        m_spBTCon = PreferenceManager.getDefaultSharedPreferences(this);
        m_editorBTCon = m_spBTCon.edit();
        readSettings();
    }
    
    private String getTimeString(long lTime) {
        Date date = new Date(lTime);
        Locale locale = Locale.getDefault(); 
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy MMM d, EEE. aa h:mm:ss", locale);
        
        return sdf.format(date);
    }
    
    public void readSettings() {
        m_strBTDevice = m_spBTCon.getString(KEY_BTDEVICE, null);
        if (m_strBTDevice != null)
            m_strBTAddr = m_strBTDevice.substring(m_strBTDevice.length() - 17);
        
        getInstalledTime();
        m_strVer = m_spBTCon.getString(KEY_INSTALL_VER, null);
    }
    
    private void getInstalledTime() {
        long lInstallTimeSP = m_spBTCon.getLong(KEY_INSTALL_TIME, -1);
        
        ByteBuffer  byteBuf = ByteBuffer.allocate(8);
        byteBuf.order(ByteOrder.LITTLE_ENDIAN);
        File file = new File(Environment.getExternalStorageDirectory().getPath() + "/Android/data/", ".noctb");
        if (file.exists()) {
            LogUtil.e("file exists");
            try {
                FileInputStream os = new FileInputStream(file);
                os.read(byteBuf.array());
                if (os != null)
                    os.close();
                m_lFirstInstallTime = byteBuf.getLong();
                LogUtil.e("installed time file : " + getTimeString(m_lFirstInstallTime));
            } catch (IOException e) {
                e.printStackTrace();
            }

            LogUtil.e("SP:" + lInstallTimeSP + ", FILE:" + m_lFirstInstallTime);
            
            if (lInstallTimeSP == -1) {
                // app is un-installed, use time of .noctb
                m_editorBTCon.putLong(KEY_INSTALL_TIME, m_lFirstInstallTime);
                m_editorBTCon.commit();
                LogUtil.e("app in un-installed, use file :" + getTimeString(m_lFirstInstallTime));
            } else if (lInstallTimeSP != m_lFirstInstallTime) {
                // different time, use min time
                m_lFirstInstallTime = Math.min(m_lFirstInstallTime, lInstallTimeSP);
                LogUtil.e("different time use min time:" + getTimeString(m_lFirstInstallTime));
            }
        } else {
            LogUtil.e("file doesn't exist");

            File fileDir = new File(Environment.getExternalStorageDirectory().getPath() + "/Android/data/");
            fileDir.mkdirs();

            if (lInstallTimeSP == -1) {
                // real first install
                m_lFirstInstallTime = System.currentTimeMillis();
                m_editorBTCon.putLong(KEY_INSTALL_TIME,  m_lFirstInstallTime);
                m_editorBTCon.commit();
                LogUtil.e("real first install!!:" + getTimeString(m_lFirstInstallTime));
            } else {
                // .noctb is removed
                m_lFirstInstallTime = lInstallTimeSP;
                LogUtil.e("file is removed!!, Use SP:" + getTimeString(m_lFirstInstallTime));
            }
            byteBuf.putLong(m_lFirstInstallTime);

            try {
                FileOutputStream os = new FileOutputStream(file);
                os.write(byteBuf.array());
                if (os != null)
                    os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void saveSettings() {
        m_editorBTCon.putString(KEY_BTDEVICE, m_strBTDevice);
        m_editorBTCon.commit();
        
        if (m_strBTDevice != null)
            m_strBTAddr = m_strBTDevice.substring(m_strBTDevice.length() - 17);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
    }
    
    public void setBTDevice(String device) {
        m_strBTDevice = device;

        if (m_strBTDevice != null)
            m_strBTAddr = m_strBTDevice.substring(m_strBTDevice.length() - 17);
    }
    
    public String getBTDevice() {
        return m_strBTDevice;
    }
    
    public String getBTAddr() {
        return m_strBTAddr;
    }
    
    public boolean IsExpired() {
        getInstalledTime();
        
        long diff  = System.currentTimeMillis() - m_lFirstInstallTime;
        long hours = diff / (1000 * 60 * 60);   

        LogUtil.e(String.format("inst:%d,  diff:%d, hour:%d", m_lFirstInstallTime, diff, hours));
        if (hours > 24)
            return true;
        
        return false;
    }

    public void testExpire() {
        m_lFirstInstallTime -= (1000 * 60 * 60 * 24);
        m_editorBTCon.putLong(KEY_INSTALL_TIME,  m_lFirstInstallTime);
        m_editorBTCon.commit();
    }
    
    public String getInstVer() {
        return m_strVer;
    }
    
    public void setInstVer(String strVer) {
        m_strVer = strVer;
        m_editorBTCon.putString(KEY_INSTALL_VER, m_strVer);
        m_editorBTCon.commit();
    }
    
    public String getPackageVer() {
        String strVer = null;
        try {
            strVer = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (NameNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        return strVer;
    }
    
    public static boolean isAboveICS() {
       return android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH;
    }
    
    public boolean isAuthorized() {
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        String imei = telephonyManager.getDeviceId();
        
        if (imei != null && imei.equals("358240055059836"))
            return true;
        
        return false;
    }
    
    
    
    
    
    private final static String INFO_FILE = "node_info.dat";
    private Vector <ZigBeeNode> mNodeList = new Vector <ZigBeeNode>();
    
    public int checkData(byte buf[]) {
        if (buf.length < ZigBeeNode.NODE_SIZE) {
            LogUtil.e("ABNORMAL DATA SIZE !!");
            return -1;
        }

        byte[] bufDst = new byte[16];
        ByteBuffer byteBuf = ByteBuffer.allocate(ZigBeeNode.NODE_SIZE);
        
        byteBuf.order(ByteOrder.LITTLE_ENDIAN);
        byteBuf.put(buf);
        byteBuf.rewind();
        
        Arrays.fill(bufDst, (byte)0);
        byteBuf.get(bufDst, 0, 4);                  // signature
        String strSig = new String(bufDst).trim();
        if (!strSig.equals(ZigBeeNode.NODE_SIGNATURE)) {
            LogUtil.e("SIGNATURE MISMATCHED !! : " + strSig);
            return -1;
        }
        
        byteBuf.get(bufDst, 0, 16);                 // addr
        String strNodeAddr = new String(bufDst).trim();
        LogUtil.i("ADDR:" + strNodeAddr);
        
        int nNodeType = (int)byteBuf.get();         // type
        byteBuf.get(bufDst, 0, 16);                 // name
        String strNodeName = new String(bufDst).trim();
        LogUtil.i("NAME:" + strNodeName + ", type:" + nNodeType);
        
        for (int i = 0; i < ZigBeeNode.GPIO_CNT; i++) {
            int mode = (int)byteBuf.get();         // gpio mode
            byteBuf.get(bufDst, 0, 16);            // gpio name
            String strName = new String(bufDst).trim();
            LogUtil.i("GPIO" + i + ", MODE:" + mode + ", NAME:" + strName);
        }
        
        return 1;
    }
    
    public void addNode(ZigBeeNode node, boolean preserveGpioName) {
        ZigBeeNode n;
        Boolean    boolFound = false;
        
        for (int i = 0; i < mNodeList.size(); i++) {
            n = mNodeList.get(i);
            if (node.getAddr().equals(n.getAddr())) {
                
                if (preserveGpioName) {
                    for (int j = 0; j < node.getMaxGPIO(); j++) {
                        node.setGpioName(j, n.getGpioName(j));
                        node.setGpioUsage(j, n.getGpioUsage(j));
                    }
                }
                
                mNodeList.set(i, node);
                boolFound = true;
                LogUtil.i("UPDATED:" + node.getAddr());
                break;
            }
        }
        
        if (!boolFound) {
            mNodeList.add(node);
            LogUtil.i("ADDED--:" + node.getAddr());
        }
    }
    
    public ZigBeeNode getNode(String addr) {
        ZigBeeNode n;

        for (int i = 0; i < mNodeList.size(); i++) {
            n = mNodeList.get(i);
            if (addr.equals(n.getAddr())) {
                return n;
            }
        }
        return null;
    }
    
    public void removeNodes() {
        mNodeList.removeAllElements();
    }
    
    public ZigBeeNode getNode(int pos) {
        if (mNodeList.size() > pos)
            return mNodeList.get(pos);
        else 
            return null;
    }
    
    public int getNodeCtr() {
        return mNodeList.size();
    }
    
    
    public static final String SIGNATURE = "ZBHA";
    
    public void load() {
        FileInputStream inputStream;
        byte[] buf = null;
        int nNodeCtr = 0;
        
        removeNodes();
        String strPath = getFilesDir().getPath() + "/" + INFO_FILE;
        File file = new File(strPath);
        if (!file.exists()) {
            return;
        }

        try {
            buf = new byte[ZigBeeNode.NODE_SIZE];
            ByteBuffer bb = ByteBuffer.allocate(4);
            bb.order(ByteOrder.LITTLE_ENDIAN);
            
            Arrays.fill(buf,  (byte)0);
            inputStream = openFileInput(file.getName());
            
            inputStream.read(buf, 0, 4);
            String strSig = new String(buf).trim();
            if (!strSig.equals(SIGNATURE)) {
                LogUtil.e("SIGNATURE MISMATCHED !! : " + strSig);
                inputStream.close();
                return;
            }
            
            inputStream.read(buf, 0, 4);
            bb.put(buf, 0, 4);
            bb.rewind();
            nNodeCtr = bb.getInt();

            for (int i = 0; i < nNodeCtr; i++) {
                ZigBeeNode node = new ZigBeeNode();
                inputStream.read(buf, 0, ZigBeeNode.NODE_SIZE);
                node.deserialize(buf);
                addNode(node, false);
            }

            inputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void save() {
        FileOutputStream outputStream;
        
        ByteBuffer bb = ByteBuffer.allocate(8);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.put(SIGNATURE.getBytes());
        bb.putInt(mNodeList.size());

        try {
            outputStream = openFileOutput(INFO_FILE, Context.MODE_PRIVATE);
            outputStream.write(bb.array());
            
            for (int i = 0; i < mNodeList.size(); i++) {
                byte buf[] = mNodeList.get(i).serialize();
                outputStream.write(buf, 0, buf.length);
            }
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }    
}
