package com.example.bluetooth_test;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.example.bluetooth_test.Data.DataManager;
import com.example.bluetooth_test.Data.DataPoint;
import com.example.bluetooth_test.Data.DataPointSet;
import com.example.bluetooth_test.Data.DataPoints;
import com.example.bluetooth_test.TimerUtils.TimeOut;

import java.io.Serializable;
import java.time.Duration;

import static com.example.bluetooth_test.Data.DataManager.ACTION_DATA_RESET;
import static com.example.bluetooth_test.Data.DataManager.ACTION_NEW_DATA_POINT;
import static com.example.bluetooth_test.Data.DataManager.DATA_POINT_KEY;
import static com.example.bluetooth_test.Data.DataManager.LABEL_KEY;
import static com.example.bluetooth_test.Data.DataManager.RESET_INSTRUCTION_KEY;

public class BreathMonitor {
    private final static String TAG = "BREATHMONITOR";
    //TODO: ADD suport for delay in acetone readings
    private float breathLimit = 90; //Humidity (percent)
    private float deepBreathLimit = 38000; //CO2

    private DataPointSet dataSets;
    private final static String humidity = "Humi";
    private final static String co2 = "CO2";
    private final static String acetone = "Acet";
    private final static String temperature = "Temp";

    private DataPoint acetoneReading;
    private boolean lastAcetoneReadingGood = false;
    private float deepBreathTime = 0;

    private LocalBroadcastManager broadcastManager;
    public static final String ACTION_STATE_CHANGED =
            "com.example.bluetooth_test.BreathMonitor.ACTION_BREATH_MONITOR_STATE_CHANGED";
    public static final String OLD_STATE = "com.example.bluetooth_test.BreathMonitor.OLD_STATE";
    public static final String NEW_STATE = "com.example.bluetooth_test.BreathMonitor.NEW_STATE";
    public static final String ACETONE_VALUE = "com.example.bluetooth_test.BreathMonitor.ACETONE_VALUE";

    public enum States implements Serializable {
//        STATE_START,
        STATE_SLEEP, //ready to read at user discretion
        STATE_WAITING, //waiting on breath
        STATE_BREATH, //waiting on deep breath
        STATE_DEEP_BREATH, //waiting for acetone to read
        STATE_POST_FAIL, //reading has failed at any stage
        STATE_POST_SUCCESS //reading has succeeded
    }

    public States getState() {
        return state;
    }

    private States state;

    private static final Duration BREATH_TIME_OUT = Duration.ofSeconds(120);
    private TimeOut breathTimeOut = new TimeOut(()->{
        if(state == States.STATE_BREATH){
            BreathMonitor.this.stateTransition(States.STATE_POST_FAIL);
        }
    });

    private static final Duration DEEP_BREATH_TIME_OUT = Duration.ofSeconds(10);
    private TimeOut deepBreathTimeOut = new TimeOut(()->{
        Log.i("deepBreathTime", ""+deepBreathTime);
        acetoneReading = dataSets.get(acetone).getMaximum(deepBreathTime);
        BreathMonitor.this.stateTransition(States.STATE_POST_SUCCESS);
    });

