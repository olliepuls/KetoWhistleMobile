package com.example.bluetooth_test.BluetoothLE;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.example.bluetooth_test.R;

import java.util.ArrayList;

public class BluetoothArrayAdapter extends ArrayAdapter<BluetoothDevice>{
    private ArrayList<BluetoothDevice> items;
    private final LayoutInflater inflater;

    BluetoothArrayAdapter(Context context, LayoutInflater inflater, ArrayList<BluetoothDevice> items){
        super(context, R.layout.bluetooth_list_item, items);
        this.items = items;
        this.inflater = inflater;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null)
            convertView = inflater.inflate(R.layout.bluetooth_list_item, null) ;
        TextView nameText = convertView.findViewById(R.id.btli_name);
        TextView addrText = convertView.findViewById(R.id.btli_addr);

        BluetoothDevice device = items.get(position);
        nameText.setText(device.getName() == null || device.getName().equals("")?"Null":device.getName());
        addrText.setText(device.getAddress());

        return convertView;

    }
}
