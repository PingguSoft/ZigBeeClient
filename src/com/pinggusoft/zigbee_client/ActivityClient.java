package com.pinggusoft.zigbee_client;


import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Locale;

import com.pinggusoft.billing.util.IabHelper;
import com.pinggusoft.billing.util.IabResult;
import com.pinggusoft.billing.util.Inventory;
import com.pinggusoft.billing.util.Purchase;
import com.pinggusoft.zigbee_client.BuildConfig;
import com.pinggusoft.zigbee_client.R;
import com.pinggusoft.listitem.EntryAdapter;
import com.pinggusoft.listitem.EntryItem;
import com.pinggusoft.listitem.EntrySelItem;
import com.pinggusoft.listitem.Item;
import com.pinggusoft.listitem.SectionItem;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
    
public class ActivityClient extends Activity {
    private static final int SETTINGS_REQUEST_CODE = 1001;
    private static final int RULE_REQUEST_CODE     = 1002;

    private ClientApp       mApp;
    private ArrayList<Item> items = new ArrayList<Item>();
    private ListView        mListView = null;
    private RPCClient       mRPC = null;
    private RPCHandler      mRPCHandler = null;
    private int             mIntNodeCtr = 0;
    private int             mIntRuleCtr = 0;
    
    /*
     ******************************************************************************************************************
     * 
     ******************************************************************************************************************
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mApp =  (ClientApp)getApplication();
        setContentView(R.layout.main_list_view);
        mApp.removeNodes();
        mRPCHandler = new RPCHandler(this);
        mRPC = new RPCClient(getApplicationContext(), mRPCHandler);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.contextmenu, menu);
        
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean result = true;
        
        switch (item.getItemId()) {
            case R.id.menuPreference:{
                startActivityForResult(new Intent(this, ActivitySettings.class), SETTINGS_REQUEST_CODE);
                break;
            }
            case R.id.menuAbout:{
                //displayAboutDialog();
                break;
            }
            
            case R.id.menuRuleConfig:
                mRPC.asyncFileRule(false);
                //startActivityForResult(new Intent(this, ActivityRuleConfig.class), 0);
                break;
            
            default:{
                result = super.onOptionsItemSelected(item);
            }
        }
        
        return result;
    }
    
    @Override
    public void onStart() {
        super.onStart();
        LogUtil.e("onStart");
        
        String strVer = mApp.getInstVer();
        String strPackVer = mApp.getPackageVer();
        if (strVer == null || !strVer.equals(strPackVer)) {
            mApp.setInstVer(strPackVer);
            onClickNotice(null);
        }
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        LogUtil.e("onResume");
        
        if (mApp.getNodeCtr() == mIntNodeCtr) {
            LogUtil.d("REQUEST GPIO STATUS");
            for (int i = 0; i < mApp.getNodeCtr(); i++) {
                mRPC.asyncReadGpio(ZigBeeNode.buildID(i, 0xff));
                mRPC.asyncReadAnalog(ZigBeeNode.buildID(i, 0xff));
            }
        }
    }
    
    @Override
    public synchronized void onPause() {
        super.onPause();
        LogUtil.e("onPause");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        switch(requestCode) {
            case SETTINGS_REQUEST_CODE:
                LogUtil.d("resultCode:%d", resultCode);
                if (resultCode == RESULT_OK) {
                    mApp.removeNodes();
                    mRPCHandler.obtainMessage(RPCClient.CMD_RPC_READY, 0, 0, null).sendToTarget();
                }
                break;
                
            case RULE_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    for (int i = 0; i < RuleManager.getOutputPortCnt(); i++) {
                        RuleOutput output = RuleManager.getAt(i);
                        mRPC.asyncSetRule(output.serialize());
                    }
                    mRPC.asyncFileRule(true);
                }
                break;
        }
    }
    
    
    /*
     ******************************************************************************************************************
     * 
     ******************************************************************************************************************
     */
    private int getResID(int usage, int val) {
        switch(usage) {
        case ZigBeeNode.TYPE_INPUT_TOUCH:
            return (val == 1) ? R.drawable.type_touch_on_48 : R.drawable.type_touch_off_48;
            
        case ZigBeeNode.TYPE_INPUT_SWITCH:
            return (val == 1) ? R.drawable.switch_on_48 : R.drawable.switch_off_48;
            
        case ZigBeeNode.TYPE_INPUT_ANALOG:
            return R.drawable.type_adc_64;
            
        case ZigBeeNode.TYPE_OUTPUT_LIGHT:
            return (val == 1) ? R.drawable.light_on_48 : R.drawable.light_off_48;
            
        default:
            return -1;
        }
    }
    
