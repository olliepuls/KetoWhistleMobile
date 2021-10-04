package com.example.bluetooth_test.Data;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;

import java.util.ArrayList;

public class DataManager {
    private LocalBroadcastManager mBroadcastManager;

    public static final String ACTION_NEW_MESSAGE = "MY_DATA_MESSAGE_INTENT";
    public static final String ACTION_NEW_DATA_POINT = "MY_DATA_POINT_INTENT";
    public static final String ACTION_DATA_RESET = "MY_DATA_RESET_INTENT";
    public static final String MESSAGE_KEY = "MESSAGE";
    public static final String DATA_POINT_KEY = "DATA_POINT";
    public static final String LABEL_KEY = "DATA_LABEL";
    public static final String RESET_INSTRUCTION_KEY = "RESET_INSTRUCTION";

    public DataManager(Context context){
        mBroadcastManager = LocalBroadcastManager.getInstance(context);
    }

    public void sendMessage(String message){
        Intent mIntent = new Intent(ACTION_NEW_MESSAGE);
        mIntent.putExtra(MESSAGE_KEY, message);
        mBroadcastManager.sendBroadcast(mIntent);
    }

    public void sendDataPoint(String label, DataPoint dp){
        Intent mIntent = new Intent(ACTION_NEW_DATA_POINT);
        mIntent.putExtra(DATA_POINT_KEY, dp);
        mIntent.putExtra(LABEL_KEY, label);
        mBroadcastManager.sendBroadcast(mIntent);
    }

    public void sendReset(boolean all){
        Intent mIntent = new Intent(ACTION_DATA_RESET);
        mIntent.putExtra(RESET_INSTRUCTION_KEY, all);
        mBroadcastManager.sendBroadcast(mIntent);
    }

    public static IntentFilter createFilter(boolean message, boolean data, boolean reset){
        IntentFilter filter = new IntentFilter();
        if(message) filter.addAction(ACTION_NEW_MESSAGE);
        if(data) filter.addAction(ACTION_NEW_DATA_POINT);
        if(reset) filter.addAction(ACTION_DATA_RESET);
        return filter;
    }

    public void registerReceiver(@NonNull BroadcastReceiver broadcastReceiver, IntentFilter filter){
        mBroadcastManager.registerReceiver(broadcastReceiver, filter);
    }

    public void deregisterReceiver(@NonNull BroadcastReceiver broadcastReceiver){
        mBroadcastManager.unregisterReceiver(broadcastReceiver);
    }
}
