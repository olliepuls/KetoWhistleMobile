package com.example.bluetooth_test.BluetoothLE;

/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The scripts that this is based upon can be found at,
 * https://github.com/googlesamples/android-BluetoothLeGatt/blob/master/Application/src/main/java/com/example/android/bluetoothlegatt/DeviceControlActivity.java
 * and
 * https://github.com/googlesamples/android-BluetoothLeGatt/blob/master/Application/src/main/java/com/example/android/bluetoothlegatt/DeviceScanActivity.java
 */

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.Toast;

import com.example.bluetooth_test.PermissionManager;
import com.example.bluetooth_test.PermissionManager.Permission;
import com.example.bluetooth_test.Procedure;
import com.example.bluetooth_test.R;
import com.example.bluetooth_test.TimerUtils.RepeatAction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * For a given BLE device, this Activity provides the user interface to connect, display lineData,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */
public abstract class BluetoothLEActivity extends AppCompatActivity {

    private final static String TAG = BluetoothLEActivity.class.getSimpleName();
    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    protected String mDeviceName;
    protected String mDeviceAddress;
    protected BluetoothGattCharacteristic mNotifyCharacteristic;

    private BluetoothLeService mBluetoothLeService;
    private BluetoothArrayAdapter bluetoothArrayAdapter;
    private ArrayList<BluetoothDevice> scanned;

    private boolean mConnected = false;
    private boolean autoConnected = false;

    private RepeatAction sendData;
    private final long sendInterval = 150;

    protected PermissionManager mPermissionManager;

    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
    private ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
            = new ArrayList<ArrayList<HashMap<String, String>>>();

    ///////////////////////////////////////////////////////
    ////////////Some potentially useful getters////////////
    ///////////////////////////////////////////////////////
    public ArrayList<ArrayList<BluetoothGattCharacteristic>> getmGattCharacteristics() {
        return mGattCharacteristics;
    }

    public ArrayList<HashMap<String, String>> getGattServiceData() {
        return gattServiceData;
    }

    public ArrayList<ArrayList<HashMap<String, String>>> getGattCharacteristicData() {
        return gattCharacteristicData;
    }

    public boolean isConnected(){
        return mConnected;
    }

