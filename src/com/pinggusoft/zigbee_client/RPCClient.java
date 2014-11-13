package com.pinggusoft.zigbee_client;

//The Client sessions package
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.thetransactioncompany.jsonrpc2.client.*;

//The Base package for representing JSON-RPC 2.0 messages
import com.thetransactioncompany.jsonrpc2.*;

//The JSON Smart package for JSON encoding/decoding (optional)
import net.minidev.json.*;

//For creating URLs
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


public class RPCClient {
    
    private Context         mCtx = null;
    private Handler         mHandler = null;
    private MessageManager  mMessageManager;
    
    public  static final int    CMD_BASE              = 3;
    public  static final int    CMD_RPC_READY         = CMD_BASE + 2;
    public  static final int    CMD_GET_NODE_CTR      = CMD_BASE + 3;
    public  static final int    CMD_GET_NODE          = CMD_BASE + 4;
    public  static final int    CMD_READ_GPIO         = CMD_BASE + 5;
    public  static final int    CMD_WRITE_GPIO        = CMD_BASE + 6;
    public  static final int    CMD_READ_ANALOG       = CMD_BASE + 7;
    public  static final int    CMD_GET_RULE_CTR      = CMD_BASE + 8;
    public  static final int    CMD_GET_RULE          = CMD_BASE + 9;
    public  static final int    CMD_SET_RULE          = CMD_BASE + 10;
    public  static final int    CMD_FILE_RULE         = CMD_BASE + 11;
    
    public RPCClient(Context ctx, Handler handler) {
        mCtx     = ctx;
        mHandler = handler;
        mMessageManager = new MessageManager();
        new Thread(mMessageManager).start();
        mHandler.obtainMessage(CMD_RPC_READY, 0, 0, null).sendToTarget();
    }
    
    private JSONRPC2Response callRPC(String strMethod, Map<String, Object> param, int id)
    {
        JSONRPC2Request req;
        
        URL         serverURL = null;
        ClientApp   app = (ClientApp)mCtx.getApplicationContext();
        int         serverPort = app.getServerPort();
        String      strURL = String.format("http://%s:%d/json", app.getServerAddr(), serverPort);
        try {
            serverURL = new URL(strURL);
        } catch (MalformedURLException e) {
        }

        JSONRPC2Session session = new JSONRPC2Session(serverURL);
        
        if (param == null)
            req = new JSONRPC2Request(strMethod, id);
        else
            req = new JSONRPC2Request(strMethod, param, id);

        JSONRPC2Response response = null;
        try {
            response = session.send(req);
        } catch (JSONRPC2SessionException e) {
            LogUtil.d(e.getMessage());
        }
//        if (response.indicatesSuccess())
//            LogUtil.d("CLIENT RECEIVED : " + response.getResult());
        if (response != null && !response.indicatesSuccess())
            LogUtil.d(response.getError().getMessage());
        
       
        return response;
    }
    
    /*
     ******************************************************************************************************************
     * MessageManager
     ******************************************************************************************************************
     */
    private byte[] getByteArray(JSONArray array) {
        byte[] buf = new byte[array.size()];
        for (int i = 0; i < array.size(); i++) {
            Long val = (Long)array.get(i);
            buf[i] = val.byteValue();
        }
        
        return buf;
    }
    
    private JSONArray getJSONArray(byte[] byteBuf) {
        JSONArray array = new JSONArray();
        for (int i = 0; i < byteBuf.length; i++) {
            array.add(i, byteBuf[i]);
        }
        return array;
    }
    
    private class MessageManager implements Runnable {
        private Handler messageHandler;
        private final BlockingQueue<Message> messageQueue = new LinkedBlockingQueue<Message>();

