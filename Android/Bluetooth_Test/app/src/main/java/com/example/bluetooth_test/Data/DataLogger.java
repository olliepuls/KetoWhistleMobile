package com.example.bluetooth_test.Data;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.example.bluetooth_test.PermissionManager;
import com.example.bluetooth_test.PermissionManager.Permission;
import com.example.bluetooth_test.Procedure;
import com.example.bluetooth_test.Storage.SaveLoadAndroid;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static com.example.bluetooth_test.Data.DataManager.ACTION_DATA_RESET;
import static com.example.bluetooth_test.Data.DataManager.ACTION_NEW_DATA_POINT;
import static com.example.bluetooth_test.Data.DataManager.DATA_POINT_KEY;
import static com.example.bluetooth_test.Data.DataManager.LABEL_KEY;
import static com.example.bluetooth_test.Data.DataManager.RESET_INSTRUCTION_KEY;

public class DataLogger {
    public static Permission STORAGE_ACCESS_PERMISSION(BiConsumer<Activity, Procedure> onGranted) {
        return new Permission("Data Logging Storage Access",
                Manifest.permission.WRITE_EXTERNAL_STORAGE, 999,
                "So that we can log the data from the device to your documents folder",
                true, onGranted,
                (activity, p) -> new AlertDialog.Builder(activity).setMessage("Have a nice day")
                        .setTitle("Understandable,").setNeutralButton("Ok", (d, i) -> {
                            d.dismiss();
                            p.perform();
                        }).show());
    }

    private final LocalBroadcastManager broadcastManager;
    private final String label;
    private String filename;



    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case ACTION_NEW_DATA_POINT:
                        if(intent.getStringExtra(LABEL_KEY).equals(label)){
                            DataPoint dataPoint = (DataPoint)
                                    intent.getSerializableExtra(DATA_POINT_KEY);
                            String data = dataPoint.time+","+dataPoint.value+"\n";
                            SaveLoadAndroid.saveText(context, filename, data,false);
                            Log.i("DataLogger:", "Logging");
                        }
                        break;
                    case ACTION_DATA_RESET:
                        if(intent.getBooleanExtra(RESET_INSTRUCTION_KEY, false))
                            DataLogger.this.filename = label+ LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)+".txt";
                        break;
                }
            }
        }
    };

    public DataLogger(Context context, String label){
        this.label = label;
        this.filename = label+ LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)+".txt";
        broadcastManager = LocalBroadcastManager.getInstance(context);
        broadcastManager.registerReceiver(broadcastReceiver,
                DataManager.createFilter(false, true, true));

    }

    public void onDestroy(){
        broadcastManager.unregisterReceiver(broadcastReceiver);
    }

}
