package com.example.bluetooth_test.Data;

import com.github.mikephil.charting.data.Entry;

import java.io.Serializable;
import java.util.Comparator;

public class DataPoint implements Serializable, Comparable<DataPoint> {
    public final float time;
    public final float value;


    public DataPoint(Float time, Float value) {
        this.time = time;
        this.value = value;
    }



    public static Comparator<DataPoint> compareValues = new Comparator<DataPoint>() {
        @Override
        public int compare(DataPoint o1, DataPoint o2) {
            return (int) (o1.value-o2.value);
        }
    };

    public static Comparator<DataPoint> compareOccurrence = new Comparator<DataPoint>() {
        @Override
        public int compare(DataPoint o1, DataPoint o2) {
            return (int) (o1.time-o2.time);
        }
    };


    @Override
    public int compareTo(DataPoint o) {
        return compareOccurrence.compare(this, o);
    }

    public Entry toEntry(){
        return new Entry(time, value);
    }
}