        @Override
        public void run() {
            Looper.prepare();
            messageHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    LogUtil.w("Please post() your blocking runnables to Mr Manager, " +
                            "don't use sendMessage()");
                }
            };
            Looper.loop();
        }
        
        private void consumeAsync() {
            messageHandler.post(new Runnable() {
                Map<String, Object> param = new HashMap<String,Object>();
                Map<String, Object> res;
                JSONRPC2Response resp;
                byte[] buf = null;
                int nRet;
                
                @Override
                public void run() {
                    Message msg;
                    
                    do {
                        msg = messageQueue.poll();
                        if (msg == null)
                            break;
                        
                        switch(msg.what) {
                        case CMD_GET_NODE_CTR:
                            
                            resp = callRPC("getNodeCtr", null, msg.what);
                            if (resp != null && resp.indicatesSuccess())
                                nRet = ((Long)resp.getResult()).intValue();
                            else
                                nRet = -1;
                            mHandler.obtainMessage(msg.what, msg.arg1, nRet, null).sendToTarget();
                            break;

                        case CMD_GET_NODE:
                            param.clear();
                            param.put("idx", msg.arg1);
                            resp = callRPC("getNode", param, msg.what);
                            
                            if (resp != null && resp.indicatesSuccess())
                                res = (Map<String, Object>)resp.getResult();
                            else
                                res = null;

                            buf = null;
                            if (res != null) {
                                buf = getByteArray((JSONArray)res.get("node"));
                            }
                            mHandler.obtainMessage(msg.what, msg.arg1, 0, buf).sendToTarget();
                            break;
                            
                        case CMD_WRITE_GPIO:
                            param.clear();
                            param.put("id", msg.arg1);
                            param.put("value", msg.arg2);
                            resp = callRPC("asyncWriteGpio", param, msg.what);
                            if (resp != null && resp.indicatesSuccess())
                                nRet = ((Long)resp.getResult()).intValue();
                            else
                                nRet = -1;
                            mHandler.obtainMessage(msg.what, msg.arg1, nRet, null).sendToTarget();
                            break;
                            
                        case CMD_READ_GPIO:
                            param.clear();
                            param.put("id", msg.arg1);
                            resp = callRPC("asyncReadGpio", param, msg.what);
                            if (resp != null && resp.indicatesSuccess())
                                nRet = ((Long)resp.getResult()).intValue();
                            else
                                nRet = -1;
                            mHandler.obtainMessage(msg.what, msg.arg1, nRet, null).sendToTarget();
                            break;
                            
                        case CMD_READ_ANALOG:
                            param.clear();
                            param.put("id", msg.arg1);
                            resp = callRPC("asyncReadAnalog", param, msg.what);
                            if (resp != null && resp.indicatesSuccess())
                                nRet = ((Long)resp.getResult()).intValue();
                            else
                                nRet = -1;
                            mHandler.obtainMessage(msg.what, msg.arg1, nRet, null).sendToTarget();
                            break;
                            
                        case CMD_GET_RULE_CTR:
                            resp = callRPC("getRuleCtr", null, msg.what);
                            if (resp != null && resp.indicatesSuccess())
                                nRet = ((Long)resp.getResult()).intValue();
                            else
                                nRet = -1;
                            mHandler.obtainMessage(msg.what, msg.arg1, nRet, null).sendToTarget();
                            break;
                            
                        case CMD_GET_RULE:
                            param.clear();
                            param.put("idx", msg.arg1);
                            resp = callRPC("getRule", param, msg.what);
                            
                            if (resp != null && resp.indicatesSuccess())
                                res = (Map<String, Object>)resp.getResult();
                            else
                                res = null;

                            buf = null;
                            if (res != null) {
                                buf = getByteArray((JSONArray)res.get("rule"));
                            }
                            mHandler.obtainMessage(msg.what, msg.arg1, 0, buf).sendToTarget();
                            break;
                            
                        case CMD_SET_RULE:
                            param.clear();
                            param.put("rule", (byte[])msg.obj);
                            resp = callRPC("setRule", param, msg.what);

                            if (resp != null && resp.indicatesSuccess())
                                nRet = ((Long)resp.getResult()).intValue();
                            else
                                nRet = -1;

                            mHandler.obtainMessage(msg.what, msg.arg1, nRet, null).sendToTarget();
                            break;
                            
                        case CMD_FILE_RULE:
                            param.put("save", msg.arg1);
                            resp = callRPC("fileRule", param, msg.what);
                            if (resp != null && resp.indicatesSuccess())
                                nRet = ((Long)resp.getResult()).intValue();
                            else
                                nRet = -1;
                            mHandler.obtainMessage(msg.what, msg.arg1, nRet, null).sendToTarget();
                            break;
                        }
                    } while (msg != null);
                }
            });
        }
        
        public boolean offer(final Message msg) {
            final boolean success = messageQueue.offer(msg);
            if (success) {
                consumeAsync();
            } else {
                LogUtil.d("Error offerring !!! ");
            }
            return success;
        }
    }
    
    public void asyncGetNodeCtr() {
        mMessageManager.offer(Message.obtain(null, CMD_GET_NODE_CTR, 0, 0, null));
    }
    
    public void asyncGetNode(int idx) {
        mMessageManager.offer(Message.obtain(null, CMD_GET_NODE, idx, 0, null));
    }
    
    public void asyncWriteGpio(int id, int value) {
        mMessageManager.offer(Message.obtain(null, CMD_WRITE_GPIO, id, value, null));
    }
    
    public void asyncReadGpio(int id) {
        mMessageManager.offer(Message.obtain(null, CMD_READ_GPIO, id, 0, null));
    }
    
    public void asyncReadAnalog(int id) {
        mMessageManager.offer(Message.obtain(null, CMD_READ_ANALOG, id, 0, null));
    }
    
    public void asyncGetRuleCtr() {
        mMessageManager.offer(Message.obtain(null, CMD_GET_RULE_CTR, 0, 0, null));
    }
    
    public void asyncGetRule(int idx) {
        mMessageManager.offer(Message.obtain(null, CMD_GET_RULE, idx, 0, null));
    }
    
    public void asyncSetRule(byte[] buf) {
        mMessageManager.offer(Message.obtain(null, CMD_SET_RULE, 0, 0, buf));
    }
    
    public void asyncFileRule(boolean save) {
        mMessageManager.offer(Message.obtain(null, CMD_FILE_RULE, save ? 1 : 0, 0, null));
    }
}
