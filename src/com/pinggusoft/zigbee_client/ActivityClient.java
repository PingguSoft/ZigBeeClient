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
    private static final int    RC_REQUEST = 10001;
    private static final String SKU_PRODUCT = "com.pinggusoft.btcon";

    private ClientApp       mApp;
    private boolean         mIsPurchased = true;
    private IabHelper       mHelper;
    
    private ArrayList<Item> items = new ArrayList<Item>();
    private ListView        mListView = null;
    private EntryItem       mPurchaseItem = null;
    private RPCClient       mRPC = null;
    private int             mIntNodeCtr = 0;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mApp =  (ClientApp)getApplication();
        setContentView(R.layout.main_list_view);

        String base64EncodedPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAowry+1jfMdRZlz2gDrc3gkzwKtNyuFdwm+Pk2y+IyE2D67m17I4ZAq0zlhkJSU2NrSA6Su/3GPXVv412zIk3vveMVS4SwqTBhDVfJQek9YRWPVNOMJZBVA5j+C1T5ekdippU1I6fG/q1+NmdInE5xdk4K1bBNlHYZL40eZ6X2ejf7zi9RIthTPqM7c+Nl52GInbPRT0nlFgC9HUGDIMegiLtYiWSdlTFTUz5/Re8/ieM3bH6KXF289ZbsExZTJXvM6Io44D5Pf41XeSiVhGktvs8Chk0YZQ/h5S/4G+WpQ7TlgXhmsPQ91RVR49sUKLk1rh44urQbJ5kpptd2OLwOQIDAQAB";
        LogUtil.e("Creating IAB helper.");
        mHelper = new IabHelper(this, base64EncodedPublicKey);

        // enable debug logging (for a production application, you should set this to false).
        mHelper.enableDebugLogging(BuildConfig.DEBUG ? true : false);

        // Start setup. This is asynchronous and the specified listener
        // will be called once setup completes.
        LogUtil.e("Starting setup.");
        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                LogUtil.e("Setup finished.");

                if (!result.isSuccess()) {
                    // Oh noes, there was a problem.
                    complain(getResources().getString(R.string.main_inapp_setting_fail) + " " + result);
                    mIsPurchased = false;
                    return;
                }

                // Have we been disposed of in the meantime? If so, quit.
                if (mHelper == null) 
                    return;

                // IAB is fully set up. Now, let's get an inventory of stuff we own.
                LogUtil.e("Setup successful. Querying inventory.");
                mHelper.queryInventoryAsync(mGotInventoryListener);
            }
        });
        
        mRPC = new RPCClient(getApplicationContext(), new RPCHandler(this));
//        composeScreen();
    }
    
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
        
        for (int i = 0; i < mIntNodeCtr; i++) {
            ZigBeeNode node = mApp.getNode(i);
            
            items.add(new SectionItem(node.getName() + " [" + node.getAddr() + "]"));
            
            for (int j = 0; j < node.getMaxGPIO(); j++) {
                LogUtil.d("GPIO:%d, USAGE:%d", j, node.getGpioUsage(j));
                int nResID = getResID(node.getGpioUsage(j), 0);
                if (nResID > 0) {
                    items.add(new EntryItem(nResID, node.getGpioName(j), 
                            " ", (i << 16) | j));
                }
            }
        }

