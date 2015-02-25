package com.pinggusoft.zigbee_client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.TimePicker;

public class ActivityRuleConfig extends Activity {
    private ClientApp               mApp;
    private ZigBeeNode              mNode = null;
    
    private Vector <Integer>        mListInputID  = new Vector <Integer>();
    private Vector <Integer>        mListOutputID = new Vector <Integer>();
    
    
    /*
     ***************************************************************************
     * 
     ***************************************************************************
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.config_rule);
        
        mApp = (ClientApp)getApplication();
        createScreen();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
    }
    
    @Override
    public synchronized void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
    
    public void onClickDone(View v) {
        //RuleManager.save(this);
        setResult(RESULT_OK, null);
        finish();
    }
    
    public void onClickCancel(View v) {
        setResult(RESULT_CANCELED, null);
        finish();
        //RuleManager.evaluate(mApp, null);
    }
    
    
    /*
    ***************************************************************************
    * 
    ***************************************************************************
    */
    public Dialog onClickTime(View v, final RuleOutput ruleOutput, final int rowKey, final int id) {
        final Dialog dlgView = new Dialog(this);
        dlgView.setTitle(R.string.config_rule_time);
        dlgView.setContentView(R.layout.config_rule_time);
        
        final TimePicker tpStart = (TimePicker)dlgView.findViewById(R.id.timePickerStart);
        final RuleInput  rule    = ruleOutput.getRule(rowKey);
        
        tpStart.setIs24HourView(true);
        tpStart.setCurrentHour(rule.getHour());
        tpStart.setCurrentMinute(rule.getMinute());
        
        for (int i = 0; i < 7; i++) {
            CheckBox cb = (CheckBox)dlgView.findViewById(R.id.checkBoxSun + i);
            cb.setChecked(rule.isCheckedDay(i));
        }
        
        final Button btnCancel = (Button)dlgView.findViewById(R.id.buttonCancel);
        btnCancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                dlgView.dismiss();
            }
        });
        
        final Button btnOK = (Button)dlgView.findViewById(R.id.buttonDone);
        btnOK.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                rule.setTime(tpStart.getCurrentHour(), tpStart.getCurrentMinute());
                for (int i = 0; i < 7; i++) {
                    CheckBox cb = (CheckBox)dlgView.findViewById(R.id.checkBoxSun + i);
                    rule.checkDay(i, cb.isChecked());
                }
                ruleOutput.putRule(rowKey, rule);
                dlgView.dismiss();
            }
        });

        dlgView.show();
        return dlgView;
    }

    public Dialog onClickThermo(View v, final RuleOutput ruleOutput, final int rowKey, final int id) {
        final Dialog dlgView = new Dialog(this);
        dlgView.setTitle(R.string.config_rule_thermo);
        dlgView.setContentView(R.layout.config_rule_thermo);
        
        final RuleInput rule = ruleOutput.getRule(rowKey);
        final TextView textMin = (TextView)dlgView.findViewById(R.id.textThermoMin);
        final SeekBar thermoMin = (SeekBar)dlgView.findViewById(R.id.sliderThermoMin);
        thermoMin.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
            }
        });
        thermoMin.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                    boolean fromUser) {
                progress -= 20;
                textMin.setText(String.format("%d กษ", progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
            
        });
        thermoMin.setProgress(rule.getMin() + RuleInput.THERMO_OFFSET);
        
        final TextView textMax = (TextView)dlgView.findViewById(R.id.textThermoMax);
        final SeekBar thermoMax = (SeekBar)dlgView.findViewById(R.id.sliderThermoMax);
        thermoMax.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
            }
        });
        thermoMax.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                    boolean fromUser) {
                progress -= 20;
                textMax.setText(String.format("%d กษ", progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
            
        });
        thermoMax.setProgress(rule.getMax() + RuleInput.THERMO_OFFSET);
        

        final Button btnCancel = (Button)dlgView.findViewById(R.id.buttonCancel);
        btnCancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                dlgView.dismiss();
            }
        });
        
        final Button btnOK = (Button)dlgView.findViewById(R.id.buttonDone);
        btnOK.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                SeekBar tp = (SeekBar)dlgView.findViewById(R.id.sliderThermoMin);
                int min = tp.getProgress();
                tp = (SeekBar)dlgView.findViewById(R.id.sliderThermoMax);
                int max = tp.getProgress();

                rule.setRange(min - RuleInput.THERMO_OFFSET, max - RuleInput.THERMO_OFFSET);
                ruleOutput.putRule(rowKey, rule);
                dlgView.dismiss();
            }
        });

        dlgView.show();
        
        return dlgView;
    }
    
    private TableRow addRow(final int nRowKey, final RuleOutput ruleOutput, final TableLayout tblInput, final ArrayAdapter<String> adapterInput) {
        final TableRow    tblRow        = (TableRow)LayoutInflater.from(this).inflate(R.layout.config_rule_input_row, null);
        final Spinner     spinInput     = (Spinner)tblRow.findViewById(R.id.spinnerGpioInput);
        final Spinner     spinCondition = (Spinner)tblRow.findViewById(R.id.spinnerCondition);
        final Spinner     spinOperator  = (Spinner)tblRow.findViewById(R.id.spinnerOperator);
        final Button      btnCondition  = (Button)tblRow.findViewById(R.id.buttonRangeTime);
        final Button      btnMinus      = (Button)tblRow.findViewById(R.id.buttonMinus);
        final int         ids           = mListInputID.get(0);
        
        // spin input handler
        spinInput.setAdapter(adapterInput);
        spinInput.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent,
                    View view, int position, long id) {
                final int ids = mListInputID.get(position);
                final int usage = (int)(ids & 0x0f);
                final RuleInput rule = ruleOutput.getRule(nRowKey);
                
                if (rule == null) {
                    LogUtil.e("NO ROW : %d", nRowKey);
                    return;
                }
                
                rule.setID(ids);
                if (usage == RuleInput.USAGE_TIME) {
                    spinCondition.setVisibility(View.GONE);
                    btnCondition.setVisibility(View.VISIBLE);
                    btnCondition.setText(rule.getTimeString());
                    btnCondition.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Dialog dlg = onClickTime(v, ruleOutput, nRowKey, ids);
                            dlg.setOnDismissListener(new OnDismissListener() {
                                @Override
                                public void onDismiss(DialogInterface dialog) {
                                    btnCondition.setText(rule.getTimeString());
                                }
                            });
                        }
                    });
                } else if (usage == ZigBeeNode.TYPE_INPUT_ANALOG){
                    spinCondition.setVisibility(View.GONE);
                    btnCondition.setVisibility(View.VISIBLE);
                    btnCondition.setText(rule.getThermoString());
                    btnCondition.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Dialog dlg = onClickThermo(v, ruleOutput, nRowKey, ids);
                            dlg.setOnDismissListener(new OnDismissListener() {
                                @Override
                                public void onDismiss(DialogInterface dialog) {
                                    btnCondition.setText(rule.getThermoString());
                                }
                            });
                        }
                    });
                } else {
                    spinCondition.setVisibility(View.VISIBLE);
                    btnCondition.setVisibility(View.GONE);
                    ArrayAdapter<CharSequence> adapterCondition = ArrayAdapter.createFromResource(ActivityRuleConfig.this, R.array.config_rule_condition_gpio, R.layout.config_spinner); 
                    spinCondition.setAdapter(adapterCondition);
                    spinCondition.setSelection(rule.getMax());
                    spinCondition.setOnItemSelectedListener(new OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent,
                                View view, int position, long id) {
                            RuleInput rule = ruleOutput.getRule(nRowKey);
                            rule.setRange(position, position);
                            ruleOutput.putRule(nRowKey, rule);
                        }
                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {
                            
                        }
                    });
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // spin operator 
        ArrayAdapter<CharSequence> adapterOperator = ArrayAdapter.createFromResource(ActivityRuleConfig.this, R.array.config_rule_operator, R.layout.config_spinner); 
        spinOperator.setAdapter(adapterOperator);
        spinOperator.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                    int position, long id) {
                RuleInput rule = ruleOutput.getRule(nRowKey);
                if (rule == null)
                    rule = new RuleInput(ids);
                rule.setOP(position);
                ruleOutput.putRule(nRowKey, rule);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
            
        });

        // minus button handler
        btnMinus.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                tblInput.removeViewInLayout(tblRow);
                ruleOutput.removeRow(nRowKey);
                ruleOutput.removeRule(nRowKey);
                
                tblInput.requestLayout();
                ruleOutput.redrawRules();
            }
        });
        
        // add or update row
        tblInput.addView(tblRow);
        ruleOutput.putRow(nRowKey, tblRow);
        
        RuleInput rule = ruleOutput.getRule(nRowKey);
        if (rule == null) {
            rule = new RuleInput(ids);
            ruleOutput.putRule(nRowKey, rule);
            ruleOutput.incRowKey();
        } else {
            int pos = mListInputID.indexOf(rule.getID());
            spinInput.setSelection(pos);
            spinOperator.setSelection(rule.getOP());
        }
        ruleOutput.redrawRules();

        return tblRow;
    }
    
    private void createRuleTable(int pos) {
        if (mListOutputID.size() < (pos + 1))
            return;
        
        int id = mListOutputID.get(pos);
        
        LinearLayout layoutContainer = (LinearLayout)findViewById(R.id.container);
        layoutContainer.removeAllViewsInLayout();
        
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        llp.setMargins(10, 10, 10, 10);
        
        LinearLayout.LayoutParams llpBlank = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 60);
        llpBlank.setMargins(10, 50, 10, 50);
        
        for (int k = 0; k < 3; k++) {
            id = RuleOutput.rebuildID(id, k);

            LinearLayout layoutPort = (LinearLayout)LayoutInflater.from(this).inflate(R.layout.config_rule_port, null);
            TextView text = (TextView)layoutPort.findViewById(R.id.textGpioAction);
            text.setText(R.string.config_rule_action_off + k);
            
            RuleOutput rp = RuleManager.get(id);
            if (rp == null) {
                LogUtil.d("new ruleoutput");
                rp = new RuleOutput(id);
                RuleManager.put(id, rp);
            }
            final RuleOutput ruleOutput = rp;

            // rule row
            final TableLayout tblInput = (TableLayout)LayoutInflater.from(this).inflate(R.layout.config_rule_input_header, null);
            final Button      btnPlus  = (Button)tblInput.findViewById(R.id.buttonPlus);
            btnPlus.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    addRow(ruleOutput.getRowKey(), ruleOutput, tblInput, mAdapterInput);
                }
            });
            
            
            int nRuleCnt = ruleOutput.getRuleCnt();
            if (nRuleCnt > 0) {
                LogUtil.d("ID:%x, rules:%d", rp.getID(), nRuleCnt);
                for (int i = 0; i < nRuleCnt; i++) {
                    addRow(i, ruleOutput, tblInput, mAdapterInput);
                }
            }
            layoutPort.addView(tblInput);
            layoutContainer.addView(layoutPort, llp);
            
            if (k < 2) {
                LinearLayout blankLayout = new LinearLayout(this);
                blankLayout.setBackgroundColor(0x7f00007f);
                layoutContainer.addView(blankLayout, llpBlank);
            }
        }
    }
    
    private ArrayAdapter<String> mAdapterInput = null;
    private ArrayAdapter<String> mAdapterOutput = null;
    
    private void createScreen() {
        int nCtr = mApp.getNodeCtr();
        
        List<String> listInputs = new ArrayList<String>();
        List<String> listOutputs = new ArrayList<String>();
        for (int i = 0; i < nCtr; i++) {
            ZigBeeNode node = mApp.getNode(i);
            for (int j = 0; j < node.getMaxGPIO(); j++) {
                int usage = node.getGpioUsage(j);
                if (ZigBeeNode.TYPE_INPUT_TOUCH <= usage && usage <= ZigBeeNode.TYPE_INPUT_ANALOG) {
                    listInputs.add(node.getGpioName(j));
                    int id = RuleOutput.buildID(i, j, usage);
                    mListInputID.add((Integer)(id));
                } else if (usage == ZigBeeNode.TYPE_OUTPUT_LIGHT) {
                    listOutputs.add(node.getGpioName(j));
                    int id = RuleOutput.buildID(i, j, usage);
                    mListOutputID.add((Integer)(id));
                }
            }
        }
        listInputs.add(getResources().getString(R.string.config_rule_input_time));
        mListInputID.add((Integer)(RuleInput.USAGE_TIME));

        mAdapterInput  = new ArrayAdapter<String>(this, R.layout.config_spinner, listInputs);
        mAdapterOutput = new ArrayAdapter<String>(this, R.layout.config_spinner, listOutputs);
        
        final Spinner spinPort = (Spinner)findViewById(R.id.spinnerGpioOutput);
        spinPort.setAdapter(mAdapterOutput);
        spinPort.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                    int position, long id) {
                LogUtil.d("PORT SELECTED : %d", position);
                createRuleTable(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
            
        });
        
        if (mListOutputID.size() > 0) {
            for (int i = 0; i < mListOutputID.size(); i++)
                createRuleTable(i);
        }
    }


    
    /*
    ***************************************************************************
    * 
    ***************************************************************************
    */

}
