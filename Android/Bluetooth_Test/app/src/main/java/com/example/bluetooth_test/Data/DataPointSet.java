package com.example.bluetooth_test.Data;

import android.support.annotation.Nullable;

import java.util.HashMap;



public class DataPointSet extends HashMap<String, DataPoints> {

    @Nullable
    public DataPoints put(DataPoints obj) {
        return super.put(obj.label, obj);
    }

    public boolean addDataPoint(String label, DataPoint value){
        DataPoints dp = this.get(label);
        if(dp != null){
            dp.add(value);
            return true;
        } return false;
    }
}