/*        
        mPurchaseItem = new EntryItem(R.drawable.icon_purchase, getString(R.string.main_purchase), 
                getString(R.string.main_purchase_desc), ID_PURCHASE);
        items.add(mPurchaseItem);
        items.add(new EntryItem(R.drawable.icon_notice, getString(R.string.main_notice), 
                getString(R.string.main_notice_desc), ID_NOTICE));
        items.add(new EntryItem(R.drawable.icon_quit, getString(R.string.main_quit), 
                getString(R.string.main_quit_desc), ID_QUIT));
*/
        
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

                    int nNode = item.id >> 16;
                    int gpio = (int)(item.id & 0xffff);
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
        if (ClientApp.isAboveICS()) {
            ActionBar bar = getActionBar();
            bar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#222222")));
            int titleId = getResources().getIdentifier("action_bar_title", "id", "android");
            TextView abTitle = (TextView) findViewById(titleId);
            abTitle.setTextColor(Color.WHITE);
        }
        
        updateButtons(mIsPurchased);
    }
    
    @Override
    public synchronized void onPause() {
        super.onPause();
        LogUtil.e("onPause");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        
        // very important:
        LogUtil.e("Destroying helper.");
        if (mHelper != null) {
            mHelper.dispose();
            mHelper = null;
        }
    }
    
    private Dialog mDialog = null;
    public void onClickNotice(View v) {
        mDialog = new Dialog(this);
        mDialog.setTitle(R.string.main_notice);
        mDialog.setContentView(R.layout.main_notice);
        mDialog.setCancelable(false);

        final WebView view = (WebView)mDialog.findViewById(R.id.webView);
        
        String strURL;
        if (Locale.getDefault().getLanguage().equals("ko"))
            strURL = "file:///android_res/raw/notice_ko.html";
        else
            strURL = "file:///android_res/raw/notice_en.html";
        view.loadUrl(strURL);
        view.setBackgroundColor(0x00000000);
        
        final Button btnOK = (Button)mDialog.findViewById(R.id.buttonOK);
        btnOK.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mDialog.dismiss();
            }
        });

        mDialog.show();
    }


    static class RPCHandler extends Handler {
        private WeakReference<ActivityClient> mParent;
        
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
            
            switch (msg.what) {
            case RPCClient.CMD_RPC_READY:
                LogUtil.i("READY !!!");
                parent.mRPC.asyncGetNodeCtr();
                break;
                
            case RPCClient.CMD_GET_NODE_CTR:
                if (msg.arg2 < 0)
                    break;
                
                parent.mIntNodeCtr = msg.arg2;
                for (int i = 0; i < msg.arg2; i++)
                    parent.mRPC.asyncGetNode(i);
                break;
                
            case RPCClient.CMD_GET_NODE:
                if (msg.obj == null)
                    break;
                
                byte[] buf = (byte[])msg.obj;
                node = new ZigBeeNode();
                node.deserialize(buf);
                parent.mApp.addNode(node, false);
                
                if (parent.mApp.getNodeCtr() == parent.mIntNodeCtr) {
                    parent.composeScreen();
                    for (int i = 0; i < parent.mIntNodeCtr; i++) {
                        parent.mRPC.asyncReadGpio((i << 16) | (0xffff));
                        parent.mRPC.asyncReadAnalog((i << 16) | (0xffff));
                    }
                }
                break;
                
            case RPCClient.CMD_READ_GPIO:
                if (msg.arg2 < 0)
                    break;
                
                nid  = (int)(msg.arg1 >> 16);
                gpio = (int)(msg.arg1 & 0xffff);
                node = parent.mApp.getNode(nid);
                
                if (gpio < node.getMaxGPIO()) {
                    node.setGpioValue(gpio, msg.arg2);
                    val   = node.getGpioValue(gpio);
                    resID = parent.getResID(node.getGpioUsage(gpio), val);
                    LogUtil.d(String.format("GPIO%d=%d", gpio, val));
                    parent.setResById(msg.arg1, resID);
                } else {
                    node.setGpioValue(msg.arg2);
                    LogUtil.d(String.format("NID:%x GPIO=%s", msg.arg1, node.getGpioValue()));
                    for (int i = 0; i < node.getMaxGPIO(); i++) {
                        val   = node.getGpioValue(i);
                        resID = parent.getResID(node.getGpioUsage(i), val);
                        parent.setResById((nid << 16) | i, resID);
                    }
                }
                break;
                
            case RPCClient.CMD_WRITE_GPIO:
                if (msg.arg2 < 0)
                    break;
                
                nid  = (int)(msg.arg1 >> 16);
                gpio = (int)(msg.arg1 & 0xffff);
                node = parent.mApp.getNode(nid);
                node.setGpioValue(gpio, msg.arg2);
                break;
            
            case RPCClient.CMD_READ_ANALOG:
                if (msg.arg2 < 0)
                    break;

                nid  = msg.arg1 >> 16;
                gpio = (int)(msg.arg1 & 0xffff);
                node = parent.mApp.getNode(nid);
                
                if (gpio < node.getMaxGPIO()) {
                    node.setGpioAnalog(gpio, msg.arg2);
                    parent.setSubTitleById(msg.arg1, String.format("ADC=%d", node.getGpioAnalog(gpio)));
                    LogUtil.d(String.format("NID:%d GPIO=%d ADC=%d", nid, gpio, node.getGpioAnalog(gpio)));
                } else {
                    LogUtil.d(String.format("NID:%d ADCs=%s", nid, node.getGpioAnalog()));
                }
                break;
            }
        }
    }
    
    
    /*
    ***************************************************************************
    * In-App Billing
    ***************************************************************************
    */
    public void onClickPurchase(View v) {
        LogUtil.e("Buy button clicked; launching purchase flow for upgrade.");

        /* TODO: for security, generate your payload here for verification. See the comments on
         *        verifyDeveloperPayload() for more info. Since this is a SAMPLE, we just use
         *        an empty string, but on a production mApp you should carefully generate this. */
        String payload = String.valueOf(System.currentTimeMillis());

        mHelper.launchPurchaseFlow(this, SKU_PRODUCT, RC_REQUEST,
                mPurchaseFinishedListener, payload);
    }
    
    // Listener that's called when we finish querying the items and subscriptions we own
    IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            LogUtil.e("Query inventory finished.");

            // Have we been disposed of in the meantime? If so, quit.
            if (mHelper == null) return;

            // Is it a failure?
            if (result.isFailure()) {
                complain(getResources().getString(R.string.main_inapp_query_fail) + " : " + result);
                return;
            }

            LogUtil.e("Query inventory was successful.");

            /*
             * Check for items we own. Notice that for each purchase, we check
             * the developer payload to see if it's correct! See
             * verifyDeveloperPayload().
             */
            Purchase premiumPurchase = inventory.getPurchase(SKU_PRODUCT);
            mIsPurchased = (premiumPurchase != null && verifyDeveloperPayload(premiumPurchase));
            LogUtil.e("Product is " + (mIsPurchased ? "Purchased" : "not purchased"));
            updateButtons(mIsPurchased);
        }
    };
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        LogUtil.e("onActivityResult(" + requestCode + "," + resultCode + "," + data);
        if (mHelper == null) return;

        // Pass on the activity result to the helper for handling
        if (!mHelper.handleActivityResult(requestCode, resultCode, data)) {
            // not handled, so handle it ourselves (here's where you'd
            // perform any handling of activity results not related to in-mApp
            // billing...
            super.onActivityResult(requestCode, resultCode, data);
        }
        else {
            LogUtil.e("onActivityResult handled by IABUtil.");
        }
    }

    /** Verifies the developer payload of a purchase. */
    boolean verifyDeveloperPayload(Purchase p) {
        String payload = p.getDeveloperPayload();

        /*
         * TODO: verify that the developer payload of the purchase is correct. It will be
         * the same one that you sent when initiating the purchase.
         *
         * WARNING: Locally generating a random string when starting a purchase and
         * verifying it here might seem like a good approach, but this will fail in the
         * case where the user purchases an item on one device and then uses your mApp on
         * a different device, because on the other device you will not have access to the
         * random string you originally generated.
         *
         * So a good developer payload has these characteristics:
         *
         * 1. If two different users purchase an item, the payload is different between them,
         *    so that one user's purchase can't be replayed to another user.
         *
         * 2. The payload must be such that you can verify it even when the mApp wasn't the
         *    one who initiated the purchase flow (so that items purchased by the user on
         *    one device work on other devices owned by the user).
         *
         * Using your own server to store and verify developer payloads across mApp
         * installations is recommended.
         */
        LogUtil.e("Payload:" + payload);

        return true;
    }

    // Callback for when a purchase is finished
    IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            LogUtil.e("Purchase finished: " + result + ", purchase: " + purchase);

            // if we were disposed of in the meantime, quit.
            if (mHelper == null) return;

            if (result.isFailure()) {
                complain(getResources().getString(R.string.main_inapp_fail) + " : " + result);
                return;
            }
            if (!verifyDeveloperPayload(purchase)) {
                complain(getResources().getString(R.string.main_inapp_fail) + " : " + getResources().getString(R.string.main_inapp_auth_fail));
                return;
            }

            LogUtil.e("Purchase successful.");
            if (purchase.getSku().equals(SKU_PRODUCT)) {
                alert(R.string.main_inapp_success);
                mIsPurchased = true;
                updateButtons(mIsPurchased);
            }
        }
    };
    
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
    
    boolean mBoolRemoved = false;
    
    private void updateButtons(boolean boolPurchased) {
        boolean boolEnable = false;

        if(mApp.isAuthorized()) {
            alert(R.string.main_authorized);
            boolPurchased = true;
        }

        if (boolPurchased) {
            //removeItemById(ID_PURCHASE);
            mBoolRemoved = true;
        } else if (mBoolRemoved) {
            items.add(8, mPurchaseItem);
            mBoolRemoved = false;
        }

        if (!boolPurchased && mApp.IsExpired()) {
            alert(R.string.main_free_timeout);
            boolEnable = false;
        } else {
            if (mApp.getBTDevice() == null || mApp.getBTDevice().length() == 0) {
                boolEnable = false;
            } else {
                boolEnable = true;
            }
        }
        
        //setEnableItemById(ID_DEVICE_SETTING, boolEnable);
        if (mListView != null) {
            EntryAdapter adapter = (EntryAdapter)mListView.getAdapter();
            if (adapter != null)
                adapter.notifyDataSetChanged();
        }
    }
}