    public void composeScreen() {
        mListView = (ListView)findViewById(R.id.listView);
        
        items.clear();
        for (int i = 0; i < mIntNodeCtr; i++) {
            ZigBeeNode node = mApp.getNode(i);
  
            int nCtr = 0;
            for (int j = 0; j < node.getMaxGPIO(); j++) {
                int nResID = getResID(node.getGpioUsage(j), 0);
                if (nResID > 0)
                    nCtr++;
            }            
            
            if (nCtr > 0) {
                items.add(new SectionItem(node.getName())); // + " [" + node.getAddr() + "]"));
                
                for (int j = 0; j < node.getMaxGPIO(); j++) {
    //                LogUtil.d("GPIO:%d, USAGE:%d", j, node.getGpioUsage(j));
                    int nResID = getResID(node.getGpioUsage(j), 0);
                    if (nResID > 0) {
                        items.add(new EntryItem(nResID, node.getGpioName(j), 
                                " ", ZigBeeNode.buildID(i, j)));
                    }
                }
            }
        }

        EntryAdapter adapter = new EntryAdapter(this, items, R.layout.list_item_entry_main);

        mListView.setAdapter(adapter);
        mListView.setDivider( null ); 
        mListView.setDividerHeight(0);
        mListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> l, View v, int position, long id) {
                if(items.get(position).getMode() != Item.MODE_SECTION) {
                    EntryAdapter fia = (EntryAdapter) l.getAdapter();
                    EntryItem item = (EntryItem)fia.getItem(position);
                    EntrySelItem it = null;
                    
                    if (item.getMode() == Item.MODE_ITEM_SEL) {
                        it = (EntrySelItem)item;
                    }

                    int nNode = ZigBeeNode.getNIDFromID(item.id);
                    int gpio  = ZigBeeNode.getGpioFromID(item.id);
                    ZigBeeNode node = mApp.getNode(nNode); 
                    LogUtil.d("CLICK : " + node.getName() + ", GPIO:" + gpio);
                    
                    if (node.getGpioMode(gpio) == ZigBeeNode.GPIO_MODE_DIN)
                        mRPC.asyncReadGpio(item.id);
                    else if (node.getGpioMode(gpio) == ZigBeeNode.GPIO_MODE_AIN)
                        mRPC.asyncReadAnalog(item.id);
                    else {
                        int val = node.getGpioValue(gpio) == 0 ? 1 : 0;
                        mRPC.asyncWriteGpio(item.id, val);
                        setResById(item.id, getResID(node.getGpioUsage(gpio), val));
                    }
                }
            }
        });
    }
    
    
    public void onClickNotice(View v) {
        final Dialog dlgNotice = new Dialog(this);
        
        dlgNotice.setTitle(R.string.main_notice);
        dlgNotice.setContentView(R.layout.main_notice);
        dlgNotice.setCancelable(false);

        final WebView view = (WebView)dlgNotice.findViewById(R.id.webView);
        
        String strURL;
        if (Locale.getDefault().getLanguage().equals("ko"))
            strURL = "file:///android_res/raw/notice_ko.html";
        else
            strURL = "file:///android_res/raw/notice_en.html";
        view.loadUrl(strURL);
        view.setBackgroundColor(0x00000000);
        
        final Button btnOK = (Button)dlgNotice.findViewById(R.id.buttonOK);
        btnOK.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                dlgNotice.dismiss();
            }
        });

        dlgNotice.show();
    }


    static class RPCHandler extends Handler {
        private WeakReference<ActivityClient> mParent;
        private int mRuleCtr;
        
        RPCHandler(ActivityClient parent) {
            mParent = new WeakReference<ActivityClient>(parent);
        }

        @Override
        public void handleMessage(Message msg) {
            final ActivityClient parent = mParent.get();
            
            ZigBeeNode  node;
            int         val;
            int         resID;
            int         gpio;
            int         nid;
            byte[]      buf;
            RuleOutput  ruleOutput;
            
            switch (msg.what) {
            case RPCClient.CMD_RPC_READY:
                LogUtil.i("READY !!!");
                parent.mRPC.asyncGetNodeCtr();
                break;
                
            case RPCClient.CMD_GET_NODE_CTR:
                if (msg.arg2 < 0) {
                    parent.mIntNodeCtr = 0;
                    parent.mApp.removeNodes();
                    parent.composeScreen();
                    parent.complain("failed to connect to server");
                    break;
                }

                parent.mIntNodeCtr = msg.arg2;
                for (int i = 0; i < msg.arg2; i++)
                    parent.mRPC.asyncGetNode(i);
                break;
                
            case RPCClient.CMD_GET_NODE:
                if (msg.obj == null)
                    break;
                
                buf = (byte[])msg.obj;
                node = new ZigBeeNode();
                node.deserialize(buf);
                parent.mApp.addNode(node, false);
                
                if (parent.mApp.getNodeCtr() == parent.mIntNodeCtr) {
                    parent.composeScreen();
                    for (int i = 0; i < parent.mIntNodeCtr; i++) {
                        parent.mRPC.asyncReadGpio(ZigBeeNode.buildID(i, 0xff));
                        //parent.mRPC.asyncReadAnalog(ZigBeeNode.buildID(i, 0xff));
                        int start = node.getAnalogGpioRange() & 0xff;
                        int end   = (node.getAnalogGpioRange() >> 8) & 0xff;
                        for (int j = start; j <= end; j++) {
                            parent.mRPC.asyncReadAnalog(ZigBeeNode.buildID(i, j));
                        }
                    }
                }
                break;
                
            case RPCClient.CMD_READ_GPIO:
                if (msg.arg2 < 0)
                    break;
                
                nid  = ZigBeeNode.getNIDFromID(msg.arg1);
                gpio = ZigBeeNode.getGpioFromID(msg.arg1);
                node = parent.mApp.getNode(nid);
                
                if (gpio < node.getMaxGPIO()) {
                    node.setGpioValue(gpio, msg.arg2);
                    val   = node.getGpioValue(gpio);
                    resID = parent.getResID(node.getGpioUsage(gpio), val);
                    LogUtil.d(String.format("GPIO%d=%d", gpio, val));
                    parent.setResById(msg.arg1, resID);
                } else {
                    node.setGpioValue(msg.arg2);
                    LogUtil.d(String.format("NID:%x GPIO=%s", nid, node.getGpioValue()));
                    for (int i = 0; i < node.getMaxGPIO(); i++) {
                        val   = node.getGpioValue(i);
                        resID = parent.getResID(node.getGpioUsage(i), val);
                        parent.setResById(ZigBeeNode.buildID(nid, i), resID);
                    }
                }
                break;
                
            case RPCClient.CMD_WRITE_GPIO:
                if (msg.arg2 < 0)
                    break;
                
                nid  = ZigBeeNode.getNIDFromID(msg.arg1);
                gpio = ZigBeeNode.getGpioFromID(msg.arg1);
                node = parent.mApp.getNode(nid);
                node.setGpioValue(gpio, msg.arg2);
                break;
            
            case RPCClient.CMD_READ_ANALOG:
                if (msg.arg2 < 0)
                    break;

                nid  = ZigBeeNode.getNIDFromID(msg.arg1);
                gpio = ZigBeeNode.getGpioFromID(msg.arg1);
                node = parent.mApp.getNode(nid);
                
                if (gpio < node.getMaxGPIO()) {
                    node.setGpioAnalog(gpio, msg.arg2);
                    parent.setSubTitleById(msg.arg1, String.format("ADC=%d", node.getGpioAnalog(gpio)));
                    LogUtil.d(String.format("NID:%d GPIO=%d ADC=%d", nid, gpio, node.getGpioAnalog(gpio)));
                }
                break;

                
            case RPCClient.CMD_FILE_RULE:
                if (msg.arg1 == 0)
                    parent.mRPC.asyncGetRuleCtr();
                break;

            case RPCClient.CMD_GET_RULE_CTR:
                parent.mIntRuleCtr = msg.arg2;
                mRuleCtr = msg.arg2;
                for (int i = 0; i < msg.arg2; i++)
                    parent.mRPC.asyncGetRule(i);
                if (mRuleCtr == 0)
                    parent.startActivityForResult(new Intent(parent, ActivityRuleConfig.class), RULE_REQUEST_CODE);
                break;

            case RPCClient.CMD_GET_RULE:
                if (msg.obj == null)
                    break;
                
                buf = (byte[])msg.obj;
                ruleOutput = new RuleOutput();
                ruleOutput.deserialize(buf);
                RuleManager.put(ruleOutput.getID(), ruleOutput);
                
                if ((--mRuleCtr) == 0) {
                    LogUtil.d("START !!!");
                    parent.startActivityForResult(new Intent(parent, ActivityRuleConfig.class), RULE_REQUEST_CODE);
                }
                break;
            }
        }
    }
    
   
    private void complain(String strMsg) {
        LogUtil.e(strMsg);
        alert(strMsg);
    }
    
    private void complain(int nResID) {
        String strMsg = getResources().getString(nResID);
        
        LogUtil.e(strMsg);
        alert(strMsg);
    }

    private void alert(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    
    private void alert(int nResID) {
        String strMsg = getResources().getString(nResID);
        Toast.makeText(this, strMsg, Toast.LENGTH_SHORT).show();
    }
    
    
    private int findItemPosbyId(int id) {
        int pos = 0;

        for (Item i : items) {
            if (i.getMode() != Item.MODE_SECTION) {
                EntryItem ei = (EntryItem)i;
                if (ei.id == id)
                  return pos;  
            }
            pos++;
        }
        
        return -1;
    }
    
    private void enableItemById(int id, boolean en) {
        int pos = findItemPosbyId(id);
        if (pos > 0) {
            EntryItem ei = (EntryItem)items.get(pos);
            
            ei.setEnabled(en);
            items.set(pos, ei);
        }
    }
    
    private void setResById(int id, int res) {
        int pos = findItemPosbyId(id);
        if (pos > 0) {
            EntryItem ei = (EntryItem)items.get(pos);
            ei.setDrawable(res);
            items.set(pos, ei);
        }
        updateUI();
    }
    
    private void setSubTitleById(int id, String text) {
        int pos = findItemPosbyId(id);
        if (pos > 0) {
            EntryItem ei = (EntryItem)items.get(pos);
            ei.setSubTitle(text);
            items.set(pos, ei);
        }
        updateUI();
    }
    
    private void updateUI() {
        if (mListView != null) {
            EntryAdapter adapter = (EntryAdapter)mListView.getAdapter();
            if (adapter != null)
                adapter.notifyDataSetChanged();
        }
    }
    
    private void removeItemById(int id) {
        int pos = findItemPosbyId(id);
        if (pos > 0) {
            items.remove(pos);
        }
    }
}
