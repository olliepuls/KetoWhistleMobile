package com.example.bluetooth_test.Data;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DataPoints extends ArrayList<DataPoint> {
    public final String label;
    private DataPoint maximum;
    private DataPoint minimum;

    public DataPoints(String label){
        super();
        maximum = null;
        minimum = null;
        this.label = label;
    }

    public DataPoint getMaximum(){
        return maximum;
    }

    public DataPoint getMinimum() {
        return minimum;
    }

    @Override
    public boolean add(DataPoint dataPoint) {
        boolean b = super.add(dataPoint);
        if(maximum == null || dataPoint.value > maximum.value) maximum = dataPoint;
        if(minimum == null || dataPoint.value < minimum.value) minimum = dataPoint;
        return b;
    }

    public DataPoint getMaximum(int n){
        if (n <= 0) throw new IllegalArgumentException("getMaximum: n is strictly positive");
        List<DataPoint> sub = this.subList(Math.max(0,this.size()-n), this.size());
        sub.sort(DataPoint.compareValues.reversed());
        return sub.get(0);
    }

    public DataPoint getMaximum(float time){
        if (time <= 0) throw new IllegalArgumentException("getMaximum: time is strictly positive");
        return this.stream().filter(dataPoint -> dataPoint.time >= this.last().time-time)
                .sorted(DataPoint.compareValues.reversed()).collect(Collectors.toList()).get(0);
    }

    public DataPoint getMinimum(int n){
        if (n <= 0) throw new IllegalArgumentException("getMaximum: n is strictly positive");
        List<DataPoint> sub = this.subList(Math.max(0,this.size()-n), this.size());
        sub.sort(DataPoint.compareValues);
        return sub.get(0);
    }

    public DataPoint getMinimum(float time){
        if (time <= 0) throw new IllegalArgumentException("getMaximum: time is strictly positive");
        return this.stream().filter(dataPoint -> this.last().time-dataPoint.time >= time)
                .sorted(DataPoint.compareValues).collect(Collectors.toList()).get(0);
    }

    public float average(int n){
        if (n <= 0) throw new IllegalArgumentException("getMaximum: n is strictly positive");
        float avg = 0;
        List<DataPoint> sub = this.subList(Math.max(0,this.size()-n), this.size());
        for(DataPoint dp: sub) avg += dp.value;
        return avg/sub.size();
    }

    public float average(float time){
        if (time <= 0) throw new IllegalArgumentException("getMaximum: time is strictly positive");
        Stream<DataPoint> vals = this.stream().filter(dataPoint -> this.last().time-dataPoint.time >= time);
        return vals.map(dp->dp.value).reduce(0f, Float::sum)/vals.count();
    }

    public float derivative(int n){
        return (get(size()-1).value-get(size()-n).value)/n;
    }

    public DataPoint last(){
        if(this.size() == 0) return null;
        return this.get(size()-1);
    }

}
