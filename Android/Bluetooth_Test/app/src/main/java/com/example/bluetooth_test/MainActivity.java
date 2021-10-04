package com.example.bluetooth_test;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

import com.example.bluetooth_test.BluetoothLE.BluetoothLEActivity;
import com.example.bluetooth_test.Data.DataLogger;
import com.example.bluetooth_test.Data.DataManager;
import com.example.bluetooth_test.Data.DataPoint;
import com.example.bluetooth_test.Data.DataPointSet;
import com.example.bluetooth_test.Data.DataSetInfo;
import com.example.bluetooth_test.Data.SerializablePair;
import com.example.bluetooth_test.Fragments.BreathMonitorPopup;
import com.example.bluetooth_test.Fragments.HelpDialog;
import com.example.bluetooth_test.Fragments.MessageFragment;
import com.example.bluetooth_test.Fragments.PlotFragment;
import com.example.bluetooth_test.PermissionManager.Permission;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

public class MainActivity extends BluetoothLEActivity implements BreathMonitorPopup.BreathMonitorPopupListener{

    static final int bufferLen = 10;
    private static final String PREF_DEVICE_NAME_KEY = "PREF_DEVICE_NAME_KEY";
    private static final String PREF_DEVICE_ADDR_KEY = "PREF_DEVICE_ADDR_KEY";
    private static final String PREF_CHARACTERISTIC_UUID_KEY = "PREF_CHAR_UUID_KEY";
    private static final String PREF_CHARACTERISTIC_PROPERTIES_KEY = "PREF_CHAR_PROPERTIES_KEY";
    private static final String PREF_CHARACTERISTIC_PERMISSIONS_KEY = "PREF_CHAR_PERMISSIONS_KEY";
    private AlertDialog mNotificationDialog;

    private Menu menu;
    private Button deviceMeasure;

    private ArrayList<SerializablePair<String, DataPointSet>> labelledData;

    private DataManager mDataManager;
    private BreathMonitor mBreathMonitor;
    private DataLogger AceDataLogger;
    private DataLogger CO2DataLogger;

    //Plot information for the main display
    private static final ArrayList<DataSetInfo> plot1 = new ArrayList<>(Arrays.asList(
            new DataSetInfo("CO2", Color.BLACK, true),
            new DataSetInfo("Acet", Color.GREEN, false)
    ));

    //Plot information for the secondary plot
    private static final ArrayList<DataSetInfo> plot2 = new ArrayList<>(Arrays.asList(
            new DataSetInfo("Humi", Color.BLUE, true),
            new DataSetInfo("Temp", Color.RED, false)
    ));


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mDataManager = new DataManager(this);
//        mDataLogger = new DataLogger(this, "Acet");
        //get the last device connected to so that it can try to auto connect
        //in the Resume part of the activity lifecycle
        SharedPreferences preferences = getPreferences(Context.MODE_PRIVATE);
        mDeviceName = preferences.getString(PREF_DEVICE_NAME_KEY,"");
        mDeviceAddress = preferences.getString(PREF_DEVICE_ADDR_KEY, "");
        try {
            mNotifyCharacteristic = new BluetoothGattCharacteristic(
                    UUID.fromString(preferences.getString(PREF_CHARACTERISTIC_UUID_KEY, "")),
                    preferences.getInt(PREF_CHARACTERISTIC_PROPERTIES_KEY, 0),
                    preferences.getInt(PREF_CHARACTERISTIC_PERMISSIONS_KEY, 0)
            );
        } catch (Exception e){
            Log.i("onCreate: ", "No Notification");
            e.printStackTrace();
        }
        Log.i("onCreate: mDeviceName", mDeviceName);
        Log.i("onCreate: mDeviceAddr", mDeviceAddress);
        //Set up the activity
        ((TextView) findViewById(R.id.connectionInfo)).setText(R.string.disconnected_message);
        ViewPager viewPager = findViewById(R.id.viewPager);
        viewPager.setOffscreenPageLimit(3);
        viewPager.setAdapter(new MyPagerAdapter(getSupportFragmentManager()));

        Button deviceReset = findViewById(R.id.buttonReset);
        deviceReset.setOnClickListener(v->{
            writeCharacteristic("RESET");
        });

        deviceMeasure = findViewById(R.id.buttonMeasure);
        deviceMeasure.setOnClickListener(v->{
//            if(mBreathMonitor.readyToDetect()) {
                Log.i("POPUP", "create new");
                FragmentManager fragmentManager = getSupportFragmentManager();
                BreathMonitorPopup breathMonitorPopup = new BreathMonitorPopup();
                Log.i("POPUP", "show");
                breathMonitorPopup.show(fragmentManager, "BMP");
//            }
        });
        deviceMeasure.setEnabled(false);

