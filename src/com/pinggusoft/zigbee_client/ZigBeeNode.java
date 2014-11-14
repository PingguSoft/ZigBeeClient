package com.pinggusoft.zigbee_client;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

public class ZigBeeNode {
    public  final static int TYPE_NONE         = 0;
    public  final static int TYPE_INPUT_TOUCH  = 1;
    public  final static int TYPE_INPUT_SWITCH = 2;
    public  final static int TYPE_INPUT_ANALOG = 3;
    public  final static int TYPE_OUTPUT_LIGHT = 4;
    public  final static int TYPE_RESERVED     = 5;
    
    public  final static int GPIO_CNT  = 17;
    public  final static int GPIO_MODE_DISABLED = 0;
    public  final static int GPIO_MODE_DIN      = 1;
    public  final static int GPIO_MODE_DOUT_LO  = 2;
    public  final static int GPIO_MODE_DOUT_HI  = 3;
    public  final static int GPIO_MODE_AIN      = 4;
    public  final static int GPIO_MODE_RESERVED = 5;
    
    private final static int    GPIO_PINS[]       = { 3, 4, 5, 6, 7, 8, 9, 10, 11, 32, 31, 30, 29, 28, 27, 24, 23 };
    private final static String GPIO_MODE_LABEL[] = { "0 - disabled", "1 - DIN", "2 - DOUT (low)", "3 - DOUT (high)", "4 - Analog In", "5 - Reserved" };
    
    private boolean     mBoolRemote    = false;
    private int         mIntNodeType   = 0;
    private String      mStrNodeAddr   = null;
    private String      mStrNodeName   = null;
    private String      mStrGpioMode   = null;
    private String      mStrGpioUsage  = null;
    private String      mStrAIValue    = null;
    private String      mStrGpioName[] = new String[GPIO_CNT];
    private int         mIntAnalog[]   = new int[6];
    private int         mIntGpioValue  = 0;

//    public ZigBeeNode(String addr, boolean remote, int type, String name, String mode) {
//        mStrNodeAddr = addr;
//        mBoolRemote  = remote;
//        mIntNodeType = type;
//        mStrNodeName = name;
//        mStrGpioMode = mode;
//    }
    
    public ZigBeeNode(String addr) {
        mStrNodeAddr = addr;
        mBoolRemote  = true;
        mIntNodeType = 0;
        mStrNodeName = null;
        mStrGpioMode = null;
    }

    public ZigBeeNode() {
        mStrNodeAddr = null;
        mBoolRemote  = false;
        mIntNodeType = 0;
        mStrNodeName = null;
        mStrGpioMode = null;
    }

    public static boolean isNumeric(String str)  
    {  
        try {  
            double d = Double.parseDouble(str);  
        } catch(NumberFormatException nfe) {  
            return false;
        }  
        return true;
    }
    
    public boolean isRemote() {
        return mBoolRemote;
    }
    
    public static int getGpioModeFromUsage(int mode) {
        switch(mode) {
        case ZigBeeNode.TYPE_INPUT_TOUCH:
        case ZigBeeNode.TYPE_INPUT_SWITCH:
            mode = ZigBeeNode.GPIO_MODE_DIN;
            break;
            
        case ZigBeeNode.TYPE_INPUT_ANALOG:
            mode = ZigBeeNode.GPIO_MODE_AIN;
            break;
            
        case ZigBeeNode.TYPE_OUTPUT_LIGHT:
            mode = ZigBeeNode.GPIO_MODE_DOUT_LO;
            break;

        case ZigBeeNode.TYPE_RESERVED:
            mode = ZigBeeNode.GPIO_MODE_RESERVED;
            break;
            
        default:
            mode = ZigBeeNode.GPIO_MODE_DISABLED;
            break;
        }
        return mode;
    }
    
    // GPIO USAGE
    public int getGpioUsage(int gpio) {
        int mode = 0;
        
        if (gpio >= GPIO_CNT)
            return 0;
        
        if (mStrGpioUsage == null)
            mStrGpioUsage = "00000000000000000";
        
        String strVal = mStrGpioUsage.substring(gpio, gpio + 1);
        if (isNumeric(strVal))
            mode = Integer.valueOf(strVal);
        
        return mode;
    }
    
