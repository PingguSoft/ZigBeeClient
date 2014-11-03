package com.pinggusoft.zigbee_client;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

public class ActivitySettings  extends PreferenceActivity {
    private boolean mBoolChanged = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();
    }
    
    
    @Override
    public synchronized void onPause() {
        super.onPause();
        LogUtil.e("onPause");
        
        if (mBoolChanged)
            setResult(RESULT_OK, null);
        else
            setResult(RESULT_CANCELED, null);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
    
//    @Override
//    public void onBuildHeaders(List<Header> target) {
//        loadHeadersFromResource(R.xml.preference_headers, target);
//    }
    
    public class SettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Make sure default values are applied.  In a real app, you would
            // want this in a shared function that is used to retrieve the
            // SharedPreferences wherever they are needed.
            PreferenceManager.setDefaultValues(getActivity(),
                    R.xml.preference, false);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preference);
            Preference pref = findPreference(ClientApp.KEY_SERVER_ADDR);
            pref.setSummary(((ClientApp)getApplicationContext()).getServerAddr());
            pref.setOnPreferenceChangeListener(onPreferenceChange);
            
            pref = findPreference(ClientApp.KEY_SERVER_PORT);
            pref.setSummary(""+((ClientApp)getApplicationContext()).getServerPort());
            pref.setOnPreferenceChangeListener(onPreferenceChange);
        }
    }
   
    private Preference.OnPreferenceChangeListener onPreferenceChange = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference pref, Object newValue) {
            LogUtil.d("Preference Change: " + pref.getKey() + ", " + newValue.toString());
            pref.setSummary(newValue.toString());
            mBoolChanged = true;
            setResult(RESULT_OK, null);
            
            return true;
        }
    };

}