        mBreathMonitor = new BreathMonitor(this);
    }

    @Override
    protected ArrayList<Permission> permissionsOnStart() {
        ArrayList<Permission> permissions = super.permissionsOnStart();
        permissions.add(DataLogger.STORAGE_ACCESS_PERMISSION((activity, p) -> {
            AceDataLogger = new DataLogger(this, "Acet");
            CO2DataLogger = new DataLogger(this, "CO2");
            p.perform();
        }));
        return permissions;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        this.menu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id){
            case R.id.action_settings:
                //TODO: Actually have settings
                return true;
            case R.id.action_bluetooth_connect:
                //stop current scans and start a new scan
                stopScan();
                startScan();
                return true;
            case R.id.action_bluetooth_disconnect:
                //stops any current scans and disconnects any connected devices
                stopScan();
                disconnect();
                return true;
            case R.id.action_bluetooth_receive:
                //create a dialog so that the user can select a characteristic to follow
                mNotificationDialog = characteristicSelector(d->mNotificationDialog=null);
                mNotificationDialog.show();
                return true;
            case R.id.action_help:
                HelpDialog dialog = new HelpDialog(MainActivity.this);
                dialog.show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void sendMessage(String message) {
        writeCharacteristic(message);
    }

    @NonNull
    @Override
    public BreathMonitor setBreathMonitor() {
        return mBreathMonitor;
    }

    public class MyPagerAdapter extends FragmentPagerAdapter {
        public MyPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            switch (i){
                case 0:
                    return MessageFragment.newInstance(bufferLen);
                case 1:
                    return PlotFragment.newInstance(plot1);
                case 2:
                    return PlotFragment.newInstance(plot2);
                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            return 3;
        }
    };

    @Override
    protected void onNotificationEnabled(boolean autoConnected) {
        //Save the last connected device to auto connect on restart
        if(!autoConnected) {
            SharedPreferences.Editor prefEditor = getPreferences(Context.MODE_PRIVATE).edit();
            prefEditor.putString(PREF_DEVICE_NAME_KEY, mDeviceName);
            prefEditor.putString(PREF_DEVICE_ADDR_KEY, mDeviceAddress);
            prefEditor.putString(PREF_CHARACTERISTIC_UUID_KEY,
                    mNotifyCharacteristic.getUuid().toString());
            prefEditor.putInt(PREF_CHARACTERISTIC_PROPERTIES_KEY,
                    mNotifyCharacteristic.getProperties());
            prefEditor.putInt(PREF_CHARACTERISTIC_PERMISSIONS_KEY,
                    mNotifyCharacteristic.getPermissions());
            prefEditor.apply();
        }
        mDataManager.sendReset(true);
        if(mNotificationDialog!= null) mNotificationDialog.dismiss();
    }


    @Override
    protected void onConnectionUpdate(boolean connected) {
        TextView connInfo = findViewById(R.id.connectionInfo);
        if(connected){
            //Update UI
            deviceMeasure.setEnabled(true);
            connInfo.setText(mDeviceName);
            ((MenuItem) menu.findItem(R.id.action_bluetooth_receive)).setVisible(true);
            ((MenuItem) menu.findItem(R.id.action_bluetooth_disconnect)).setVisible(true);
        } else {
            //Update UI
            connInfo.setText(getString(R.string.disconnected_message));
            deviceMeasure.setEnabled(false);
            ((MenuItem) menu.findItem(R.id.action_bluetooth_receive)).setVisible(false);
            ((MenuItem) menu.findItem(R.id.action_bluetooth_disconnect)).setVisible(false);
        }
    }

    @Override
    protected void onDataAvailable(byte[] data) {
        String ascii = bytesToASCII(data);
        String message = ascii + "\t->\t" + bytesToHexString(data);
        mDataManager.sendMessage(message);
        if(ascii.equals("STARTED")) mDataManager.sendReset(false);
        else {
            String[] inputs = ascii.split(":");
            try {
                DataPoint dataPoint = new DataPoint((Integer.parseUnsignedInt(inputs[2]) / 10f),
                        (float) Integer.parseUnsignedInt(inputs[1]));
                mDataManager.sendDataPoint(inputs[0], dataPoint);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBreathMonitor.onDestroy();
        if(AceDataLogger!=null) AceDataLogger.onDestroy();
        if(CO2DataLogger!=null) CO2DataLogger.onDestroy();
    }
}