    public String getGpioUsage() {
        if (mStrGpioUsage == null)
            mStrGpioUsage = "00000000000000000";
        
        return mStrGpioUsage;
    }
    
    public void setGpioUsage(int gpio, int mode) {
        if (gpio >= GPIO_CNT)
            return;

        if (mStrGpioUsage == null)
            mStrGpioUsage = "00000000000000000";
        
        StringBuilder builder = new StringBuilder(mStrGpioUsage);
        builder.setCharAt(gpio, (char)('0' + mode));
        mStrGpioUsage = builder.toString();
    }
    
    public void setGpioUsage(String mode) {
        mStrGpioUsage = mode;
    }
    
    // GPIO Mode
    public int getGpioMode(int gpio) {
        int mode = 0;
        
        if (gpio >= GPIO_CNT)
            return 0;
        
        String strVal = mStrGpioMode.substring(gpio, gpio + 1);
        if (isNumeric(strVal))
            mode = Integer.valueOf(strVal);
        
        return mode;
    }
    
    public String getGpioMode() {
        if (mStrGpioMode == null)
            mStrGpioMode = "00000000000000000";
        
        return mStrGpioMode;
    }
    
    public void setGpioMode(int gpio, int mode) {
        if (gpio >= GPIO_CNT)
            return;

        if (mStrGpioMode == null)
            mStrGpioMode = "00000000000000000";
        
        StringBuilder builder = new StringBuilder(mStrGpioMode);
        builder.setCharAt(gpio, (char)('0' + mode));
        mStrGpioMode = builder.toString();
    }
    
    public void setGpioMode(String mode) {
        mStrGpioMode = mode;
    }

    // GPIO Value
    public void setGpioValue(String value) {
        for (int i = GPIO_CNT - 1; i >= 0; i--) {
            String val  = String.valueOf(value.charAt(i));
            int    mask = (1 << i);
            mIntGpioValue &= (~mask);
            if (isNumeric(val) && Integer.valueOf(val) == 1)
                mIntGpioValue |= mask;
        }
    }
    
    public void setGpioValue(int value) {
        mIntGpioValue = value;
    }

    public void setGpioValue(int gpio, int value) {
        if (gpio >= GPIO_CNT)
            return;

        int mask = (1 << gpio);
        if (value == 0)
            mIntGpioValue &= (~mask);
        else
            mIntGpioValue |= mask;
    }

    public int getGpioValue(int gpio) {
        if (gpio >= GPIO_CNT) {
            return mIntGpioValue;
        }
        
        int value = (mIntGpioValue & (1 << gpio));
        if (value > 0)
            return 1;

        return 0;
    }
    
    public String getGpioValue() {
        String val = "";
        for (int i = 0; i < GPIO_CNT; i++) {
            int  mask = (1 << i);
            if ((mIntGpioValue & mask) == mask)
                val += "1";
            else
                val += "0";
        }
        return val;
    }
   
    // GPIO Analog
    public int getGpioAnalog(int gpio) {
        if (9 <= gpio && gpio <= 14) {
            gpio -= 9;
            return mIntAnalog[gpio];
        }
        return 0;
    }
    
    public String getGpioAnalog() {
        return mStrAIValue;
    }
    
    public void setGpioAnalog(int gpio, int value) {
        if (9 <= gpio && gpio <= 14) {
            if (getGpioMode(gpio) == GPIO_MODE_AIN) {
                gpio -= 9;
                mIntAnalog[gpio] = value;
            }
        }
    }
    
    public void setGpioAnalog(String str) {
        mStrAIValue = str;
    }
    
    
    // ETCS
    public int getMaxGPIO() {
        return GPIO_CNT;
    }
    
    public int getAnalogGpioRange() {
        return (14 << 8) | (9);
    }
    
    public static int getPinNo(int gpio) {
        return GPIO_PINS[gpio];
    }
    
    public static String getGpioModeLabel(int mode) {
        return GPIO_MODE_LABEL[mode];
    }
    
    public static String[] getGpioModeLabels() {
        return GPIO_MODE_LABEL;
    }
    
    public void setGpioName(int gpio, String name) {
        if (gpio >= GPIO_CNT)
            return;
        
        mStrGpioName[gpio] = name;
    }
    
