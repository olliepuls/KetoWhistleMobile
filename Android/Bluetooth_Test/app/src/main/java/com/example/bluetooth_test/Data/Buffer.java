package com.example.bluetooth_test.Data;


import android.support.annotation.NonNull;

import java.util.Collection;
import java.util.concurrent.ArrayBlockingQueue;

public class Buffer<E> extends ArrayBlockingQueue<E> {

    public Buffer(int capacity) {
        super(capacity);
    }

    @Override
    public boolean add(E e) {
        if(remainingCapacity() == 0) remove();
        return super.add(e);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        for (int i = 0; i < c.size()-remainingCapacity(); i++) {
            remove();
        }
        return super.addAll(c);
    }

    @NonNull
    @Override
    public String toString() {
        String s = super.toString().replaceAll(", ", ",\n");
        return s.substring(1,s.length()-1);
    }
}