    private BluetoothGattCharacteristic hasCharacteristic(UUID uuid){
        for (ArrayList<BluetoothGattCharacteristic> array:mGattCharacteristics) {
            for (BluetoothGattCharacteristic characteristic: array){
                if(characteristic.getUuid().equals(uuid)) return characteristic;
            }
        }
        return null;
    }


    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            autoConnect();
            Log.i(TAG, "Initialized Bluetooth");
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
            Log.i("onServiceDisconnected: ", "Service is null");
        }
    };

    private void autoConnect(){
        try {
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
            autoConnected = true;
        } catch (Exception e){
            Log.e("autoConnect: ", "Something went wrong", e );
            e.printStackTrace();
        }
    }

    /**
     * This broadcast receiver handles the intents broadcast by the BluetoothLE Service.
     * Specifically those related to scanning
     */
    private final BroadcastReceiver mScanReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, @NonNull Intent intent) {
            String action = intent.getAction();

            //Scanning has ended
            if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Toast.makeText(context, "Discovery finished", Toast.LENGTH_LONG).show();
            }

            //Scanning has started
            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                Toast.makeText(context, "Discovery started", Toast.LENGTH_LONG).show();
            }
        }

    };

    /**
     * Resets the scanned devices list and then starts a new scan
     */
    public void startScan(){
        scanned = new ArrayList<>();
        bluetoothArrayAdapter = new BluetoothArrayAdapter(BluetoothLEActivity.this,
                getLayoutInflater(),
                scanned);

        bluetoothScanResults(bluetoothArrayAdapter, (d, i)->{
            mDeviceAddress = bluetoothArrayAdapter.getItem(i).getAddress();
            mDeviceName = bluetoothArrayAdapter.getItem(i).getName();
            Toast.makeText(BluetoothLEActivity.this,
                    "Selected "+ mDeviceName + " @ " + mDeviceAddress,
                    Toast.LENGTH_LONG).show();
            if(mDeviceAddress!=null) mBluetoothLeService.connect(mDeviceAddress);
        }, dismiss -> mBluetoothLeService.scanLeDevices(false, mScanCallback))
                .show();
        mBluetoothLeService.scanLeDevices(true, mScanCallback);
    }

    /**
     * Stops any current scans
     */
    public void stopScan(){
        mBluetoothLeService.scanLeDevices(false, mScanCallback);
    }

    /**
     * Disconnects from any attached BluetoothLE device
     */
    public void disconnect(){
        mBluetoothLeService.disconnect();
    }

    /**
     * Registers or unregisters notifications for updates on the given characteristic. Updates
     * the relevant descriptor if it is given.
     * @param characteristic the characteristic to act upon
     * @param enabled true to enable notifications, false to disable
     * @param descriptor the descriptor to update
     */
    public void registerCharacteristic(BluetoothGattCharacteristic characteristic,
                                       boolean enabled,
                                       @Nullable BluetoothGattDescriptor descriptor){
        mBluetoothLeService.setCharacteristicNotification(characteristic, enabled, descriptor);
    }


    /**
     * Displays the results of a scanning session. Can be displayed before or after scanning
     * has stopped.
     * @param btaa the array adapter that holds the device list
     * @param onItemselected an OnClickListener for when an item is selected
     * @param canceler a function that is called when the program cancels.
     * @return A formatted Alert Dialog to display results as they arrive.
     */
    private AlertDialog bluetoothScanResults(BluetoothArrayAdapter btaa,
                                             Dialog.OnClickListener onItemselected,
                                             Consumer<Void> canceler
    ){
        return new AlertDialog.Builder(this)
                .setAdapter(btaa, onItemselected).setTitle("Scanned Devices")
                .setOnDismissListener(d -> {canceler.accept(null);})
                .setNegativeButton("Cancel",(d,i)->d.cancel()).create();
    }

    /**
     * A scan callback that activates when a device is found during scanning
     */
    private ScanCallback mScanCallback = new ScanCallback()
    {
        @Override
        public void onScanResult(int callbackType, ScanResult result)
        {
            /* List device found */
            Log.i("callbackType", String.valueOf(callbackType));
            BluetoothDevice btDevice = result.getDevice();
            scanned.add(btDevice);
            HashSet<BluetoothDevice> tmp = new HashSet<>(scanned);
            scanned.clear();
            scanned.addAll(tmp);
            bluetoothArrayAdapter.notifyDataSetChanged();
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results)
        {

        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                autoConnected = false;
                updateConnectionState();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                Log.i(TAG, "GATT Services Discovered");
                updateGattServices(mBluetoothLeService.getSupportedGattServices());
                if(autoConnected){
                    mNotifyCharacteristic = hasCharacteristic(mNotifyCharacteristic.getUuid());
                    mBluetoothLeService.setCharacteristicNotification(mNotifyCharacteristic,
                            true, null);
                    onNotificationEnabled(true);
                }

            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                byte[] data = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                if (bytesToASCII(data).equals("RECEIVED")||bytesToASCII(data).equals("STARTED")) sendData.cancel();
                onDataAvailable(data);
            } else if (BluetoothLeService.ACTION_DATA_READ.equals(action)){
                Log.i(TAG, "Read data");
            }
        }
    };

    /**
     * Parses a byte array to its hexadecimal equivalent
     * @param data the input array
     * @return a hexadecimal string
     */
    public String bytesToHexString(byte[] data) {
        final StringBuilder stringBuilder = new StringBuilder(data.length);
        for (byte byteChar : data)
            stringBuilder.append(String.format("%02X", byteChar));
        return stringBuilder.toString();
    }

    /**
     * Converts a byte array to its ASCII equivalent
     * @param data the input array
     * @return an ASCII String
     */
    public String bytesToASCII(byte[] data){
        return new String(data);
    }

    /**
     * converts a byte array to an integer equivalent
     * @param data the input array
     * @param raw true if the array is ASCII hex
     * @return the integer equivalent value
     */
    public int bytesToInteger(byte[] data, boolean raw){
        if(raw) return Integer.parseUnsignedInt(bytesToHexString(data), 16);
        try {
            return Integer.parseInt(bytesToASCII(data),10);
        } catch (NumberFormatException e){
            return 0;
        }
    }

    // If a given GATT characteristic is selected, check for supported features.  This sample
    // demonstrates 'Read' and 'Notify' features.  See
    // http://d.android.com/reference/android/bluetooth/BluetoothGatt.html for the complete
    // list of supported characteristic features.

    public final ExpandableListView.OnChildClickListener servicesListClickListner =
            new ExpandableListView.OnChildClickListener() {
                @Override
                public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
                                            int childPosition, long id) {
                    if (mGattCharacteristics != null) {
                        final BluetoothGattCharacteristic characteristic =
                                mGattCharacteristics.get(groupPosition).get(childPosition);
                        final int charaProp = characteristic.getProperties();
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                            // If there is an active notification on a characteristic, clear
                            // it first so it doesn't update the data field on the user interface.
                            if (mNotifyCharacteristic != null) {
                                mBluetoothLeService.setCharacteristicNotification(
                                        mNotifyCharacteristic, false, null);
                                mNotifyCharacteristic = null;
                            }
                            mBluetoothLeService.readCharacteristic(characteristic);
                        }
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                            mNotifyCharacteristic = characteristic;
                            mBluetoothLeService.setCharacteristicNotification(
                                    characteristic, true, null);
                            onNotificationEnabled(false);
                        }
                        return true;
                    }
                    return false;
                }
            };

    /**
     * Creates a new Expandable list adapter based on the currently connected device's
     * available Services and Characteristics
     * @return a Simple Expandable List Adapter
     */
    public ExpandableListAdapter GATTServiceListAdapter(){
        return  new SimpleExpandableListAdapter(
                this,
                gattServiceData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {LIST_NAME, LIST_UUID},
                new int[] { android.R.id.text1, android.R.id.text2 },
                gattCharacteristicData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {LIST_NAME, LIST_UUID},
                new int[] { android.R.id.text1, android.R.id.text2 }
        );
    }

    /**
     * Writes the given value to the characteristic that has been chosen by the user in the App
     * @param value the value to set the Characteristic to
     * @return true if there is a characteristic that is available
     */
    public boolean writeCharacteristic(String value){
        if(mConnected && mNotifyCharacteristic != null) {
            sendData.cancel();
            sendData = new RepeatAction(()->mBluetoothLeService.writeCharacteristic(mNotifyCharacteristic, value), sendInterval);
            sendData.start();
            return true;
        }
        return false;
    }
    public final Permission COARSE_LOCATION_PERMISSION =
            new Permission(Manifest.permission.ACCESS_COARSE_LOCATION, 10101,
                    "I would rather not ask for this permission but it is needed to use " +
                            "Blutoothe LE.\nWithout it you will not get any scan results.",
                    false, PermissionManager.empty,
                    (activity, p)-> {
                        new AlertDialog.Builder(activity)
                                .setMessage(R.string.location_permission_denied_String)
                                .setTitle(":(").setNeutralButton("Good-Bye", (d, i) -> {
                            d.dismiss();
                        }).setOnDismissListener(d -> BluetoothLEActivity.this.finish()).show();
                    });

    protected ArrayList<Permission> permissionsOnStart(){
        ArrayList<Permission> permissions = new ArrayList<>();
        permissions.add(COARSE_LOCATION_PERMISSION);
        return permissions;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sendData = new RepeatAction(()->{}, sendInterval);
        mPermissionManager = new PermissionManager();
        //check if the necessary location permissions have been given
        mPermissionManager.addPermissions(permissionsOnStart());
        mPermissionManager.checkPermissions(this,
                (ArrayList<String>) permissionsOnStart().stream().map(permission -> permission.name)
                        .collect(Collectors.toList()), true);
        //create a binding to the GATT Service
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        boolean bound = bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        Log.i(TAG, "Bound: " + String.valueOf(bound));

    }

    @Override
    protected void onResume() {
        super.onResume();
        //register as a receiver to scan and data events
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        registerReceiver(mScanReceiver, makeScanningIntentFilter());
        if (mBluetoothLeService != null)
            if (!mBluetoothLeService.isBluetoothEnabled()) {
                BluetoothLeService.enableBluetooth(
                        BluetoothLEActivity.this);
            } else {
                autoConnect();
            }
    }

    /**
     * This function activates when a user responds to a permission request
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        mPermissionManager.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    @Override
    protected void onPause() {
        super.onPause();
        //on pausing of the app don't receive any updates
        unregisterReceiver(mGattUpdateReceiver);
        unregisterReceiver(mScanReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //when the app is destroyed unbind itself from the bluetooth connection service
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
        Log.i("onDestroy: ", "Service is null");
    }

    /**
     * Updates the connection state by the child class' overwritten method
     */
    private void updateConnectionState() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                onConnectionUpdate(mConnected);
            }
        });
    }


    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    public void updateGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
        gattServiceData = new ArrayList<HashMap<String, String>>();
        gattCharacteristicData
                = new ArrayList<ArrayList<HashMap<String, String>>>();
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(
                    LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
                    new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas =
                    new ArrayList<BluetoothGattCharacteristic>();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();
                currentCharaData.put(
                        LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharaString));
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);
            }
            mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }
    }

    protected AlertDialog characteristicSelector(DialogInterface.OnDismissListener onDismissListener){
        ExpandableListView expandableListView = new ExpandableListView(this);
        expandableListView.setAdapter(GATTServiceListAdapter());
        expandableListView.setOnChildClickListener(servicesListClickListner);
        expandableListView.setOnItemClickListener((parent, view, position, list_id) -> {});
        return new AlertDialog.Builder(this).setTitle("Available Services")
                .setView(expandableListView)
                .setNegativeButton("cancel", (d, i)->d.dismiss())
                .setOnDismissListener(onDismissListener).create();
    }

    /**
     * Creates an intent filter with al the necessary filters for scanning
     * @return a Scanning Intent Filter
     */
    private static IntentFilter makeScanningIntentFilter(){
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        return intentFilter;
    };

    /**
     * Creates an intent filter with al the necessary filters for GATT Events
     * @return a GATT Event filter
     */
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == BluetoothLeService.REQUEST_ENABLE_BT){
            if(resultCode == RESULT_OK){
                autoConnect();
            } else finish();
        }
    }

    /**
     * This function is called when a connection event has occurred
     * @param connected true if a device is currently connected
     */
    protected abstract void onConnectionUpdate(boolean connected);

    /**
     * This function is called when new data has been written to the subscribed characteristic
     * @param data the new data from the characteristic
     */
    protected abstract void onDataAvailable(byte[] data);

    /**
     * This function is called when a notifications have been enabled on a characteristic
     */
    protected abstract void onNotificationEnabled(boolean autoConnected);
}