    public String getGpioName(int gpio) {
        if (gpio >= GPIO_CNT)
            return null;
        
        return mStrGpioName[gpio];
    }
    
    public String getAddr() {
        return mStrNodeAddr;
    }
    
    public void setAddr(String addr) {
        mStrNodeAddr = addr;
    }
    
    public int getType() {
        return mIntNodeType;
    }
    
    public void setType(int type) {
        mIntNodeType = type;
    }
    
    public String getName() {
        return mStrNodeName;
    }
    
    public void setName(String name) {
        mStrNodeName = name;
    }

    private byte[] get16Bytes(String str) {
        byte[] bufDst = new byte[16];
        Arrays.fill(bufDst, (byte)0);
        
        if (str != null) {
            byte[] bufStr = str.getBytes();
            int    nLen   = Math.min(16, bufStr.length);
            System.arraycopy(bufStr, 0, bufDst, 0, nLen);
        }
        return bufDst;
    }
    
    public final static String  NODE_SIGNATURE = "NODE";
    public final static int     NODE_SIZE      = 4 + 16 + 1 + 16 + 17 * (2 + 16);
    
    public byte[] serialize() {
        ByteBuffer byteBuf = ByteBuffer.allocate(NODE_SIZE);
        byteBuf.order(ByteOrder.LITTLE_ENDIAN);
        byteBuf.clear();
        byteBuf.put(NODE_SIGNATURE.getBytes());             // signature
        byteBuf.put(get16Bytes(mStrNodeAddr));              // addr
        byteBuf.put((byte)mIntNodeType);                    // type
        byteBuf.put(get16Bytes(mStrNodeName));              // name
        for (int i = 0; i < GPIO_CNT; i++) {
            byteBuf.put((byte)getGpioUsage(i));             // gpio usage
            byteBuf.put((byte)getGpioMode(i));              // gpio mode
            byteBuf.put(get16Bytes(getGpioName(i)));        // gpio name
        }
        LogUtil.d("DONE !!");
        return byteBuf.array();
    }
    
    public int deserialize(byte buf[]) {
        if (buf.length < NODE_SIZE) {
            LogUtil.e("ABNORMAL DATA SIZE !!");
            return -1;
        }

        byte[] bufDst = new byte[16];
        ByteBuffer byteBuf = ByteBuffer.allocate(NODE_SIZE);
        
        byteBuf.order(ByteOrder.LITTLE_ENDIAN);
        byteBuf.put(buf);
        byteBuf.rewind();
        
        Arrays.fill(bufDst, (byte)0);
        byteBuf.get(bufDst, 0, 4);                  // signature
        String strSig = new String(bufDst).trim();
        if (!strSig.equals(NODE_SIGNATURE)) {
            LogUtil.e("SIGNATURE MISMATCHED !! : " + strSig);
            return -1;
        }
        
        byteBuf.get(bufDst, 0, 16);                 // addr
        mStrNodeAddr = new String(bufDst).trim();
        LogUtil.i("ADDR:" + mStrNodeAddr);
        
        mIntNodeType = (int)byteBuf.get();          // type
        if (mIntNodeType != 1)
            mBoolRemote = true;

        byteBuf.get(bufDst, 0, 16);                 // name
        mStrNodeName = new String(bufDst).trim();
        LogUtil.i("NAME:" + mStrNodeName);
        
        for (int i = 0; i < GPIO_CNT; i++) {
            setGpioUsage(i, (int)byteBuf.get());    // gpio usage
            setGpioMode(i, (int)byteBuf.get());     // gpio mode
            byteBuf.get(bufDst, 0, 16);             // gpio name
            setGpioName(i, new String(bufDst).trim());
//            LogUtil.i("GPIO" + i + ", MODE:" + getGpioMode(i) + ", NAME:" + getGpioName(i));
        }
        return 1;
    }
    
    public static int getNIDFromID(int id) {
        return (int)id >> 16;
    }
    
    public static int getGpioFromID(int id) {
        return (int)((id >> 8) & 0xff);
    }
    
    public static int buildID(int nid, int gpio) {
        return (int)((nid << 16) | (gpio << 8));
    }
}
