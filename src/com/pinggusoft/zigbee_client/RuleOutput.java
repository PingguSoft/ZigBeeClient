package com.pinggusoft.zigbee_client;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import android.content.Context;
import android.util.SparseArray;
import android.view.View;
import android.widget.Spinner;
import android.widget.TableRow;

public class RuleOutput {
    public  final static int OP_OFF     = 0;
    public  final static int OP_ON      = 1;
    public  final static int OP_TOGGLE  = 2;
    
    private int     nID;            // (node_nid << 16) | (gpio_no << 8) | (op << 4) | usage
    private int     nRowKey;
    private SparseArray <RuleInput> listRules    = new SparseArray <RuleInput>();
    private SparseArray <TableRow>  listTableRow = new SparseArray <TableRow>();
    
    public RuleOutput() {
        nID = 0;
        nRowKey = 0;
    }

    public RuleOutput(int id) {
        nID = id;
        nRowKey = 0;
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
    
    public int getOP() {
        return ((nID >> 4) & 0xf);
    }
    
    public int getRuleCnt() {
        return listRules.size();
    }
    
    public RuleInput getRule(int key) {
        return listRules.get(key);
    }
    
    public RuleInput getRuleAt(int idx) {
        if (idx >= getRuleCnt())
            return null;

        return listRules.get(listRules.keyAt(idx));
    }
    
    public void putRule(int key, RuleInput rule) {
        listRules.put(key, rule);
    }
    
    public void removeRule(int key) {
        listRules.remove(key);
    }
    
    public void putRow(int key, TableRow row) {
        listTableRow.put(key, row);
        LogUtil.d("ROW added to :%d Total:%d", key, listTableRow.size());
    }

    public void removeRow(int key) {
        listTableRow.remove(key);
        LogUtil.d("ROW removed  :%d Total:%d", key, listTableRow.size());
    }
    
    public TableRow getRowAt(int idx) {
        if (idx >= listTableRow.size())
            return null;
        
        return listTableRow.get(listTableRow.keyAt(idx));
    }
    
    public void incRowKey() {
        nRowKey++;
    }
    
    public int getRowKey() {
        return nRowKey;
    }
    
    public void redrawRules() {
        TableRow    row;
        Spinner     spinOperator = null;
        
        for (int i = 0; i < getRuleCnt(); i++) {
            row = getRowAt(i);
            if (row != null) {
                spinOperator  = (Spinner)row.findViewById(R.id.spinnerOperator);
                if (i == listTableRow.size() - 1) {
                    spinOperator.setVisibility(View.INVISIBLE);
                } else {
                    spinOperator.setVisibility(View.VISIBLE);
                }
            }
        }
    }
    
    public void printRule() {
        RuleInput    row;

        LogUtil.d("---- rule for %x ----", nID);
        for (int i = 0; i < getRuleCnt(); i++) {
            row = getRuleAt(i);
            if (row != null)
                row.printRule();
        }
    }
    
    public byte[] serialize() {
        int nBufSize = (Integer.SIZE / 8) * 2;
       
        RuleInput row;
        for (int i = 0; i < getRuleCnt(); i++) {
            row = getRuleAt(i);
            if (row != null) {
                nBufSize += row.getSize();
            }
        }

        ByteBuffer byteBuf = ByteBuffer.allocate(nBufSize);
        byteBuf.order(ByteOrder.LITTLE_ENDIAN);
        byteBuf.clear();
        byteBuf.putInt(nID);                // ID
        byteBuf.putInt(getRuleCnt());       // rule count
        
        for (int i = 0; i < getRuleCnt(); i++) {
            row = getRuleAt(i);
            if (row != null) {
                byte[] data = row.serialize();
                byteBuf.put(data);
            }
        }

        LogUtil.d("DONE !!  RULE:%d", listRules.size());
        return byteBuf.array();
    }
    
    public void deserialize(byte[] buf) {
        listRules.clear();
        
        ByteBuffer byteBuf = ByteBuffer.allocate(buf.length);
        byteBuf.order(ByteOrder.LITTLE_ENDIAN);
        byteBuf.put(buf);
        byteBuf.rewind();
        
        nID     = byteBuf.getInt();
        nRowKey = byteBuf.getInt();
        for (int i = 0; i < nRowKey; i++) {
            RuleInput row = new RuleInput();
            row.deserialize(byteBuf);
            listRules.put(i, row);
        }
        LogUtil.d("DONE !!  ID:%x, RULE:%d", nID, nRowKey);
    }
    
    public boolean evaluate(Context ctx) {
        boolean   retFinal = false;
        boolean   ret;
        int       nextOP = 0;
        RuleInput rule;

        for (int i = 0; i < getRuleCnt(); i++) {
            rule   = getRuleAt(i);
            ret    = rule.evaluate(ctx);

            if (i == 0) {
                retFinal = ret;
            }
            else {
                if (nextOP == RuleInput.OP_OR)
                    retFinal = retFinal || ret;
                else
                    retFinal = retFinal && ret;
            }
            nextOP = rule.getOP();
        }
        LogUtil.d("---- final output for %x  ==> %d", nID, ((retFinal == true) ? 1 : 0));
        
        return retFinal;
    }
    
    public static int buildID(int nid, int gpio, int op, int usage) {
        return (nid << 16) | (gpio << 8) | (op << 4) | usage;
    }
    
    public static int buildID(int nid, int gpio, int usage) {
        return (nid << 16) | (gpio << 8) | usage;
    }
    
    public static int rebuildID(int id, int op) {
        int mask = ~(0x0f << 4);
        int ret  = (id & mask) | (op << 4);
        return ret;
    }
}
