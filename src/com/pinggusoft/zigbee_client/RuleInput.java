package com.pinggusoft.zigbee_client;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Calendar;

import android.content.Context;
import android.text.format.DateFormat;

public class RuleInput {
    public  final static int OP_AND = 0;
    public  final static int OP_OR  = 1;
    public  final static int USAGE_TIME = 0x0f;
    
    public  final static int THERMO_OFFSET = 20; 
    
    private int     nID;    // (node_nid << 16) | (gpio_no << 8) | usage
    private int     nMin;
    private int     nMax;
    private boolean boolDays[] = new boolean[7];
    private int     nOP;

    public RuleInput() {
        nID   = 0;
        nMin  = 0;
        nMax  = 0;
        nOP   = OP_AND;
    }
    
    public RuleInput(int id) {
        nID   = id;
        nMin  = 0;
        nMax  = 0;
        nOP   = OP_AND;
    }
    
    public void setID(int id) {
        nID = id;
    }
    
    public int getID() {
        return nID;
    }
    
    public int getUsage() {
        return (nID & 0xff);
    }
    
    public int getGpio() {
        return ((nID >> 8) & 0xff);
    }
    
    public int getNodeID() {
        return ((nID >> 16) & 0xffff);
    }
    
    public void setRange(int min, int max) {
        nMin = min;
        nMax = max;
    }
    
    public void setDay(int day, boolean check) {
        if (0 <= day && day < boolDays.length)
            boolDays[day] = check;
    }
    
    public boolean getDay(int day) {
        if (0 <= day && day < boolDays.length)
            return boolDays[day];
        
        return false;
    }
    
    public void setOP(int op) {
        nOP = op;
    }
    
    public int getOP() {
        return nOP;
    }
    
    public int getMin() {
        return nMin;
    }
    
    public int getMax() {
        return nMax;
    }
    
    public int getHour() {
        return nMin >> 8;
    }
    
    public int getMinute() {
        return nMin & 0xff;
    }
    
    public void setTime(int hour, int min) {
        nMin = (hour << 8) | min;
    }
    
    public void printRule() {
        LogUtil.d("id:%x, min:%d, max:%d, op:%d", nID, nMin, nMax, nOP);
    }
    
    public String getTimeString() {
        return String.format("%d:%02d", nMin >> 8, (nMin & 0xff));
    }
    
    public String getThermoString() {
        return String.format("%d ~ %d", nMin, nMax);
    }
    
    public int getSize() {
        int nBufSize = (Integer.SIZE / 8) * 3 + 1;
        
        if (getUsage() == USAGE_TIME) {
            nBufSize += 7;
        }
        
        return nBufSize;
    }
    
    public byte[] serialize() {
        int nBufSize = (Integer.SIZE / 8) * 3 + 1;
        
        if (getUsage() == USAGE_TIME) {
            nBufSize += boolDays.length;
        }

        ByteBuffer byteBuf = ByteBuffer.allocate(nBufSize);
        byteBuf.order(ByteOrder.LITTLE_ENDIAN);
        byteBuf.clear();
        byteBuf.putInt(nID);                        // ID
        byteBuf.putInt(nMin);                       // Min
        byteBuf.putInt(nMax);                       // Max
        byteBuf.put((byte)nOP);
        if (getUsage() == USAGE_TIME) {
            for (int i = 0; i < boolDays.length; i++) {
                byteBuf.put((byte)((boolDays[i] == true) ? 1 : 0));
            }
        }
        LogUtil.d("DONE !!");

        return byteBuf.array();
    }
    
    public void deserialize(ByteBuffer buf) {
        nID  = buf.getInt();
        nMin = buf.getInt();
        nMax = buf.getInt();
        nOP  = buf.get();
        if (getUsage() == USAGE_TIME) {
            for (int i = 0; i < boolDays.length; i++) {
                boolDays[i] = (buf.get() == 1 ? true : false);
            }
        }
    }
    
    public boolean evaluate(Context ctx) {
        boolean   ret   = false;
        ClientApp app   = (ClientApp)ctx;
        int       usage = getUsage();

        if (usage == ZigBeeNode.TYPE_INPUT_TOUCH || usage == ZigBeeNode.TYPE_INPUT_SWITCH) { 
            ZigBeeNode node  = app.getNode(getNodeID());
            LogUtil.d("node:%d. gpio:%d, val:%d, cond:%d", getNodeID(), getGpio(), node.getGpioValue(getGpio()), getMax());
            if (node.getGpioValue(getGpio()) == getMax())
                ret = true;
        } else if (usage == ZigBeeNode.TYPE_INPUT_ANALOG) {
            ZigBeeNode node  = app.getNode(getNodeID());
            int value = node.getGpioAnalog(getGpio());
            if (getMin() <= value && value <= getMax())
                ret = true;
        } else if (usage == USAGE_TIME) {
            Calendar c = Calendar.getInstance();
            int      d = c.get(Calendar.DAY_OF_WEEK) - 1;
            
            if (getDay(d)) {
                int h = c.get(Calendar.HOUR_OF_DAY);
                int m = c.get(Calendar.MINUTE);
                
                Calendar  calCur = Calendar.getInstance();
                calCur.set(0, 0, 1, h, m);
                
                Calendar  calSet = Calendar.getInstance();
                calSet.set(0, 0, 1, getHour(), getMinute());
                LogUtil.d("SET:" + String.valueOf(DateFormat.format("MMM d, EEE. aa h:mm", calSet)));
                LogUtil.d("CUR:" + String.valueOf(DateFormat.format("MMM d, EEE. aa h:mm", calCur)));
                if (calCur.compareTo(calSet) == 0)
                    ret = true;
            }
        }
        LogUtil.d("evaluate rule for %x  ==> %d", nID, ((ret == true) ? 1 : 0));

        return ret;
    }
}