    private static final Duration START_TIME_OUT = Duration.ofSeconds(30);
    private TimeOut startTimeOut = new TimeOut(()->{
        if(state == States.STATE_WAITING){
            BreathMonitor.this.stateTransition(States.STATE_POST_FAIL);
        }
    });

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                switch (action){
                    case ACTION_NEW_DATA_POINT:
                        dataSets.addDataPoint(intent.getStringExtra(LABEL_KEY),
                                (DataPoint) intent.getSerializableExtra(DATA_POINT_KEY));
                        detection();
                        break;
                    case ACTION_DATA_RESET:
                        if(intent.getBooleanExtra(RESET_INSTRUCTION_KEY, false))
                            reset(true);
                        break;
                }
            }
        }
    };


    public BreathMonitor(Context context){
        broadcastManager = LocalBroadcastManager.getInstance(context);
        broadcastManager.registerReceiver(broadcastReceiver,
                DataManager.createFilter(false, true, true));
        reset(true);
    }

    private void reset(boolean hard){
        dataSets = new DataPointSet();
        dataSets.put(new DataPoints(humidity));
        dataSets.put(new DataPoints(co2));
        dataSets.put(new DataPoints(acetone));
        dataSets.put(new DataPoints(temperature));
        if (hard) state = States.STATE_SLEEP;
    }

    private void stateTransition(States newState){
        Log.d(TAG, "stateTransition() called with: newState = [" + newState + "]");
        States oldState = state;
        state = newState;
        Intent intent = new Intent(ACTION_STATE_CHANGED);
        intent.putExtra(OLD_STATE, oldState);
        intent.putExtra(NEW_STATE, newState);
        switch (oldState){

            case STATE_SLEEP:
                break;
            case STATE_WAITING:
                startTimeOut.cancel();
                break;
            case STATE_BREATH:
                breathTimeOut.cancel();
                break;
            case STATE_DEEP_BREATH:
                deepBreathTimeOut.cancel();
                break;
            case STATE_POST_FAIL:
                break;
            case STATE_POST_SUCCESS:
                break;
        }
        broadcastManager.sendBroadcast(intent);
        switch (newState){
            case STATE_SLEEP:
                break;
            case STATE_WAITING:
                startTimeOut.timeOut(START_TIME_OUT);
                break;
            case STATE_BREATH:
                breathTimeOut.timeOut(BREATH_TIME_OUT);
                break;
            case STATE_DEEP_BREATH:
                deepBreathTimeOut.timeOut(DEEP_BREATH_TIME_OUT);
                break;
            case STATE_POST_FAIL:
                break;
            case STATE_POST_SUCCESS:
                break;
        }
    }

    public BreathMonitor(Context context, float breathLimit, float deepBreathLimit){
        this(context);
        this.breathLimit = breathLimit;
        this.deepBreathLimit = deepBreathLimit;
    }

    public boolean readyToDetect(){
        DataPoint lastHumidity = dataSets.get(humidity).last();
        DataPoint lastCO2 = dataSets.get(co2).last();
        return state == States.STATE_SLEEP && lastHumidity != null &&  lastHumidity.value < 30 &&
                lastCO2 != null && lastCO2.value < 10000;
    }

    public boolean startingDetection(){
        if(readyToDetect()){
            stateTransition(States.STATE_WAITING);
            return true;
        }
        return false;
    }

    private void detection(){
        switch (state){
            case STATE_WAITING:
                if((dataSets.get(humidity).getMaximum() != null &&
                        dataSets.get(humidity).getMaximum().value > breathLimit) ||
                   (dataSets.get(co2).getMaximum() != null &&
                           dataSets.get(humidity).getMaximum() != null &&
                           dataSets.get(humidity).getMaximum().value > 60 &&
                           dataSets.get(co2).getMaximum().value > 1500)
                   ) {
                    stateTransition(States.STATE_BREATH);
                }
                break;
            case STATE_BREATH:
                if(dataSets.get(co2).average(4) > deepBreathLimit) {
                    Log.i(TAG, "detection: " + dataSets.get(co2).average(4) + " Thresh: " + deepBreathLimit);
                    deepBreathTime = dataSets.get(acetone).last().time;
                    stateTransition(States.STATE_DEEP_BREATH);
                }
                break;
            case STATE_DEEP_BREATH:
                if(dataSets.get(co2).average(4) < deepBreathLimit) {
                    acetoneReading = dataSets.get(acetone).getMaximum();
                    stateTransition(States.STATE_POST_SUCCESS);
                }
                break;
            case STATE_POST_FAIL:
            case STATE_POST_SUCCESS:
                stateTransition(States.STATE_SLEEP);
                break;
        }
    }

    public void onDestroy(){
        broadcastManager.unregisterReceiver(broadcastReceiver);
    }

    public boolean isLastAcetoneReadingGood(){return lastAcetoneReadingGood;}

    public DataPoint getAcetoneReading(){return acetoneReading;}


    public void debugStateTransition(States newState){
        Log.d(TAG, "debugStateTransition() called with: newState = [" + newState + "]");
        stateTransition(newState);
    }


}
