package com.pinggusoft.zigbee_client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Calendar;

import android.content.Context;
import android.os.Handler;
import android.util.SparseArray;

public class RuleManager {
    private final static String SIGNATURE = "ZBHA";
    private final static String RULE_FILE = "rule.dat";

    private static SparseArray <RuleOutput>  mListOutput = new SparseArray <RuleOutput>();
    
    synchronized public static void save(Context ctx) {
        FileOutputStream outputStream;
        
        ByteBuffer bb = ByteBuffer.allocate(8);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.put(SIGNATURE.getBytes());
        bb.putInt(mListOutput.size());

        try {
            outputStream = ctx.openFileOutput(RULE_FILE, Context.MODE_PRIVATE);
            outputStream.write(bb.array());

            int key;
            for (int i = 0; i < mListOutput.size(); i++) {
                key = mListOutput.keyAt(i);
                RuleOutput port = mListOutput.get(key);
                byte buf[] = port.serialize();
                bb.clear();
                bb.putInt(buf.length);
                outputStream.write(bb.array(), 0, 4);
                outputStream.write(buf, 0, buf.length);
            }
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        LogUtil.d("DONE !!");
    }
    
    synchronized public static void load(Context ctx) {
        FileInputStream inputStream;
        byte[] buf = null;
        int nCtr = 0;
        
        mListOutput.clear();
        String strPath = ctx.getFilesDir().getPath() + "/" + RULE_FILE;
        File file = new File(strPath);
        if (!file.exists()) {
            LogUtil.e("no rule file !!");
            return;
        }

        try {
            buf = new byte[1024];
            ByteBuffer bb = ByteBuffer.allocate(4);
            bb.order(ByteOrder.LITTLE_ENDIAN);
            
            Arrays.fill(buf, (byte)0);
            inputStream = ctx.openFileInput(file.getName());
            
            inputStream.read(buf, 0, 4);
            String strSig = new String(buf).trim();
            if (!strSig.equals(SIGNATURE)) {
                LogUtil.e("SIGNATURE MISMATCHED !! : " + strSig);
                inputStream.close();
                return;
            }
            
            // port count
            inputStream.read(buf, 0, 4);
            bb.put(buf, 0, 4);
            bb.rewind();
            nCtr = bb.getInt();

            for (int i = 0; i < nCtr; i++) {
                // port data size
                inputStream.read(buf, 0, 4);
                bb.clear();
                bb.put(buf, 0, 4);
                bb.rewind();
                int size = bb.getInt();
                
                inputStream.read(buf, 0, size);
                RuleOutput rp = new RuleOutput();
                rp.deserialize(buf);
                
                mListOutput.put(rp.getID(), rp);
            }

            inputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        LogUtil.d("DONE !!");
    }
    
    synchronized public static int getOutputPortCnt() {
        return mListOutput.size();
    }
    
    synchronized public static RuleOutput getAt(int idx) {
        if (idx >= getOutputPortCnt())
            return null;

        return mListOutput.get(mListOutput.keyAt(idx));
    }
    
    synchronized public static RuleOutput get(int key) {
        return mListOutput.get(key);
    }
    
    synchronized public static void put(int key, RuleOutput output) {
        mListOutput.put(key, output);
    }
    
    synchronized public static void printRules() {
        for (int i = 0; i < getOutputPortCnt(); i++)
            getAt(i).printRule();
    }
    
    synchronized public static void evaluate(Context ctx, Handler handler) {
        for (int i = 0; i < getOutputPortCnt(); i++) {
            RuleOutput output = getAt(i);
            if (output.evaluate(ctx)) {
                int id  = ZigBeeNode.buildID(output.getNodeID(), output.getGpio());
                int val = output.getOP();
                if (handler != null) {
                    //handler.obtainMessage(ServerService.CMD_EVALUATE, id, val, null).sendToTarget();
                }
            }
        }
    }
    
    synchronized public static long getNearestNextTime() {
        Calendar calCur = Calendar.getInstance();
        int hour     = calCur.get(Calendar.HOUR_OF_DAY);
        int min      = calCur.get(Calendar.MINUTE);
        int day      = calCur.get(Calendar.DAY_OF_WEEK) - 1;
        int timeCur  = RuleInput.buildTime(day, hour, min);
        int nextTime = 0;
        int diffTime = 0;
        int selTime = -1;
        int minTime  = Integer.MAX_VALUE;
        
        LogUtil.d("getNearestNextTime!! %d:%d", hour, min);
        for (int i = 0; i < getOutputPortCnt(); i++) {
            RuleOutput output = getAt(i);
            for (int j = 0; j < output.getRuleCnt(); j++) {
                RuleInput input = output.getRuleAt(j);
                if (input.getUsage() == RuleInput.USAGE_TIME) {
                    
                    nextTime = input.getNearestEvent(timeCur);
                    LogUtil.d("CUR:%x, INPUT:%x", timeCur, nextTime);
                    
                    if (nextTime == -1)
                        continue;

                    diffTime = nextTime - timeCur;
                    if (diffTime < minTime) {
                        LogUtil.d("SEL : %x", nextTime);
                        minTime = diffTime;
                        selTime = nextTime;
                    }
                }
            }
        }
        
        day = RuleInput.getDay(selTime) - day;
        Calendar calNext = calCur;
        calNext.set(Calendar.HOUR_OF_DAY, RuleInput.getHour(selTime));
        calNext.set(Calendar.MINUTE, RuleInput.getMinute(selTime));
        calNext.set(Calendar.SECOND, 0);
        calNext.add(Calendar.DAY_OF_MONTH, day);
        LogUtil.d("getNearestNextTime : %d %d:%d", RuleInput.getDay(selTime), RuleInput.getHour(selTime), RuleInput.getMinute(selTime));
        
        return calNext.getTimeInMillis();
    }
}
